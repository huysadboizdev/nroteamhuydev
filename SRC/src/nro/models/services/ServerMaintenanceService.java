package nro.models.services;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.*;

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
    private static final class Holder {
        static final ServerMaintenanceService INSTANCE = new ServerMaintenanceService(
                new ServerMaintenanceDAO(nro.models.data.LocalManager::getConnection),
                ServerMaintenanceRuntime::drain);
    }
    private final Store store;
    private final Drain drain;
    private final Duration drainTimeout;
    private final String run = UUID.randomUUID().toString();
    private ScheduledExecutorService worker;
    private ScheduledExecutorService heartbeatWorker;
    private volatile State state;
    private volatile long lastError;

    public ServerMaintenanceService(Store store, Drain drain) {
        this(store, drain, Duration.ofSeconds(30));
    }
    ServerMaintenanceService(Store store, Drain drain, Duration drainTimeout) {
        if (drainTimeout == null || drainTimeout.toMillis() <= 0) throw new IllegalArgumentException("Positive drain timeout required");
        this.store = store;
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
    }
    private void publish(State value) {
        state = value;
        if ("OPEN".equals(value.phase())) ServerMaintenanceGate.resume();
        else ServerMaintenanceGate.pause();
    }
    public synchronized void tick() throws Exception {
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
        ServerMaintenanceGate.Freeze barrier;
        try {
            barrier = ServerMaintenanceGate.freeze(drainTimeout);
        } catch (ServerMaintenanceGate.DrainTimeoutException waiting) {
            // Nothing has been saved/disconnected yet. Keep the durable intent and
            // admission closed; the next coordinator tick retries this command.
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
