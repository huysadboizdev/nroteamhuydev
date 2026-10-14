package nro.models.player;

import nro.models.item.Item;

/** Upgrade bonus shared by stats and the existing set-effect protocol. */
public final class EquipmentUpgradeBonus {
    private EquipmentUpgradeBonus() {}

    public static int level(Player player) {
        if (player == null || player.inventory == null || player.inventory.itemsBody == null
                || player.inventory.itemsBody.size() < 5) return 0;
        int minimum = 8;
        for (int slot = 0; slot < 5; slot++) {
            Item item = player.inventory.itemsBody.get(slot);
            if (item == null || !item.isNotNullItem() || item.template.type != slot) return 0;
            int upgrade = 0;
            for (Item.ItemOption option : item.itemOptions) {
                if (option != null && option.optionTemplate != null && option.optionTemplate.id == 72) {
                    upgrade = option.param;
                    break;
                }
            }
            minimum = Math.min(minimum, upgrade);
        }
        return Math.max(0, minimum);
    }

    public static int percent(Player player) {
        return Math.max(0, level(player) - 4);
    }
}
