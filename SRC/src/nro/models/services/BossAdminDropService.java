package nro.models.services;

import java.util.*;
import java.util.function.IntUnaryOperator;
import nro.models.boss.Boss;
import nro.models.map.ItemMap;
import nro.models.player.Player;
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
                if (!replace) RequestedBossEquipment.drop(boss, killer, random);
                Player owner = MiniBossPinkCapsuleDrop.creditedPlayer(killer);
                if (custom && owner != null && boss.zone != null && boss.zone.map != null && boss.location != null) {
                    for (BossAdminConfig.Rule rule : roll(policy.drops(), random)) {
                        int quantity = rule.quantityMin() + random.applyAsInt(rule.quantityMax() - rule.quantityMin() + 1);
                        int x = boss.location.x;
                        ItemMap item = new ItemMap(boss.zone, rule.itemId(), quantity, x,
                                boss.zone.map.yPhysicInTop(x, boss.location.y - 24), owner.id, true, rule.options());
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
            if (rule.group().isEmpty()) { if (passes(rule.chanceBp(), random)) out.add(rule); }
            else groups.computeIfAbsent(rule.group(), key -> new ArrayList<>()).add(rule);
        }
        for (List<BossAdminConfig.Rule> group : groups.values()) {
            if (!passes(group.get(0).chanceBp(), random)) continue;
            int total = 0; for (BossAdminConfig.Rule rule : group) total = Math.addExact(total, rule.weight());
            int pick = random.applyAsInt(total);
            for (BossAdminConfig.Rule rule : group) { pick -= rule.weight(); if (pick < 0) { out.add(rule); break; } }
        }
        return out;
    }
    private static boolean passes(int chance, IntUnaryOperator random) {
        return chance == 10000 || chance > 0 && random.applyAsInt(10000) < chance;
    }
}
