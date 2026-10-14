package nro.models.services;

import java.util.function.IntUnaryOperator;
import nro.models.item.Item;
import nro.models.item.ItemTime;
import nro.models.map.service.MapService;
import nro.models.mob.Mob;
import nro.models.player.Pet;
import nro.models.player.Player;

/** Credits energy from a real hit to the player's bag, including pet attacks. */
public final class KilisAbsorptionService {
    public static final int NORMAL_RATE_PERCENT = 30;
    public static final int BUFF_RATE_PERCENT = 60;
    private KilisAbsorptionService() {}

    public static void absorb(Mob mob, Player attacker, long removedHp, IntUnaryOperator roll) {
        if (removedHp <= 0 || mob == null || mob.zone == null || attacker == null
                || !MapService.gI().isMapCadic(mob.zone.map.mapId)) return;
        Player owner = attacker instanceof Pet ? ((Pet) attacker).master : attacker;
        if (owner == null || !owner.isPl() || owner.zone != mob.zone
                || attacker.zone != mob.zone || owner.inventory == null) return;
        synchronized (owner) {
            Item jar = InventoryService.gI().findItemBag(owner, 1795);
            if (jar == null || !jar.isNotNullItem() || jar.quantity <= 0) return;
            ItemTime timer = owner.itemTime;
            boolean boosted = timer != null && timer.isUseKilis
                    && !nro.models.utils.Util.canDoWithTime(timer.lastTimeUseKilis,
                            timer.timeLengthKilis > 0 ? timer.timeLengthKilis : ItemTime.TIME_KILIS);
            if (roll.applyAsInt(100) >= (boosted ? BUFF_RATE_PERCENT : NORMAL_RATE_PERCENT)) return;
            Item.ItemOption option = jar.getOptionById(250);
            if (option == null) jar.itemOptions.add(new Item.ItemOption(250, 1));
            else {
                if (option.param == Integer.MAX_VALUE) return;
                option.param++;
            }
            InventoryService.gI().sendItemBags(owner);
            Service.gI().sendThongBao(owner, "Bạn vừa nhận được 1 chỉ số Kilis!");
        }
    }
}
