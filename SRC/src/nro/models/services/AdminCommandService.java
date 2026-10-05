package nro.models.services;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import nro.models.boss.Boss;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.map.service.MapService;
import nro.models.network.Message;
import nro.models.network.MySession;
import nro.models.player.Player;
import nro.models.utils.Logger;

/** Server-side authorization and session-scoped admin boss selections. */
public final class AdminCommandService {
    public static final int BOSS_MENU = 3;
    public static final int MAX_BOSS_ROWS = 127; // Unity client reads a signed byte.
    private static final long BOSS_LIST_TTL_MS = 120_000;
    private static final AtomicInteger NEXT_SELECTION = new AtomicInteger(1);
    private static final Map<Player, BossSnapshot> BOSS_LISTS = new WeakHashMap<>();
    private record BossSnapshot(WeakReference<MySession> session, BossManager manager,
            long created, Map<Integer, Boss> selections) {}

    private AdminCommandService() {}

    public static boolean isPrivilegedMenu(int menu) {
        return menu == nro.models.consts.ConstNpc.MENU_ADMIN
                || menu == nro.models.consts.ConstNpc.MENU_FIND_PLAYER
                || menu == nro.models.consts.ConstNpc.BAN_PLAYER
                || menu == nro.models.consts.ConstNpc.BUFF_PET || menu == 206783;
    }

    public static boolean isAuthorized(Player player) {
        return player != null && player.session != null && player.session.player == player
                && player.session.isAdmin;
    }

    public static boolean requireAdmin(Player player) {
        if (isAuthorized(player)) return true;
        if (player != null) Service.gI().sendThongBao(player, "Chức năng này chỉ dành cho admin.");
        return false;
    }

    public static void recoverSelf(Player player) {
        if (!requireAdmin(player)) return;
        var special = player.newSkill;
        if (special != null && !special.beginAdminRecovery(() -> isAuthorized(player))) {
            requireAdmin(player);
            return;
        }
        try {
            synchronized (player) {
              if (player.session == null) return;
              synchronized (player.session) {
                if (!requireAdmin(player) || player.nPoint == null || player.effectSkill == null) return;
                boolean dead = player.isDie();
                EffectSkillService.gI().clearAdminControlEffects(player);
                player.nPoint.calPoint();
                if (dead) Service.gI().hsChar(player, player.nPoint.hpMax, player.nPoint.mpMax);
                else {
                    player.nPoint.setFullHpMp();
                    PlayerService.gI().sendInfoHpMp(player);
                    Service.gI().Send_Info_NV(player);
                    Service.gI().sendInfoPlayerEatPea(player);
                }
                Service.gI().releaseCooldownSkill(player);
              }
            }
        } finally {
            if (special != null) special.endAdminRecovery();
        }
    }

    public static void completeKol(Player player) {
        KolQuestService.gI().completeCurrentForAdmin(player);
    }

    public static nro.models.player_system.Template.ItemTemplate validateItem(int itemId, int quantity) {
        if (itemId < 0 || itemId > Short.MAX_VALUE || quantity <= 0)
            throw new IllegalArgumentException("ID vật phẩm hoặc số lượng không hợp lệ");
        var template = ItemService.gI().getTemplate(itemId);
        if (template == null || template.id != itemId)
            throw new IllegalArgumentException("Vật phẩm không có trong catalog");
        if ((!template.isUpToUp || itemId == 517 || itemId == 518)
                && quantity > nro.models.player.Inventory.MAX_ITEMS_BAG)
            throw new IllegalArgumentException("Số lượng đồ không cộng dồn vượt sức chứa hành trang");
        return template;
    }

    public static nro.models.item.Item.ItemOption validateOption(int id, int value) {
        if (id < 0 || id > 250 || value < 0) throw new IllegalArgumentException("Option không hợp lệ");
        var template = ItemService.gI().getItemOptionTemplate(id);
        if (template == null || template.id != id) throw new IllegalArgumentException("Option không có trong catalog");
        return new nro.models.item.Item.ItemOption(template,value);
    }

