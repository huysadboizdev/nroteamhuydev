package nro.models.services;

import java.lang.reflect.Modifier;
import java.sql.*;
import java.util.*;
import nro.models.boss.Boss;
import nro.models.boss.BossID;

/** Sparse settings apply only when a boss initializes its next phase/spawn. */
public final class BossLiveReload {
    private BossLiveReload() {}
    public record Key(int bossId,int phase) {}
    public record Stats(Integer hp,Integer damage) {}
    private static volatile Map<Key,Stats> overrides=Map.of();
    private static final Set<Integer> IDS=knownIds();
    private static Set<Integer> knownIds() {
        Set<Integer> ids=new HashSet<>();
        for(var f:BossID.class.getFields())try {
            if(f.getType()==int.class&&Modifier.isStatic(f.getModifiers()))ids.add(f.getInt(null));
        }catch(IllegalAccessException e){throw new ExceptionInInitializerError(e);}
        return Set.copyOf(ids);
    }
    public static Runnable stage(Connection con)throws SQLException {
        Map<Key,Stats> next=new HashMap<>();
        try(PreparedStatement q=con.prepareStatement("SELECT boss_id,phase,hp,damage FROM boss_live_override ORDER BY boss_id,phase");ResultSet r=q.executeQuery()) {
            while(r.next()) {
                int id=r.getInt("boss_id"),phase=r.getInt("phase");
                Integer hp=nullable(r,"hp"),damage=nullable(r,"damage");
                if(!IDS.contains(id)||phase<0||phase>31||hp!=null&&hp<=0||damage!=null&&damage<0||hp==null&&damage==null)
                    throw new IllegalArgumentException("Invalid boss override: "+id+" phase "+phase);
                if(next.put(new Key(id,phase),new Stats(hp,damage))!=null)throw new IllegalArgumentException("Duplicate boss phase");
            }
        }
        Map<Key,Stats> snapshot=Map.copyOf(next);
        return ()->overrides=snapshot;
    }
    private static Integer nullable(ResultSet r,String column)throws SQLException {int v=r.getInt(column);return r.wasNull()?null:v;}
    public static void loadStartup(Connection con)throws SQLException {
        Runnable legacy=stage(con), admin=BossAdminConfig.stage(con);legacy.run();admin.run();
    }
    public static void applyAtSpawn(Boss boss) {
        if(BossAdminConfig.config(boss)!=null)BossAdminConfig.applyAtSpawn(boss);else applyLegacyAtSpawn(boss);
    }
    static void applyLegacyAtSpawn(Boss boss) {
        Stats s=overrides.get(new Key(boss.questBossId(),boss.currentLevel));
        if(s==null)return;
        if(s.hp()!=null){boss.nPoint.hpg=s.hp();boss.nPoint.hpMax=s.hp();boss.nPoint.hp=s.hp();}
        if(s.damage()!=null){boss.nPoint.dameg=s.damage();boss.nPoint.dame=s.damage();}
    }
}
