package nro.models.task;
import java.sql.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import com.google.gson.Gson;
import nro.models.data.LocalManager;
import nro.models.database.*;
import nro.models.player.*;
import nro.models.services.KolQuestService;
public final class KolQuestRewardDbTest {
 static String schema;
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static Connection open()throws SQLException{Connection c=LocalManager.getConnection();c.setCatalog(schema);if(!schema.equals(c.getCatalog())){c.close();throw new SQLException("isolated catalog required");}return c;}
 static String json(int normal,int vip){KOLProgressData d=new KOLProgressData();d.v2.normalStage=normal;d.v2.vipStage=vip;return new Gson().toJson(d);}
 static void reset()throws Exception {try(Connection c=open();Statement s=c.createStatement()){
  for(String table:List.of("wallet_debit_receipt","kol_reward_claim","player","account"))s.executeUpdate("DELETE FROM `"+schema+"`."+table);
  s.executeUpdate("INSERT INTO `"+schema+"`.account(id,vnd,tongnap,active) VALUES(1,1000,700,1)");
  try(PreparedStatement q=c.prepareStatement("INSERT INTO `"+schema+"`.player(id,account_id,event_point,nhiem_vu_kol) VALUES(1,1,10,?)")){q.setString(1,json(1,1));q.executeUpdate();}
 }}
 static long read(String query)throws Exception{try(Connection c=open();Statement s=c.createStatement();ResultSet r=s.executeQuery(query)){r.next();return r.getLong(1);}}
 static KolRewardDAO dao(){return new KolRewardDAO(KolQuestRewardDbTest::open);}
 static KolRewardDAO fault(int afterUpdate,boolean ambiguous){return new KolRewardDAO(()->{
  Connection real=open();final int[] updates={0};
  return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(proxy,m,args)->{
   try{
    if(m.getName().equals("commit")&&ambiguous){real.commit();throw new SQLException("ambiguous commit fixture");}
    Object result=m.invoke(real,args);
    if(m.getName().equals("prepareStatement")){
     PreparedStatement ps=(PreparedStatement)result;
     return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class[]{PreparedStatement.class},(p,pm,pa)->{try{Object out=pm.invoke(ps,pa);if(pm.getName().equals("executeUpdate")&&++updates[0]==afterUpdate)throw new SQLException("failure after update "+afterUpdate);return out;}catch(InvocationTargetException e){throw e.getCause();}});
    }
    return result;
   }catch(InvocationTargetException e){throw e.getCause();}
  });
 });}
 static void rollback()throws Exception{
  for(int failure=1;failure<=3;failure++){reset();try{fault(failure,false).claim(1,1,false,1,100000,1000,15,json(2,1));throw new AssertionError("injected failure expected");}catch(SQLException expected){}
   check(read("SELECT vnd FROM account WHERE id=1")==1000&&read("SELECT event_point FROM player WHERE id=1")==10&&read("SELECT count(*) FROM kol_reward_claim")==0,"rollback all after update "+failure);
  }
 }
 static void concurrency()throws Exception{
  reset();ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
  Callable<KolRewardDAO.Balance> c=()->{start.await();return dao().claim(1,1,false,1,100000,1000,15,json(2,1));};
  Future<KolRewardDAO.Balance> a=pool.submit(c),b=pool.submit(c);start.countDown();var x=a.get();var y=b.get();pool.shutdown();check(x.newlyClaimed()!=y.newlyClaimed(),"exactly one concurrent claim");check(read("SELECT vnd FROM account WHERE id=1")==101000&&read("SELECT event_point FROM player WHERE id=1")==1015&&read("SELECT count(*) FROM kol_reward_claim")==1,"one ledger credit and live unsaved five points");
 }
 static void overflowAndOwnership()throws Exception{
  reset();try{dao().claim(1,2,false,1,100000,1000,10,json(2,1));throw new AssertionError("ownership");}catch(SQLException expected){}check(read("SELECT count(*) FROM kol_reward_claim")==0,"ownership rejects ledger");
  try(Connection c=open();Statement s=c.createStatement()){s.executeUpdate("UPDATE account SET vnd=2147483640 WHERE id=1");}
  try{dao().claim(1,1,false,1,100000,1000,10,json(2,1));throw new AssertionError("overflow");}catch(SQLException expected){}
  check(read("SELECT count(*) FROM kol_reward_claim")==0&&read("SELECT vnd FROM account WHERE id=1")==2147483640L,"overflow no lost stage");
 }
 static void ambiguous()throws Exception{
  reset();var result=fault(0,true).claim(1,1,false,1,100000,1000,15,json(2,1));check(result.vnd()==101000&&result.points()==1015,"ambiguous commit recovered by ledger");var again=dao().claim(1,1,false,1,100000,1000,15,json(2,1));check(!again.newlyClaimed()&&read("SELECT count(*) FROM kol_reward_claim")==1,"restart duplicate not recredited");
 }
 static void allRewards()throws Exception{
  reset();Player p=KolQuestBehaviorTest.player();p.session.vnd=1000;p.event.setEventPoint(10);
  KolQuestState state=p.kolQuestState;state.woodenDummyKills=10000;state.birdDemonKills=100000;state.fide1Kills=state.fide2Kills=state.fide3Kills=10;state.perfectCellKills=3;state.pinkCapsulesPicked=200;state.blackGokuKills=state.superBlackGokuKills=10;
  nro.models.item.Item t=new nro.models.item.Item();t.template=new nro.models.player_system.Template.ItemTemplate();t.template.id=1825;t.quantity=1;p.inventory.itemsBag.add(t);var service=new KolQuestService(dao());
  for(int quest=1;quest<=7;quest++){final int expected=quest;ExecutorService pool=Executors.newFixedThreadPool(2);Future<?> a=pool.submit(()->check(service.claim(p,false,expected).success(),"normal "+expected));Future<?> b=pool.submit(()->check(service.claim(p,true,expected).success(),"VIP "+expected));a.get();b.get();pool.shutdown();}
  check(p.session.vnd==5951000&&p.event.getEventPoint()==195010&&p.kolQuestState.normalStage==8&&p.kolQuestState.vipStage==8&&t.quantity==1,"fourteen rewards exact and ticket retained");check(read("SELECT tongnap FROM account WHERE id=1")==700&&read("SELECT count(*) FROM kol_reward_claim")==14,"no recharge credit");check(!service.claim(p,false,7).success(),"double click cannot receive next stage");
  int wallet=dao().debit(1,50000);check(wallet==5901000&&read("SELECT vnd FROM account WHERE id=1")==wallet,"VIP debit persists across reconnect");check(dao().debit(1,Integer.MAX_VALUE)==-1,"conditional debit insufficient funds");
  check(dao().activate(1,10000,true)==5891000 && read("SELECT active FROM account WHERE id=1")==1,"membership uses same atomic wallet debit");
  try(Connection c=open();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT nhiem_vu_kol,event_point FROM player WHERE id=1")){r.next();check(KOLProgressData.parse(r.getString(1)).v2.vipStage==8&&r.getInt(2)==195010,"restart durable stage and points");}
 }

 static void unresolvedCommit()throws Exception {
  reset();final boolean[] outage={false};
  KolRewardDAO failing=new KolRewardDAO(()->{
   if(outage[0])throw new SQLException("reconciliation outage");
   Connection real=open();return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(proxy,m,args)->{
    try {if(m.getName().equals("commit")){real.commit();outage[0]=true;throw new SQLException("commit acknowledgement lost");}return m.invoke(real,args);}catch(InvocationTargetException e){throw e.getCause();}
   });
  });
  Player p=KolQuestBehaviorTest.player();p.session.vnd=1000;p.event.setEventPoint(15);p.kolQuestState.woodenDummyKills=10000;
  var service=new KolQuestService(failing);
  check(!service.claim(p,false,1).success()&&p.kolRewardPending&&p.kolQuestState.normalStage==1&&p.event.getEventPoint()==15,"uncertain outcome keeps RAM unchanged and blocks saves");
  check(!service.reconcilePending(p),"autosave shared helper refuses while outcome unknown");
  p.event.addEventPoint(5);p.kolQuestState.pinkCapsulesPicked=20;
  check(!service.claim(p,false,1).success()&&p.kolRewardPending,"outage prevents a second credit");
  var recovery=new KolQuestService(dao());check(!recovery.claim(p,false,1).success()&&!p.kolRewardPending,"reconciliation consumes pending context without claiming next quest");
  check(p.event.getEventPoint()==1020&&p.kolQuestState.normalStage==2&&p.kolQuestState.pinkCapsulesPicked==20&&read("SELECT count(*) FROM kol_reward_claim")==1,"recovery retains intervening progress and points");
 }

 static KolRewardDAO walletFault(boolean applied,boolean outageAfterCommit,boolean[] outage) {
  return new KolRewardDAO(()-> {
   if(outage[0])throw new SQLException("wallet receipt lookup outage");
   Connection real=open();return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(proxy,m,args)-> {
    try {
     if(m.getName().equals("commit")) {
      if(applied)real.commit();else real.rollback();
      if(outageAfterCommit)outage[0]=true;
      throw new SQLException("wallet COMMIT acknowledgement lost");
     }
     return m.invoke(real,args);
    } catch(InvocationTargetException e) { throw e.getCause(); }
   });
  });
 }
 static void walletReceipts()throws Exception {
  walletAmbiguousRed();
  reset();final int[] gifts={0,0};boolean[] outage={false};
  var applied=new nro.models.services.WalletPurchaseService(walletFault(true,false,outage));
  Player p=KolQuestBehaviorTest.player();p.session.vnd=1000;
  check(applied.purchase(p,"vip_1",100,()->gifts[0]++)==nro.models.services.WalletPurchaseService.Result.SUCCESS,"applied commit resolves successful purchase");
  check(gifts[0]==1&&p.session.vnd==900&&read("SELECT vnd FROM account WHERE id=1")==900&&read("SELECT count(*) FROM wallet_debit_receipt")==1,"one debit and original grant");
  reset();gifts[0]=0;
  var absent=new nro.models.services.WalletPurchaseService(walletFault(false,false,outage));p=KolQuestBehaviorTest.player();p.session.vnd=1000;
  check(absent.purchase(p,"vip_1",100,()->gifts[0]++)==nro.models.services.WalletPurchaseService.Result.FAILED,"non applied commit confirmed absent");check(gifts[0]==0&&p.walletPurchasePending==null&&read("SELECT vnd FROM account WHERE id=1")==1000&&read("SELECT count(*) FROM wallet_debit_receipt")==0,"no grant no charge absent commit");
  reset();gifts[0]=gifts[1]=0;outage[0]=false;
  var pending=new nro.models.services.WalletPurchaseService(walletFault(true,true,outage));p=KolQuestBehaviorTest.player();p.session.vnd=1000;
  check(pending.purchase(p,"vip_1",100,()->gifts[0]++)==nro.models.services.WalletPurchaseService.Result.PENDING&&p.walletPurchasePending!=null,"uncertain debit retains original grant");
  String receipt=p.walletPurchasePending.receiptId();check(receipt.length()==36&&p.session.vnd==1000&&gifts[0]==0,"UUID context and no premature wallet/grant mutation");
  check(pending.purchase(p,"gem_exchange",200,()->gifts[1]++)==nro.models.services.WalletPurchaseService.Result.PENDING&&gifts[1]==0,"outage blocks another kind of purchase");
  check(!pending.reconcilePending(p),"autosave shared helper waits for receipt");
  outage[0]=false;
  check(pending.purchase(p,"gem_exchange",200,()->gifts[1]++)==nro.models.services.WalletPurchaseService.Result.RECOVERED,"retry consumed after recovering original purchase");
  check(gifts[0]==1&&gifts[1]==0&&p.walletPurchasePending==null&&p.session.vnd==900&&read("SELECT count(*) FROM wallet_debit_receipt")==1,"recovery grants only original and does not debit again");
  check(pending.reconcilePending(p)&&gifts[0]==1,"original callback never replayed");
  try {dao().reconcileDebit(1,200,"vip_1",receipt);throw new AssertionError("receipt cost mismatch");}catch(SQLException expected){}
  try {dao().reconcileDebit(1,100,"vip_2",receipt);throw new AssertionError("receipt kind mismatch");}catch(SQLException expected){}
  try(Connection c=open();Statement q=c.createStatement()){q.executeUpdate("INSERT INTO account(id,vnd,tongnap,active) VALUES(2,1000,0,0)");}
  try {dao().reconcileDebit(2,100,"vip_1",receipt);throw new AssertionError("receipt account mismatch");}catch(SQLException expected){}
  try {dao().debit(1,100,"x".repeat(33),java.util.UUID.randomUUID().toString(),null);throw new AssertionError("kind bounds");}catch(SQLException expected){}
  try {dao().debit(1,100,"vip_1","not-a-uuid",null);throw new AssertionError("UUID bounds");}catch(SQLException expected){}
  reset();gifts[0]=0;p=KolQuestBehaviorTest.player();p.session.vnd=1000;var normal=new nro.models.services.WalletPurchaseService(dao());
  check(normal.purchase(p,"gold_exchange",100,()->{gifts[0]++;throw new RuntimeException("post grant packet fixture");})==nro.models.services.WalletPurchaseService.Result.GRANT_FAILED,"packet exception after grant reported separately");
  check(p.walletPurchasePending==null&&normal.reconcilePending(p)&&gifts[0]==1&&read("SELECT count(*) FROM wallet_debit_receipt")==1,"callback exception cannot replay grant");
 }
 static void walletAmbiguousRed()throws Exception {
  reset();try { check(fault(0,true).debit(1,100)==900,"successful debit outcome reconciled"); }
  catch(SQLException wrongOutcome){throw new AssertionError("successful wallet COMMIT must reconcile and deliver original purchase",wrongOutcome);}
 }
 public static void main(String[] args)throws Exception{
  schema=args[0];if(!schema.matches("kol_verify_20261004_[0-9]+"))throw new IllegalArgumentException("isolated schema only");
  if(args.length>1&&args[1].equals("wallet-red")){try{walletAmbiguousRed();}finally{LocalManager.close();}return;}
  try{rollback();concurrency();overflowAndOwnership();ambiguous();unresolvedCommit();allRewards();walletReceipts();System.out.println("PASS KOL isolated DB rollback concurrency ownership overflow ambiguity 14rewards debit restart walletReceiptAppliedAbsentOutageGrantOnce");}finally{LocalManager.close();}
 }
}
