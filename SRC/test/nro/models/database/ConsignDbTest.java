package nro.models.database;

import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import nro.models.shop_ky_gui.ConsignItem;
import nro.models.item.Item.ItemOption;

/** Runs only against a freshly named isolated fixture catalog. */
public final class ConsignDbTest {
    static String schema; static Properties config=new Properties(); static Class<?> daoType;
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static Connection open()throws SQLException {
        Connection c=DriverManager.getConnection("jdbc:mysql://"+config.getProperty("database.host")+":"+config.getProperty("database.port")+"/"+schema+"?useSSL=false&allowPublicKeyRetrieval=true",config.getProperty("database.user"),config.getProperty("database.pass"));
        if(!schema.equals(c.getCatalog()))throw new SQLException("isolated catalog required");return c;
    }
    static Object dao(RubyGiftDAO.Connections connections)throws Exception {
        Class<?> factory=Class.forName("nro.models.database.ConsignDAO$Connections");
        Object proxy=Proxy.newProxyInstance(factory.getClassLoader(),new Class[]{factory},(p,m,a)->connections.open());
        return daoType.getConstructor(factory).newInstance(proxy);
    }
    static Object invoke(Object dao,String name,Class<?>[] types,Object... arguments)throws Exception {
        try{return daoType.getMethod(name,types).invoke(dao,arguments);}catch(InvocationTargetException e){throw (Exception)e.getCause();}
    }
    static int transact(Object dao,String token,String action,ConsignItem before,ConsignItem after,RubyGiftDAO.Snapshot p)throws Exception {
        return (Integer)invoke(dao,"transact",new Class[]{String.class,String.class,ConsignItem.class,ConsignItem.class,RubyGiftDAO.Snapshot.class},token,action,before,after,p);
    }
    static RubyGiftDAO.Snapshot snapshot(int player,int ruby){return new RubyGiftDAO.Snapshot(player,player,"[9,8,"+ruby+",7,6]","[1]","[2]","[3]","[4]","[5]");}
    static ConsignItem listing(int owner)throws Exception {
        ConsignItem i=new ConsignItem(0,(short)17,owner,(byte)1,-1,100,1,(byte)0,new ArrayList<>(),false);
        ItemOption option=new ItemOption();option.optionTemplate=new nro.models.player_system.Template.ItemOptionTemplate();option.optionTemplate.id=73;option.param=1;i.options.add(option);ConsignItem.class.getField("currency").set(i,"ruby");return i;
    }
    static String token(){return UUID.randomUUID().toString();}
    static int count(String table)throws Exception {try(Connection c=open();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT COUNT(*) FROM "+table)){r.next();return r.getInt(1);}}
    static String stored(String column)throws Exception {try(Connection c=open();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT "+column+" FROM player WHERE id=1")){r.next();return r.getString(1);}}
    static void reset()throws Exception {try(Connection c=open();Statement q=c.createStatement()){q.executeUpdate("DELETE FROM consign_receipt");q.executeUpdate("DELETE FROM shop_ky_gui");q.executeUpdate("DELETE FROM player");q.executeUpdate("INSERT INTO player(id,account_id,data_inventory,items_bag,items_body,items_box,items_box_lucky_round,items_daban) VALUES(1,1,'[9,8,1000,7,6,12345]','[]','[]','[]','[]','[]'),(2,2,'[9,8,1000,7,6,99]','[]','[]','[]','[]','[]')");}}
    static Object fault(int write,boolean committed,boolean[] unavailable)throws Exception {
        return dao(()->{if(unavailable[0])throw new SQLException("outage");Connection c=open();int[] writes={0};return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{try{
            if(m.getName().equals("commit")&&committed){c.commit();unavailable[0]=true;throw new SQLException("lost commit acknowledgement");}
            Object result=m.invoke(c,a);if(m.getName().equals("prepareStatement")){PreparedStatement q=(PreparedStatement)result;return Proxy.newProxyInstance(PreparedStatement.class.getClassLoader(),new Class[]{PreparedStatement.class},(pp,mm,aa)->{try{Object x=mm.invoke(q,aa);if(mm.getName().equals("executeUpdate")&&++writes[0]==write)throw new SQLException("write failure");return x;}catch(InvocationTargetException e){throw e.getCause();}});}return result;
        }catch(InvocationTargetException e){throw e.getCause();}});});
    }
    static void success()throws Exception {
        reset();Object dao=dao(ConsignDbTest::open);ConsignItem item=listing(1);String request=token();int id=transact(dao,request,"post",null,item,snapshot(1,990));
        check(id>0&&item.id==id&&count("shop_ky_gui")==1,"generated durable listing id");
        check(transact(dao,request,"post",null,item,snapshot(1,1))==id&&count("consign_receipt")==1,"replay returns receipt without rewriting inventory");
        check(stored("data_inventory").equals("[9,8,990,7,6,12345]"),"inventory extensions retained");
        for(String c:List.of("items_body","items_box","items_box_lucky_round","items_daban"))check(!stored(c).equals("[]"),"container saved: "+c);
        ConsignItem sold=listing(1);sold.id=id;sold.isBuy=true;transact(dao,token(),"buy",item,sold,snapshot(2,900));
        try{transact(dao,token(),"buy",item,sold,snapshot(1,2));throw new AssertionError("stale listing must reject");}catch(SQLException expected){}
        transact(dao,token(),"claim",sold,null,snapshot(1,1085));check(count("shop_ky_gui")==0&&count("consign_receipt")==3,"claim deletes only listing, retains receipts");
        try{invoke(dao,"reconcile",new Class[]{String.class,String.class,RubyGiftDAO.Snapshot.class},request,"buy",snapshot(1,0));throw new AssertionError("wrong action must reject");}catch(SQLException expected){}
    }
    static void rollback()throws Exception {for(int w=1;w<=3;w++){reset();try{transact(fault(w,false,new boolean[]{false}),token(),"post",null,listing(1),snapshot(1,990));throw new AssertionError("write must fail");}catch(SQLException expected){}check(count("shop_ky_gui")==0&&count("consign_receipt")==0&&stored("data_inventory").contains("1000"),"all writes rollback together");}}
    static void ambiguous()throws Exception {
        reset();boolean[] outage={false};Object dao=fault(0,true,outage);ConsignItem item=listing(1);String request=token();
        try{transact(dao,request,"post",null,item,snapshot(1,990));throw new AssertionError("unknown commit must gate caller");}catch(SQLException expected){check(expected.getClass().getSimpleName().equals("CommitUncertain"),"typed commit uncertainty");}
        outage[0]=false;check((Integer)invoke(dao,"reconcile",new Class[]{String.class,String.class,RubyGiftDAO.Snapshot.class},request,"post",snapshot(1,0))==item.id,"committed request reconciles");
        check(transact(dao,request,"post",null,item,snapshot(1,0))==item.id&&count("shop_ky_gui")==1&&stored("data_inventory").contains("990"),"uncertain replay does not duplicate");
    }
    static void ownership()throws Exception {
        reset();Object dao=dao(ConsignDbTest::open);ConsignItem item=listing(1);transact(dao,token(),"post",null,item,snapshot(1,990));
        try{transact(dao,token(),"cancel",item,null,snapshot(2,900));throw new AssertionError("another player cannot cancel listing");}catch(SQLException expected){}
        check(count("shop_ky_gui")==1&&count("consign_receipt")==1,"ownership rejection has no writes");
    }
    static void concurrency()throws Exception {
        reset();try(Connection c=open();Statement q=c.createStatement()){q.execute("INSERT INTO player(id,account_id,data_inventory,items_bag,items_body,items_box,items_box_lucky_round,items_daban) VALUES(3,3,'[9,8,1000,7,6]','[]','[]','[]','[]','[]')");}
        Object dao=dao(ConsignDbTest::open);ConsignItem before=listing(1);transact(dao,token(),"post",null,before,snapshot(1,990));
        ConsignItem sold=listing(1);sold.id=before.id;sold.isBuy=true;CountDownLatch start=new CountDownLatch(1);ExecutorService pool=Executors.newFixedThreadPool(2);
        try {List<Future<Boolean>> calls=new ArrayList<>();for(int p:new int[]{2,3})calls.add(pool.submit(()->{start.await();try{transact(dao,token(),"buy",before,sold,snapshot(p,900));return true;}catch(SQLException conflict){return false;}}));start.countDown();check(calls.get(0).get(10,TimeUnit.SECONDS)!=calls.get(1).get(10,TimeUnit.SECONDS),"one buyer wins CAS");check(count("consign_receipt")==2,"losing buyer leaves no receipt");}
        finally{pool.shutdownNow();}
    }
    static void absentCommit()throws Exception {
        reset();boolean[] unavailable={false};Object dao=dao(()->{if(unavailable[0])throw new SQLException("outage");Connection c=open();return(Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{try{if(m.getName().equals("commit")){c.rollback();unavailable[0]=true;throw new SQLException("lost acknowledgement before apply");}return m.invoke(c,a);}catch(InvocationTargetException e){throw e.getCause();}});});String request=token();
        try{transact(dao,request,"post",null,listing(1),snapshot(1,990));throw new AssertionError("absent unknown commit must remain gated");}catch(SQLException failure){check(failure.getClass().getSimpleName().equals("CommitUncertain"),"absent commit typed uncertainty");}
        unavailable[0]=false;check(invoke(dao,"reconcile",new Class[]{String.class,String.class,RubyGiftDAO.Snapshot.class},request,"post",snapshot(1,0))==null&&count("shop_ky_gui")==0&&stored("data_inventory").contains("1000"),"locked lookup confirms absence and rollback");
    }
    static void idCap()throws Exception {
        reset();try(Connection c=open();Statement q=c.createStatement()){q.execute("ALTER TABLE shop_ky_gui AUTO_INCREMENT=32768");}
        ConsignItem item=listing(1);Object dao=dao(ConsignDbTest::open);String request=token();int id=transact(dao,request,"post",null,item,snapshot(1,990));
        check(id>=32768&&item.id==id&&count("shop_ky_gui")==1&&count("consign_receipt")==1&&stored("data_inventory").contains("990"),"database ids remain usable beyond wire signed-short capacity");
        check((Integer)invoke(dao,"reconcile",new Class[]{String.class,String.class,RubyGiftDAO.Snapshot.class},request,"post",snapshot(1,0))==id,"receipt retains full database id");
    }
    static RubyGiftDAO.Snapshot vipSnapshot(String vip) {
        return new RubyGiftDAO.Snapshot(1,1,"[4000000000,8,1000,7,6]","[\"reward\"]","[2]","[3]","[4]","[5]",vip);
    }
    static void skhVip()throws Exception {
        for(String action:List.of("skh_vip","skh_claim")) {
            reset();Object dao=dao(ConsignDbTest::open);String request=token();
            String vip="[0,0,false,false,false,0,0,0,1,1,1,[2]]";
            check(transact(dao,request,action,null,null,vipSnapshot(vip))==0,"VIP receipt has no listing: "+action);
            check(stored("data_inventory").equals("[4000000000,8,1000,7,6,12345]")
                    &&stored("data_vip").equals(vip)&&stored("items_bag").equals("[\"reward\"]")
                    &&count("consign_receipt")==1&&count("shop_ky_gui")==0,"VIP gold, bag, progress and receipt commit together: "+action);
            transact(dao,request,action,null,null,vipSnapshot("[99]"));
            check(stored("data_vip").equals(vip)&&count("consign_receipt")==1,"VIP replay cannot rewrite progress: "+action);
            transact(dao,token(),"box1538",null,null,snapshot(1,1000));
            check(stored("data_vip").equals(vip),"legacy null-VIP snapshot preserves progress");
            for(int write=1;write<=2;write++) {
                reset();try {transact(fault(write,false,new boolean[]{false}),token(),action,null,null,vipSnapshot(vip));
                    throw new AssertionError("VIP write must rollback");}catch(SQLException expected){}
                check(stored("data_vip").equals("[]")&&stored("items_bag").equals("[]")
                        &&stored("data_inventory").contains("1000")&&count("consign_receipt")==0,"VIP rollback is atomic: "+action+" write "+write);
            }
            reset();boolean[] outage={false};Object uncertain=fault(0,true,outage);String uncertainToken=token();
            try {transact(uncertain,uncertainToken,action,null,null,vipSnapshot(vip));throw new AssertionError("VIP uncertain outcome must gate");}
            catch(SQLException expected){check(expected instanceof ConsignDAO.CommitUncertain,"VIP typed uncertainty");}
            outage[0]=false;check((Integer)invoke(uncertain,"reconcile",new Class[]{String.class,String.class,RubyGiftDAO.Snapshot.class},
                    uncertainToken,action,vipSnapshot(vip))==0,"VIP uncertain receipt reconciles");
            transact(uncertain,uncertainToken,action,null,null,vipSnapshot("[99]"));
            check(stored("data_vip").equals(vip)&&stored("items_bag").equals("[\"reward\"]")
                    &&count("consign_receipt")==1,"VIP commit acknowledgement loss cannot replay reward");
        }
    }
    public static void main(String[] args)throws Exception {
        try{daoType=Class.forName("nro.models.database.ConsignDAO");}catch(ClassNotFoundException missing){throw new AssertionError("durable consignment DAO missing",missing);}
        schema=args[0];if(!schema.matches("consign_verify_[0-9_]+"))throw new IllegalArgumentException("isolated schema only");
        try(var in=Files.newInputStream(Path.of("Config.properties"))){config.load(in);}Class.forName(config.getProperty("database.driver"));
        try(Connection c=DriverManager.getConnection("jdbc:mysql://"+config.getProperty("database.host")+":"+config.getProperty("database.port")+"/",config.getProperty("database.user"),config.getProperty("database.pass"));Statement q=c.createStatement()){q.execute("CREATE DATABASE `"+schema+"`");}
        try(Connection c=open();Statement q=c.createStatement()){
            q.execute("CREATE TABLE player(id BIGINT PRIMARY KEY,account_id INT,data_inventory LONGTEXT,items_bag LONGTEXT,items_body LONGTEXT,items_box LONGTEXT,items_box_lucky_round LONGTEXT,items_daban LONGTEXT,data_vip LONGTEXT DEFAULT '[]') ENGINE=InnoDB");
            q.execute("CREATE TABLE shop_ky_gui(id INT PRIMARY KEY,player_id INT,tab INT,item_id INT,gold INT,gem INT,quantity INT,itemOption TEXT,isUpTop INT,isBuy INT) ENGINE=InnoDB");
            q.execute("INSERT INTO shop_ky_gui VALUES(8,1,1,17,9,-1,1,'[]',0,0)");
            String migration=Files.readString(Path.of("sql/migrations/20261005_consignment_ruby.sql")).replaceAll("(?m)^--.*$","");
            for(int run=0;run<2;run++)for(String sql:migration.split(";"))if(!sql.isBlank())q.execute(sql);
            try(ResultSet r=q.executeQuery("SELECT id,currency FROM shop_ky_gui")){r.next();check(r.getInt(1)==8&&r.getString(2).equals("legacy"),"migration retains legacy listings without currency conversion");}
        }
        success();rollback();ambiguous();ownership();concurrency();absentCommit();idCap();skhVip();
        reset();Object activationDao=dao(ConsignDbTest::open);String activationToken=token();
        check(transact(activationDao,activationToken,"box1538",null,null,snapshot(1,1000))==0,"activation box inventory-only receipt");
        check(count("shop_ky_gui")==0&&count("consign_receipt")==1&&stored("items_bag").equals("[1]"),"activation reward inventory saved without a listing");
        transact(activationDao,activationToken,"box1538",null,null,snapshot(1,1));
        check(stored("data_inventory").contains("1000")&&count("consign_receipt")==1,"activation box receipt replay cannot rewrite inventory");
        reset();Object inventoryDao=dao(ConsignDbTest::open);String purchaseToken=token();
        check(transact(inventoryDao,purchaseToken,"shop1775",null,null,snapshot(1,900))==0,"inventory-only receipt id");
        check(count("shop_ky_gui")==0&&count("consign_receipt")==1&&stored("data_inventory").contains("900"),"shop receipt must update only player");
        transact(inventoryDao,purchaseToken,"shop1775",null,null,snapshot(1,1));
        check(stored("data_inventory").contains("900")&&count("consign_receipt")==1,"inventory-only replay must be idempotent");
        transact(inventoryDao,token(),"box1775",null,null,snapshot(1,900));
        check(count("shop_ky_gui")==0&&count("consign_receipt")==2,"box receipt must not alter listings");
        String materialToken=token();
        transact(inventoryDao,materialToken,"ruby_material",null,null,snapshot(1,800));
        transact(inventoryDao,materialToken,"ruby_material",null,null,snapshot(1,1));
        check(count("shop_ky_gui")==0&&count("consign_receipt")==3&&stored("data_inventory").contains("800"),"material receipt saves wallet once without listings");
        try(Connection c=open();Statement q=c.createStatement()){q.execute("ALTER TABLE player ADD data_intrinsic TEXT");q.execute("UPDATE player SET data_intrinsic='[1,5,0,4,true,1,300,98765]' WHERE id=1");}
        var maxDao=new ConsignDAO(ConsignDbTest::open);String maxToken=token();String maxData="[3,35,35,4,true,1,300,98765]";
        check(maxDao.transactIntrinsic(maxToken,snapshot(1,700),maxData)==0,"max receipt inventory-only");
        check(stored("data_intrinsic").equals(maxData)&&stored("data_inventory").contains("700")&&count("consign_receipt")==4,"intrinsic and ruby commit together");
        maxDao.transactIntrinsic(maxToken,snapshot(1,600),"[23,300,0,4,true,1,300,98765]");
        check(stored("data_intrinsic").equals(maxData)&&stored("data_inventory").contains("700"),"max replay changes neither money nor intrinsic");
        try{((ConsignDAO)fault(1,false,new boolean[]{false})).transactIntrinsic(token(),snapshot(1,600),"[23,300,0,4,true,1,300,98765]");throw new AssertionError("max rollback required");}catch(SQLException expected){}
        check(stored("data_intrinsic").equals(maxData)&&stored("data_inventory").contains("700")&&count("consign_receipt")==4,"intrinsic and money rollback together");
        boolean[] maxOutage={false};ConsignDAO uncertainMax=(ConsignDAO)fault(0,true,maxOutage);String maxUnknown=token();
        try{uncertainMax.transactIntrinsic(maxUnknown,snapshot(1,600),"[23,300,0,4,true,1,300,98765]");throw new AssertionError("max unknown outcome required");}catch(ConsignDAO.CommitUncertain expected){}
        maxOutage[0]=false;check(uncertainMax.reconcile(maxUnknown,"intrinsic_max",snapshot(1,600))==0,"max receipt reconciliation");
        check(stored("data_intrinsic").startsWith("[23,300,0,4,true,1,300,98765]")&&stored("data_inventory").contains("600"),"unknown commit has durable max, countOpen and cooldown");
        System.out.println("ConsignDbTest PASS: idempotent migration, durable ruby listing/claim, CAS/ownership/concurrent buyers, full containers/extensions, replay, rollback, committed/absent uncertain commit, database IDs beyond 32767, durable divine box/shop receipts");
    }
}
