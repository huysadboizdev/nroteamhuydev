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
        } finally { thread.shutdownNow(); lease.close(); resume.invoke(null); }
        System.out.println("PASS gameplay admission, drain barrier, reopen");
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
