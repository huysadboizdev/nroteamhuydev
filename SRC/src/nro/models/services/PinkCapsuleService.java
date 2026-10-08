package nro.models.services;

import java.util.function.IntUnaryOperator;
import java.util.List;
import nro.models.boss.Boss;
import nro.models.consts.ConstItem;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.utils.Util;
import nro.models.services_func.TransactionService;

/** Ruby capsules are committed to the player's existing inventory save. */
public final class PinkCapsuleService {
    private PinkCapsuleService() {}

    public static int rollRuby(IntUnaryOperator randomBounded) {
        return randomBounded.applyAsInt(100) < 80
                ? 1 + randomBounded.applyAsInt(5)
                : 6 + randomBounded.applyAsInt(95);
    }

    public static boolean open(Player player, Item capsule) {
        if (player == null) return false;
        int reward;
        synchronized (player) {
            if (player.inventory == null || player.inventory.itemsBag == null
                    || capsule == null || capsule.template == null
                    || capsule.template.id != ConstItem.CAPSULE_HONG || capsule.quantity <= 0) return false;
            int slot = -1;
            for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
                if (player.inventory.itemsBag.get(i) == capsule) { slot = i; break; }
            }
            if (slot < 0) return false;
            if (RubyGiftService.hasPending(player)) return false;
            if (player.tradeWVP || TransactionService.isTrading(player)) {
                notifyRejected(player, "Hãy kết thúc giao dịch trước khi mở Capsule.");
                return false;
            }
            if (player.inventory.ruby < 0) {
                notifyRejected(player, "Số dư hồng ngọc không hợp lệ, không thể mở Capsule.");
                return false;
            }
            reward = rollRuby(Util::nextInt);
            final int balance;
            try { balance = Math.addExact(player.inventory.ruby, reward); }
            catch (ArithmeticException overflow) {
                notifyRejected(player, "Hồng ngọc đã đạt giới hạn, hãy sử dụng bớt trước khi mở Capsule.");
                return false;
            }
            if ((long)balance+VipRewardCatalog.reservedRuby(player)>Integer.MAX_VALUE) {
                notifyRejected(player,"Cần chừa chỗ cho hồng ngọc của gói VIP đang chờ đối soát.");
                return false;
            }
            // Prepare on copies; a full bag must never consume the capsule or ruby.
            List<Item> resultBag = InventoryService.gI().copyItemsBag(player);
            if (capsule.quantity == 1) resultBag.set(slot, ItemService.gI().createItemNull());
            else resultBag.get(slot).quantity--;
            Item ticket = ItemService.gI().createNewItem(ConstItem.VE_TANG_HONG_NGOC, 1);
            if (!InventoryService.gI().addItemList(resultBag, ticket)) {
                notifyRejected(player, "Hành trang không đủ chỗ nhận vé tặng hồng ngọc.");
                return false;
            }
            capsule.quantity--;
            player.inventory.itemsBag = resultBag;
            player.inventory.ruby = balance;
        }
        // Notification failure must never replay the consumed capsule or the roll.
        synchronized (player) {
            try {
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendMoney(player);
                Service.gI().sendThongBao(player, "Bạn nhận được " + reward + " hồng ngọc và 1 vé tặng hồng ngọc");
            } catch (Exception packetFailure) {
                // The next inventory refresh sends the already committed state.
            }
        }
        return true;
    }

    private static void notifyRejected(Player player, String message) {
        try { Service.gI().sendThongBao(player, message); }
        catch (Exception packetFailure) { /* No inventory mutation has occurred. */ }
    }

    public static void dropShared(Boss boss) {
        for (int i = 0; i < 5; i++) {
            int x = Math.max(0, Math.min(boss.zone.map.mapWidth - 1, boss.location.x + (i - 2) * 20));
            int y = boss.zone.map.yPhysicInTop(x, boss.location.y - 24);
            ItemMap drop = new ItemMap(boss.zone, ConstItem.CAPSULE_HONG, 1, x, y, -1);
            Service.gI().dropItemMap(boss.zone, drop);
        }
    }
}
