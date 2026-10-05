package nro.models.combine;

import java.lang.reflect.Field;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.InventoryService;
import nro.models.services.Service;

/** Exercises real menu/payment/item logic with only network output replaced. */
public final class DirectCrystalStarTest {
    public static void main(String[] args) throws Exception {
        RecordingNpc npc = new RecordingNpc();
        replaceSingleton(Service.class, "instance", new Service() {
            @Override public void sendMoney(Player player) { }
            @Override public void sendThongBao(Player player, String text) { }
        });
        replaceSingleton(InventoryService.class, "I", new InventoryService() {
            @Override public void sendItemBags(Player player) { }
        });
        for (int id = 0; id <= 228; id++) {
            Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id, "Option #", 0));
        }
        checkDirectUpgrade(npc, 7, 8);
        checkDirectUpgrade(npc, 7, 9);
        checkDirectUpgrade(npc, 8, 9);
        checkRejected(npc, 8, 8);
        checkRejected(npc, 9, 9);
        checkRejected(npc, 9, 10);
        checkInsufficientGems(npc);
        System.out.println("PASS: direct 8th/9th stars without reinforcement, costs, separate options, full slots and 9-star cap");
    }

    private static Player playerWithItems(int filledStars, int holes) {
        Player player = new Player() {
            @Override public void sendMessage(Message message) { }
        };
        player.inventory.gem = 100;
        Item gear = new Item();
        gear.template = new Template.ItemTemplate();
        gear.template.type = 0;
        gear.template.name = "Test armor";
        gear.quantity = 1;
        gear.itemOptions.add(new Item.ItemOption(102, filledStars));
        gear.itemOptions.add(new Item.ItemOption(107, holes));
        gear.itemOptions.add(new Item.ItemOption(50, 21));
        Item stone = new Item();
        stone.template = new Template.ItemTemplate();
        stone.template.type = 30;
        stone.template.name = "Crystal";
        stone.quantity = 2;
        stone.itemOptions.add(new Item.ItemOption(50, 3));
        player.combineNew.itemsCombine.add(gear);
        player.combineNew.itemsCombine.add(stone);
        player.inventory.itemsBag.add(gear);
        player.inventory.itemsBag.add(stone);
        return player;
    }

    private static void checkDirectUpgrade(RecordingNpc npc, int stars, int holes) {
        Player player = playerWithItems(stars, holes);
        EpSaoTrangBi.showInfoCombine(player);
        check(npc.menu == ConstNpc.MENU_START_COMBINE, "Menu must allow direct star " + (stars + 1));
        check(player.combineNew.gemCombine == 10, "Normal gem cost must be retained");
        EpSaoTrangBi.epSaoTrangBi(player);
        Item gear = player.combineNew.itemsCombine.get(0);
        check(option(gear, 102).param == stars + 1, "Must insert the next crystal star");
        check(player.inventory.gem == 90, "Must charge exactly 10 gems");
        check(player.combineNew.itemsCombine.get(1).quantity == 1, "Must consume exactly one crystal");
        check(gear.itemOptions.stream().noneMatch(o -> o.optionTemplate.id == 228), "Reinforcement option must not be required or added");
        check(gear.itemOptions.stream().filter(o -> o.optionTemplate.id == 50).count() == 2, "8th/9th star bonus stays separate");
        check(option(gear, 50).param == 21, "Existing bonus must not be overwritten");
    }

    private static void checkRejected(RecordingNpc npc, int stars, int holes) {
        Player player = playerWithItems(stars, holes);
        player.combineNew.gemCombine = 10;
        EpSaoTrangBi.showInfoCombine(player);
        check(npc.menu == ConstNpc.IGNORE_MENU, "Full or invalid slots must not open upgrade menu");
        EpSaoTrangBi.epSaoTrangBi(player);
        check(option(player.combineNew.itemsCombine.get(0), 102).param == stars, "Rejected action must not change stars");
        check(player.inventory.gem == 100, "Rejected action must not charge gems");
        check(player.combineNew.itemsCombine.get(1).quantity == 2, "Rejected action must not consume crystal");
    }

    private static void checkInsufficientGems(RecordingNpc npc) {
        Player player = playerWithItems(8, 9);
        EpSaoTrangBi.showInfoCombine(player);
        player.inventory.gem = 9;
        EpSaoTrangBi.epSaoTrangBi(player);
        check(option(player.combineNew.itemsCombine.get(0), 102).param == 8, "Insufficient gems must not upgrade");
        check(player.inventory.gem == 9 && player.combineNew.itemsCombine.get(1).quantity == 2, "Insufficient gems must not consume resources");
    }

    private static Item.ItemOption option(Item item, int id) {
        return item.itemOptions.stream().filter(o -> o.optionTemplate.id == id).findFirst().orElseThrow();
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }

    private static void replaceSingleton(Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(null, value);
    }

    private static final class RecordingNpc extends Npc {
        int menu = -1;
        RecordingNpc() { super(-1, 1, 0, 0, ConstNpc.BA_HAT_MIT, 0); }
        @Override public void confirmMenu(Player player, int selection) { }
        @Override public void createOtherMenu(Player player, int index, String text, String... choices) {
            menu = index;
        }
    }
}
