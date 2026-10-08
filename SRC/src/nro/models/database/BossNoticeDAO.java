package nro.models.database;
import java.sql.*;
import java.util.*;
import nro.models.data.LocalManager;
import nro.models.boss.notice.BossNoticeRow;

public final class BossNoticeDAO {
    @FunctionalInterface public interface Connections{Connection open()throws SQLException;}
    private final Connections connections;
    public BossNoticeDAO(){this(LocalManager::getConnection);}
    public BossNoticeDAO(Connections c){connections=c;}
    public void save(BossNoticeRow r)throws SQLException{
        String[] cols={"server_run","boss_id","form_id","boss_name","map_name","appeared_at","defeated_at","killer_name","status","next_spawn_at","eta_kind","updated_at","category","semantic_boss_id","quest_support","zone_id","schema_version"};
        StringJoiner changes=new StringJoiner(",");for(String col:cols)changes.add(col+"=IF(VALUES(version)>version,VALUES("+col+"),"+col+")");changes.add("version=GREATEST(version,VALUES(version))");
        String sql="INSERT INTO boss_notice_history(spawn_id,server_run,boss_id,form_id,boss_name,map_name,appeared_at,defeated_at,killer_name,status,next_spawn_at,eta_kind,version,updated_at,category,semantic_boss_id,quest_support,zone_id,schema_version) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE "+changes;
        try(Connection c=connections.open();PreparedStatement q=c.prepareStatement(sql)){
            q.setString(1,r.spawnId());q.setString(2,r.serverRun());q.setLong(3,r.bossId());q.setInt(4,r.form());q.setString(5,r.bossName());q.setString(6,r.mapName());q.setLong(7,r.appearedAt());q.setLong(8,r.defeatedAt());q.setString(9,r.killerName());q.setString(10,r.status());q.setLong(11,r.nextSpawnAt());q.setString(12,r.etaKind());q.setLong(13,r.version());q.setLong(14,r.updatedAt());q.setInt(15,r.category());q.setInt(16,r.semanticBossId());q.setBoolean(17,r.questSupport());q.setInt(18,r.zoneId());q.setInt(19,r.schemaVersion());q.executeUpdate();
        }
    }
    public List<BossNoticeRow> history(long since)throws SQLException{
        List<BossNoticeRow> out=new ArrayList<>();
        try(Connection c=connections.open();PreparedStatement q=c.prepareStatement("SELECT * FROM boss_notice_history WHERE appeared_at>=? ORDER BY GREATEST(appeared_at,defeated_at) DESC,spawn_id DESC LIMIT 10000")){
            q.setLong(1,since);try(ResultSet r=q.executeQuery()){while(r.next())out.add(new BossNoticeRow(r.getString("spawn_id"),r.getString("server_run"),r.getLong("boss_id"),r.getInt("form_id"),r.getString("boss_name"),r.getString("map_name"),r.getLong("appeared_at"),r.getLong("defeated_at"),r.getString("killer_name"),r.getString("status"),r.getLong("next_spawn_at"),r.getString("eta_kind"),r.getLong("version"),r.getLong("updated_at"),r.getInt("category"),r.getInt("semantic_boss_id"),r.getBoolean("quest_support"),r.getInt("zone_id"),r.getInt("schema_version")));}
        }return out;
    }
    public void prune(long since)throws SQLException{
        try(Connection c=connections.open();PreparedStatement q=c.prepareStatement("DELETE FROM boss_notice_history WHERE appeared_at<? LIMIT 1000")){q.setLong(1,since);q.executeUpdate();}
    }
}
