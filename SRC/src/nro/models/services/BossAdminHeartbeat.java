package nro.models.services;
import java.sql.*;
import java.util.*;
import java.util.logging.*;
import nro.models.boss.Boss;
import nro.models.server.Manager;
import nro.models.data.LocalManager;
/** Read-only world census off combat threads; rows expire in the admin UI. */
public final class BossAdminHeartbeat {
 public record Row(String key,int phase,int aliveCount,long appliedRevision,int currentHp,int maxHp,int damage){}
 private static final String RUN=UUID.randomUUID().toString();
 private static long last;
 private BossAdminHeartbeat(){}
 public static List<Row> collect(Collection<Boss> bosses){
  Set<Boss> seen=Collections.newSetFromMap(new IdentityHashMap<>());Map<String,Row> rows=new TreeMap<>();
  for(Boss b:bosses){if(b==null||!seen.add(b)||b.zone==null||b.nPoint==null||b.nPoint.hp<=0)continue;
   String key=BossAdminConfig.keyFor(b),group=key+"\u0000"+b.currentLevel;Row prior=rows.get(group);
   rows.put(group,new Row(key,b.currentLevel,prior==null?1:prior.aliveCount+1,prior==null?b.adminAppliedRevision:Math.min(prior.appliedRevision,b.adminAppliedRevision),b.nPoint.hp,b.nPoint.hpMax,b.nPoint.dame));
  }return List.copyOf(rows.values());
 }
 public static synchronized void tick(){
  long now=System.currentTimeMillis();if(now-last<5000)return;last=now;
  try{
   List<Boss> bosses=new ArrayList<>();
   for(var map:Manager.MAPS)for(var zone:map.zones)for(var player:zone.getBosses())if(player instanceof Boss b)bosses.add(b);
   List<Row> rows=collect(bosses);
   try(Connection c=LocalManager.getConnection()){
    c.setAutoCommit(false);
    try(Statement s=c.createStatement()){s.executeUpdate("DELETE FROM boss_admin_runtime");}
    try(PreparedStatement q=c.prepareStatement("INSERT INTO boss_admin_runtime(boss_key,phase,alive_count,applied_revision,server_run,current_hp,max_hp,damage) VALUES(?,?,?,?,?,?,?,?)")){
     for(Row r:rows){q.setString(1,r.key);q.setInt(2,r.phase);q.setInt(3,r.aliveCount);q.setLong(4,r.appliedRevision);q.setString(5,RUN);q.setInt(6,r.currentHp);q.setInt(7,r.maxHp);q.setInt(8,r.damage);q.addBatch();}q.executeBatch();
    }c.commit();
   }
  }catch(Exception e){Logger.getLogger(BossAdminHeartbeat.class.getName()).log(Level.WARNING,"Boss admin census unavailable",e);}
 }
}
