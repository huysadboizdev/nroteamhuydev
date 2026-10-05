package nro.models.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import nro.models.database.PlayerDAO;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.Logger;

/** Explicit, confirmed bag cleanup. Automatic worn expiration is a separate operation. */
public final class TimedBagCleanupService {
    private static TimedBagCleanupService instance = new TimedBagCleanupService(PlayerDAO::saveChecked);
    private static final long CONFIRM_TIMEOUT = 120_000;
    private final Predicate<Player> saver;

    public TimedBagCleanupService(Predicate<Player> saver) { this.saver = Objects.requireNonNull(saver); }
    public static TimedBagCleanupService gI() { return instance; }

    private record Entry(int slot, Item item, short templateId, int quantity, long createTime,
                         List<Integer> options) {
        static Entry capture(int slot, Item item) {
            List<Integer> options = new ArrayList<>();
            if (item.itemOptions != null) for (Item.ItemOption option : item.itemOptions) {
                options.add(option == null || option.optionTemplate == null ? -1 : option.optionTemplate.id);
                options.add(option == null ? 0 : option.param);
            }
            return new Entry(slot, item, item.template.id, item.quantity, item.createTime, List.copyOf(options));
        }
    }
    public record Preview(long createdAt, List<Entry> entries) { }

    private boolean pending(Player p) {
        return RubyGiftService.hasPendingId(p.id) || AdminPlayerActions.hasRenamePendingId(p.id)
                || KolQuestService.hasProgressPendingId(p.id) || p.walletPurchasePending != null
                || p.kolRewardPending || p.kolProgressPending;
    }
    private boolean available(Player p) {
        return p != null && p.isPl() && !p.beforeDispose && !p.isOffline && p.inventory != null
                && p.idMark != null && p.idMark.isLoadedAllDataPlayer()
                && p.idMark.getPlayerTrade() == null
                && !nro.models.services_func.TransactionService.isTrading(p) && !pending(p);
    }
    private boolean timed(Item item) {
        return item != null && item.isNotNullItem() && item.quantity > 0 && item.getOptionById(93) != null;
    }
    private boolean protectedItem(Player p, Item item) {
        return p.inventory.itemsBody.contains(item) || p.inventory.itemsBox.contains(item)
                || (p.pet != null && p.pet.inventory != null && p.pet.inventory.itemsBody.contains(item));
    }
    private List<Entry> capture(Player p) {
        List<Entry> entries = new ArrayList<>();
        for (int slot = 0; slot < p.inventory.itemsBag.size(); slot++) {
            Item item = p.inventory.itemsBag.get(slot);
            if (timed(item) && !protectedItem(p, item)) entries.add(Entry.capture(slot, item));
        }
        return List.copyOf(entries);
    }
    public void cancel(Player p) {
        if (p != null) synchronized (p) { if (p.idMark != null) p.idMark.setTimedBagCleanupPreview(null); }
    }
    public String prepare(Player p, long now) {
        if (p == null) return "Không thể thực hiện.";
        synchronized (p) {
            cancel(p);
            if (!available(p)) return "Không thể dọn đồ lúc này. Hãy kết thúc giao dịch rồi thử lại.";
            List<Entry> entries = capture(p);
            if (entries.isEmpty()) return "Hành trang không có đồ có hạn sử dụng.";
            p.idMark.setTimedBagCleanupPreview(new Preview(now, entries));
            return "Bạn có muốn bỏ đồ có hạn sử dụng trong hành trang không?";
        }
    }
    public String confirm(Player p, long now) {
        if (p == null) return "Không thể thực hiện.";
        synchronized (p) {
            Preview preview = p.idMark == null ? null : p.idMark.getTimedBagCleanupPreview();
            cancel(p); // Consume once, including rejected confirmations.
            if (!available(p)) return "Không thể dọn đồ lúc này. Hãy kết thúc giao dịch rồi thử lại.";
            if (preview == null || now < preview.createdAt() || now - preview.createdAt() > CONFIRM_TIMEOUT)
                return "Hãy mở lại chức năng để xác nhận.";
            if (!preview.entries().equals(capture(p)))
                return "Hành trang đã thay đổi. Hãy mở lại chức năng để xác nhận.";
            for (Entry entry : preview.entries())
                p.inventory.itemsBag.set(entry.slot(), ItemService.gI().createItemNull());
            boolean saved = false;
            try { saved = saver.test(p); }
            catch (RuntimeException ex) { Logger.logException(TimedBagCleanupService.class, ex); }
            if (!saved) {
                for (Entry entry : preview.entries()) p.inventory.itemsBag.set(entry.slot(), entry.item());
                return "Không lưu được dữ liệu. Đồ của bạn vẫn được giữ nguyên, hãy thử lại sau.";
            }
            StringBuilder audit = new StringBuilder("TIMED_BAG_CLEANUP player=").append(p.id);
            for (Entry entry : preview.entries()) audit.append(" [slot=").append(entry.slot())
                    .append(" id=").append(entry.templateId()).append(" quantity=").append(entry.quantity())
                    .append(" createTime=").append(entry.createTime()).append(" options=")
                    .append(entry.options()).append(']');
            Logger.log(Logger.GREEN, audit.append('\n').toString());
            writeAudit(audit.toString());
            InventoryService.gI().sendItemBags(p);
            return "Đã bỏ " + preview.entries().size() + " ô đồ có hạn sử dụng trong hành trang.";
        }
    }