    /** Returns the number actually accepted. Each item owns independent mutable options. */
    public static int grantItems(Player actor, Player recipient, int itemId, int quantity,
            List<nro.models.item.Item.ItemOption> options) {
        if (!requireAdmin(actor)) return 0;
        if (recipient == null || recipient.inventory == null || options == null)
            throw new IllegalArgumentException("Người nhận hoặc option không hợp lệ");
        var template = validateItem(itemId,quantity);
        List<nro.models.item.Item.ItemOption> validated = new ArrayList<>();
        for (var option : options) {
            if (option == null || option.optionTemplate == null) throw new IllegalArgumentException("Option không hợp lệ");
            validated.add(validateOption(option.optionTemplate.id,option.param));
        }
        List<nro.models.item.Item.ItemOption> source = validated.isEmpty()
                ? ItemService.gI().getListOptionItemShop((short)itemId) : validated;
        boolean batchQuantity = template.isUpToUp && itemId != 517 && itemId != 518
                && source.stream().noneMatch(option -> option.optionTemplate != null
                    && (option.optionTemplate.id == 1 || option.optionTemplate.id == 31));
        if (!batchQuantity && quantity > nro.models.player.Inventory.MAX_ITEMS_BAG)
            throw new IllegalArgumentException("Số lượng vật phẩm xử lý từng chiếc vượt giới hạn");
        synchronized (recipient) {
            if (!requireAdmin(actor) || AdminPlayerActions.hasRenamePendingId(recipient.id)
                    || RubyGiftService.hasPendingId(recipient.id)
                    || (recipient.session != null && (RubyGiftService.hasPendingAccount(recipient.session.userId)
                        || KolQuestService.hasProgressPendingAccount(recipient.session.userId)
                        || AdminPlayerActions.hasRenamePendingAccount(recipient.session.userId)))
                    || RubyGiftService.hasPending(recipient) || recipient.kolRewardPending
                    || recipient.walletPurchasePending != null || recipient.kolProgressPending || KolQuestService.hasProgressPendingId(recipient.id)) {
                Service.gI().sendThongBao(actor,"Người nhận đang có giao dịch chờ đối soát."); return 0;
            }
            int accepted = 0;
            for (int i=0, count=batchQuantity ? 1 : quantity; i<count; i++) {
                var item = ItemService.gI().createNewItem((short)itemId);
                item.quantity = batchQuantity ? quantity : 1;
                if (!source.isEmpty()) {
                    item.itemOptions = new ArrayList<>();
                    for (var option : source) item.itemOptions.add(new nro.models.item.Item.ItemOption(option));
                }
                int currencyBefore = template.type == 10 ? recipient.inventory.gem : recipient.inventory.ruby;
                if (!InventoryService.gI().addItemBag(recipient,item)) break;
                if (template.type == 10 || template.type == 34) {
                    int currencyAfter = template.type == 10 ? recipient.inventory.gem : recipient.inventory.ruby;
                    accepted += Math.max(0,currencyAfter-currencyBefore);
                } else accepted += batchQuantity ? quantity : 1;
            }
            if (accepted > 0) InventoryService.gI().sendItemBags(recipient);
            return accepted;
        }
    }

    private static boolean visible(Boss boss) {
        if (boss == null || boss.data == null || boss.data.length == 0 || boss.data[0] == null
                || boss.data[0].getMapJoin() == null || boss.data[0].getMapJoin().length == 0) return false;
        int map = boss.data[0].getMapJoin()[0];
        MapService service = MapService.gI();
        return !service.isMapBossFinal(map) && !service.isMapHuyDiet(map) && !service.isMapCadic(map)
                && !service.isMapYardart(map) && !service.isMapMaBu(map) && !service.isMapBlackBallWar(map);
    }

