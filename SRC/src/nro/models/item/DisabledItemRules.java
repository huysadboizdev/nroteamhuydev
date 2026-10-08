package nro.models.item;

/** Temporarily unavailable features; catalog entries remain for safe legacy restoration. */
public final class DisabledItemRules {
    private DisabledItemRules() {}
    public static boolean disabled(int id) { return id == 1015; }
    public static void requireEnabled(int id) {
        if (disabled(id)) throw new IllegalArgumentException("Item temporarily disabled: " + id);
    }
}
