package nro.models.services;

import java.util.*;
import nro.models.boss.*;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.player.Player;
import nro.models.task.*;
import static nro.models.services.PinkFixture.*;

public final class QuestSupportProgressTest {
    static Player player(int task, int step) {
        Player p = new Player(); p.isPlayer = true;
        p.playerTask.taskMain = new TaskMain(); p.playerTask.taskMain.id = task; p.playerTask.taskMain.index = step;
        for (int i = 0; i < 8; i++) { SubTaskMain s = new SubTaskMain(); s.maxCount = 1; p.playerTask.taskMain.subTasks.add(s); }
        return p;
    }
    static Boss support(int id) {
        QuestSupportIdentity.begin();
        try {
            Boss boss = BossManager.gI().createBoss(id);
            check(boss != null, "constructor group exists"); return boss;
        } finally { QuestSupportIdentity.end(); }
    }
    static void reset(Boss boss) throws Exception {
        var method = Boss.class.getDeclaredMethod("resetBase"); method.setAccessible(true); method.invoke(boss);
    }
    public static void main(String[] args) throws Exception {
        Set<Long> ids = new HashSet<>();
        int[][] cases = {{BossID.SO_4,20,1},{BossID.SO_3,20,2},{BossID.SO_2,20,3},{BossID.SO_1,20,4},
                {BossID.TIEU_DOI_TRUONG,20,5},{BossID.ANDROID_19,23,1},{BossID.DR_KORE,23,2},
                {BossID.ANDROID_15,24,1},{BossID.ANDROID_14,24,2},{BossID.ANDROID_13,24,3},
                {BossID.POC,25,1},{BossID.PIC,25,2},{BossID.KING_KONG,25,3}};
        for (int[] c : cases) {
            Boss boss = support(c[0]);
            check(ids.add(boss.id) && boss.id >= -900_000_000 && boss.id < -100_000_000 && boss.questBossId() == c[0], "separate safe network identity");
            boss.currentLevel = 0; boss.nPoint.hp = 1;
            Player alive = player(c[1], c[2]); boss.reward(alive);
            check(alive.playerTask.taskMain.index == c[2], "living boss cannot grant callback credit");
            boss.nPoint.hp = 0;
            Player wrong = player(c[1], 0); boss.reward(wrong);
            check(wrong.playerTask.taskMain.index == 0, "wrong power/story step retained");
            reset(boss); Player p = player(c[1], c[2]); boss.reward(p);
            check(p.playerTask.taskMain.index == c[2]+1, "correct task progresses " + c[0]);
            boss.reward(p); check(p.playerTask.taskMain.index == c[2]+1, "death callback consumes once");
            check(p.event.getEventPoint() == 0 && p.inventory.gold == 0 && p.inventory.itemsBag.isEmpty(), "no event/drop rewards");
            boss.retireQuestSupport(); reset(boss); boss.reward(p);
            check(p.playerTask.taskMain.index == c[2]+1, "retired callback has no effect");
        }
        for (int id : new int[]{BossID.FIDE, BossID.XEN_BO_HUNG}) {
            Boss boss = support(id); int task = id == BossID.FIDE ? 21 : 26;
            Player p = player(task,1);
            for (int level = 0; level < 3; level++) {
                reset(boss); boss.currentLevel = level; boss.nPoint.hp = 0;
                boss.reward(p); boss.reward(p);
                check(p.playerTask.taskMain.index == level+2, "three forms progress in order once");
            }
            reset(boss); boss.currentLevel = 0; boss.nPoint.hp = 0;
            Player third = player(task,3); boss.reward(third);
            check(third.playerTask.taskMain.index == 3, "first form cannot substitute for third");
        }
        Boss ordinary = BossManager.gI().createBoss(BossID.FIDE);
        check(!ordinary.isQuestSupport() && ordinary.id == BossID.FIDE, "ordinary identity unchanged");
        ordinary.currentLevel = 1; Player ordinaryPlayer = player(21,2);
        TaskService.gI().checkDoneTaskKillBoss(ordinaryPlayer, ordinary);
        check(ordinaryPlayer.playerTask.taskMain.index == 3, "ordinary task recognition unchanged");
        Player power = player(20,0); TaskService.gI().checkDoneTaskPower(power,599_999_999L);
        check(power.playerTask.taskMain.index == 0, "existing 600m power gate retained");
        TaskService.gI().checkDoneTaskPower(power,600_000_000L); check(power.playerTask.taskMain.index == 1, "existing power gate still advances");
        check(!QuestSupportIdentity.supports(BossID.KUKU) && !QuestSupportIdentity.supports(BossID.SIEU_BO_HUNG), "excluded groups");
        Boss statBase = BossManager.gI().createBoss(BossID.FIDE), statExtra = support(BossID.FIDE);
        replace(BossLiveReload.class, "overrides", Map.of(new BossLiveReload.Key(BossID.FIDE,1), new BossLiveReload.Stats(7654321,12345)));
        statBase.currentLevel = 1; statExtra.currentLevel = 1;
        BossLiveReload.applyAtSpawn(statBase); BossLiveReload.applyAtSpawn(statExtra);
        check(statBase.nPoint.hpg == statExtra.nPoint.hpg && statBase.nPoint.dameg == statExtra.nPoint.dameg, "support uses same live HP/damage overrides");
        replace(BossLiveReload.class, "overrides", Map.of());
        // The original concrete Android14 injury path must activate only its own unique-ID group.
        replace(PlayerService.class, "i", new PlayerService() {
            @Override public void hoiPhuc(Player p, long hp, long mp) { p.nPoint.hp = p.nPoint.hpMax; }
        });
        var android = (nro.models.boss.Android.Android14)support(BossID.ANDROID_14);
        Boss otherAndroid = support(BossID.ANDROID_14), originalAndroid = BossManager.gI().createBoss(BossID.ANDROID_14);
        android.currentLevel = 0; android.nPoint.hp = 10; android.nPoint.hpMax = 10;
        for (Boss child : android.getCompanionBosses()) { child.currentLevel = 0; child.nPoint.hp = 10; child.nPoint.hpMax = 10; }
        android.injured(null,10L,true,false);
        Boss android13 = Arrays.stream(android.getCompanionBosses()).filter(b -> b.questBossId() == BossID.ANDROID_13).findFirst().orElseThrow();
        var android15 = (nro.models.boss.Android.Android15)Arrays.stream(android.getCompanionBosses()).filter(b -> b.questBossId() == BossID.ANDROID_15).findFirst().orElseThrow();
        check(android.callApk13 && android15.callApk13 && android13.bossStatus == nro.models.consts.BossStatus.RESPAWN, "actual lethal trigger retains Android13 progression");
        check(!((nro.models.boss.Android.Android14)otherAndroid).callApk13 && !((nro.models.boss.Android.Android14)originalAndroid).callApk13, "no cross-chain or original mutation");
        Boss squad = support(BossID.TIEU_DOI_TRUONG), otherSquad = support(BossID.TIEU_DOI_TRUONG);
        squad.currentLevel = 0; otherSquad.currentLevel = 0;
        for (Boss b : squad.getCompanionBosses()) { b.currentLevel = 0; b.nPoint.hp = 10; b.bossStatus = nro.models.consts.BossStatus.AFK; }
        Boss number4 = Arrays.stream(squad.getCompanionBosses()).filter(b -> b.questBossId() == BossID.SO_4).findFirst().orElseThrow();
        Boss number3 = Arrays.stream(squad.getCompanionBosses()).filter(b -> b.questBossId() == BossID.SO_3).findFirst().orElseThrow();
        number4.doneChatE(); check(number3.bossStatus == nro.models.consts.BossStatus.ACTIVE, "Nappa actual semantic sibling kill order activates number3");
        check(otherSquad.getCompanionBosses()[0].bossStatus == nro.models.consts.BossStatus.REST, "Nappa other chain asleep");
        install(); replace(TaskService.class,"i",new TaskService());
        Boss originalRewards = BossManager.gI().createBoss(BossID.FIDE);
        originalRewards.zone = zone(); originalRewards.currentLevel = 0;
        Player rewarded = player(21,1); originalRewards.reward(rewarded);
        check(rewarded.event.getEventPoint() == 5 && !originalRewards.zone.getItemsSnapshot().isEmpty()
                && rewarded.playerTask.taskMain.index == 2, "base boss event/drop/task rewards retained");
        var notify = blank(nro.models.server.ServerNotify.class); List<String> publicMessages = new ArrayList<>();
        var messages = nro.models.server.ServerNotify.class.getDeclaredField("notifies"); messages.setAccessible(true); messages.set(notify,publicMessages);
        replace(nro.models.server.ServerNotify.class,"i",notify);
        var notification = Boss.class.getDeclaredMethod("notifyJoinMap"); notification.setAccessible(true);
        Boss silentSupport = support(BossID.FIDE); silentSupport.currentLevel = 0; silentSupport.zone = zone();
        notification.invoke(silentSupport); check(publicMessages.isEmpty(), "support spawn does not spam public boss announcements");
        notification.invoke(originalRewards); check(publicMessages.size()==1, "original spawn notice retained");
        for (Boss b : List.copyOf(BossManager.gI().getBosses())) BossManager.gI().removeBoss(b);
        System.out.println("PASS QuestSupport progress: unique network IDs, every boss/form, actual task gates, duplicate and late callbacks");
    }
}
