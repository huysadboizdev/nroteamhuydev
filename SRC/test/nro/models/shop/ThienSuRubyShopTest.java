package nro.models.shop;
import java.util.*;
import nro.models.player.*;
import nro.models.item.Item;
import nro.models.services.*;
import nro.models.server.Manager;
import static nro.models.services.PinkFixture.*;
public final class ThienSuRubyShopTest {
 static Player buyer(int ruby)throws Exception{
  Player p=new Player();p.isPlayer=true;p.inventory.ruby=ruby;p.inventory.gold=100000;p.inventory.gem=100000;
  for(int i=0;i<20;i++)p.inventory.itemsBag.add(new Item());
  Shop s=new Shop();s.tagName="THIEN_SU";s.typeShop=3;s.npcId=56;TabShop t=new TabShop();t.id=37;t.shop=s;t.name="Cửa<>hàng";s.tabShops.add(t);
  for(int id=1071;id<=1086;id++){ItemShop r=new ItemShop();r.id=id;r.temp=template(id);r.temp.gender=3;r.tabShop=t;r.cost=10;r.typeSell=3;r.iconSpec=7743;t.itemShops.add(r);}
  p.idMark.setShopOpen(s);p.idMark.setTagNameShop(s.tagName);return p;
 }
 static int count(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToInt(i->i.quantity).sum();}
 public static void main(String[] args){try{
  install();for(int id=0;id<=1100;id++){var t=template(id);t.iconID=(short)(id==861?7743:0);Manager.ITEM_TEMPLATES.add(t);}
  replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}});
  String mode=args[0];
  if(mode.equals("wallet")){for(int id=1071;id<=1086;id++){Player p=buyer(10);p.inventory.gem=0;ShopService.gI().buyItem(p,id);check(p.inventory.ruby==0&&count(p,id)==1&&p.inventory.gold==100000&&p.inventory.gem==0,"exact ruby wallet purchase "+id);}}
  else if(mode.equals("funds")){Player p=buyer(9);ShopService.gI().buyItem(p,1083);check(p.inventory.ruby==9&&count(p,1083)==0&&p.inventory.gold==100000&&p.inventory.gem==100000,"other wallets cannot cover ruby");}
  else if(mode.equals("full")){Player p=buyer(10);p.inventory.itemsBag.clear();p.inventory.itemsBag.add(item(500,1));ShopService.gI().buyItem(p,1071);check(p.inventory.ruby==10&&count(p,1071)==0,"full bag no charge");}
  else if(mode.equals("forged")){for(int i=0;i<3;i++){Player p=buyer(20);ItemShop row=p.idMark.getShopOpen().getItemShop(1071);if(i==0)row.cost=1;if(i==1)row.typeSell=1;if(i==2)row.iconSpec=4028;ShopService.gI().buyItem(p,1071);check(p.inventory.ruby==20&&count(p,1071)==0,"reject wrong price/currency "+i);}}
  else if(mode.equals("repeat")){Player p=buyer(20);for(int i=0;i<3;i++)ShopService.gI().buyItem(p,1071);check(p.inventory.ruby==0&&count(p,1071)==2,"each unit costs ten");}
  else throw new AssertionError(mode);
  System.out.println("THIEN_SU_RUBY PASS "+mode);System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
