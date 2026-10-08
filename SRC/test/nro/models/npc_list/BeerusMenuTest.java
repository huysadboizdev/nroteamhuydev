package nro.models.npc_list;
import nro.models.player.Player;
import nro.models.services.*;
import nro.models.shop.ShopService;
import nro.models.item.Item;
public final class BeerusMenuTest {
 public static class TestBill extends Bill {
  public String dialog;
  public TestBill(){super(48,0,0,0,55,0);}
  @Override public boolean canOpenNpc(Player p){return true;}
  @Override public void createOtherMenu(Player p,int index,String text,String... choices){dialog=text;p.idMark.setIndexMenu(index);}
 }
 static Player player(){Player p=new Player();p.gender=0;p.inventory.itemsBody.clear();int[] ids={555,556,562,563,561};for(int s=0;s<5;s++){Item i=PinkFixture.item(ids[s],1);i.template.level=13;i.template.type=(byte)s;i.template.gender=(byte)(s==4?3:0);p.inventory.itemsBody.add(i);}p.inventory.itemsBag.clear();p.inventory.itemsBag.add(PinkFixture.item(663,99));return p;}
 public static void main(String[] args)throws Exception{
  PinkFixture.install();PinkFixture.replace(InventoryService.class,"I",new InventoryService());boolean[] opened={false};PinkFixture.replace(ShopService.class,"I",new ShopService(){@Override public void opendShop(Player p,String tag,boolean all){PinkFixture.check("BILL".equals(tag),"BILL catalog");opened[0]=true;}});
  TestBill b=PinkFixture.blank(TestBill.class);b.mapId=48;Player p=player();p.idMark.setIndexMenu(100);b.confirmMenu(p,0);PinkFixture.check(opened[0],"map48 confirmation opens catalog");
  opened[0]=false;p.inventory.itemsBody.set(0,new Item());b.confirmMenu(p,0);PinkFixture.check(!opened[0],"recheck removed body item");
  p=player();p.idMark.setIndexMenu(100);b.confirmMenu(p,1);PinkFixture.check(!opened[0],"cancel");
  p=player();p.inventory.itemsBox.add(p.inventory.itemsBag.remove(0));p.idMark.setIndexMenu(100);b.confirmMenu(p,0);PinkFixture.check(!opened[0],"box food rejected");
  p=player();p.inventory.itemsBag.clear();p.inventory.itemsBag.add(PinkFixture.item(663,49));p.inventory.itemsBag.add(PinkFixture.item(663,50));PinkFixture.check(InventoryService.gI().canOpenBillShop(p),"same food aggregate");p.inventory.itemsBag.get(1).template.id=664;PinkFixture.check(!InventoryService.gI().canOpenBillShop(p),"mixed food rejected");
  p=player();p.inventory.itemsBody.get(0).quantity=0;PinkFixture.check(!InventoryService.gI().fullSetThan(p),"zero divine rejected");p=player();p.inventory.itemsBody.get(0).template.type=1;PinkFixture.check(!InventoryService.gI().fullSetThan(p),"wrong slot rejected");p=player();p.inventory.itemsBody.get(0).template.gender=1;PinkFixture.check(!InventoryService.gI().fullSetThan(p),"wrong gender rejected");p=player();p.inventory.itemsBody.get(0).expire=System.currentTimeMillis()-1;PinkFixture.check(!InventoryService.gI().fullSetThan(p),"expired divine rejected");p=player();p.inventory.itemsBody.clear();PinkFixture.check(!InventoryService.gI().fullSetThan(p),"short body rejected");
  p=player();p.idMark.setIndexMenu(nro.models.consts.ConstNpc.BASE_MENU);b.confirmMenu(p,0);PinkFixture.check(p.idMark.getIndexMenu()==100,"offer confirmation menu");PinkFixture.check(b.dialog.contains("100 hồng ngọc") && b.dialog.contains("1–5%") && !b.dialog.contains("15%"),"current trade terms");p.inventory.itemsBody.set(2,new Item());b.confirmMenu(p,0);PinkFixture.check(!opened[0],"offer does not grant stale permission");
  p=player();b.mapId=2;p.idMark.setIndexMenu(100);b.confirmMenu(p,100);PinkFixture.check(!opened[0],"wrong map no accidental open");b.mapId=48;
  p=player();Item timed=p.inventory.itemsBody.get(0);timed.createTime=System.currentTimeMillis()-3*86400000L;timed.itemOptions.add(new Item.ItemOption(93,1));long created=timed.createTime;PinkFixture.check(!InventoryService.gI().fullSetThan(p),"timed divine expired");PinkFixture.check(timed.createTime==created && timed.itemOptions.get(0).param==1,"preflight does not mutate lifetime");
  p=player();p.inventory.itemsBody.get(0).quantity=2;PinkFixture.check(!InventoryService.gI().fullSetThan(p),"corrupt stacked equipment rejected");
  System.out.println("BeerusMenuTest PASS: 14 menu/gate groups");System.exit(0);
 }
}
