package nro.models.item;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import nro.models.player.Inventory;

public final class CollectionChestRules {
    public static final int CAPACITY = 20;
    private CollectionChestRules() {}

    public static boolean permanent(Item item) {
        if (item == null || item.template == null || item.quantity <= 0 || item.expire != 0
                || item.itemOptions == null) return false;
        for (Item.ItemOption option : item.itemOptions) {
            if (option == null || option.optionTemplate == null || option.optionTemplate.id == 93) return false;
        }
        return true;
    }

    public static int count(Inventory inventory) {
        if (inventory == null || inventory.itemsCollection == null) return 0;
        int count = 0;
        Set<Item> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Item item : inventory.itemsCollection) {
            if (permanent(item) && item.quantity == 1 && seen.add(item)) count++;
        }
        return count;
    }

    public static boolean hasBonus(Inventory inventory) {
        return inventory != null && inventory.itemsCollection != null
                && inventory.itemsCollection.size() == CAPACITY && count(inventory) == CAPACITY;
    }
}
