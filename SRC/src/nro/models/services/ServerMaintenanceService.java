package nro.models.services;

import java.time.Duration;
import java.net.SocketException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;
import nro.models.database.PlayerDAO;
import nro.models.data.LocalManager;
import nro.models.player.Player;
import nro.models.server.Client;

/** Independent control worker. Never stops or waits for its own command executor. */
public final class ServerMaintenanceService {
    public record State(String phase, long revision, long commandId, String message) {}
    public record Intent(long id, String type) {}
    public interface Store {
        State load(String run) throws Exception;
        void heartbeat(String run) throws Exception;
        Intent next() throws Exception;
        State accept(Intent intent, String run) throws Exception;
        State finish(State expected, String phase, String message, String run) throws Exception;
    }
    @FunctionalInterface public interface Drain { void run() throws Exception; }
    @FunctionalInterface public interface Prepare { void run() throws Exception; }
    private static final class Holder {
        static final ServerMaintenanceService INSTANCE = new ServerMaintenanceService(
                new ServerMaintenanceDAO(nro.models.data.LocalManager::getConnection),
                ServerMaintenanceRuntime::disconnectSessions,
                ServerMaintenanceRuntime::drain);
    }
    private final Store store;
    private final Prepare prepare;
    private final Drain drain;
    private final Duration drainTimeout;
    private final String run = UUID.randomUUID().toString();
    private ScheduledExecutorService worker;
    private ScheduledExecutorService heartbeatWorker;
    private ScheduledExecutorService checkpointWorker;
    private volatile State state;
    private volatile long lastError;
    private volatile boolean nativeMaintenance;
    private final Object maintenanceModeLock = new Object();
    private volatile String lastCheckpointSummary = "no checkpoint yet";
    private final Map<Long, CheckpointRecord> checkpoints = new ConcurrentHashMap<>();

    private record CheckpointRecord(String name, long savedAt, String issue) {}

    public ServerMaintenanceService(Store store, Drain drain) {
        this(store, () -> {}, drain, Duration.ofSeconds(30));
    }
    ServerMaintenanceService(Store store, Drain drain, Duration drainTimeout) {
        this(store, () -> {}, drain, drainTimeout);
    }
    public ServerMaintenanceService(Store store, Prepare prepare, Drain drain) {
        this(store, prepare, drain, Duration.ofSeconds(30));
    }
    ServerMaintenanceService(Store store, Prepare prepare, Drain drain, Duration drainTimeout) {
        if (drainTimeout == null || drainTimeout.toMillis() <= 0) throw new IllegalArgumentException("Positive drain timeout required");
        this.store = store;
        this.prepare = prepare;
        this.drain = drain;
        this.drainTimeout = drainTimeout;
    }
    public static ServerMaintenanceService gI() { return Holder.INSTANCE; }
    public static boolean gameplayPaused() { return ServerMaintenanceGate.paused(); }
    public State state() { return state; }
    public static boolean supports(String type) { return "MAINTENANCE_START".equals(type) || "MAINTENANCE_OPEN".equals(type); }

