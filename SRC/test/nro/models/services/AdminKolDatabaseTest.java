package nro.models.services;
import java.sql.*;
import nro.models.database.KolRewardDAO;
import nro.models.data.LocalManager;
import nro.models.player.*;
import static nro.models.services.PinkFixture.*;
public class AdminKolDatabaseTest {
 static String schema;
 static Connection open()throws SQLException {Connection c=LocalManager.getConnection();c.setCatalog(schema);if(!schema.equals(c.getCatalog()))throw new SQLException("fixture catalog mismatch");return c;}
 static Connection faultConnection(boolean afterCommit)throws SQLException {
  Connection real=open();
  return (Connection)java.lang.reflect.Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,args)->{
   try {
    Object result=method.invoke(real,args);
    if(afterCommit&&method.getName().equals("commit"))throw new SQLException("fixture fault after actual commit");
    if(!afterCommit&&method.getName().equals("prepareStatement")){
     PreparedStatement statement=(PreparedStatement)result;
     return java.lang.reflect.Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class<?>[]{PreparedStatement.class},(p,m,a)->{try{Object r=m.invoke(statement,a);if(m.getName().equals("executeUpdate"))throw new SQLException("fixture fault after actual update before commit");return r;}catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}});
    }
    return result;
   }catch(java.lang.reflect.InvocationTargetException e){throw e.getCause();}
  });
 }
 public static void main(String[] args)throws Exception {
  schema=args.length==0?"hs_kol_verify_20261004_"+System.currentTimeMillis():args[0];if(!schema.matches("hs_kol_verify_20261004_[0-9]+"))throw new IllegalArgumentException("guarded fixture schema required");
  try(Connection c=LocalManager.getConnection();Statement q=c.createStatement()){q.execute("CREATE DATABASE `"+schema+"` CHARACTER SET utf8mb4");}
  try(Connection c=open();Statement q=c.createStatement()){
   q.execute("CREATE TABLE player(id BIGINT PRIMARY KEY,account_id INT NOT NULL,event_point INT NOT NULL,nhiem_vu_kol TEXT NOT NULL) ENGINE=InnoDB");
   q.execute("CREATE TABLE account(id INT PRIMARY KEY,vnd INT NOT NULL) ENGINE=InnoDB");q.execute("INSERT INTO account VALUES(77,123)");
  }
  AdminRecoveryKolTest.setup();var dao=new KolRewardDAO(AdminKolDatabaseTest::open);var service=new KolQuestService(dao);
  for(int stage=1;stage<=7;stage++){
   Player p=AdminRecoveryKolTest.admin();p.id=700+stage;p.kolQuestState.normalStage=stage;p.kolQuestState.vipStage=8;p.kolQuestStage=5;p.monsterKillCountAutoTrain=987;
   try(Connection c=open();PreparedStatement q=c.prepareStatement("INSERT INTO player VALUES(?,77,456,?)")){q.setLong(1,p.id);q.setString(2,new com.google.gson.Gson().toJson(KOLProgressData.snapshot(p,p.kolQuestState)));q.executeUpdate();}
   check(service.completeCurrentForAdmin(p).success(),"SQL stage save failed "+stage);
   check(service.completeCurrentForAdmin(p).success(),"SQL repeat idempotence failed "+stage);
   var loaded=KOLProgressData.parse(dao.readProgress(p.id,77));check(loaded.v2.normalStage==stage&&loaded.v2.vipStage==8&&loaded.kolQuestStage==5&&loaded.monsterKillCountAutoTrain==987,"SQL legacy/relogin changed");
   check(nro.models.task.KolQuestCatalog.progress(loaded.v2,stage)==nro.models.task.KolQuestCatalog.required(stage),"SQL stage progress mismatch");
   try{dao.saveProgress(p.id,78,"{}");throw new AssertionError("ownership mismatch accepted");}catch(SQLException expected){}
   check(dao.readProgress(p.id,77).equals(new com.google.gson.Gson().toJson(KOLProgressData.snapshot(p,p.kolQuestState))),"ownership failure overwrote JSON");
  }
  try{dao.saveProgress(9999,77,"{}");throw new AssertionError("zero row save accepted");}catch(SQLException expected){}
  try(Connection c=open();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT (SELECT vnd FROM account WHERE id=77),SUM(event_point),COUNT(*) FROM player")){r.next();check(r.getInt(1)==123&&r.getInt(2)==3192&&r.getInt(3)==7,"SQL balances changed");}
  Player pending=AdminRecoveryKolTest.admin();pending.id=800;
  try(Connection c=open();PreparedStatement q=c.prepareStatement("INSERT INTO player VALUES(?,77,0,?)")){q.setLong(1,pending.id);q.setString(2,new com.google.gson.Gson().toJson(KOLProgressData.snapshot(pending,pending.kolQuestState)));q.executeUpdate();}
  String original=dao.readProgress(800,77);
  KolRewardDAO rollbackDao=new KolRewardDAO(()->faultConnection(false));
  try{rollbackDao.saveProgress(800,77,"{}");throw new AssertionError("SQL update failure accepted");}catch(SQLException expected){}
  check(dao.readProgress(800,77).equals(original),"actual update-before-failure did not roll back");
  KolQuestService uncertainService=new KolQuestService(new KolRewardDAO(()->faultConnection(true)));
  check(!uncertainService.completeCurrentForAdmin(pending).success()&&pending.kolProgressPending&&pending.kolQuestState.woodenDummyKills==0,"actual committed-but-unknown result not guarded");
  pending.kolQuestState.pinkCapsulesPicked=99;
  check(uncertainService.reconcileProgressPending(pending)&&!pending.kolProgressPending&&pending.kolQuestState.woodenDummyKills==10000&&pending.kolQuestState.pinkCapsulesPicked==99,"actual uncertain commit reconciliation dropped progress");
  System.out.println("PASS AdminKolDatabaseTest seven stages/repeat/relogin/ownership/zero row/balances; isolated schema "+schema);System.exit(0);
 }
}
