package nro.models.player;
import java.lang.reflect.*;
import java.util.*;
import nro.models.item.Item;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.*;
import nro.models.services_func.UseItem;
import nro.models.shop.*;

public final class StackPurchaseTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static Item item(int id,int qty){Item i=new Item((short)id);i.quantity=qty;i.itemOptions.add(new Item.ItemOption(73,0));return i;}
 static Player buyer(int id,boolean stack){Player p=new Player();p.isPlayer=true;p.inventory.gem=1000;Template.ItemTemplate t=Manager.ITEM_TEMPLATES.get(id);t.isUpToUp=stack;Shop s=new Shop();s.tagName="SANTA";s.typeShop=0;TabShop tab=new TabShop();tab.id=17;tab.shop=s;s.tabShops.add(tab);ItemShop is=new ItemShop();is.temp=t;is.cost=100;is.typeSell=1;is.tabShop=tab;tab.itemShops.add(is);p.idMark.setShopOpen(s);p.inventory.itemsBag.add(item(id,5));return p;}
 public static void main(String[]args)throws Exception{
  for(int id=0;id<2001;id++){Template.ItemTemplate t=new Template.ItemTemplate();t.id=(short)id;t.name="Item "+id;t.type=27;t.gender=3;t.description="Test";t.isUpToUp=true;Manager.ITEM_TEMPLATES.add(t);}
  Manager.ITEM_TEMPLATES.get(1628).type=29;
  for(int id=0;id<256;id++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"Option #",0));
  Field f=Service.class.getDeclaredField("instance");f.setAccessible(true);f.set(null,new Service(){@Override public void sendThongBao(Player p,String s){} @Override public void point(Player p){} @Override public void sendMoney(Player p){}});
  switch(args[0]){
   case "santa":case "materials": {int[] ids=args[0].equals("santa")?new int[]{1628,1532,1652,1653}:new int[]{1072,1073,1085,1086,1143,1608,1609,1775};for(int id:ids){Manager.ITEM_TEMPLATES.get(id).isUpToUp=false;List<Item> bag=new ArrayList<>(List.of(item(id,99)));Item in=item(id,999901);check(InventoryService.gI().addItemList(bag,in),"Consumable should merge without free slot: "+id);check(bag.size()==1&&bag.get(0).quantity==1000000,"Complete million-item stack: "+id);}break;}
   case "full": {Player p=buyer(1628,true);ShopService.gI().buyItem(p,1628);check(p.inventory.itemsBag.get(0).quantity==6&&p.inventory.gem==900,"Full bag can buy into existing stack and charge once");break;}
   case "funds": {Player p=buyer(1628,true);p.inventory.gem=99;ShopService.gI().buyItem(p,1628);check(p.inventory.itemsBag.get(0).quantity==5&&p.inventory.gem==99,"Insufficient funds never change stack");break;}
   case "unique": {Player p=buyer(933,false);Manager.ITEM_TEMPLATES.get(933).type=5;ShopService.gI().buyItem(p,933);check(p.inventory.itemsBag.get(0).quantity==5&&p.inventory.gem==1000,"Full bag cannot buy nonstack equipment or debit money");break;}
   case "options": {Item a=item(933,10),b=item(933,1);a.itemOptions.clear();b.itemOptions.clear();a.itemOptions.add(new Item.ItemOption(50,5));a.itemOptions.add(new Item.ItemOption(77,10));b.itemOptions.add(new Item.ItemOption(77,10));b.itemOptions.add(new Item.ItemOption(50,5));List<Item> bag=new ArrayList<>(List.of(a));check(InventoryService.gI().addItemList(bag,b)&&a.quantity==11,"Equivalent option order must merge");b=item(933,1);b.itemOptions.clear();b.itemOptions.add(new Item.ItemOption(50,6));b.itemOptions.add(new Item.ItemOption(77,10));check(!InventoryService.gI().addItemList(bag,b)&&a.quantity==11,"Different stats must stay separate");break;}
   case "expiry": {Item a=item(933,10),b=item(933,1);a.itemOptions.add(new Item.ItemOption(93,30));b.itemOptions.add(new Item.ItemOption(93,30));a.createTime=System.currentTimeMillis()-86400000;List<Item> bag=new ArrayList<>(List.of(a));check(!InventoryService.gI().addItemList(bag,b)&&a.quantity==10&&b.quantity==1,"Different expiration timestamps must not merge");break;}
   case "time": {Player p=buyer(1628,true);Item i=p.inventory.itemsBag.get(0);p.itemTime.isUseBuaSanta=true;p.itemTime.lastTimeBuaSanta=System.currentTimeMillis()-3600000;Method m=UseItem.class.getDeclaredMethod("useItemTime",Player.class,Item.class);m.setAccessible(true);m.invoke(UseItem.gI(),p,i);long expiry=p.itemTime.lastTimeBuaSanta+nro.models.item.ItemTime.TIME_BUA_SANTA;check(expiry-System.currentTimeMillis()>1799000,"Stale expired charm flag must start a fresh 30 minutes");m.invoke(UseItem.gI(),p,i);check(p.itemTime.lastTimeBuaSanta+nro.models.item.ItemTime.TIME_BUA_SANTA-System.currentTimeMillis()>3599000,"Second use adds another 30 minutes");break;}
   default:throw new AssertionError(args[0]);
  }
  System.out.println("PASS: Stack purchase "+args[0]);
 }
}
