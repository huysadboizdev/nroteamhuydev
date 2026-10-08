package nro.models.services;

import java.lang.reflect.Field;
import java.nio.file.*;
import java.sql.SQLException;
import nro.models.boss.Boss;
import nro.models.boss.Boss_mini.Odo;
import nro.models.boss.notice.BossNoticeOutbox;
import nro.models.database.BossNoticeDAO;
import nro.models.player.NPoint;
import static nro.models.services.PinkFixture.*;

public final class PinkFixtureNoticeIsolationTest {
    static Object value(Object target,String name)throws Exception {Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    public static void main(String[] args)throws Exception {try {
        try {PinkFixture.class.getDeclaredMethod("isolateBossNotices");}
        catch(NoSuchMethodException missing){throw new AssertionError("fixture must isolate boss notice persistence before callbacks",missing);}
        install();BossNoticeService notices=BossNoticeService.gI();
        BossNoticeOutbox outbox=(BossNoticeOutbox)value(notices,"outbox");Path dir=(Path)value(outbox,"dir");
        check(Files.isDirectory(dir)&&dir.getFileName().toString().startsWith("pink-fixture-notices-"),"outbox uses a distinct temporary fixture directory");
        Boss boss=blank(Odo.class);boss.id=-999;boss.isBoss=true;boss.name="isolated fixture";boss.zone=zone();boss.zone.map.mapName="fixture";boss.nPoint=blank(NPoint.class);boss.nPoint.hp=1000;
        notices.appeared(boss);check(notices.rows(0).size()==1,"notice remains available in memory");
        check(notices.rows(0).get(0).bossName().equals("isolated fixture"),"memory row reflects real service callback");
        try(var files=Files.list(dir)){check(files.findAny().isEmpty(),"tracker noop sink never writes fixture row to outbox");}
        BossNoticeDAO dao=(BossNoticeDAO)value(notices,"dao");
        try {dao.save(notices.rows(0).get(0));throw new AssertionError("fixture DAO must reject every SQL connection");}
        catch(SQLException isolated){check(isolated.getMessage().equals("Boss notices are isolated by PinkFixture"),"fixture blocks production connection before opening it");}
        notices.departed(boss);check(notices.rows(0).isEmpty()&&notices.rows(1).size()==1,"departures update only memory history");
        try(var files=Files.list(dir)){check(files.findAny().isEmpty(),"departures produce no durable fixture rows");}
        System.out.println("PASS PinkFixture notice isolation: memory-only callbacks, isolated empty outbox, production SQL rejected");System.exit(0);
    }catch(Throwable failed){failed.printStackTrace();System.exit(1);}}
}
