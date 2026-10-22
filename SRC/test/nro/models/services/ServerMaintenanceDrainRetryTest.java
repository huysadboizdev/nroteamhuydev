package nro.models.services;

import java.util.concurrent.*;

public final class ServerMaintenanceDrainRetryTest {
    public static void main(String[] args) throws Exception {
        var store = new ServerMaintenanceTest.MemoryStore();
        store.state = new ServerMaintenanceService.State("DRAINING", 1, 124, "");
        int[] disconnects = {0}, saves = {0};
        var service = new ServerMaintenanceService(store, () -> disconnects[0]++, () -> saves[0]++, java.time.Duration.ofMillis(10));
        ExecutorService worker = Executors.newSingleThreadExecutor();
        CountDownLatch holding = new CountDownLatch(1), release = new CountDownLatch(1);
        ServerMaintenanceGate.resume();
        Future<?> packet = worker.submit(() -> {
            try (var lease = ServerMaintenanceGate.enter()) {
                if (!lease.allowed()) throw new AssertionError("packet admitted");
                holding.countDown();
                release.await();
            } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        });
        try {
            check(holding.await(2, TimeUnit.SECONDS), "packet started");
            service.tick();
            check("DRAINING".equals(store.state.phase()), "busy gameplay stays DRAINING instead of FAILED");
            check(store.state.commandId() == 124 && disconnects[0] == 1 && saves[0] == 0,
                    "disconnect before barrier; no save before exclusive barrier");
            check(ServerMaintenanceGate.paused(), "admission remains closed while retrying");
            release.countDown();
            packet.get(2, TimeUnit.SECONDS);
            service.tick();
            check("MAINTENANCE".equals(store.state.phase()) && disconnects[0] == 2 && saves[0] == 1,
                    "prepare reruns safely and drain completes after gameplay finishes");
            service.tick();
            check(saves[0] == 1, "completed maintenance never saves twice");
        } finally {
            release.countDown();
            worker.shutdownNow();
            ServerMaintenanceGate.resume();
        }
        System.out.println("PASS busy gameplay retries the same maintenance command and saves once");
    }
    private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
}
