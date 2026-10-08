package nro.models.services;

import java.util.function.IntUnaryOperator;
import nro.models.boss.Boss;
import nro.models.consts.ConstItem;
import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.player.Pet;
import nro.models.player.NewPet;
import nro.models.player.Player;
import nro.models.utils.Util;

/** One private capsule reward per mini boss life. Each boss owns its own instance. */
public final class MiniBossPinkCapsuleDrop {
    private boolean granted;

    /** Matches player kill credit, assigning a pet's reward to its master. */
    public static Player creditedPlayer(Player killer) {
        Player owner = killer instanceof Pet ? ((Pet)killer).master
                : killer instanceof NewPet ? ((NewPet)killer).master : killer;
        return owner != null && owner.isPl() ? owner : null;
    }

    public boolean drop(Boss boss, Player killer) {
        return drop(boss, killer, Util::nextInt);
    }

    synchronized boolean drop(Boss boss, Player killer, IntUnaryOperator randomBounded) {
        Player owner = creditedPlayer(killer);
        if (granted || owner == null || boss == null || boss.zone == null
                || boss.zone.map == null || boss.zone.map.mapWidth <= 0 || boss.location == null) return false;
        Zone zone = boss.zone;
        int count = 1 + randomBounded.applyAsInt(5);
        if (count < 1 || count > 5) throw new IllegalArgumentException("capsule count must be between one and five");
        granted = true;
        for (int i = 0; i < count; i++) {
            int x = Math.max(0, Math.min(zone.map.mapWidth - 1, boss.location.x + (i - count / 2) * 20));
            int y = zone.map.yPhysicInTop(x, boss.location.y - 24);
            ItemMap drop = new ItemMap(zone, ConstItem.CAPSULE_HONG, 1, x, y, owner.id, true);
            try { Service.gI().dropItemMap(zone, drop); }
            catch (Exception packetFailure) { /* Already published in the zone; a refresh sends the drop. */ }
        }
        return true;
    }

    /** Called by the boss's resetBase hook when a new life starts. */
    public synchronized void reset() {
        granted = false;
    }
}
