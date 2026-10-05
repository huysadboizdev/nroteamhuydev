package nro.models.item;

import nro.models.consts.ConstItem;

/** Shared rules for saved, copied, rewarded and shop-created ruby items. */
public final class RubyItemRules {
    private RubyItemRules() {}

    public static void normalize(Item item) {
        if (item == null || item.template == null || item.itemOptions == null) return;
        SealedEquipment.normalize(item);
        if (item.template.id == 1775) {
            item.itemOptions.removeIf(o -> o != null && o.optionTemplate != null
                    && (o.optionTemplate.id == 30 || o.optionTemplate.id == 86 || o.optionTemplate.id == 87));
            item.itemOptions.add(new Item.ItemOption(30, 1));
        }
        if (item.template.id == ConstItem.CAPSULE_HONG) {
            item.itemOptions.removeIf(o -> o != null && o.optionTemplate != null && o.optionTemplate.id == 97);
        } else if (item.template.id == ConstItem.VE_TANG_HONG_NGOC) {
            boolean[] seen = {false};
            item.itemOptions.removeIf(o -> {
                if (o == null || o.optionTemplate == null || o.optionTemplate.id != 30) return false;
                if (seen[0]) return true;
                seen[0] = true;
                o.param = 1;
                return false;
            });
            if (!seen[0]) item.itemOptions.add(new Item.ItemOption(30, 1));
        }
    }
}
