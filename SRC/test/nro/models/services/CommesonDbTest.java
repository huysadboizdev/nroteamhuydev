package nro.models.services;

import java.io.FileInputStream;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import nro.models.commeson.CommesonAttempt;
import nro.models.commeson.CommesonAttempt.State;
import nro.models.database.CommesonDAO;

/** Actual MySQL transactions in a unique disposable database, never the game database. */
public final class CommesonDbTest {
    static String url,user,password;
    static Connection open() throws SQLException {return DriverManager.getConnection(url,user,password);}
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static CommesonAttempt attempt(long player,long now){return new CommesonAttempt(UUID.randomUUID().toString(),player,now);}
    static String bag(long player)throws Exception {
        try(Connection c=open();PreparedStatement q=c.prepareStatement("SELECT items_bag FROM player WHERE id=?")){
            q.setLong(1,player);try(ResultSet r=q.executeQuery()){check(r.next(),"player exists");return r.getString(1);}
        }
    }
    static Connection fault(boolean commitThenThrow)throws SQLException {
        Connection c=open();
        return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,m,args)->{
            try {
                if(commitThenThrow && m.getName().equals("commit")){c.commit();throw new SQLException("injected lost commit acknowledgement");}
                if(!commitThenThrow && m.getName().equals("prepareStatement") && ((String)args[0]).startsWith("UPDATE player SET items_bag"))throw new SQLException("injected inventory SQL failure");
                return m.invoke(c,args);
            }catch(InvocationTargetException e){throw e.getCause();}
        });
    }
    public static void main(String[] args)throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        Properties p=new Properties();try(FileInputStream in=new FileInputStream("Config.properties")){p.load(in);}
        user=p.getProperty("database.user");password=p.getProperty("database.pass","");
        String base="jdbc:mysql://"+p.getProperty("database.host")+":"+p.getProperty("database.port")+"/";
        String database="commeson_spec_"+Long.toHexString(System.nanoTime());
        String options="?useSSL=false&allowPublicKeyRetrieval=true";
        url=base+database+options;
        try(Connection admin=DriverManager.getConnection(base+options,user,password)) {
            try(Statement s=admin.createStatement()){s.execute("CREATE DATABASE `"+database+"`");}
            try {
                try(Connection c=open();Statement s=c.createStatement()) {
                    s.execute("CREATE TABLE player(id BIGINT PRIMARY KEY,lasttimepkcommeson BIGINT NOT NULL DEFAULT 0,items_bag LONGTEXT) ENGINE=InnoDB");
                    s.execute("CREATE TABLE commeson_attempt(player_id BIGINT NOT NULL,attempt_day DATE NOT NULL,match_id VARCHAR(36) NOT NULL,state VARCHAR(12) NOT NULL,deadline BIGINT NOT NULL,PRIMARY KEY(player_id,attempt_day),UNIQUE KEY(match_id)) ENGINE=InnoDB");
                    s.execute("INSERT INTO player(id,items_bag) VALUES(1,'before'),(2,'before'),(3,'before'),(4,'before'),(5,'before')");
                }
                CommesonDAO dao=new CommesonDAO(CommesonDbTest::open);
                long now=java.time.Instant.parse("2026-10-05T12:00:00Z").toEpochMilli();
                ExecutorService pool=Executors.newFixedThreadPool(8);
                CountDownLatch gate=new CountDownLatch(1);List<Future<Boolean>> claims=new ArrayList<>();
                try {
                    for(int i=0;i<8;i++)claims.add(pool.submit(()->{gate.await();return dao.claim(attempt(1,now),0);}));
                    gate.countDown();int won=0;for(Future<Boolean> f:claims)if(f.get(10,TimeUnit.SECONDS))won++;
                    check(won==1,"parallel sessions claim exactly one daily attempt");
                }finally{pool.shutdownNow();}
                check(dao.claim(attempt(1,now+86_400_000),0),"next UTC+7 day claim");
                check(!dao.claim(attempt(2,now),now-1000),"legacy same-day attempt blocks including admin callers");
                check(dao.claim(attempt(2,now),now-86_400_000),"previous legacy day permits claim");
                CommesonAttempt retry=attempt(3,now);retry.state=State.RESERVED;check(dao.claim(retry,0),"prestart claim");dao.releasePreStart(retry);
                check(dao.claim(attempt(3,now),0),"failed prestart can retry same day");
                System.out.println("PASS daily SQL concurrency, timezone, legacy migration, prestart retry");
                CommesonAttempt a=attempt(4,now);check(dao.claim(a,0),"reward claim");a.state=State.WON;check(dao.transition(a,State.FIGHT),"win reservation");
                a.state=State.PICKING;check(dao.transition(a,State.WON),"pickup reservation");a.state=State.PICKED;a.deadline=now+60_000;
                try{new CommesonDAO(()->fault(false)).persistPickup(a,"after");throw new AssertionError("fault must throw");}catch(SQLException expected){}
                check("before".equals(bag(4)),"failed reward write preserves bag");check(dao.pending(4).state==State.PICKING,"failed reward transaction rolls receipt back");
                dao.persistPickup(a,"after");check("after".equals(bag(4)),"reward bag committed");
                check(dao.pending(4).state==State.PICKED && dao.pending(4).deadline==a.deadline,"reward receipt and fresh deadline committed together");
                try{dao.persistPickup(a,"duplicate");throw new AssertionError("duplicate pickup must throw");}catch(SQLException expected){}
                check("after".equals(bag(4)),"duplicate reward cannot overwrite bag");
                CommesonAttempt b=attempt(5,now);check(dao.claim(b,0),"unknown commit claim");b.state=State.WON;dao.transition(b,State.FIGHT);b.state=State.PICKING;dao.transition(b,State.WON);b.state=State.PICKED;b.deadline=now+60_000;
                try{new CommesonDAO(()->fault(true)).persistPickup(b,"committed");throw new AssertionError("unknown commit must throw");}catch(SQLException expected){}
                check(dao.pending(5).state==State.PICKED && "committed".equals(bag(5)),"lost commit acknowledgement reconciles durable successful reward");
                System.out.println("PASS pickup atomic rollback, commit, duplicate denial, lost commit acknowledgement");
                Method finish=CommesonDAO.class.getMethod("finish",CommesonAttempt.class);
                CommesonAttempt pending=dao.pending(3);pending.state=State.RETURN;
                check((boolean)finish.invoke(dao,pending),"terminal completion handles stale in-memory state");
                check(dao.pending(3)==null,"no permanent pending SQL attempt after return");
                check((boolean)finish.invoke(dao,pending),"terminal completion idempotent");
                System.out.println("PASS terminal recovery and idempotent completion");
                CommesonServiceTest.run(dao);
            }finally{try(Statement s=admin.createStatement()){s.execute("DROP DATABASE `"+database+"`");}}
        }
    }
}
