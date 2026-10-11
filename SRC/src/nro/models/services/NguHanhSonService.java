package nro.models.services;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.consts.ConstNpc;
import nro.models.map.service.ChangeMapService;

/** Random timed Tay Du costumes paid with the existing region's peach drops. */
public final class NguHanhSonService {
    private NguHanhSonService() {}
    public record Offer(int itemId, String name, int currency, int price) {}
    public static final List<Offer> OFFERS=List.of(
        new Offer(544,"Ngộ Không Trái Đất",541,200),
        new Offer(545,"Ngộ Không Namec",541,200),
        new Offer(546,"Ngộ Không Xayda",541,200),
        new Offer(547,"Ngộ Không đệ tử",542,150),
        new Offer(548,"Bát Giới đệ tử",542,150),
        new Offer(1698,"Urôn Trư Bát Giới",542,300));

    public static boolean isVillage(int id){return id==0||id==7||id==14;}
    public static boolean ownsOffer(int npcId,int itemId){
        return npcId==ConstNpc.NGO_KHONG ? itemId>=544&&itemId<=547
            : npcId==ConstNpc.DUONG_TANG && (itemId==548||itemId==1698);
    }
    public static String priceLabel(Offer o){
        return o.name()+"\n"+o.price()+" "+(o.currency()==541?"Hồng Đào":"Đào Chín");
    }
    public static boolean exchange(Player player,int itemId){
        Offer offer=OFFERS.stream().filter(o->o.itemId()==itemId).findFirst().orElse(null);
        if(offer==null||player==null||player.zone==null||player.zone.map.mapId!=124||player.isPet||player.isBoss)return false;
        try(var permit=nro.models.event.EventItemPolicy.admit()){
            synchronized(LiveSyncMutationGate.owner(player)){
                if(LiveSyncMutationGate.blocked(player)||CommesonChallengeService.hasPendingId(player.id)
                    ||RubyGiftService.hasPending(player)||nro.models.shop_ky_gui.ConsignTransactions.hasPending(player))return false;
                long total=0;
                for(Item i:player.inventory.itemsBag)if(i.isNotNullItem()&&i.template.id==offer.currency()&&i.quantity>0)total+=i.quantity;
                if(total<offer.price()){
                    Service.gI().sendThongBao(player,"Cần "+offer.price()+" "+(offer.currency()==541?"Quả Hồng Đào":"Quả Hồng Đào Chín"));return false;
                }
                Item reward=ItemService.gI().createNewItem((short)itemId);
                reward.itemOptions.clear();
                for(int option:new int[]{50,94,103,77,101,100,114}){
                    int cap=option==50?30:option==94?50:100;
                    reward.itemOptions.add(new Item.ItemOption(option,ThreadLocalRandom.current().nextInt(1,cap+1)));
                }
                reward.itemOptions.add(new Item.ItemOption(106,0));
                int duration=ThreadLocalRandom.current().nextInt(3,8);
                reward.itemOptions.add(new Item.ItemOption(93,duration));
                reward.itemOptions.add(new Item.ItemOption(174,2026));
                // Existing template gender governs which ordinary Wukong variant can be worn.
                reward.itemOptions.add(new Item.ItemOption(73,0));
                InventoryService inventory=InventoryService.gI();
                if(!inventory.canAddItemBag(player,reward)){
                    Service.gI().sendThongBao(player,"Hãy để trống 1 ô hành trang để nhận cải trang");return false;
                }
                if(!inventory.addItemBag(player,reward))return false;
                int remaining=offer.price();
                for(int slot=0;slot<player.inventory.itemsBag.size()&&remaining>0;slot++){
                    Item i=player.inventory.itemsBag.get(slot);
                    if(i.isNotNullItem()&&i.template.id==offer.currency()&&i.quantity>0){
                        int used=Math.min(remaining,i.quantity);
                        inventory.subQuantityItem(player.inventory.itemsBag,i,used);remaining-=used;
                    }
                }
                inventory.sendItemBags(player);
                Service.gI().sendThongBao(player,"Nhận "+offer.name()+" chỉ số ngẫu nhiên, hạn dùng "+duration+" ngày");
                return true;
            }
        }
    }
    public static void enter(Player player){
        if(player.zone!=null&&isVillage(player.zone.map.mapId)&&!player.isDie())
            ChangeMapService.gI().changeMapNonSpaceship(player,122,60,288);
    }
    public static void returnVillage(Player player){
        if(player.zone==null||player.zone.map.mapId<122||player.zone.map.mapId>124||player.isDie())return;
        int id=player.gender==0?0:player.gender==1?7:14;
        ChangeMapService.gI().changeMapNonSpaceship(player,id,id==0?672:id==7?689:478,id==14?408:432);
    }
}
