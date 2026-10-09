package nro.models.services;

import nro.models.intrinsic.Intrinsic;
import nro.models.item.Item;
import nro.models.map.Zone;
import nro.models.player.*;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.task.TaskMain;

/** Exercises actual inventory/model lifecycle; only outbound packets are silenced. */
public final class DiscipleCompanionTest {
    static class QuietZone extends Zone {
        QuietZone(nro.models.map.Map map) { super(map, 0, 20); }
        @Override public void load_Me_To_Another(Player p) {}
        @Override public void load_Another_To_Me(Player p) {}
    }
    static void check(boolean ok, String message) { PinkFixture.check(ok, message); }
    static void prepare(Player p, int slots) {
        p.playerIntrinsic.intrinsic = new Intrinsic();
        p.playerTask.taskMain = new TaskMain(); p.playerTask.taskMain.id = -1;
        p.nPoint.power = 40_000_000_000L; p.nPoint.limitPower = 9;
        p.nPoint.hpg = 10000; p.nPoint.mpg = 10000; p.nPoint.dameg = 1000;
        for (int i = 0; i < slots; i++) p.inventory.itemsBody.add(new Item());
        p.nPoint.calPoint(); p.nPoint.setFullHpMp();
    }
    static Player owner() throws Exception {
        Player p = new Player(); p.id = 991943; p.isPlayer = true; prepare(p, 11);
        for (int i = 0; i < 4; i++) p.inventory.itemsBag.add(new Item());
        p.zone = new QuietZone(PinkFixture.zone().map); p.zone.addPlayer(p);
        Pet pet = new Pet(p); pet.id = -p.id; pet.typePet = 2; prepare(pet, 9);
        // Deliberately leave legacy pet.type at zero: freshly created VIPs must work.
        pet.zone = p.zone; p.zone.addPlayer(pet); p.pet = pet;
        return p;
    }
    static Item item(int id, int option) {
        Item i = ItemService.gI().createNewItem((short) id); i.itemOptions.clear();
        i.itemOptions.add(new Item.ItemOption(option, 10)); i.quantity = 1; return i;
    }
    static void equip(Player p, Item i) {
        p.inventory.itemsBag.set(0, i); InventoryService.gI().itemBagToPetBody(p, 0);
    }
    static void fixture() throws Exception {
        for (int id = 0; id <= 250; id++) Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id, "Fixture #", 0));
        for (int id = 0; id <= 1000; id++) {
            var t = new Template.ItemTemplate(); t.id = (short) id; t.gender = 3;
            t.type = (byte) (id == 900 ? 11 : 27); t.name = "Fixture"; t.part = (short) (id == 900 ? 42 : -1);
            Manager.ITEM_TEMPLATES.add(t);
        }
        PinkFixture.replace(Service.class, "instance", new Service() {
            @Override public void point(Player p) { p.nPoint.calPoint(); }
            @Override public void showInfoPet(Player p) {}
            @Override public void Send_Caitrang(Player p) {}
            @Override public void player(Player p) {}
            @Override public void sendFlagBag(Player p) {}
            @Override public void sendThongBao(Player p, String s) {}
            @Override public void sendThongBaoOK(Player p, String s) {}
            @Override public void sendMessAnotherNotMeInMap(Player p, nro.models.network.Message m) {}
        });
        PinkFixture.replace(InventoryService.class, "I", new InventoryService() {
            @Override public void sendItemBags(Player p) {}
            @Override public void sendItemBody(Player p) {}
        });
    }
    static void eligibility() throws Exception {
        for (byte type : new byte[]{0, 1, 2, 3, 4}) {
            Player p = owner(); p.pet.typePet = type; Item tiger = item(942, 77); equip(p, tiger);
            check((p.pet.inventory.itemsBody.get(7) == tiger) == (type >= 2), "VIP companion eligibility type=" + type);
            if (type < 2) check(p.inventory.itemsBag.get(0) == tiger && p.pet.newPet == null, "reject leaves ordinary disciple item untouched");
        }
        Player p = owner(); Item food = item(74, 77); equip(p, food);
        check(p.inventory.itemsBag.get(0) == food && !p.pet.inventory.itemsBody.get(7).isNotNullItem(), "type-27 consumables cannot occupy companion slot");
        Item weak = item(942, 77); p.pet.nPoint.power = 1_000_000; equip(p, weak);
        check(p.inventory.itemsBag.get(0) == weak && p.pet.newPet == null, "insufficient power leaves item untouched");
    }
    static void transfers() throws Exception {
        Player p = owner(); Pet pet = p.pet;
        p.inventory.itemsBody.set(7, item(942, 77)); p.sendNewPet();
        NewPet masterCompanion = p.newPet;
        int hp = pet.nPoint.hpMax, masterHp = p.nPoint.hpMax;
        Item tiger = item(942, 77); equip(p, tiger);
        check(pet.inventory.itemsBody.get(7) == tiger && !p.inventory.itemsBag.get(0).isNotNullItem(), "equip moves tiger into disciple slot 7");
        check(pet.nPoint.hpMax == hp * 110 / 100 && p.nPoint.hpMax == masterHp, "companion bonuses apply once to disciple only");
        NewPet first = pet.newPet;
        check(first != null && first.master == pet && first.getHead() == 966, "tiger actor belongs to disciple");
        check(p.newPet == masterCompanion && first != masterCompanion, "master and disciple keep independent companions");
        first.joinMapMaster(); check(first.zone == pet.zone, "companion joins disciple zone");
        pet.sendNewPet(); check(pet.newPet == first, "reconstruction is idempotent");
        Item cat = item(892, 77); equip(p, cat);
        check(p.inventory.itemsBag.get(0) == tiger && pet.inventory.itemsBody.get(7) == cat, "replacement returns original item to same bag slot");
        check(pet.newPet != first && pet.newPet.getHead() == 882 && first.master == null && first.zone == null, "replacement retires old actor");
        NewPet second = pet.newPet; second.joinMapMaster();
        for (int i = 0; i < p.inventory.itemsBag.size(); i++) p.inventory.itemsBag.set(i, item(74, 77));
        InventoryService.gI().itemPetBodyToBag(p, 7);
        check(pet.inventory.itemsBody.get(7) == cat && pet.newPet == second && second.master == pet, "full bag preserves equipped item and actor");
        p.inventory.itemsBag.set(1, new Item()); InventoryService.gI().itemPetBodyToBag(p, 7);
        check(!pet.inventory.itemsBody.get(7).isNotNullItem() && p.inventory.itemsBag.get(1) == cat && pet.newPet == null, "unequip returns item and clears companion");
        check(second.master == null && second.zone == null && pet.nPoint.hpMax == hp, "unequip disposes actor and restores disciple stats");
        pet.sendNewPet(); check(pet.newPet == null, "empty slot cannot summon");
    }
    static void lifecycle() throws Exception {
        Player p = owner(); Pet pet = p.pet;
        pet.inventory.itemsBody.set(7, item(942, 77)); pet.sendNewPet();
        check(pet.newPet != null && pet.newPet.master == pet, "loaded equipment reconstructs disciple companion");
        NewPet follower = pet.newPet; follower.joinMapMaster();
        Zone previous = pet.zone; pet.zone = new QuietZone(PinkFixture.zone().map); pet.zone.addPlayer(pet);
        follower.update(); check(follower.zone == pet.zone && !previous.getHumanoids().contains(follower), "companion migrates to disciple map");
        for (byte status : new byte[]{Pet.GOHOME, Pet.FUSION, Pet.HTVV}) {
            var f = Pet.class.getDeclaredField("status"); f.setAccessible(true); f.setByte(pet, status);
            follower.update(); check(follower.zone == null, "companion hides with inactive disciple status=" + status);
            f.setByte(pet, Pet.FOLLOW); follower.update(); check(follower.zone == pet.zone, "companion returns with disciple");
        }
        pet.nPoint.hp = 0; follower.update(); check(follower.zone == null, "companion hides when disciple dies");
        pet.nPoint.hp = pet.nPoint.hpMax; follower.update(); check(follower.zone == pet.zone, "companion returns after revive");
        pet.dispose(); check(follower.master == null && follower.zone == null, "disciple disposal removes nested companion");
    }
    static void backAccessory() throws Exception {
        Player p = owner(); Pet pet = p.pet; Item flag = item(900, 77); equip(p, flag);
        check(pet.inventory.itemsBody.get(8) == flag, "fresh VIP uses typePet for accessory eligibility");
        check(pet.getFlagBag() == 42, "back accessory renders from slot 8");
        equip(p, item(942, 77)); check(pet.getFlagBag() == 42, "companion slot cannot override back accessory");
    }
    static void departure() throws Exception {
        Player p = owner(); equip(p, item(942, 77)); Pet pet = p.pet;
        NewPet follower = pet.newPet; follower.joinMapMaster(); Zone old = pet.zone;
        nro.models.map.service.ChangeMapService.gI().exitMap(pet);
        check(follower.zone == null && !old.getHumanoids().contains(follower), "disciple map exit immediately removes companion, even if ticks stop");
        check(pet.newPet == follower && follower.master == pet, "map exit keeps equipped companion ready for next map");
    }
    public static void main(String[] args) {
        try {
            fixture(); String mode = args.length == 0 ? "all" : args[0];
            if (mode.equals("all") || mode.equals("eligibility")) eligibility();
            if (mode.equals("all") || mode.equals("transfers")) transfers();
            if (mode.equals("all") || mode.equals("lifecycle")) lifecycle();
            if (mode.equals("all") || mode.equals("accessory")) backAccessory();
            if (mode.equals("all") || mode.equals("departure")) departure();
            System.out.println("DISCIPLE_COMPANION PASS " + mode); System.exit(0);
        } catch (Throwable e) { e.printStackTrace(); System.exit(1); }
    }
}
