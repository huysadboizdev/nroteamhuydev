package nro.models.services;
import java.lang.reflect.*;
import nro.models.player.*;
import nro.models.item.*;
import nro.models.server.*;
import nro.models.player_system.Template;
import nro.models.services_func.UseItem;
import nro.models.shop.ShopService;
public final class DisabledDragonItemTest {
 static int notices;
 public static void main(String[] args)throws Exception{
  for(int i=0;i<=2000;i++){Template.ItemTemplate t=new Template.ItemTemplate();t.id=(short)i;t.type=27;t.name="fixture";Manager.ITEM_TEMPLATES.add(t);}
  for(int i=0;i<256;i++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(i,"fixture #",0));
  PinkFixture.replace(Service.class,"instance",new Service(){public void sendThongBao(Player p,String s){notices++;}});
  Item legacy=ItemService.gI().createItemForRestore((short)1015,1);
  PinkFixture.check(legacy.template.id==1015&&legacy.quantity==1,"restore keeps legacy item readable");
  try {ItemService.gI().createNewItem((short)1015);throw new AssertionError("disabled super1015 still created");}catch(IllegalArgumentException expected){}
  PinkFixture.check(ItemService.gI().createNewItem((short)942).template.id==942,"tiger creation stays enabled");
  Player p=new Player();p.isPlayer=true;p.zone=PinkFixture.zone();p.inventory.itemsBag.add(legacy);
  Method use=UseItem.class.getDeclaredMethod("useItem",Player.class,Item.class,int.class);use.setAccessible(true);use.invoke(UseItem.gI(),p,legacy,p.inventory.itemsBag.size()-1);
  PinkFixture.check(notices==1&&legacy.quantity==1&&p.shenronEvent==null,"disabled use explains rejection without consuming");
  ShopService.gI().buyItem(p,1015);PinkFixture.check(notices==2,"disabled purchase rejects before currency or current shop lookup");
  ShopService.gI().takeItem(p,(byte)0,1015);PinkFixture.check(notices==3,"alternate shop routing rejects before currency");
  nro.models.shop.ItemShop offer=PinkFixture.blank(nro.models.shop.ItemShop.class);offer.temp=legacy.template;
  try {ItemService.gI().createItemFromItemShop(offer);throw new AssertionError("alternate shop factory bypassed disabled item");}catch(IllegalArgumentException expected){}
  try {GiftCodeService.stage(p,1,new nro.models.database.GiftCodeClaimDAO.Catalog(1,"DISABLED",1,"[{\"id\":1015,\"quantity\":1}]"));throw new AssertionError("gift bypassed disabled item");}catch(IllegalArgumentException expected){PinkFixture.check(expected.getMessage().contains("temporarily disabled"),"gift rejected by explicit disable rule");}
  System.out.println("DISABLED_DRAGON PASS creation, use, buy, gift, restoration, tiger preserved");
 }
}
