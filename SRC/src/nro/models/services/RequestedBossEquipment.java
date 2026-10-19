package nro.models.services;

import java.util.function.IntUnaryOperator;
import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.player.Player;

/** Requested equipment pools; rates are basis points per credited defeat. */
public final class RequestedBossEquipment {
    private RequestedBossEquipment() {}
    // Per credited defeat: 40% sealed shop gear, 20% divine, 10% sealed divine.
    public static final int SEALED_SHOP_BP = 4000, DIVINE_BP = 2000, SEALED_DIVINE_BP = 1000;
    public static boolean hasEquipmentDrops(Boss boss) { return categories(boss) != 0; }
    static int categories(Boss boss) {
        if (boss == null || boss.isQuestSupport()) return 0;
        if (nro.models.boss.PrisonBill.PrisonBillBossCatalog.contains(boss.questBossId())) return 7;
        return switch (boss.questBossId()) {
            case BossID.XEN_BO_HUNG -> 7;
            case BossID.MABU -> 6;
            case BossID.SIEU_BO_HUNG, BossID.BLACK_GOKU, BossID.ZAMASU,
                    BossID.CUMBER, BossID.COOLER, BossID.DRABURA, BossID.DRABURA_2, BossID.DRABURA_3 -> 7;
            default -> 0;
        };
    }
    static boolean replacesNativeDivine(Boss boss) { return (categories(boss) & 6) != 0; }
    static Item choose(Player owner, int category, IntUnaryOperator random) {
        int chance = switch(category) { case 0 -> SEALED_SHOP_BP; case 1 -> DIVINE_BP; case 2 -> SEALED_DIVINE_BP; default -> throw new IllegalArgumentException("Invalid category"); };
        int roll = random.applyAsInt(10000);
        if (roll < 0 || roll >= 10000) throw new IllegalArgumentException("Invalid drop roll");
        return roll < chance ? SealedBossEquipmentLoot.buildCategory(owner, category, random) : null;
    }
    static void drop(Boss boss, Player killer, IntUnaryOperator random) {
        int mask = categories(boss); Player owner = MiniBossPinkCapsuleDrop.creditedPlayer(killer);
        if (mask == 0 || owner == null || boss.zone == null || boss.zone.map == null || boss.location == null || boss.zone.map.mapWidth <= 0) return;
        int x = Math.max(0, Math.min(boss.zone.map.mapWidth - 1, boss.location.x));
        int y = boss.zone.map.yPhysicInTop(x, boss.location.y - 24); if (y < 0) return;
        for (int category = 0; category < 3; category++) {
            if ((mask & (1 << category)) == 0) continue;
            Item equipment = choose(owner, category, random); if (equipment == null) continue;
            ItemMap item = new ItemMap(boss.zone, equipment.template.id, 1, x, y, owner.id, true, equipment.itemOptions);
            try { Service.gI().dropItemMap(boss.zone, item); }
            catch (Exception packetFailure) { /* The zone owns the completed item; do not create it again. */ }
        }
    }
}
