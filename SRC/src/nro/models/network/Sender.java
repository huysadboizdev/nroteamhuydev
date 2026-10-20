package nro.models.network;

import java.net.Socket;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import nro.models.interfaces.IMessageSendCollect;
import nro.models.interfaces.ISession;

public final class Sender implements Runnable {
    // Allow resource frames up to 16 MiB while bounding each slow client's memory.
    private static final int MAX_MESSAGES = positiveInt("nro.sender.maxMessages", 4096);
    private static final long MAX_BYTES = positiveLong("nro.sender.maxBytes", 32L * 1024 * 1024);
    private static final long WRITE_TIMEOUT = positiveLong("nro.sender.writeTimeoutMillis", 30000);
    private static final long MAX_QUEUE_AGE = positiveLong("nro.sender.maxQueueAgeMillis", 60000);
    private static final ScheduledThreadPoolExecutor WATCHDOG = createWatchdog();
    private static final Logger LOG = Logger.getLogger(Sender.class.getName());
    private final ISession session;
    private final Object queueLock = new Object();
    private final ArrayDeque<Pending> messages = new ArrayDeque<>();
    private volatile Socket socket;
    private volatile DataOutputStream dos;
    private volatile IMessageSendCollect sendCollect;
    private volatile boolean closed;
    private long queuedBytes;
    private ScheduledFuture<?> queueTimeout;

    public Sender(ISession session, Socket socket) {
        if (session == null || socket == null) throw new NullPointerException("session/socket");
        this.session = session;
        setSocket(socket);
    }
    public Sender setSocket(Socket socket) {
        if (socket == null) throw new NullPointerException("socket");
        this.socket = socket;
        try { this.dos = new DataOutputStream(socket.getOutputStream()); }
        catch (IOException e) { failTransport("output unavailable"); }
        return this;
    }
    @Override public void run() {
        try {
            while (!closed && session.isConnected()) {
                Pending pending;
                synchronized (queueLock) {
                    while (messages.isEmpty() && !closed && session.isConnected()) queueLock.wait();
                    if (closed || !session.isConnected()) break;
                    pending = messages.removeFirst();
                    queuedBytes -= pending.bytes;
                    scheduleQueueTimeout();
                }
                try { doSendMessage(pending.message); }
                finally { pending.message.cleanup(); }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            failTransport("send failed: " + e.getClass().getSimpleName());
        } finally { close(); }
    }
    // Serialize handshake and queued writes to preserve cipher position.
    public synchronized void doSendMessage(Message message) throws Exception {
        if (closed) throw new IOException("Sender closed");
        ScheduledFuture<?> timeout = WATCHDOG.schedule(() -> failTransport("write timed out"), WRITE_TIMEOUT, TimeUnit.MILLISECONDS);
        try { sendCollect.doSendMessage(session, dos, message); }
        catch (Exception e) { failTransport("send failed: " + e.getClass().getSimpleName()); throw e; }
        finally { timeout.cancel(false); }
    }
    public void sendMessage(Message message) {
        if (message == null || closed || !session.isConnected()) return;
        byte[] data;
        try { data = message.getData(); }
        catch (RuntimeException e) { failTransport("invalid outgoing message"); return; }
        long bytes = (long) data.length + 4;
        boolean overflow;
        synchronized (queueLock) {
            if (closed || !session.isConnected()) return;
            overflow = messages.size() >= MAX_MESSAGES || bytes > MAX_BYTES - queuedBytes;
            if (!overflow) {
                // Broadcast callers may mutate/dispose their Message after enqueueing.
                Message snapshot = new Message(message.command);
                try { snapshot.write(data); }
                catch (IOException e) { throw new IllegalStateException(e); }
                messages.addLast(new Pending(snapshot, bytes));
                queuedBytes += bytes;
                if (messages.size() == 1) scheduleQueueTimeout();
                queueLock.notifyAll();
            }
        }
        if (overflow) failTransport("outgoing queue limit exceeded");
    }
    private void scheduleQueueTimeout() {
        if (queueTimeout != null) queueTimeout.cancel(false);
        Pending oldest = messages.peekFirst();
        queueTimeout = oldest == null ? null : WATCHDOG.schedule(this::checkQueueAge,
                Math.max(1, MAX_QUEUE_AGE - TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - oldest.enqueued)), TimeUnit.MILLISECONDS);
    }
    private void checkQueueAge() {
        boolean expired;
        synchronized (queueLock) {
            Pending oldest = messages.peekFirst();
            expired = !closed && oldest != null && System.nanoTime() - oldest.enqueued >= TimeUnit.MILLISECONDS.toNanos(MAX_QUEUE_AGE);
            if (!expired && !closed) scheduleQueueTimeout();
        }
        if (expired) failTransport("outgoing queue aged out");
    }
    private void failTransport(String reason) {
        if (closeTransport()) LOG.warning("Session " + session.getID() + ": " + reason);
        // Socket close wakes Collector, which owns normal logout/save. Never save here.
    }
    public void setSend(IMessageSendCollect sendCollect) { this.sendCollect = sendCollect; }
    public int getNumMessages() { synchronized (queueLock) { return messages.size(); } }
    public void close() { closeTransport(); }
    private boolean closeTransport() {
        synchronized (queueLock) {
            if (closed) return false;
            closed = true;
            messages.clear(); queuedBytes = 0;
            if (queueTimeout != null) queueTimeout.cancel(false);
            queueLock.notifyAll();
        }
        // Never take the write monitor or close the output stream before the socket.
        try { socket.close(); } catch (IOException ignored) { }
        return true;
    }
    public void dispose() { close(); }
    private static ScheduledThreadPoolExecutor createWatchdog() {
        ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "Sender-watchdog"); thread.setDaemon(true); return thread;
        });
        executor.setRemoveOnCancelPolicy(true);
        return executor;
    }
    private static long positiveLong(String name, long fallback) {
        long value = Long.getLong(name, fallback); return value > 0 ? value : fallback;
    }
    private static int positiveInt(String name, int fallback) {
        int value = Integer.getInteger(name, fallback); return value > 0 ? value : fallback;
    }
    private static final class Pending {
        final Message message; final long bytes; final long enqueued = System.nanoTime();
        Pending(Message message, long bytes) { this.message = message; this.bytes = bytes; }
    }
}
