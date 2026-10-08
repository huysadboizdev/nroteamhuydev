package nro.models.boss;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import nro.models.boss.Boss_mini.SoiHecQuyn;
import nro.models.player.Location;
import static nro.models.services.PinkFixture.*;

/** Feeding holds the owner before the wolf; direct reward must follow that order. */
public final class MiniWolfRewardLockTest {
    public static void main(String[] args) throws Exception {
        try {
            install();
            SoiHecQuyn wolf = blank(SoiHecQuyn.class);
            wolf.zone = zone(); wolf.location = blank(Location.class);
            wolf.location.x = 100; wolf.location.y = 200;
            var owner = player(777);
            CountDownLatch ownerHeld = new CountDownLatch(1), acquireWolf = new CountDownLatch(1);
            AtomicReference<Throwable> error = new AtomicReference<>();
            Thread feeding = new Thread(() -> {
                try {
                    synchronized(owner) {
                        ownerHeld.countDown(); acquireWolf.await();
                        synchronized(wolf) { check(wolf.zone != null, "feeding acquires wolf after owner"); }
                    }
                } catch(Throwable t) { error.set(t); }
            });
            Thread reward = new Thread(() -> {
                try { wolf.reward(owner); } catch(Throwable t) { error.set(t); }
            });
            feeding.setDaemon(true); reward.setDaemon(true); feeding.start();
            check(ownerHeld.await(2,TimeUnit.SECONDS), "feeding holds owner monitor"); reward.start();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (reward.getState() != Thread.State.BLOCKED && reward.isAlive() && System.nanoTime() < deadline) Thread.yield();
            check(reward.getState() == Thread.State.BLOCKED, "reward waits for the held owner");
            acquireWolf.countDown(); feeding.join(2000); reward.join(2000);
            check(!feeding.isAlive() && !reward.isAlive(), "direct reward and bone feeding must not deadlock");
            check(error.get() == null && owner.event.getEventPoint() == 5, "reward finishes once");
            System.out.println("PASS MiniWolfRewardLock"); System.exit(0);
        } catch(Throwable failure) { failure.printStackTrace(); System.exit(1); }
    }
}
