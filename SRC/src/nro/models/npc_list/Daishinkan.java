package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.VillageRankingService;

/** Village ranking guide; uses the existing NPC interaction and menu protocol. */
public final class Daishinkan extends Npc {
    public Daishinkan(int mapId, int status, int cx, int cy, int tempId, int avatar) {
        super(mapId, status, cx, cy, tempId, avatar);
    }

    private boolean isVillage() {
        return mapId == 0 || mapId == 7 || mapId == 14;
    }

    @Override
    public void openBaseMenu(Player player) {
        if (isVillage() && canOpenNpc(player)) {
            createOtherMenu(player, ConstNpc.BASE_MENU, "Hãy xem thứ hạng của các chiến binh.",
                    "Top sức mạnh", "Top nhiệm vụ", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (!isVillage() || !canOpenNpc(player) || !player.idMark.isBaseMenu()) {
            return;
        }
        switch (select) {
            case 0 -> VillageRankingService.gI().showPower(player);
            case 1 -> VillageRankingService.gI().showTask(player);
            default -> { }
        }
    }
}
