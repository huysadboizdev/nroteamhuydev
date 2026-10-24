package nro.models.services;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/** Admission closes first; the coordinator then waits for already admitted work. */
public final class ServerMaintenanceGate {
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock(true);
    private static final ConcurrentHashMap<Thread, Long> HOLDERS = new ConcurrentHashMap<>();
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
        HOLDERS.put(Thread.currentThread(), System.nanoTime());
        return new Lease(true, true);
    }

    public static Freeze freeze(Duration timeout) throws InterruptedException {
        pause();
        if (!LOCK.writeLock().tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS))
            throw new DrainTimeoutException("Gameplay has not drained; data retained, retry maintenance; " + describeHolders());
        return new Freeze();
    }

    /** Takes a consistent checkpoint without closing normal admission. */
    public static Freeze tryCheckpointFreeze(Duration timeout) throws InterruptedException {
        if (paused || !LOCK.writeLock().tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS)) return null;
        if (paused) {
            LOCK.writeLock().unlock();
            return null;
        }
        return new Freeze();
    }

    /** Only failure to acquire the barrier is retryable; save errors remain terminal. */
    public static final class DrainTimeoutException extends IllegalStateException {
        private DrainTimeoutException(String message) { super(message); }
    }

    private static String describeHolders() {
        StringBuilder detail = new StringBuilder("activeGameplay=").append(LOCK.getReadLockCount());
        int count = 0;
        for (var holder : HOLDERS.entrySet()) {
            if (count++ == 8) { detail.append("; additional holders omitted"); break; }
            Thread thread = holder.getKey();
            detail.append("; thread=").append(thread.getName()).append("#").append(thread.getId())
                    .append(" state=").append(thread.getState()).append(" heldMs=")
                    .append(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - holder.getValue()));
            StackTraceElement[] stack = thread.getStackTrace();
            for (int i = 0; i < Math.min(stack.length, 12); i++) detail.append("\n    at ").append(stack[i]);
        }
        return detail.toString();
    }

    public static final class Lease implements AutoCloseable {
        private boolean locked;
        private final boolean allowed;
        private Lease(boolean locked, boolean allowed) { this.locked = locked; this.allowed = allowed; }
        public boolean allowed() { return allowed; }
        @Override public void close() {
            if (locked) {
                locked = false;
                HOLDERS.remove(Thread.currentThread());
                LOCK.readLock().unlock();
            }
        }
    }
    public static final class Freeze implements AutoCloseable {
        private boolean locked = true;
        private Freeze() {}
        @Override public void close() { if (locked) { locked = false; LOCK.writeLock().unlock(); } }
    }
}
