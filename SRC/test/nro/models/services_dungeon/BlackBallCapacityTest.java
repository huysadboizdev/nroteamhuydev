package nro.models.services_dungeon;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import nro.models.consts.ConstMap;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.player.Player;
import nro.models.server.Maintenance;
import nro.models.server.Manager;
import nro.models.services.PinkFixture;

public final class BlackBallCapacityTest {
    private static Zone zone(int mapId) throws Exception {
        var map = PinkFixture.blank(PinkFixture.Terrain.class); map.mapId = mapId; map.mapWidth = 240;
        return new Zone(map, 0, 50);
    }
    private static final class DroppingZone extends Zone {
        final CountDownLatch gate;
        DroppingZone(nro.models.map.Map map, CountDownLatch gate) { super(map, 0, 50); this.gate = gate; }
        @Override public void addItem(nro.models.map.ItemMap item) {
            gate.countDown();
            try { PinkFixture.check(gate.await(2, java.util.concurrent.TimeUnit.SECONDS), "Both held balls reach their original zone"); }
            catch (InterruptedException e) { throw new RuntimeException(e); }
            super.addItem(item);
        }
    }
    private static Player entrant(long id, Zone origin) throws Exception {
        Player p = new Player() { @Override public void sendMessage(nro.models.network.Message m) {} };
        p.id = id; p.isPlayer = true; p.idNRNM = -1; p.zone = origin;
        if (origin != null) origin.addPlayer(p);
        return p;
    }
    private static void fill(Zone z, int count) throws Exception {
        for (int i = 0; i < count; i++) z.addPlayer(entrant(1_000 + i, null));
    }
    private static void run(String mode) throws Exception {
        PinkFixture.install();
        if (mode.equals("zones")) {
            Maintenance.isRunning = true;
            Method init = nro.models.map.Map.class.getDeclaredMethod("initZone", int.class, int.class);
            init.setAccessible(true);
            for (int id = 85; id <= 91; id++) {
                var map = PinkFixture.blank(nro.models.map.Map.class);
                map.mapId = id; map.type = ConstMap.MAP_BLACK_BALL_WAR; map.zones = new ArrayList<>();
                init.invoke(map, 10, 15);
                PinkFixture.check(map.zones.size() == (id <= 87 ? 2 : 3), "Zone count map " + id);
                for (Zone z : map.zones) PinkFixture.check(z.maxPlayer == 50, "Capacity map " + id);
            }
        } else if (mode.equals("full")) {
            Zone target = zone(85), origin = zone(5); fill(target, 49);
            Player fiftieth = entrant(50, origin), extra = entrant(51, origin);
            PinkFixture.check(ChangeMapService.gI().goToMap(fiftieth, target), "50th accepted");
            PinkFixture.check(!ChangeMapService.gI().goToMap(extra, target) && extra.zone == origin && origin.getPlayers().contains(extra),
                    "51st rejected before leaving original zone");
            MapService.gI().goToMap(extra, target);
            PinkFixture.check(extra.zone == origin && target.getNumOfPlayers() == 50, "Alternate map entry cannot bypass capacity");
            PinkFixture.check(ChangeMapService.gI().goToMap(fiftieth, target) && target.getNumOfPlayers() == 50, "Same-zone refresh remains valid");
            var boss = entrant(99, null); boss.isBoss = true;
            PinkFixture.check(ChangeMapService.gI().goToMap(boss, target) && target.getNumOfPlayers() == 50, "Boss does not consume player slot");
            var pet = entrant(100, null); pet.isPet = true;
            PinkFixture.check(ChangeMapService.gI().goToMap(pet, target) && target.getNumOfPlayers() == 50, "Pet does not consume player slot");
        } else if (mode.equals("race")) {
            for (int round = 0; round < 40; round++) {
                Zone target = zone(85); fill(target, 49);
                Player a = entrant(50, null), b = entrant(51, null);
                CountDownLatch gate = new CountDownLatch(1);
                AtomicInteger accepted = new AtomicInteger(); AtomicReference<Throwable> failure = new AtomicReference<>();
                Thread[] threads = new Thread[2]; int i = 0;
                for (Player p : new Player[]{a, b}) {
                    threads[i++] = new Thread(() -> { try { gate.await(); if (ChangeMapService.gI().goToMap(p, target)) accepted.incrementAndGet(); }
                        catch (Throwable t) { failure.set(t); } });
                }
                for (Thread t : threads) t.start(); gate.countDown();
                for (Thread t : threads) { t.join(2_000); PinkFixture.check(!t.isAlive(), "Entry completes without deadlock"); }
                if (failure.get() != null) throw new AssertionError(failure.get());
                PinkFixture.check(accepted.get() == 1 && target.getNumOfPlayers() == 50, "Concurrent entry retains exact capacity");
            }
        } else if (mode.equals("crossing")) {
            CountDownLatch dropGate = new CountDownLatch(2);
            Zone left = new DroppingZone(zone(85).map, dropGate), right = new DroppingZone(zone(86).map, dropGate);
            Player a = entrant(51, left), b = entrant(52, right);
            a.idMark.setHoldBlackBall(true); a.idMark.setTempIdBlackBallHold(372);
            b.idMark.setHoldBlackBall(true); b.idMark.setTempIdBlackBallHold(373);
            AtomicReference<Throwable> failure = new AtomicReference<>();
            Thread ta = new Thread(() -> { try { PinkFixture.check(ChangeMapService.gI().goToMap(a,right), "Left enters right"); } catch(Throwable t){failure.set(t);} });
            Thread tb = new Thread(() -> { try { PinkFixture.check(ChangeMapService.gI().goToMap(b,left), "Right enters left"); } catch(Throwable t){failure.set(t);} });
            ta.setDaemon(true); tb.setDaemon(true); ta.start(); tb.start(); ta.join(3_000); tb.join(3_000);
            PinkFixture.check(!ta.isAlive() && !tb.isAlive(), "Opposite held-ball moves must not deadlock");
            if(failure.get()!=null) throw new AssertionError(failure.get());
            PinkFixture.check(a.zone==right && b.zone==left && left.items.size()==1 && right.items.size()==1,
                    "Each ball drops in its original zone");
        } else if (mode.equals("selection")) {
            Zone target = zone(85); fill(target, 50);
            target.map.zones = new ArrayList<>(); target.map.zones.add(target);
            Manager.MAPS.add(target.map);
            PinkFixture.check(MapService.gI().getZone(85) == null, "All full returns immediately");
            Zone available = new Zone(target.map, 1, 50); target.map.zones.add(available);
            PinkFixture.check(MapService.gI().getZone(85) == available, "Select remaining available zone");
            PinkFixture.check(MapService.gI().getZoneByMapIDAndZoneID(85, 2) == null, "Invalid zone cannot be selected");
        } else throw new AssertionError("Unknown mode");
    }
    public static void main(String[] args) {
        try { run(args[0]); System.out.println("BLACK_BALL_CAPACITY PASS " + args[0]); System.exit(0); }
        catch (Throwable t) { t.printStackTrace(); System.exit(1); }
    }
}
