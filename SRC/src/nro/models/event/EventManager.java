package nro.models.event;
import nro.models.event_list.Default;
/** Seasonal state comes exclusively from the durable event policy. */
public final class EventManager {
    private static final EventManager INSTANCE = new EventManager();
    @Deprecated public static volatile boolean LUNNAR_NEW_YEAR, INTERNATIONAL_WOMANS_DAY, CHRISTMAS,
        HALLOWEEN, HUNG_VUONG, TRUNG_THU, TOP_UP;
    private boolean initialized;
    public static EventManager gI() { return INSTANCE; }
    public synchronized void init() {
        if (initialized) return;
        new Default().init();
        initialized = true;
    }
}
