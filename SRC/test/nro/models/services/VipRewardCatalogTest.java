package nro.models.services;
import java.lang.reflect.*;
import java.util.*;
import nro.models.item.Item;
import nro.models.player.*;
import nro.models.player_system.Template;
import static nro.models.services.PinkFixture.*;

public class VipRewardCatalogTest {
 public static void main(String[] args)throws Exception {
  install();
  replace(ItemService.class,"i",new ItemService(){
   @Override public Template.ItemTemplate getTemplate(int id){var t=template(id);t.name="item "+id;t.isUpToUp=Set.of(722,1283,1284,1794,1204,1791,1792,1793,987,956).contains(id);return t;}
   @Override public Template.ItemOptionTemplate getItemOptionTemplate(int id){return new Template.ItemOptionTemplate(id,"Option #",0);}
  });
  Class<?> c=Class.forName("nro.models.services.VipRewardCatalog");
  Method prepare=c.getMethod("prepare",int.class),describe=c.getMethod("description",int.class);
  for(int level=1;level<=4;level++) {
   Object pack=prepare.invoke(null,level);int ruby=(int)pack.getClass().getMethod("ruby").invoke(pack);
   check(ruby==new int[]{200,500,700,1000}[level-1],"ruby VIP"+level);
   @SuppressWarnings("unchecked") List<Item> items=(List<Item>)pack.getClass().getMethod("items").invoke(pack);
   for(int[] want:new int[][]{{722,10},{1283,level*1000},{1284,100},{1794,100},{new int[]{1204,1791,1792,1793}[level-1],1000}})
    check(items.stream().filter(i->i.template.id==want[0]).mapToInt(i->i.quantity).sum()==want[1],"quantity "+Arrays.toString(want));
   check(items.stream().noneMatch(i->i.template.id==457||i.template.id==459),"no gold/coupon");
   String text=(String)describe.invoke(null,level);check(text.contains("hồng ngọc")&&text.contains("100 Con dấu")&&text.contains("100 Bùa giám định"),"menu matches reward");
   Player p=player(500+level);p.inventory.ruby=5;
   for(int i=0;i<items.size();i++)p.inventory.itemsBag.add(new Item());
   replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}});
   Method fits=pack.getClass().getMethod("fits",Player.class),grant=pack.getClass().getMethod("grant",Player.class);
   check((boolean)fits.invoke(pack,p),"full bundle fits");
   grant.invoke(pack,p);check(p.inventory.ruby==5+ruby,"ruby credited");
   for(Item expected:items)check(p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==expected.template.id).mapToInt(i->i.quantity).sum()>=expected.quantity,"grant all items");
   int credited=p.inventory.ruby;grant.invoke(pack,p);check(p.inventory.ruby==credited,"grant once only");
   Object insufficient=prepare.invoke(null,level);Player full=player(600+level);full.inventory.itemsBag.add(item(999,1));
   check(!(boolean)fits.invoke(insufficient,full),"whole bundle cannot fit one occupied slot");
   grant.invoke(insufficient,full);check(full.inventory.itemsBoxCrackBall.size()==items.size(),"pending fallback stores whole package");
   full.inventory.ruby=Integer.MAX_VALUE;
   check(!(boolean)fits.invoke(prepare.invoke(null,level),full),"ruby overflow preflight");
  }
  System.out.println("VipRewardCatalogTest PASS4tiers, exact quantities, bag capacity, no partial grants, pending chest fallback, overflow and once-only");
 }
}
