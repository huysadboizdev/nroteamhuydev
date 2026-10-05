package nro.models.npc_list;

import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.map.service.NpcService;
import nro.models.shop_ky_gui.ConsignShopService;

public class KyGui extends Npc {

    public KyGui(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            createOtherMenu(player, 0,
                    "Siêu thị chỉ mua bán bằng hồng ngọc. Phí đăng 1 hồng ngọc, lên đầu 5 hồng ngọc, phí bán 10%.",
                    "Hướng\ndẫn\nthêm", "Mua bán\nKý gửi", "Từ chối");
        }
    }

    @Override
    public void confirmMenu(Player pl, int select) {
        if (canOpenNpc(pl)) {
            switch (select) {
                case 0 ->
                    NpcService.gI().createTutorial(pl, tempId, avartar,
                            "Chỉ ký gửi bằng hồng ngọc, chọn Ký gửi ngọc.\bMua đồ: tài khoản đã kích hoạt, sức mạnh từ 17 tỷ, đủ hồng ngọc và chỗ trong hành trang; không mua đồ tự đăng.\bGiá 1–1.000.000 hồng ngọc, tối đa 99 vật phẩm.\bPhí đăng 1 hồng ngọc, lên đầu 5 hồng ngọc, phí bán 10%.\bTin đăng cũ bằng vàng/ngọc xanh chỉ được chủ lấy lại đồ.");
                case 1 -> {
                    if (pl.getSession().actived) {
                        ConsignShopService.gI().openShopKyGui(pl);
                        return;
                    }
                    this.npcChat(pl, "Bạn chưa kích hoạt thành viên!!!");
                }
            }
        }
    }
}
