package nro.models.services;

import java.lang.ref.WeakReference;
import java.util.*;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.network.MySession;
import nro.models.map.service.NpcService;
import nro.models.shop_ky_gui.ConsignTransactions;

public final class DivineEquipmentBoxService {
    public static final int MENU=17750;
    private static final Map<Player,Selection> selections=Collections.synchronizedMap(new WeakHashMap<>());
    private record Selection(WeakReference<Item> box,WeakReference<MySession> session,long time) {}
    private static final int[][] IDS={{555,556,562,563,561},{557,558,564,565,561},{559,560,566,567,561}};
    private DivineEquipmentBoxService() {}
    public static List<Item> createSet(int planet) {
        if(planet<0||planet>2)throw new IllegalArgumentException("Invalid planet");
        List<Item> rewards=new ArrayList<>();
        for(int id:IDS[planet]) {
            Item item=ItemService.gI().createNewItem((short)id);
            item.itemOptions.clear();
            RewardService.gI().initChiSoItemBase(item);
            item.itemOptions.add(new Item.ItemOption(30,1));
            rewards.add(item);
        }
        return rewards;
    }
    public static void show(Player p,Item box) {
        synchronized(p) {
            if(p.session==null||box==null||box.template==null||box.template.id!=1775||box.quantity<1
                    ||!p.inventory.itemsBag.contains(box)||AdminPlayerActions.hasPending(p))return;
            selections.put(p,new Selection(new WeakReference<>(box),new WeakReference<>(p.session),System.currentTimeMillis()));
            NpcService.gI().createMenuConMeo(p,MENU,-1,
                    "Chọn set Thần Linh",
                    "Set Thần Linh\nNamếc","Set Thần Linh\nTrái Đất","Set Thần Linh\nXayda","Đóng");
        }
    }
    public static void choose(Player p,int select) {
        synchronized(p) {
            Selection choice=selections.remove(p);
            if(select<0||select>2)return;
            if(choice==null||choice.session.get()!=p.session||System.currentTimeMillis()-choice.time>120_000) {
                Service.gI().sendThongBao(p,"Lựa chọn hết hạn. Hãy mở lại hộp.");return;
            }
            Item box=choice.box.get();
            if(box==null){Service.gI().sendThongBao(p,"Hộp không còn hợp lệ.");return;}
            int planet=select==0?1:select==1?0:2;
            var result=new ConsignTransactions().redeemDivineBox(p,box,planet);
            Service.gI().sendThongBao(p,result.message());
            InventoryService.gI().sendItemBags(p);
        }
    }
}
