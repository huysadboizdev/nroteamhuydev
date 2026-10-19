package nro.models.services;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.item.DisabledItemRules;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.shop.ItemShop;
import nro.models.shop.Shop;
import nro.models.shop.TabShop;

/** Level 8-12 shop gear drops for the requested boss group. */
final class BossShopLevelLoot {
    private static final int DROP_CHANCE_BP = 7000;
    private static final int MIN_LEVEL = 8;
    private static final int MAX_LEVEL = 12;
    private static final int MIN_STAT_BONUS_PERCENT = 5;
    private static final int MAX_STAT_BONUS_PERCENT = 15;
    private static final Set<Integer> ELIGIBLE_BOSSES = Set.of(
            BossID.SO_1, BossID.SO_2, BossID.SO_3, BossID.SO_4, BossID.TIEU_DOI_TRUONG,
            BossID.FIDE, BossID.ANDROID_19, BossID.DR_KORE, BossID.ANDROID_13,
            BossID.ANDROID_14, BossID.ANDROID_15, BossID.PIC, BossID.POC,
            BossID.KING_KONG, BossID.KUKU, BossID.MAP_DAU_DINH, BossID.RAMBO);
    private static final Set<Integer> BASE_STAT_OPTIONS = Set.of(0, 14, 22, 23, 27, 28, 47);

    private BossShopLevelLoot() {}

    static boolean drop(Boss boss, Player killer, IntUnaryOperator random) {
        if (boss == null || boss.isQuestSupport() || !ELIGIBLE_BOSSES.contains(boss.questBossId())
                || (boss.questBossId() == BossID.FIDE && boss.currentLevel != boss.data.length - 1)
                || boss.zone == null || boss.zone.map == null || boss.zone.map.mapWidth <= 0
                || boss.location == null) return false;
        Player owner = MiniBossPinkCapsuleDrop.creditedPlayer(killer);
        if (owner == null || owner.gender < 0 || owner.gender > 2) return false;

        Map<Integer, List<ItemShop>> candidates = candidatesByLevel(owner.gender);
        if (candidates.isEmpty() || roll(random, 10000) >= DROP_CHANCE_BP) return false;

        List<Integer> levels = new ArrayList<>(candidates.keySet());
        int level = levels.get(roll(random, levels.size()));
        List<ItemShop> rows = candidates.get(level);
        ItemShop selected = rows.get(roll(random, rows.size()));
        Item equipment = ItemService.gI().createItemFromItemShop(selected);
        int bonus = MIN_STAT_BONUS_PERCENT + roll(random, MAX_STAT_BONUS_PERCENT - MIN_STAT_BONUS_PERCENT + 1);
        applyBaseStatBonus(equipment, bonus);

        int x = Math.max(0, Math.min(boss.zone.map.mapWidth - 1, boss.location.x));
        int y = boss.zone.map.yPhysicInTop(x, boss.location.y - 24);
        if (y < 0) return false;
        ItemMap item = new ItemMap(boss.zone, equipment.template.id, 1, x, y, owner.id,
                true, equipment.itemOptions);
        try {
            Service.gI().dropItemMap(boss.zone, item);
        } catch (Exception packetFailure) {
            // The item was already installed in the zone by ItemMap; never publish a duplicate.
        }
        return true;
    }

    private static Map<Integer, List<ItemShop>> candidatesByLevel(int gender) {
        Map<Integer, ItemShop> unique = new HashMap<>();
        List<Shop> shops = Manager.SHOPS;
        if (shops == null) return Map.of();
        for (Shop shop : shops) {
            if (shop == null || shop.tabShops == null) continue;
            for (TabShop tab : shop.tabShops) {
                if (tab == null || tab.itemShops == null) continue;
                for (ItemShop row : tab.itemShops) {
                    if (!eligibleShopRow(row, gender)) continue;
                    unique.putIfAbsent((int) row.temp.id, row);
                }
            }
        }
        Map<Integer, List<ItemShop>> byLevel = new LinkedHashMap<>();
        unique.values().stream()
                .sorted(Comparator.comparingInt((ItemShop row) -> row.temp.level)
                        .thenComparingInt(row -> row.temp.id))
                .forEach(row -> byLevel.computeIfAbsent((int) row.temp.level, ignored -> new ArrayList<>()).add(row));
        return byLevel;
    }

    private static boolean eligibleShopRow(ItemShop row, int gender) {
        if (row == null || row.temp == null || row.options == null || row.options.isEmpty()) return false;
        int type = row.temp.type;
        int level = row.temp.level;
        int itemGender = row.temp.gender;
        return type >= 0 && type <= 4 && level >= MIN_LEVEL && level <= MAX_LEVEL
                && (itemGender == gender || itemGender > 2)
                && !DisabledItemRules.disabled(row.temp.id);
    }

    private static void applyBaseStatBonus(Item item, int bonusPercent) {
        if (item == null || item.itemOptions == null) return;
        for (ItemOption option : item.itemOptions) {
            if (option == null || option.optionTemplate == null
                    || !BASE_STAT_OPTIONS.contains(option.optionTemplate.id)) continue;
            long scaled = Math.floorDiv((long) option.param * (100 + bonusPercent) + 99, 100);
            option.param = (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, scaled));
        }
    }

    private static int roll(IntUnaryOperator random, int bound) {
        int result = random.applyAsInt(bound);
        if (result < 0 || result >= bound) throw new IllegalArgumentException("random value outside bound " + bound);
        return result;
    }
}
