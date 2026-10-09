package nro.models.services;

import nro.models.boss.Cell.XenBoHung;
import nro.models.boss.luyen_tap_tu_dong.TrainingBoss;
import nro.models.boss.luyen_tap_tu_dong.Whis;
import nro.models.boss.vo_dai_hat_mit.DeathOrAliveArena;
import nro.models.consts.BossType;
import nro.models.consts.BossStatus;
import nro.models.player.*;
import static nro.models.services.PinkFixture.*;

/** Executes production arithmetic paths while suppressing transport and SQL. */
public final class LiveSyncBossArithmeticTest {
    public static class Trainer extends TrainingBoss {
        public Trainer() throws Exception { super(BossType.PHOBAN, 0, null); }
    }
    public static class Arena extends DeathOrAliveArena {
        public Arena() throws Exception { super(BossType.PHOBAN, 0, null); }
        public void opponent(Player p) { playerAtt = p; }
    }
    public static class WhisProbe extends Whis {
        public WhisProbe() throws Exception { super(null); }
        @Override public void chat(String text) { }
        @Override public void die(Player p) { bossStatus = BossStatus.DIE; }
        @Override protected void setDie(Player p) { }
    }
    public static class Absorber extends XenBoHung {
        public Absorber() throws Exception { super(); }
        @Override public void attack() { }
        @Override public void chat(String text) { }
        @Override public boolean chat(int prefix, String text) { return true; }
    }
    public static class AbsorbedPlayer extends Player {
        public AbsorbedPlayer() { }
        @Override public synchronized int injured(Player p, long d, boolean pierce, boolean mob) { nPoint.hp = 0; return 0; }
    }
    public static class AbsorbPoints extends NPoint {
        int recalculations;
        public AbsorbPoints(Player p) { super(p); }
        @Override public void calPoint() { recalculations++; }
    }
    static void fixtures() throws Exception {
        install();
        replace(Service.class, "instance", new Service() {
            @Override public void sendInfoPlayerEatPea(Player p) { }
            @Override public void sendThongBao(Player p, String s) { }
            @Override public void resetPoint(Player p, int x, int y) { }
        });
        replace(PlayerService.class, "i", new PlayerService() {
            @Override public void hoiPhuc(Player p, long hp, long mp) { }
        });
    }
    static void damage() throws Exception {
        NPoint p = blank(NPoint.class); p.hp = 100;
        p.subHP(4_294_967_296L); check(p.hp == 0, "long damage must kill rather than wrap back to unchanged HP");
        p.hp = Integer.MAX_VALUE; p.subHP(Long.MAX_VALUE); check(p.hp == 0, "Long.MAX damage clamps at zero");
        p.hp = 100; p.subHP(-1); check(p.hp == 100, "negative damage cannot heal");
        p.subHP(Long.MIN_VALUE); check(p.hp == 100, "Long.MIN damage cannot overflow subtraction");
        p.subHP(40); check(p.hp == 60, "normal damage unchanged");
    }
    static void defense() throws Exception {
        NPoint p = blank(NPoint.class); p.def = 1;
        check(p.subDameInjureWithDeff(Long.MAX_VALUE) == Integer.MAX_VALUE,
                "defense calculation cannot narrow long damage to negative int");
        check(p.subDameInjureWithDeff(Long.MIN_VALUE) == 1,
                "minimum long damage cannot wrap to maximum after defense subtraction");
        p.def = 0; check(p.subDameInjureWithDeff(0) == 0, "zero damage keeps existing semantics");
        p.def = 30; check(p.subDameInjureWithDeff(100) == 70, "ordinary defense unchanged");
    }
    static void returnedDamage() throws Exception {
        WhisProbe b = blank(WhisProbe.class); b.nPoint = new NPoint(b); b.nPoint.hp = 100;
        var level = Whis.class.getDeclaredField("level"); level.setAccessible(true); level.setLong(b, 1);
        // Whis only accepts damage from its assigned training partner; use a matching
        // attacker so the test exercises long-to-int clamping instead of the null guard.
        var attacker = player(901); b.playerAtt = attacker;
        check(b.injured(attacker, 4_294_967_296L, true, false) == Integer.MAX_VALUE,
                "Whis reports bounded nonnegative damage rather than narrow long to zero");
    }
    static void pea() throws Exception {
        Trainer b = blank(Trainer.class); b.nPoint = new NPoint(b);
        b.nPoint.hp = b.nPoint.hpMax = Integer.MAX_VALUE; b.buffPea();
        check(b.nPoint.hp == Integer.MAX_VALUE, "pea heal cannot wrap a full large HP pool");
        b.lastTimeBuff = 0; b.nPoint.hpMax = 100; b.nPoint.hp = 95; b.buffPea();
        check(b.nPoint.hp == 100, "pea heal clamps to boss max HP");
        b.lastTimeBuff = 0; b.nPoint.hp = 60; b.buffPea(); check(b.nPoint.hp == 80, "ordinary pea heals one fifth");
    }
    static void whis() throws Exception {
        Player owner = player(902); owner.zone = zone(); owner.zone.map.mapId = 154; owner.traning = new Traning(); owner.traning.setTop(3904);
        WhisProbe b = blank(WhisProbe.class); b.playerAtt = owner; b.nPoint = new NPoint(b);
        b.location = new Location(); b.lastTimePickNRNM = System.currentTimeMillis();
        b.nPoint.hpMax = 550_000; b.nPoint.dame = 100; b.joinMap();
        check(b.nPoint.hpMax == Integer.MAX_VALUE && b.nPoint.hp == Integer.MAX_VALUE, "Whis LV3905 HP clamps instead of narrowing negative");
        check(b.nPoint.dame == 390_500, "normal Whis damage scale retained");
        owner.traning.setTop(Integer.MAX_VALUE); b.nPoint.hpMax = 550_000; b.nPoint.dame = 2_000; b.joinMap();
        check(b.nPoint.hp == Integer.MAX_VALUE && b.nPoint.dame == Integer.MAX_VALUE, "top+1 and Whis damage scale use long intermediates");
        b.nPoint.hp = 100; b.injured(owner, Long.MAX_VALUE, true, false);
        check(b.nPoint.hp == 0, "large Whis injury reaches zero without overflow");
    }
    static void arena() throws Exception {
        Player owner = player(903); owner.zone = zone(); owner.nPoint = new NPoint(owner); owner.nPoint.hpMax = 2_100_000_000;
        Arena b = blank(Arena.class); b.id = -93; b.nPoint = new NPoint(b); b.opponent(owner);
        b.location = new Location(); b.lastTimePickNRNM = System.currentTimeMillis(); b.joinMap();
        check(b.nPoint.hp == Integer.MAX_VALUE, "Dracula 111 percent of 2.1 billion HP clamps safely");
        owner.nPoint.hpMax = 1_000; b.joinMap(); check(b.nPoint.hp == 1_110, "ordinary arena HP scale retained");
    }
    static void absorb() throws Exception {
        var random = nro.models.utils.Util.class.getDeclaredField("rand"); random.setAccessible(true);
        ((java.util.Random) random.get(null)).setSeed(90210L);
        AbsorbedPlayer victim = blank(AbsorbedPlayer.class); victim.id = 904; victim.isPlayer = true;
        victim.nPoint = new NPoint(victim); victim.nPoint.hp = victim.nPoint.hpMax = 1_500_000_000;
        victim.nPoint.dame = 1_500_000_000; victim.location = new Location();
        Absorber b = blank(Absorber.class); b.typePk = 3; b.zone = zone(); b.zone.addPlayer(victim);
        b.location = new Location(); b.lastTimePickNRNM = System.currentTimeMillis();
        AbsorbPoints stats = new AbsorbPoints(b); b.nPoint = stats; stats.hpg = 100; stats.dameg = 100;
        for (int i = 0; i < 100_000 && stats.recalculations == 0; i++) b.active();
        check(stats.recalculations == 1, "production absorption executed");
        check(stats.hpg == 30_000_100 && stats.dameg == 75_000_100, "absorption percentages computed in long before division");
        var last = XenBoHung.class.getDeclaredField("lastTimeHapThu"); last.setAccessible(true); last.setLong(b, 0);
        stats.hpg = stats.dameg = Integer.MAX_VALUE; victim.nPoint.hp = 1_500_000_000;
        for (int i = 0; i < 100_000 && stats.recalculations == 1; i++) b.active();
        check(stats.recalculations == 2, "production absorption executed on saturated stats");
        check(stats.hpg == Integer.MAX_VALUE && stats.dameg == Integer.MAX_VALUE, "absorption increments clamp saturated base stats");
    }
    public static void main(String[] args) throws Exception {
        fixtures();
        for (String test : args.length == 0 ? new String[]{"damage", "defense", "returnedDamage", "pea", "whis", "arena", "absorb"} : args) {
            switch (test) { case "damage" -> damage(); case "defense" -> defense(); case "returnedDamage" -> returnedDamage(); case "pea" -> pea(); case "whis" -> whis(); case "arena" -> arena(); case "absorb" -> absorb(); default -> throw new AssertionError(test); }
        }
        System.out.println("PASS long HP damage, bounded pea heal, actual Whis/arena scaling and Cell absorption");
    }
}
