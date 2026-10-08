package nro.models.services;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import nro.models.item.CollectionChestRules;
import nro.models.item.Item;
import nro.models.map.Zone;
import nro.models.network.Message;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.utils.Util;

/** A separate inventory, using explicit transfer types so stale panels cannot touch the home box. */
public final class CollectionChestService {
    public static final int BAG_TO_COLLECTION=11, BODY_TO_COLLECTION=12, COLLECTION_TO_BAG=13;
    private record Access(Npc npc,Zone zone,long opened) {}
    private static final Map<Player,Access> access=Collections.synchronizedMap(new WeakHashMap<>());
    private CollectionChestService() {}

    public static boolean open(Player player,Npc npc) {
        if(player==null) return false;
        synchronized(player) {
            if(!near(player,npc) || blocked(player)) return false;
            access.put(player,new Access(npc,player.zone,System.currentTimeMillis()));
            send(player,true);
            return true;
        }
    }

    private static boolean near(Player p,Npc npc) {
        return p!=null && p.isPlayer && !p.isPet && p.zone!=null && p.zone.map!=null && p.location!=null
                && npc!=null && npc.tempId==82 && npc.mapId==p.zone.map.mapId
                && Util.getDistance(npc.cx,npc.cy,p.location.x,p.location.y)<=120;
    }

    private static boolean blocked(Player p) {
        return LiveSyncMutationGate.blocked(p) || CommesonChallengeService.hasPendingId(p.id)
                || nro.models.shop_ky_gui.ConsignTransactions.hasPending(p)
                || RubyGiftService.hasPending(p) || WalletPurchaseService.hasPendingId(p.id)
                || p.walletPurchasePending != null || p.kolProgressPending || p.kolRewardPending
                || p.beforeDispose;
    }

    private static int empty(List<Item> list) {
        if(list!=null)for(int n=0;n<list.size();n++)if(list.get(n)!=null&&!list.get(n).isNotNullItem())return n;
        return -1;
    }

    public static boolean transfer(Player player,int type,int slot) {
        if(player==null)return false;
        synchronized(player) {
            Access context=access.get(player);
            if(context==null || context.zone()!=player.zone || !near(player,context.npc())
                    || System.currentTimeMillis()-context.opened()>600_000L || blocked(player)) return false;
            List<Item> chest=player.inventory.itemsCollection;
            if(chest==null || chest.size()!=CollectionChestRules.CAPACITY)return false;
            List<Item> source=type==BAG_TO_COLLECTION?player.inventory.itemsBag:
                    type==BODY_TO_COLLECTION?player.inventory.itemsBody:type==COLLECTION_TO_BAG?chest:null;
            if(source==null||slot<0||slot>=source.size())return false;
            Item item=source.get(slot);
            if(item==null||!item.isNotNullItem()||item.quantity<=0||item.quantityGD>0)return false;
            List<Item> target=type==COLLECTION_TO_BAG?player.inventory.itemsBag:chest;
            int targetSlot=empty(target);
            if(targetSlot<0){Service.gI().sendThongBao(player,type==COLLECTION_TO_BAG?"Hành trang đã đầy.":"Rương sưu tầm đã đủ 20 ô.");return false;}
            if(type!=COLLECTION_TO_BAG&&!CollectionChestRules.permanent(item)){
                Service.gI().sendThongBao(player,"Rương sưu tầm chỉ nhận vật phẩm vĩnh viễn, không có hạn sử dụng.");return false;
            }
            if(java.util.Collections.frequency(source,item)!=1 || chest.contains(item) && source!=chest)return false;
            Item moved=item;
            if(item.quantity>1) {
                if(type==COLLECTION_TO_BAG)return false;
                moved=InventoryService.gI().copyList(List.of(item)).get(0);moved.quantity=1;
                moved.id=item.id;moved.expire=item.expire;item.quantity--;
            } else source.set(slot,new Item());
            target.set(targetSlot,moved);
            if(type==BODY_TO_COLLECTION) {
                if(slot==12)Service.gI().sendPetFollow(player,(short)0);
                if(slot==7 && item.template.type!=25 && player.newPet!=null) {
                    nro.models.map.service.ChangeMapService.gI().exitMap(player.newPet);
                    player.newPet.dispose();player.newPet=null;
                }
            }
            if(type==BODY_TO_COLLECTION && player.setClothes!=null)player.setClothes.setup();
            Service.gI().point(player);
            InventoryService.gI().sendItemBags(player);
            if(type==BODY_TO_COLLECTION){InventoryService.gI().sendItemBody(player);Service.gI().Send_Caitrang(player);Service.gI().sendFlagBag(player);Service.gI().sendSpeedPlayer(player,-1);}
            send(player,false);
            return true;
        }
    }

    private static void send(Player player,boolean open) {
        Message data=null,show=null;
        try {
            data=new Message(-35);data.writer().writeByte(0);data.writer().writeByte(CollectionChestRules.CAPACITY);
            for(Item item:player.inventory.itemsCollection) {
                data.writer().writeShort(item.isNotNullItem()?item.template.id:-1);
                if(!item.isNotNullItem())continue;
                data.writer().writeInt(item.quantity);data.writer().writeUTF(item.getInfo());data.writer().writeUTF(item.getContent());
                data.writer().writeByte(item.itemOptions.size());
                for(Item.ItemOption option:item.itemOptions){data.writer().writeByte(option.optionTemplate.id);nro.models.network.ItemOptionWire.write(data,player,option.param);}
            }
            player.sendMessage(data);
            if(open){show=new Message(-35);show.writer().writeByte(1);show.writer().writeByte(2);player.sendMessage(show);}
        }catch(Exception failure){nro.models.utils.Logger.logException(CollectionChestService.class,failure);}
        finally{if(data!=null)data.cleanup();if(show!=null)show.cleanup();}
    }
}
