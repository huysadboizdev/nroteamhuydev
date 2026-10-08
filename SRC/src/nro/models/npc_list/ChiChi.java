package nro.models.npc_list;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.TimedBagCleanupService;
import nro.models.server.Manager;
import nro.models.services.Service;
import nro.models.services_func.Input;
import nro.models.shop.ShopService;

/**
 *
 * @author By Mr Blue
 *
 */
public class ChiChi extends Npc {
    private static final int BASE_MENU_WITHOUT_EVENTS=1_051_000;

    public ChiChi(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        synchronized(player) {
        try(var eventPermit=nro.models.event.EventItemPolicy.admit()) {
        if(player.beforeDispose||nro.models.services.LiveSyncMutationGate.blocked(player))return;

        if (canOpenNpc(player)) {
            TimedBagCleanupService.gI().cancel(player);
            List<String> menu = new ArrayList<>(Arrays.asList(
                    "Top\nHộp quà\nthiếu nhi\n2025",
                    "Top\nNước mía",
                    "Top\nKem trái cây",
                    "Cửa hàng",
                    "Bỏ đồ\ncó hạn sử dụng",
                    "Đóng"));

            String[] menus = menu.toArray(new String[0]);

            boolean summer=nro.models.event.EventItemPolicy.isEnabled("summer");
            if(!summer)menu=menu.subList(3,menu.size());
            createOtherMenu(player, summer?ConstNpc.BASE_MENU:BASE_MENU_WITHOUT_EVENTS,
                    "Bạn muốn hỏi chi?", menu.toArray(new String[0]));
        }

    
        }}
}

    @Override
    public void confirmMenu(Player player, int select) {
        synchronized(player) {
        try(var eventPermit=nro.models.event.EventItemPolicy.admit()) {
        if(player.beforeDispose||nro.models.services.LiveSyncMutationGate.blocked(player))return;

        if (canOpenNpc(player)) {
            int soLuong = 0;
            if (this.mapId == 5) {
                if (player.idMark.isBaseMenu()||player.idMark.getIndexMenu()==BASE_MENU_WITHOUT_EVENTS) {
                    if(player.idMark.getIndexMenu()==BASE_MENU_WITHOUT_EVENTS) {
                        if(select<0||select>2)return;
                        select+=3;
                    }
                    if(select>=0&&select<=2&&!nro.models.event.EventItemPolicy.isEnabled("summer"))return;
                    switch (select) {
                        case 4:
                            synchronized (player) {
                                String text = TimedBagCleanupService.gI().prepare(player, System.currentTimeMillis());
                                if (player.idMark.getTimedBagCleanupPreview() == null) {
                                    Service.gI().sendThongBao(player, text);
                                } else {
                                    createOtherMenu(player, ConstNpc.CONFIRM_TIMED_BAG_CLEANUP,
                                            text, "Có", "Không");
                                }
                            }
                            break;
                        case 3:
                            ShopService.gI().opendShop(player, "SHOP_CHI_CHI", false);
                            break;
                        case 0:
                            createOtherMenu(player, ConstNpc.PHAO_BONG_VIP,
                                    "Sự kiện đua top Hộp quà thiếu nhi nhận quà khủng\n Kết thúc và trao giải sau (....)\nHạn chót nhận giải: (15 ngày nữa)\nĐến gặp ChiChi để nhận giải nhé\nChi tiết xem tại diễn đàn, Fanpage",
                                    "Top 100\nHộp quà\nthiếu nhi\n2025",
                                    "Xem điểm",
                                    "Đóng");
                            break;
                        case 1:
                            createOtherMenu(player, ConstNpc.PHAO_BONG,
                                    "Sự kiện đua top Nước mía nhận quà khủng\n Kết thúc và trao giải sau (....)\nHạn chót nhận giải: (15 ngày nữa)\nĐến gặp ChiChi để nhận giải nhé\nChi tiết xem tại diễn đàn, Fanpage",
                                    "Top 100\nNước mía",
                                    "Xem điểm",
                                    "Đóng");
                            break;
                        case 2:
                            createOtherMenu(player, ConstNpc.GOKU_DAY,
                                    "Sự kiện đua top Kem trái cây nhận quà khủng\n Kết thúc và trao giải sau (....)\nHạn chót nhận giải: (15 ngày nữa)\nĐến gặp ChiChi để nhận giải nhé\nChi tiết xem tại diễn đàn, Fanpage",
                                    "Top 100\nKem trái cây",
                                    "Xem điểm",
                                    "Đóng");
                            break;
                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.CONFIRM_TIMED_BAG_CLEANUP) {
                    synchronized (player) {
                        player.idMark.setIndexMenu(ConstNpc.BASE_MENU);
                        if (select == 0) {
                            Service.gI().sendThongBao(player,
                                    TimedBagCleanupService.gI().confirm(player, System.currentTimeMillis()));
                        } else {
                            TimedBagCleanupService.gI().cancel(player);
                        }
                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.PHAO_BONG_VIP) {
                    if(!nro.models.event.EventItemPolicy.isEnabled("summer"))return;
                    switch (select) {
                        case 0:
                            Service.gI().showListTop(player, Manager.Topsukien);
                            break;
                        case 1:
                            Service.gI().sendThongBao(player, "Bạn có " + player.point_sukien + " điểm Hộp quà thiếu nhi.");
                            break;
                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.PHAO_BONG) {
                    if(!nro.models.event.EventItemPolicy.isEnabled("summer"))return;
                    switch (select) {
                        case 0:
                            Service.gI().showListTop(player, Manager.Topsukien1);
                            break;
                        case 1:
                            Service.gI().sendThongBao(player, "Bạn có " + player.point_sukien1 + " điểm Nước mía.");
                            break;
                    }
                } else if (player.idMark.getIndexMenu() == ConstNpc.GOKU_DAY) {
                    if(!nro.models.event.EventItemPolicy.isEnabled("summer"))return;
                    switch (select) {
                        case 0:
                            Service.gI().showListTop(player, Manager.Topsukien2);
                            break;
                        case 1:
                            Service.gI().sendThongBao(player, "Bạn có " + player.point_sukien2 + " điểm Kem trái cây.");
                            break;
                    }
                }
            }
        }
    
        }}
}
}
