package nro.models.services;
import nro.models.item.*;
import static nro.models.services.PinkFixture.*;
public final class DivinePowerRequirementTest {
 public static void main(String[] args)throws Exception {try{
  install();
  for(int id=555;id<=567;id++){
   Item item=item(id,1);item.itemOptions.add(new Item.ItemOption(23,48));item.itemOptions.add(new Item.ItemOption(34,1));
   check(ItemDefaultOptions.requiredPower(item)==15_000_000_000L,"missing divine option must enforce15b not zero:"+id);
   ItemDefaultOptions.complete(item);check(item.getContent().contains("15000000000"),"inventory content must not advertise zero power");check(item.itemOptions.stream().anyMatch(o->o.optionTemplate.id==21&&o.param==15),"restore adds visible requirement");
   check(item.itemOptions.stream().anyMatch(o->o.optionTemplate.id==23&&o.param==48)&&item.itemOptions.stream().anyMatch(o->o.optionTemplate.id==34&&o.param==1),"preserves stats and seal");
   ItemDefaultOptions.complete(item);check(item.itemOptions.stream().filter(o->o.optionTemplate.id==21).count()==1,"repeat restore idempotent");
  }
  Item saved=item(563,1);saved.itemOptions.add(new Item.ItemOption(21,17));ItemDefaultOptions.complete(saved);
  check(ItemDefaultOptions.requiredPower(saved)==17_000_000_000L&&saved.itemOptions.size()==1,"preserves valid saved17b");
  Item zero=item(563,1);zero.itemOptions.add(new Item.ItemOption(21,0));ItemDefaultOptions.complete(zero);
  check(ItemDefaultOptions.requiredPower(zero)==15_000_000_000L&&zero.itemOptions.size()==1,"repairs zero without duplicate options");
  HuntingTrioLootTest.setup();var owner=player(7);owner.gender=0;
  for(int roll:new int[]{60,90}){var drop=SealedBossEquipmentLoot.build(owner,HuntingTrioLootTest.standard(roll,3,0));
   check(drop.template.id==563&&ItemDefaultOptions.requiredPower(drop)==15_000_000_000L,"ordinary and sealed actual drops have15b");}
  System.out.println("PASS divine missing/zero repair, retained requirements/stats/seals, actual drops");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
