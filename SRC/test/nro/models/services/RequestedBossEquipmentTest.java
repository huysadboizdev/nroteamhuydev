package nro.models.services;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.IntUnaryOperator;
import nro.models.boss.*;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import static nro.models.services.PinkFixture.*;
public class RequestedBossEquipmentTest {
 static Method choose;
 static Item choice(nro.models.player.Player p,int category,int chance,int index)throws Exception{
  return (Item)choose.invoke(null,p,category,HuntingTrioLootTest.rolls(chance,index,0,0));
 }
 public static void main(String[] args){try{
  HuntingTrioLootTest.setup();Class<?> type;
  try{type=Class.forName("nro.models.services.RequestedBossEquipment");}catch(ClassNotFoundException absent){throw new AssertionError("requested boss rare equipment policy missing",absent);}
  choose=type.getDeclaredMethod("choose",nro.models.player.Player.class,int.class,IntUnaryOperator.class);choose.setAccessible(true);
  var p=player(123);
  for(int gender=0;gender<3;gender++){p.gender=(byte)gender;for(int category=0;category<3;category++){
   int cutoff=new int[]{500,100,50}[category];
   for(int chance:new int[]{0,cutoff-1,cutoff,9999}){
    Item item=choice(p,category,chance,0);check((item!=null)==(chance<cutoff),"exact perkill rate boundary "+category);
    if(item!=null){check((item.template.id<555)==(category==0),"correct category pool");long seals=item.itemOptions.stream().filter(o->o.optionTemplate.id>=34&&o.optionTemplate.id<=36).count();check(seals==(category==1?0:1),"exact seal count");check(item.itemOptions.stream().noneMatch(o->o.optionTemplate.id>=127&&o.optionTemplate.id<=135),"no accidental activation set");}
   }
  }}
  for(int id:new int[]{BossID.XEN_BO_HUNG,BossID.SIEU_BO_HUNG,BossID.BLACK_GOKU,BossID.ZAMASU,BossID.CUMBER,BossID.COOLER,BossID.DRABURA,BossID.DRABURA_2,BossID.DRABURA_3,BossID.MABU,
    BossID.HEART,BossID.KAMIN,BossID.OREN,BossID.BEERUS_BATTLE,BossID.WHIS_BATTLE,BossID.CHAMPA_BATTLE,BossID.VADOS_BATTLE}){
   Boss b=HuntingTrioLootTest.boss();b.id=id;
   List<Integer> suppressed=new ArrayList<>();
   BossAdminDropService.reward(b,p,()->{
    if(BossAdminDropService.suppressConstruction(555))suppressed.add(555);
    check(!BossAdminDropService.suppressConstruction(190)&&!BossAdminDropService.suppressConstruction(18)&&!BossAdminDropService.suppressConstruction(230),"gold, objectives and plain equipment retained");
   },null,bound->0);
   check(suppressed.size()==(id==BossID.XEN_BO_HUNG?0:1),"native divine suppression only selected bosses");
   int expected=id==BossID.XEN_BO_HUNG?1:id==BossID.MABU?2:3;
   check(b.zone.items.size()==expected,"requested categories for boss "+id);
   for(ItemMap item:b.zone.items)check(kept(item)&&item.playerId==123,"owner preserved");
   BossAdminDropService.reward(b,p,()->{throw new AssertionError("duplicate native callback");},null,bound->0);
   check(b.zone.items.size()==expected,"no double payout");BossAdminDropService.reset(b);
   BossAdminDropService.reward(b,p,()->{},null,bound->9999%bound);check(b.zone.items.size()==expected,"failed rolls create no items");
  }
  Boss b=HuntingTrioLootTest.boss();b.id=BossID.BROLY;BossAdminDropService.reward(b,p,()->{},null,bound->0);check(b.zone.items.isEmpty(),"unrequested bosses unchanged");
  Set<Integer> shopIds=new HashSet<>();
  for(int gender=0;gender<3;gender++)for(int index=0;index<20;index++){p.gender=(byte)gender;Item item=choice(p,0,0,index);check(item.template.id>=230&&item.template.id<=281,"only level9-12 shop IDs");shopIds.add((int)item.template.id);}
  check(shopIds.size()==52,"all52level9-12templates reachable");
  b=HuntingTrioLootTest.boss();b.id=BossID.BLACK_GOKU;final Boss selected=b;
  BossAdminDropService.reward(b,p,()->{
   new ItemMap(selected.zone,555,1,1,1,p.id);new ItemMap(selected.zone,190,1,1,1,p.id);new ItemMap(selected.zone,18,1,1,1,p.id);
  },null,bound->9999%bound);
  check(b.zone.items.size()==2&&b.zone.items.stream().noneMatch(i->i.itemTemplate.id==555),"native divine constructor suppressed, gold and dragon ball stay in zone");
  check(!BossAdminDropService.suppressConstruction(555),"scope released after callback");
  b=HuntingTrioLootTest.boss();b.id=BossID.COOLER;
  var replace=new BossAdminConfig.Config(null,null,null,null,null,null,"next","replace",List.of(),1);
  BossAdminDropService.reward(b,p,()->{},replace,bound->0);check(b.zone.items.isEmpty(),"admin replace disables new native pools");
  b=HuntingTrioLootTest.boss();b.id=BossID.COOLER;
  var append=new BossAdminConfig.Config(null,null,null,null,null,null,"next","append",List.of(),1);
  BossAdminDropService.reward(b,p,()->{},append,bound->0);check(b.zone.items.size()==3,"admin append retains new native pools");
  b=HuntingTrioLootTest.boss();b.id=BossID.ZAMASU;var pet=blank(nro.models.player.Pet.class);pet.master=p;
  BossAdminDropService.reward(b,pet,()->{},null,bound->0);for(ItemMap item:b.zone.items)check(item.playerId==p.id,"pet credits master");
  System.out.println("REQUESTED_EQUIPMENT PASS rates/categories/seals/targets/native-preservation/once/ownership/exclusions");System.exit(0);
 }catch(Throwable failure){failure.printStackTrace();System.exit(1);}}
}
