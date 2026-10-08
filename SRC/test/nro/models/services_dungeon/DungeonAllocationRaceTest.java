package nro.models.services_dungeon;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import nro.models.clan.Clan;
import nro.models.item.Item;
import nro.models.map.phoban.BanDoKhoBau;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.PinkFixture;

/** Runs without maps, database or server threads; only allocation uses real code. */
public final class DungeonAllocationRaceTest {
    static final class Instance extends BanDoKhoBau {
        final CountDownLatch captured = new CountDownLatch(2);
        final AtomicInteger openings = new AtomicInteger();
        Instance() { super(0); }
        @Override public boolean openBanDoKhoBau(Player player, Clan owner, byte level, java.util.function.BooleanSupplier charge) {
            openings.incrementAndGet();
            captured.countDown();
            try { captured.await(700, TimeUnit.MILLISECONDS); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
            clan = owner;
            owner.BanDoKhoBau = this;
            isOpened = true;
            isInitializing = false;
            PinkFixture.check(charge.getAsBoolean(), "successful initialized instance charges once");
            return true;
        }
    }
    public static void main(String[] args) {
        try {
            PinkFixture.install();
            PinkFixture.replace(InventoryService.class, "I", new InventoryService() {
                @Override public Item findItemBag(Player p, int id) { return PinkFixture.item(611, 1); }
                @Override public void subQuantityItemsBag(Player p, Item item, int count) { item.quantity -= count; }
                @Override public void sendItemBags(Player p) {}
            });
            var service = TreasureUnderSeaService.gI();
            var instance = new Instance();
            service.banDoKhoBaus = List.of(instance);
            Player a = PinkFixture.player(9001), b = PinkFixture.player(9002);
            a.clan = new Clan(); b.clan = new Clan();
            boolean sameClan = args.length > 0 && args[0].equals("same-clan");
            if (sameClan) b.clan = a.clan;
            CountDownLatch start = new CountDownLatch(1);
            AtomicReference<Throwable> error = new AtomicReference<>();
            Thread[] threads = new Thread[2];
            int i = 0;
            for (Player p : new Player[]{a, b}) {
                threads[i++] = new Thread(() -> {
                    try { start.await(); service.openBanDoKhoBau(p, (byte) 1); }
                    catch (Throwable e) { error.set(e); }
                });
            }
            for (Thread thread : threads) thread.start();
            start.countDown();
            for (Thread thread : threads) { thread.join(3000); PinkFixture.check(!thread.isAlive(), "allocation completes"); }
            if (error.get() != null) throw new AssertionError(error.get());
            int owners = (a.clan.BanDoKhoBau == instance ? 1 : 0) + (!sameClan && b.clan.BanDoKhoBau == instance ? 1 : 0);
            PinkFixture.check(owners == 1, "one dungeon instance must belong to one clan; actual owners=" + owners);
            PinkFixture.check(instance.openings.get() == 1, "one reservation starts initialization once");
            System.out.println("DUNGEON_ALLOCATION PASS " + (sameClan ? "same clan initializes once" : "different clans reserve exclusive instance"));
            System.exit(0);
        } catch (Throwable e) { e.printStackTrace(); System.exit(1); }
    }
}
