package nro.models.network;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import nro.models.interfaces.*;

/** Standalone behavioral tests: no database, listener, or live server required. */
public final class SenderLifecycleTest {
    private static final ConcurrentHashMap<Long, Integer> logouts = new ConcurrentHashMap<>();
    private static volatile Thread logoutThread;

    public static void main(String[] args) throws Exception {
        System.setProperty("nro.sender.maxMessages", "4");
        System.setProperty("nro.sender.maxBytes", "64");
        System.setProperty("nro.sender.writeTimeoutMillis", "150");
        System.setProperty("nro.sender.maxQueueAgeMillis", "200");
        Network.gI().setAcceptHandler(new ISessionAcceptHandler() {
            public void sessionInit(ISession s) { }
            public void sessionDisconnect(ISession s) { logoutThread = Thread.currentThread(); logouts.merge(s.getID(), 1, Integer::sum); }
        });
        run("error closes socket and logs out on collector", SenderLifecycleTest::sendFailure);
        run("blocked write is cancelled by socket close", SenderLifecycleTest::stalledWrite);
        run("count overflow disconnects", () -> overflow(5, 1));
        run("byte overflow disconnects", () -> overflow(2, 40));
        run("queue age disconnects before sender starts", SenderLifecycleTest::queueAge);
        run("concurrent disconnect preserves collector logout", SenderLifecycleTest::concurrentDisconnect);
        run("shared broadcast is retained and FIFO", SenderLifecycleTest::broadcast);
        run("real unread loopback socket cancels stalled write", SenderLifecycleTest::realSlowSocket);
    }
    private static void sendFailure() throws Exception {
        TestSocket socket = new TestSocket(true, false);
        Session session = session(socket);
        session.start(); session.sendMessage(message(1));
        try { await(() -> socket.closed, "send failure left transport open");
            await(() -> logouts.getOrDefault(session.getID(), 0) == 1, "missing collector logout");
            check(logoutThread.getName().startsWith("Collector"), "logout ran on send/watchdog thread");
        } finally { session.disconnect(); }
    }
    private static void stalledWrite() throws Exception {
        TestSocket socket = new TestSocket(false, true); Session session = session(socket);
        session.start(); session.sendMessage(message(1));
        try { check(socket.writing.await(1, TimeUnit.SECONDS), "write did not start");
            await(() -> socket.closed, "write watchdog did not close socket");
        } finally { session.disconnect(); }
    }
    private static void overflow(int count, int bytes) throws Exception {
        TestSocket socket = new TestSocket(false, false); Session session = session(socket);
        session.startCollect();
        try { for (int i = 0; i < count; i++) session.sendMessage(message(bytes));
            check(socket.closed, "queue overflow left transport open");
        } finally { session.disconnect(); }
    }
    private static void queueAge() throws Exception {
        TestSocket socket = new TestSocket(false, false); Session session = session(socket);
        session.startCollect(); session.sendMessage(message(1));
        try { await(() -> socket.closed, "aged queue left transport open"); }
        finally { session.disconnect(); }
    }
    private static void concurrentDisconnect() throws Exception {
        TestSocket socket = new TestSocket(false, true); Session session = session(socket);
        session.start(); session.sendMessage(message(1));
        check(socket.writing.await(1, TimeUnit.SECONDS), "write did not start");
        Thread[] closers = new Thread[8];
        for (int i = 0; i < closers.length; i++) { closers[i] = new Thread(session::disconnect); closers[i].start(); }
        for (Thread thread : closers) { thread.join(1500); check(!thread.isAlive(), "disconnect deadlocked"); }
        await(() -> logouts.getOrDefault(session.getID(), 0) == 1, "concurrent disconnect lost collector logout");
        check(socket.closes.get() == 1, "transport closed more than once");
    }
    private static void broadcast() throws Exception {
        TestSocket a = new TestSocket(false, false), b = new TestSocket(false, false);
        Session first = session(a), second = session(b); Message shared = message(2);
        first.sendMessage(shared); second.sendMessage(shared); shared.cleanup();
        shared.writeByte(99); // Already queued bytes must not change for either recipient.
        first.sendMessage(message(1)); second.sendMessage(message(1)); first.start(); second.start();
        try { await(() -> a.bytes.size() == 9 && b.bytes.size() == 9, "broadcast/FIFO lost data");
            check(java.util.Arrays.equals(a.bytes.toByteArray(), b.bytes.toByteArray()), "broadcast packets differ");
            check(shared.getData().length == 3, "sender disposed shared message");
        } finally { first.disconnect(); second.disconnect(); }
    }
    private static void realSlowSocket() throws Exception {
        try (ServerSocket listener = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
             Socket peer = new Socket(InetAddress.getLoopbackAddress(), listener.getLocalPort());
             Socket accepted = listener.accept()) {
            peer.setReceiveBufferSize(1024);
            Session session = new Session(accepted);
            session.setSendCollect(new MessageSendCollect()); session.startCollect();
            Message resource = new Message(-32); resource.write(new byte[8 * 1024 * 1024]);
            AtomicReference<Throwable> result = new AtomicReference<>();
            Thread writer = new Thread(() -> {
                try { session.doSendMessage(resource); }
                catch (Throwable expected) { result.set(expected); }
            }, "loopback-write");
            try {
                writer.start(); writer.join(2000);
                check(!writer.isAlive(), "real socket write did not unblock");
                check(accepted.isClosed(), "real socket remained open");
                check(result.get() instanceof IOException, "stalled write did not fail with transport error");
            } finally { session.disconnect(); resource.cleanup(); writer.join(2000); }
        }
    }
    private static Session session(TestSocket socket) { Session s = new Session(socket); s.setSendCollect(new MessageSendCollect()); return s; }
    private static Message message(int bytes) throws Exception { Message m = new Message(1); m.write(new byte[bytes]); return m; }
    private interface Checked { void run() throws Exception; }
    private static void run(String name, Checked test) throws Exception { test.run(); System.out.println("PASS " + name); }
    private static void check(boolean value, String text) { if (!value) throw new AssertionError(text); }
    private static void await(java.util.function.BooleanSupplier condition, String failure) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(5);
        check(condition.getAsBoolean(), failure);
    }
    private static final class TestSocket extends Socket {
        volatile boolean closed; final boolean fail, block;
        final AtomicInteger closes = new AtomicInteger();
        final CountDownLatch writing = new CountDownLatch(1);
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        TestSocket(boolean fail, boolean block) { this.fail = fail; this.block = block; }
        public SocketAddress getRemoteSocketAddress() { return new InetSocketAddress("127.0.0.1", 1234); }
        public void setSendBufferSize(int size) { }
        public void setReceiveBufferSize(int size) { }
        public InputStream getInputStream() { return new InputStream() {
            public int read() throws IOException { synchronized (TestSocket.this) {
                while (!closed) try { TestSocket.this.wait(); } catch (InterruptedException e) { }
                throw new SocketException("closed");
            } }
        }; }
        public OutputStream getOutputStream() { return new OutputStream() {
            public void write(int value) throws IOException { writing.countDown(); synchronized (TestSocket.this) {
                while (block && !closed) try { TestSocket.this.wait(); } catch (InterruptedException e) { }
                if (closed || fail) throw new SocketException("simulated write failure");
                bytes.write(value);
            } }
        }; }
        public synchronized void close() { closes.incrementAndGet(); closed = true; notifyAll(); }
        public boolean isClosed() { return closed; }
    }
}
