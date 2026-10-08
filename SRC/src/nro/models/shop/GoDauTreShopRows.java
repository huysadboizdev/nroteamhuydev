package nro.models.shop;

import java.util.List;
import nro.models.player.Player;
import nro.models.player_badges.BadgeRules;
import nro.models.player_badges.BadgesData;
import nro.models.player_badges.BagesTemplate;
import nro.models.task.BadgesTaskService;

/** Three legacy title IDs share one visible row; ownership and reward IDs stay intact. */
public final class GoDauTreShopRows {
    private GoDauTreShopRows() {}

    public static boolean isFamily(int itemId) { return itemId >= 1392 && itemId <= 1394; }

    public static int selectedItem(Player player, List<ItemShop> rows) {
        synchronized (player) {
            int selected = -1, bestRank = -1, bestProgress = -1;
            long bestExpiry = -1;
            for (ItemShop row : rows) {
                if (row == null || row.temp == null || !isFamily(row.temp.id)
                        || row.temp.gender <= 2 && row.temp.gender != player.gender) continue;
                BagesTemplate title = BagesTemplate.fineBadgesbyIdItem(row.temp.id);
                if (title == null) continue;
                int rank = 0;
                long expiry = -1;
                for (BadgesData badge : player.dataBadges) {
                    if (badge.idBadGes != title.idEffect || !BadgeRules.usable(player, badge)) continue;
                    int ownedRank = badge.isUse || player.badges.idBadges == badge.idBadGes ? 2 : 1;
                    if (ownedRank > rank || ownedRank == rank && badge.timeofUseBadges > expiry) {
                        rank = ownedRank;
                        expiry = badge.timeofUseBadges;
                    }
                }
                int progress = rank == 0 ? BadgesTaskService.sendPercenBadgesTask(player, title.idEffect) : 0;
                if (rank > bestRank || rank == bestRank && (expiry > bestExpiry
                        || expiry == bestExpiry && progress > bestProgress)) {
                    selected = row.temp.id;
                    bestRank = rank;
                    bestExpiry = expiry;
                    bestProgress = progress;
                }
            }
            return selected;
        }
    }
}
