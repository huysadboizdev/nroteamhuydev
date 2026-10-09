package nro.models.services;
import java.sql.*;
import java.util.concurrent.*;
import com.google.gson.*;
import nro.models.database.AchievementRewardDAO;
public class AchievementSqlTest {
    static final String DB="codex_achievement_20261009";
    static Connection connection() throws SQLException {return DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/"+DB,"root","");}
    static void check(boolean yes,String text){if(!yes)throw new AssertionError(text);}
    static String rows(int claimed){JsonArray rows=new JsonArray();for(int i=0;i<20;i++){JsonArray r=new JsonArray();r.add(10);r.add(i==claimed);rows.add(r.toString());}return rows.toString();}
    public static void main(String[] args) throws Exception {
        Class.forName("com.mysql.jdbc.Driver");
        try(Connection admin=DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/","root","");Statement s=admin.createStatement()) {
            s.executeUpdate("CREATE DATABASE "+DB);
        }
        try {
            try(Connection c=connection();Statement s=c.createStatement()){
                s.executeUpdate("CREATE TABLE player(id BIGINT PRIMARY KEY,account_id INT,data_inventory LONGTEXT,data_achievement LONGTEXT) ENGINE=InnoDB");
                try(PreparedStatement p=c.prepareStatement("INSERT INTO player VALUES(1,2,?,?)")){p.setString(1,"[50,60,0,0,0,777]");p.setString(2,rows(3));p.executeUpdate();}
            }
            var dao=new AchievementRewardDAO(AchievementSqlTest::connection);
            var snapshot=new AchievementRewardDAO.Snapshot(1,2,4,"[50,60,1000,0,0]",rows(4));
            ExecutorService pool=Executors.newFixedThreadPool(8);java.util.List<Future<Boolean>> calls=new java.util.ArrayList<>();
            for(int i=0;i<40;i++)calls.add(pool.submit(()->dao.claim(snapshot).credited()));
            int paid=0;for(var f:calls)if(f.get())paid++;pool.shutdown();check(paid==1,"one transaction credits across concurrent callers");
            try(Connection c=connection();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT data_inventory,data_achievement FROM player")){
                r.next();JsonArray wallet=new JsonParser().parse(r.getString(1)).getAsJsonArray();check(wallet.get(2).getAsInt()==1000&&wallet.get(1).getAsInt()==60&&wallet.get(5).getAsInt()==777,"ruby atomic, gems and extra wallet fields preserved");
                JsonArray saved=new JsonParser().parse(r.getString(2)).getAsJsonArray();for(int i:new int[]{3,4})check(new JsonParser().parse(saved.get(i).getAsString()).getAsJsonArray().get(1).getAsBoolean(),"other durable claim preserved");
            }
            try{dao.claim(new AchievementRewardDAO.Snapshot(1,9,5,"[50,60,2000,0,0]",rows(5)));throw new AssertionError("wrong account accepted");}catch(SQLException expected){}
            try{dao.claim(new AchievementRewardDAO.Snapshot(1,2,5,"[]",rows(5)));throw new AssertionError("invalid snapshot accepted");}catch(SQLException expected){}
            check(!dao.claim(snapshot).credited(),"retry never credits twice");
            try(Connection c=connection();PreparedStatement p=c.prepareStatement("UPDATE player SET data_achievement=? WHERE id=1")) {
                JsonArray legacy=new JsonParser().parse(rows(4)).getAsJsonArray();legacy.set(0,new JsonPrimitive("broken"));legacy.set(1,new JsonPrimitive("[\"invalid\",true]"));p.setString(1,legacy.toString());p.executeUpdate();
            }
            check(dao.claim(new AchievementRewardDAO.Snapshot(1,2,5,"[50,60,2000,0,0]",rows(5))).credited(),"malformed legacy counter does not block other missions");
            check(!dao.claim(new AchievementRewardDAO.Snapshot(1,2,1,"[50,60,3000,0,0]",rows(1))).credited(),"parseable claimed marker survives malformed counter");
            System.out.println("ACHIEVEMENT_SQL PASS atomic/concurrency/idempotence/identity/rollback/preserved-wallet-and-claims");
        } finally {try(Connection c=DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/","root","");Statement s=c.createStatement()){s.executeUpdate("DROP DATABASE "+DB);}}
        System.exit(0);
    }
}
