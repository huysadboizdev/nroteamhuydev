package nro.models.event;

import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import com.zaxxer.hikari.*;

/** Only a freshly created, explicitly named isolated database is used. */
public final class EventCleanupServiceDbTest {
    private static String schema;
    private static final String URL="jdbc:mysql://127.0.0.1:3306/";
    private static final String OPTIONS="?useSSL=false&allowPublicKeyRetrieval=true";
    private static Connection open() throws SQLException {
        Connection connection=DriverManager.getConnection(URL+schema+OPTIONS,"root","");
        if(!schema.equals(connection.getCatalog()))throw new SQLException("Fixture catalog guard failed");
        return connection;
    }
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);}
    private static Object invoke(String name,Class<?>[] signature,Object...args)throws Exception {
        Method method=EventCleanupService.class.getDeclaredMethod(name,signature);method.setAccessible(true);
        try{return method.invoke(null,args);}catch(InvocationTargetException e){throw (Exception)e.getCause();}
    }
    private static void clean(Connection c,boolean maintenance)throws Exception {
        invoke("clean",new Class<?>[]{Connection.class,long.class,boolean.class,Map.class,nro.models.player.Player.class},c,1L,maintenance,null,null);
    }
    private static long count(String sql)throws Exception {try(Connection c=open();Statement q=c.createStatement();ResultSet r=q.executeQuery(sql)){r.next();return r.getLong(1);}}
    private static void sql(String sql)throws Exception {try(Connection c=open();Statement q=c.createStatement()){q.executeUpdate(sql);}}
    public static void main(String[] args)throws Exception {
        schema=args[0];if(!schema.matches("event_cleanup_verify_[0-9_]+"))throw new IllegalArgumentException("isolated fixture only");
        Class.forName("com.mysql.jdbc.Driver");
        try(Connection c=DriverManager.getConnection(URL+OPTIONS,"root","");Statement q=c.createStatement()){q.execute("CREATE DATABASE `"+schema+"`");}
        try(Connection c=open();Statement q=c.createStatement()){
            String migration=Files.readString(Path.of("sql/migrations/20261008_event_control.sql")).replaceAll("(?m)^--.*$","");
            for(String statement:migration.split(";"))if(!statement.isBlank())q.execute(statement);
            q.execute("CREATE TABLE player(id BIGINT PRIMARY KEY,items_bag MEDIUMTEXT,items_body MEDIUMTEXT,items_daban MEDIUMTEXT,pet MEDIUMTEXT,items_box MEDIUMTEXT,items_collection MEDIUMTEXT,items_box_lucky_round MEDIUMTEXT) ENGINE=InnoDB");
            q.execute("CREATE TABLE shop_ky_gui(id BIGINT PRIMARY KEY,player_id BIGINT,item_id INT,isBuy INT,quantity INT,itemOption TEXT,gold INT,gem INT) ENGINE=InnoDB");
            String rewards="[1776,1,[],5],[1777,1,[],6],[648,1,[],7],[1804,1,[],8],[569,1,[],9]";
            q.execute("INSERT INTO player VALUES(1,'[[1220,2,[],1],[1612,3,[],2],[1150,1,[[50,20]],3],"+rewards+"]','[]','[]','[]','[[1612,4,[],4],"+rewards+"]','["+rewards+"]','["+rewards+"]')");
            q.execute("INSERT INTO game_event_state(event_key,last_error) VALUES('hung_vuong',''),('summer',''),('halloween','')");
            q.execute("INSERT INTO game_event_cleanup_job(event_key,run_id,consumed_ids,error_message) VALUES('hung_vuong',1,'[1220]',''),('summer',2,'[1612]',''),('halloween',3,'[]','')");
        }
        try(Connection c=open()){clean(c,false);}
        check(count("SELECT COUNT(DISTINCT job_id) FROM game_event_cleanup_backup WHERE field_name='items_bag'")==2,"each event has its own immutable backup");
        check(count("SELECT COUNT(*) FROM player WHERE items_bag LIKE '%1150,1%' AND items_box LIKE '%1612,4%'")==1,"permanent reward retained and boxes deferred");
        try(Connection c=open()){clean(c,true);}
        check(count("SELECT COUNT(*) FROM game_event_cleanup_receipt WHERE phase='maintenance'")==3,"empty consumable jobs still receive maintenance receipts");
        check(count("SELECT SUM(processed_players) FROM game_event_cleanup_job")==3,"durable progress per job");
        for(int reward:new int[]{1776,1777,648,1804,569})for(String field:new String[]{"items_bag","items_box","items_collection","items_box_lucky_round"})
            check(count("SELECT COUNT(*) FROM player WHERE "+field+" LIKE '%["+reward+",1,%'")==1,"reward/shared ID "+reward+" retained in "+field);
        long backups=count("SELECT COUNT(*) FROM game_event_cleanup_backup");
        try(Connection c=open()){clean(c,true);}
        check(count("SELECT COUNT(*) FROM game_event_cleanup_backup")==backups,"retry retains immutable backups");
        check(count("SELECT SUM(processed_players) FROM game_event_cleanup_job")==3,"retry does not double count progress");
        sql("UPDATE player SET items_bag='[[1220,1,[],9]]',items_box='BROKEN' WHERE id=1");
        try(Connection c=open()){try{clean(c,true);throw new AssertionError("malformed inventory accepted");}catch(IllegalArgumentException expected){}}
        check(count("SELECT COUNT(*) FROM player WHERE items_bag='[[1220,1,[],9]]'")==1,"malformed player rolls back all field changes");
        sql("UPDATE player SET items_box='[]' WHERE id=1");
        try(Connection c=open()){Connection fault=(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->{
            if(m.getName().equals("commit")){c.rollback();throw new SQLException("commit rejected");}
            try{return m.invoke(c,a);}catch(InvocationTargetException e){throw e.getCause();}
        });try{clean(fault,false);throw new AssertionError("commit failure missing");}catch(SQLException expected){}}
        check(count("SELECT COUNT(*) FROM player WHERE items_bag='[[1220,1,[],9]]'")==1,"confirmed failed commit retains original items");
        try(Connection c=open()){check(!(Boolean)invoke("committed",new Class<?>[]{Connection.class,long.class,String.class},c,1L,UUID.randomUUID().toString()),"receipt absent after player lock confirms rollback");}
        try(Connection c=open()){Connection fault=(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(p,m,a)->{
            if(m.getName().equals("commit")){c.commit();throw new SQLException("lost acknowledgement after commit");}
            try{return m.invoke(c,a);}catch(InvocationTargetException e){throw e.getCause();}
        });try{clean(fault,false);throw new AssertionError("lost commit acknowledgement missing");}catch(SQLException expected){}}
        String operation;try(Connection c=open();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT operation_id FROM game_event_cleanup_backup WHERE before_json='[[1220,1,[],9]]'")){check(r.next(),"uncertain commit retains immutable backup");operation=r.getString(1);}
        try(Connection c=open()){check((Boolean)invoke("committed",new Class<?>[]{Connection.class,long.class,String.class},c,1L,operation),"receipt proves committed operation after lost acknowledgement");}
        sql("INSERT INTO shop_ky_gui VALUES(1,1,1220,0,2,'reward options',99,0),(2,1,1220,1,2,'sold claim',99,0),(3,1,1150,0,1,'permanent reward',0,100)");
        try(Connection c=open()){invoke("cleanConsign",new Class<?>[]{Connection.class},c);}
        check(count("SELECT COUNT(*) FROM shop_ky_gui")==2,"sold money claim and permanent reward listings retained");
        check(count("SELECT COUNT(*) FROM game_event_cleanup_consign_backup WHERE listing_id=1 AND before_json LIKE '%reward options%'")==1,"unsold listing backed up before deletion");
        try(Connection c=open()){invoke("cleanConsign",new Class<?>[]{Connection.class},c);}
        check(count("SELECT COUNT(*) FROM game_event_cleanup_consign_backup")==1,"consign cleanup retry idempotent");
        sql("INSERT INTO player VALUES(2,'[]','[]','[]','[]','BROKEN','[]','[]')");
        Class<?> connections=Class.forName("nro.models.event.EventCleanupService$Connections");
        // Receipt absence cannot be interpreted until the original player transaction finishes.
        ExecutorService threads=Executors.newSingleThreadExecutor();
        try(Connection held=open()) {
            held.setAutoCommit(false);
            try(Statement q=held.createStatement()){q.executeQuery("SELECT id FROM player WHERE id=1 FOR UPDATE").close();}
            String uncertain=UUID.randomUUID().toString();
            try(PreparedStatement q=held.prepareStatement("INSERT INTO game_event_cleanup_operation(operation_id,player_id,phase) VALUES(?,1,'live')")){q.setString(1,uncertain);q.executeUpdate();}
            CountDownLatch started=new CountDownLatch(1);
            Future<Boolean> reconciled=threads.submit(()->{try(Connection c=open()){started.countDown();return (Boolean)invoke("committed",new Class<?>[]{Connection.class,long.class,String.class},c,1L,uncertain);}});
            check(started.await(2,TimeUnit.SECONDS),"reconciliation begins");
            try{reconciled.get(200,TimeUnit.MILLISECONDS);throw new AssertionError("reconciliation bypassed original transaction lock");}catch(TimeoutException expected){}
            held.commit();check(reconciled.get(3,TimeUnit.SECONDS),"waited for in-flight commit before receipt proof");
        }finally {threads.shutdownNow();}
        Object direct=Proxy.newProxyInstance(connections.getClassLoader(),new Class<?>[]{connections},(p,m,a)->open());
        var detached=new nro.models.player.Player();detached.id=1;detached.beforeDispose=true;
        Class<?> pendingType=Class.forName("nro.models.event.EventCleanupService$Pending");
        Constructor<?> constructor=pendingType.getDeclaredConstructors()[0];constructor.setAccessible(true);
        Field pendingField=EventCleanupService.class.getDeclaredField("PENDING");pendingField.setAccessible(true);
        @SuppressWarnings("unchecked") Map<Long,Object> pending=(Map<Long,Object>)pendingField.get(null);
        pending.put(1L,constructor.newInstance(UUID.randomUUID().toString(),42,Map.of("items_bag","must not publish rollback"),detached));
        nro.models.services.LiveSyncCommandGate.mark(1,42);
        check(EventCleanupService.pendingPlayers().contains(detached),"detached logout player remains discoverable");
        Object originalBag=detached.inventory.itemsBag;
        invoke("recover",new Class<?>[]{nro.models.player.Player.class,connections},detached,direct);
        check(!EventCleanupService.pending(1)&&!nro.models.services.LiveSyncCommandGate.hasPendingId(1)&&!nro.models.services.LiveSyncCommandGate.hasPendingAccount(42),"confirmed rollback clears player/account gates");
        check(detached.inventory.itemsBag==originalBag,"rollback recovery retains RAM references");
        HikariConfig config=new HikariConfig();config.setJdbcUrl(URL+schema+OPTIONS);config.setUsername("root");config.setPassword("");config.setMaximumPoolSize(1);config.setMinimumIdle(0);config.setConnectionTimeout(1000);
        try(HikariDataSource pool=new HikariDataSource(config)) {
            Object factory=Proxy.newProxyInstance(connections.getClassLoader(),new Class<?>[]{connections},(p,m,a)->pool.getConnection());
            try{invoke("maintenance",new Class<?>[]{connections},factory);throw new AssertionError("corrupt player should fail maintenance");}catch(IllegalArgumentException expected){}
            check(count("SELECT COUNT(*) FROM game_event_cleanup_job WHERE status='error' AND error_message LIKE '%Invalid%'")==3,"failed maintenance records durable error");
            check(count("SELECT SUM(processed_players) FROM game_event_cleanup_job")==3,"failed maintenance retains committed progress");
            sql("UPDATE player SET items_box='[]' WHERE id=2");
            invoke("maintenance",new Class<?>[]{connections},factory);
            check(count("SELECT COUNT(*) FROM game_event_cleanup_job WHERE status='completed' AND processed_players=2")==3,"retry finishes with complete receipts under pool max=1");
        }
        System.out.println("EventCleanupServiceDbTest passed: "+schema);
    }
}
