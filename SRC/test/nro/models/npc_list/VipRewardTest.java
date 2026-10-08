package nro.models.npc_list;
import java.util.*;
import nro.models.item.Item;
import nro.models.player.*;
import nro.models.services.*;
import static nro.models.services.PinkFixture.*;
/** Preserve existing VIP gear, pet and duration while extending reward packages. */
public final class VipRewardTest {
 public static void main(String[] args)throws Exception {
  install(); replace(InventoryService.class,"I",new InventoryService());
  for(int level=1;level<=4;level++)for(boolean existing:new boolean[]{false,true}) {
   Player p=player(900+level); if(existing)p.pet=blank(Pet.class);Pet previous=p.pet;
   for(int slot=0;slot<20;slot++)p.inventory.itemsBag.add(new Item());
   long before=System.currentTimeMillis();var pack=VipRewardCatalog.prepare(level);pack.grant(p);
   check(p.pet==previous,"VIP must preserve pet");
   check(p.vip==level&&p.vipPurchaseCount==1,"level and purchase count");
   check(p.timevip>=before+30L*24*3600*1000,"30 days");
   int[][] gear=level==1?new int[][]{{1252,10},{1256,10}}:level<=3?new int[][]{{1252,12},{1254,12}}:new int[][]{{1554,15},{1772,15}};
   for(int[] spec:gear) {
    Item i=p.inventory.itemsBag.stream().filter(it->it.isNotNullItem()&&it.template.id==spec[0]).findFirst().orElseThrow();
    for(int id:new int[]{50,77,103})check(option(i,id)==spec[1],"gear stat preserved");
    check(option(i,93)==(level<=2?30:0),"gear expiry preserved");
   }
   if(level>=2) {
    int id=level==4?1557:584;Item i=p.inventory.itemsBag.stream().filter(it->it.isNotNullItem()&&it.template.id==id).findFirst().orElseThrow();
    check(option(i,50)==(level==4?25:24)&&option(i,77)==(level==4?25:24),"costume HP/SD preserved");
    check(option(i,117)==(level==4?25:15)&&option(i,93)==(level==2?30:0),"costume crit/expiry");
   }
   if(level==3)check(count(p,956)==10,"captain cards");
   if(level==4)check(count(p,1204)==20,"existing Namek bonus");
   check(count(p,987)==new int[]{5,10,30,50}[level-1],"stones preserved");
  }
  System.out.println("VipRewardTest PASS four tiers, pet preservation, duration and original gear options");
 }
 static int option(Item i,int id){return i.itemOptions.stream().filter(o->o.optionTemplate.id==id).mapToInt(o->o.param).sum();}
 static int count(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToInt(i->i.quantity).sum();}
}