    private static synchronized void writeAudit(String text) {
        try {
            java.nio.file.Path directory = java.nio.file.Path.of("log");
            java.nio.file.Files.createDirectories(directory);
            java.nio.file.Files.writeString(directory.resolve("timed-bag-cleanup.log"),
                    nro.models.utils.TimeUtil.formatTime(System.currentTimeMillis(), "yyyy-MM-dd HH:mm:ss")
                            + " " + text, java.nio.charset.StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException ex) { Logger.logException(TimedBagCleanupService.class, ex); }
    }
    private record BodyUpdate(boolean changed, int removed) { }

    /** Called by the owner's update loop, also at home. Never scans bag or house box. */
    public void updateEquipped(Player p, long now) {
        if (p == null || !p.isPl()) return;
        synchronized (p) {
            if (p.beforeDispose || p.inventory == null || pending(p)) return;
            if (now >= p.lastTimedEquipmentCheck && now - p.lastTimedEquipmentCheck < 1000) return;
            p.lastTimedEquipmentCheck = now;
            BodyUpdate owner = expireBody(p, now);
            BodyUpdate pet = p.pet != null && p.pet.inventory != null ? expireBody(p.pet, now) : new BodyUpdate(false, 0);
            if (owner.changed() || pet.changed()) {
                InventoryService.gI().sendItemBody(p);
                if (owner.removed() + pet.removed() > 0) {
                    Service.gI().point(p);
                    Service.gI().sendThongBao(p, "Đồ đang mặc đã hết hạn và được gỡ bỏ.");
                }
                if (pet.changed()) Service.gI().showInfoPet(p);
            }
        }
    }
    private BodyUpdate expireBody(Player wearer, long now) {
        boolean changed = false;
        int removed = 0;
        for (int slot = 0; slot < wearer.inventory.itemsBody.size(); slot++) {
            Item item = wearer.inventory.itemsBody.get(slot);
            int before = timed(item) ? item.getOptionById(93).param : 0;
            if (ItemService.gI().isEquippedOutOfDateTime(item, now)) {
                wearer.inventory.itemsBody.set(slot, ItemService.gI().createItemNull());
                removed++;
                if (!wearer.isPet && slot == 12) Service.gI().sendPetFollow(wearer, (short) 0);
                if (!wearer.isPet && slot == 7 && wearer.newPet != null) {
                    ChangeMapService.gI().exitMap(wearer.newPet);
                    wearer.newPet.dispose();
                    wearer.newPet = null;
                }
                changed = true;
            } else if (timed(item) && before != item.getOptionById(93).param) changed = true;
        }
        if (removed > 0) {
            wearer.nPoint.calPoint();
            Service.gI().Send_Caitrang(wearer);
            if (!wearer.isPet) Service.gI().sendFlagBag(wearer);
        }
        return new BodyUpdate(changed, removed);
    }
}
