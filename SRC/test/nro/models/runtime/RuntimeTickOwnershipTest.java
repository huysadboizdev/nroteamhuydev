package nro.models.runtime;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import nro.models.services.ServerMaintenanceGate;
import nro.models.network.MySession;
import nro.models.map.Map;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.map.phoban.*;
import sun.misc.Unsafe;

/** Standalone behavioral regressions; no server, database or connected clients. */
public final class RuntimeTickOwnershipTest {
    static Unsafe unsafe() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        return (Unsafe)f.get(null);
    }
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    static final class CountPlayer extends Player {
        volatile int ticks;
        CountPlayer() { super("fixture"); }
        @Override public void update() { ticks++; }
    }
    static void ownership() throws Exception {
        Map map = (Map)unsafe().allocateInstance(Map.class);
        Zone zone = new Zone(map, 0, 10);
        CountPlayer real = new CountPlayer(); real.isPlayer = true;
        CountPlayer passive = new CountPlayer();
        CountPlayer bot = new CountPlayer(); bot.isBot = true;
        CountPlayer pet = new CountPlayer(); pet.isPet = true;
        CountPlayer boss = new CountPlayer(); boss.isBoss = true;
        zone.addPlayer(real); zone.addPlayer(passive); zone.addPlayer(bot);
        zone.addPlayer(pet); zone.addPlayer(boss);
        zone.update();
        check(real.ticks == 0, "zone must not tick real players owned by Player.start");
        check(passive.ticks == 1, "legacy passive entities retain their zone tick");
        check(bot.ticks == 0 && pet.ticks == 0 && boss.ticks == 0, "bot/pet/boss owners remain unchanged");
        ServerMaintenanceGate.pause();
        try { zone.update(); check(passive.ticks == 1, "maintenance suspends zone updates"); }
        finally { ServerMaintenanceGate.resume(); }
        zone.update(); check(passive.ticks == 2, "zone resumes after web maintenance");
    }
    static class FixtureSession extends MySession {
        volatile boolean connected;
        FixtureSession() { super(null); }
        @Override public boolean isConnected() { return connected; }
    }
    static void playerStart() throws Exception {
        FixtureSession session = (FixtureSession)unsafe().allocateInstance(FixtureSession.class);
        session.connected=true;
        CountPlayer player = new CountPlayer(); player.session=session;
        for(int i=0;i<20;i++)player.start();
        long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
        while(player.ticks==0 && System.nanoTime()<until)Thread.sleep(5);
        check(player.ticks==1,"repeated Player.start must have one tick owner");
        ServerMaintenanceGate.pause();
        try { Thread.sleep(1100); check(player.ticks==1,"player worker survives pause without updating"); }
        finally { ServerMaintenanceGate.resume(); }
        until=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
        while(player.ticks==1 && System.nanoTime()<until)Thread.sleep(5);
        check(player.ticks==2,"player worker resumes after web maintenance");
        session.connected=false;
        until=System.nanoTime()+TimeUnit.SECONDS.toNanos(2);
        while(Thread.getAllStackTraces().keySet().stream().anyMatch(t->t.getName().equals("Update player fixture")) && System.nanoTime()<until)Thread.sleep(5);
        check(Thread.getAllStackTraces().keySet().stream().noneMatch(t->t.getName().equals("Update player fixture")),"disconnect ends player worker without parked executor");
    }
    static void mapIsolation() throws Exception {
        Map map=(Map)unsafe().allocateInstance(Map.class);
        AtomicInteger ticks=new AtomicInteger();
        map.zones=new java.util.ArrayList<>();
        map.zones.add(new Zone(map,0,10){@Override public void update(){throw new IllegalStateException("expected failed zone");}});
        map.zones.add(new Zone(map,1,10){@Override public void update(){ticks.incrementAndGet();}});
        var update=Map.class.getDeclaredMethod("updateZones");update.setAccessible(true);update.invoke(map);
        check(ticks.get()==1,"failed zone must not skip later zones");
    }
    static void dungeonPause() throws Exception {
        CountDownLatch tick=new CountDownLatch(1);
        SnakeWay dungeon=new SnakeWay(0){@Override public void update(){tick.countDown();}};
        dungeon.isOpened=true;
        ServerMaintenanceGate.pause();
        Thread thread=new Thread(dungeon,"pause-fixture");thread.start();
        try {
            check(!tick.await(220,TimeUnit.MILLISECONDS),"paused dungeon must not update");
            check(thread.isAlive(),"maintenance must keep dungeon loop alive");
            ServerMaintenanceGate.resume();
            check(tick.await(1,TimeUnit.SECONDS),"dungeon must resume after maintenance");
        } finally { dungeon.isOpened=false; ServerMaintenanceGate.resume();thread.join(2000); }
        check(!thread.isAlive(),"closed dungeon worker ends");
    }
    static void errorDelay(String name, Runnable loop, AtomicInteger ticks) {
        long before = System.nanoTime(); loop.run();
        long elapsed = (System.nanoTime() - before) / 1_000_000;
        check(ticks.get() == 3, name + " keeps running after a tick exception");
        check(elapsed >= 350, name + " must sleep on failed ticks; took " + elapsed + "ms");
    }
    static void dungeonDelay() {
        AtomicInteger a = new AtomicInteger();
        BanDoKhoBau t = new BanDoKhoBau(0) { @Override public void update() { if(a.incrementAndGet()==3)isOpened=false; throw new IllegalStateException("expected fixture"); }};
        t.isOpened=true; errorDelay("treasure", t, a);
        AtomicInteger b = new AtomicInteger();
        SnakeWay s = new SnakeWay(0) { @Override public void update() { if(b.incrementAndGet()==3)isOpened=false; throw new IllegalStateException("expected fixture"); }};
        s.isOpened=true; errorDelay("snake", s, b);
        AtomicInteger c = new AtomicInteger();
        DestronGas d = new DestronGas(0) { @Override public void update() { if(c.incrementAndGet()==3)isOpened=false; throw new IllegalStateException("expected fixture"); }};
        d.isOpened=true; errorDelay("gas", d, c);
        AtomicInteger e = new AtomicInteger();
        RedRibbonHQ r = new RedRibbonHQ(0) { @Override public void update() { if(e.incrementAndGet()==3)isOpened=false; throw new IllegalStateException("expected fixture"); }};
        r.isOpened=true; errorDelay("ribbon", r, e);
    }
    public static void main(String[] args) throws Exception {
        if(args.length == 0 || args[0].equals("ownership")) ownership();
        if(args.length == 0 || args[0].equals("delay")) dungeonDelay();
        if(args.length == 0) { playerStart(); mapIsolation(); dungeonPause(); }
        System.out.println("RUNTIME_TICK_OWNERSHIP PASS");
    }
}
