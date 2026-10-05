package nro.models.combine;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.player.IDMark;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.InventoryService;
import nro.models.services.Service;
import nro.models.utils.Util;

public final class CrystalizationSingleStarTest {
    public static void main(String[] args) throws Exception {
        new Npc(-1, 1, 0, 0, ConstNpc.BA_HAT_MIT, 0) {
            @Override public void confirmMenu(Player p, int selection) { }
            @Override public void createOtherMenu(Player p, int index, String text, String... choices) { }
            @Override public void npcChat(Player p, String text) { }
        };
        replace(Service.class, "instance", new Service() {
            @Override public void sendMoney(Player p) { }
            @Override public void sendThongBao(Player p, String text) { }
        });
        replace(InventoryService.class, "I", new InventoryService() {
            @Override public void sendItemBags(Player p) { }
        });
        for (int id = 0; id <= 228; id++)
            Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id, "Option #", 0));
        for (int count : new int[]{1, 10, 100}) {
            Player p = player(0);
            PhaLeHoaTrangBi.phaLeHoa(p, count);
            check(stars(p) == 1, "Batch " + count + " must add exactly one star");
            paid(p, 0, 1);
            PhaLeHoaTrangBi.phaLeHoa(p, count);
            check(stars(p) == 1, "Repeated request during cooldown must not upgrade again");
        }
        // Seed with three failures before success, at the 5% final-star rate.
        long seed = 0;
        while (firstSuccess(seed, 10) != 4) seed++;
        setSeed(seed);
        Player p = player(8);
        PhaLeHoaTrangBi.phaLeHoa(p, 100);
        check(stars(p) == 9, "Failures then success must add only the ninth star");
        paid(p, 8, 4);
        seed = 0;
        while (firstSuccess(seed, 100) != -1) seed++;
        setSeed(seed);
        Player failed = player(8);
        // Gold permits only two attempts even when the button requests 100.
        failed.inventory.gold = (long) CombineSystem.getGoldPhaLeHoa(8) * 2;
        PhaLeHoaTrangBi.phaLeHoa(failed, 100);
        check(stars(failed) == 8 && failed.inventory.gold == 0
                && failed.inventory.gem == 1000 - CombineSystem.getGemPhaLeHoa(8) * 2,
                "Failed batch must stop when funds run out without changing stars or going negative");
        for (int count : new int[]{0, -1, 101}) {
            Player invalidCount = player(0);
            PhaLeHoaTrangBi.phaLeHoa(invalidCount, count);
            check(stars(invalidCount) == 0, "Invalid batch count must not upgrade");
            paid(invalidCount, 0, 0);
        }
        for (int invalid : new int[]{-1, 9, 10}) {
            Player rejected = player(invalid);
            PhaLeHoaTrangBi.phaLeHoa(rejected, 100);
            check(stars(rejected) == invalid, "Invalid/max stars must stay unchanged");
            paid(rejected, 0, 0);
        }
        Player multiple = player(0);
        multiple.combineNew.itemsCombine.add(multiple.combineNew.itemsCombine.get(0));
        PhaLeHoaTrangBi.phaLeHoa(multiple, 100);
        check(stars(multiple) == 0, "More than one selected item must be rejected");
        paid(multiple, 0, 0);
        Player poor = player(0);
        poor.inventory.gold = CombineSystem.getGoldPhaLeHoa(0) - 1;
        PhaLeHoaTrangBi.phaLeHoa(poor, 100);
        check(stars(poor) == 0 && poor.inventory.gem == 1000, "Insufficient funds must not upgrade or charge");
        checkConcurrentRequests();
        System.out.println("PASS: batches 1/10/100 add one star, first-success stop, actual costs, simultaneous requests, cooldown, invalid selections and 9-star cap");
    }

    private static final long INITIAL_GOLD = 5_000_000_000L;
    private static Player player(int star) {
        Player p = new Player() {
            @Override public void sendMessage(Message message) { }
        };
        p.inventory.gold = INITIAL_GOLD;
        p.inventory.gem = 1000;
        Item item = new Item();
        item.template = new Template.ItemTemplate();
        item.template.type = 0;
        item.template.name = "Test armor";
        item.quantity = 1;
        if (star != 0) item.itemOptions.add(new Item.ItemOption(107, star));
        p.combineNew.itemsCombine.add(item);
        p.inventory.itemsBag.add(item);
        // Deliberately leave cached costs at zero: execution must derive actual costs.
        return p;
    }
    private static int stars(Player p) {
        return p.combineNew.itemsCombine.get(0).itemOptions.stream()
            .filter(o -> o.optionTemplate.id == 107).mapToInt(o -> o.param).findFirst().orElse(0);
    }
    private static void paid(Player p, int star, int attempts) {
        check(p.inventory.gold == INITIAL_GOLD - (long) CombineSystem.getGoldPhaLeHoa(star) * attempts,
            "Must charge gold for every attempted roll using the actual star count");
        check(p.inventory.gem == 1000 - CombineSystem.getGemPhaLeHoa(star) * attempts,
            "Must charge gems for every attempted roll");
    }
    private static int firstSuccess(long seed, int limit) {
        Random r = new Random(seed);
        for (int i = 1; i <= limit; i++) if (r.nextLong(100_000L) < 5000) return i;
        return -1;
    }
    private static void checkConcurrentRequests() throws Exception {
        Player p = player(0);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch secondFinished = new CountDownLatch(1);
        AtomicBoolean first = new AtomicBoolean(true);
        AtomicReference<Throwable> error = new AtomicReference<>();
        p.idMark = new IDMark() {
            @Override public void setLastTimeCombine(long time) {
                if (first.compareAndSet(true, false)) {
                    entered.countDown();
                    try {
                        if (!release.await(5, TimeUnit.SECONDS)) throw new AssertionError("Release timed out");
                    } catch (InterruptedException e) { throw new AssertionError(e); }
                }
                super.setLastTimeCombine(time);
            }
        };
        setSeed(0);
        Runnable action = () -> {
            try { PhaLeHoaTrangBi.phaLeHoa(p, 100); }
            catch (Throwable t) { error.compareAndSet(null, t); }
        };
        Thread a = new Thread(action);
        Thread b = new Thread(() -> { try { action.run(); } finally { secondFinished.countDown(); } });
        a.start();
        try {
            check(entered.await(5, TimeUnit.SECONDS), "First request must reach cooldown update");
            b.start();
            secondFinished.await(200, TimeUnit.MILLISECONDS);
        } finally { release.countDown(); }
        a.join(5000);
        b.join(5000);
        check(!a.isAlive() && !b.isAlive() && error.get() == null, "Concurrent requests must complete safely");
        check(stars(p) == 1, "Concurrent duplicate requests must not add multiple stars");
        paid(p, 0, 1);
    }
    private static void setSeed(long seed) throws Exception {
        Field f = Util.class.getDeclaredField("rand");
        f.setAccessible(true);
        ((Random) f.get(null)).setSeed(seed);
    }
    private static void replace(Class<?> type, String name, Object value) throws Exception {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }
    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
