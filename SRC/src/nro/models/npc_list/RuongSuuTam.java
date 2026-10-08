package nro.models.npc_list;

import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.item.CollectionChestRules;
import nro.models.services.CollectionChestService;

public final class RuongSuuTam extends Npc {
    public RuongSuuTam(int mapId,int status,int cx,int cy,int tempId,int avatar) {
        super(mapId,status,cx,cy,tempId,avatar);
    }
    @Override public void openBaseMenu(Player player) {
        if(canOpenNpc(player))createOtherMenu(player,0,
                "Rương sưu tầm: "+CollectionChestRules.count(player.inventory)+"/20 món vĩnh viễn.\n"
                +"Đủ 20 món: tăng tổng 3% HP, KI và sức đánh. Rút bớt đồ sẽ mất thưởng.\nMỗi món chiếm 1 ô.",
                "Mở rương","Đóng");
    }
    @Override public void confirmMenu(Player player,int select) {
        if(select==0 && canOpenNpc(player))CollectionChestService.open(player,this);
    }
}
