package nro.models.player;

import java.lang.reflect.*;
import nro.models.item.Item;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.*;
import nro.models.task.TaskMain;
import nro.models.intrinsic.Intrinsic;

public class DragonPetCollectionTest {
    private static Method collection;
    private static int refreshes;
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    private static Item dragon(int id) {
        Item item = ItemService.gI().createNewItem((short) id);
        item.quantity = 1;
        item.itemOptions.clear();
        return item;
    }
    private static boolean full(Inventory inventory) throws Exception {
        return (boolean) collection.invoke(null, inventory);
    }
    private static Inventory set() {
        Inventory inventory = new Inventory();
        for (int id = 1765; id <= 1771; id++) inventory.itemsBag.add(dragon(id));
        return inventory;
    }
    private static Player owner() {
        return init(new Player());
    }
    private static Player init(Player p) {
        p.isPlayer = true;
        p.playerIntrinsic.intrinsic = new Intrinsic();
        p.playerTask.taskMain = new TaskMain();
        p.playerTask.taskMain.id = -1;
        p.nPoint.hpg = 10000; p.nPoint.mpg = 10000; p.nPoint.dameg = 1000; p.nPoint.limitPower = 9;
        for (int i = 0; i < 12; i++) p.inventory.itemsBody.add(new Item());
        return p;
    }
    private static void grant(Player p) {
        for (int id = 1765; id <= 1771; id++) p.inventory.itemsBag.add(dragon(id));
    }
    public static void main(String[] args) throws Exception {
        for (int id = 0; id <= 250; id++) Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id, "Option #", 0));
        for (int id = 0; id <= 1900; id++) {
            Template.ItemTemplate t = new Template.ItemTemplate();
            t.id = (short) id; t.type = 27; t.gender = 3; t.name = "Fixture";
            Manager.ITEM_TEMPLATES.add(t);
        }
        Field service = Service.class.getDeclaredField("instance"); service.setAccessible(true);
        service.set(null, new Service() {
            @Override public void point(Player p) { refreshes++; p.nPoint.calPoint(); }
            @Override public void Send_Caitrang(Player p) {}
        });
        if (args[0].equals("eligibility")) {
            try { collection = Class.forName("nro.models.item.DragonPetCollection").getMethod("hasFullPermanentSet", Inventory.class); }
            catch (ReflectiveOperationException e) { throw new AssertionError("Permanent dragon collection detector has not been implemented", e); }
            eligibility();
        } else if (args[0].equals("stats")) stats();
        else if (args[0].equals("refresh")) refresh();
        else throw new AssertionError(args[0]);
        System.out.println("PASS dragon collection " + args[0]);
    }
    private static void eligibility() throws Exception {
        check(!full(null), "Null inventory");
        check(!full(new Inventory()), "Empty inventory");
        Inventory inv = set(); check(full(inv), "Seven distinct permanent dragons");
        inv.itemsBag.remove(6); inv.itemsBag.add(dragon(1765));
        check(!full(inv), "Duplicate cannot replace missing dragon");
        for (int days : new int[]{0, 1, 30}) {
            inv = set(); inv.itemsBag.get(6).itemOptions.add(new Item.ItemOption(93, days));
            check(!full(inv), "Duration option excludes dragon even at zero");
        }
        inv = set(); inv.itemsBag.get(6).quantity = 0; check(!full(inv), "Zero quantity");
        inv.itemsBag.get(6).quantity = -1; check(!full(inv), "Negative quantity");
        inv = set(); inv.itemsBag.set(6, null); check(!full(inv), "Null item");
        inv = set(); inv.itemsBag.get(6).template = null; check(!full(inv), "Null template");
        inv = set(); inv.itemsBag.get(6).itemOptions = null; check(!full(inv), "Null options");
        inv = set(); inv.itemsBag.get(6).itemOptions.add(null); check(!full(inv), "Null option");
        inv = set(); inv.itemsBag.get(6).itemOptions.add(new Item.ItemOption()); check(!full(inv), "Null option template");
        inv = set(); inv.itemsBox.add(inv.itemsBag.remove(6)); inv.itemsBody.add(inv.itemsBag.remove(5));
        inv.itemsBag.add(null); inv.itemsBag.add(new Item()); check(full(inv), "Mixed storage tolerates unrelated null slots");
        inv = set(); inv.itemsBag.addAll(set().itemsBag); check(full(inv), "Two sets still eligible");
        inv = new Inventory(); inv.itemsBag = null; inv.itemsBody = null; inv.itemsBox = set().itemsBag;
        check(full(inv), "Nullable storage lists");
    }
    private static void stats() {
        Player p = owner(); p.nPoint.calPoint();
        int hp = p.nPoint.hpMax, mp = p.nPoint.mpMax, damage = p.nPoint.dame;
        p.nPoint.hp = 500; p.nPoint.mp = 600; grant(p);
        for (int i = 0; i < 20; i++) {
            p.nPoint.calPoint();
            check(p.nPoint.hpMax == hp * 110 / 100 && p.nPoint.mpMax == mp * 110 / 100 && p.nPoint.dame == damage * 110 / 100, "Collection adds exactly ten percent, calculation " + i);
            check(p.nPoint.hp == 500 && p.nPoint.mp == 600, "Gaining bonus never heals");
        }
        grant(p); p.nPoint.calPoint(); check(p.nPoint.hpMax == hp * 110 / 100, "Two sets do not stack");
        p.inventory.itemsBag.clear(); grant(p);
        p.inventory.itemsBody.set(7, p.inventory.itemsBag.remove(6));
        p.inventory.itemsBody.get(7).itemOptions.add(new Item.ItemOption(77, 20));
        p.inventory.itemsBody.get(7).itemOptions.add(new Item.ItemOption(50, 20));
        p.inventory.itemsBody.get(7).itemOptions.add(new Item.ItemOption(103, 20));
        p.nPoint.calPoint(); check(p.nPoint.hpMax == hp * 120 / 100 * 110 / 100 && p.nPoint.mpMax == mp * 120 / 100 * 110 / 100 && p.nPoint.dame == damage * 120 / 100 * 110 / 100, "Collection multiplies after equipped options");
        p.inventory.itemsBody.set(7, new Item()); p.inventory.itemsBag.clear(); grant(p);
        p.nPoint.calPoint(); p.nPoint.hp = p.nPoint.hpMax; p.nPoint.mp = p.nPoint.mpMax;
        p.inventory.itemsBag.remove(6); p.nPoint.calPoint();
        check(p.nPoint.hpMax == hp && p.nPoint.mpMax == mp && p.nPoint.dame == damage, "Loss removes bonus");
        check(p.nPoint.hp == hp && p.nPoint.mp == mp, "Loss clamps current HP and KI");
        for (int flag = 0; flag < 4; flag++) {
            Player excluded = flag == 0 ? init(new Pet(owner())) : owner(); grant(excluded);
            excluded.isPet = flag == 0; excluded.isBoss = flag == 1; excluded.isBot = flag == 2; excluded.isNewPet = flag == 3;
            excluded.nPoint.calPoint();
            check(!excluded.nPoint.tlHp.contains(10) && !excluded.nPoint.tlMp.contains(10) && !excluded.nPoint.tlDame.contains(10), "Non-primary actor excluded " + flag);
        }
        Player login = owner(); grant(login); login.nPoint.calPoint();
        check(login.nPoint.hpMax == hp * 110 / 100, "Relogin rebuilds bonus from inventory");
    }
    private static void refresh() {
        InventoryService inventory = new InventoryService() { @Override public void openBox(Player p) {} };
        Player p = owner(); p.nPoint.calPoint(); refreshes = 0;
        inventory.sendItemBags(p); check(refreshes == 0, "Unchanged incomplete collection");
        grant(p); inventory.sendItemBags(p); check(refreshes == 1 && p.nPoint.hpMax == 11000, "Bag gain triggers point immediately");
        inventory.sendItemBody(p); inventory.sendItemBox(p); check(refreshes == 1, "Unchanged complete collection sends no repeated point");
        p.inventory.itemsBox.add(new Item());
        inventory.itemBagToBox(p, 6);
        check(refreshes == 1 && p.nPoint.hpMax == 11000 && p.inventory.itemsBox.get(0).template.id == 1771, "Real bag-box transfer retains bonus");
        inventory.itemBoxToBodyOrBag(p, 0);
        check(refreshes == 1 && p.nPoint.hpMax == 11000 && !p.inventory.itemsBox.get(0).isNotNullItem(), "Real box-bag transfer retains bonus");
        inventory.itemBagToBox(p, 6);
        p.inventory.itemsBox.clear(); inventory.sendItemBox(p); check(refreshes == 2 && p.nPoint.hpMax == 10000, "Box loss refreshes immediately");
        p.inventory.itemsBody.set(7, dragon(1771)); inventory.sendItemBody(p); check(refreshes == 3, "Body gain refreshes immediately");
        p.inventory.itemsBody.set(7, new Item()); inventory.sendItemBody(p); check(refreshes == 4, "Body loss refreshes immediately");
        Player pet = init(new Pet(owner())); pet.nPoint.calPoint(); grant(pet);
        inventory.sendItemBags(pet); check(refreshes == 4, "Pet collection never refreshes owner's bonus");
        inventory.sendItemBags(null); inventory.sendItemBody(null); inventory.sendItemBox(null);
        Player disposed = owner(); disposed.inventory = null;
        inventory.sendItemBags(disposed); inventory.sendItemBody(disposed); inventory.sendItemBox(disposed);
        disposed = owner(); disposed.nPoint = null;
        inventory.sendItemBags(disposed); inventory.sendItemBody(disposed); inventory.sendItemBox(disposed);
        check(refreshes == 4, "Null guards do not refresh");
    }
}
