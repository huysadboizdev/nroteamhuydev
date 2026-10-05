package nro.models.task;
import java.lang.reflect.*;
import java.util.*;
import sun.misc.Unsafe;
import com.google.gson.Gson;
import nro.models.player.*;
import nro.models.item.*;
import nro.models.map.*;
import nro.models.boss.Boss;
import nro.models.mob.*;
import nro.models.network.*;
import nro.models.player_system.Template;
import nro.models.services.*;
import nro.models.database.*;
public final class KolQuestBehaviorTest {
 static Unsafe u;
 static {try{Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);u=(Unsafe)f.get(null);}catch(Exception e){throw new RuntimeException(e);}}
 static <T> T blank(Class<T> c)throws Exception{return c.cast(u.allocateInstance(c));}
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static void replace(Class<?> c,String f,Object value)throws Exception{Field field=c.getDeclaredField(f);field.setAccessible(true);field.set(null,value);}
 public static Player player()throws Exception {Player p=blank(Player.class);p.isPlayer=true;p.id=1;p.kolQuestState=new KolQuestState();p.inventory=new Inventory();p.event=new PlayerEvent(p);p.itemTime=blank(ItemTime.class);p.session=blank(MySession.class);p.session.userId=1;p.itemTime.isUseTDLT=true;p.itemTime.lastTimeUseTDLT=System.currentTimeMillis();p.itemTime.timeTDLT=3600000;return p;}
 static Zone zone(int map)throws Exception{nro.models.map.Map m=blank(nro.models.map.Map.class);m.mapId=map;return new Zone(m,0,20);}
 static Mob mob(int id,int map)throws Exception{Mob m=blank(Mob.class);m.tempId=id;m.zone=zone(map);return m;}
 static Boss boss(int id,int level,int map)throws Exception{Boss b=blank(Boss.class);b.id=id;b.currentLevel=level;b.zone=zone(map);return b;}
 static KolQuestService service(){return new KolQuestService(new KolRewardDAO(()->{throw new java.sql.SQLException("offline");}));}
 static void state() {
  KOLProgressData d=KOLProgressData.parse("{\"kolQuestStage\":6,\"monsterKillCountAutoTrain\":12345}");
  check(d.kolQuestStage==6&&d.monsterKillCountAutoTrain==12345,"legacy preserved");check(d.v2.valid()&&d.v2.normalStage==1&&d.v2.woodenDummyKills==0,"legacy creates zero v2");
  d.v2.fide1Kills=12;d.v2.normalStage=4;d.v2.vipStage=2;KOLProgressData saved=KOLProgressData.parse(new Gson().toJson(d));check(saved.v2.fide1Kills==12&&saved.v2.normalStage==4&&saved.v2.vipStage==2,"relogin roundtrip");
  for(String json:List.of("broken","[]","{\"v2\":null}","{\"v2\":{}}","{\"v2\":{\"version\":2,\"normalStage\":9,\"vipStage\":1}}","{\"v2\":{\"version\":2,\"normalStage\":1,\"vipStage\":1,\"fide1Kills\":-1}}"))check(!KOLProgressData.parse(json).v2.valid(),"malformed fail closed: "+json);
  String prefix="{\"v2\":{\"version\":2,\"normalStage\":1,\"vipStage\":1,";
  for(String fragment:List.of("\"fide1Kills\":1.5","\"fide1Kills\":null","\"fide1Kills\":\"1\"","\"fide1Kills\":9223372036854775808"))check(!KOLProgressData.parse(prefix+fragment+"}}").v2.valid(),"strict counter "+fragment);
  check(KOLProgressData.parse(prefix+"\"fide1Kills\":9223372036854775807}}").v2.fide1Kills==Long.MAX_VALUE,"exact max supported");
  for(String fragment:List.of("\"version\":2.5,\"normalStage\":1,\"vipStage\":1","\"version\":2,\"normalStage\":1.5,\"vipStage\":1","\"version\":2,\"normalStage\":null,\"vipStage\":1","\"version\":2,\"normalStage\":\"1\",\"vipStage\":1"))check(!KOLProgressData.parse("{\"v2\":{"+fragment+"}}").v2.valid(),"strict stages");
  check(KolQuestState.add(Long.MAX_VALUE-1,20)==Long.MAX_VALUE,"counter saturation");
 }
 static void catalog(){
  check(KolQuestCatalog.description(1).contains("M\u1ed9c nh\u00e2n") && KolQuestCatalog.description(1).contains("T\u1ef1 \u0111\u1ed9ng luy\u1ec7n t\u1eadp") && KolQuestCatalog.description(5).contains("Capsule h\u1ed3ng"),"Vietnamese descriptions preserved");
  for(int id=1;id<=7;id++)check(!KolQuestCatalog.description(id).contains("?"),"no encoding damage");
  int nv=0,np=0,vv=0,vp=0;long[] requirements={10000,100000,10,3,200,10,10};
  for(int i=1;i<=7;i++){var a=KolQuestCatalog.reward(i,false);var b=KolQuestCatalog.reward(i,true);nv+=a.vnd();np+=a.points();vv+=b.vnd();vp+=b.points();check(KolQuestCatalog.required(i)==requirements[i-1],"threshold "+i);}
  check(nv==2800000&&np==71000&&vv==3150000&&vp==124000,"exact total catalog");
  KolQuestState s=new KolQuestState();s.fide1Kills=30;check(KolQuestCatalog.progress(s,3)==0,"one form never creates turns");s.fide1Kills=12;s.fide2Kills=10;s.fide3Kills=11;check(KolQuestCatalog.progress(s,3)==10,"turn minimum");check(KolQuestCatalog.progress(s,1)==0,"counters independent");
 }
 static void tracking()throws Exception{
  var k=service();Player p=player();Mob dummy=mob(0,5);k.onMobKilled(p,dummy);check(p.kolQuestState.woodenDummyKills==1,"dummy valid");
  k.onMobKilled(p,mob(1,5));p.itemTime.isUseTDLT=false;k.onMobKilled(p,dummy);p.itemTime.isUseTDLT=true;p.itemTime.lastTimeUseTDLT=System.currentTimeMillis()-4000000;k.onMobKilled(p,dummy);check(p.kolQuestState.woodenDummyKills==1,"wrong mob/off/expired TDLT excluded");p.itemTime.lastTimeUseTDLT=System.currentTimeMillis();
  for(int map:new int[]{77,81,8,76})k.onMobKilled(p,mob(50,map));check(p.kolQuestState.birdDemonKills==2,"bird maps exact");
  for(String f:List.of("isBot","isPet","isBoss","isNewPet","isNewPet1")){Player actor=player();Player.class.getField(f).setBoolean(actor,true);k.onMobKilled(actor,dummy);check(actor.kolQuestState.woodenDummyKills==0,"excluded "+f);}
  Method guard=Mob.class.getDeclaredMethod("claimKolDeath");guard.setAccessible(true);check((boolean)guard.invoke(dummy)&&!(boolean)guard.invoke(dummy),"one mob event per death");dummy.point=new MobPoint(dummy);dummy.point.maxHp=100;dummy.hoiSinh();check((boolean)guard.invoke(dummy),"mob guard resets on respawn");
 }
 static void bosses()throws Exception{
  var k=service();Player p=player();for(int i=0;i<10;i++)for(int level=0;level<3;level++)k.onBossKilled(p,boss(-28,level,1));check(KolQuestCatalog.progress(p.kolQuestState,3)==10,"ten full Fide turns");
  k.onBossKilled(p,boss(-101,0,103));k.onBossKilled(p,boss(-101,1,103));k.onBossKilled(p,boss(-100,2,100));check(p.kolQuestState.perfectCellKills==1,"perfect Cell identity level map");
  k.onBossKilled(p,boss(-203,0,1));k.onBossKilled(p,boss(-203,1,1));check(p.kolQuestState.blackGokuKills==1&&p.kolQuestState.superBlackGokuKills==1,"Black forms independent");
  Boss b=boss(-28,0,1);Method guard=Boss.class.getDeclaredMethod("claimKolDeath");guard.setAccessible(true);check((boolean)guard.invoke(b)&&!(boolean)guard.invoke(b),"boss duplicate guard");Method reset=Boss.class.getDeclaredMethod("resetBase");reset.setAccessible(true);reset.invoke(b);check((boolean)guard.invoke(b),"boss form reset");
  check(KOLProgressData.parse(new Gson().toJson(KOLProgressData.snapshot(p,p.kolQuestState))).v2.fide2Kills==10,"Fide relog retains counters");
 }
 static class Owner extends Player {boolean failPacket;@Override public void sendMessage(Message msg){if(failPacket)throw new RuntimeException("network fixture");}}
 static void pickup()throws Exception{
  replace(ItemService.class,"i",new ItemService(){@Override public Item createItemFromItemMap(ItemMap m){Item i=new Item();i.template=m.itemTemplate;i.quantity=m.quantity;return i;}});
  final boolean[] allow={true};replace(InventoryService.class,"I",new InventoryService(){@Override public boolean addItemBag(Player p,Item i){if(allow[0])i.quantity=0;return allow[0];}@Override public void sendItemBags(Player p){}});
  replace(Service.class,"instance",new Service(){@Override public void sendThongBao(Player p,String s){}@Override public void sendToAntherMePickItem(Player p,int id){}});
  replace(TaskService.class,"i",new TaskService(){@Override public void checkDoneTaskPickItem(Player p,ItemMap m){}@Override public void checkDoneSideTaskPickItem(Player p,ItemMap m){}@Override public void checkDoneClanTaskPickItem(Player p,ItemMap m){}});
  Owner p=blank(Owner.class);p.isPlayer=true;p.id=1;p.kolQuestState=new KolQuestState();p.session=blank(MySession.class);p.failPacket=true;Zone z=zone(5);p.zone=z;
  ItemMap drop=blank(ItemMap.class);drop.itemTemplate=new Template.ItemTemplate();drop.itemTemplate.id=722;drop.itemTemplate.type=27;drop.quantity=20;drop.itemMapId=1;drop.playerId=1;z.items.add(drop);
  z.pickItem(p,1);check(p.kolQuestState.pinkCapsulesPicked==20&&drop.isPickedUp&&!z.items.contains(drop),"pickup committed despite network failure, preserved stack quantity");z.pickItem(p,1);check(p.kolQuestState.pinkCapsulesPicked==20,"duplicate packet excluded");
  drop.itemMapId=2;drop.isPickedUp=false;z.items.add(drop);allow[0]=false;z.pickItem(p,2);check(p.kolQuestState.pinkCapsulesPicked==20&&!drop.isPickedUp,"full bag no count");allow[0]=true;drop.playerId=9;z.pickItem(p,2);check(p.kolQuestState.pinkCapsulesPicked==20,"wrong owner no count");drop.playerId=1;drop.itemTemplate.id=861;z.pickItem(p,2);check(p.kolQuestState.pinkCapsulesPicked==20,"ruby excluded");
 }
 static void rewards()throws Exception{
  Player p=player();var k=service();p.kolQuestState.woodenDummyKills=10000;check(k.eligibility(p,false,1)==null,"normal no ticket");check(k.eligibility(p,true,1)!=null,"VIP ticket required");Item t=new Item();t.template=new Template.ItemTemplate();t.template.id=1825;t.quantity=1;p.inventory.itemsBox.add(t);check(k.eligibility(p,true,1)!=null,"box ticket excluded");p.inventory.itemsBag.add(t);check(k.eligibility(p,true,1)==null,"bag ticket allowed");
  check(!k.claim(p,true,1).success()&&p.kolQuestState.vipStage==1&&p.event.getEventPoint()==0&&t.quantity==1,"DB failure no RAM mutation no ticket debit");check(k.eligibility(p,false,2)!=null,"expected menu stage mismatch");p.kolQuestState.normalStage=8;check(k.eligibility(p,false,8)!=null,"finished no reward");
  Thread a=new Thread(()->{for(int i=0;i<10000;i++)p.event.addEventPoint(1);});Thread b=new Thread(()->{for(int i=0;i<10000;i++)p.event.addEventPoint(1);});a.start();b.start();a.join();b.join();check(p.event.getEventPoint()==20000,"event operations use player monitor");
 }
 static void menu()throws Exception{Player p=player();p.kolQuestState.normalStage=3;p.kolQuestState.fide1Kills=12;p.kolQuestState.fide2Kills=10;p.kolQuestState.fide3Kills=11;String text=service().menuText(p,false);check(text.contains("\u0111i\u1ec3m"),"Unicode point text");check(text.contains("10/10")&&text.contains("12/10/11")&&text.contains("300000 VND + 3000"),"Fide menu progress and reward");p.kolQuestState.vipStage=7;check(service().menuText(p,true).contains("750000 VND + 100000")&&service().menuText(p,true).contains("1825"),"VIP seven reward and ticket");p.kolQuestState.vipStage=8;check(service().menuText(p,true).contains("ho\u00e0n th\u00e0nh"),"finished menu");}

 static void actualHooks()throws Exception {
  replace(Service.class,"instance",new Service(){@Override public void updatePlayerTotalDamage(Player p){}});
  replace(TaskService.class,"i",new TaskService(){
   @Override public void checkDoneTaskKillMob(Player p,Mob m){}
   @Override public void checkDoneSideTaskKillMob(Player p,Mob m){}
   @Override public void checkDoneClanTaskKillMob(Player p,Mob m){}
   @Override public void checkDoneTaskKillBoss(Player p,Boss b){}
  });
  Player p=player();Mob m=mob(0,164);m.point=new MobPoint(m);m.point.hp=m.point.maxHp=100;m.temporaryEnemies=new ArrayList<>();
  try { m.injured(p,10,true); } catch(NullPointerException fixtureIncomplete) {}
  check(m.point.hp==90 && p.kolQuestState.woodenDummyKills==0,"nonlethal real injury does not count");
  try {m.injured(p,100,true);}catch(NullPointerException fixtureIncomplete){}
  check(m.point.hp==0 && p.kolQuestState.woodenDummyKills==1,"real mob death hook");
  m.injured(p,100,true);check(p.kolQuestState.woodenDummyKills==1,"repeat injury on corpse excluded");
  m.hoiSinh();try {m.injured(p,100,true);}catch(NullPointerException fixtureIncomplete){}
  check(p.kolQuestState.woodenDummyKills==2,"real respawn second death");
  Boss b=boss(-28,0,5);b.name="fixture";
  try {b.die(p);}catch(NullPointerException fixtureIncomplete){}
  try {b.die(p);}catch(NullPointerException fixtureIncomplete){}
  check(p.kolQuestState.fide1Kills==1,"real boss death duplicate excluded");
 }

 static class MenuNpc extends nro.models.npc_list.QuyLaoKame {
  int menu;String[] buttons;
  MenuNpc(){super(5,1,0,0,0,0);}
  @Override public void createOtherMenu(Player p,int index,String text,String... buttons){menu=index;this.buttons=buttons;}
  @Override public void openBaseMenu(Player p){menu=nro.models.consts.ConstNpc.BASE_MENU;p.kolMenuQuest=0;}
 }
 static void menuContext()throws Exception {
  final int[] commits={0};
  KolRewardDAO dao=new KolRewardDAO(()->{throw new java.sql.SQLException("no live database allowed");}) {
   @Override public Balance claim(long id,int account,boolean vip,int quest,int vnd,int points,int live,String json){commits[0]++;return new Balance(vnd,live+points,json,true);}
  };
  Field f=KolQuestService.class.getDeclaredField("INSTANCE");u.putObject(u.staticFieldBase(f),u.staticFieldOffset(f),new KolQuestService(dao));
  replace(Service.class,"instance",new Service(){@Override public void sendThongBao(Player p,String message){}});
  MenuNpc npc=blank(MenuNpc.class);Player p=player();p.kolQuestState.woodenDummyKills=10000;p.kolQuestState.birdDemonKills=100000;
  Method open=nro.models.npc_list.QuyLaoKame.class.getDeclaredMethod("handleKOLQuest",Player.class,boolean.class);open.setAccessible(true);
  Method click=nro.models.npc_list.QuyLaoKame.class.getDeclaredMethod("handleKOLQuestRewardConfirm",Player.class,int.class,boolean.class);click.setAccessible(true);
  open.invoke(npc,p,false);check(p.kolMenuQuest==1&&npc.buttons.length==2,"ready menu captures exact quest branch");
  click.invoke(npc,p,0,false);check(commits[0]==1&&p.kolQuestState.normalStage==2&&p.kolMenuQuest==0&&npc.menu==nro.models.consts.ConstNpc.BASE_MENU,"success consumes context and returns base");
  click.invoke(npc,p,0,false);check(commits[0]==1&&p.kolQuestState.normalStage==2,"second click cannot claim already eligible next quest");
  open.invoke(npc,p,true);check(p.kolMenuQuest==0&&npc.buttons.length==1,"missing VIP ticket only close button");click.invoke(npc,p,0,true);check(commits[0]==1,"close index zero never claims");
  p.kolQuestState.normalStage=8;open.invoke(npc,p,false);check(p.kolMenuQuest==0&&npc.buttons.length==1,"finished menu no claim context");
 }
 public static void main(String[] args)throws Exception{switch(args[0]){case "state"->state();case "catalog"->catalog();case "tracking"->tracking();case "hooks"->actualHooks();case "boss"->bosses();case "pickup"->pickup();case "reward"->rewards();case "menu"->menu();case "context"->menuContext();default->throw new AssertionError(args[0]);}System.out.println("PASS KOL "+args[0]);System.exit(0);}
}
