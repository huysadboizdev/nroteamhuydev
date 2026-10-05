package nro.models.task;

import nro.models.task.BadgesTask;
import nro.models.player.Player;
import nro.models.player_badges.BadgesData;
import nro.models.server.Manager;
import nro.models.task.BadgesTaskTemplate;

/**
 *
 * @author By Mr Blue
 * 
 */

public class BadgesTaskService {

    public static void createAndResetTask(Player player) {
        player.badges.progressDay = nro.models.player_badges.BadgeRules.day();
        player.dataTaskBadges.clear();
        for (BadgesTaskTemplate BTT : Manager.TASKS_BADGES_TEMPLATE) {
            BadgesTask data = new BadgesTask();
            data.id = BTT.id;
            data.count = 0;
            data.countMax = BTT.count;
            data.idBadgesReward = BTT.idbadgesReward;
            player.dataTaskBadges.add(data);
        }
    }

    public static void updateDoneTask(Player player) {
        synchronized (player) {
        nro.models.player_badges.BadgeRules.syncTasks(player);
        nro.models.player_badges.BadgeRules.refresh(player);
        for (BadgesTask data : player.dataTaskBadges) {
            if (data.isDone() && nro.models.player_badges.BadgeRules.template(data.idBadgesReward)!=null
                    && nro.models.player_badges.DragonBadgeRules.canUse(player, data.idBadgesReward)) {
                boolean owned = false;
                for (BadgesData bg : player.dataBadges) {
                    if (bg.idBadGes == data.idBadgesReward) {
                        owned = true;
                        break;
                    }
                }
                if (owned) continue;
                // This constructor inserts itself into dataBadges exactly once.
                new BadgesData(player, data.idBadgesReward, 30);
                if (data.id != nro.models.consts.ConstTaskBadges.ME_RONG && data.id != 19) data.count = 0;
            }
        }
        }
    }

    public static void updateCountBagesTask(Player player, int id, int amount) {
        if(player==null || !player.isPl() || player.dataTaskBadges==null || player.badges==null || amount<=0) return;
        synchronized(player) {
        nro.models.player_badges.BadgeRules.ensureDay(player);
        if (id == nro.models.consts.ConstTaskBadges.ME_RONG) {
            nro.models.player_badges.DragonBadgeRules.refresh(player);
            return;
        }
        for (BadgesTask data : player.dataTaskBadges) {
            if (data.id == id) {
                data.count = (int)Math.min(data.countMax, (long)Math.max(0,data.count)+amount);
                if (data.count > data.countMax) {
                    data.count = data.countMax;
                }
                break;
            }
        }
    }

    }

    public static int sendPercenBadgesTask(Player player, int idBadgesReward) {
        nro.models.player_badges.BadgeRules.syncTasks(player);
        if (nro.models.player_badges.DragonBadgeRules.isDragonBadge(player, idBadgesReward))
            return player.isPl() ? nro.models.item.DragonPetCollection.countPermanentTypes(player.inventory) * 100 / 7 : 0;
        for (BadgesTask data : player.dataTaskBadges) {
            if (data.idBadgesReward == idBadgesReward) {
                if (data.getPercentProcess() > 0) {
                    return data.getPercentProcess();
                } else {
                    return 0;
                }
            }
        }
        return 0;
    }

    public static int sendDay(Player player, int id) {
        for (BadgesData data : player.dataBadges) {
            if (data.idBadGes == id) {
                long timeDifference = data.timeofUseBadges - System.currentTimeMillis();
                return (int) Math.max(0, (timeDifference + 86_399_999L) / 86_400_000L);
            }
        }
        return 0;
    }

}
