package nro.models.services_dungeon;

import java.lang.reflect.Field;
import nro.models.clan.Clan;
import nro.models.map.Zone;
import nro.models.map.service.MapService;
import nro.models.map.phoban.BanDoKhoBau;
import nro.models.map.phoban.DestronGas;
import nro.models.map.phoban.BlackBallWar;
import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.Boss_Manager.GasDestroyManager;
import nro.models.consts.BossStatus;
import nro.models.player.Player;
import sun.misc.Unsafe;

/** Exercises actual finish, retry, and disposal with an unavailable destination. */
public final class DungeonClosingBehaviorTest {
    private static final Unsafe U;
    static {
        try { Field f = Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true); U = (Unsafe) f.get(null); }
        catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }
    private static int cases;
    public static class BrokenPlayer extends Player {
        int attempts;
        @Override public boolean isDie() { attempts++; throw new IllegalStateException("deliberate isolated exit failure"); }
    }
    public static class WaitingPlayer extends Player {
        int attempts;
        @Override public boolean isDie() { attempts++; return false; }
    }
    public static class StageBoss extends Boss {
        int stageCallbacks;
        public StageBoss() throws Exception { super(nro.models.consts.BossType.PHOBANKGHD, 0, new BossData[0]); }
        @Override public boolean isDie() { return true; }
        @Override public void changeStatus(BossStatus status) { bossStatus = status; }
        @Override public void leaveMap() { stageCallbacks++; }
    }
    private static class Treasure extends BanDoKhoBau {
        int awards;
        Treasure() { super(0); }
        @Override public void sendThanhTichBanDoKhoBau(Player player) { awards++; }
    }
    private static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
        cases++; System.out.println("PASS " + name);
    }
    private static boolean closing(BanDoKhoBau dungeon) throws Exception {
        return BanDoKhoBau.class.getField("isClosing").getBoolean(dungeon);
    }
    public static void main(String[] args) throws Exception {
        Field namek = NgocRongNamecService.class.getDeclaredField("instance"); namek.setAccessible(true);
        namek.set(null, U.allocateInstance(NgocRongNamecService.class));
        Field singleton = MapService.class.getDeclaredField("i"); singleton.setAccessible(true);
        singleton.set(null, new MapService() {
            @Override public nro.models.map.Map getMapById(int mapId) { return null; }
        });
        nro.models.map.Map map = (nro.models.map.Map) U.allocateInstance(nro.models.map.Map.class);
        map.mapId = 135;
        Zone origin = new Zone(map, 0, 20), outside = new Zone(map, 1, 20);
        Treasure dungeon = new Treasure();
        Clan clan = new Clan(); clan.BanDoKhoBau = dungeon; dungeon.clan = clan;
        dungeon.zones.add(origin); dungeon.isOpened = true;
        BrokenPlayer broken = (BrokenPlayer) U.allocateInstance(BrokenPlayer.class);
        WaitingPlayer waiting = (WaitingPlayer) U.allocateInstance(WaitingPlayer.class);
        WaitingPlayer escaped = (WaitingPlayer) U.allocateInstance(WaitingPlayer.class);
        for (Player player : new Player[]{broken, waiting, escaped}) {
            player.isPlayer = true; player.idNRNM = -1; player.clan = clan; player.zone = origin;
            origin.getPlayers().add(player);
        }
        escaped.zone = outside;
        origin.getPlayers().add(null);
        Field start = BanDoKhoBau.class.getDeclaredField("lastTimeOpen"); start.setAccessible(true);
        start.setLong(dungeon, System.currentTimeMillis() - 1_800_100L);
        dungeon.update();
        check(closing(dungeon) && origin.dungeonClosing && dungeon.isOpened && clan.BanDoKhoBau == dungeon,
                "failed exits retain closing instance and clan pointer");
        check(broken.attempts == 1 && waiting.attempts == 1,
                "one exit exception does not skip another occupant");
        check(escaped.attempts == 0 && escaped.zone == outside,
                "stale occupant already outside is never pulled back");
        dungeon.update();
        check(broken.attempts == 2 && waiting.attempts == 2 && dungeon.awards == 1,
                "closing tick retries failed exits and records achievement once");
        broken.zone = outside; waiting.zone = outside;
        dungeon.update();
        check(!dungeon.isOpened && origin.dungeonClosing && clan.BanDoKhoBau == null,
                "instance disposes only after its occupants have left");
        DestronGas gas = new DestronGas(0);
        Clan gasClan = new Clan(); gasClan.KhiGasHuyDiet = gas; gas.clan = gasClan;
        StageBoss deadBoss = (StageBoss) U.allocateInstance(StageBoss.class);
        gas.bosses.add(deadBoss); GasDestroyManager.gI().addBoss(deadBoss);
        gas.isOpened = true;
        Field gasStart = DestronGas.class.getDeclaredField("lastTimeOpen"); gasStart.setAccessible(true);
        gasStart.setLong(gas, System.currentTimeMillis() - 1_800_100L);
        gas.update();
        check(!gas.isOpened && gas.bosses.isEmpty() && !GasDestroyManager.gI().getBosses().contains(deadBoss)
                        && deadBoss.stageCallbacks == 0,
                "dead stage boss is retired without spawning its next stage");
        BlackBallWar war = (BlackBallWar) U.allocateInstance(BlackBallWar.class);
        nro.models.map.Map blackMap = (nro.models.map.Map) U.allocateInstance(nro.models.map.Map.class);
        blackMap.mapId = 85;
        Zone blackOrigin = new Zone(blackMap, 0, 20), blackElsewhere = new Zone(blackMap, 1, 20);
        Field warZone = BlackBallWar.class.getDeclaredField("zone"); warZone.setAccessible(true); warZone.set(war, blackOrigin);
        WaitingPlayer transferred = (WaitingPlayer) U.allocateInstance(WaitingPlayer.class);
        transferred.isPlayer = true; transferred.zone = blackElsewhere;
        // No idMark: any stale-zone processing would fail or change the new instance.
        war.updatePlayer(transferred);
        check(transferred.zone == blackElsewhere && !blackElsewhere.finishBlackBallWar,
                "black ball snapshot cannot process player who joined another zone");
        System.out.println("DUNGEON_CLOSING PASS " + cases + " cases");
    }
}
