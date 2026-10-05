package nro.models.npc_list;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.PetService;
import sun.misc.Unsafe;

/** Offline exercise of the real VIP reward handlers; captures inventory grants. */
public final class VipRewardTest {
    private static final Map<Short, Integer> rewards = new LinkedHashMap<>();
    private static int sidekickGrants;
    private static void replace(Class<?> type, String name, Object value) throws Exception {
        Field f = type.getDeclaredField(name);
        f.setAccessible(true);
        f.set(null, value);
    }
    public static void main(String[] args) throws Exception {
        replace(ItemService.class, "i", new ItemService() {
            @Override public Item createNewItem(short id, int quantity) {
                Item item = new Item();
                item.template = new Template.ItemTemplate();
                item.template.id = id;
                item.quantity = quantity;
                return item;
            }
            @Override public Template.ItemOptionTemplate getItemOptionTemplate(int id) {
                Template.ItemOptionTemplate option = new Template.ItemOptionTemplate();
                option.id = id;
                return option;
            }
        });
        replace(InventoryService.class, "I", new InventoryService() {
            @Override public boolean addItemBag(Player player, Item item) {
                rewards.merge(item.template.id, item.quantity, Integer::sum);
                return true;
            }
            @Override public void sendItemBags(Player player) {}
        });
        replace(PetService.class, "instance", new PetService() {
            @Override public void createNormalPet(Player player, byte... limit) {
                sidekickGrants++;
            }
        });
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);
        ToriBot npc = (ToriBot) unsafe.allocateInstance(ToriBot.class);
        Player player = (Player) unsafe.allocateInstance(Player.class);
        boolean hasSidekick = args.length > 1 && args[1].equals("existing");
        if (hasSidekick) player.pet = (nro.models.player.Pet) unsafe.allocateInstance(nro.models.player.Pet.class);
        nro.models.player.Pet previousSidekick = player.pet;
        int level = Integer.parseInt(args[0]);
        Method handler = ToriBot.class.getDeclaredMethod("VIP" + level, Player.class);
        handler.setAccessible(true);
        handler.invoke(npc, player);
        if (sidekickGrants != 0 || player.pet != previousSidekick)
            throw new AssertionError("VIP " + level + " must not grant or replace a sidekick");
        Map<Short, Integer> expected = new LinkedHashMap<>();
        int[][] entries = switch (level) {
            case 1 -> new int[][]{{457,200},{459,10},{1252,1},{1256,1},{987,5}};
            case 2 -> new int[][]{{457,500},{459,10},{1252,1},{1254,1},{987,10},{584,1}};
            case 3 -> new int[][]{{457,700},{459,10},{1252,1},{1254,1},{987,30},{584,1},{956,10}};
            case 4 -> new int[][]{{457,1000},{459,10},{987,50},{1554,1},{1772,1},{1557,1},{1204,20}};
            default -> throw new AssertionError("Invalid VIP level");
        };
        for (int[] entry : entries) expected.put((short) entry[0], entry[1]);
        if (!rewards.equals(expected)) throw new AssertionError("VIP " + level + ": expected " + expected + ", received " + rewards);
        if (player.timevip <= System.currentTimeMillis()) throw new AssertionError("VIP duration missing");
        System.out.println("PASS VIP " + level + " sidekick=" + hasSidekick + " grants: " + rewards);
    }
}
