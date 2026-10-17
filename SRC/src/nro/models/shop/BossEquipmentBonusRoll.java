package nro.models.shop;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.services.ItemService;

/** Applies the requested primary-stat and secondary-percent bonuses to boss equipment drops. */
public final class BossEquipmentBonusRoll {
    private static final int[] PRIMARY_OPTIONS = {0, 22, 23, 14, 27, 28, 47};
    private static final int[] SECONDARY_OPTIONS = {77, 103, 50};

    private BossEquipmentBonusRoll() {}

    public static int apply(Item item) {
        if (item == null || item.template == null || item.itemOptions == null) return 0;
        int bonus = apply(item.itemOptions, item.template.type, ThreadLocalRandom.current());
        if (bonus > 0) item.info = item.getInfo();
        return bonus;
    }

    public static int apply(List<ItemOption> options, int equipmentType) {
        return apply(options, equipmentType, ThreadLocalRandom.current());
    }

    static int apply(List<ItemOption> options, int equipmentType, RandomGenerator random) {
        if (options == null || equipmentType < 0 || equipmentType > 4) return 0;

        int bonusPercent = random.nextInt(7, 16);
        for (ItemOption option : options) {
            if (option == null || option.optionTemplate == null
                    || !contains(PRIMARY_OPTIONS, option.optionTemplate.id)) continue;
            long scaled = (long) option.param * (100 + bonusPercent);
            option.param = Math.toIntExact(Math.floorDiv(scaled + 99, 100));
        }

        boolean hasSecondary = options.stream().anyMatch(option -> option != null
                && option.optionTemplate != null
                && contains(SECONDARY_OPTIONS, option.optionTemplate.id));
        if (!hasSecondary) {
            int[] available = new int[SECONDARY_OPTIONS.length];
            int count = 0;
            for (int optionId : SECONDARY_OPTIONS) {
                if (ItemService.gI().getItemOptionTemplate(optionId) != null) available[count++] = optionId;
            }
            if (count > 0) {
                int optionId = available[random.nextInt(count)];
                options.add(new ItemOption(optionId, random.nextInt(1, 6)));
            }
        }
        return bonusPercent;
    }

    private static boolean contains(int[] values, int target) {
        for (int value : values) if (value == target) return true;
        return false;
    }
}
