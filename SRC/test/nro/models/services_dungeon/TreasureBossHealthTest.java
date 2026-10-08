package nro.models.services_dungeon;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import nro.models.boss.Boss;
import nro.models.clan.Clan;
import nro.models.map.phoban.BanDoKhoBau;
import nro.models.services.PinkFixture;

public final class TreasureBossHealthTest {
    public static void main(String[] args) {
        try {
            PinkFixture.install();
            Method init = BanDoKhoBau.class.getDeclaredMethod("init");
            init.setAccessible(true);
            Field field = BanDoKhoBau.class.getDeclaredField("boss");
            field.setAccessible(true);
            for (int level : new int[]{1, 100, 107, 108, 109, 110}) {
                var dungeon = new BanDoKhoBau(0);
                dungeon.level = (byte) level;
                dungeon.clan = new Clan();
                var zone = PinkFixture.zone();
                zone.map.mapId = 137;
                dungeon.zones.add(zone);
                init.invoke(dungeon);
                Boss boss = (Boss) field.get(dungeon);
                PinkFixture.check(boss != null, "boss initialized at level " + level);
                int hp = boss.data[0].getHp()[0];
                int expected = level == 1 ? 20_000_000 : 2_000_000_000;
                PinkFixture.check(hp == expected, "level " + level + " boss HP expected=" + expected + " actual=" + hp);
                System.out.println("PASS treasure boss level " + level + " hp=" + hp);
            }
            System.out.println("TREASURE_BOSS_HEALTH PASS 6 levels");
            System.exit(0);
        } catch (Throwable e) { e.printStackTrace(); System.exit(1); }
    }
}
