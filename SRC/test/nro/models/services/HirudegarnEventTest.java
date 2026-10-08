package nro.models.services;
import java.time.*;
public final class HirudegarnEventTest {
 static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
 static void schedule() throws Exception {
  Class<?> service;
  try { service=Class.forName("nro.models.services.HirudegarnEventService"); }
  catch(ClassNotFoundException e) { throw new AssertionError("Fixed-session Hirudegarn policy is missing",e); }
  var end=service.getMethod("sessionEnd",Instant.class);
  String[] times={"14:59:59","15:00:00","15:59:59","16:00:00","21:59:59","22:00:00","22:59:59","23:00:00"};
  boolean[] open={false,true,true,false,false,true,true,false};
  for(int i=0;i<times.length;i++) {
   Instant now=ZonedDateTime.of(LocalDate.of(2026,10,5),LocalTime.parse(times[i]),ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
   Instant actual=(Instant)end.invoke(null,now);
   check((actual!=null)==open[i],"Boundary "+times[i]);
   if(actual!=null)check(actual.atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalTime().equals(LocalTime.of(i<4?16:23,0)),"Fixed deadline "+times[i]);
  }

 }

 static final class MutableClock extends Clock {
  volatile Instant now;
  volatile java.util.concurrent.CountDownLatch observed;
  MutableClock(String time){set(time);}
  void set(String time){now=ZonedDateTime.of(LocalDate.of(2026,10,5),LocalTime.parse(time),ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();}
  @Override public Instant instant(){Instant value=now;var latch=observed;if(latch!=null){observed=null;latch.countDown();}return value;}
  @Override public ZoneId getZone(){return ZoneOffset.UTC;}
  @Override public Clock withZone(ZoneId z){return this;}
 }
 static final class Owner extends nro.models.player.Player {
  int hits, updates, ships;
  boolean admin, dead;
  int heals;
  @Override public boolean isAdmin(){return admin;}
  @Override public boolean isDie(){return dead;}
  @Override public void update(){updates++;}
  @Override public synchronized int injured(nro.models.player.Player attacker,long damage,boolean piercing,boolean mob){hits++;return (int)damage;}
  @Override public void sendMessage(nro.models.network.Message message){if(message.command==-65)ships++;}
 }
 static nro.models.map.Zone zone(int id)throws Exception {
  var map=PinkFixture.blank(PinkFixture.Terrain.class);map.mapId=id;map.mapName="event fixture";map.mapWidth=1200;map.mapHeight=480;map.zones=new java.util.ArrayList<>();map.npcs=new java.util.ArrayList<>();map.wayPoints=new java.util.ArrayList<>();
  var zone=new nro.models.map.Zone(map,0,15);map.zones.add(zone);return zone;
 }
 static Owner player(long id,nro.models.map.Zone zone){
  Owner p=new Owner();p.id=id;p.isPlayer=true;p.idNRNM=-1;p.name="event fixture";p.zone=zone;p.playerIntrinsic.intrinsic=new nro.models.intrinsic.Intrinsic();p.playerTask.taskMain=new nro.models.task.TaskMain();p.playerTask.taskMain.id=-1;p.nPoint.hp=p.nPoint.hpMax=p.nPoint.hpg=100000;p.nPoint.mp=p.nPoint.mpMax=p.nPoint.mpg=100000;p.nPoint.dameg=1000;p.nPoint.limitPower=9;p.nPoint.power=90010000000L;for(int i=0;i<12;i++)p.inventory.itemsBody.add(new nro.models.item.Item());if(zone!=null)zone.addPlayer(p);return p;
 }
 static nro.models.mob_bigboss.Hirudegarn boss(nro.models.map.Zone zone) {
  var mob=new nro.models.mob.Mob();mob.tempId=nro.models.consts.ConstMob.HIRUDEGARN;mob.point.setHpFull(1000);
  var boss=new nro.models.mob_bigboss.Hirudegarn(mob);boss.zone=zone;zone.mobs.add(boss);return boss;
 }
 static void setup()throws Exception{
  PinkFixture.install();
  // Keep the randomized loot options dependency isolated; the actual reward branch still runs.
  PinkFixture.replace(ItemService.class,"i",new ItemService(){
   @Override public nro.models.player_system.Template.ItemTemplate getTemplate(int id){return PinkFixture.template(id);}
   @Override public nro.models.player_system.Template.ItemOptionTemplate getItemOptionTemplate(int id){var t=new nro.models.player_system.Template.ItemOptionTemplate();t.id=id;return t;}
   @Override public nro.models.item.Item createItemFromItemMap(nro.models.map.ItemMap m){return PinkFixture.item(m.itemTemplate.id,m.quantity);}
   @Override public nro.models.map.ItemMap randDoTL(nro.models.map.Zone zone,int quantity,int x,int y,long owner){return new nro.models.map.ItemMap(zone,555,quantity,x,y,owner);}
  });
 }
 static void run(String mode)throws Exception {
  if(mode.equals("schedule")){schedule();return;}
  setup();
  MutableClock clock=new MutableClock("15:00:00");var event=HirudegarnEventService.gI();event.injectClock(clock);
  var target=zone(126);var origin=zone(19);
  nro.models.server.Manager.MAPS.clear();nro.models.server.Manager.MAPS.add(target.map);nro.models.server.Manager.MAPS.add(origin.map);
  if(mode.equals("startup")){
   java.lang.reflect.Method start;
   try{start=nro.models.map.Map.class.getMethod("startHirudegarnEvent");}
   catch(NoSuchMethodException missing){throw new AssertionError("Real server startup needs an idempotent Hirudegarn scheduler registration",missing);}
   var updates=new java.util.concurrent.atomic.AtomicInteger();var threeTicks=new java.util.concurrent.CountDownLatch(3);
   var tickingBoss=new nro.models.mob_bigboss.Hirudegarn(new nro.models.mob.Mob()){@Override public void update(){updates.incrementAndGet();threeTicks.countDown();}};
   tickingBoss.zone=target;target.mobs.add(tickingBoss);Owner participant=player(111,target);
   start.invoke(origin.map);start.invoke(target.map);start.invoke(target.map);
   check(threeTicks.await(3,java.util.concurrent.TimeUnit.SECONDS),"Registered event executes its fast ticks");
   long threads=Thread.getAllStackTraces().keySet().stream().filter(t->t.isAlive()&&t.getName().equals("hirudegarn-event-126")).count();
   check(threads==1&&updates.get()>=3&&participant.updates==0,"Startup registration is idempotent and never starts regular player updates");
   byte[] managerClass=nro.models.server.Manager.class.getResourceAsStream("Manager.class").readAllBytes();
   check(new String(managerClass,java.nio.charset.StandardCharsets.ISO_8859_1).contains("startHirudegarnEvent"),"Compiled server startup registers the event scheduler");
  } else if(mode.equals("zones")){
   var map=PinkFixture.blank(nro.models.map.Map.class);map.mapId=126;map.zones=new java.util.ArrayList<>();
   var init=nro.models.map.Map.class.getDeclaredMethod("initZone",int.class,int.class);init.setAccessible(true);init.invoke(map,2,15);
   check(map.zones.size()==20,"Map 126 has twenty independent zones");
   for(var z:map.zones)check(z.maxPlayer==15,"Existing capacity fifteen");
   map.initMob(new byte[]{(byte)nro.models.consts.ConstMob.HIRUDEGARN},new byte[]{1},new int[]{1000},new short[]{300},new short[]{360});
   // Direct constructor evidence independent of template fixtures.
   var a=boss(map.zones.get(0));var b=boss(map.zones.get(1));a.point.hp=1;
   check(b.point.hp==1000 && a.effectSkill!=b.effectSkill,"Boss combat state is independent in each zone");
   var tickingBoss=boss(target);event.prepareZone(target);Owner participant=player(222,target);tickingBoss.point.hp=0;
   target.update();check(tickingBoss.lvMob==0&&tickingBoss.isDie()&&participant.updates==1,"Slow tick updates players without duplicating event boss update");
   event.tick(target.map);check(tickingBoss.lvMob==1&&participant.updates==1,"Fast tick advances boss without speeding player updates");
   var snapshot=target.getPlayers();target.removePlayer(participant);check(snapshot.size()==1&&target.getPlayers().isEmpty(),"Event player snapshot remains safe during membership changes");

  } else if(mode.equals("admission")){
   Owner p=player(1,origin);clock.set("16:00:00");p.admin=true;
   check(nro.models.map.service.MapService.gI().getMapCanJoin(p,126,-1)==null,"Closed selection rejects administrators");
   check(!nro.models.map.service.ChangeMapService.gI().goToMap(p,target)&&p.zone==origin,"Closed final commit preserves origin");
   nro.models.map.service.MapService.gI().goToMap(p,target);check(p.zone==origin,"Alternate entry rejects closed event");
   clock.set("15:59:59");check(nro.models.map.service.ChangeMapService.gI().goToMap(p,target),"Late entry allowed");
   check(event.sessionEnd().equals(HirudegarnEventService.sessionEnd(clock.instant())),"Late entry has fixed deadline");
   for(int i=1;i<15;i++)target.addPlayer(player(100+i,null));
   check(!nro.models.map.service.ChangeMapService.gI().goToMap(player(999,origin),target),"Capacity fifteen cannot be bypassed");
   check(nro.models.map.service.MapService.gI().getZone(126)==null,"All zones full returns immediately");
   clock.set("16:00:00");check(!nro.models.map.service.ChangeMapService.gI().goToMap(p,target),"Same-zone refresh rejected at deadline");
  } else if(mode.equals("race")){
   for(int round=0;round<30;round++){
    var destination=zone(126);for(int i=0;i<14;i++)destination.addPlayer(player(i+100,null));
    Owner a=player(1,null),b=player(2,null);var gate=new java.util.concurrent.CountDownLatch(1);var accepts=new java.util.concurrent.atomic.AtomicInteger();
    var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();Thread[] threads=new Thread[2];int i=0;
    for(var p:new Owner[]{a,b})threads[i++]=new Thread(()->{try{gate.await();if(nro.models.map.service.ChangeMapService.gI().goToMap(p,destination))accepts.incrementAndGet();}catch(Throwable t){failure.set(t);}});
    for(var t:threads)t.start();gate.countDown();for(var t:threads){t.join(3000);check(!t.isAlive(),"Concurrent admission finishes");}
    if(failure.get()!=null)throw new AssertionError(failure.get());check(accepts.get()==1&&destination.getNumOfPlayers()==15,"Final slot admits exactly one player");
   }
   Owner master=player(901,target);var pet=new nro.models.player.Pet(master);pet.zone=target;
   var petBoss=new nro.models.mob_bigboss.Hirudegarn(new nro.models.mob.Mob()){@Override public long getTiemNangForPlayer(nro.models.player.Player p,long damage){return 1;}};
   petBoss.point.setHpFull(1000);petBoss.point.hp=1000;petBoss.zone=target;target.mobs.add(petBoss);event.prepareZone(target);
   PinkFixture.replace(Service.class,"instance",new Service(){
    @Override public void sendMessAllPlayerInMap(nro.models.map.Zone z,nro.models.network.Message m){}
    @Override public void addSMTN(nro.models.player.Player p,byte type,long amount,boolean original){p.nPoint.powerUp(amount);p.nPoint.tiemNangUp(amount);}
    @Override public void updatePlayerTotalDamage(nro.models.player.Player p){}
   });
   var petFailure=new java.util.concurrent.atomic.AtomicReference<Throwable>();var gateAvailable=new java.util.concurrent.CountDownLatch(1);
   Thread hit=new Thread(()->{try{petBoss.injured(pet,100,false);}catch(Throwable t){petFailure.set(t);}});hit.setDaemon(true);
   Thread probe=new Thread(()->{synchronized(event.eventGate){gateAvailable.countDown();}});probe.setDaemon(true);boolean available;
   synchronized(master){
    hit.start();long until=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
    while(hit.getState()!=Thread.State.BLOCKED&&hit.isAlive()&&System.nanoTime()<until)Thread.yield();
    check(hit.getState()==Thread.State.BLOCKED,"Pet SMTN waits for its master monitor");
    probe.start();available=gateAvailable.await(1,java.util.concurrent.TimeUnit.SECONDS);
   }
   hit.join(3000);probe.join(3000);check(available&&!hit.isAlive()&&!probe.isAlive()&&petFailure.get()==null,"Pet SMTN never holds event gate while waiting for master");
   PinkFixture.replace(InventoryService.class,"I",new InventoryService(){
    @Override public boolean addItemBag(nro.models.player.Player p,nro.models.item.Item i){synchronized(p){p.inventory.itemsBag.add(i);i.quantity=0;return true;}}
    @Override public void sendItemBags(nro.models.player.Player p){}
   });
   var groundItem=new nro.models.map.ItemMap(target,555,1,100,360,master.id);target.addItem(groundItem);
   var pickupFailure=new java.util.concurrent.atomic.AtomicReference<Throwable>();var itemAvailable=new java.util.concurrent.CountDownLatch(1);
   Thread pickup=new Thread(()->{try{target.pickItem(master,groundItem.itemMapId);}catch(Throwable t){pickupFailure.set(t);}});pickup.setDaemon(true);
   Thread itemProbe=new Thread(()->{synchronized(groundItem){itemAvailable.countDown();}});itemProbe.setDaemon(true);boolean itemUnlocked;
   synchronized(master){
    pickup.start();long until=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
    while(pickup.getState()!=Thread.State.BLOCKED&&pickup.isAlive()&&System.nanoTime()<until)Thread.yield();
    check(pickup.getState()==Thread.State.BLOCKED,"Event pickup waits for player monitor");
    itemProbe.start();itemUnlocked=itemAvailable.await(1,java.util.concurrent.TimeUnit.SECONDS);
   }
   pickup.join(3000);itemProbe.join(3000);check(itemUnlocked&&!pickup.isAlive()&&!itemProbe.isAlive()&&pickupFailure.get()==null,"Event pickup never holds item monitor while waiting for player");
   clock.set("15:59:59");
   var crossingOrigin=new nro.models.map.Zone(origin.map,1,15){@Override public void removePlayer(nro.models.player.Player p){super.removePlayer(p);clock.set("16:00:00");}};
   Owner crossing=player(44,crossingOrigin);
   check(!nro.models.map.service.ChangeMapService.gI().goToMap(crossing,target)&&crossing.zone==crossingOrigin&&crossingOrigin.getPlayers().contains(crossing),"Deadline during origin removal restores original membership");
   clock.set("15:59:59");
   var laterSessionOrigin=new nro.models.map.Zone(origin.map,2,15){@Override public void removePlayer(nro.models.player.Player p){super.removePlayer(p);clock.set("22:00:00");}};
   Owner delayed=player(45,laterSessionOrigin);check(!nro.models.map.service.ChangeMapService.gI().goToMap(delayed,target)&&delayed.zone==laterSessionOrigin,"Old session admission cannot finish in evening session");
   clock.set("15:59:59");
   var selected=nro.models.map.service.MapService.gI().getMapCanJoin(player(3,origin),126,-1);clock.set("16:00:00");
   Owner p=player(4,origin);check(!nro.models.map.service.ChangeMapService.gI().goToMap(p,selected)&&p.zone==origin,"Selection before close cannot commit after close");
  } else if(mode.equals("phases")||mode.equals("deadline")||mode.equals("reset")){
   var boss=boss(target);event.prepareZone(target);var drops=new java.util.concurrent.atomic.AtomicInteger();
   PinkFixture.replace(Service.class,"instance",new Service(){
    @Override public void dropItemMap(nro.models.map.Zone z,nro.models.map.ItemMap item){if(item.itemTemplate.id==190)drops.incrementAndGet();}
    @Override public void sendMessAllPlayerInMap(nro.models.map.Zone z,nro.models.network.Message m){}
   });
   if(mode.equals("phases")){
    var counted=nro.models.mob.Mob.class.getDeclaredField("kolDeathCounted");counted.setAccessible(true);
    for(int cycle=0;cycle<3;cycle++){
     for(int phase=0;phase<3;phase++){
      check(boss.lvMob==phase && boss.point.hp==1000,"Full HP phase "+phase);boss.point.hp=0;boss.lastTimeDie=System.currentTimeMillis();counted.setBoolean(boss,true);
      boss.update();check(boss.lvMob==phase+1,"Immediate transition "+phase);
      if(phase<2){check(boss.point.hp==1000&&!counted.getBoolean(boss),"New life restores HP and quest death flag");boss.update();}
      check(drops.get()==30*(cycle*3+phase+1),"Exactly one gold reward per phase");
     }
     check(boss.isDie()&&boss.lvMob==3,"Final death transition packet state retained");boss.update();
     check(boss.lvMob==0&&boss.point.hp==1000&&!counted.getBoolean(boss),"Next fast tick starts phase one");check(drops.get()==90*(cycle+1),"Respawn adds no rewards");
    }
    int before=drops.get();boss.point.hp=0;var gate=new java.util.concurrent.CountDownLatch(1);var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
    Runnable concurrent=()->{try{gate.await();boss.update();}catch(Throwable t){failure.set(t);}};
    Thread left=new Thread(concurrent),right=new Thread(concurrent);left.start();right.start();gate.countDown();left.join(3000);right.join(3000);
    check(!left.isAlive()&&!right.isAlive()&&failure.get()==null&&drops.get()==before+30&&boss.lvMob==1,"Concurrent death callbacks award once without deadlock");
    var failed=zone(126);target.map.zones.add(0,failed);
    var failedMob=new nro.models.mob_bigboss.Hirudegarn(new nro.models.mob.Mob()){@Override public void update(){throw new IllegalStateException("deliberate isolated boss update failure");}};
    failedMob.zone=failed;failed.mobs.add(failedMob);event.prepareZone(failed);boss.point.hp=0;
    event.tick(target.map);check(boss.lvMob==2&&drops.get()==before+60,"Failed boss cannot skip later zone update");

   } else if(mode.equals("deadline")){
    boss.point.hp=1;clock.set("16:00:00");boss.injured(null,999,true);check(boss.point.hp==1,"Deadline checked before injury");
    boss.point.hp=0;boss.lvMob=2;boss.update();check(boss.lvMob==2&&boss.point.hp==0&&drops.get()==0,"Deadline suppresses death reward and phase respawn");
    boss.lvMob=3;boss.update();check(boss.isDie()&&boss.lvMob==3,"Deadline suppresses final respawn");
    Owner p=player(1,target);boss.point.hp=1000;boss.attack();check(p.hits==0,"Deadline suppresses boss combat");
    clock.set("15:59:59");event.prepareZone(target);boss.lvMob=0;boss.point.hp=0;drops.set(0);target.items.clear();
    PinkFixture.replace(Service.class,"instance",new Service(){
     @Override public void dropItemMap(nro.models.map.Zone z,nro.models.map.ItemMap item){if(item.itemTemplate.id==190){drops.incrementAndGet();clock.set("16:00:00");}}
     @Override public void sendMessAllPlayerInMap(nro.models.map.Zone z,nro.models.network.Message message){}
    });
    boss.update();check(drops.get()==1&&target.items.size()==1&&boss.lvMob==0&&boss.isDie(),"Deadline during loot suppresses actual world rewards and respawn");
    clock.set("15:59:59");event.prepareZone(target);boss.lvMob=0;boss.point.hp=0;boss.effectSkill.isStun=true;var spawnPackets=new java.util.concurrent.atomic.AtomicInteger();
    PinkFixture.replace(Service.class,"instance",new Service(){
     @Override public void dropItemMap(nro.models.map.Zone z,nro.models.map.ItemMap item){}
     @Override public void sendMessAllPlayerInMap(nro.models.map.Zone z,nro.models.network.Message message){if(message.command==-124)clock.set("16:00:00");if(message.command==-9)spawnPackets.incrementAndGet();}
    });
    boss.update();check(boss.isDie()&&spawnPackets.get()==0,"Deadline during effect cleanup suppresses HP refill and spawn packet");


   } else {
    boss.point.hp=0;boss.lvMob=3;boss.effectSkill.isStun=true;boss.effectSkill.isAnTroi=true;boss.effectSkill.isSocola=true;boss.lastTimeDie=123;
    clock.set("16:00:00");check(!event.prepareZone(target),"Closed session");clock.set("22:00:00");check(event.prepareZone(target),"Second session opens");
    check(boss.lvMob==0&&boss.point.hp==1000&&boss.lastTimeDie==0&&!boss.effectSkill.isStun&&!boss.effectSkill.isAnTroi&&!boss.effectSkill.isSocola,"New session restores clean boss");
    boss.point.hp=42;clock.set("22:30:00");check(event.prepareZone(target)&&boss.point.hp==42,"Existing session never resets for late entry");
    check(event.sessionEnd().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).getHour()==23,"Restart halfway retains 23h deadline");
   }
  } else if(mode.equals("exit")){
   PinkFixture.replace(Service.class,"instance",new Service(){
    @Override public void hsChar(nro.models.player.Player p,int hp,int mp){Owner owner=(Owner)p;owner.dead=false;owner.heals++;}
    @Override public void sendThongBao(nro.models.player.Player p,String text){}
    @Override public void sendMessAllPlayerInMap(nro.models.player.Player p,nro.models.network.Message m){if(m.command==-65)((Owner)p).ships++;}
    @Override public void sendMessAllPlayerInMap(nro.models.map.Zone z,nro.models.network.Message m){}
    @Override public void sendMessAnotherNotMeInMap(nro.models.player.Player p,nro.models.network.Message m){}
   });
   Owner p=player(1,target);p.dead=true;p.admin=true;clock.set("16:00:00");nro.models.server.Manager.MAPS.remove(origin.map);
   event.tick(target.map);check(p.zone==target,"Missing destination retains player for retry");
   nro.models.server.Manager.MAPS.add(origin.map);event.tick(target.map);
   check(p.zone==origin&&p.location.x==1000&&p.location.y==360&&p.ships>0,"Retry exits to map19 with ship packet");
   check(p.updates==0&&p.heals==1&&!p.dead,"Fast event tick exits and heals dead administrator without player update");
   Owner newSession=player(2,target);clock.set("16:00:00");
   var closeObserved=new java.util.concurrent.CountDownLatch(1);clock.observed=closeObserved;
   Thread staleClose=new Thread(()->event.tick(target.map));
   synchronized(newSession){staleClose.start();check(closeObserved.await(3,java.util.concurrent.TimeUnit.SECONDS),"Closed callback reached policy");clock.set("22:00:00");}
   staleClose.join(3000);check(!staleClose.isAlive()&&newSession.zone==target&&newSession.ships==0,"Stale closed callback cannot eject new-session occupants");
   event.tick(target.map);check(p.zone==origin,"Stale snapshot cannot move escaped player");
  } else if(mode.equals("login")){
   check(HirudegarnEventService.normalizeSavedMap(126)==19,"Saved event map normalizes even while open");
   check(HirudegarnEventService.normalizeSavedMap(5)==5,"Other saved maps retain location");
  } else throw new AssertionError("Unknown mode "+mode);
 }
 public static void main(String[] args) {
  try {run(args.length==0?"schedule":args[0]);System.out.println("HIRUDEGARN PASS "+(args.length==0?"schedule":args[0]));System.exit(0);}
  catch(Throwable e){e.printStackTrace();System.exit(1);}
 }
}
