package nro.models.boss.Boss_mini;

/** Unique wire identities for repeated mini bosses; their Java class identifies the type. */
public final class MiniBossIdentity {
    // Ordinary boss IDs and randomBossId occupy [-1,000,000, -1].
    // Clone IDs are below -100,000,000, so do not allocate in that range.
    private static int next = -2_000_000;

    private MiniBossIdentity() { }

    public static synchronized int nextId() {
        if (next <= -100_000_000) {
            throw new IllegalStateException("Mini boss identity range exhausted");
        }
        return next--;
    }
}
