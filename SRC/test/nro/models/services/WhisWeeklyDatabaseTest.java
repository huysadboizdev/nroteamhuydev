package nro.models.services;
import java.lang.reflect.*;
import java.sql.*;
import java.time.*;
import java.util.concurrent.atomic.AtomicLong;
import nro.models.player.*;
import nro.models.server.Manager;
import nro.models.services.PinkFixture;

public final class WhisWeeklyDatabaseTest {
 public static class QuietWhis extends nro.models.boss.luyen_tap_tu_dong.Whis {
  public QuietWhis()throws Exception{super(null);}
  @Override public boolean chatE(){return true;}
 }
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 static final AtomicLong now=new AtomicLong(Instant.parse("2026-10-08T03:00:00Z").toEpochMilli());
 static final Clock clock=new Clock(){public ZoneId getZone(){return ZoneId.of("UTC");}public Clock withZone(ZoneId z){return this;}public Instant instant(){return Instant.ofEpochMilli(now.get());}};
 static String url;
 static Object service()throws Exception{
  Class<?> type=Class.forName("nro.models.services.WhisWeeklyService"),factory=Class.forName("nro.models.services.WhisWeeklyService$Connections");
  Object f=Proxy.newProxyInstance(factory.getClassLoader(),new Class[]{factory},(p,m,a)->DriverManager.getConnection(url,"root",""));
  return type.getConstructor(factory,Clock.class).newInstance(f,clock);
 }
 static Object call(Object s,String method,Class<?>[]types,Object...args)throws Exception{
  try{return s.getClass().getMethod(method,types).invoke(s,args);}catch(InvocationTargetException e){throw (Exception)e.getCause();}
 }
 static void refresh(Object s)throws Exception{call(s,"refresh",new Class[]{});}
 static Player player(long id){Player p=new Player();p.id=id;p.isPlayer=true;p.nPoint.hpg=10000;p.nPoint.mpg=10000;p.nPoint.dameg=1000;return p;}
 static void win(Object s,Player p,int level)throws Exception{call(s,"recordWin",new Class[]{Player.class,int.class,int.class},p,level,1000);}
 static int bonus(Object s,long id)throws Exception{return (int)call(s,"bonusPercent",new Class[]{long.class},id);}
 static long count(String sql)throws Exception{try(Connection c=DriverManager.getConnection(url,"root","");Statement st=c.createStatement();ResultSet r=st.executeQuery(sql)){r.next();return r.getLong(1);}}
 public static void main(String[] args)throws Exception{
  Class.forName("com.mysql.jdbc.Driver");
  url=args[0];check(url.matches("jdbc:mysql://localhost:3306/whis_weekly_test_[0-9]+.*"),"Only isolated test schema allowed");
  Object s;try{s=service();}catch(ClassNotFoundException e){throw new AssertionError("Whis weekly transactional service is missing",e);}
  refresh(s);Player a=player(1),b=player(2),c=player(3),d=player(4);
  win(s,a,2);now.addAndGet(100);win(s,b,1);now.addAndGet(100);win(s,c,1);now.addAndGet(100);win(s,d,1);win(s,a,1);
  check(a.traning.getTop()==2,"Training level never regresses");check(bonus(s,1)==0,"No bonus before first weekly close");
  check(count("SELECT wins FROM whis_weekly_score WHERE player_id=1")==2,"Atomic weekly wins");
  now.set(Instant.parse("2026-10-11T17:00:00Z").toEpochMilli());refresh(s);
  check(bonus(s,1)==5&&bonus(s,2)==4&&bonus(s,3)==3&&bonus(s,4)==0,"Last week winners and tie order");
  check(((java.util.List<?>)call(s,"top",new Class[]{})).isEmpty(),"New week top begins empty");
  Object restarted=service();refresh(restarted);check(bonus(restarted,1)==5,"Restart retains reward");refresh(restarted);check(count("SELECT COUNT(*) FROM whis_weekly_reward")==3,"Rollover idempotent");
  Field instance=s.getClass().getDeclaredField("instance");instance.setAccessible(true);instance.set(null,restarted);
  for(int rank=1;rank<=3;rank++){Player p=player(rank);for(int n=0;n<4;n++){p.nPoint.calPoint();check(p.nPoint.hpMax==10000*(106-rank)/100&&p.nPoint.mpMax==10000*(106-rank)/100&&p.nPoint.dame==1000*(106-rank)/100,"Exact bonus without compounding");}}
  Player pet=new Pet(player(1));pet.id=1;pet.nPoint.hpg=10000;pet.nPoint.mpg=10000;pet.nPoint.dameg=1000;
  pet.nPoint.calPoint();check(pet.nPoint.hpMax==10000&&pet.nPoint.dame==1000,"Pet receives no ranking bonus");
  PinkFixture.replace(Service.class,"instance",new Service(){@Override public void sendPlayerVS(Player p,Player q,byte type){}@Override public void sendThongBao(Player p,String text){}});
  QuietWhis whis=PinkFixture.blank(QuietWhis.class);whis.playerAtt=a;whis.nPoint=new NPoint(whis);whis.nPoint.hp=0;
  Field bossLevel=nro.models.boss.luyen_tap_tu_dong.Whis.class.getDeclaredField("level");bossLevel.setAccessible(true);bossLevel.setLong(whis,3);
  whis.die(a);whis.die(a);
  check(count("SELECT wins FROM whis_weekly_score WHERE week_start='2026-10-12' AND player_id=1")==1&&a.traning.getTop()==3,"Actual Whis death awards exactly one win and keeps level");
  try(Connection cn=DriverManager.getConnection(url,"root","");Statement st=cn.createStatement()){st.execute("CREATE TRIGGER fail_whis BEFORE UPDATE ON player FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='test rollback'");}
  boolean failed=false;try{win(restarted,b,2);}catch(Exception expected){failed=true;}
  check(failed&&count("SELECT COUNT(*) FROM whis_weekly_score WHERE week_start='2026-10-12' AND player_id=2")==0,"Training failure rolls back score");
  now.set(Instant.parse("2026-11-01T17:00:00Z").toEpochMilli());check(bonus(restarted,1)==0,"Expired snapshot does not grant old reward during SQL outage");refresh(restarted);check(bonus(restarted,1)==0,"Skipping multiple weeks grants no stale reward");
  a.nPoint.calPoint();check(a.nPoint.hpMax==10000,"Expired bonus removed on recalculation");
  System.out.println("WHIS_WEEKLY_DATABASE_STATS PASS");
 }
}
