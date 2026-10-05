package nro.models.item;

import java.util.List;
import nro.models.player.Inventory;

/** Current ownership of all seven permanent golden-egg dragons. */
public final class DragonPetCollection {
    private DragonPetCollection() {}

    public static boolean hasFullPermanentSet(Inventory inventory) {
        return countPermanentTypes(inventory) == 7;
    }

    public static int countPermanentTypes(Inventory inventory) {
        if (inventory == null) return 0;
        return Integer.bitCount(collect(inventory.itemsBag) | collect(inventory.itemsBox) | collect(inventory.itemsBody));
    }

    private static int collect(List<Item> items) {
        int found = 0;
        if (items == null) return found;
        for (Item item : items) {
            if (item == null || item.template == null || item.quantity <= 0 || item.itemOptions == null
                    || item.template.id < 1765 || item.template.id > 1771) continue;
            boolean permanent = true;
            for (Item.ItemOption option : item.itemOptions) {
                if (option == null || option.optionTemplate == null || option.optionTemplate.id == 93) {
                    permanent = false;
                    break;
                }
            }
            if (permanent) found |= 1 << (item.template.id - 1765);
        }
        return found;
    }
}
