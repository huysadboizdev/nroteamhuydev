package nro.models.combine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.IntUnaryOperator;
import nro.models.item.Item;

/** Applied only to newly crafted equipment, after the factory's racial bonus. */
public final class ThienSuCraftStats {
    private ThienSuCraftStats() { }
    private static final int[] MAIN_OPTIONS = {47, 22, 0, 23, 14};

    static int roll(IntUnaryOperator random, int bound) {
        int value = random.applyAsInt(bound);
        if (value < 0 || value >= bound) throw new IllegalArgumentException("Dữ liệu ngẫu nhiên không hợp lệ");
        return value;
    }

    public static void apply(Item item, int luck, IntUnaryOperator random) {
        if (item == null || item.template == null || item.template.id < 1048 || item.template.id > 1062
                || item.template.type != (item.template.id - 1048) / 3 || item.itemOptions == null) {
            throw new IllegalArgumentException("Dữ liệu trang bị Thiên Sứ không hợp lệ");
        }
        int mainId = MAIN_OPTIONS[item.template.type];
        Item.ItemOption main = null;
        for (Item.ItemOption option : item.itemOptions) {
            if (option == null || option.optionTemplate == null) throw new IllegalArgumentException("Dữ liệu chỉ số không hợp lệ");
            if (option.optionTemplate.id == mainId) {
                if (main != null) throw new IllegalArgumentException("Chỉ số chính bị trùng");
                main = option;
            }
        }
        if (main == null || main.param <= 0) throw new IllegalArgumentException("Thiếu chỉ số chính Thiên Sứ");
        int percent = 1 + roll(random, 30);
        long base = main.param;
        long increase = Math.min((base * percent + 99) / 100, base * 30 / 100);
        main.param = Math.toIntExact(base + increase);

        // Retain the historical inclusive 0..50 draw and tier thresholds.
        int draw = roll(random, 51);
        if (draw > luck) return;
        int count = draw >= luck - 3 ? 3 : draw >= luck - 10 ? 2 : 1;
        ArrayList<Integer> pool = new ArrayList<>(Arrays.asList(50, 77, 103, 94, 5));
        for (int n = 0; n < count; n++) {
            int option = pool.remove(roll(random, pool.size()));
            item.itemOptions.add(new Item.ItemOption(option, 1 + roll(random, 7)));
        }
    }
}
