package nro.models.services;

import java.sql.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import com.google.gson.*;
import nro.models.database.RubyGiftDAO;
import nro.models.database.PlayerDAO;
import nro.models.player.*;
import nro.models.network.MySession;
import nro.models.network.Message;
import nro.models.item.Item;
import nro.models.player_system.Template;
import sun.misc.Unsafe;

public final class RubyGiftDbTest {
 static String schema;static Properties config=new Properties();static Unsafe unsafe;
 static {try{Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);unsafe=(Unsafe)f.get(null);}catch(Exception e){throw new RuntimeException(e);}}
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 static Connection open()throws SQLException {
  Connection c=DriverManager.getConnection("jdbc:mysql://"+config.getProperty("database.host")+":"+config.getProperty("database.port")+"/"+schema+"?useSSL=false&allowPublicKeyRetrieval=true",config.getProperty("database.user"),config.getProperty("database.pass"));
  if(!schema.equals(c.getCatalog()))throw new SQLException("isolated catalog required");return c;
 }
 public static class Connected extends MySession {public Connected()throws Exception{super(null);} @Override public boolean isConnected(){return true;}}
 public static class Owner extends Player {@Override public void sendMessage(Message message){}}
 static Player player(int id,int ruby,int tickets)throws Exception {
  Player p=(Player)unsafe.allocateInstance(Owner.class);p.id=id;p.name="gift"+id;p.isPlayer=true;p.idMark=new IDMark();p.inventory=new Inventory();p.inventory.ruby=ruby;p.inventory.gold=71;p.inventory.gem=72;p.inventory.coupon=73;p.inventory.event=74;
  p.session=(Connected)unsafe.allocateInstance(Connected.class);p.session.userId=id;p.session.player=p;
  Item t=new Item();t.template=new Template.ItemTemplate();t.template.id=2000;t.template.type=27;t.quantity=tickets;p.inventory.itemsBag.add(t);p.inventory.itemsBag.add(new Item());return p;
 }
 static RubyGiftService service(RubyGiftDAO dao){return new RubyGiftService(dao,p->p.getSession()!=null&&p.getSession().player==p&&p.getSession().isConnected());}
 static RubyGiftDAO dao(){return new RubyGiftDAO(RubyGiftDbTest::open);}
 static void reset()throws Exception {try(Connection c=open();Statement q=c.createStatement()){
  q.executeUpdate("DELETE FROM ruby_gift_receipt");q.executeUpdate("DELETE FROM player");
  q.executeUpdate("INSERT INTO player(id,account_id,data_inventory,items_bag) VALUES(1,1,'[9,8,1000,7,6,12345]','[]'),(2,2,'[5,4,20,3,2,67890]','[]')");
 }}
 static int count()throws Exception{try(Connection c=open();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT COUNT(*) FROM ruby_gift_receipt")){r.next();return r.getInt(1);}}
 static String stored(int id,String column)throws Exception {try(Connection c=open();PreparedStatement q=c.prepareStatement("SELECT "+column+" FROM player WHERE id=?")){q.setInt(1,id);try(ResultSet r=q.executeQuery()){r.next();return r.getString(1);}}}
 static RubyGiftDAO fault(int after,boolean commit,boolean[] outage){return new RubyGiftDAO(()->{
  if(outage[0])throw new SQLException("fixture outage");Connection c=open();int[] writes={0};
  return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{try{
   if(m.getName().equals("commit")&&commit){c.commit();outage[0]=true;throw new SQLException("lost COMMIT acknowledgement");}
   Object out=m.invoke(c,a);if(m.getName().equals("prepareStatement")){PreparedStatement q=(PreparedStatement)out;return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class[]{PreparedStatement.class},(pp,mm,aa)->{try{Object x=mm.invoke(q,aa);if(mm.getName().equals("executeUpdate")&&++writes[0]==after)throw new SQLException("injected write failure");return x;}catch(InvocationTargetException e){throw e.getCause();}});}return out;
  }catch(InvocationTargetException e){throw e.getCause();}});
 });}
 static void success()throws Exception {
  reset();Player a=player(1,1000,2),b=player(2,20,1);Item unusual=new Item();unusual.template=new Template.ItemTemplate();unusual.template.id=17;unusual.quantity=0;b.inventory.itemsBag.add(unusual);var s=service(dao());check(s.prepare(a,b,101).success(),"valid proposal");check(s.confirm(a).success(),"gift succeeds");
  check(a.inventory.ruby==899&&b.inventory.ruby==110&&a.inventory.itemsBag.get(0).quantity==1,"debit includes fee and one ticket");check(!s.confirm(a).success()&&count()==1,"one shot confirmation");
  JsonArray inv=new JsonParser().parse(stored(1,"data_inventory")).getAsJsonArray();check(inv.get(0).getAsLong()==71&&inv.get(1).getAsInt()==72&&inv.get(2).getAsInt()==899&&inv.get(3).getAsInt()==73&&inv.get(4).getAsInt()==74&&inv.get(5).getAsInt()==12345,"all inventory fields preserved including extension");
  JsonArray bag=new JsonParser().parse(stored(1,"items_bag")).getAsJsonArray();check(bag.size()==2&&new JsonParser().parse(bag.get(0).getAsString()).getAsJsonArray().get(1).getAsInt()==1,"bag slots and serialized nested shape preserved");
  JsonArray recipient=new JsonParser().parse(stored(2,"items_bag")).getAsJsonArray();check(new JsonParser().parse(recipient.get(2).getAsString()).getAsJsonArray().get(0).getAsInt()==17,"unrelated bag item retains template even with zero quantity");
 }
 static void rejection()throws Exception {
  reset();Player a=player(1,1000,1),b=player(2,20,1);var s=service(dao());
  for(int amount:new int[]{Integer.MIN_VALUE,-1,0,1,1001})check(!s.prepare(a,b,amount).success(),"reject amount "+amount);
  check(!s.prepare(a,a,10).success(),"self rejected");b.inventory.ruby=Integer.MAX_VALUE;check(!s.prepare(a,b,10).success(),"overflow rejected");b.inventory.ruby=-1;check(!s.prepare(a,b,10).success(),"negative recipient rejected");b.inventory.ruby=20;
  a.inventory.itemsBag.get(0).quantity=0;check(!s.prepare(a,b,10).success(),"ticket required");a.inventory.itemsBag.get(0).quantity=1;check(s.prepare(a,b,10).success(),"prepare valid");b.session.player=null;check(!s.confirm(a).success()&&a.inventory.ruby==1000&&count()==0,"replaced session rejects confirmation");
  b.session.player=b;check(s.prepare(a,b,10).success(),"prepare trading test");b.tradeWVP=true;check(!s.confirm(a).success()&&count()==0,"receiver trading rejects");b.tradeWVP=false;
 }
 static void rollback()throws Exception {for(int write=1;write<=3;write++){
  reset();Player a=player(1,1000,1),b=player(2,20,1);var s=service(fault(write,false,new boolean[]{false}));check(s.prepare(a,b,100).success(),"prepare rollback");check(!s.confirm(a).success(),"write rollback");check(a.inventory.ruby==1000&&b.inventory.ruby==20&&a.inventory.itemsBag.get(0).quantity==1&&count()==0,"failure leaves both players and ledger unchanged");check(new JsonParser().parse(stored(1,"data_inventory")).getAsJsonArray().get(2).getAsInt()==1000,"DB rolled back");
 }}
 static void ambiguous()throws Exception {
  reset();Player a=player(1,1000,1),b=player(2,20,1);boolean[] outage={false};var s=service(fault(0,true,outage));check(s.prepare(a,b,100).success(),"prepare uncertain");check(!s.confirm(a).success()&&RubyGiftService.hasPending(a)&&RubyGiftService.hasPending(b),"uncertain commit reserves both players");check((boolean)RubyGiftService.class.getMethod("hasPendingAccount",int.class).invoke(null,1)&&(boolean)RubyGiftService.class.getMethod("hasPendingId",long.class).invoke(null,2L),"pending ID/account blocks replacement sessions");check(a.inventory.ruby==1000&&b.inventory.ruby==20&&a.inventory.itemsBag.get(0).quantity==1,"no premature RAM changes");check(!s.reconcilePending(a),"save blocked while receipt inaccessible");PlayerDAO.updatePlayer(a);a.dispose();check(a.inventory!=null&&a.inventory.itemsBag.size()==2&&RubyGiftService.hasPending(a),"autosave and logout disposal retain unresolved inventory");outage[0]=false;check(s.reconcilePending(b)&&!RubyGiftService.hasPending(a)&&!RubyGiftService.hasPending(b),"both reconciled together");check(a.inventory.ruby==900&&b.inventory.ruby==110&&a.inventory.itemsBag.get(0).template==null&&count()==1,"exact gift recovered");check(s.reconcilePending(a)&&a.inventory.ruby==900,"recovery never replayed");
 }
 static void expiry()throws Exception {
  reset();Player a=player(1,1000,1),b=player(2,20,1);var s=service(dao());check(s.prepare(a,b,100).success(),"prepare expiry");Field f=RubyGiftService.class.getDeclaredField("proposals");f.setAccessible(true);Map proposals=(Map)f.get(s);Object pending=proposals.get(a);RecordComponent[] fields=pending.getClass().getRecordComponents();Object[] values=new Object[fields.length];Class<?>[] types=new Class<?>[fields.length];boolean expiry=false;
  for(int i=0;i<fields.length;i++){types[i]=fields[i].getType();Method accessor=fields[i].getAccessor();accessor.setAccessible(true);values[i]=accessor.invoke(pending);if(fields[i].getName().equals("createdAt")){values[i]=System.currentTimeMillis()-61000;expiry=true;}}
  check(expiry,"proposal requires expiration timestamp");Constructor<?> constructor=pending.getClass().getDeclaredConstructor(types);constructor.setAccessible(true);proposals.put(a,constructor.newInstance(values));check(!s.confirm(a).success()&&count()==0&&a.inventory.ruby==1000,"expired form cannot commit");
 }
 static void concurrency()throws Exception {
  reset();Player a=player(1,1000,2),b=player(2,20,2);var s=service(dao());check(s.prepare(a,b,10).success()&&s.prepare(b,a,10).success(),"opposite proposals");ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);Future<?> x=pool.submit(()->{try{start.await();check(s.confirm(a).success(),"forward gift");}catch(InterruptedException e){throw new RuntimeException(e);}}),y=pool.submit(()->{try{start.await();check(s.confirm(b).success(),"reverse gift");}catch(InterruptedException e){throw new RuntimeException(e);}});start.countDown();x.get(10,TimeUnit.SECONDS);y.get(10,TimeUnit.SECONDS);pool.shutdownNow();check(a.inventory.ruby==999&&b.inventory.ruby==19&&count()==2,"opposite direction has no deadlock or lost credit");
 }
 static void doubleConfirm()throws Exception {
  reset();Player a=player(1,1000,2),b=player(2,20,1);var s=service(dao());check(s.prepare(a,b,100).success(),"double confirm prepare");ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);Callable<Boolean> call=()->{start.await();return s.confirm(a).success();};Future<Boolean> x=pool.submit(call),y=pool.submit(call);start.countDown();check(x.get(10,TimeUnit.SECONDS)!=y.get(10,TimeUnit.SECONDS),"one concurrent confirmation succeeds");pool.shutdownNow();check(count()==1&&a.inventory.ruby==900&&b.inventory.ruby==110,"duplicate form packets debit once");
 }
 static void packetFailure()throws Exception {
  reset();Field sf=Service.class.getDeclaredField("instance"),inf=InventoryService.class.getDeclaredField("I");sf.setAccessible(true);inf.setAccessible(true);Object originalService=sf.get(null),originalInventory=inf.get(null);int[] attempts={0};
  sf.set(null,new Service(){@Override public void sendMoney(Player p){attempts[0]++;throw new IllegalStateException("packet fixture");}@Override public void sendThongBao(Player p,String text){attempts[0]++;throw new IllegalStateException("packet fixture");}});
  inf.set(null,new InventoryService(){@Override public void sendItemBags(Player p){attempts[0]++;throw new IllegalStateException("packet fixture");}});
  try{Player a=player(1,1000,1),b=player(2,20,1);var s=service(dao());check(s.prepare(a,b,100).success()&&s.confirm(a).success(),"packet errors do not fail durable gift");check(attempts[0]==4&&!s.confirm(a).success()&&s.reconcilePending(a)&&count()==1&&a.inventory.ruby==900,"all refreshes attempted separately and never replay credit");}finally{sf.set(null,originalService);inf.set(null,originalInventory);}
 }
 static void savingAndDisposalWait()throws Exception {
  reset();Player a=player(1,1000,1),b=player(2,20,1);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),hooks=new CountDownLatch(2);RubyGiftDAO blocked=new RubyGiftDAO(RubyGiftDbTest::open){@Override public Receipt transfer(String token,Snapshot sender,Snapshot target,int amount)throws SQLException{entered.countDown();try{if(!release.await(10,TimeUnit.SECONDS))throw new SQLException("test release timeout");}catch(InterruptedException e){throw new SQLException(e);}return super.transfer(token,sender,target,amount);}};var s=service(blocked);check(s.prepare(a,b,100).success(),"prepare save lock test");ExecutorService pool=Executors.newFixedThreadPool(3);Future<Boolean> gift=pool.submit(()->s.confirm(a).success());check(entered.await(5,TimeUnit.SECONDS),"gift holds paired locks");Future<?> save=pool.submit(()->{hooks.countDown();PlayerDAO.updatePlayer(a);}),dispose=pool.submit(()->{hooks.countDown();b.dispose();});check(hooks.await(5,TimeUnit.SECONDS),"save/disposal started");
  try{save.get(100,TimeUnit.MILLISECONDS);throw new AssertionError("save must wait for paired gift lock");}catch(TimeoutException expected){}try{dispose.get(100,TimeUnit.MILLISECONDS);throw new AssertionError("disposal must wait for paired gift lock");}catch(TimeoutException expected){}release.countDown();check(gift.get(5,TimeUnit.SECONDS),"gift commits before waiting hooks");save.get(5,TimeUnit.SECONDS);dispose.get(5,TimeUnit.SECONDS);pool.shutdownNow();check(a.inventory.ruby==900&&b.inventory==null&&count()==1,"save/disposal resume only after gift resolves");
 }
 static void absentUncertainCommit()throws Exception {
  reset();Player a=player(1,1200,3),b=player(2,35,2);boolean[] outage={false};RubyGiftDAO absent=new RubyGiftDAO(()->{if(outage[0])throw new SQLException("outage");Connection c=open();return(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,args)->{try{if(m.getName().equals("commit")){c.rollback();outage[0]=true;throw new SQLException("COMMIT acknowledgement lost before apply");}return m.invoke(c,args);}catch(InvocationTargetException e){throw e.getCause();}});});var s=service(absent);check(s.prepare(a,b,100).success()&&!s.confirm(a).success()&&RubyGiftService.hasPending(a),"absent unknown commit remains gated");outage[0]=false;
  // A recovered connection can now confirm absence and durably preserve pre-gift RAM.
  var recovery=service(dao());Field active=RubyGiftService.class.getDeclaredField("ACTIVE");active.setAccessible(true);Map activeMap=(Map)active.get(null);Object operation=activeMap.get(a);RecordComponent[] fields=operation.getClass().getRecordComponents();Object[] values=new Object[fields.length];Class<?>[] types=new Class<?>[fields.length];for(int i=0;i<fields.length;i++){types[i]=fields[i].getType();Method getter=fields[i].getAccessor();getter.setAccessible(true);values[i]=getter.invoke(operation);if(fields[i].getName().equals("owner"))values[i]=recovery;}Constructor<?> constructor=operation.getClass().getDeclaredConstructor(types);constructor.setAccessible(true);Object recovered=constructor.newInstance(values);activeMap.put(a,recovered);activeMap.put(b,recovered);
  check(recovery.reconcilePending(a)&&!RubyGiftService.hasPendingAccount(1)&&count()==0,"absence restored before reopening account");check(new JsonParser().parse(stored(1,"data_inventory")).getAsJsonArray().get(2).getAsInt()==1200&&new JsonParser().parse(stored(2,"data_inventory")).getAsJsonArray().get(2).getAsInt()==35,"pre-gift unsaved ruby survives rollback and reconnect");check(new JsonParser().parse(new JsonParser().parse(stored(1,"items_bag")).getAsJsonArray().get(0).getAsString()).getAsJsonArray().get(1).getAsInt()==3,"ticket retained durably after absent commit");
 }
 static void deferredFullLogout()throws Exception {
  reset();Player a=player(1,1000,1),b=player(2,20,1);Item box=new Item();box.template=new Template.ItemTemplate();box.template.id=77;box.quantity=8;a.inventory.itemsBox.add(box);a.point_sukien=987;boolean[] outage={false},saveFails={true};int[] saves={0};
  java.util.function.Predicate<Player> saver=p->{saves[0]++;check(RubyGiftService.hasPendingAccount(p.session.userId),"account remains gated during full deferred save");check(!PlayerDAO.savePendingRubyLogout(p),"unloaded full-player data cannot report durable save success");if(saveFails[0])return false;try(Connection c=open();PreparedStatement q=c.prepareStatement("UPDATE player SET items_box=?,data_task=? WHERE id=?")){q.setString(1,"[77,"+p.inventory.itemsBox.get(0).quantity+"]");q.setString(2,Integer.toString(p.point_sukien));q.setLong(3,p.id);return q.executeUpdate()==1;}catch(SQLException e){return false;}};
  Constructor<RubyGiftService> constructor=RubyGiftService.class.getConstructor(RubyGiftDAO.class,java.util.function.Predicate.class,java.util.function.Predicate.class);var s=constructor.newInstance(fault(0,true,outage),(java.util.function.Predicate<Player>)(p->p.session!=null&&p.session.player==p),saver);
  check(s.prepare(a,b,100).success()&&!s.confirm(a).success(),"pending transfer before logout");a.beforeDispose=true;outage[0]=false;check(!s.reconcilePending(a)&&RubyGiftService.hasPendingAccount(1)&&a.inventory!=null,"fullsave failure prevents account reuse and disposal");check(a.inventory.ruby==900&&b.inventory.ruby==110&&count()==1,"receipt applied once while deferredsave retries");saveFails[0]=false;check(s.reconcilePending(a)&&!RubyGiftService.hasPendingAccount(1)&&saves[0]==2,"fullsave confirmed before account unblocked");check(stored(1,"items_box").equals("[77,8]")&&stored(1,"data_task").equals("987"),"unsaved box/progress survives deferredlogout");check(a.inventory.ruby==900&&s.reconcilePending(a)&&saves[0]==2,"deferredsave never replays ruby debit");
 }
 static void movedContainersPersistTogether()throws Exception {
  reset();Player a=player(1,1000,1),b=player(2,20,1);String ticketBox="[\"[2000,1,\\\"[]\\\",123]\"]",body="[\"[77,1,\\\"[]\\\",456]\"]";
  try(Connection c=open();PreparedStatement q=c.prepareStatement("UPDATE player SET items_box=?,items_body=? WHERE id=?")){q.setString(1,ticketBox);q.setString(2,"[]");q.setInt(3,1);q.executeUpdate();q.setString(1,"[]");q.setString(2,body);q.setInt(3,2);q.executeUpdate();}
  // Both moves happened in RAM since the last save: ticket box -> bag, body -> bag.
  Item moved=new Item();moved.template=new Template.ItemTemplate();moved.template.id=77;moved.quantity=1;b.inventory.itemsBag.add(moved);var s=service(dao());check(s.prepare(a,b,100).success()&&s.confirm(a).success(),"gift after unsaved container moves");
  check(new JsonParser().parse(stored(1,"items_box")).getAsJsonArray().size()==0,"consumed ticket cannot resurrect from stale persisted box");check(new JsonParser().parse(new JsonParser().parse(stored(1,"items_bag")).getAsJsonArray().get(0).getAsString()).getAsJsonArray().get(0).getAsInt()==-1,"last gift ticket consumed durably");check(new JsonParser().parse(stored(2,"items_body")).getAsJsonArray().size()==0,"recipient body source removed durably with bag snapshot");check(new JsonParser().parse(new JsonParser().parse(stored(2,"items_bag")).getAsJsonArray().get(2).getAsString()).getAsJsonArray().get(0).getAsInt()==77,"moved recipient item retained once");
  for(String column:List.of("items_box_lucky_round","items_daban"))check(new JsonParser().parse(stored(1,column)).getAsJsonArray().size()==0&&new JsonParser().parse(stored(2,column)).getAsJsonArray().size()==0,"all other inventory containers saved atomically");
 }
 public static void main(String[] args)throws Exception {
  schema=args[0];if(!schema.matches("ruby_verify_[0-9_]+"))throw new IllegalArgumentException("isolated schema only");try(var in=Files.newInputStream(Path.of("Config.properties"))){config.load(in);}
  Class.forName(config.getProperty("database.driver"));
  try(Connection c=DriverManager.getConnection("jdbc:mysql://"+config.getProperty("database.host")+":"+config.getProperty("database.port")+"/",config.getProperty("database.user"),config.getProperty("database.pass"));Statement q=c.createStatement()){q.execute("CREATE DATABASE IF NOT EXISTS `"+schema+"`");}
  try(Connection c=open();Statement q=c.createStatement()){q.execute("CREATE TABLE IF NOT EXISTS player(id BIGINT PRIMARY KEY,account_id INT NOT NULL,data_inventory LONGTEXT NOT NULL,items_bag LONGTEXT NOT NULL) ENGINE=InnoDB");q.execute(RubyGiftDAO.CREATE_TABLE_SQL);}
  try(Connection c=open();Statement q=c.createStatement()){for(String column:List.of("items_box","data_task","items_body","items_box_lucky_round","items_daban")){try(ResultSet columns=c.getMetaData().getColumns(schema,null,"player",column)){if(!columns.next())q.execute("ALTER TABLE player ADD COLUMN "+column+" LONGTEXT NULL");}}}
  success();rejection();rollback();ambiguous();concurrency();doubleConfirm();packetFailure();savingAndDisposalWait();absentUncertainCommit();deferredFullLogout();movedContainersPersistTogether();expiry();System.out.println("RubyGiftDbTest PASS: fee/debit/ticket, all inventory containers atomic, validation, rollback, concurrency/double confirm, full deferred logout gating, committed/absent reconciliation, packet failure");
 }
}
