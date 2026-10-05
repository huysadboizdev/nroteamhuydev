package nro.models.player_badges;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import nro.models.consts.ConstTaskBadges;
import nro.models.item.DragonPetCollection;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.services.Service;
import nro.models.task.BadgesTask;
import nro.models.task.BadgesTaskTemplate;

/** Dragon title follows current ownership, never cumulative receipts of duplicate dragons. */
public final class DragonBadgeRules {
    private DragonBadgeRules() { }

    public static boolean isDragonBadge(Player player, int effectId) {
        if (effectId < 0) return false;
        if (player != null && player.dataTaskBadges != null) for (BadgesTask task : player.dataTaskBadges)
            if (task.id == ConstTaskBadges.ME_RONG && task.idBadgesReward == effectId) return true;
        for (BadgesTaskTemplate task : Manager.TASKS_BADGES_TEMPLATE)
            if (task.id == ConstTaskBadges.ME_RONG && task.idbadgesReward == effectId) return true;
        return false;
    }

    public static boolean canUse(Player player, int effectId) {
        return !isDragonBadge(player, effectId) || (player != null && player.isPl()
                && DragonPetCollection.hasFullPermanentSet(player.inventory));
    }

    /** Returns whether active badge stats changed. Leaves other titles and ownership records intact. */
    public static boolean refresh(Player player) {
        if (player == null) return false;
        synchronized (player) {
            int count = player.isPl() ? DragonPetCollection.countPermanentTypes(player.inventory) : 0;
            for (BadgesTask task : player.dataTaskBadges) if (task.id == ConstTaskBadges.ME_RONG) {
                task.count = count;
                task.countMax = 7;
            }
            boolean changed = false;
            Map<Integer, BadgesData> unique = new HashMap<>();
            Iterator<BadgesData> iterator = player.dataBadges.iterator();
            while (iterator.hasNext()) {
                BadgesData badge = iterator.next();
                if (!isDragonBadge(player, badge.idBadGes)) continue;
                BadgesData first = unique.putIfAbsent(badge.idBadGes, badge);
                if (first != null) {
                    first.isUse |= badge.isUse;
                    first.timeofUseBadges = Math.max(first.timeofUseBadges, badge.timeofUseBadges);
                    iterator.remove();
                    changed = true;
                }
            }
            if (count < 7) {
                for (BadgesData badge : unique.values()) if (badge.isUse) {
                    badge.isUse = false;
                    changed = true;
                }
                if (isDragonBadge(player, player.badges.idBadges)) {
                    Service.gI().sendBadgesPlayer(player, 0, player.badges.idBadges);
                    player.badges.idBadges = -1;
                    changed = true;
                }
            }
            return changed;
        }
    }
}
