package nro.models.services;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import nro.models.boss.Boss;
import nro.models.item.Item;
import nro.models.map.*;
import nro.models.player.*;
import static nro.models.services.PinkFixture.*;

public final class BossAdminDropServiceTest {
 static BossAdminConfig.Rule rule(String g,int id,int chance,int weight){return new BossAdminConfig.Rule(g,id,2,4,chance,weight,List.of(new Item.ItemOption(31,7)));}
 static BossAdminConfig.Config policy(String mode,List<BossAdminConfig.Rule> rules){return new BossAdminConfig.Config(null,null,null,null,null,null,"next",mode,rules,1);}
 static Boss boss()throws Exception{Boss b=blank(Boss.class);b.zone=zone();b.location=new Location();b.location.x=100;b.location.y=100;return b;}
 public static void main(String[] args)throws Exception{
  install();
  check(BossAdminDropService.roll(List.of(rule("",1,0,1)),n->0).isEmpty(),"0 percent disabled");
  check(BossAdminDropService.roll(List.of(rule("",1,10000,1)),n->n-1).size()==1,"100 percent unconditional");
  check(BossAdminDropService.roll(List.of(rule("",1,25,1)),n->24).size()==1,"0.25 percent below boundary");
  check(BossAdminDropService.roll(List.of(rule("",1,25,1)),n->25).isEmpty(),"fractional exact boundary");
  var group=List.of(rule("g",1,10000,1),rule("g",2,10000,3));
  check(BossAdminDropService.roll(group,n->1).get(0).itemId()==2,"weighted group chooses one");
  Boss b=boss();Player owner=player(44);AtomicInteger effects=new AtomicInteger();List<ItemMap> forbidden=new ArrayList<>();
  Runnable nativeReward=()->{effects.incrementAndGet();ItemMap i=new ItemMap(b.zone,190,2,100,100,owner.id);forbidden.add(i);Service.gI().dropItemMap(b.zone,i);};
  BossAdminDropService.reward(b,owner,nativeReward,policy("replace",List.of(rule("",77,10000,1))),n->n-1);
  check(effects.get()==1&&b.zone.getItemsSnapshot().size()==1,"replace retains native side effects but no constructor leak");
  ItemMap drop=b.zone.getItemsSnapshot().get(0);check(drop.itemTemplate.id==77&&drop.quantity==4&&drop.playerId==44&&drop.keepOwnerUntilDespawn,"custom quantity and private owner");
  check(drop.options.get(0).param==7,"custom options initialized before publication");
  b.zone.addItem(forbidden.get(0));check(b.zone.getItemsSnapshot().size()==1&&!BossAdminDropService.mayPublish(forbidden.get(0)),"suppression permanent after scope");
  BossAdminDropService.reward(b,owner,nativeReward,policy("replace",List.of()),n->0);check(effects.get()==1,"duplicate reward ignored");
  BossAdminDropService.reset(b);BossAdminDropService.reward(b,owner,nativeReward,policy("default",List.of(rule("",77,10000,1))),n->0);
  check(effects.get()==2&&b.zone.getItemsSnapshot().size()==2,"default exact native drop baseline and next life reset");
  BossAdminDropService.reset(b);Pet pet=blank(Pet.class);pet.master=owner;
  BossAdminDropService.reward(b,pet,()->{},policy("replace",List.of(rule("",77,10000,1))),n->0);
  check(b.zone.getItemsSnapshot().get(0).playerId==44,"pet kill credits master");
  BossAdminDropService.reset(b);BossAdminDropService.reward(b,owner,()->{new ItemMap(b.zone,74,1,100,100,44);},policy("replace",List.of()),n->0);
  check(b.zone.getItemsSnapshot().stream().anyMatch(i->i.itemTemplate.id==74),"mandatory quest object preserved");
  Boss c=boss();AtomicInteger calls=new AtomicInteger();ExecutorService threads=Executors.newFixedThreadPool(8);List<Future<?>> futures=new ArrayList<>();
  for(int k=0;k<30;k++)futures.add(threads.submit(()->BossAdminDropService.reward(c,owner,calls::incrementAndGet,policy("replace",List.of()),n->0)));
  for(Future<?> f:futures)f.get();threads.shutdown();check(calls.get()==1,"concurrent death claim exactly once");
  Boss outer=boss(),inner=boss();BossAdminDropService.reward(outer,owner,()->{
   BossAdminDropService.reward(inner,owner,()->new ItemMap(inner.zone,190,1,1,1,44),policy("default",List.of()),n->0);
   new ItemMap(outer.zone,190,1,1,1,44);
  },policy("replace",List.of()),n->0);
  check(inner.zone.getItemsSnapshot().size()==1&&outer.zone.getItemsSnapshot().isEmpty(),"nested scope restored");
  AtomicInteger packets=new AtomicInteger();Service wire=new Service(){@Override public void sendMessAllPlayerInMap(Zone z,nro.models.network.Message m){packets.incrementAndGet();}};
  wire.dropItemMap(b.zone,forbidden.get(0));wire.dropItemMapForMe(owner,forbidden.get(0));
  check(packets.get()==0,"permanently suppressed item never sends a packet");
  wire.dropItemMap(b.zone,drop);check(packets.get()==1,"permitted item sends real publication packet");
  Boss quest=boss();java.lang.reflect.Field support=Boss.class.getDeclaredField("questSupport");support.setAccessible(true);support.setBoolean(quest,true);
  BossAdminDropService.reward(quest,owner,()->new ItemMap(quest.zone,190,1,1,1,44),policy("replace",List.of(rule("",77,10000,1))),n->0);
  check(quest.zone.getItemsSnapshot().size()==1&&quest.zone.getItemsSnapshot().get(0).itemTemplate.id==190,"quest support remains native only");
  Boss failure=boss();try{BossAdminDropService.reward(failure,owner,()->{throw new IllegalStateException("native");},policy("replace",List.of()),n->0);}catch(IllegalStateException expected){}
  check(!BossAdminDropService.suppressConstruction(190),"exception releases thread local scope");
  AtomicInteger retries=new AtomicInteger();BossAdminDropService.reward(failure,owner,retries::incrementAndGet,policy("replace",List.of()),n->0);check(retries.get()==0,"failed native reward never retries potentially completed effects");
  System.out.println("BossAdminDropServiceTest PASS");
 }
}
