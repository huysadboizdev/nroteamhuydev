package nro.models.services_dungeon;

import java.lang.reflect.Field;
import nro.models.map.phoban.*;

/** Exercises the actual timeout tick, with deliberately unusable post-close data. */
public final class DungeonTimeoutRegressionTest {
    private static int failures;
    private static void expired(Object dungeon, Class<?> type) throws Exception {
        Field time = type.getDeclaredField("lastTimeOpen");
        time.setAccessible(true);
        time.setLong(dungeon, System.currentTimeMillis() - 1_800_001L);
    }
    private static void run(String name, Runnable test) {
        try { test.run(); System.out.println("PASS " + name); }
        catch (Throwable failure) { failures++; System.out.println("FAIL " + name + ": " + failure); }
    }
    private static void check(boolean condition) {
        if (!condition) throw new AssertionError("timeout did not finish exactly once");
    }
    public static void main(String[] args) throws Exception {
        BanDoKhoBau treasure = new BanDoKhoBau(0) {
            int closes;
            @Override public void finish() { closes++; }
            @Override public void dispose() { check(closes == 1); isOpened = false; }
        };
        treasure.isOpened = true; treasure.zones.add(null); expired(treasure, BanDoKhoBau.class);
        run("treasure stops tick after close", treasure::update);
        SnakeWay snake = new SnakeWay(0) {
            int closes;
            @Override public void finish() { closes++; }
            @Override public void dispose() { check(closes == 1); isOpened = false; }
        };
        snake.isOpened = true; snake.zones.add(null); expired(snake, SnakeWay.class);
        run("snake stops tick after close", snake::update);
        DestronGas gas = new DestronGas(0) {
            int closes;
            @Override public void finish() { closes++; }
            @Override public void dispose() { check(closes == 1); isOpened = false; }
        };
        gas.isOpened = true; gas.zones.add(null); expired(gas, DestronGas.class);
        run("gas stops tick after close", gas::update);
        // An empty instance can close during failed initialization, before it has a clan.
        RedRibbonHQ ribbon = new RedRibbonHQ(0);
        ribbon.isOpened = true; expired(ribbon, RedRibbonHQ.class);
        run("ribbon closes empty uninitialized instance safely", () -> {
            ribbon.update(); check(!ribbon.isOpened);
        });
        if (failures != 0) throw new AssertionError(failures + " dungeon timeout regressions");
        System.out.println("DUNGEON_TIMEOUT PASS 4 cases");
    }
}
