package nro.models.services.shenron;

import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import nro.models.item.Item;
import nro.models.player.Inventory;
import nro.models.services.*;
import nro.models.consts.ConstNpc;
import nro.models.player.Player;
import nro.models.map.service.NpcService;
import nro.models.services.PinkFixture;
import static nro.models.services.PinkFixture.*;

public final class NamekWishTest {
 static class DendeFixture extends nro.models.npc_list.Dende {DendeFixture(){super(7,0,0,0,0,0);}@Override public boolean canOpenNpc(Player p){return true;}}
 static int menus;
 static void field(Object o,String n,Object v)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static Object field(Object o,String n)throws Exception{Field f=o.getClass().getDeclaredField(n);f.setAccessible(true);return f.get(o);}
 public static void main(String[] args)throws Exception {
  PinkFixture.install();
  replace(NpcService.class,"i",new NpcService(){@Override public void createMenuRongThieng(Player p,int index,String text,String... choices){menus++;}@Override public void createTutorial(Player p,int avatar,String text){}});
  var dragon=blank(SummonDragonNamek.class);var owner=player(1);owner.zone=zone();owner.zone.map.mapId=7;
  field(dragon,"isShenronAppear",true);field(dragon,"playerSummonShenron",owner);field(dragon,"playerSummonShenronId",1);field(dragon,"mapShenronAppear",owner.zone);field(dragon,"lastTimeShenronWait",System.currentTimeMillis());field(dragon,"select",(byte)-1);
  dragon.showConfirmShenron(owner,ConstNpc.SHOW_SHENRON_NAMEK_CONFIRM,(byte)2);
  check((byte)field(dragon,"select")==-1&&menus==0,"Removed flour index must not change wish or open confirmation");
  var stranger=player(2);stranger.zone=owner.zone;dragon.showConfirmShenron(stranger,ConstNpc.SHOW_SHENRON_NAMEK_CONFIRM,(byte)0);
  check((byte)field(dragon,"select")==-1&&menus==0,"Other player cannot select owner's wish");
  dragon.showConfirmShenron(owner,ConstNpc.SHOW_SHENRON_NAMEK_CONFIRM,(byte)0);check((byte)field(dragon,"select")==0&&menus==1,"Owner can select first wish");
  owner.zone=zone();dragon.showConfirmShenron(owner,ConstNpc.SHOW_SHENRON_NAMEK_CONFIRM,(byte)1);check((byte)field(dragon,"select")==0,"Changed zone rejects wish selection");
  owner.zone=(nro.models.map.Zone)field(dragon,"mapShenronAppear");field(dragon,"lastTimeShenronWait",System.currentTimeMillis()-300001);dragon.showConfirmShenron(owner,ConstNpc.SHOW_SHENRON_NAMEK_CONFIRM,(byte)1);check((byte)field(dragon,"select")==0,"Expired session rejects new wish");
  System.out.println("PASS Namek owner, zone, timeout, removed index guards");
  summon();
  rewards();
  if(args.length>0)sql(args[0]);
 }
 static void summon()throws Exception {
  replace(Service.class,"instance",new Service(){@Override public void sendMessAllPlayer(nro.models.network.Message m){}@Override public void sendMessAllPlayerIgnoreMe(Player p,nro.models.network.Message m){}@Override public void sendThongBao(Player p,String text){}});
  var dragon=blank(SummonDragonNamek.class);replace(SummonDragonNamek.class,"instance",dragon);
  var owner=player(100);owner.zone=zone();owner.zone.map.mapId=7;owner.location=new nro.models.player.Location();owner.idMark=new nro.models.player.IDMark();owner.idMark.setIndexMenu(1);owner.idNRNM=353;
  var clan=new nro.models.clan.Clan();owner.clan=clan;
  var member=new nro.models.clan.ClanMember();member.id=200;clan.members.add(member);
  var old=player(200);old.inventory.itemsBag.add(item(999,1));owner.inventory.itemsBag.add(new Item());
  var registry=Map.<Long,Player>of(owner.id,owner,old.id,old);
  field(dragon,"rewards",new NamekWishRewards(()->{throw new SQLException("No offline recipients expected");},registry::get));
  check(dragon.summonNamec(owner),"First summon accepted");
  var stranger=player(300);stranger.zone=owner.zone;
  check(!dragon.summonNamec(stranger)&&field(dragon,"playerSummonShenron")==owner,"Busy dragon cannot overwrite summon owner");
  var hunt=new nro.models.services_dungeon.NgocRongNamecService();hunt.tOpenNrNamec=777;
  replace(nro.models.services_dungeon.NgocRongNamecService.class,"instance",hunt);
  blank(DendeFixture.class).confirmMenu(owner,1);
  check(hunt.tOpenNrNamec==777&&owner.idNRNM==353,"Busy Dende call cannot consume hunt or reset stones");
  owner.clan=new nro.models.clan.Clan();var replacement=new nro.models.clan.ClanMember();replacement.id=300;owner.clan.members.add(replacement);
  dragon.showConfirmShenron(owner,ConstNpc.SHOW_SHENRON_NAMEK_CONFIRM,(byte)1);dragon.confirmWish(owner);
  var session=(NamekWishRewards.Session)field(dragon,"rewardSession");
  check(session.awards.keySet().equals(new TreeSet<>(List.of(100L,200L)))&&!session.completed&&(boolean)field(dragon,"isShenronAppear"),"Clan switch cannot redirect captured wish; full bag keeps dragon and wish");
  field(dragon,"lastTimeShenronWait",System.currentTimeMillis()-300001);dragon.confirmWish(owner);check(!session.completed,"Expired confirm cannot grant captured reward");
  check(SummonDragonNamek.reconcilePendingForMaintenance(),"Maintenance has no unresolved receipt for blocked capacity");
  System.out.println("PASS Namek summon exclusivity, Dende busy no-consume, original clan snapshot, failed wish retained");
 }
 static void rewards()throws Exception {
  var a=player(10);var b=player(20);a.inventory.itemsBag.add(new Item());b.inventory.itemsBag.add(item(999,1));
  var registry=new HashMap<Long,Player>();registry.put(a.id,a);registry.put(b.id,b);
  var rewards=new NamekWishRewards(()->{throw new SQLException("DB not used for online");},registry::get);
  replace(InventoryService.class,"I",new InventoryService(){@Override public boolean addItemList(List<Item> bag,Item item){
   check(Thread.holdsLock(LiveSyncCommandGate.LIFECYCLE_LOCK)&&Thread.holdsLock(a)&&Thread.holdsLock(b),"Real shared player/lifecycle locks protect staged award");return super.addItemList(bag,item);
  }@Override public void sendItemBags(Player p){}});
  var session=new NamekWishRewards.Session(1,List.of(a.id,b.id,a.id));
  check(session.awards.size()==2,"Duplicate clan IDs grant once");
  var original=a.inventory.itemsBag;Item occupied=b.inventory.itemsBag.get(0);
  check(rewards.grant(session)==NamekWishRewards.Result.BLOCKED&&a.inventory.itemsBag==original&&b.inventory.itemsBag.get(0)==occupied&&!session.completed,"One full bag prevents all clan grants without loss");
  b.inventory.itemsBag.set(0,new Item());b.kolRewardPending=true;
  check(rewards.grant(session)==NamekWishRewards.Result.BLOCKED&&a.inventory.itemsBag==original,"Pending recipient prevents partial grant");b.kolRewardPending=false;
  b.beforeDispose=true;check(rewards.grant(session)==NamekWishRewards.Result.BLOCKED,"Logging-out receiver rejected");b.beforeDispose=false;
  ExecutorService pool=Executors.newFixedThreadPool(2);try{
   var x=pool.submit(()->rewards.grant(session));var y=pool.submit(()->rewards.grant(session));
   check((x.get()==NamekWishRewards.Result.SUCCESS)!=(y.get()==NamekWishRewards.Result.SUCCESS),"Concurrent replay grants once");
  }finally{pool.shutdownNow();}
  check(a.inventory.itemsBag.get(0).template.id==942&&b.inventory.itemsBag.get(0).template.id==942,"Actual staged bags receive retained tiger reward");
  check(rewards.grant(session)==NamekWishRewards.Result.BLOCKED,"Consumed session cannot replay");
  Item possession=item(999,1);a.inventory.itemsBag=new ArrayList<>(List.of(possession,new Item()));b.inventory.itemsBag=new ArrayList<>(List.of(new Item()));
  var oldBag=a.inventory.itemsBag;var identitySession=new NamekWishRewards.Session(1,List.of(a.id,b.id));
  check(rewards.grant(identitySession)==NamekWishRewards.Result.SUCCESS,"Wish succeeds alongside existing possessions");
  check(a.inventory.itemsBag==oldBag&&a.inventory.itemsBag.get(0)==possession,"Successful wish preserves live bag and occupied Item identities for queued gameplay");
  Item stack=item(16,100);a.inventory.itemsBag=new ArrayList<>(List.of(stack,new Item()));b.inventory.itemsBag=new ArrayList<>(List.of(new Item()));
  var stackBag=a.inventory.itemsBag;var mergeSession=new NamekWishRewards.Session(0,List.of(a.id,b.id));int awardQuantity=mergeSession.awards.get(a.id).quantity;
  check(rewards.grant(mergeSession)==NamekWishRewards.Result.SUCCESS&&a.inventory.itemsBag==stackBag&&a.inventory.itemsBag.get(0)==stack&&stack.quantity==100+awardQuantity,"Merged reward updates exact existing stack without replacing queued Item references");
  var bag=new ArrayList<Item>();Item timed=item(900,1);timed.createTime=1234;timed.itemOptions.add(new Item.ItemOption(93,17));bag.add(timed);bag.add(new Item());
  String encoded=NamekWishRewards.encodeBag(bag);var decoded=NamekWishRewards.decodeBag(encoded);
  check(decoded.size()==Inventory.MAX_ITEMS_BAG&&decoded.get(0).createTime==1234&&decoded.get(0).itemOptions.get(0).param==17,"Offline save format preserves timer/options and pads ordinary bag");
  for(String invalid:List.of("oops","null","[\"[]\"]")){try{NamekWishRewards.decodeBag(invalid);throw new AssertionError("Invalid bag accepted");}catch(IllegalArgumentException expected){}}
  System.out.println("PASS Namek actual capacity, all-or-none, pending/logout, locks, concurrency/replay, codec");
 }
 static void sql(String schema)throws Exception {
  check(schema.startsWith("codex_namek_test_"),"Only isolated test schema allowed");
  Class.forName("com.mysql.jdbc.Driver");
  String url="jdbc:mysql://127.0.0.1:3306/"+schema+"?useSSL=false&allowPublicKeyRetrieval=true";
  NamekWishRewards.Connections connections=()->DriverManager.getConnection(url,"root","");
  replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}});
  var online=player(10);online.inventory.itemsBag.add(new Item());var registry=new HashMap<Long,Player>();registry.put(10L,online);
  try(Connection c=connections.open()){
   try(PreparedStatement q=c.prepareStatement("INSERT INTO player(id,items_bag,untouched) VALUES(20,?,'preserve')")){q.setString(1,NamekWishRewards.encodeBag(List.of(new Item())));q.executeUpdate();}
  }
  AtomicBoolean lost=new AtomicBoolean(true);
  NamekWishRewards.Connections uncertain=()->{
   Connection real=connections.open();
   return (Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(proxy,method,argv)->{
    try{Object value=method.invoke(real,argv);if(method.getName().equals("commit")&&lost.getAndSet(false))throw new SQLException("Lost commit acknowledgement");return value;}
    catch(java.lang.reflect.InvocationTargetException failure){throw failure.getCause();}
   });
  };
  var service=new NamekWishRewards(uncertain,registry::get);var session=new NamekWishRewards.Session(1,List.of(10L,20L));
  check(service.grant(session)==NamekWishRewards.Result.PENDING&&session.uncertain&&!online.inventory.itemsBag.get(0).isNotNullItem()&&LiveSyncCommandGate.hasPendingId(20),"Uncertain offline commit gates login and withholds online publication");
  var broken=new NamekWishRewards(()->{throw new SQLException("unavailable");},registry::get);
  check(broken.grant(session)==NamekWishRewards.Result.PENDING&&LiveSyncCommandGate.hasPendingId(20),"Unavailable reconciliation retains pending identity");
  check(service.grant(session)==NamekWishRewards.Result.SUCCESS&&online.inventory.itemsBag.get(0).template.id==942&&!LiveSyncCommandGate.hasPendingId(20),"Receipt reconciliation publishes online reward and releases login gate");
  try(Connection c=connections.open();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT items_bag,untouched,(SELECT COUNT(*) FROM namek_wish_receipt) FROM player WHERE id=20")){
   check(r.next(),"Offline player remains");var loaded=NamekWishRewards.decodeBag(r.getString(1));check(loaded.stream().filter(i->i.isNotNullItem()&&i.template.id==942).count()==1&&r.getString(2).equals("preserve")&&r.getInt(3)==1,"Offline reward exactly once and unrelated columns unchanged");
  }
  // All offline rows stage before UPDATE: one full recipient rolls back every grant.
  var full=new ArrayList<Item>();for(int i=0;i<Inventory.MAX_ITEMS_BAG;i++)full.add(item(999,1));
  try(Connection c=connections.open();PreparedStatement q=c.prepareStatement("INSERT INTO player(id,items_bag,untouched) VALUES(30,?,'full'),(40,'[]','empty')")){q.setString(1,NamekWishRewards.encodeBag(full));q.executeUpdate();}
  var all=new NamekWishRewards(connections,id->null);var batch=new NamekWishRewards.Session(1,List.of(30L,40L));
  check(all.grant(batch)==NamekWishRewards.Result.BLOCKED,"Full offline bag blocks entire batch");
  try(Connection c=connections.open();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT items_bag,(SELECT COUNT(*) FROM namek_wish_receipt) FROM player WHERE id=40")){check(r.next()&&r.getString(1).equals("[]")&&r.getInt(2)==1,"Blocked offline batch changes no bag or receipt");}
  System.out.println("PASS Namek isolated SQL all-or-none, bag-only update, receipt retry, uncertain commit, login gate");
 }
}
