package nro.models.database;
import java.sql.*;
import java.nio.file.*;
import java.util.*;
import nro.models.boss.notice.*;
public final class BossNoticeDbTest {
    static Properties config=new Properties();static String schema;
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    static Connection open()throws SQLException{return connect(schema);}
    static Connection connect(String db)throws SQLException{return DriverManager.getConnection("jdbc:mysql://"+config.getProperty("database.host")+":"+config.getProperty("database.port")+"/"+db+"?useSSL=false&allowPublicKeyRetrieval=true",config.getProperty("database.user"),config.getProperty("database.pass"));}
    static BossNoticeRow row(String id,long version,String status,String killer,long death,long next){return new BossNoticeRow(id,"run",1,0,"Fide","Núi",1000,death,killer,status,next,"COOLDOWN",version,1000+version);}
    public static void main(String[] args)throws Exception{
        schema=args[0];check(schema.matches("boss_notice_verify_[0-9_]+"),"isolated catalog only");
        try(var in=Files.newInputStream(Path.of("Config.properties"))){config.load(in);}Class.forName(config.getProperty("database.driver"));
        try(Connection c=connect("");Statement q=c.createStatement()){q.execute("CREATE DATABASE "+schema);}
        Path spool=Path.of(args[1]);check(!Files.exists(spool),"new spool only");
        try{
            String migration=Files.readString(Path.of("sql/migrations/20261005_boss_notice_board.sql"))+Files.readString(Path.of("sql/migrations/20261008_boss_notice_categories.sql"));migration=migration.replaceAll("(?m)^--.*$","");
            try(Connection c=open();Statement q=c.createStatement()){
                for(int twice=0;twice<2;twice++)for(String s:migration.split(";"))if(!s.isBlank())q.execute(s);
                try(ResultSet r=q.executeQuery("SHOW COLUMNS FROM boss_notice_history")){while(r.next()){check(!r.getString(1).isEmpty(),"column present");}}
            }
            var dao=new BossNoticeDAO(BossNoticeDbTest::open);String id=UUID.randomUUID().toString();
            String questId=UUID.randomUUID().toString();
            var classified=new BossNoticeRow(questId,"run",-900000000,0,"Support","Map",1000,0,"","ALIVE",0,"UNKNOWN",2,1002,1,nro.models.boss.BossID.KUKU,true,7,2);
            dao.save(classified);var found=dao.history(0).stream().filter(r->r.spawnId().equals(questId)).findFirst().orElseThrow();
            check(found.category()==1&&found.semanticBossId()==nro.models.boss.BossID.KUKU&&found.questSupport()&&found.zoneId()==7&&found.schemaVersion()==2,"SQL classified support round trip");
            dao.save(new BossNoticeRow(questId,"run",-900000000,0,"Support","Map",1000,0,"","ALIVE",0,"UNKNOWN",1,1001));
            found=dao.history(0).stream().filter(r->r.spawnId().equals(questId)).findFirst().orElseThrow();check(found.category()==1&&found.zoneId()==7,"legacy stale replay cannot downgrade classification");
            try(Connection c=open();PreparedStatement q=c.prepareStatement("DELETE FROM boss_notice_history WHERE spawn_id=?")){q.setString(1,questId);q.executeUpdate();}
            dao.save(row(id,3,"DEFEATED","admin",2000,3000));dao.save(row(id,1,"ALIVE","",0,0));dao.save(row(id,3,"ALIVE","wrong",0,0));
            var got=dao.history(0).get(0);check(got.category()==BossNoticeCatalog.legacyCategory(1,0)&&got.zoneId()==-1,"legacy category and hidden unknown zone");check(got.version()==3&&got.killerName().equals("admin")&&got.status().equals("DEFEATED"),"stale/equal replay never overwrites kill");
            var box=new BossNoticeOutbox(spool);box.stage(row(id,4,"DEFEATED","admin",2000,4000));box.stage(row(id,4,"DEFEATED","admin",2000,4000));
            try{box.drain(new BossNoticeDAO(()->{throw new SQLException("simulated outage");}),64);throw new AssertionError("failure required");}catch(SQLException expected){}
            try(var files=Files.list(spool)){check(files.count()==1,"outage keeps durable snapshot, stage idempotent");}
            var recovered=new BossNoticeOutbox(spool);check(recovered.drain(dao,64)==1&&dao.history(0).get(0).nextSpawnAt()==4000,"restart recovers outbox");
            recovered.stage(row(id,2,"ALIVE","",0,0));recovered.drain(dao,64);check(dao.history(0).get(0).version()==4,"late file replay cannot downgrade");
            String unknownId=UUID.randomUUID().toString();var unknown=new BossNoticeRow(unknownId,"run",-900000999,0,"Kuku","Map",1000,0,"","ALIVE",0,"UNKNOWN",1,1001);
            recovered.stage(unknown);Path oldFile=spool.resolve(unknownId+"-1.json");var legacyJson=new com.google.gson.JsonParser().parse(Files.readString(oldFile)).getAsJsonObject();
            for(String field:new String[]{"category","semanticBossId","questSupport","zoneId","schemaVersion"})legacyJson.remove(field);Files.writeString(oldFile,legacyJson.toString());
            recovered.drain(dao,64);found=dao.history(0).stream().filter(r->r.spawnId().equals(unknownId)).findFirst().orElseThrow();check(found.category()==0&&found.zoneId()==-1&&found.schemaVersion()==1,"legacy outbox unknown semantic ID not guessed by name");
            dao.prune(1001);check(dao.history(0).isEmpty(),"retention cleanup");
            System.out.println("PASS BossNotice SQL/outbox: repeat migration, no zones, monotonic replay, SQL outage, restart recovery, retention");
        }finally{
            try(Connection c=connect("");Statement q=c.createStatement()){q.execute("DROP DATABASE "+schema);}
            if(Files.isDirectory(spool)){try(var files=Files.list(spool)){for(Path p:files.toList())Files.delete(p);}Files.delete(spool);}
        }
    }
}
