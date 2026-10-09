package nro.models.services;

import com.google.gson.*;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import nro.models.database.GiftCodeClaimDAO;

/** Uses real JDBC/DAO transactions, exclusively in disposable codex_gift_test_* schemas. */
public final class GiftCodeSupplementDatabaseTest {
    static String url;
    static Connection db() throws SQLException { return DriverManager.getConnection(url,"root",""); }
    static void check(boolean ok,String message) { if(!ok) throw new AssertionError(message); }
    static String value(String sql) throws Exception {
        try(Connection c=db();Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)) {
            check(r.next(),"No scalar result: "+sql);return r.getString(1);
        }
    }
    static long number(String sql) throws Exception { return Long.parseLong(value(sql)); }
    static void sql(String sql) throws Exception { try(Connection c=db();Statement s=c.createStatement()) {s.execute(sql);} }
    static String token() {return UUID.randomUUID().toString();}
    static String rewards(int quantity) {return "[{\"id\":457,\"quantity\":"+quantity+",\"options\":[{\"id\":30,\"param\":0},{\"id\":73,\"param\":5}]}]";}
    static String withNew(int quantity,int extra) {return rewards(quantity).substring(0,rewards(quantity).length()-1)+",{\"id\":458,\"quantity\":"+extra+",\"options\":[]}]";}
    static void catalog(String code,int count,String detail) throws Exception {
        sql("INSERT INTO giftcode(code,count_left,detail,datecreate,expired) VALUES('"+code+"',"+count+",'"+detail+"','2000-01-01','2037-12-31')");
    }
    static void detail(String code,String detail) throws Exception {sql("UPDATE giftcode SET detail='"+detail+"' WHERE code='"+code+"'");}
    static Map<Integer,Integer> quantities(String text) {
        Map<Integer,Integer> out=new TreeMap<>();for(JsonElement entry:new JsonParser().parse(text).getAsJsonArray()) {
            JsonObject o=entry.getAsJsonObject();out.put(o.get("id").getAsInt(),o.get("quantity").getAsInt());
        }return out;
    }
    static GiftCodeClaimDAO.Stage stage(long player,int account,Map<Integer,Integer> expected,AtomicInteger calls) {
        return gift->{
            calls.incrementAndGet();check(quantities(gift.detail()).equals(expected),"DAO stage must receive delta: "+gift.detail()+" expected "+expected);
            long gold=number("SELECT JSON_EXTRACT(data_inventory,'$[0]') FROM player WHERE id="+player);
            int added=expected.values().stream().mapToInt(Integer::intValue).sum();
            return new GiftCodeClaimDAO.Snapshot(player,account,"["+(gold+added)+",0,0,0,0]",gift.detail(),"[\""+gift.code()+"\"]");
        };
    }
    static GiftCodeClaimDAO.Receipt claim(GiftCodeClaimDAO dao,String code,String token,Map<Integer,Integer> expected) throws Exception {
        return dao.claim(1,10,code,token,stage(1,10,expected,new AtomicInteger()));
    }
    static void refused(GiftCodeClaimDAO dao,long player,int account,String code) throws Exception {
        try {dao.claim(player,account,code,token(),gift->{throw new AssertionError("Refused claim reached stage: "+code);});throw new AssertionError("Expected SQLException for "+code);}
        catch(SQLException expected) { }
    }
    static String durable(String code) throws Exception {
        return value("SELECT CONCAT_WS('|',data_inventory,items_bag,giftcode) FROM player WHERE id=1")+"|"+
            value("SELECT CONCAT_WS('|',request_id,delivered_detail,remaining_after,UNIX_TIMESTAMP(claimed_at)) FROM giftcode_claim WHERE account_id=10 AND canonical_code='"+code+"'")+"|"+
            value("SELECT count_left FROM giftcode WHERE code='"+code+"'")+"|"+number("SELECT COUNT(*) FROM giftcode_claim_request");
    }
    static Connection fault(Connection inner,String mode) {
        return (Connection)Proxy.newProxyInstance(GiftCodeSupplementDatabaseTest.class.getClassLoader(),new Class[]{Connection.class},(proxy,m,args)->{
            if(m.getName().equals("commit")&&mode.equals("commit")) {inner.commit();throw new SQLException("Lost commit acknowledgement");}
            if(m.getName().equals("prepareStatement")&&mode.equals("update")&&((String)args[0]).startsWith("UPDATE player SET"))throw new SQLException("Injected reward SQL failure");
            try {return m.invoke(inner,args);}catch(InvocationTargetException e) {throw e.getCause();}
        });
    }
    static void tests(boolean red) throws Exception {
        GiftCodeClaimDAO dao=new GiftCodeClaimDAO(GiftCodeSupplementDatabaseTest::db);
        catalog("DELTA",1,rewards(10));String first=token();
        long created=number("SELECT UNIX_TIMESTAMP(datecreate) FROM giftcode WHERE code='DELTA'");
        check(claim(dao,"DELTA",first,Map.of(457,10)).remaining()==0,"First claim consumes one quota");
        long timestamp=number("SELECT UNIX_TIMESTAMP(claimed_at) FROM giftcode_claim WHERE canonical_code='DELTA' AND account_id=10");
        detail("DELTA",withNew(15,2));String second=token();
        check(claim(dao,"DELTA",second,Map.of(457,5,458,2)).remaining()==0,"Exhausted existing claimant receives supplement");
        if(red) {System.out.println("Unexpected old implementation success");return;}
        check(number("SELECT count_left FROM giftcode WHERE code='DELTA'")==0,"Supplement consumed quota");
        check(number("SELECT JSON_EXTRACT(data_inventory,'$[0]') FROM player WHERE id=1")==17,"Delta player balance");
        check(number("SELECT JSON_EXTRACT(data_inventory,'$[5]') FROM player WHERE id=1")==777,"Stored inventory extension preserved");
        check(quantities(value("SELECT delivered_detail FROM giftcode_claim WHERE canonical_code='DELTA' AND account_id=10")).equals(Map.of(457,15,458,2)),"Watermark not cumulative");
        check(number("SELECT UNIX_TIMESTAMP(claimed_at) FROM giftcode_claim WHERE canonical_code='DELTA' AND account_id=10")==timestamp,"Original claimed_at changed");
        check(number("SELECT UNIX_TIMESTAMP(datecreate) FROM giftcode WHERE code='DELTA'")==created,"Catalog creation time changed");
        check(dao.reconcile(1,10,"DELTA",first).remaining()==0,"Old token receipt lost after supplement");
        dao.claim(1,10,"DELTA",first,gift->{throw new AssertionError("Old token restaged");});
        dao.claim(1,10,"DELTA",second,gift->{throw new AssertionError("Same token restaged");});
        check(number("SELECT COUNT(*) FROM giftcode_claim_request WHERE canonical_code='DELTA'")==2,"Per-request receipts missing");
        refused(dao,1,10,"DELTA");refused(dao,2,20,"DELTA");

        detail("DELTA","[{\"id\":458,\"quantity\":3,\"options\":[]}]");
        claim(dao,"DELTA",token(),Map.of(458,1));
        check(quantities(value("SELECT delivered_detail FROM giftcode_claim WHERE canonical_code='DELTA' AND account_id=10")).equals(Map.of(457,15,458,3)),"Removed item watermark lost");
        detail("DELTA",withNew(10,4));claim(dao,"DELTA",token(),Map.of(458,1));
        detail("DELTA",withNew(15,4));refused(dao,1,10,"DELTA");
        detail("DELTA",withNew(16,4).replace("{\"id\":30,\"param\":0},{\"id\":73,\"param\":5}","{\"id\":73,\"param\":5},{\"id\":30,\"param\":0}"));
        claim(dao,"DELTA",token(),Map.of(457,1));
        detail("DELTA",withNew(17,4).replace("\"param\":5","\"param\":6"));String before=durable("DELTA");refused(dao,1,10,"DELTA");check(before.equals(durable("DELTA")),"Option change altered durable state");
        detail("DELTA",withNew(17,4));before=durable("DELTA");
        try {dao.claim(1,10,"DELTA",token(),gift->{throw new IllegalArgumentException("Bag full");});throw new AssertionError("Stage failure accepted");}catch(IllegalArgumentException expected) { }
        check(before.equals(durable("DELTA")),"Stage failure leaked durable changes");
        GiftCodeClaimDAO broken=new GiftCodeClaimDAO(()->fault(db(),"update"));
        try {claim(broken,"DELTA",token(),Map.of(457,1));throw new AssertionError("SQL failure accepted");}catch(SQLException expected) { }
        check(before.equals(durable("DELTA")),"SQL failure leaked watermark/player/receipt/quota");

        AtomicInteger stages=new AtomicInteger();ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch go=new CountDownLatch(1);
        try {
            List<Future<Boolean>> futures=new ArrayList<>();for(int i=0;i<2;i++)futures.add(pool.submit(()->{
                go.await();try {dao.claim(1,10,"DELTA",token(),stage(1,10,Map.of(457,1),stages));return true;}catch(SQLException empty) {return false;}
            }));go.countDown();int successes=0;for(Future<Boolean> f:futures)if(f.get(15,TimeUnit.SECONDS))successes++;
            check(successes==1&&stages.get()==1,"Two tokens delivered same supplement twice");
        }finally {pool.shutdownNow();}

        detail("DELTA",withNew(18,4));AtomicInteger opens=new AtomicInteger();String lostToken=token();
        GiftCodeClaimDAO lost=new GiftCodeClaimDAO(()->opens.incrementAndGet()==1?fault(db(),"commit"):db());
        check(claim(lost,"DELTA",lostToken,Map.of(457,1)).remaining()==0,"Lost commit not recovered");
        detail("DELTA",withNew(19,4));claim(dao,"DELTA",token(),Map.of(457,1));
        check(dao.reconcile(1,10,"DELTA",lostToken)!=null&&dao.reconcile(1,10,"DELTA",first)!=null,"Earlier request receipts unavailable");
        AtomicInteger uncertainOpens=new AtomicInteger();GiftCodeClaimDAO uncertain=new GiftCodeClaimDAO(()->{
            if(uncertainOpens.incrementAndGet()>1)throw new SQLException("Reconciliation unavailable");return fault(db(),"commit");
        });detail("DELTA",withNew(20,4));String uncertainToken=token();
        try {claim(uncertain,"DELTA",uncertainToken,Map.of(457,1));throw new AssertionError("Unknown commit outcome not surfaced");}catch(GiftCodeClaimDAO.CommitUncertain expected) { }
        check(dao.reconcile(1,10,"DELTA",uncertainToken)!=null,"Uncertain committed receipt absent");
        detail("DELTA",withNew(21,4));sql("UPDATE giftcode SET expired='2001-01-01' WHERE code='DELTA'");
        before=durable("DELTA");refused(dao,1,10,"DELTA");check(before.equals(durable("DELTA")),"Expired supplement changed durable state");
        sql("UPDATE giftcode SET expired='2037-12-31',count_left=7 WHERE code='DELTA'");String refillToken=token();
        check(claim(dao,"DELTA",refillToken,Map.of(457,1)).remaining()==7,"Supplement consumed administrator refill");
        check(dao.reconcile(1,10,"DELTA",first).remaining()==0&&dao.reconcile(1,10,"DELTA",refillToken).remaining()==7,"Reconciliation returned latest receipt for an older token");
        refused(dao,2,20,"RECREATED_UNKNOWN");
        String legacyToken=value("SELECT request_id FROM giftcode_claim WHERE canonical_code='LEGACY' AND account_id=20");
        check(dao.reconcile(2,20,"LEGACY",legacyToken)!=null,"Migrated legacy token not journaled");
        dao.claim(2,20,"LEGACY",token(),stage(2,20,Map.of(457,5),new AtomicInteger()));
        check(number("SELECT count_left FROM giftcode WHERE code='LEGACY'")==5,"Legacy supplement consumes quota");
        check(dao.reconcile(2,20,"LEGACY",legacyToken)!=null,"Migrated receipt lost after later claim");
        catalog("FIRSTROLLBACK",3,rewards(10));
        try {claim(broken,"FIRSTROLLBACK",token(),Map.of(457,10));throw new AssertionError("First claim SQL failure accepted");}catch(SQLException expected) { }
        check(number("SELECT count_left FROM giftcode WHERE code='FIRSTROLLBACK'")==3&&number("SELECT COUNT(*) FROM giftcode_claim WHERE canonical_code='FIRSTROLLBACK'")==0&&number("SELECT COUNT(*) FROM giftcode_claim_request WHERE canonical_code='FIRSTROLLBACK'")==0,"First rollback consumed quota/claim/receipt");
        System.out.println("GiftCodeSupplementDatabaseTest PASS: delta/quota/watermark/options/races/rollback/receipts/recovery");
    }
    public static void main(String[] args) throws Exception {
        check(args.length>0&&args[0].matches("codex_gift_test_[A-Za-z0-9_]+"),"Unsafe test database name");
        Class.forName("com.mysql.jdbc.Driver");url="jdbc:mysql://localhost:3306/"+args[0]+"?useSSL=false&serverTimezone=UTC";
        tests(args.length>1&&args[1].equals("--red"));
    }
}
