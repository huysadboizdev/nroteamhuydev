package nro.models.services;

import java.util.function.IntUnaryOperator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
    private static final long INVENTORY_SYNC_DELAY_MS = 500L;
    private static final ConcurrentMap<Player, PendingSync> PENDING_SYNC = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService SYNC_SCHEDULER = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "kilis-inventory-sync");
        thread.setDaemon(true);
        return thread;
    });
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
            queueInventorySync(owner);
        }
    }

    private static void queueInventorySync(Player owner) {
        PendingSync pending = PENDING_SYNC.computeIfAbsent(owner, PendingSync::new);
        synchronized (pending) {
            pending.gains++;
            if (!pending.scheduled) schedule(pending);
        }
    }

    private static void schedule(PendingSync pending) {
        pending.scheduled = true;
        SYNC_SCHEDULER.schedule(() -> flush(pending), INVENTORY_SYNC_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private static void flush(PendingSync pending) {
        int gains;
        synchronized (pending) {
            gains = pending.gains;
            pending.gains = 0;
        }
        try {
            if (gains > 0) {
                synchronized (pending.owner) {
                    InventoryService.gI().sendItemBags(pending.owner);
                    String notice = gains == 1
                            ? "Bạn vừa nhận được 1 chỉ số Kilis!"
                            : "Bạn vừa nhận được " + gains + " chỉ số Kilis!";
                    Service.gI().sendThongBao(pending.owner, notice);
                }
            }
        } finally {
            synchronized (pending) {
                if (pending.gains > 0) schedule(pending);
                else {
                    pending.scheduled = false;
                    PENDING_SYNC.remove(pending.owner, pending);
                }
            }
        }
    }

    private static final class PendingSync {
        final Player owner;
        int gains;
        boolean scheduled;

        PendingSync(Player owner) { this.owner = owner; }
    }
}
