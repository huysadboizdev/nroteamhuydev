package nro.models.event;

import java.lang.reflect.Field;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.simple.JSONObject;

/** Durable revision and run tests on a newly created MariaDB fixture only. */
public final class EventStateDbTest {
    private static String schema;
    private static final String ROOT="jdbc:mysql://127.0.0.1:3306/";
    private static final String OPTIONS="?useSSL=false&allowPublicKeyRetrieval=true";
    private static Connection open()throws SQLException {
        Connection c=DriverManager.getConnection(ROOT+schema+OPTIONS,"root","");
        if(!schema.equals(c.getCatalog()))throw new SQLException("Fixture database guard");
        return c;
    }
    private static void check(boolean b,String label){if(!b)throw new AssertionError(label);}
    private static long number(String sql)throws Exception {
        try(Connection c=open();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getLong(1);}
    }
    @SuppressWarnings("unchecked")
    private static JSONObject payload(boolean enabled,long revision) {
        JSONObject p=new JSONObject();p.put("event_key","summer");p.put("enabled",enabled);p.put("revision",revision);return p;
    }
    private static void stage(boolean enabled,long revision)throws Exception {
        try(Connection c=open()){c.setAutoCommit(false);try{EventStateDAO.stage(c,payload(enabled,revision));c.commit();}catch(Exception failure){c.rollback();throw failure;}}
    }
    private static void rejected(boolean enabled,long revision,String word)throws Exception {
        try{stage(enabled,revision);throw new AssertionError("Expected stage rejection "+word);}
        catch(IllegalArgumentException expected){check(expected.getMessage().contains(word),"Correct rejection: "+word);}
    }
    public static void main(String[]args)throws Exception {
        schema=args[0];if(!schema.matches("event_state_verify_[0-9_]+"))throw new IllegalArgumentException("Isolated fixture only");
        Class.forName("com.mysql.jdbc.Driver");
        try(Connection c=DriverManager.getConnection(ROOT+OPTIONS,"root","");Statement s=c.createStatement()){s.execute("CREATE DATABASE `"+schema+"`");}
        try(Connection c=open();Statement s=c.createStatement()) {
            for(String sql:Files.readString(Path.of("sql/migrations/20261008_event_control.sql")).replaceAll("(?m)^--.*$","").split(";"))
                if(!sql.isBlank())s.execute(sql);
            s.execute("INSERT INTO game_event_state(event_key,last_error) VALUES('summer','')");
        }
        EventRuntimeService runtime=EventRuntimeService.gI();Field backend=EventRuntimeService.class.getDeclaredField("backend");backend.setAccessible(true);
        Object original=backend.get(runtime);
        backend.set(runtime,new EventRuntimeService.Backend(){public boolean ready(String key){return true;}public List<EventRuntimeService.Handle> create(String key){return List.of();}});
        try {
            stage(true,0);
            check(number("SELECT revision FROM game_event_state")==1&&number("SELECT run_id FROM game_event_state")==1,"ON increments revision and starts run");
            stage(true,1);
            check(number("SELECT run_id FROM game_event_state")==1&&number("SELECT revision FROM game_event_state")==2,"Repeated ON retains run and advances revision");
            rejected(false,0,"thay");
            check(number("SELECT desired_enabled FROM game_event_state")==1&&number("SELECT COUNT(*) FROM game_event_cleanup_job")==0,"Stale toggle has no side effects");
            CountDownLatch ready=new CountDownLatch(2),go=new CountDownLatch(1);
            ExecutorService pool=Executors.newFixedThreadPool(2);
            Callable<Boolean> stop=()->{ready.countDown();go.await();try{stage(false,2);return true;}catch(IllegalArgumentException stale){return false;}};
            Future<Boolean> a=pool.submit(stop),b=pool.submit(stop);ready.await(5,TimeUnit.SECONDS);go.countDown();
            try {check(a.get(10,TimeUnit.SECONDS)^b.get(10,TimeUnit.SECONDS),"Exactly one simultaneous revision accepted");}finally{pool.shutdownNow();}
            check(number("SELECT revision FROM game_event_state")==3&&number("SELECT COUNT(*) FROM game_event_cleanup_job")==1,"One closed run creates one durable cleanup job");
            rejected(true,3,"bảo trì");
            check(number("SELECT desired_enabled FROM game_event_state")==0&&number("SELECT run_id FROM game_event_state")==1,"Pending cleanup blocks reopening");
            try(Connection c=open();Statement s=c.createStatement()){s.executeUpdate("UPDATE game_event_cleanup_job SET status='completed'");}
            stage(true,3);
            check(number("SELECT run_id FROM game_event_state")==2,"Completed cleanup allows a new run");
            try(Connection c=open()){c.setAutoCommit(false);EventStateDAO.stage(c,payload(false,4));c.rollback();}
            check(number("SELECT revision FROM game_event_state")==4&&number("SELECT desired_enabled FROM game_event_state")==1&&number("SELECT COUNT(*) FROM game_event_cleanup_job")==1,"Rollback restores desired state and job creation");
            try(Connection c=open()){List<EventStateDAO.State> states=EventStateDAO.read(c);check(states.size()==1&&states.get(0).run()==2&&states.get(0).revision()==4,"Fresh connection retains committed run/revision");}
            System.out.println("EventStateDbTest passed: "+schema);
        }finally{backend.set(runtime,original);}
    }
}
