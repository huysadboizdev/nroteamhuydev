package nro.models.services;

import java.lang.reflect.*;
import java.time.Duration;
import java.util.concurrent.*;

/** Runs without a database or a running server. */
public final class ServerMaintenanceGateTest {
    public static void main(String[] args) throws Exception {
        Class<?> gate;
        try { gate = Class.forName("nro.models.services.ServerMaintenanceGate"); }
        catch (ClassNotFoundException e) { throw new AssertionError("Missing gameplay drain barrier", e); }
        Method enter = gate.getMethod("enter"), pause = gate.getMethod("pause"), resume = gate.getMethod("resume");
        Method freeze = gate.getMethod("freeze", Duration.class);
        AutoCloseable lease = (AutoCloseable) enter.invoke(null);
        check((boolean) lease.getClass().getMethod("allowed").invoke(lease), "open admission");
        pause.invoke(null);
        ExecutorService thread = Executors.newSingleThreadExecutor();
        Future<Boolean> drained = thread.submit(() -> {
            try (AutoCloseable ignored = (AutoCloseable) freeze.invoke(null, Duration.ofSeconds(2))) { return true; }
        });
        try {
            Thread.sleep(50);
            check(!drained.isDone(), "drain must wait for active packet/tick");
            lease.close();
            check(drained.get(2, TimeUnit.SECONDS), "active work drained");
            try (AutoCloseable denied = (AutoCloseable) enter.invoke(null)) {
                check(!(boolean) denied.getClass().getMethod("allowed").invoke(denied), "new work blocked");
            }
            resume.invoke(null);
            try (AutoCloseable admitted = (AutoCloseable) enter.invoke(null)) {
                check((boolean) admitted.getClass().getMethod("allowed").invoke(admitted), "reopen admission");
            }
            CountDownLatch holding = new CountDownLatch(1), release = new CountDownLatch(1);
            Future<?> packet = thread.submit(() -> {
                Thread.currentThread().setName("maintenance-blocking-packet");
                try (AutoCloseable ignored = (AutoCloseable) enter.invoke(null)) {
                    holding.countDown();
                    release.await();
                } catch (Exception e) { throw new RuntimeException(e); }
            });
            check(holding.await(2, TimeUnit.SECONDS), "packet admitted");
            try {
                freeze.invoke(null, Duration.ofMillis(10));
                throw new AssertionError("must not freeze before packet finishes");
            } catch (InvocationTargetException e) {
                check(e.getCause().getMessage().contains("maintenance-blocking-packet"),
                        "timeout identifies the gameplay holder");
                check(pausedState(gate), "timeout keeps admission closed");
            } finally { release.countDown(); }
            packet.get(2, TimeUnit.SECONDS);
            try (AutoCloseable ignored = (AutoCloseable) freeze.invoke(null, Duration.ofSeconds(1))) {
                check(pausedState(gate), "retry obtains exclusive barrier after holder exits");
            }
        } finally { thread.shutdownNow(); lease.close(); resume.invoke(null); }
        System.out.println("PASS gameplay admission, drain barrier, reopen");
    }
    private static boolean pausedState(Class<?> gate) throws Exception {
        return (boolean) gate.getMethod("paused").invoke(null);
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
