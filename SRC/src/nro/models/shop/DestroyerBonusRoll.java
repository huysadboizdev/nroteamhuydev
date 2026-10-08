package nro.models.shop;

import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import nro.models.item.Item;

/** The original weighted base bonus plus one independent HP/KI/damage line. */
public final class DestroyerBonusRoll {
    private static final int[] OPTIONS = {77, 103, 50};
    private DestroyerBonusRoll() {}
    public static void apply(Item item) { apply(item, ThreadLocalRandom.current()); }
    static void apply(Item item, RandomGenerator random) {
        for (Item.ItemOption option : item.itemOptions) {
            int id = option.optionTemplate.id;
            if (id == 50 || id == 77 || id == 103 || id == 34 || id == 35 || id == 36
                    || id == 87) {
                throw new IllegalArgumentException("Invalid destroyer catalog bonus/flags: " + id);
            }
        }
        int roll = random.nextInt(1, 101);
        int basePercent = roll == 1 ? 15 : roll <= 15 ? random.nextInt(11, 15)
                : roll <= 35 ? random.nextInt(7, 11)
                : roll <= 60 ? random.nextInt(4, 7) : random.nextInt(0, 4);
        for (Item.ItemOption option : item.itemOptions) {
            int id = option.optionTemplate.id;
            if (id == 0 || id == 22 || id == 23 || id == 14 || id == 27 || id == 28 || id == 47) {
                option.param = Math.toIntExact(option.param + (long) option.param * basePercent / 100);
            }
        }
        item.itemOptions.add(new Item.ItemOption(OPTIONS[random.nextInt(3)], random.nextInt(1, 6)));
        item.info = item.getInfo();
    }
}
