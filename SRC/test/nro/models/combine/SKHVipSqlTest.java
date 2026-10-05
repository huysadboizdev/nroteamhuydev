package nro.models.combine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nro.models.item.ActivationSet;
import nro.models.item.Item;
import nro.models.item.SKHCapsuleCatalog;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.ItemService;
import nro.models.shop.ItemShop;
import nro.models.shop.Shop;
import nro.models.shop.TabShop;

/** Validate real SQL equipment/shop shapes, including level-one HP/KI without regen. */
public final class SKHVipSqlTest {
    private static final int[][] BASE = {{47}, {6,27}, {0}, {7,28}, {14}};

    public static void main(String[] args) throws Exception {
        for (int id = 0; id < 256; id++) Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id, "Option #", 0));
        for (int id = 0; id < 282; id++) {
            Template.ItemTemplate t = new Template.ItemTemplate(); t.id = (short) id; t.name = "SQL equipment " + id;
            Manager.ITEM_TEMPLATES.add(t);
        }
        Map<Integer,Integer> levels = new HashMap<>();
        Map<Integer,Map<Integer,Integer>> maximums = new HashMap<>();
        Map<Integer,Shop> shops = new HashMap<>();
        List<String[]> catalogs = new ArrayList<>();
        for (String line : Files.readAllLines(Path.of(".analysis/gold-economy-check/vip-sql-fixture.tsv"))) {
            String[] fields = line.split("\t");
            if (fields[0].equals("T")) {
                int id = Integer.parseInt(fields[1]); Template.ItemTemplate t = Manager.ITEM_TEMPLATES.get(id);
                t.type = Byte.parseByte(fields[2]); t.gender = Byte.parseByte(fields[3]); levels.put(id,Integer.parseInt(fields[4]));
            } else if (fields[0].equals("S")) {
                int sid = Integer.parseInt(fields[1]), tid = Integer.parseInt(fields[2]), id = Integer.parseInt(fields[3]);
                Shop shop = shops.computeIfAbsent(sid, key -> { Shop s = new Shop(); s.id = key; Manager.SHOPS.add(s); return s; });
                TabShop tab = shop.tabShops.stream().filter(t -> t.id == tid).findFirst().orElse(null);
                if (tab == null) { tab = new TabShop(); tab.id = tid; tab.shop = shop; shop.tabShops.add(tab); }
                ItemShop entry = new ItemShop(); entry.temp = Manager.ITEM_TEMPLATES.get(id); tab.itemShops.add(entry);
                for (String value : fields[4].split(",")) {
                    String[] pair = value.split(":"); int opt = Integer.parseInt(pair[0]), param = Integer.parseInt(pair[1]);
                    entry.options.add(new Item.ItemOption(opt,param));
                    if (Arrays.stream(BASE[entry.temp.type]).anyMatch(i -> i == opt)) maximums.computeIfAbsent(id,k -> new HashMap<>()).merge(opt,param,Math::max);
                }
            } else catalogs.add(fields);
        }
        int count = 0;
        for (String[] fields : catalogs) {
            int planet = Integer.parseInt(fields[1]), type = Integer.parseInt(fields[2]);
            int[] ids = Arrays.stream(fields[3].split(",")).mapToInt(Integer::parseInt).toArray();
            check(Arrays.equals(ids,SKHCapsuleCatalog.vipItems(planet,type)),"Catalog must match SQL levels");
            for (int i = 0; i < ids.length; i++) {
                int id = ids[i]; check(levels.get(id) == i+1,"Actual SQL level order");
                for (int set : SKHCapsuleCatalog.sets(planet)) {
                    Item out = ItemService.gI().createItemSKHVipMax(id,set,planet);
                    Map<Integer,Integer> expected = new HashMap<>(maximums.get(id)); expected.put(set,1); expected.put(30,1);
                    for (int effect : ActivationSet.effectIds(set)) expected.put(effect,1);
                    Map<Integer,Integer> actual = new HashMap<>();
                    for (Item.ItemOption option : out.itemOptions) check(actual.put(option.optionTemplate.id,option.param) == null,"Duplicate option");
                    check(actual.equals(expected),"Exact SQL maximum stats and set for template " + id);
                    check(out.template.id == id && out.template.type == type && out.quantity == 1,"Output template"); count++;
                }
            }
        }
        check(count == 900,"All 180 planet/type/level slots times five sets");
        check(maximums.get(257).get(0) == 2200 && maximums.get(261).get(0) == 2150 && maximums.get(265).get(0) == 2250 && maximums.get(281).get(14) == 12,"High-level SQL maxima");
        System.out.println("PASS: 900 outputs from actual SQL village joins, all 12 levels and 15 radar set cases");
    }
    private static void check(boolean valid,String text) { if (!valid) throw new AssertionError(text); }
}
