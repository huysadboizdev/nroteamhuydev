package nro.models.services;

import java.util.*;
import java.util.function.IntUnaryOperator;
import nro.models.boss.Boss;
import nro.models.item.Item.ItemOption;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.shop.BossEquipmentBonusRoll;
import nro.models.utils.Util;

/** One immutable drop policy per life; native rewards retain all non-loot effects. */
public final class BossAdminDropService {
    private BossAdminDropService() {}
    private static final Map<Boss, Boolean> CLAIMED = Collections.synchronizedMap(new WeakHashMap<>());
    private record Scope(Boss boss, boolean replace, boolean nativeEquipment) {}
    private static final ThreadLocal<Scope> SCOPE = new ThreadLocal<>();

    public static void reset(Boss boss) { synchronized (boss) { CLAIMED.remove(boss); } }
    /** Death paths with no native reward keep that baseline while allowing configured loot. */
    public static void rewardWithoutDefault(Boss boss, Player killer) {
        if (boss.seasonalAllowed()) reward(boss, killer, () -> {});
    }
    public static void reward(Boss boss, Player killer, Runnable nativeReward) {
        reward(boss, killer, nativeReward, BossAdminConfig.config(boss), Util::nextInt);
    }
    static void reward(Boss boss, Player killer, Runnable nativeReward,
            BossAdminConfig.Config policy, IntUnaryOperator random) {
        // Bone feeding acquires the owner before the wolf; keep that order
        // before acquiring the boss monitor used by the reward claim.
        Player owner = MiniBossPinkCapsuleDrop.creditedPlayer(killer);
        if (owner == null) return;
        if (boss instanceof nro.models.boss.Boss_mini.SoiHecQuyn) {
            synchronized (owner) { rewardClaimed(boss, killer, nativeReward, policy, random); }
        } else rewardClaimed(boss, killer, nativeReward, policy, random);
    }
    private static void rewardClaimed(Boss boss, Player killer, Runnable nativeReward,
            BossAdminConfig.Config policy, IntUnaryOperator random) {
        synchronized (boss) {
            if (MiniBossPinkCapsuleDrop.creditedPlayer(killer) == null || !boss.isQuestSupport()
                    && (boss.zone == null || boss.zone.map == null || boss.location == null)) return;
            if (CLAIMED.putIfAbsent(boss, true) != null) return;
            Scope previous = SCOPE.get();
            boolean custom = policy != null && !"default".equals(policy.dropMode()) && !boss.isQuestSupport();
            try {
                boolean replace = custom && "replace".equals(policy.dropMode());
                SCOPE.set(new Scope(boss, replace, RequestedBossEquipment.replacesNativeDivine(boss)));
                nativeReward.run();
                // Restore outer scope before custom construction: nested boss rewards remain isolated.
                SCOPE.set(new Scope(boss, false, false));
                if (!replace) {
                    RequestedBossEquipment.drop(boss, killer, random);
                    BossShopLevelLoot.drop(boss, killer, random);
                }
                Player owner = MiniBossPinkCapsuleDrop.creditedPlayer(killer);
                if (custom && owner != null && boss.zone != null && boss.zone.map != null && boss.location != null) {
                    for (BossAdminConfig.Rule rule : roll(policy.drops(), random)) {
                        int quantity = rule.quantityMin() + random.applyAsInt(rule.quantityMax() - rule.quantityMin() + 1);
                        int x = boss.location.x;
                        List<ItemOption> options = new ArrayList<>();
                        for (ItemOption option : rule.options()) options.add(new ItemOption(option));
                        var template = ItemService.gI().getTemplate((short) rule.itemId());
                        boolean sealedShopGear = rule.itemId() >= 230 && rule.itemId() <= 281 && hasSeal(rule);
                        boolean divineGear = rule.itemId() >= 555 && rule.itemId() <= 567;
                        if (template != null && template.type >= 0 && template.type <= 4
                                && (sealedShopGear || divineGear)) {
                            BossEquipmentBonusRoll.apply(options, template.type);
                        }
                        ItemMap item = new ItemMap(boss.zone, rule.itemId(), quantity, x,
                                boss.zone.map.yPhysicInTop(x, boss.location.y - 24), owner.id, true, options);
                        Service.gI().dropItemMap(boss.zone, item);
                    }
                }
            } finally { if (previous == null) SCOPE.remove(); else SCOPE.set(previous); }
        }
    }
    /** Captured before a constructor adds anything to the zone; suppression lasts forever. */
    public static boolean suppressConstruction(int id) {
        Scope scope = SCOPE.get();
        return scope != null && (scope.replace && !mandatory(id)
                || scope.nativeEquipment && id >= 555 && id <= 567);
    }
    private static boolean mandatory(int id) {
        // Task NPC object, quest dragon balls and tournament/Namek objectives.
        return id == 74 || id == 78 || id == 726 || id == 992 || id >= 14 && id <= 20;
    }
    public static boolean mayPublish(ItemMap item) { return item != null && !item.bossAdminSuppressed; }
    static List<BossAdminConfig.Rule> roll(List<BossAdminConfig.Rule> rules, IntUnaryOperator random) {
        List<BossAdminConfig.Rule> out = new ArrayList<>();
        Map<String,List<BossAdminConfig.Rule>> groups = new LinkedHashMap<>();
        for (BossAdminConfig.Rule rule : rules) {
            if (rule.group().isEmpty()) {
                if (passes(boostedChance(rule), random)) out.add(rule);
            }
            else groups.computeIfAbsent(rule.group(), key -> new ArrayList<>()).add(rule);
        }
        for (List<BossAdminConfig.Rule> group : groups.values()) {
            boolean allEquipment = group.stream().allMatch(BossAdminDropService::isTargetEquipment);
            int minimumChance = group.stream().mapToInt(BossAdminDropService::minimumEquipmentChance).max().orElse(0);
            int chance = allEquipment ? Math.max(group.get(0).chanceBp(), minimumChance) : group.get(0).chanceBp();
            if (!passes(chance, random)) continue;
            // For mixed groups, triple target gear's relative weight while preserving the group's chance.
            int[] weights = selectionWeights(group, allEquipment);
            int total = 0;
            for (int weight : weights) total = Math.addExact(total, weight);
            int pick = random.applyAsInt(total);
            for (int i = 0; i < group.size(); i++) {
                pick -= weights[i];
                if (pick < 0) { out.add(group.get(i)); break; }
            }
        }
        return out;
    }
    private static int boostedChance(BossAdminConfig.Rule rule) {
        return Math.max(rule.chanceBp(), minimumEquipmentChance(rule));
    }
    private static int minimumEquipmentChance(BossAdminConfig.Rule rule) {
        boolean sealed = hasSeal(rule);
        if (rule.itemId() >= 555 && rule.itemId() <= 567) return sealed ? 1000 : 2000;
        return sealed ? 4000 : 0;
    }
    private static int[] selectionWeights(List<BossAdminConfig.Rule> group, boolean allEquipment) {
        long[] raw = new long[group.size()];
        long rawTotal = 0;
        for (int i = 0; i < group.size(); i++) {
            BossAdminConfig.Rule rule = group.get(i);
            raw[i] = (long) rule.weight() * (!allEquipment && isTargetEquipment(rule) ? 3 : 1);
            rawTotal += raw[i];
        }
        long maxTotal = Integer.MAX_VALUE - group.size();
        long scale = Math.max(1, (rawTotal + maxTotal - 1) / maxTotal);
        int[] weights = new int[group.size()];
        for (int i = 0; i < raw.length; i++) weights[i] = (int) Math.max(1, raw[i] / scale);
        return weights;
    }
    private static boolean isTargetEquipment(BossAdminConfig.Rule rule) {
        return minimumEquipmentChance(rule) > 0;
    }
    private static boolean hasSeal(BossAdminConfig.Rule rule) {
        return rule.options().stream().anyMatch(option -> option != null && option.optionTemplate != null
                && option.optionTemplate.id >= 34 && option.optionTemplate.id <= 36);
    }
    private static boolean passes(int chance, IntUnaryOperator random) {
        return chance == 10000 || chance > 0 && random.applyAsInt(10000) < chance;
    }
}
