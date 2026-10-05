package nro.models.player_badges;

import nro.models.player.Player;

/**
 *
 * @author By Mr Blue
 * 
 */

public class BadgesService {

    public static void turnOnBadges(Player player, int id) {
        synchronized (player) {
        BadgeRules.refresh(player);
        if (!BadgeRules.owned(player, id)) return;
        if (player.dataBadges != null) {
            for (BadgesData data : player.dataBadges) {
                if (data.idBadGes == id) {
                    data.isUse = true;
                } else {
                    data.isUse = false;
                }
            }
        }
        }
    }

}
