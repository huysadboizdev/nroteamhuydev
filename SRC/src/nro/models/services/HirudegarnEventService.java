package nro.models.services;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.IdentityHashMap;
import nro.models.map.Map;
import nro.models.map.Zone;
import nro.models.mob.Mob;
import nro.models.mob_bigboss.Hirudegarn;
import nro.models.player.Player;

/** Fixed daily sessions; the gate never waits for a player monitor. */
public final class HirudegarnEventService {
    public static final int MAP_ID = 126;
    public static final int ZONES = 20;
    private static final ZoneId TIME_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final HirudegarnEventService INSTANCE = new HirudegarnEventService();
    public final Object eventGate = new Object();
    private Clock clock;
    private final java.util.Map<Zone, Instant> sessions = new IdentityHashMap<>();

    public HirudegarnEventService() { this(Clock.systemUTC()); }
    public HirudegarnEventService(Clock clock) { this.clock = java.util.Objects.requireNonNull(clock); }
    public static HirudegarnEventService gI() { return INSTANCE; }

    public static Instant sessionEnd(Instant instant) {
        ZonedDateTime local = instant.atZone(TIME_ZONE);
        LocalTime time = local.toLocalTime();
        int endHour = time.compareTo(LocalTime.of(15, 0)) >= 0 && time.compareTo(LocalTime.of(16, 0)) < 0 ? 16
                : time.compareTo(LocalTime.of(22, 0)) >= 0 && time.compareTo(LocalTime.of(23, 0)) < 0 ? 23 : -1;
        return endHour == -1 ? null : local.toLocalDate().atTime(endHour, 0).atZone(TIME_ZONE).toInstant();
    }
    public Instant sessionEnd() { synchronized (eventGate) { return sessionEnd(clock.instant()); } }
    public boolean isOpen() { return sessionEnd() != null; }
    public void injectClock(Clock clock) { synchronized (eventGate) { this.clock = java.util.Objects.requireNonNull(clock); sessions.clear(); } }
    public void restoreClock() { injectClock(Clock.systemUTC()); }

    /** Called under the gate before entry and combat, so a restart or late entry cannot extend a session. */
    public boolean prepareZone(Zone zone) {
        synchronized (eventGate) {
            Instant end = sessionEnd(clock.instant());
            if (end == null) { sessions.remove(zone); return false; }
            if (!end.equals(sessions.get(zone))) {
                for (Mob mob : zone.mobs) if (mob instanceof Hirudegarn boss && !boss.resetEventLife()) {
                    sessions.remove(zone);
                    return false;
                }
                if (!end.equals(sessionEnd(clock.instant()))) {
                    sessions.remove(zone);
                    return false;
                }
                sessions.put(zone, end);
            }
            return true;
        }
    }

    public static int normalizeSavedMap(int mapId) { return mapId == MAP_ID ? 19 : mapId; }

    /** Only event bosses and forced exits use the 100ms cadence. */
    public void tick(Map map) {
        if (map.mapId != MAP_ID) return;
        for (Zone zone : map.zones) {
            try {
                if (prepareZone(zone)) {
                    for (Mob mob : zone.mobs) if (mob instanceof Hirudegarn) {
                        try { mob.update(); }
                        catch (Exception e) { nro.models.utils.Logger.logException(HirudegarnEventService.class, e); }
                    }
                } else {
                    for (Player player : zone.getPlayers()) {
                        if (player == null) continue;
                        try {
                            // Match admission ordering, and recheck after waiting for the character.
                            synchronized (player) {
                                if (!isOpen() && player.zone == zone)
                                    DungeonExitService.exit(player, zone, 19, 1000, 360);
                            }
                        } catch (Exception e) { nro.models.utils.Logger.logException(HirudegarnEventService.class, e); }
                    }
                }
            } catch (Exception e) { nro.models.utils.Logger.logException(HirudegarnEventService.class, e); }
        }
    }
}
