package nro.models.player;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.managers.GiftCodeManager;
import nro.models.map.service.NpcService;
import nro.models.network.Message;
import nro.models.player_system.GiftCode;
import nro.models.player_system.Template;
import nro.models.server.Maintenance;
import nro.models.server.Manager;
import nro.models.services.GiftCodeService;
import nro.models.services.InventoryService;
import nro.models.services.Service;
import nro.models.services.shenron.SummonDragon;
import nro.models.services_func.Input;
import nro.models.shop.Shop;
import nro.models.shop.ShopService;

/** Real balance/item/wish behavior; only database and network output are replaced. */
public final class GoldEconomyTest {
    private static final long CAP = 1_000_000_000_000L;
    private static final long PRICE = 500_000_000L;

    public static void main(String[] args) throws Exception {
        setup();
        switch (args[0]) {
            case "inventory": inventory(); break;
            case "bulk": bulk(); break;
            case "retail": retail(); break;
            case "wishes": wishes(); break;
            case "giftcode": giftcode(); break;
            case "pickup": pickup(); break;
            default: throw new AssertionError("Unknown test");
        }
        System.out.println("PASS: " + args[0]);
    }

    private static void setup() throws Exception {
        replace(Service.class, "instance", new Service() {
            @Override public void sendMoney(Player p) { }
            @Override public void sendThongBao(Player p, String text) { }
            @Override public void sendThongBaoOK(Player p, String text) { }
            @Override public void sendMessAllPlayer(Message msg) { }
        });
        replace(InventoryService.class, "I", new InventoryService() {
            @Override public void sendItemBags(Player p) { }
        });
        replace(NpcService.class, "i", new NpcService() {
            @Override public void createMenuRongThieng(Player p, int menu, String text, String... choices) { }
            @Override public void createTutorial(Player p, int avatar, String text) { }
        });
        for (int id = 0; id <= 73; id++) {
            Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id, "Option #", 0));
        }
        for (int id = 0; id <= 457; id++) {
            Template.ItemTemplate template = new Template.ItemTemplate();
            template.id = (short) id;
            template.type = 12;
            template.name = "Item " + id;
            template.isUpToUp = true;
            template.gold = 37_000_000; // Old DB price must not override the new sale price.
            Manager.ITEM_TEMPLATES.add(template);
        }
        Maintenance.isRunning = true; // Dragon updater exits immediately; no server startup.
    }

    private static Player player(long gold, int bars) {
        Player p = new Player();
        p.inventory.gold = gold;
        if (bars > 0) p.inventory.itemsBag.add(bar(bars));
        else p.inventory.itemsBag.add(new Item());
        p.idMark.setTypeInput(Input.BANSLL);
        p.idMark.setShopOpen(new Shop());
        p.idMark.setTagNameShop("TEST");
        return p;
    }

    private static Item bar(int quantity) {
        Item item = new Item();
        item.template = Manager.ITEM_TEMPLATES.get(457);
        item.quantity = quantity;
        item.itemOptions.add(new Item.ItemOption(73, 0));
        return item;
    }

    private static int bars(Player p) {
        return p.inventory.itemsBag.stream().filter(it -> it.isNotNullItem() && it.template.id == 457)
                .mapToInt(it -> it.quantity).sum();
    }

    private static void inventory() {
        check(Inventory.LIMIT_GOLD == CAP, "Gold cap must be 1000 billion");
        Inventory inv = new Inventory();
        inv.gold = 500_000_000_000L;
        inv.addGold(1_000_000);
        check(inv.gold == 500_001_000_000L, "High balance must not shrink on rewards");
        inv.gold = CAP - 5;
        inv.addGold(10);
        check(inv.gold == CAP, "Rewards must clamp to the cap");
        inv.gold = 0;
        inv.addGold(Long.MAX_VALUE);
        check(inv.gold == CAP, "A long reward must saturate without overflow");
        inv.addGold(-1);
        check(inv.gold == CAP, "Negative rewards must not subtract gold");
    }

    private static void input(Player p, String quantity) throws Exception {
        Message encoded = new Message(0);
        encoded.writer().writeByte(1);
        encoded.writer().writeUTF(quantity);
        Message request = new Message((byte) 0, encoded.getData());
        Input.gI().doInput(p, request);
        encoded.cleanup();
        request.cleanup();
    }

    private static void bulk() throws Exception {
        Player p = player(0, 2000);
        input(p, "2000");
        check(p.inventory.gold == CAP && bars(p) == 0, "2000 bars must sell for exactly 1000 billion");
        p = player(CAP - PRICE, 2);
        input(p, "2");
        check(p.inventory.gold == CAP - PRICE && bars(p) == 2, "Over-cap sale must preserve resources");
        input(p, "1");
        check(p.inventory.gold == CAP && bars(p) == 1, "Sale landing on cap must succeed");
        for (String invalid : new String[]{"0", "-1", "-2147483648", "2147483648", "abc", "3"}) {
            p = player(10_000_000_000L, 2);
            input(p, invalid);
            check(p.inventory.gold == 10_000_000_000L && bars(p) == 2,
                    "Invalid or insufficient quantity must not change resources: " + invalid);
        }
        p = player(0, 2);
        input(p, "2147483647");
        check(p.inventory.gold == 0 && bars(p) == 2, "Huge quantity must not overflow or consume bars");
        Player shared = player(0, 1);
        concurrently(() -> input(shared, "1"), () -> ShopService.gI().sellItem(shared, 1, 0));
        check(shared.inventory.gold == PRICE && bars(shared) == 0,
                "Concurrent retail and bulk sale must consume/pay for a bar only once");
    }

    private static void retail() {
        Player p = player(0, 6);
        ShopService.gI().sellItem(p, 1, 0);
        check(p.inventory.gold == PRICE && bars(p) == 5, "Single bar must sell for 500 million");
        p = player(CAP - PRICE + 1, 2);
        ShopService.gI().sellItem(p, 1, 0);
        check(p.inventory.gold == CAP - PRICE + 1 && bars(p) == 2, "Retail cap rejection must preserve bars");
        // A large ordinary stack must use long multiplication too.
        p = player(0, 6);
        p.inventory.itemsBag.get(0).template = Manager.ITEM_TEMPLATES.get(456);
        p.inventory.itemsBag.get(0).template.gold = 2_000_000_000;
        ShopService.gI().sellItem(p, 1, 0);
        check(p.inventory.gold == 3_000_000_000L, "Ordinary stack sale must not overflow int multiplication");
        p = player(0, 1);
        ShopService.gI().sellItem(p, 1, -1);
        ShopService.gI().sellItem(p, 1, 999);
        check(p.inventory.gold == 0 && bars(p) == 1, "Invalid item indexes must preserve resources");
    }

    private static SummonDragon dragon(Player p, int menu, byte choice) throws Exception {
        Constructor<SummonDragon> ctor = SummonDragon.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        SummonDragon dragon = ctor.newInstance();
        dragon.playerSummonShenron = p;
        set(dragon, "menuShenron", menu);
        set(dragon, "select", choice);
        return dragon;
    }

    private static void wishes() throws Exception {
        int[] menus = {ConstNpc.SHENRON_1_1, ConstNpc.SHENRON_2, ConstNpc.SHENRON_3};
        int[] rewards = {200, 150, 100};
        String[] labels = {SummonDragon.SHENRON_1_STAR_WISHES_1[0],
                SummonDragon.SHENRON_2_STARS_BlackGokuHES[2], SummonDragon.SHENRON_3_STARS_BlackGokuHES[2]};
        for (int i = 0; i < menus.length; i++) {
            Player p = player(500_000_000_000L, 0);
            SummonDragon d = dragon(p, menus[i], (byte) (i == 0 ? 0 : 2));
            d.confirmWish();
            check(bars(p) == rewards[i], "Dragon reward must be " + rewards[i] + " bars");
            check(p.inventory.gold == 500_000_000_000L, "Dragon must not overwrite or add bag gold");
            check(labels[i].contains("" + rewards[i]) && labels[i].contains("Thỏi"), "Menu must match reward");
            d.confirmWish();
            check(bars(p) == rewards[i], "Repeated confirmation must not duplicate rewards");
            p = player(1234, 0);
            p.inventory.itemsBag.clear();
            Item filler = bar(1);
            filler.template = Manager.ITEM_TEMPLATES.get(1);
            p.inventory.itemsBag.add(filler);
            d = dragon(p, menus[i], (byte) (i == 0 ? 0 : 2));
            d.confirmWish();
            check(d.playerSummonShenron == p && bars(p) == 0 && p.inventory.gold == 1234,
                    "Full bag must retain wish and resources");
            p.inventory.itemsBag.add(new Item());
            d.confirmWish();
            check(bars(p) == rewards[i], "Wish must succeed after freeing a slot");
            p = player(1234, 3); // Full bag with a matching stack can still receive the reward.
            d = dragon(p, menus[i], (byte) (i == 0 ? 0 : 2));
            d.confirmWish();
            check(bars(p) == rewards[i] + 3, "Dragon bars must stack without needing an empty slot");
            p = player(1234, 0);
            SummonDragon shared = dragon(p, menus[i], (byte) (i == 0 ? 0 : 2));
            concurrently(shared::confirmWish, shared::confirmWish);
            check(bars(p) == rewards[i], "Concurrent confirmations must award bars once");
        }
    }

    private static void giftcode() throws Exception {
        var dao = new nro.models.database.GiftCodeClaimDAO() {
            @Override public Receipt claim(long player, int account, String code, String token, Stage stage) throws Exception {
                stage.prepare(new Catalog(1, code, 10, "[{\"id\":-1,\"quantity\":1000000}]"));
                return new Receipt(1, 9);
            }
        };
        var service = new GiftCodeService(dao, ignored -> true);
        Player p = player(500_000_000_000L, 0);
        p.isPlayer = true;
        p.id = 99001;
        var unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        var unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        p.session = (nro.models.network.MySession) unsafe.allocateInstance(nro.models.network.MySession.class);
        p.session.userId = 99001;
        p.session.player = p;
        check(service.claim(p, "test"), "Giftcode must commit successfully");
        check(p.inventory.gold == 500_001_000_000L, "Giftcode must not reset a high balance to 2 billion");
        p.inventory.gold = CAP - 1;
        check(service.claim(p, "test_cap"), "Distinct giftcode must commit successfully");
        check(p.inventory.gold == CAP, "Giftcode must obey the new cap");
    }

    private static void pickup() {
        Player p = player(CAP - 15, 0);
        p.effectSkill.isChibi = true;
        p.typeChibi = 0;
        Item gold = new Item();
        gold.template = new Template.ItemTemplate();
        gold.template.type = 9;
        gold.quantity = 10;
        check(!InventoryService.gI().addItemBag(p, gold), "Doubled pickup exceeding cap must be rejected");
        check(p.inventory.gold == CAP - 15, "Rejected pickup must not change gold");
        p.inventory.gold = CAP - 20;
        check(InventoryService.gI().addItemBag(p, gold) && p.inventory.gold == CAP,
                "Doubled pickup landing on cap must succeed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    @FunctionalInterface private interface Action { void run() throws Exception; }
    private static void concurrently(Action first, Action second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread[] workers = new Thread[2];
        Action[] actions = {first, second};
        for (int i = 0; i < workers.length; i++) {
            Action action = actions[i];
            workers[i] = new Thread(() -> {
                try { start.await(); action.run(); }
                catch (Throwable e) { failure.compareAndSet(null, e); }
            });
            workers[i].start();
        }
        start.countDown();
        for (Thread worker : workers) {
            worker.join(5000);
            check(!worker.isAlive(), "Concurrent requests must not deadlock");
        }
        if (failure.get() != null) throw new AssertionError("Concurrent request failed", failure.get());
    }
    private static void replace(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }
    private static void set(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
