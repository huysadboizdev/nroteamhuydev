package nro.models.services;
import java.util.*;
import java.lang.reflect.*;
import java.util.function.IntUnaryOperator;
import nro.models.player.*;
import nro.models.item.*;
import nro.models.mob.Mob;
import nro.models.map.*;
import static nro.models.services.PinkFixture.*;
public class EquipmentFarmDropTest {
 static Method rewards;
 static final int[][] GOD={{555,556,562,563,561},{557,558,564,565,561},{559,560,566,567,561}};
 static Player owner(int gender,boolean destroy)throws Exception {
  Player p=player(2300+gender);p.gender=(byte)gender;p.itemTime=new ItemTime(p);p.zone=zone();
  for(int slot=0;slot<5;slot++){Item i=item(GOD[gender][slot]+(destroy?95:0),1);i.template.type=(byte)slot;i.template.gender=(byte)(slot==4?3:gender);i.template.level=(byte)(destroy?14:13);p.inventory.itemsBody.add(i);}
  return p;
 }
 static Mob mob(Player p,int map)throws Exception {Mob m=blank(Mob.class);m.tempId=1;m.zone=p.zone;m.zone.map.mapId=map;return m;}
 @SuppressWarnings("unchecked") static List<ItemMap> drop(Mob m,Player p,int roll,int choice,long now)throws Exception {
  return (List<ItemMap>)rewards.invoke(null,m,p,50,100,(IntUnaryOperator)(bound->bound==100?roll:choice),now);
 }
 public static void main(String[] a)throws Exception {
  install();replace(InventoryService.class,"I",new InventoryService());
  rewards=Class.forName("nro.models.services.EquipmentFarmDropService").getMethod("rewards",Mob.class,Player.class,int.class,int.class,IntUnaryOperator.class,long.class);
  long now=System.currentTimeMillis();
  for(int g=0;g<3;g++)for(int map:new int[]{92,93,94,96,97,98,99,100,102,103,105,106,107,108,109,110}) {
   Player p=owner(g,false);int rate=map>=105?60:20;
   for(int roll=0;roll<100;roll++) {
    List<ItemMap> d=drop(mob(p,map),p,roll,roll%5,now);check(d.size()==(roll<rate?1:0),"exact boundary "+map+"/"+roll);
    if(!d.isEmpty())check(d.get(0).quantity==1&&d.get(0).itemTemplate.id==663+roll%5&&d.get(0).playerId==p.id,"food choice/owner");
   }
  }
  for(int g=0;g<3;g++)for(int choice=0;choice<5;choice++) {
   Player p=owner(g,true);List<ItemMap> normal=drop(mob(p,155),p,99,choice,now);
   check(normal.size()==1&&normal.get(0).itemTemplate.id==1066+choice&&normal.get(0).quantity==1,"100% shard every type");
   p.itemTime.isUseCoBonLa=true;p.itemTime.lastTimeUseCoBonLa=now-1;p.itemTime.timeLengthCoBonLa=100;
   Mob m=mob(p,155);check(drop(m,p,99,choice,now).get(0).quantity==20,"active clover x20");check(drop(m,p,0,choice,now).isEmpty(),"same death cannot replay");
   p.itemTime.lastTimeUseCoBonLa=now-100;check(drop(mob(p,155),p,99,choice,now).get(0).quantity==1,"clover exact expiry");
   p.itemTime.lastTimeUseCoBonLa=now+1;check(drop(mob(p,155),p,99,choice,now).get(0).quantity==1,"future timer rejected");
  }
  for(boolean des:new boolean[]{false,true})for(int invalid=0;invalid<8;invalid++) {
   Player p=owner(0,des);Item i=p.inventory.itemsBody.get(0);
   switch(invalid){case 0:i.quantity=0;break;case 1:i.quantity=2;break;case 2:i.expire=now-1;break;case 3:i.template.type=1;break;case 4:i.template.gender=2;break;case 5:i.template.id=123;break;case 6:p.inventory.itemsBody.remove(0);break;case 7:p.inventory.itemsBody.set(0,null);break;}
   check(drop(mob(p,des?155:92),p,0,0,now).isEmpty(),"invalid set "+invalid);
  }
  for(int map:new int[]{0,95,101,104,111,152,154,156}) {Player p=owner(0,false);check(drop(mob(p,map),p,0,0,now).isEmpty(),"outside food maps");}
  Player master=owner(0,true);Mob m=mob(master,155);Pet pet=blank(Pet.class);pet.master=master;pet.zone=master.zone;pet.isPet=true;
  check(drop(m,pet,99,0,now).get(0).playerId==master.id,"pet rewards master");
  for(int region:new int[]{92,105,155})for(boolean second:new boolean[]{false,true}) {
   Player lead=owner(0,region==155);Player follower;
   if(second){NewPet n=blank(NewPet.class);n.master=lead;n.isNewPet=true;follower=n;}
   else {Pet n=blank(Pet.class);n.master=lead;n.isPet=true;follower=n;}
   follower.zone=lead.zone;check(drop(mob(lead,region),follower,0,0,now).get(0).playerId==lead.id,"both followers in all regions");
  }
  Player parallel=owner(0,true);Mob one=mob(parallel,155);
  var executor=java.util.concurrent.Executors.newFixedThreadPool(4);
  try {
   List<java.util.concurrent.Future<Integer>> requests=new ArrayList<>();
   for(int n=0;n<16;n++)requests.add(executor.submit(()->drop(one,parallel,0,0,now).size()));
   int awarded=0;for(var request:requests)awarded+=request.get();check(awarded==1,"concurrent death only one farm stack");
  }finally{executor.shutdownNow();}
  one.point=new nro.models.mob.MobPoint(one);one.point.maxHp=100;one.hoiSinh();
  check(drop(one,parallel,0,0,now).size()==1,"revived monster can reward again");
  parallel.itemTime.isUseCoBonLa=true;parallel.itemTime.lastTimeUseCoBonLa=now-1000;parallel.itemTime.timeLengthCoBonLa=ItemTime.TIME_CO_BON_LA;
  var saved=ItemTimePersistence.encode(parallel.itemTime,now);
  parallel.itemTime=new ItemTime(parallel);ItemTimePersistence.restore(parallel.itemTime,saved,now+1000);
  check(drop(mob(parallel,155),parallel,0,0,now+1000).get(0).quantity==20,"clover survives persisted logout/login");
  Player p=owner(0,true);m=mob(p,155);m.tempId=117;check(drop(m,p,0,0,now).isEmpty(),"meter excluded");
  System.out.println("EquipmentFarmDropTest PASS all exact rolls, planets, set guards, clover expiry, pet owner and replay");
 }
}
