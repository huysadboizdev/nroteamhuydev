package nro.models.services;
import java.lang.reflect.*;
import java.util.*;
import nro.models.player.*;
import nro.models.item.Item;
import nro.models.network.MySession;
import nro.models.managers.GiftCodeManager;
import nro.models.shop.*;
import nro.models.shop_ky_gui.ConsignTransactions;
import nro.models.services_func.Trade;
import static nro.models.services.PinkFixture.*;

/** Pending web commits cannot be crossed by regular game resource mutations. */
public final class LiveSyncMutationGateTest {
    static Player player(long id)throws Exception {
        Player p=new Player();p.id=id;p.isPlayer=true;p.session=blank(MySession.class);p.session.userId=(int)id;p.session.player=p;p.session.actived=true;
        p.inventory.itemsBag.add(new Item());p.inventory.gold=10;p.inventory.ruby=1000;return p;
    }
    public static void main(String[] args)throws Exception {
        install();replace(InventoryService.class,"I",new InventoryService());Player p=player(101),other=player(102);
        LiveSyncCommandGate.mark(p.id,p.session.userId);
        try {
            switch(args[0]) {
                case "pickup" -> check(!InventoryService.gI().addItemBag(p,item(500,1)),"Pending web command allowed pickup");
                case "deduct" -> {Item owned=item(500,10);p.inventory.itemsBag.set(0,owned);InventoryService.gI().subQuantityItemsBag(p,owned,1);check(owned.quantity==10,"Pending web command allowed item deduction");}
                case "remove" -> {Item owned=item(500,10);p.inventory.itemsBag.set(0,owned);InventoryService.gI().removeItemBag(p,0);check(p.inventory.itemsBag.get(0)==owned,"Pending web command removed a bag item");}
                case "move" -> {Item owned=item(500,10);p.inventory.itemsBag.set(0,owned);p.inventory.itemsBox.add(new Item());InventoryService.gI().itemBagToBox(p,0);check(p.inventory.itemsBag.get(0)==owned&&p.inventory.itemsBox.get(0).template==null,"Pending web command moved bag item");}
                case "shop" -> {Shop s=new Shop();s.tagName="fixture";TabShop t=new TabShop();t.shop=s;s.tabShops.add(t);ItemShop row=new ItemShop();row.temp=template(500);row.cost=1;row.tabShop=t;t.itemShops.add(row);p.idMark.setShopOpen(s);p.idMark.setTagNameShop(s.tagName);ShopService.gI().buyItem(p,500);check(p.inventory.gold==10&&p.inventory.itemsBag.get(0).template==null,"Pending web command allowed shop purchase");}
                case "giftcode" -> {int[] calls={0};replace(GiftCodeManager.class,"instance",new GiftCodeManager(){@Override public nro.models.player_system.GiftCode checkUseGiftCode(Player player,String code){calls[0]++;return null;}});GiftCodeService.gI().giftCode(p,"fixture");check(calls[0]==0,"Pending web command consumed giftcode");}
                case "consign" -> {var service=new ConsignTransactions(new nro.models.database.ConsignDAO(),x->true);Method validate=ConsignTransactions.class.getDeclaredMethod("validate",Player.class);validate.setAccessible(true);check(validate.invoke(service,p)!=null,"Pending web command allowed consignment");}
                case "ruby" -> {p.inventory.itemsBag.add(item(nro.models.consts.ConstItem.VE_TANG_HONG_NGOC,1));var service=new RubyGiftService(new nro.models.database.RubyGiftDAO(),x->true);check(!service.prepare(p,other,100).success(),"Pending web command allowed ruby gift");}
                case "wallet" -> {int[] dbCalls={0};var service=new WalletPurchaseService(new nro.models.database.KolRewardDAO(()->{dbCalls[0]++;throw new java.sql.SQLException("fixture");}));check(service.purchase(p,"fixture",1,()->{})==WalletPurchaseService.Result.PENDING&&dbCalls[0]==0,"Pending web command reached wallet DB");}
                case "trade" -> {try{new Trade(p,other);throw new AssertionError("Pending web command allowed trade");}catch(IllegalStateException expected){}}
                case "account" -> {LiveSyncCommandGate.clear(p.id,p.session.userId);LiveSyncCommandGate.mark(0,p.session.userId);check(!InventoryService.gI().addItemBag(p,item(500,1)),"Pending account allowed pickup");}
                case "pet" -> {Pet pet=new Pet(p);Item owned=item(500,10);pet.inventory.itemsBody.add(owned);InventoryService.gI().subQuantityItemsBody(pet,owned,1);check(owned.quantity==10,"Pending master allowed pet item deduction");}
                default -> throw new AssertionError(args[0]);
            }
        }finally{LiveSyncCommandGate.clear(p.id,p.session.userId);}
        System.out.println("LiveSyncMutationGateTest PASS "+args[0]);
    }
}
