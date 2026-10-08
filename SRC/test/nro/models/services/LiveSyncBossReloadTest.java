package nro.models.services;
import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import nro.models.boss.*;
public final class LiveSyncBossReloadTest {
    static Connection fixture(List<Map<String,Object>> rows) {
        return (Connection)Proxy.newProxyInstance(LiveSyncBossReloadTest.class.getClassLoader(),new Class[]{Connection.class},(o,m,a)->{
            if(!m.getName().equals("prepareStatement"))return null;
            return Proxy.newProxyInstance(LiveSyncBossReloadTest.class.getClassLoader(),new Class[]{PreparedStatement.class},(p,pm,pa)->{
                if(!pm.getName().equals("executeQuery"))return null;
                int[] index={-1};boolean[] nil={false};
                return Proxy.newProxyInstance(LiveSyncBossReloadTest.class.getClassLoader(),new Class[]{ResultSet.class},(r,rm,ra)->{
                    if(rm.getName().equals("next"))return ++index[0]<rows.size();
                    if(rm.getName().equals("wasNull"))return nil[0];
                    if(rm.getName().equals("getInt")){Object v=rows.get(index[0]).get(ra[0]);nil[0]=v==null;return v==null?0:((Number)v).intValue();}
                    return null;
                });
            });
        });
    }
    public static void main(String[] args)throws Exception {
        Class<?> c=Class.forName("nro.models.services.BossLiveReload");
        var stage=c.getMethod("stage",Connection.class);var apply=c.getMethod("applyAtSpawn",Boss.class);
        PinkFixture.install();
        Boss boss=(Boss)PinkFixture.U.allocateInstance(Boss.class);boss.id=BossID.FIDE;boss.currentLevel=0;boss.nPoint=new nro.models.player.NPoint(boss);boss.nPoint.hpg=100;boss.nPoint.hp=50;boss.nPoint.dameg=10;
        Runnable op=(Runnable)stage.invoke(null,fixture(List.of(Map.of("boss_id",BossID.FIDE,"phase",0,"hp",500,"damage",30))));
        if(boss.nPoint.hp!=50)throw new AssertionError("staging touched alive boss");op.run();
        if(boss.nPoint.hp!=50)throw new AssertionError("reload touched alive boss");
        apply.invoke(null,boss);if(boss.nPoint.hpg!=500||boss.nPoint.hp!=500||boss.nPoint.dameg!=30)throw new AssertionError("next spawn overrides missing");
        boss.currentLevel=1;boss.nPoint.hpg=200;boss.nPoint.hp=200;apply.invoke(null,boss);if(boss.nPoint.hp!=200)throw new AssertionError("phase leak");
        try{stage.invoke(null,fixture(List.of(Map.of("boss_id",BossID.FIDE,"phase",0,"hp",0,"damage",30))));throw new AssertionError("bad HP accepted");}catch(InvocationTargetException expected){if(!(expected.getCause() instanceof IllegalArgumentException))throw expected;}
        boss.currentLevel=0;apply.invoke(null,boss);if(boss.nPoint.hp!=500)throw new AssertionError("invalid snapshot replaced cache");
        ((Runnable)stage.invoke(null,fixture(List.of()))).run();boss.nPoint.hpg=100;boss.nPoint.hp=100;apply.invoke(null,boss);if(boss.nPoint.hp!=100)throw new AssertionError("deleted override retained");
        System.out.println("Boss reload stages atomically, applies next spawn/phase only, retains defaults PASS");
    }
}
