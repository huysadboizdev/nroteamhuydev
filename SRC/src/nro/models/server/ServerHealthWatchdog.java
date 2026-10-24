package nro.models.server;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import nro.models.utils.Logger;

/** Detects sustained heap pressure or a stalled global map tick and enters the
 * normal save-and-maintenance path before the JVM reaches an unrecoverable state. */
public final class ServerHealthWatchdog {
    private static final long CHECK_INTERVAL_MS = 5_000L;
    private static final long MAP_STALL_MS = 20_000L;
    private static final int REQUIRED_HEAP_CHECKS = 3;
    private static final int HEAP_LIMIT_PERCENT = Integer.getInteger("server.heapThresholdPercent", 85);
    private static final AtomicBoolean STARTED = new AtomicBoolean();
    private static final AtomicBoolean MAINTENANCE_REQUESTED = new AtomicBoolean();
    private static final MemoryMXBean MEMORY = ManagementFactory.getMemoryMXBean();
    private static final ScheduledExecutorService WORKER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "Server health watchdog");
        thread.setDaemon(true);
        return thread;
    });
    private static volatile long lastMapTick = System.currentTimeMillis();
    private static int heapPressureChecks;

    private ServerHealthWatchdog() {}

    public static void start() {
        if (STARTED.compareAndSet(false, true)) {
            lastMapTick = System.currentTimeMillis();
            WORKER.scheduleWithFixedDelay(ServerHealthWatchdog::checkSafely,
                    CHECK_INTERVAL_MS, CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);
        }
    }

    public static void mapTickCompleted() {
        lastMapTick = System.currentTimeMillis();
    }

    private static void checkSafely() {
        try {
            long now = System.currentTimeMillis();
            long used = MEMORY.getHeapMemoryUsage().getUsed();
            long max = MEMORY.getHeapMemoryUsage().getMax();
            if (max > 0 && used >= max * (long) HEAP_LIMIT_PERCENT / 100L) {
                heapPressureChecks++;
            } else {
                heapPressureChecks = 0;
            }
            if (heapPressureChecks >= REQUIRED_HEAP_CHECKS) {
                requestMaintenance("heap pressure sustained: " + percent(used, max)
                        + "% used (" + used / 1_048_576 + "/" + max / 1_048_576 + " MiB)");
                return;
            }
            long mapDelay = now - lastMapTick;
            if (mapDelay >= MAP_STALL_MS) {
                requestMaintenance("global map update stalled for " + mapDelay / 1000 + "s; heap="
                        + percent(used, max) + "%");
            }
        } catch (Exception failure) {
            Logger.logException(ServerHealthWatchdog.class, failure);
        }
    }

    private static int percent(long used, long max) {
        return max <= 0 ? -1 : (int) Math.min(100, used * 100 / max);
    }

    private static void requestMaintenance(String reason) {
        if (!MAINTENANCE_REQUESTED.compareAndSet(false, true)) return;
        Logger.error("Health watchdog requested save-and-restart maintenance: " + reason);
        Thread maintenance = new Thread(() -> {
            try {
                ServerManager.gI().close();
            } catch (Exception failure) {
                MAINTENANCE_REQUESTED.set(false);
                Logger.logException(ServerHealthWatchdog.class, failure,
                        "Health-triggered maintenance did not finish; retrying on next health alarm");
            }
        }, "Health-triggered maintenance");
        maintenance.setDaemon(false);
        maintenance.start();
    }
}
