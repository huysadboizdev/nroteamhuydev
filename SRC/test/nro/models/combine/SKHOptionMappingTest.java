package nro.models.combine;

import java.util.Arrays;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.ItemService;

/** Offline regression: generated SKH option pairs and equipped set recognition. */
public final class SKHOptionMappingTest {
    public static void main(String[] args) throws Exception {
        int[][] expected = {
            {127, 139}, {128, 140}, {129, 141}, {130, 142}, {131, 143},
            {132, 144}, {133, 136}, {134, 137}, {135, 138}, {233, 234},
            {237, 238, 239, 240}, {241, 242, 243, 244}, {245, 246, 247, 248}
        };
        String[] counters = {"thienXinHang", "kirin", "songoku", "picolo", "ocTieu",
                "pikkoroDaimao", "kakarot", "cadic", "nappa", "gohan", "nail", "cadicM", "thanVuTruKaio"};
        for (int id = 0; id <= 255; id++) {
            Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id, "Option #", 0));
        }
        for (int type = 0; type < 5; type++) {
            Template.ItemTemplate template = new Template.ItemTemplate();
            template.id = (short) type;
            template.type = (byte) type;
            template.name = "Test equipment";
            Manager.ITEM_TEMPLATES.add(template);
        }
        for (int i = 0; i < expected.length; i++) {
            int primary = expected[i][0];
            int[] secondary = Arrays.copyOfRange(expected[i], 1, expected[i].length);
            int[] actual = ItemService.gI().getOptionIdsBySKH(primary);
            check(Arrays.equals(actual, secondary), "SKH " + primary + " expected "
                    + Arrays.toString(secondary) + ", got " + Arrays.toString(actual));
            Item legacy = ItemService.gI().createNewItem((short) 0);
            legacy.itemOptions.clear();
            ItemService.gI().Option(legacy, primary);
            check(legacy.itemOptions.size() == expected[i].length + 1,
                    "Option helper must include all effects for " + primary);
            for (int optionId : expected[i]) {
                check(legacy.itemOptions.stream().filter(o -> o.optionTemplate.id == optionId).count() == 1,
                        "Option helper missing SQL option " + optionId);
                Item singleOption = ItemService.gI().createNewItem((short) 0);
                singleOption.itemOptions.clear();
                singleOption.itemOptions.add(new Item.ItemOption(optionId, 1));
                check(singleOption.isSKH(), "Must recognize SQL set option " + optionId);
            }
            Player p = new Player();
            for (short type = 0; type < 5; type++) {
                Item item = ItemService.gI().createItemSKH(type, primary);
                for (int optionId : expected[i]) {
                    check(item.itemOptions.stream().filter(o -> o.optionTemplate.id == optionId).count() == 1,
                            "Generated item must have exactly one option " + optionId);
                }
                check(item.itemOptions.size() == expected[i].length + 1,
                        "Generated item must not have a foreign set option");
                p.inventory.itemsBody.add(item);
            }
            p.inventory.itemsBody.add(new Item());
            p.setClothes.setup();
            if (counters[i] != null) {
                int count = p.setClothes.getClass().getField(counters[i]).getByte(p.setClothes);
                check(count == 5, "Equipped SKH " + primary + " must count 5 pieces, got " + count);
                p.inventory.itemsBody.set(0, new Item());
                p.setClothes.setup();
                check(p.setClothes.getClass().getField(counters[i]).getByte(p.setClothes) == 4,
                        "Removing equipment must reset counter for " + primary);
            }
        }
        for (int id : new int[]{30, 126, 145, 235, 236, 249, 250}) {
            Item ordinary = ItemService.gI().createNewItem((short) 0);
            ordinary.itemOptions.clear();
            ordinary.itemOptions.add(new Item.ItemOption(id, 1));
            check(!ordinary.isSKH(), "Non-set option must not be recognized: " + id);
        }
        int[][] planetSets = {{127, 128, 129, 233, 245}, {130, 131, 132, 233, 237},
                {133, 134, 135, 233, 241}};
        for (int gender = 0; gender < 3; gender++) {
            for (int roll = 0; roll < 200; roll++) {
                int[] options = ItemService.gI().randOptionItemKichHoat(gender);
                boolean allowed = false;
                for (int id : planetSets[gender]) allowed |= id == options[0];
                check(allowed, "Random set must belong to equipment planet");
                int[] effects = ItemService.gI().getOptionIdsBySKH(options[0]);
                for (int slot = 1; slot < options.length; slot++) {
                    check(options[slot] == (slot <= effects.length ? effects[slot - 1] : -1),
                            "Random generation must use SQL mapping and -1 padding");
                }
            }
        }
        System.out.println("PASS: all 13 SQL SKH mappings, generation helpers, recognition and equipped counters");
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
