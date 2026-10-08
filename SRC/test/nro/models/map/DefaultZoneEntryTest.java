package nro.models.map;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicReference;
import nro.models.map.service.MapService;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.services.HirudegarnEventService;
import nro.models.services.PinkFixture;

public final class DefaultZoneEntryTest {
    private static Map map(int id, int count) throws Exception {
        Map map = PinkFixture.blank(PinkFixture.Terrain.class);
        map.mapId = id;
        map.zones = new ArrayList<>();
        for (int i = 0; i < count; i++) map.zones.add(new Zone(map, i, 2));
        Manager.MAPS.add(map);
        return map;
    }
    private static void fill(Zone zone) throws Exception {
        for (int i = 0; i < zone.maxPlayer; i++) zone.players.add(PinkFixture.player(i));
    }
    private static void ordered(Map map) {
        for (int i = 0; i < 256; i++)
            PinkFixture.check(MapService.gI().getZone(map.mapId) == map.zones.get(0), "Default entry chooses zone 0");
    }
    private static void run(String mode) throws Exception {
        PinkFixture.install();
        if (mode.equals("ordinary")) {
            ordered(map(5, 3));
            Player player = PinkFixture.player(10);
            PinkFixture.check(MapService.gI().getMapCanJoin(player, 5, -1).zoneId == 0, "Waypoint default selects 0");
        } else if (mode.equals("overflow")) {
            Map map = map(5, 4); fill(map.zones.get(0));
            for (int i = 0; i < 256; i++)
                PinkFixture.check(MapService.gI().getZone(5) == map.zones.get(1), "Full zone 0 falls back to first available zone");
            fill(map.zones.get(1));
            PinkFixture.check(MapService.gI().getZone(5) == map.zones.get(2), "Full zones 0 and 1 fall back to zone 2");
        } else if (mode.equals("full")) {
            Map map = map(5, 3); for (Zone zone : map.zones) fill(zone);
            AtomicReference<Zone> result = new AtomicReference<>();
            Thread worker = new Thread(() -> result.set(MapService.gI().getZone(5)));
            worker.setDaemon(true); worker.start(); worker.join(1000);
            PinkFixture.check(!worker.isAlive(), "All-full selection terminates instead of looping");
            PinkFixture.check(result.get() == null, "All-full selection returns no destination");
        } else if (mode.equals("empty")) {
            map(5, 0);
            PinkFixture.check(MapService.gI().getZone(5) == null, "Empty map has no destination");
            PinkFixture.check(MapService.gI().getZone(9999) == null, "Missing map has no destination");
        } else if (mode.equals("black")) {
            Map map = map(85, 3); ordered(map);
            map.zones.get(0).finishBlackBallWar = true;
            for (int i = 0; i < 256; i++)
                PinkFixture.check(MapService.gI().getZone(85) == map.zones.get(1), "Finished black-ball zone is skipped in order");
            fill(map.zones.get(1)); fill(map.zones.get(2));
            PinkFixture.check(MapService.gI().getZone(85) == null, "Finished or full black-ball zones reject entry");
        } else if (mode.equals("hirudegarn")) {
            Map map = map(126, 3);
            HirudegarnEventService.gI().injectClock(Clock.fixed(Instant.parse("2026-10-06T08:00:00Z"), ZoneOffset.UTC));
            ordered(map); fill(map.zones.get(0));
            for (int i = 0; i < 256; i++)
                PinkFixture.check(MapService.gI().getZone(126) == map.zones.get(1), "Open Tapion uses first available zone");
            HirudegarnEventService.gI().injectClock(Clock.fixed(Instant.parse("2026-10-06T09:00:00Z"), ZoneOffset.UTC));
            PinkFixture.check(MapService.gI().getZone(126) == null, "Closed Tapion rejects entry");
        } else if (mode.equals("fallback")) {
            for (int id : new int[]{21, 5, 48}) {
                Map destination = map(id, 2);
                for (Zone zone : destination.zones) fill(zone);
            }
            Player player = PinkFixture.player(10);
            for (int id : new int[]{53, 135, 147, 141}) {
                map(id, 1);
                PinkFixture.check(MapService.gI().getMapCanJoin(player, id, -1) == null,
                        "Full safe-return map rejects dungeon fallback " + id + " without dereferencing null");
                PinkFixture.check(player.zone == null && player.location == null,
                        "Rejected fallback does not mutate player state");
            }
        } else if (mode.equals("explicit")) {
            Map map = map(5, 3); Player player = PinkFixture.player(10);
            PinkFixture.check(MapService.gI().getMapCanJoin(player, 5, 2) == map.zones.get(2), "Explicit zone selection remains explicit");
            map(21, 3);
            PinkFixture.check(MapService.gI().getMapCanJoin(player, 21, -1).zoneId == 0, "Offline map uses owner zone 0");
        } else throw new AssertionError("Unknown mode " + mode);
    }
    public static void main(String[] args) {
        try { run(args[0]); System.out.println("DEFAULT_ZONE_ENTRY PASS " + args[0]); System.exit(0); }
        catch (Throwable failure) { failure.printStackTrace(); System.exit(1); }
    }
}
