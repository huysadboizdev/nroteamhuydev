package nro.models.services;
import java.util.*;
import java.lang.reflect.*;
import nro.models.item.Item;
import nro.models.player.*;
import nro.models.npc_list.ToriBot;
import nro.models.shop.ShopService;
import static nro.models.services.PinkFixture.*;
public final class VipRewardInteractionTest {
 static class Menu extends ToriBot {
  Menu(){super(5,0,0,0,0,0);}
  @Override public boolean canOpenNpc(Player p){return true;}
 }
 public static void main(String[] args)throws Exception {
  install();
  Player p=player(123);p.idMark=new IDMark();
  Menu menu=blank(Menu.class);
  for(int level=1;level<=4;level++){p.idMark.setIndexMenu(level);menu.confirmMenu(p,1);check(p.vipPurchaseCount==0,"Close must not purchase");}
  Method take=ShopService.class.getDeclaredMethod("getItemSideBoxLuckyRound",Player.class,List.class,byte.class,int.class);take.setAccessible(true);
  p.inventory.itemsBag.add(new Item());p.inventory.itemsBoxCrackBall.add(item(1283,1000));
  replace(InventoryService.class,"I",new InventoryService(){@Override public boolean addItemBag(Player p,Item i){return false;}});
  take.invoke(ShopService.gI(),p,p.inventory.itemsBoxCrackBall,(byte)0,0);
  check(p.inventory.itemsBoxCrackBall.size()==1&&p.inventory.itemsBoxCrackBall.get(0).quantity==1000,"failed add keeps entire chest reward");
  replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}});
  p.inventory.itemsBag.clear();p.inventory.itemsBag.add(item(1283,100));
  take.invoke(ShopService.gI(),p,p.inventory.itemsBoxCrackBall,(byte)0,0);
  check(p.inventory.itemsBoxCrackBall.isEmpty()&&p.inventory.itemsBag.get(0).quantity==1100,"full bag stacks chest reward");
  Player old=player(124);Item keep=item(933,1);keep.id=7;keep.expire=123456;keep.quantityGD=2;keep.text="original";
  keep.itemOptions.add(new Item.ItemOption(93,10));old.inventory.itemsBag.add(keep);
  for(int n=0;n<20;n++)old.inventory.itemsBag.add(new Item());
  VipRewardCatalog.prepare(1).grant(old);Item retained=old.inventory.itemsBag.get(0);
  check(retained.id==7&&retained.expire==123456&&retained.quantityGD==2&&retained.text.equals("original")&&retained.itemOptions.size()==1,"all original metadata retained");
  Player trading=player(125);trading.tradeWVP=true;
  for(int n=0;n<20;n++)trading.inventory.itemsBag.add(new Item());
  var pack=VipRewardCatalog.prepare(1);pack.grant(trading);
  check(pack.storedInChest()&&trading.inventory.itemsBoxCrackBall.size()==pack.items().size()&&trading.inventory.itemsBag.stream().noneMatch(Item::isNotNullItem),"pending trade snapshot cannot overwrite gifts");
  System.out.println("VipRewardInteractionTest PASS Close no purchase, failed chest add preserved, full bag stacking");
 }
}