    /** Required migration must be present before the socket is opened. */
    public synchronized void initialize() throws Exception {
        ServerMaintenanceGate.pause();
        publish(store.load(run));
    }
    public synchronized void start() {
        if (worker != null) return;
        worker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Web maintenance coordinator"); t.setDaemon(true); return t;
        });
        worker.scheduleWithFixedDelay(this::tickSafely, 0, 1000, TimeUnit.MILLISECONDS);
        heartbeatWorker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Web maintenance heartbeat"); t.setDaemon(true); return t;
        });
        heartbeatWorker.scheduleWithFixedDelay(this::heartbeatSafely, 0, 5, TimeUnit.SECONDS);
        checkpointWorker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Player persistence checkpoint"); t.setDaemon(true); return t;
        });
        checkpointWorker.scheduleWithFixedDelay(this::checkpointSafely, 30, 30, TimeUnit.SECONDS);
    }
    private void publish(State value) {
        synchronized (maintenanceModeLock) {
            state = value;
            if ("OPEN".equals(value.phase()) && !nativeMaintenance) ServerMaintenanceGate.resume();
            else ServerMaintenanceGate.pause();
        }
    }
    public synchronized void tick() throws Exception {
        if (nativeMaintenance) {
            ServerMaintenanceGate.pause();
            return;
        }
        publish(store.load(run));
        if ("DRAINING".equals(state.phase())) { drainCurrent(); return; }
        Intent intent = store.next();
        if (intent == null) return;
        // A lost COMMIT acknowledgement must never leave admissions open.
        if ("MAINTENANCE_START".equals(intent.type())) ServerMaintenanceGate.pause();
        publish(store.accept(intent, run));
        if ("DRAINING".equals(state.phase())) drainCurrent();
    }
    private void drainCurrent() throws Exception {
        State draining = state;
        // Close client transports before waiting for already admitted gameplay to drain.
        // The prepare action must be safe to retry when the barrier is busy.
        prepare.run();
        ServerMaintenanceGate.Freeze barrier;
        try {
            barrier = ServerMaintenanceGate.freeze(drainTimeout);
        } catch (ServerMaintenanceGate.DrainTimeoutException waiting) {
            // Player state is still untouched. Keep the durable intent and admission
            // closed; the next coordinator tick retries after clients were disconnected.
            System.err.println("[Maintenance] Waiting for gameplay: phase=DRAINING, commandId="
                    + draining.commandId() + ", cause=" + waiting.getMessage());
            return;
        }
        try (barrier) {
            drain.run();
            publish(store.finish(draining, "MAINTENANCE", "Đã lưu dữ liệu và ngắt toàn bộ người chơi", run));
        } catch (Exception failure) {
            // CAS in the DAO protects a successfully committed terminal state from an uncertain acknowledgement.
            try { publish(store.finish(draining, "FAILED", "Chưa hoàn tất lưu/ngắt; dữ liệu được giữ lại. Thử bảo trì lại.", run)); }
            catch (Exception statusFailure) { failure.addSuppressed(statusFailure); }
            throw failure;
        }
    }

    /** Native countdown/CLI maintenance uses the same admission barrier and drain as web maintenance. */
    public void beginNativeMaintenance() throws Exception {
        synchronized (maintenanceModeLock) {
            nativeMaintenance = true;
            ServerMaintenanceGate.pause();
        }
        synchronized (this) {
            prepare.run();
        }
    }

    public void drainForNativeMaintenance() throws Exception {
        beginNativeMaintenance();
        try (ServerMaintenanceGate.Freeze ignored = ServerMaintenanceGate.freeze(drainTimeout)) {
            drain.run();
        }
    }

    private boolean checkpointAllowed() {
        State current = state;
        return !nativeMaintenance && current != null && "OPEN".equals(current.phase())
                && !ServerMaintenanceGate.paused();
    }

    private void checkpointSafely() {
        if (!checkpointAllowed()) return;
        long started = System.nanoTime();
        if (!checkpointDatabaseAvailable()) {
            recordDatabaseUnavailable();
            return;
        }
        ServerMaintenanceGate.Freeze barrier = null;
        try {
            barrier = ServerMaintenanceGate.tryCheckpointFreeze(Duration.ofMillis(250));
            if (barrier == null) {
                lastCheckpointSummary = "deferred: gameplay barrier busy; prior checkpoint retained";
                System.out.println("[Checkpoint] deferred: gameplay barrier busy");
                return;
            }
            if (!checkpointAllowed()) return;
            int saved = 0, skipped = 0, failed = 0;
            Set<Long> onlineIds = new HashSet<>();
            boolean databaseUnavailable = false;
            for (Player player : new ArrayList<>(Client.gI().getPlayers())) {
                if (player == null) continue;
                onlineIds.add(player.id);
                String issue;
                if (databaseUnavailable) {
                    CheckpointRecord previous = checkpoints.get(player.id);
                    checkpoints.put(player.id, new CheckpointRecord(player.name,
                            previous == null ? 0 : previous.savedAt(), "database-unavailable"));
                    failed++;
                    continue;
                }
                try {
                    synchronized (player) {
                        if (player.beforeDispose || player.isOffline || !player.isPl()
                                || player.inventory == null || player.idMark == null
                                || !player.idMark.isLoadedAllDataPlayer()) {
                            issue = "not-ready";
                        } else if (PlayerDAO.hasPendingLogoutOperation(player)) {
                            issue = "pending-operation";
                        } else {
                            PlayerDAO.CheckedSaveResult result = PlayerDAO.saveCheckedDetailed(player);
                            if (!result.saved()) {
                                issue = result.failure() == null ? "save-not-confirmed" : "save-failed";
                                if (isDatabaseConnectionFailure(result.failure())) databaseUnavailable = true;
                            } else {
                                checkpoints.put(player.id, new CheckpointRecord(player.name,
                                        System.currentTimeMillis(), null));
                                saved++;
                                continue;
                            }
                        }
                    }
                } catch (Exception playerFailure) {
                    issue = "exception:" + playerFailure.getClass().getSimpleName();
                }
                CheckpointRecord previous = checkpoints.get(player.id);
                checkpoints.put(player.id, new CheckpointRecord(player.name,
                        previous == null ? 0 : previous.savedAt(), issue));
                if ("save-not-confirmed".equals(issue) || "save-failed".equals(issue)
                        || "database-unavailable".equals(issue) || issue.startsWith("exception:")) failed++;
                else skipped++;
                if (failed + skipped > 0) {
                    System.err.println("[Checkpoint] player=" + player.name + " issue=" + issue
                            + ", lastSavedAt=" + (previous == null || previous.savedAt() == 0
                                    ? "never" : previous.savedAt()));
                }
            }
            checkpoints.keySet().retainAll(onlineIds);
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            lastCheckpointSummary = "saved=" + saved + ", skipped=" + skipped + ", failed=" + failed
                    + ", duration=" + elapsedMs + "ms";
            System.out.println("[Checkpoint] " + lastCheckpointSummary);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Exception failure) {
            System.err.println("[Checkpoint] Cycle failed: " + failure);
            failure.printStackTrace(System.err);
        } finally {
            if (barrier != null) barrier.close();
        }
    }

    public String checkpointSummary() {
        StringBuilder summary = new StringBuilder(lastCheckpointSummary);
        int count = 0;
        for (CheckpointRecord record : checkpoints.values()) {
            if (record.savedAt() == 0 || record.issue() != null) count++;
        }
        summary.append(", attention=").append(count);
        int remaining = count;
        for (CheckpointRecord record : checkpoints.values()) {
            if (record.savedAt() == 0 || record.issue() != null) {
                String name = record.name() == null ? "?" : record.name();
                if (summary.length() + name.length() + 2 > 220) break;
                summary.append(summary.toString().contains(" [") ? "," : " [").append(name);
                remaining--;
            }
        }
        if (remaining > 0) summary.append(" +").append(remaining).append(" more");
        if (summary.length() > 255) return summary.substring(0, 252) + "...";
        return summary.toString();
    }

    private boolean checkpointDatabaseAvailable() {
        try (Connection connection = LocalManager.getConnection();
             Statement statement = connection.createStatement();
             var result = statement.executeQuery("SELECT 1")) {
            return result.next();
        } catch (Exception failure) {
            System.err.println("[Checkpoint] deferred: database unavailable: " + failure);
            return false;
        }
    }

    private void recordDatabaseUnavailable() {
        lastCheckpointSummary = "deferred: database unavailable";
        Set<Long> onlineIds = new HashSet<>();
        for (Player player : Client.gI().getPlayers()) {
            if (player == null) continue;
            onlineIds.add(player.id);
            CheckpointRecord previous = checkpoints.get(player.id);
            checkpoints.put(player.id, new CheckpointRecord(player.name,
                    previous == null ? 0 : previous.savedAt(), "database-unavailable"));
        }
        checkpoints.keySet().retainAll(onlineIds);
    }

    private static boolean isDatabaseConnectionFailure(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current instanceof SocketException) return true;
            if (current instanceof SQLException sql) {
                String state = sql.getSQLState();
                if (state != null && state.startsWith("08")) return true;
            }
        }
        return false;
    }
    private void tickSafely() {
        try { tick(); }
        catch (Exception failure) {
            long now = System.currentTimeMillis();
            if (now - lastError > 10000) {
                lastError = now;
                State current = state;
                String phase = current == null ? "UNKNOWN" : current.phase();
                long commandId = current == null ? 0 : current.commandId();
                System.err.println("[Maintenance] Control unavailable: phase=" + phase
                        + ", commandId=" + commandId + ", cause=" + failure);
                failure.printStackTrace(System.err);
            }
        }
    }
    void heartbeatSafely() {
        try { store.heartbeat(run); }
        catch (Exception failure) {
            long now = System.currentTimeMillis();
            if (now - lastError > 10000) {
                lastError = now;
                System.err.println("[Maintenance] Heartbeat unavailable: " + failure);
                failure.printStackTrace(System.err);
            }
        }
    }
}
