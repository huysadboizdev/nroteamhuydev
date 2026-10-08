package nro.models.shop;
import java.util.*;
import nro.models.player.*;
import nro.models.item.Item;
import nro.models.services.*;
import static nro.models.services.PinkFixture.*;
public final class BeerusDestroyerTradeTest {
 static String notice=""; static String grant="ok";
 static final int[] DIVINE={555,556,557,558,559,560,561,562,563,564,565,566,567};
 static int type(int id){return switch(id){case 555,557,559,650,652,654->0;case 556,558,560,651,653,655->1;case 562,564,566,657,659,661->2;case 563,565,567,658,660,662->3;default->4;};}
 static int gender(int id){return switch(id){case 555,556,562,563,650,651,657,658->0;case 557,558,564,565,652,653,659,660->1;case 559,560,566,567,654,655,661,662->2;default->3;};}
 static Item gear(int id){Item i=item(id,1);i.template.type=(byte)type(id);i.template.gender=(byte)gender(id);i.template.level=(byte)(id<650?13:14);i.template.isUpToUp=false;return i;}
 static Player buyer(int id)throws Exception{Player p=new Player();p.gender=(byte)(gender(id)==3?0:gender(id));p.isPlayer=true;p.inventory.ruby=2000;p.inventory.gold=5000;p.inventory.gem=4000;
  p.inventory.itemsBody.clear();for(int j=0;j<7;j++)p.inventory.itemsBody.add(new Item());for(int d:DIVINE)if(gender(d)==p.gender||gender(d)==3)p.inventory.itemsBody.set(type(d),gear(d));
  p.inventory.itemsBag.clear();for(int j=0;j<8;j++)p.inventory.itemsBag.add(new Item());p.inventory.itemsBag.set(0,item(663,198));
  Shop s=new Shop();s.tagName="BILL";s.npcId=55;s.typeShop=3;TabShop t=new TabShop();t.shop=s;s.tabShops.add(t);for(int v=650;v<=662;v++){ItemShop r=new ItemShop();r.id=v;r.temp=gear(v).template;r.cost=100;r.typeSell=3;r.iconSpec=7743;r.tabShop=t;r.options.add(new Item.ItemOption(47,1600));t.itemShops.add(r);}p.idMark.setShopOpen(s);p.idMark.setTagNameShop("BILL");return p;
 }
 static int count(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i!=null&&i.isNotNullItem()&&i.template.id==id).mapToInt(i->i.quantity).sum();}
 static void buy(Player p,int id){ShopService.gI().takeItem(p,(byte)3,id);}
 static void unchanged(Player p,int id){check(p.inventory.ruby==2000&&count(p,663)==198&&count(p,id)==0,"failed trade must preserve materials and wallets");}
 public static void main(String[] a){try{install();replace(Service.class,"instance",new Service(){public void sendThongBao(Player p,String s){notice=s;}public void sendMoney(Player p){}public void point(Player p){}public void Send_Caitrang(Player p){}});
  replace(InventoryService.class,"I",new InventoryService(){public void sendItemBody(Player p){}public void sendItemBags(Player p){}public boolean addItemBag(Player p,Item i){if(grant.equals("false")){p.inventory.itemsBag.get(0).quantity=123;return false;}if(grant.equals("throw")){p.inventory.itemsBag.get(0).quantity=123;throw new IllegalStateException("injected grant failure");}return super.addItemBag(p,i);}});
  String m=a[0];
  if(m.equals("success")){for(int id=650;id<=662;id++){Player p=buyer(id);buy(p,id);check(p.inventory.ruby==1900&&count(p,663)==99&&count(p,id)==1,"exact charge and reward "+id);check(!p.inventory.itemsBody.get(type(id)).isNotNullItem(),"matching equipped divine consumed "+id);check(p.inventory.gold==5000&&p.inventory.gem==4000,"other wallets unchanged");}}
  else if(m.equals("repeat")){Player p=buyer(650);for(int n=0;n<20;n++)buy(p,650);check(count(p,650)==1&&p.inventory.ruby==1900&&count(p,663)==99,"only one of twenty trades passes");check(notice.equals("Thiếu 1 món Thần trên người, không được mua."),"exact missing-one notice");p.inventory.itemsBody.set(0,gear(555));buy(p,650);check(count(p,650)==2&&p.inventory.ruby==1800,"re-equipped full set can buy again");}
  else if(m.equals("funds")){Player p=buyer(650);p.inventory.ruby=99;buy(p,650);check(p.inventory.ruby==99&&count(p,663)==198&&p.inventory.itemsBody.get(0).isNotNullItem(),"insufficient ruby no consume");}
  else if(m.equals("food")){Player p=buyer(650);p.inventory.itemsBag.get(0).quantity=98;buy(p,650);check(p.inventory.ruby==2000&&count(p,663)==98&&count(p,650)==0,"insufficient food no charge");}
  else if(m.equals("divine")){for(int bad=0;bad<5;bad++){Player p=buyer(650);Item i=p.inventory.itemsBody.get(0);if(bad==0)i.quantity=0;if(bad==1)i.template.level=12;if(bad==2)i.template.type=1;if(bad==3)i.template.gender=2;if(bad==4)i.expire=System.currentTimeMillis()-1;buy(p,650);unchanged(p,650);}}
  else if(m.equals("forged")){for(int bad=0;bad<5;bad++){Player p=buyer(650);Shop s=p.idMark.getShopOpen();ItemShop r=s.getItemShop(650);if(bad==0)s.npcId=56;if(bad==1)s.typeShop=0;if(bad==2)r.cost=1;if(bad==3)r.iconSpec=0;if(bad==4)r.typeSell=0;buy(p,650);unchanged(p,650);}}
  else if(m.equals("gender")){Player p=buyer(650);buy(p,652);unchanged(p,652);}
  else if(m.equals("rollback")){for(String g:List.of("false","throw")){Player p=buyer(650);Item before=p.inventory.itemsBody.get(0);before.id=321;before.quantityGD=7;before.text="metadata";before.expire=Long.MAX_VALUE;before.itemOptions.add(new Item.ItemOption(77,3));grant=g;buy(p,650);unchanged(p,650);Item after=p.inventory.itemsBody.get(0);check(after==before&&after.template.id==555&&after.quantity==1&&after.id==321&&after.quantityGD==7&&after.expire==Long.MAX_VALUE&&after.itemOptions.size()==1,"body identity and metadata restored");}grant="ok";}
  else if(m.equals("capacity")){Player p=buyer(650);for(int n=1;n<8;n++)p.inventory.itemsBag.set(n,item(500+n,1));buy(p,650);unchanged(p,650);p.inventory.itemsBag.get(0).quantity=99;buy(p,650);check(p.inventory.ruby==1900&&count(p,650)==1,"consumed food frees bag slot");}
  else if(m.equals("split")){Player p=buyer(650);p.inventory.itemsBag.get(0).quantity=50;p.inventory.itemsBag.set(1,item(663,49));buy(p,650);check(count(p,663)==0&&count(p,650)==1,"same food ID aggregates");}
  else if(m.equals("expiredfood")){Player p=buyer(650);Item expired=p.inventory.itemsBag.get(0);expired.expire=System.currentTimeMillis()-1;p.inventory.itemsBag.set(1,item(663,99));buy(p,650);check(count(p,650)==1&&expired.quantity==198&&p.inventory.ruby==1900,"only usable food is consumed");}
  else if(m.equals("concurrent")){Player p=buyer(650);Thread x=new Thread(()->buy(p,650)),y=new Thread(()->buy(p,650));x.start();y.start();x.join();y.join();check(count(p,650)==1&&p.inventory.ruby==1900&&count(p,663)==99,"concurrent request only one succeeds");}
  else if(m.equals("protection")){Player p=buyer(650);p.baovetaikhoan=true;buy(p,650);unchanged(p,650);}
  else if(m.equals("direct")){Player p=buyer(650);ShopService.gI().buyItem(p,650);check(count(p,650)==1&&p.inventory.ruby==1900&&!p.inventory.itemsBody.get(0).isNotNullItem(),"direct route uses same exchange");}
  else if(m.equals("stale")){Player p=buyer(650);buy(p,650);p.inventory.ruby=0;p.inventory.itemsBag.get(0).quantity=0;buy(p,650);check(notice.equals("Thiếu 1 món Thần trên người, không được mua.")&&count(p,650)==1&&p.inventory.ruby==0,"missing equipped item takes priority over funds/food");}
  else if(m.equals("pending")){Player p=buyer(650);p.walletPurchasePending=new WalletPurchaseService.Pending("test",1,"test",()->{});buy(p,650);unchanged(p,650);}
  else if(m.equals("rubyPending")){Player p=buyer(650);java.lang.reflect.Field f=RubyGiftService.class.getDeclaredField("ACTIVE");f.setAccessible(true);@SuppressWarnings("unchecked") Map<Object,Object> active=(Map<Object,Object>)f.get(null);active.put(p,new Object());try{buy(p,650);unchanged(p,650);}finally{active.remove(p);}}
  else throw new AssertionError(m);
  System.out.println("BEERUS_TRADE PASS "+m);System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
