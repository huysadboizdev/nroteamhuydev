package nro.models.boss.PrisonBill;

import nro.models.boss.*;
import nro.models.boss.Black_Goku.BlackGoku;
import nro.models.consts.BossStatus;

/** Uses the existing Black Goku battle/reward logic with coordinated Kamin/Oren lifetimes. */
public class PrisonBillBoss extends BlackGoku {
    public PrisonBillBoss(int id) throws Exception { super(id, PrisonBillBossCatalog.data(id)); }
    @Override protected String getSpawnName() { return data[0].getName(); }
    @Override public void joinMap() {
        if (parentBoss != null && (parentBoss.zone == null || parentBoss.isDie())) {
            changeStatus(BossStatus.REST); return;
        }
        super.joinMap();
    }
    @Override public void autoLeaveMap() {
        if (parentBoss == null) super.autoLeaveMap();
    }
    @Override public long noticeNextSpawnAt() {
        if (bossStatus != BossStatus.REST || parentBoss != null || lastTimeRest <= 0) return 0;
        long from = lastTimeRest;
        if (questBossId() == BossID.KAMIN) for (Boss child : getCompanionBosses()) {
            if (child != null) {
                if (child.zone != null) return 0;
                from = Math.max(from, child.questLastRest());
            }
        }
        return from + Math.max(0L, secondsRest) * 1000L;
    }
    @Override public void rest() {
        if (questBossId() == BossID.KAMIN && data.length > 0) {
            if (bossAppearTogether != null && bossAppearTogether.length > 0 && bossAppearTogether[0] != null) {
                for (Boss child : bossAppearTogether[0]) if (child != null) {
                    if (child.zone != null) return;
                    if (child instanceof PrisonBillBoss paired && paired.lastTimeRest > lastTimeRest)
                        lastTimeRest = paired.lastTimeRest;
                }
            }
        }
        super.rest();
    }
    @Override public void wakeupAnotherBossWhenDisappear() {
        // A living leader leaving an idle map retires the pair; a defeated leader lets Oren fight on.
        if (questBossId() == BossID.KAMIN && !isDie()) {
            for (Boss child : getCompanionBosses()) if (child != null && child.zone != null) child.leaveMap();
        }
    }
}