    public static void showBossList(Player player, BossManager manager) {
        if (!requireAdmin(player)) return;
        MySession session = player.session;
        synchronized (BOSS_LISTS) { BOSS_LISTS.remove(player); }
        List<Boss> rows = new ArrayList<>();
        for (Boss boss : manager.getBosses().toArray(Boss[]::new)) if (visible(boss)) rows.add(boss);
        if (rows.isEmpty()) { Service.gI().sendThongBao(player,"Danh sách boss đang trống."); return; }
        boolean truncated = rows.size() > MAX_BOSS_ROWS;
        if (truncated) rows = new ArrayList<>(rows.subList(0, MAX_BOSS_ROWS));
        Map<Integer, Boss> selections = new HashMap<>();
        Message msg = null;
        try {
            msg = new Message(-96);
            msg.writer().writeByte(0); msg.writer().writeUTF("Boss"); msg.writer().writeByte(rows.size());
            for (Boss boss : rows) {
                int selection = NEXT_SELECTION.getAndUpdate(n -> n == Integer.MAX_VALUE ? 1 : n + 1);
                selections.put(selection, boss);
                msg.writer().writeInt(selection); msg.writer().writeInt(selection);
                short[] outfit = boss.data[0].getOutfit();
                msg.writer().writeShort(outfit[0]);
                if (session.version >= 214) msg.writer().writeShort(-1);
                msg.writer().writeShort(outfit[1]); msg.writer().writeShort(outfit[2]);
                msg.writer().writeUTF(boss.data[0].getName());
                msg.writer().writeUTF(String.valueOf(boss.bossStatus));
                Zone zone = boss.zone;
                msg.writer().writeUTF(zone != null && zone.map != null
                        ? zone.map.mapName + "(" + zone.map.mapId + ") khu " + zone.zoneId
                        : "Chưa xuất hiện hoặc đã rời khu");
            }
            if (!requireAdmin(player) || player.session != session) return;
            player.idMark.setMenuType(BOSS_MENU);
            synchronized (BOSS_LISTS) {
                long now = System.currentTimeMillis();
                BOSS_LISTS.entrySet().removeIf(e -> now - e.getValue().created() > BOSS_LIST_TTL_MS);
                BOSS_LISTS.put(player,new BossSnapshot(new WeakReference<>(session),manager,now,Map.copyOf(selections)));
            }
            player.sendMessage(msg);
            if (truncated) Service.gI().sendThongBao(player,"Bảng hiển thị tối đa 127 boss; hãy mở lại bảng để cập nhật.");
        } catch (Exception e) {
            synchronized (BOSS_LISTS) { BOSS_LISTS.remove(player); }
            Logger.logException(AdminCommandService.class,e);
            Service.gI().sendThongBao(player,"Không thể mở bảng boss. Hãy thử lại.");
        } finally { if (msg != null) msg.cleanup(); }
    }

    public static Boss resolveBossSelection(Player player, int selection) {
        if (!requireAdmin(player)) return null;
        BossSnapshot snapshot;
        synchronized (BOSS_LISTS) { snapshot = BOSS_LISTS.get(player); }
        if (player.idMark == null || player.idMark.getMenuType() != BOSS_MENU || snapshot == null
                || snapshot.session().get() != player.session
                || System.currentTimeMillis() - snapshot.created() > BOSS_LIST_TTL_MS) {
            stale(player,"Lựa chọn boss đã hết hạn"); return null;
        }
        Boss boss = snapshot.selections().get(selection);
        if (boss == null || !snapshot.manager().getBosses().contains(boss)) {
            stale(player,"Boss đã rời danh sách hoặc lựa chọn không hợp lệ"); return null;
        }
        if (boss.nPoint == null || boss.isDie()) { stale(player,"Boss đã chết"); return null; }
        Zone zone = boss.zone;
        if (zone == null || zone.map == null || boss.location == null || !zone.getBosses().contains(boss)
                || boss.location.x < 0 || boss.location.y < 0 || boss.location.x >= zone.map.mapWidth
                || boss.location.y >= zone.map.mapHeight) {
            stale(player,"Boss chưa xuất hiện hoặc đã rời khu"); return null;
        }
        return boss;
    }

    public static void challengeBoss(Player player, int selection) {
        Boss boss = resolveBossSelection(player, selection);
        if (boss == null) return;
        Zone zone = boss.zone;
        var location = boss.location;
        if (zone == null || zone.map == null || location == null) { stale(player,"Boss vừa rời khu"); return; }
        int x = location.x, y = location.y;
        if (!requireAdmin(player)) return;
        if (boss.isDie() || boss.zone != zone || boss.location != location || !zone.getBosses().contains(boss)
                || x != location.x || y != location.y || x < 0 || y < 0
                || x >= zone.map.mapWidth || y >= zone.map.mapHeight) {
            stale(player,"Boss vừa chuyển khu hoặc di chuyển"); return;
        }
        ChangeMapService.gI().changeMapYardrat(player,zone,x,y);
        if (boss.zone != zone || boss.isDie()) stale(player,"Boss vừa chết hoặc chuyển khu");
    }

    private static void stale(Player player, String reason) {
        Service.gI().sendThongBao(player,reason + ". Hãy mở lại bảng boss.");
    }
}
