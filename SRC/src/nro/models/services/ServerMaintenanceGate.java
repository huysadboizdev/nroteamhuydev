package nro.models.services;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Admission closes first; the coordinator then waits for already admitted work. */
public final class ServerMaintenanceGate {
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock(true);
    private static volatile boolean paused;
    private ServerMaintenanceGate() {}
    public static boolean paused() { return paused; }
    public static void pause() { paused = true; }
    public static void resume() { paused = false; }

    public static Lease enter() {
        // Reentrant calls belong to an already admitted operation and must finish.
        if (LOCK.getReadHoldCount() > 0 || LOCK.isWriteLockedByCurrentThread()) return new Lease(false, true);
        if (paused || !LOCK.readLock().tryLock()) return new Lease(false, false);
        if (paused) { LOCK.readLock().unlock(); return new Lease(false, false); }
        return new Lease(true, true);
    }

    public static Freeze freeze(Duration timeout) throws InterruptedException {
        pause();
        if (!LOCK.writeLock().tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS))
            throw new IllegalStateException("Gameplay has not drained; data retained, retry maintenance");
        return new Freeze();
    }

    public static final class Lease implements AutoCloseable {
        private boolean locked;
        private final boolean allowed;
        private Lease(boolean locked, boolean allowed) { this.locked = locked; this.allowed = allowed; }
        public boolean allowed() { return allowed; }
        @Override public void close() { if (locked) { locked = false; LOCK.readLock().unlock(); } }
    }
    public static final class Freeze implements AutoCloseable {
        private boolean locked = true;
        private Freeze() {}
        @Override public void close() { if (locked) { locked = false; LOCK.writeLock().unlock(); } }
    }
}
