package nro.models.item;
import nro.models.services.*;
import nro.models.services_func.Trade;
import nro.models.shop_ky_gui.ConsignTransactions;
import static nro.models.services.PinkFixture.*;
public final class HighTierEquipmentTradeTest {
 public static void main(String[] args)throws Exception {
  install();var deny=Trade.class.getDeclaredMethod("isItemCannotTran",Item.class);deny.setAccessible(true);
  Trade trade=blank(Trade.class);
  for(int id=650;id<=1062;id++) {
   if(id>662&&id<1048)continue;
   Item i=item(id,1);i.template.type=0;i.template.level=(byte)(id<1000?14:15);
   check((boolean)deny.invoke(trade,i),"legacy item cannot trade by ID "+id);
   i.itemOptions.add(new Item.ItemOption(87,1));i.itemOptions.add(new Item.ItemOption(34,1));
   check(!ConsignTransactions.eligible(i),"even legacy seal/consign permission cannot override "+id);
   ItemDefaultOptions.complete(i);ItemDefaultOptions.complete(i);
   check(i.itemOptions.stream().filter(o->o.optionTemplate.id==30).count()==1,"one visible nontrade option "+id);
  }
  Item ordinary=item(555,1);ItemDefaultOptions.complete(ordinary);
  check(ordinary.itemOptions.stream().noneMatch(o->o.optionTemplate.id==30),"divine gear untouched");
  System.out.println("HIGH_TIER_TRADE PASS all28 items and legacy guards");System.exit(0);
 }
}
