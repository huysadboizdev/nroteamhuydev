package nro.models.item;

import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import nro.models.services.ItemService;

/** Decode completely before publishing; malformed data must never become an empty chest. */
public final class CollectionChestCodec {
    private CollectionChestCodec() {}

    @SuppressWarnings("unchecked")
    public static String encode(List<Item> items) {
        if (items == null || items.size() != CollectionChestRules.CAPACITY)
            throw new IllegalArgumentException("Invalid collection capacity");
        JSONArray result = new JSONArray();
        for (Item item : items) {
            if (item == null) throw new IllegalArgumentException("Null collection slot");
            JSONArray row = new JSONArray();
            row.add(item.isNotNullItem() ? (int)item.template.id : -1);
            row.add(item.isNotNullItem() ? item.quantity : 0);
            JSONArray options = new JSONArray();
            if (item.isNotNullItem()) {
                for (Item.ItemOption option : item.itemOptions) {
                    JSONArray pair = new JSONArray();pair.add(option.optionTemplate.id);pair.add(option.param);options.add(pair);
                }
            }
            row.add(options);row.add(item.createTime);row.add(item.expire);row.add(item.id);
            result.add(row);
        }
        return result.toJSONString();
    }

    public static List<Item> decode(String json) {
        try {
            Object parsed = JSONValue.parse(json);
            if (!(parsed instanceof JSONArray rows) || rows.size() > CollectionChestRules.CAPACITY)
                throw new IllegalArgumentException("Invalid saved collection");
            List<Item> items = new ArrayList<>();
            for (Object value : rows) {
                if (!(value instanceof JSONArray row) || row.size() != 6 || !(row.get(2) instanceof JSONArray options))
                    throw new IllegalArgumentException("Invalid saved collection item");
                int id = Math.toIntExact((Long)row.get(0));
                int quantity = Math.toIntExact((Long)row.get(1));
                Item item = new Item();
                if (id != -1) {
                    if (id < 0 || id > Short.MAX_VALUE || quantity != 1)
                        throw new IllegalArgumentException("Invalid saved collection quantity/template");
                    item = ItemService.gI().createItemForRestore((short)id, quantity);
                    if (item.template == null) throw new IllegalArgumentException("Unknown collection template");
                    for (Object option : options) {
                        if (!(option instanceof JSONArray pair) || pair.size() != 2)
                            throw new IllegalArgumentException("Invalid saved collection option");
                        int optionId = Math.toIntExact((Long)pair.get(0));
                        if (optionId < 0 || optionId > 255) throw new IllegalArgumentException("Invalid option ID");
                        Item.ItemOption decoded = new Item.ItemOption(optionId,Math.toIntExact((Long)pair.get(1)));
                        if(decoded.optionTemplate == null) throw new IllegalArgumentException("Unknown option template");
                        item.itemOptions.add(decoded);
                    }
                } else if (quantity != 0 || !options.isEmpty()) throw new IllegalArgumentException("Invalid empty slot");
                item.createTime=(Long)row.get(3);item.expire=(Long)row.get(4);item.id=Math.toIntExact((Long)row.get(5));
                items.add(item);
            }
            while(items.size()<CollectionChestRules.CAPACITY) items.add(new Item());
            return items;
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("Cannot restore collection chest",failure);
        }
    }
}
