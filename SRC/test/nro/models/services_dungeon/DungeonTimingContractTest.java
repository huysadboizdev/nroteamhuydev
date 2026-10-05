package nro.models.services_dungeon;

import java.lang.reflect.Field;
import nro.models.clan.Clan;
import nro.models.map.phoban.*;

/** Runs real update methods without server threads or network fixtures. */
public final class DungeonTimingContractTest {
    private static int cases;
    private static void set(Object target, Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        cases++;
        System.out.println("PASS " + message);
    }
    public static void main(String[] args) throws Exception {
        long now = System.currentTimeMillis();
        BanDoKhoBau treasure = new BanDoKhoBau(0);
        treasure.isOpened = true;
        set(treasure, BanDoKhoBau.class, "lastTimeOpen", now - 1_740_000L);
        treasure.update();
        check(treasure.isOpened, "treasure keeps final minute grace period");
        SnakeWay snake = new SnakeWay(0);
        snake.isOpened = true;
        snake.endCDRD = true;
        set(snake, SnakeWay.class, "lastTimeOpen", now);
        snake.update();
        check(snake.isOpened, "snake victory starts grace period without immediate exit");
        DestronGas gas = new DestronGas(0);
        gas.isOpened = true;
        set(gas, DestronGas.class, "lastTimeOpen", now);
        set(gas, DestronGas.class, "callBoss", true);
        set(gas, DestronGas.class, "hatchiyatchDead", true);
        gas.update();
        check(gas.isOpened, "gas victory starts grace period without immediate exit");
        RedRibbonHQ ribbon = new RedRibbonHQ(0);
        Clan clan = new Clan();
        clan.doanhTrai = ribbon;
        set(ribbon, RedRibbonHQ.class, "clan", clan);
        set(ribbon, RedRibbonHQ.class, "lastTimeOpen", now - 1_900_000L);
        ribbon.isOpened = true;
        ribbon.isTimePicking = true;
        ribbon.lastTimePick = now;
        ribbon.update();
        check(ribbon.isOpened, "ribbon loot phase may continue after original thirty minutes");
        ribbon.lastTimePick = System.currentTimeMillis() - 300_100L;
        ribbon.update();
        check(!ribbon.isOpened && clan.doanhTrai == null,
                "ribbon closes after five minute loot phase and releases clan instance");
        System.out.println("DUNGEON_TIMING PASS " + cases + " cases");
    }
}
