package nro.models.services_dungeon;

/** Guards only instance reservation and publication; never held during movement. */
public final class DungeonAllocationLock {
    public static final Object LOCK = new Object();
    private DungeonAllocationLock() {}
}
