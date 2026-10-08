package nro.models.services;

import nro.models.boss.BossID;
import nro.models.boss.Boss_Manager.OtherBossManager;
import nro.models.boss.luyen_tap_tu_dong.TauPayPay;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstTask;
import nro.models.map.Zone;
import nro.models.network.Message;
import nro.models.player.*;
import nro.models.services_dungeon.TrainingService;
import static nro.models.services.PinkFixture.*;

/** Isolated training opponents; no constructors register real bosses or open SQL. */
public final class LiveSyncBossLifecycleTest {
    public static class Tau extends TauPayPay {
        public Tau() throws Exception { super(null); }
        @Override public boolean noticePublic() { return false; }
        @Override public void chat(String text) { }
        @Override protected void setDie(Player p) { ensureDeathTransition(); }
        public void expireDefeatWait() { lastTimeAFK = 0; afk(); }
        public void departureTick() { updateDeathLifecycle(); }
    }
    static int departures, ends;
    static boolean failNotification;
    static void installFixtures() throws Exception {
        install();
        replace(Service.class, "instance", new Service() {
            @Override public void sendMessAnotherNotMeInMap(Player p, Message m) {
                if (m.command == -65) departures++;
            }
            @Override public void sendPlayerVS(Player p, Player b, byte type) { }
        });
        replace(TaskService.class, "i", new TaskService() {
            @Override public int getIdTask(Player p) { return ConstTask.TASK_10_1; }
        });
        replace(TrainingService.class, "instance", new TrainingService() {
            @Override public Player getNonInteractiveNPC(Zone z, int id) { return null; }
            @Override public void luyenTapEnd(Player p, int id) {
                ends++;
                if (failNotification) throw new IllegalStateException("training notification failed");
            }
        });
    }
    static Tau tau() throws Exception {
        Tau b = blank(Tau.class);
        b.id = BossID.TAUPAYPAY; b.isBoss = true; b.currentLevel = 0;
        b.zone = zone(); b.playerAtt = player(901); b.playerAtt.zone = b.zone;
        b.nPoint = new NPoint(b); b.nPoint.hp = b.nPoint.hpMax = 100;
        b.effectSkill = new EffectSkill(b); b.effectSkin = new EffectSkin(b); b.idMark = new IDMark();
        b.bossStatus = BossStatus.ACTIVE;
        b.zone.addPlayer(b); OtherBossManager.gI().addBoss(b);
        return b;
    }
    static void assertRemoved(Tau b, Zone origin) {
        check(!origin.getBosses().contains(b), "dead Tau removed from original Zone boss registry");
        check(!origin.getHumanoids().contains(b), "dead Tau cannot be resent to a returning player");
        check(!OtherBossManager.gI().getBosses().contains(b), "dead Tau removed from manager");
        check(b.zone == null && b.nPoint == null && b.bossStatus == BossStatus.REST,
                "Tau reaches REST and disposes after leaving");
    }
    public static void main(String[] args) throws Exception {
        installFixtures();
        Tau b = tau(); Zone origin = b.zone;
        b.injured(b.playerAtt, 100, true, false);
        check(b.nPoint.hp == 0 && b.bossStatus == BossStatus.AFK, "lethal injury retains intended defeat wait");
        b.expireDefeatWait(); b.departureTick();
        assertRemoved(b, origin);
        check(departures == 1 && ends == 1, "departure animation and training end sent once");
        b.leaveMap(); assertRemoved(b, origin);
        check(departures == 1 && ends == 1, "repeated leave after disposal is safe and silent");
        failNotification = true;
        b = tau(); origin = b.zone;
        try { b.leaveMap(); } catch (IllegalStateException optionalAnimationFailure) { }
        assertRemoved(b, origin);
        System.out.println("PASS Tau lethal injury, AFK departure, Zone/manager cleanup, idempotent leave and notification failure");
    }
}
