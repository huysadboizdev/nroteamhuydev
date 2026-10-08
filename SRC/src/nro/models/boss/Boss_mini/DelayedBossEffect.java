package nro.models.boss.Boss_mini;

import nro.models.boss.Boss;
import nro.models.consts.BossStatus;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.services.ItemTimeService;
import nro.models.services.PlayerService;
import nro.models.utils.Util;

/** A timed attack belongs to one character life and one visit to its source zone. */
final class DelayedBossEffect {
    final Player target;
    final Zone zone;
    final long targetVisit, sourceVisit, revived;
    final int icon;
    final long deadline;

    DelayedBossEffect(Boss source, Player target, long duration, int icon) {
        this.target = target;
        this.zone = source.zone;
        this.targetVisit = target.zoneExitSequence;
        this.sourceVisit = source.zoneExitSequence;
        this.revived = target.lastTimeRevived;
        this.icon = icon;
        this.deadline = System.currentTimeMillis() + duration;
    }

    boolean valid(Boss source) {
        return zone != null && source.zone == zone && target.zone == zone
                && source.zoneExitSequence == sourceVisit && target.zoneExitSequence == targetVisit
                && target.lastTimeRevived == revived && !target.beforeDispose
                && target.nPoint != null && !target.isDie() && source.nPoint != null
                && !source.isDie() && source.bossStatus == BossStatus.ACTIVE;
    }

    /** Caller holds target so leaving the zone cannot race the damage application. */
    void expire(Boss source) {
        if (valid(source) && Util.isTrue(80, 100)) {
            // Attribute the damage to the boss: null attackers skip Player's death handling.
            target.injured(source, target.nPoint.hp, true, false);
            PlayerService.gI().sendInfoHpMpMoney(target);
        }
        if (!target.beforeDispose) ItemTimeService.gI().sendItemTime(target, icon, 0);
    }
}
