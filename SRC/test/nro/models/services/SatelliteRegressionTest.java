package nro.models.services;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import nro.models.map.*;
import nro.models.player.*;
import nro.models.clan.Clan;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.network.MySession;
import nro.models.services_func.UseItem;

public final class SatelliteRegressionTest {
 static class Owner extends PinkFixture.Owner {
  final List<byte[]> packets=new CopyOnWriteArrayList<>();
  public void sendMessage(Message m){if(m.command==-30)packets.add(m.getData());}
 }
 static Zone zone(int id)throws Exception{Zone z=PinkFixture.zone();z.map.mapId=id;return z;}
 static Owner player(long id,Zone z,int x)throws Exception{
  Owner p=new Owner();p.id=id;p.isPlayer=true;p.zone=z;p.location=new Location();p.location.x=x;
  p.nPoint=new NPoint(p);p.nPoint.hpMax=p.nPoint.mpMax=1000;p.nPoint.hp=p.nPoint.mp=500;
  p.satellite=new Satellite();p.inventory=new Inventory();p.session=PinkFixture.blank(MySession.class);z.addPlayer(p);return p;
 }
 static Item item(int id,int quantity){Item i=PinkFixture.item(id,quantity);i.template.type=22;return i;}
 static ItemMap sat(Zone z,int id,long owner){var t=PinkFixture.template(id);t.type=22;return new ItemMap(z,t,1,0,0,owner);}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static void use(Player p,Item i)throws Exception{Method m=UseItem.class.getDeclaredMethod("useItem",Player.class,Item.class,int.class);m.setAccessible(true);m.invoke(UseItem.gI(),p,i,0);}
 static void setup()throws Exception{
  PinkFixture.install();
  PinkFixture.replace(Service.class,"instance",new Service(){
   public void sendMessAllPlayerInMap(Zone z,Message m){}
   public void sendThongBaoOK(Player p,String s){}
   public void dropSatellite(Player p,Item i,Zone z,int x,int y){try{Thread.sleep(15);}catch(InterruptedException e){throw new RuntimeException(e);}super.dropSatellite(p,i,z,x,y);}
  });
  PinkFixture.replace(InventoryService.class,"I",new InventoryService(){public void sendItemBags(Player p){}public void subQuantityItemsBag(Player p,Item i,int n){i.quantity-=n;}});
  PinkFixture.replace(TaskService.class,"i",new TaskService(){public void checkDoneTaskUseItem(Player p,Item i){}});
 }
 static void parallel(List<Runnable> work)throws Exception{
  CountDownLatch ready=new CountDownLatch(work.size()),start=new CountDownLatch(1);var errors=new CopyOnWriteArrayList<Throwable>();var pool=Executors.newFixedThreadPool(work.size());
  for(Runnable r:work)pool.submit(()->{ready.countDown();try{start.await();r.run();}catch(Throwable t){errors.add(t);}});
  check(ready.await(5,TimeUnit.SECONDS),"threads ready");start.countDown();pool.shutdown();check(pool.awaitTermination(15,TimeUnit.SECONDS),"no deadlock");check(errors.isEmpty(),"thread errors "+errors);
 }
 static void run(String mode)throws Exception{
  setup();
  if(mode.equals("race")){
   Zone z=zone(5);sat(z,342,100);sat(z,343,100);var tasks=new ArrayList<Runnable>();var people=new ArrayList<Owner>();
   for(int n=0;n<8;n++){var p=player(n+1,z,0);var i=item(345,1);p.inventory.itemsBag.add(i);people.add(p);tasks.add(()->{try{use(p,i);}catch(Exception e){throw new RuntimeException(e);}});}
   parallel(tasks);check(z.items.size()==3,"cap3 got "+z.items.size());int consumed=0;for(var p:people)if(p.inventory.itemsBag.get(0).quantity==0)consumed++;check(consumed==1,"only winner consumed");return;
  }
  if(mode.equals("duplicate")){
   var z=zone(5);var p=player(1,z,0);var i=item(345,1);p.inventory.itemsBag.add(i);parallel(List.of(()->{try{use(p,i);}catch(Exception e){throw new RuntimeException(e);}},()->{try{use(p,i);}catch(Exception e){throw new RuntimeException(e);}}));
   check(z.items.size()==1&&i.quantity==0,"one unit used once: sats"+z.items.size()+" qty"+i.quantity);return;
  }
  if(mode.equals("expiry")){
   for(int id:new int[]{5,21,22,23}){var z=zone(id);var p=player(1,z,0);var s=sat(z,345,1);s.createTime=System.currentTimeMillis()-1800001;s.update();check(p.nPoint.hp==500,"expired must not heal map"+id);check(!z.items.contains(s)&&s.zone==null,"expiry every map"+id);}
   return;
  }
  if(mode.equals("stale")){
   var z=zone(5);var p=player(1,z,0);var s=sat(z,345,1);p.zone=zone(6);s.update();check(p.nPoint.hp==500&&!p.satellite.isHP,"old membership cannot buff after move");return;
  }
  if(mode.equals("zero")){
   var z=zone(5);var p=player(1,z,0);p.nPoint.hp=p.nPoint.mp=5;p.nPoint.hpMax=p.nPoint.mpMax=9;sat(z,345,1).update();sat(z,342,1).update();check(p.packets.isEmpty(),"no packet for rounded zero heal");return;
  }
  if(mode.equals("packet")){
   var z=zone(5);var p=player(1,z,0);var observer=player(2,z,0);sat(z,345,1).update();check(p.nPoint.hp==600,"HP10percent");
   check(observer.packets.size()==1,"observer one update");byte[] wire=observer.packets.get(0);check(wire.length==13,"compact HP payload expected13 got"+wire.length);
   var reader=new DataInputStream(new ByteArrayInputStream(wire));check(reader.readByte()==9&&reader.readInt()==1&&reader.readInt()==600&&reader.readInt()==1000&&reader.available()==0,"existing client fields without optional reads");
   sat(z,345,1).update();check(p.nPoint.hp==600&&observer.packets.size()==1,"no stacking or duplicatepacket");return;
  }
  if(mode.equals("eligibility")){
   var z=zone(5);var p=player(1,z,0);var mate=player(2,z,199);var edge=player(3,z,200);var other=player(4,z,0);var dead=player(5,z,0);dead.nPoint.hp=0;
   var clan=PinkFixture.blank(Clan.class);clan.id=77;p.clan=mate.clan=edge.clan=dead.clan=clan;var s=sat(z,345,1);s.clanId=77;s.update();
   check(p.nPoint.hp==600&&mate.nPoint.hp==600&&edge.nPoint.hp==500&&other.nPoint.hp==500&&dead.nPoint.hp==0,"eligibility");
   sat(z,342,1).update();check(p.nPoint.mp==600,"MP10percent");sat(z,343,1).update();sat(z,344,1).update();check(p.satellite.isIntelligent&&p.satellite.isDefend,"other buffs");
   p.charms=new Charms(p);p.effectSkill=new EffectSkill(p);nro.models.server.Manager.RATE_EXP_SERVER=1;check(p.nPoint.calSucManhTiemNang(1000)==1200,"intelligence20percent");return;
  }
  if(mode.equals("snapshot")){
   var z=zone(5);var p=player(1,z,0);var s=sat(z,345,1);
   // Actual snapshot must remain detached across subsequent entry/exit.
   Method method;try{method=Zone.class.getMethod("getPlayersSnapshot");}catch(NoSuchMethodException e){throw new AssertionError("satellite needs synchronized player snapshot",e);}
   var snap=(List<?>)method.invoke(z);z.removePlayer(p);player(2,z,0);check(snap.size()==1&&snap.get(0)==p,"detached snapshot");
   var mover=player(3,z,0);parallel(List.of(()->{for(int n=0;n<3000;n++){z.removePlayer(mover);z.addPlayer(mover);}},()->{for(int n=0;n<3000;n++)s.update();}));return;
  }
  if(mode.equals("full")){
   var z=zone(5);var p=player(1,z,0);for(int n=0;n<3;n++)sat(z,342+n,99);var i=item(345,1);p.inventory.itemsBag.add(i);use(p,i);check(i.quantity==1&&z.items.size()==3,"full does not consume");return;
  }
  if(mode.equals("bot")){
   var z=zone(5);var s=sat(z,345,1);var bot=PinkFixture.blank(nro.models.Bot.Bot.class);bot.zone=z;bot.location=new Location();bot.charms=new Charms(bot);bot.charms.tdThuHut=System.currentTimeMillis()+60000;
   var ai=new nro.models.Bot.BotPemQuai(bot);var pick=nro.models.Bot.BotPemQuai.class.getDeclaredMethod("NhatItem");pick.setAccessible(true);pick.invoke(ai);
   check(z.items.contains(s)&&!s.isPickedUp,"bot cannot pick/delete a satellite");return;
  }
  if(mode.equals("inventory")){
   PinkFixture.replace(InventoryService.class,"I",new InventoryService(){public void sendItemBags(Player p){}});
   var z=zone(5);var p=player(1,z,0);var i=item(345,1);p.inventory.itemsBag.add(i);
   parallel(List.of(()->{try{use(p,i);}catch(Exception e){throw new RuntimeException(e);}},()->{try{use(p,i);}catch(Exception e){throw new RuntimeException(e);}}));
   check(z.items.size()==1&&i.template==null,"actual inventory disposes once without race");return;
  }
  if(mode.equals("cooldown")){
   var z=zone(5);var p=player(1,z,0);var s=sat(z,345,1);s.update();s.update();check(p.nPoint.hp==600,"same activation once");
   p.satellite.lastHPTime=System.currentTimeMillis()-3100;p.satellite.update();s.update();check(p.nPoint.hp==700,"heal after cooldown");
   p.location.x=200;p.satellite.lastHPTime=System.currentTimeMillis()-3100;p.satellite.update();s.update();check(!p.satellite.isHP&&p.nPoint.hp==700,"outside range no renewal");return;
  }
  throw new AssertionError("unknown mode "+mode);
 }
 public static void main(String[] args){try{run(args[0]);System.out.println("SATELLITE "+args[0]+" PASS");System.exit(0);}catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
