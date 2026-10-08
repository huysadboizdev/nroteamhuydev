package nro.models.boss.Boss_mini;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import nro.models.boss.BossID;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstPlayer;
import nro.models.player.NPoint;
import nro.models.player.Player;
import nro.models.server.Client;
import nro.models.services.ItemTimeService;
import nro.models.services.PinkFixture;

/** Offline regression checks; never opens maps, sockets or database connections. */
public final class MiniSunRegressionTest {
    static class Sun extends MatTroi {
        Sun() throws Exception { super(); }
        boolean attackedWithExpiredBurn;
        @Override public void attack() {
            attackedWithExpiredBurn = timers(this).values().stream().anyMatch(t -> t <= System.currentTimeMillis());
        }
        @Override public void chat(String text) { }
        @Override public void changeStatus(BossStatus status) { }
        @Override public void joinMap2() { }
    }

    @SuppressWarnings("unchecked")
    static Map<Long, Long> timers(MatTroi boss) {
        try {
            Field f = MatTroi.class.getDeclaredField("globalEffectTimers");
            f.setAccessible(true);
            return (Map<Long, Long>) f.get(boss);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    static Sun sun() throws Exception {
        Sun s = PinkFixture.blank(Sun.class);
        Field timers = MatTroi.class.getDeclaredField("globalEffectTimers");
        timers.setAccessible(true);
        timers.set(s, new ConcurrentHashMap<Long, Long>());
        Field st = MatTroi.class.getDeclaredField("st");
        st.setAccessible(true);
        st.setLong(s, System.currentTimeMillis());
        s.typePk = ConstPlayer.PK_ALL;
        s.nPoint = new NPoint(s);
        return s;
    }

    static void apply(Sun s, Player p) throws Exception {
        Method m = MatTroi.class.getDeclaredMethod("applyEffect", Player.class);
        m.setAccessible(true);
        m.invoke(s, p);
    }

    static void run(String mode) throws Exception {
        PinkFixture.install();
        Client client = PinkFixture.blank(Client.class);
        Field players = Client.class.getDeclaredField("players_id");
        players.setAccessible(true);
        players.set(client, new ConcurrentHashMap<Long, Player>());
        PinkFixture.replace(Client.class, "instance", client);
        final int[] countdown = {-1};
        PinkFixture.replace(ItemTimeService.class, "i", new ItemTimeService() {
            @Override public void sendItemTime(Player p, int icon, int seconds) { countdown[0] = seconds; }
        });
        Sun s = sun();
        Player p = PinkFixture.player(123);
        if (mode.equals("timing")) {
            timers(s).put(p.id, System.currentTimeMillis() - 1);
            timers(s).put(124L, System.currentTimeMillis() + 30000);
            s.active();
            PinkFixture.check(!timers(s).containsKey(p.id), "expired burn must drain during combat, not only at 15-minute despawn");
            PinkFixture.check(timers(s).containsKey(124L), "future burn must remain pending");
            PinkFixture.check(!s.attackedWithExpiredBurn, "expired burn must resolve before an attack can reapply it");
        } else if (mode.equals("reapply")) {
            long original = System.currentTimeMillis() + 15000;
            timers(s).put(p.id, original);
            apply(s, p);
            PinkFixture.check(timers(s).get(p.id) == original, "another area attack must not postpone existing burn");
            PinkFixture.check(countdown[0] == -1, "duplicate application must not restart icon countdown");
        } else if (mode.equals("countdown")) {
            long before = System.currentTimeMillis();
            apply(s, p);
            PinkFixture.check(countdown[0] == 30, "client countdown must match 30-second burn deadline, got " + countdown[0]);
            PinkFixture.check(timers(s).get(p.id) >= before + 30000 && timers(s).get(p.id) <= System.currentTimeMillis() + 30000, "burn remains 30 seconds");
        } else if (mode.equals("leave")) {
            timers(s).put(p.id, 1L);
            s.leaveMap();
            PinkFixture.check(timers(s).isEmpty(), "departed spawn must discard pending burn");
        } else if (mode.equals("join")) {
            timers(s).put(p.id, 1L);
            s.joinMap();
            PinkFixture.check(timers(s).isEmpty(), "new spawn must not inherit burn from previous spawn");
            PinkFixture.check(s.nPoint.hp == 100 && s.nPoint.hpMax == 100, "spawn retains 100-hit HP");
        } else if (mode.equals("id")) {
            MatTroi real = new MatTroi();
            MatTroi second = new MatTroi();
            PinkFixture.check(real.id < 0 && real.id != second.id && real.id != BossID.Virut,
                    "mini Sun instances must have distinct negative identities, got " + real.id);
        } else throw new AssertionError("Unknown mode " + mode);
    }

    public static void main(String[] args) {
        try { run(args[0]); System.out.println("MINI_SUN " + args[0] + " PASS"); System.exit(0); }
        catch (Throwable e) { e.printStackTrace(); System.exit(1); }
    }
}
