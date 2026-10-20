package nro.models.utils;

import java.util.function.BooleanSupplier;

/** Owns one finite worker thread; no executor remains parked after the loop exits. */
public final class OwnedWorker {
    private Thread thread;

    public synchronized void start(String name, Runnable loop, BooleanSupplier active) {
        if (thread != null || !active.getAsBoolean()) return;
        thread = new Thread(() -> {
            try {
                loop.run();
            } finally {
                synchronized (OwnedWorker.this) {
                    thread = null;
                    // An instance can reopen between its loop's final condition and this cleanup.
                    if (!Thread.currentThread().isInterrupted() && active.getAsBoolean()) start(name, loop, active);
                }
            }
        }, name);
        thread.start();
    }
}
