package nro.models.shop;

import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.services.*;
import static nro.models.services.PinkFixture.*;

public final class RubyFoodShopTest {
 static int food=880;
 static Player buyer(int ruby,int price)throws Exception {
  Player p=new Player();p.isPlayer=true;p.inventory.ruby=ruby;p.inventory.gem=100;p.inventory.gold=100;
  for(int i=0;i<3;i++)p.inventory.itemsBag.add(new Item());
  Shop s=new Shop();s.tagName="SANTA_HONG_NGOC";s.typeShop=3;s.npcId=39;
  TabShop t=new TabShop();t.id=64;t.shop=s;s.tabShops.add(t);
  ItemShop r=new ItemShop();r.temp=Manager.ITEM_TEMPLATES.get(food);r.cost=price;r.typeSell=3;r.iconSpec=7743;r.tabShop=t;t.itemShops.add(r);
  p.idMark.setShopOpen(s);p.idMark.setTagNameShop(s.tagName);return p;
 }
 static int count(Player p){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==food).mapToInt(i->i.quantity).sum();}
 public static void main(String[]args)throws Exception {
  install();for(int id=0;id<=2000;id++){var t=template(id);t.type=29;t.gender=3;Manager.ITEM_TEMPLATES.add(t);}
  replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}});
  for(food=880;food<=882;food++){
  Player p=buyer(10,5);ShopService.gI().takeItem(p,(byte)0,food);ShopService.gI().takeItem(p,(byte)0,food);
  check(p.inventory.ruby==0&&count(p)==2&&p.inventory.gold==100&&p.inventory.gem==100,"Two food purchases must cost exactly ten ruby: "+food);
  ShopService.gI().takeItem(p,(byte)0,food);check(count(p)==2,"Insufficient ruby keeps inventory");
  p=buyer(4,5);ShopService.gI().takeItem(p,(byte)0,food);check(p.inventory.ruby==4&&count(p)==0,"Other wallets cannot substitute");
  p=buyer(10,1);ShopService.gI().takeItem(p,(byte)0,food);check(p.inventory.ruby==10&&count(p)==0,"Reject forged food price");
  p=buyer(10,5);p.inventory.itemsBag.clear();p.inventory.itemsBag.add(item(999,1));ShopService.gI().takeItem(p,(byte)0,food);check(p.inventory.ruby==10&&count(p)==0,"Full bag keeps ruby");
  }
  System.out.println("PASS RubyFoodShop");System.exit(0);
 }
}
