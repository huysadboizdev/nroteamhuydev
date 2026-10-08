package nro.models.services;

import java.sql.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import nro.models.database.GiftCodeClaimDAO;
import nro.models.player.Player;
import nro.models.item.Item;
import nro.models.server.Manager;
import nro.models.player_system.Template;
import com.google.gson.*;

/** Pure fixtures by default. Optional DB mode accepts ONLY a newly-created codex_gift_test_* schema. */
public final class LiveSyncGiftClaimTest {
 static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
 static void reject(Runnable r,String message){try{r.run();throw new AssertionError(message);}catch(IllegalArgumentException expected){}}
 static Player player(long id){Player p=new Player();p.id=id;p.inventory.gold=2;p.inventory.gem=Integer.MAX_VALUE-2;p.inventory.ruby=Integer.MAX_VALUE-2;return p;}
 static void unit()throws Exception {
  var history=new nro.models.player.GiftCode();history.add(" abc ");check(history.isUsedGiftCode("ABC"),"legacy history must use canonical identity");history.add("aBc");check(history.rewards.size()==1,"canonical history duplicated");
  for(String raw:List.of("","a b","ab\0c","\u00e9","A".repeat(101)))reject(()->nro.models.player.GiftCode.canonical(raw),"invalid canonical accepted");
  Player p=player(1);
  GiftCodeClaimDAO.Catalog money=new GiftCodeClaimDAO.Catalog(1,"MONEY",-1,"[{\"id\":-1,\"quantity\":2147483647},{\"id\":-2,\"quantity\":2147483647},{\"id\":-3,\"quantity\":2147483647}]");
  var reward=GiftCodeService.stage(p,10,money);check(reward.gold()==2147483649L&&reward.gem()==200000000&&reward.ruby()==200000000,"currency overflow");
  check(p.inventory.gold==2&&p.inventory.ruby==Integer.MAX_VALUE-2&&p.giftCode.rewards.isEmpty(),"staging mutated player");check(reward.bag().isEmpty(),"money required bag slot");
  p.inventory.gold=500_000_000_000L;
  var gold=new GiftCodeClaimDAO.Catalog(1,"HIGHGOLD",1,"[{\"id\":-1,\"quantity\":1000000}]");
  check(GiftCodeService.stage(p,10,gold).gold()==500_001_000_000L,"high gold reset to int cap");
  p.inventory.gold=nro.models.player.Inventory.LIMIT_GOLD-1;
  check(GiftCodeService.stage(p,10,gold).gold()==nro.models.player.Inventory.LIMIT_GOLD,"gold cap overflow");
  Template.ItemTemplate t=new Template.ItemTemplate();t.id=457;t.type=5;t.name="fixture";Manager.ITEM_TEMPLATES.add(t);
  for(int id:new int[]{30,73}){Template.ItemOptionTemplate o=new Template.ItemOptionTemplate();o.id=id;o.name="fixture";Manager.ITEM_OPTION_TEMPLATES.add(o);}
  p.inventory.itemsBag.add(new Item());
  var gift=new GiftCodeClaimDAO.Catalog(2,"ITEM",1,"[{\"id\":457,\"quantity\":1,\"options\":[]}]");
  var staged=GiftCodeService.stage(p,10,gift);check(staged.bag().get(0).template.id==457,"457 omitted");check(staged.bag().get(0).itemOptions.stream().anyMatch(o->o.optionTemplate.id==30),"457 option missing");check(p.inventory.itemsBag.get(0).template==null,"item staging mutated real bag");
  p.inventory.itemsBag=staged.bag();reject(()->GiftCodeService.stage(p,10,gift),"full bag gift accepted");
  check(p.giftCode.rewards.isEmpty(),"failed staging consumed code");
  var manager=new nro.models.managers.GiftCodeManager();var cached=new nro.models.player_system.GiftCode();cached.id=4;cached.code="CACHE";cached.countLeft=1;manager.listGiftCode.add(cached);
  manager.publishClaim("CACHE",4,2);check(cached.countLeft==1,"late reconciliation refilled newer cache");
  cached.countLeft=-1;manager.publishClaim("CACHE",4,-1);check(cached.countLeft==-1,"unlimited cache changed");
  pendingLogout(true);pendingLogout(false);
 }
 static final class PendingDAO extends GiftCodeClaimDAO {
  boolean available,committed;int claims,reads;
  PendingDAO(boolean committed){this.committed=committed;}
  @Override public Receipt claim(long player,int account,String code,String token,Stage stage)throws Exception {
   claims++;stage.prepare(new Catalog(4,code,5,"[{\"id\":-1,\"quantity\":10}]"));
   throw new CommitUncertain(new SQLException("Lost commit acknowledgement"));
  }
  @Override public Receipt reconcile(long player,int account,String code,String token)throws SQLException {
   reads++;if(!available)throw new SQLException("DB unavailable");return committed?new Receipt(4,4):null;
  }
 }
 static <T>T blank(Class<T> type)throws Exception {
  Class<?> unsafe=Class.forName("sun.misc.Unsafe");Field field=unsafe.getDeclaredField("theUnsafe");field.setAccessible(true);
  return type.cast(unsafe.getMethod("allocateInstance",Class.class).invoke(field.get(null),type));
 }
 static void pendingLogout(boolean committed)throws Exception {
  PendingDAO dao=new PendingDAO(committed);Player p=player(committed?88001:88002);p.isPlayer=true;
  p.session=blank(nro.models.network.MySession.class);p.session.userId=(int)p.id;p.session.player=p;
  boolean[] saved={false};int[] saves={0};
  var service=new GiftCodeService(dao,player->{saves[0]++;check(GiftCodeService.isFinalizingLogout(player),"unchecked gift logout save");check(GiftCodeService.hasPendingAccount((int)p.id),"account gate cleared before logout save");return saved[0];});
  check(!service.claim(p,"PENDING"),"uncertain outcome reported success");
  check(p.inventory.gold==2&&p.giftCode.rewards.isEmpty(),"uncertain commit mutated RAM");
  check(GiftCodeService.hasPending(p)&&GiftCodeService.hasPendingId(p.id)&&GiftCodeService.hasPendingAccount((int)p.id),"uncertain identity gates missing");
  check(!service.claim(p,"OTHER")&&dao.claims==1,"pending gift started a second transaction");
  p.beforeDispose=true;check(!service.reconcilePending(p)&&!service.drainPending(),"unknown outcome drained before recovery");
  dao.available=true;check(!service.reconcilePending(p)&&saves[0]==0,"logout saved before cleanup completed");
  p.liveLogoutCleanupCompleted=true;check(!service.reconcilePending(p),"failed logout save cleared pending");
  long expected=committed?12:2;check(p.inventory.gold==expected&&p.giftCode.rewards.size()==(committed?1:0),"recovery did not match durable outcome");
  check(!service.reconcilePending(p)&&p.inventory.gold==expected,"recovery replayed reward");
  saved[0]=true;check(service.drainPending(),"resolved gift did not drain");
  check(p.giftLogoutSaved&&p.liveLogoutCompleted,"gift logout must remain saved/completed after gates clear");
  nro.models.database.PlayerDAO.updatePlayer(p); // Must stop before touching disposed fields or SQL.
  check(!GiftCodeService.hasPendingId(p.id)&&!GiftCodeService.hasPendingAccount((int)p.id)&&dao.claims==1,"gates or idempotent claim broken");
 }
 static String dbUrl;
 static Connection db()throws SQLException{return DriverManager.getConnection(dbUrl,"root","");}
 static long scalar(String sql)throws Exception{try(Connection c=db();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
 static void sql(String sql)throws Exception{try(Connection c=db();Statement s=c.createStatement()){s.execute(sql);}}
 static GiftCodeClaimDAO.Snapshot snapshot(long id,int account){return new GiftCodeClaimDAO.Snapshot(id,account,"[100,5,6,0,0]","[]","[\"ABC\"]");}
 static void refusal(GiftCodeClaimDAO dao,long player,int account,String code)throws Exception{try{dao.claim(player,account,code,UUID.randomUUID().toString(),g->snapshot(player,account));throw new AssertionError("Claim should be refused");}catch(SQLException expected){}}
 static void database(String name)throws Exception {
  Class.forName("com.mysql.jdbc.Driver");
  check(name.matches("codex_gift_test_[A-Za-z0-9_]+"),"unsafe database name");dbUrl="jdbc:mysql://localhost:3306/"+name+"?useSSL=false&serverTimezone=UTC";
  check(scalar("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='giftcode_claim'")==1,"migration not installed in isolated schema");
  GiftCodeClaimDAO dao=new GiftCodeClaimDAO();dao=new GiftCodeClaimDAO(LiveSyncGiftClaimTest::db);
  String token=UUID.randomUUID().toString();var receipt=dao.claim(1,10," abc ",token,g->snapshot(1,10));
  check(receipt.remaining()==1&&scalar("SELECT count_left FROM giftcode WHERE code='ABC'")==1,"quota not consumed once");check(scalar("SELECT COUNT(*) FROM giftcode_claim WHERE account_id=10 AND canonical_code='ABC'")==1,"claim missing");
  check(scalar("SELECT JSON_EXTRACT(data_inventory,'$[5]') FROM player WHERE id=1")==777,"stored extension overwritten");
  dao.claim(1,10,"ABC",token,g->{throw new AssertionError("idempotent retry staged twice");});check(scalar("SELECT count_left FROM giftcode WHERE code='ABC'")==1,"same token consumed twice");
  refusal(dao,1,10,"ABC");sql("DELETE FROM giftcode WHERE code='ABC'");sql("INSERT INTO giftcode(code,count_left,detail,expired) VALUES('ABC',10,'[{\"id\":-1,\"quantity\":2}]','2037-12-31')");refusal(dao,1,10,"abc");
  sql("DELETE FROM player WHERE id=1");sql("INSERT INTO player(id,account_id,data_inventory,items_bag,giftcode) VALUES(99,10,'[0,0,0,0,0]','[]','[]')");refusal(dao,99,10,"ABC");
  refusal(dao,2,20,"EXPIRED");check(scalar("SELECT count_left FROM giftcode WHERE code='EXPIRED'")==5,"expired quota changed");
  try{dao.claim(2,20,"FAIL",UUID.randomUUID().toString(),g->{throw new IllegalArgumentException("full bag");});throw new AssertionError("invalid stage accepted");}catch(IllegalArgumentException expected){}
  check(scalar("SELECT count_left FROM giftcode WHERE code='FAIL'")==5&&scalar("SELECT COUNT(*) FROM giftcode_claim WHERE canonical_code='FAIL'")==0,"refusal consumed quota/claim");
  long creation=scalar("SELECT UNIX_TIMESTAMP(datecreate) FROM giftcode WHERE code='UNLIMITED'");
  dao.claim(2,20,"UNLIMITED",UUID.randomUUID().toString(),g->snapshot(2,20));check(scalar("SELECT count_left FROM giftcode WHERE code='UNLIMITED'")==-1,"unlimited changed");
  check(scalar("SELECT UNIX_TIMESTAMP(datecreate) FROM giftcode WHERE code='UNLIMITED'")==creation,"datecreate changed on consume");
  refusal(dao,2,20,"OLD");refusal(dao,3,30,"DELETED_OLD");
  var pool=Executors.newFixedThreadPool(2);GiftCodeClaimDAO concurrent=dao;List<Future<Boolean>> results=new ArrayList<>();
  for(int index=2;index<=3;index++){int id=index;results.add(pool.submit(()->{try{concurrent.claim(id,id*10,"LAST",UUID.randomUUID().toString(),g->snapshot(id,id*10));return true;}catch(SQLException exhausted){return false;}}));}
  int successes=0;for(Future<Boolean> f:results)if(f.get(10,TimeUnit.SECONDS))successes++;pool.shutdownNow();check(successes==1&&scalar("SELECT count_left FROM giftcode WHERE code='LAST'")==0,"concurrent last quota claimed twice");
  // Throw before reward UPDATE: claim and quota rollback together.
  GiftCodeClaimDAO broken=new GiftCodeClaimDAO(()->wrap(db(),"update",false));
  refusal(broken,2,20,"SQLFAIL");check(scalar("SELECT count_left FROM giftcode WHERE code='SQLFAIL'")==5&&scalar("SELECT COUNT(*) FROM giftcode_claim WHERE canonical_code='SQLFAIL'")==0,"SQL rollback leaked claim/quota");
  // Commit succeeded but response lost: recovery reopens only AFTER the original connection closes.
  int[] opens={0};GiftCodeClaimDAO lost=new GiftCodeClaimDAO(()->{opens[0]++;return opens[0]==1?wrap(db(),"commit",true):db();});
  var recovered=lost.claim(2,20,"LOST",UUID.randomUUID().toString(),g->snapshot(2,20));check(recovered.remaining()==4&&scalar("SELECT count_left FROM giftcode WHERE code='LOST'")==4,"lost commit consumed twice");
  int[] failopens={0};GiftCodeClaimDAO unavailable=new GiftCodeClaimDAO(()->{if(++failopens[0]>1)throw new SQLException("recovery unavailable");return wrap(db(),"commit",true);});
  String uncertainToken=UUID.randomUUID().toString();try{unavailable.claim(2,20,"UNCERTAIN",uncertainToken,g->snapshot(2,20));throw new AssertionError("uncertain commit silently refused");}catch(GiftCodeClaimDAO.CommitUncertain expected){}
  check(dao.reconcile(2,20,"UNCERTAIN",uncertainToken)!=null,"uncertain receipt missing");
  sql("INSERT INTO player VALUES(4,40,'[0,0,0,0,0,888]','[]','[]')");
  String payload="[{\"id\":-1,\"quantity\":10},{\"id\":457,\"quantity\":1},{\"id\":-2,\"quantity\":2147483647}]";
  sql("INSERT INTO giftcode(code,count_left,detail,expired) VALUES('ENDTOEND',5,'"+payload+"','2037-12-31'),('FULLBAG',5,'"+payload+"','2037-12-31')");
  Player live=player(4);live.isPlayer=true;live.inventory.itemsBag.add(new Item());live.session=blank(nro.models.network.MySession.class);live.session.userId=40;live.session.player=live;
  var service=new GiftCodeService(dao,p->true);
  check(service.claim(live," endtoend "),"end-to-end service claim refused");
  check(live.inventory.gold==12&&live.inventory.gem==200000000&&live.inventory.itemsBag.get(0).template.id==457,"committed rewards not published");
  check(scalar("SELECT JSON_EXTRACT(data_inventory,'$[0]') FROM player WHERE id=4")==12&&scalar("SELECT JSON_EXTRACT(data_inventory,'$[5]') FROM player WHERE id=4")==888,"service reward SQL lost live balance or extension");
  check(scalar("SELECT JSON_EXTRACT(JSON_UNQUOTE(JSON_EXTRACT(items_bag,'$[0]')),'$[0]') FROM player WHERE id=4")==457,"item457 not persisted");
  check(!service.claim(live,"FULLBAG")&&live.inventory.gold==12&&live.giftCode.rewards.size()==1,"failed staged item published partial money/history");
  check(scalar("SELECT count_left FROM giftcode WHERE code='FULLBAG'")==5&&scalar("SELECT COUNT(*) FROM giftcode_claim WHERE canonical_code='FULLBAG'")==0,"full bag consumed durable quota/claim");
  System.out.println("LiveSyncGiftClaimTest isolated SQL PASS");
 }
 static Connection wrap(Connection inner,String fault,boolean committed){return (Connection)Proxy.newProxyInstance(LiveSyncGiftClaimTest.class.getClassLoader(),new Class[]{Connection.class},(o,m,args)->{
  if(m.getName().equals("commit")&&fault.equals("commit")){if(committed)inner.commit();throw new SQLException("commit acknowledgement lost");}
  if(m.getName().equals("prepareStatement")&&fault.equals("update")&&((String)args[0]).startsWith("UPDATE player SET"))throw new SQLException("reward write failed");
  try{return m.invoke(inner,args);}catch(InvocationTargetException e){throw e.getCause();}
 });}
 public static void main(String[] args)throws Exception {
  try{Class.forName("nro.models.database.GiftCodeClaimDAO");}catch(ClassNotFoundException missing){throw new AssertionError("Atomic giftcode claim DAO is missing",missing);}
  unit();if(args.length>0)database(args[0]);System.out.println("LiveSyncGiftClaimTest PASS");
 }
}
