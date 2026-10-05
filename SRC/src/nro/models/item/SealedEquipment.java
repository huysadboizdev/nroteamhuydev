package nro.models.item;

/** Trade restriction applies to an item instance, never an entire template. */
public final class SealedEquipment {
    private SealedEquipment() {}
    public static boolean hasSeal(Item item) {
        return item != null && item.template != null && item.template.type >= 0
                && item.template.type <= 4 && item.itemOptions != null
                && item.itemOptions.stream().anyMatch(o -> o != null && o.optionTemplate != null
                && o.optionTemplate.id >= 34 && o.optionTemplate.id <= 36);
    }
    public static void normalize(Item item) {
        if (!hasSeal(item)) return;
        item.itemOptions.removeIf(o -> o != null && o.optionTemplate != null
                && (o.optionTemplate.id == 30 || o.optionTemplate.id == 86 || o.optionTemplate.id == 87));
        item.itemOptions.add(new Item.ItemOption(30, 1));
        item.itemOptions.add(new Item.ItemOption(87, 1));
    }
}
