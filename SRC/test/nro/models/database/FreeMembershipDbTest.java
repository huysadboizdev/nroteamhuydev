package nro.models.database;

import java.sql.*;
import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;

public final class FreeMembershipDbTest {
    static Properties config=new Properties();static String schema;
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    static Connection open()throws SQLException{
        return DriverManager.getConnection("jdbc:mysql://"+config.getProperty("database.host")+":"+config.getProperty("database.port")+"/"+schema+"?useSSL=false&allowPublicKeyRetrieval=true",config.getProperty("database.user"),config.getProperty("database.pass"));
    }
    static void sql(String sql)throws Exception{try(Connection c=open();Statement q=c.createStatement()){q.execute(sql);}}
    static String value(String sql)throws Exception{try(Connection c=open();Statement q=c.createStatement();ResultSet r=q.executeQuery(sql)){r.next();return r.getString(1);}}
    static void reset()throws Exception{sql("UPDATE account SET active=0,ban=0,vnd=123456");sql("UPDATE player SET data_task='[1,1,0,0]'");}
    static Connection fault(boolean committed)throws SQLException {
        Connection c=open();return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
            try{
                if(m.getName().equals("commit")&&committed){c.commit();throw new SQLException("lost ACK");}
                if(m.getName().equals("prepareStatement")&&!committed&&((String)a[0]).startsWith("UPDATE account"))throw new SQLException("activation update failure");
                return m.invoke(c,a);
            }catch(InvocationTargetException e){throw e.getCause();}
        });
    }
    public static void main(String[] args)throws Exception {
        schema=args[0];if(!schema.matches("membership_verify_[0-9_]+"))throw new IllegalArgumentException("isolated DB only");
        try(var in=Files.newInputStream(Path.of("Config.properties"))){config.load(in);}Class.forName(config.getProperty("database.driver"));
        try(Connection c=DriverManager.getConnection("jdbc:mysql://"+config.getProperty("database.host")+":"+config.getProperty("database.port")+"/",config.getProperty("database.user"),config.getProperty("database.pass"));Statement q=c.createStatement()){q.execute("CREATE DATABASE "+schema);}
        sql("CREATE TABLE account(id INT PRIMARY KEY,active BOOLEAN NOT NULL,ban BOOLEAN NOT NULL,vnd INT) ENGINE=InnoDB");
        sql("CREATE TABLE player(id BIGINT PRIMARY KEY,account_id INT,data_task TEXT) ENGINE=InnoDB");
        sql("INSERT INTO account VALUES(1,0,0,123456),(2,0,0,123456)");sql("INSERT INTO player VALUES(10,1,'[1,1,0,0]'),(11,1,'[1,1,0,0]')");
        var dao=new FreeMembershipDAO(FreeMembershipDbTest::open);String task="[2,0,0,12345]";
        check(dao.activate(10,1,task)&&value("SELECT active FROM account WHERE id=1").equals("1"),"durable activation");
        check(value("SELECT data_task FROM player WHERE id=10").equals(task)&&value("SELECT vnd FROM account WHERE id=1").equals("123456"),"task saved with unchanged wallet");
        check(dao.activate(10,1,"[3,0,0,999]")&&value("SELECT data_task FROM player WHERE id=10").equals(task),"repeat cannot overwrite task or debit");
        reset();try{dao.activate(10,2,task);throw new AssertionError("ownership required");}catch(SQLException expected){}
        check(value("SELECT active FROM account WHERE id=2").equals("0"),"other account untouched");
        try{dao.activate(10,1,"[1,1,0,0]");throw new AssertionError("unfinished task required");}catch(SQLException expected){}
        sql("UPDATE account SET ban=1 WHERE id=1");try{dao.activate(10,1,task);throw new AssertionError("banned account denied");}catch(SQLException expected){}
        reset();try{new FreeMembershipDAO(()->fault(false)).activate(10,1,task);throw new AssertionError("failure required");}catch(SQLException expected){}
        check(value("SELECT active FROM account WHERE id=1").equals("0")&&value("SELECT data_task FROM player WHERE id=10").equals("[1,1,0,0]"),"account and task rollback together");
        check(new FreeMembershipDAO(()->fault(true)).activate(10,1,task),"lost ACK reconciles active under lock");
        reset();var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        Future<Boolean> a=pool.submit(()->{start.await();return dao.activate(10,1,task);});
        Future<Boolean> b=pool.submit(()->{start.await();return dao.activate(11,1,task);});start.countDown();
        check(a.get(8,TimeUnit.SECONDS)&&b.get(8,TimeUnit.SECONDS),"concurrent idempotent activation");pool.shutdownNow();
        check(value("SELECT active FROM account WHERE id=1").equals("1")&&value("SELECT vnd FROM account WHERE id=1").equals("123456"),"concurrent free cost zero");
        System.out.println("PASS FreeMembership SQL: ownership, task proof, ban, free cost, replay, rollback, lost ACK and concurrent activation");
    }
}
