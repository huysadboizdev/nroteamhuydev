package nro.models.services;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import nro.models.server.Client;
import org.json.simple.*;

/** Real InnoDB fixtures. The database name is fixed; production schema is never selected. */
public final class LiveSyncQueueTest {
    private static final String DATABASE="codex_web_queue_test_20261006";
    private static final String HOST="jdbc:mysql://127.0.0.1:3306/";
    private static final String OPTIONS="?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8";
    private static Connection open()throws SQLException {Connection c=DriverManager.getConnection(HOST+DATABASE+OPTIONS,"root","");if(!DATABASE.equals(c.getCatalog()))throw new SQLException("Fixture database guard failed");return c;}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static Object invoke(Object target,Method method,Object[] args)throws Throwable {try{return method.invoke(target,args);}catch(InvocationTargetException e){throw e.getCause();}}
    private static void installClient()throws Exception {
        Client client=PinkFixture.blank(Client.class);
        for(String name:List.of("players_id","players_userId","players_name")){Field f=Client.class.getDeclaredField(name);PinkFixture.U.putObject(client,PinkFixture.U.objectFieldOffset(f),new ConcurrentHashMap<>());}
        Field list=Client.class.getDeclaredField("players");PinkFixture.U.putObject(client,PinkFixture.U.objectFieldOffset(list),new CopyOnWriteArrayList<>());
        Field lock=Client.class.getDeclaredField("registryLock");PinkFixture.U.putObject(client,PinkFixture.U.objectFieldOffset(lock),new Object());PinkFixture.replace(Client.class,"instance",client);
        PinkFixture.install();PinkFixture.replace(InventoryService.class,"I",new InventoryService());
    }
    private static void setup()throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        try(Connection c=DriverManager.getConnection(HOST+OPTIONS,"root","");Statement s=c.createStatement()){s.executeUpdate("CREATE DATABASE IF NOT EXISTS "+DATABASE+" CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");}
        try(Connection c=open();Statement s=c.createStatement()){
            s.executeUpdate("CREATE TABLE IF NOT EXISTS account(id INT PRIMARY KEY,username VARCHAR(100),is_admin TINYINT NOT NULL DEFAULT 0,admin TINYINT NOT NULL DEFAULT 0,ban TINYINT NOT NULL DEFAULT 0,vnd INT NOT NULL DEFAULT 0,thoi_vang INT NOT NULL DEFAULT 0,tongnap INT NOT NULL DEFAULT 0,active TINYINT NOT NULL DEFAULT 0,password VARCHAR(100)) ENGINE=InnoDB");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS player(id BIGINT PRIMARY KEY,account_id INT NOT NULL,gender TINYINT NOT NULL,data_inventory TEXT,data_point TEXT,data_location TEXT,data_magic_tree TEXT,data_task TEXT,pet LONGTEXT,items_bag LONGTEXT,items_box LONGTEXT,items_body LONGTEXT,skills TEXT) ENGINE=InnoDB");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS server_command_queue(id BIGINT AUTO_INCREMENT PRIMARY KEY,command_type VARCHAR(50) NOT NULL,payload LONGTEXT NOT NULL,status VARCHAR(20) NOT NULL DEFAULT 'pending',created_by VARCHAR(100),request_id VARCHAR(64) UNIQUE,actor_account_id INT NOT NULL,created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,executed_at TIMESTAMP NULL,result_message VARCHAR(255)) ENGINE=InnoDB");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS admin_command_receipt(command_id BIGINT PRIMARY KEY,command_type VARCHAR(50) NOT NULL,player_id BIGINT,account_id INT,result_message VARCHAR(255),created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP) ENGINE=InnoDB");
            // Apply the actual additive production migration twice, exclusively in this fixture DB.
            String migration=java.nio.file.Files.readString(java.nio.file.Path.of("SRC/sql/migrations/20261006_web_command_receipts.sql"));
            for(int pass=0;pass<2;pass++)for(String sql:migration.split(";"))if(!sql.isBlank())s.executeUpdate(sql);
        }
        installClient();
    }
    private static void reset()throws Exception {
        installClient();
        try(Connection c=open();Statement s=c.createStatement()){
            s.executeUpdate("DELETE FROM admin_command_receipt");s.executeUpdate("DELETE FROM server_command_queue");s.executeUpdate("DELETE FROM player");s.executeUpdate("DELETE FROM account");
            s.executeUpdate("INSERT INTO account(id,username,is_admin,admin) VALUES(1,'fixture_admin',1,0),(2,'fixture_player',0,0),(3,'legacy_admin',0,1)");
            try(PreparedStatement p=c.prepareStatement("INSERT INTO player(id,account_id,gender,data_inventory,data_point,data_location,data_magic_tree,data_task,pet,items_bag,items_box,items_body,skills) VALUES(101,2,0,?,?,?,?,?,?,?,?,?,?)")){
                String[] values={"[10,20,30,40,50]","[2,300,400,88,100,1000,2000,3000,40,5,6,0,333,444]","[21,300,336]","[3,8,0,123,456]","[7,0,0,0]","[]","[\"[-1,0,\\\"[]\\\",1]\"]","[\"[-1,0,\\\"[]\\\",1]\"]","[]","[]"};
                for(int i=0;i<values.length;i++)p.setString(i+1,values[i]);p.executeUpdate();
            }
        }
    }
    private static long enqueue(String type,String payload,int actor)throws SQLException {
        try(Connection c=open();PreparedStatement q=c.prepareStatement("INSERT INTO server_command_queue(command_type,payload,actor_account_id,request_id,created_by) VALUES(?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
            q.setString(1,type);q.setString(2,payload);q.setInt(3,actor);q.setString(4,UUID.randomUUID().toString());q.setString(5,"isolated-test");q.executeUpdate();try(ResultSet r=q.getGeneratedKeys()){r.next();return r.getLong(1);}
        }
    }
    private static String scalar(String sql)throws SQLException {try(Connection c=open();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){check(r.next(),"Fixture query row missing");return r.getString(1);}}
    private static long currency(int index)throws SQLException{return Long.parseLong(String.valueOf(((JSONArray)JSONValue.parse(scalar("SELECT data_inventory FROM player WHERE id=101"))).get(index)));}
    private static String status(long id)throws SQLException{return scalar("SELECT status FROM server_command_queue WHERE id="+id);}
    private static int receipts(long id)throws SQLException{return Integer.parseInt(scalar("SELECT COUNT(*) FROM admin_command_receipt WHERE command_id="+id));}
    private static final class Faults implements LiveSyncService.Connections {
        final AtomicBoolean uncertainCommit=new AtomicBoolean(),completeFailure=new AtomicBoolean(),receiptFailure=new AtomicBoolean();
        volatile boolean openFail;
        int thrownCommits,thrownCompletes,thrownReceipts;
        public Connection open()throws SQLException {
            if(openFail)throw new SQLException("Injected database unavailable during shutdown");
            Connection real=LiveSyncQueueTest.open();boolean[] inserted={false};
            return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(proxy,method,args)->{
                if(method.getName().equals("prepareStatement")){
                    String sql=String.valueOf(args[0]).toLowerCase(Locale.ROOT);PreparedStatement stmt=(PreparedStatement)invoke(real,method,args);
                    return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class[]{PreparedStatement.class},(p,m,a)->{
                        if(m.getName().equals("executeUpdate")&&sql.startsWith("insert into admin_command_receipt")){
                            if(receiptFailure.compareAndSet(true,false)){thrownReceipts++;throw new SQLException("Injected receipt failure before COMMIT");}
                            Object result=invoke(stmt,m,a);inserted[0]=true;return result;
                        }
                        if(m.getName().equals("executeUpdate")&&sql.contains("status='completed'")&&completeFailure.compareAndSet(true,false)){thrownCompletes++;throw new SQLException("Injected completion status failure");}
                        return invoke(stmt,m,a);
                    });
                }
                if(method.getName().equals("commit")&&inserted[0]&&uncertainCommit.compareAndSet(true,false)){invoke(real,method,args);thrownCommits++;throw new SQLException("Injected lost COMMIT acknowledgement");}
                return invoke(real,method,args);
            });
        }
    }
    private static void basic()throws Exception {
        reset();LiveSyncService service=new LiveSyncService(LiveSyncQueueTest::open);
        long topup=enqueue("TOPUP_PLAYER","{\"player_id\":101,\"add_gold\":7,\"add_gem\":2,\"add_thoi_vang\":3}",1);service.processQueue();
        check(status(topup).equals("completed")&&currency(0)==17&&currency(1)==22&&receipts(topup)==1,"offline topup commits currencies/items/receipt");
        String bag=scalar("SELECT items_bag FROM player WHERE id=101");check(((JSONArray)JSONValue.parse((String)((JSONArray)JSONValue.parse(bag)).get(0))).get(1).toString().equals("3"),"offline gold bars actually granted");
        service.processQueue();check(currency(0)==17&&receipts(topup)==1,"completed command never applied twice");
        try(Connection c=open();Statement s=c.createStatement()){s.executeUpdate("UPDATE server_command_queue SET status='processing' WHERE id="+topup);}
        service.processQueue();check(status(topup).equals("completed")&&currency(0)==17,"processing receipt repairs status without repeated grant");
        long stats=enqueue("UPDATE_STATS","{\"player_id\":101,\"hpg\":2000,\"limit_power\":8}",1);service.processQueue();
        JSONArray points=(JSONArray)JSONValue.parse(scalar("SELECT data_point FROM player WHERE id=101"));check(status(stats).equals("completed")&&points.get(0).toString().equals("8")&&points.get(5).toString().equals("2000")&&points.get(3).toString().equals("88"),"offline stats persist selected fields and preserve stamina");
    }
    private static void malformed()throws Exception {
        reset();LiveSyncService service=new LiveSyncService(LiveSyncQueueTest::open);long bad=enqueue("TOPUP_PLAYER","{broken",1);long good=enqueue("TOPUP_PLAYER","{\"player_id\":101,\"add_gold\":7}",1);service.processQueue();
        check(status(bad).equals("failed")&&receipts(bad)==0&&status(good).equals("completed")&&currency(0)==17,"malformed JSON fails without blocking next command");
    }
    private static void actors()throws Exception {
        reset();LiveSyncService service=new LiveSyncService(LiveSyncQueueTest::open);long legacy=enqueue("TOPUP_PLAYER","{\"player_id\":101,\"add_gold\":7}",3);service.processQueue();check(status(legacy).equals("completed")&&currency(0)==17,"legacy admin flag follows verified web role");
        long denied=enqueue("TOPUP_PLAYER","{\"player_id\":101,\"add_gold\":7}",2);service.processQueue();check(status(denied).equals("failed")&&currency(0)==17,"non-admin denied before credit");
        try(Connection c=open();Statement s=c.createStatement()){s.executeUpdate("UPDATE account SET ban=1 WHERE id=1");}
        long banned=enqueue("TOPUP_PLAYER","{\"player_id\":101,\"add_gold\":7}",1);service.processQueue();check(status(banned).equals("failed")&&currency(0)==17,"banned admin denied before credit");
    }
    private static void uncertain()throws Exception {
        reset();Faults faults=new Faults();faults.uncertainCommit.set(true);LiveSyncService service=new LiveSyncService(faults);long id=enqueue("TOPUP_PLAYER","{\"player_id\":101,\"add_gold\":7}",1);service.processQueue();
        check(faults.thrownCommits==1&&currency(0)==17&&receipts(id)==1&&LiveSyncCommandGate.hasPendingId(101),"lost COMMIT acknowledgement keeps target gated");
        service.processQueue();check(status(id).equals("completed")&&currency(0)==17&&receipts(id)==1&&!LiveSyncCommandGate.hasPendingId(101),"uncertain committed command reconciles receipt exactly once");
    }
    private static void completion()throws Exception {
        reset();Faults faults=new Faults();faults.completeFailure.set(true);LiveSyncService service=new LiveSyncService(faults);long id=enqueue("TOPUP_PLAYER","{\"player_id\":101,\"add_gold\":7}",1);service.processQueue();
        check(faults.thrownCompletes==1&&currency(0)==17&&receipts(id)==1,"completion failure keeps committed receipt");
        service.processQueue();check(status(id).equals("completed")&&currency(0)==17&&receipts(id)==1,"completion retry does not repeat resource mutation");
    }
    private static void rollback()throws Exception {
        reset();Faults faults=new Faults();faults.receiptFailure.set(true);LiveSyncService service=new LiveSyncService(faults);long id=enqueue("TOPUP_PLAYER","{\"player_id\":101,\"add_gold\":7}",1);service.processQueue();
        check(faults.thrownReceipts==1&&currency(0)==10&&receipts(id)==0&&status(id).equals("pending"),"receipt failure rolls resource update back");service.processQueue();check(status(id).equals("completed")&&currency(0)==17,"proven rollback retry applies once");
    }
    private static void account()throws Exception {
        reset();LiveSyncService service=new LiveSyncService(LiveSyncQueueTest::open);long id=enqueue("UPDATE_ACCOUNT","{\"account_id\":2,\"add_vnd\":7,\"add_tongnap\":9}",1);service.processQueue();check(status(id).equals("completed")&&scalar("SELECT vnd FROM account WHERE id=2").equals("7")&&scalar("SELECT tongnap FROM account WHERE id=2").equals("9"),"account credits committed once");
        id=enqueue("UPDATE_ACCOUNT","{\"account_id\":2,\"refresh_only\":true,\"add_vnd\":999}",1);service.processQueue();check(status(id).equals("completed")&&scalar("SELECT vnd FROM account WHERE id=2").equals("7"),"card refresh never adds currency again");
    }
    private static void concurrent()throws Exception {
        reset();long id=enqueue("TOPUP_PLAYER","{\"player_id\":101,\"add_gold\":7}",1);CountDownLatch start=new CountDownLatch(1);ExecutorService threads=Executors.newFixedThreadPool(2);
        try{
            Callable<Void> a=()->{start.await();new LiveSyncService(LiveSyncQueueTest::open).processQueue();return null;};Future<Void> first=threads.submit(a),second=threads.submit(a);start.countDown();first.get(10,TimeUnit.SECONDS);second.get(10,TimeUnit.SECONDS);
            check(status(id).equals("completed")&&currency(0)==17&&receipts(id)==1,"two real consumers apply one command once");
        }finally{threads.shutdownNow();}
    }
    private static void capacity()throws Exception {
        reset();LiveSyncService service=new LiveSyncService(LiveSyncQueueTest::open);long id=enqueue("TOPUP_PLAYER","{\"player_id\":101,\"add_gold\":7,\"add_thoi_vang\":1000001}",1);String original=scalar("SELECT items_bag FROM player WHERE id=101");service.processQueue();
        check(status(id).equals("failed")&&currency(0)==10&&receipts(id)==0&&scalar("SELECT items_bag FROM player WHERE id=101").equals(original),"full-capacity rejection commits neither money nor item");
    }
    private static nro.models.player.Player onlineFixture()throws Exception {
        var p=new PinkFixture.Owner();p.id=101;p.name="fixture_player";p.gender=0;p.isPlayer=true;
        p.nPoint.limitPower=2;p.nPoint.power=300;p.nPoint.tiemNang=400;p.nPoint.stamina=88;p.nPoint.maxStamina=100;p.nPoint.hpg=1000;p.nPoint.mpg=2000;p.nPoint.dameg=3000;p.nPoint.defg=40;p.nPoint.critg=5;p.nPoint.hp=333;p.nPoint.mp=444;p.nPoint.hpMax=1000;p.nPoint.mpMax=2000;
        p.inventory.gold=10;p.inventory.gem=20;p.inventory.ruby=30;p.inventory.coupon=40;p.inventory.event=50;
        for(int i=0;i<9;i++)p.inventory.itemsBody.add(new nro.models.item.Item());p.inventory.itemsBag.add(new nro.models.item.Item());p.inventory.itemsBox.add(new nro.models.item.Item());
        p.magicTree=new nro.models.npc.MagicTree(p,(byte)3,(byte)8,123,false,456);p.playerTask.taskMain=new nro.models.task.TaskMain();p.playerTask.taskMain.id=7;
        var session=PinkFixture.blank(nro.models.network.MySession.class);session.userId=2;session.player=p;p.session=session;Client.gI().put(p);return p;
    }
    private static void online()throws Exception {
        reset();var player=onlineFixture();var bag=player.inventory.itemsBag;var box=player.inventory.itemsBox;Faults faults=new Faults();faults.uncertainCommit.set(true);LiveSyncService service=new LiveSyncService(faults);
        long id=enqueue("TOPUP_PLAYER","{\"player_id\":101,\"add_gold\":7,\"add_gem\":2}",1);service.processQueue();
        check(faults.thrownCommits==1&&currency(0)==17&&currency(1)==22&&receipts(id)==1,"online uncertain COMMIT stores SQL once");
        check(player.inventory.gold==10&&player.inventory.gem==20&&player.inventory.coupon==40&&player.inventory.itemsBag==bag&&LiveSyncCommandGate.hasPendingId(101)&&LiveSyncCommandGate.hasPendingAccount(2),"RAM untouched and gated until receipt reconciliation");
        service.processQueue();check(status(id).equals("completed")&&receipts(id)==1&&currency(0)==17&&player.inventory.gold==17&&player.inventory.gem==22,"receipt reconciliation publishes online currencies once");
        check(player.inventory.coupon==40&&player.inventory.event==50&&player.inventory.itemsBag==bag&&player.inventory.itemsBox==box&&!LiveSyncCommandGate.hasPendingId(101)&&!LiveSyncCommandGate.hasPendingAccount(2),"publication preserves unrelated fields and container references then clears gates");
        service.processQueue();check(player.inventory.gold==17&&currency(0)==17,"another poll does not repeat online credit");
    }
    private static void shutdown()throws Exception {
        reset();Faults faults=new Faults();faults.uncertainCommit.set(true);LiveSyncService service=new LiveSyncService(faults);long id=enqueue("TOPUP_PLAYER","{\"player_id\":101,\"add_gold\":7}",1);service.processQueue();check(LiveSyncCommandGate.hasPendingId(101)&&currency(0)==17,"shutdown fixture has unresolved committed command");
        faults.openFail=true;try{service.stop();throw new AssertionError("shutdown accepted unresolved receipt while DB unavailable");}catch(IllegalStateException expected){}
        check(LiveSyncCommandGate.hasPendingId(101)&&LiveSyncCommandGate.hasPendingAccount(2)&&receipts(id)==1,"failed shutdown retains player/account gates");
        faults.openFail=false;service.processQueue();check(status(id).equals("completed")&&currency(0)==17&&!LiveSyncCommandGate.hasPendingId(101),"database recovery finishes receipt without duplicate credit");service.stop();
    }
    private static void logoutCleanup()throws Exception {
        reset();var player=onlineFixture();var bag=player.inventory.itemsBag;Faults faults=new Faults();faults.uncertainCommit.set(true);LiveSyncService service=new LiveSyncService(faults);
        long id=enqueue("TOPUP_PLAYER","{\"player_id\":101,\"add_gold\":7}",1);service.processQueue();
        check(currency(0)==17&&player.inventory.gold==10&&receipts(id)==1&&LiveSyncCommandGate.hasPendingId(101),"logout fixture starts with one committed unresolved command");
        // Simulate the interval after registry removal starts, before cleanup/disconnect has finished.
        // No real logout or production persistence occurs in this controlled fixture.
        player.beforeDispose=true;service.processQueue();
        check(currency(0)==17&&receipts(id)==1&&player.inventory.gold==10&&player.inventory.itemsBag==bag,"recovery waits for logout cleanup before RAM publication");
        check(LiveSyncCommandGate.hasPendingId(101)&&LiveSyncCommandGate.hasPendingAccount(2)&&!status(id).equals("completed"),"cleanup interval retains both gates and unfinished command status");
        // Restore the simulated state solely to finish this fixture without executing live logout code.
        player.beforeDispose=false;service.processQueue();
        check(currency(0)==17&&player.inventory.gold==17&&receipts(id)==1&&status(id).equals("completed")&&!LiveSyncCommandGate.hasPendingId(101),"controlled cleanup recovery publishes once and releases target");
    }
    private static void singleConnection()throws Exception {
        reset();AtomicBoolean borrowed=new AtomicBoolean();
        LiveSyncService.Connections single=()->{
            if(!borrowed.compareAndSet(false,true))throw new SQLTransientConnectionException("Fixture pool has exactly one connection");
            Connection real;
            try{real=open();}catch(SQLException failure){borrowed.set(false);throw failure;}
            AtomicBoolean closed=new AtomicBoolean();
            return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(proxy,method,args)->{
                if(method.getName().equals("close")){if(closed.compareAndSet(false,true))try{return invoke(real,method,args);}finally{borrowed.set(false);}return null;}
                return invoke(real,method,args);
            });
        };
        LiveSyncService service=new LiveSyncService(single);
        long id=enqueue("TOPUP_PLAYER","{\"player_id\":101,\"add_gold\":7}",1);service.processQueue();
        check(status(id).equals("completed")&&currency(0)==17&&receipts(id)==1&&!borrowed.get(),"one-connection pool must complete and release every phase");
        try(Connection c=open();Statement s=c.createStatement()){s.executeUpdate("UPDATE server_command_queue SET status='processing' WHERE id="+id);}
        service.processQueue();check(status(id).equals("completed")&&currency(0)==17&&!borrowed.get(),"receipt replay closes probe before status write");
    }
    public static void main(String[]args)throws Exception {
        setup();List<String> tests=args.length==0?List.of("basic","malformed","actors","uncertain","completion","rollback","account","concurrent","capacity","online","shutdown","logout_cleanup"):Arrays.asList(args);
        for(String test:tests){switch(test){case "basic"->basic();case "malformed"->malformed();case "actors"->actors();case "uncertain"->uncertain();case "completion"->completion();case "rollback"->rollback();case "account"->account();case "concurrent"->concurrent();case "capacity"->capacity();case "online"->online();case "shutdown"->shutdown();case "logout_cleanup"->logoutCleanup();case "single_connection"->singleConnection();default->throw new IllegalArgumentException("Unknown fixture mode");}System.out.println("PASS actual JDBC queue "+test);}
        System.out.println("PASS isolated database "+DATABASE+"; no production schema selected");
    }
}
