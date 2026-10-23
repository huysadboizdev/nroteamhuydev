package nro.models.services;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.IntUnaryOperator;
import nro.models.consts.ConstNpc;
import nro.models.item.ActivationSet;
import nro.models.item.Item;
import nro.models.item.SKHCapsuleCatalog;
import nro.models.map.service.NpcService;
import nro.models.network.MySession;
import nro.models.player.IDMark;
import nro.models.player.Player;
import nro.models.services_func.TransactionService;
import nro.models.shop_ky_gui.ConsignTransactions;
import nro.models.utils.Logger;
import nro.models.utils.Util;

/** One selected box, one menu callback, one durably saved reward. */
public final class ActivationGiftBoxService {
    public static final int ITEM_ID = 1538;
    public static final int MENU = 15380;
    private static final long MENU_LIFETIME_MS = 120_000;
    private static final int[][] DIVINE = {
        {555, 556, 562, 563, 561}, {557, 558, 564, 565, 561}, {559, 560, 566, 567, 561}
    };
    private static final Map<Player, Selection> selections = Collections.synchronizedMap(new WeakHashMap<>());
    private record Selection(WeakReference<Item> box, WeakReference<MySession> session, long time) { }

    private ActivationGiftBoxService() { }

    public static Item createReward(int mode) {
        return createReward(mode, bound -> Util.nextInt(bound));
    }

    static Item createReward(int mode, IntUnaryOperator random) {
        if (mode < 0 || mode > 2) throw new IllegalArgumentException("Lựa chọn hộp kích hoạt không hợp lệ.");
        int planet = random.applyAsInt(3), type = random.applyAsInt(5);
        ItemService items = ItemService.gI();
        int templateId;
        if (mode == 2) {
            templateId = DIVINE[planet][type];
        } else {
            int[] pool = mode == 0 ? Arrays.stream(SKHCapsuleCatalog.legacyItems(planet, type)).distinct().toArray()
                    : SKHCapsuleCatalog.vipItems(planet, type);
            templateId = pool[random.applyAsInt(pool.length)];
        }
        int[] sets = SKHCapsuleCatalog.sets(planet);
        int setId = sets[random.applyAsInt(sets.length)];
        Item reward = switch (mode) {
            case 0 -> items.createItemSKH(templateId, setId);
            case 1 -> items.createItemSKHVipMax(templateId, setId, planet);
            default -> items.createActivatedDivineItem(templateId, setId, planet);
        };
        if (reward == null || reward.template == null || reward.template.id != templateId || reward.quantity != 1
                || reward.template.type != type || reward.template.gender != (type == 4 ? 3 : planet)) {
            throw new IllegalStateException("Thiếu dữ liệu trang bị kích hoạt. Hộp chưa bị sử dụng.");
        }
        // Legacy shop builders share option objects with the shop; the reward must own its options.
        List<Item.ItemOption> options = new ArrayList<>();
        for (Item.ItemOption option : reward.itemOptions) {
            if (option == null || option.optionTemplate == null || option.param < 0) {
                throw new IllegalStateException("Dữ liệu chỉ số trang bị không hợp lệ. Hộp chưa bị sử dụng.");
            }
            int id = option.optionTemplate.id;
            if (id == 30 || id == 107 || id == 102 || id == 72 || id == 93 || id == 86 || id == 87
                    || (id >= 34 && id <= 36)) continue;
            options.add(new Item.ItemOption(option));
        }
        boolean base = options.stream().anyMatch(o -> switch (type) {
            case 0 -> o.optionTemplate.id == 47;
            case 1 -> o.optionTemplate.id == 6 || o.optionTemplate.id == 22;
            case 2 -> o.optionTemplate.id == 0;
            case 3 -> o.optionTemplate.id == 7 || o.optionTemplate.id == 23;
            default -> o.optionTemplate.id == 14;
        });
        if (!base) throw new IllegalStateException("Thiếu chỉ số gốc trang bị. Hộp chưa bị sử dụng.");
        for (int required : activationOptions(setId)) {
            if (options.stream().filter(o -> o.optionTemplate.id == required && o.param == 1).count() != 1) {
                throw new IllegalStateException("Thiếu dữ liệu set kích hoạt. Hộp chưa bị sử dụng.");
            }
        }
        options.add(requiredOption(107, 5));
        options.add(requiredOption(30, 1));
        reward.itemOptions = options;
        reward.createTime = System.currentTimeMillis();
        reward.content = reward.getContent();
        reward.info = reward.getInfo();
        return reward;
    }

    private static int[] activationOptions(int setId) {
        int[] effects = ActivationSet.effectIds(setId);
        int[] ids = new int[effects.length + 1];
        ids[0] = setId;
        System.arraycopy(effects, 0, ids, 1, effects.length);
        return ids;
    }

    private static Item.ItemOption requiredOption(int id, int value) {
        var template = ItemService.gI().getItemOptionTemplate(id);
        if (template == null || template.id != id) throw new IllegalStateException("Thiếu dữ liệu sao hoặc khóa giao dịch.");
        return new Item.ItemOption(template, value);
    }

    public static void show(Player player, Item box) {
        if (player == null) return;
        synchronized (player) {
            selections.remove(player);
            if (player.session == null || player.inventory == null || player.inventory.itemsBag == null
                    || box == null || box.template == null || box.template.id != ITEM_ID || box.quantity < 1
                    || box.quantityGD != 0 || !player.inventory.itemsBag.contains(box)) return;
            if (AdminPlayerActions.hasPending(player) || LiveSyncMutationGate.blocked(player)
                    || player.tradeWVP || TransactionService.isTrading(player)) {
                Service.gI().sendThongBao(player, "Hãy kết thúc giao dịch trước khi mở hộp.");
                return;
            }
            if (player.idMark == null) player.idMark = new IDMark();
            selections.put(player, new Selection(new WeakReference<>(box), new WeakReference<>(player.session), System.currentTimeMillis()));
            NpcService.gI().createMenuConMeo(player, MENU, -1,
                    "Chọn nhóm trang bị kích hoạt. Mỗi hộp nhận 1 món ngẫu nhiên hành tinh, loại món và set.\n"
                    + "Trang bị có 5 ô sao pha lê chưa ép, không giao dịch. Mở hộp không mất phí.",
                    "Random SKH Thường", "Random SKH VIP", "Random SKH Thần", "Đóng");
        }
    }

    public static void choose(Player player, int select) {
        if (player == null) return;
        synchronized (player) {
            Selection choice = selections.remove(player);
            boolean currentMenu = player.idMark != null && player.idMark.getIndexMenu() == MENU;
            if (currentMenu) player.idMark.setIndexMenu(ConstNpc.IGNORE_MENU);
            if (select < 0 || select > 2) return;
            if (!currentMenu || choice == null || player.session == null || choice.session.get() != player.session
                    || System.currentTimeMillis() - choice.time > MENU_LIFETIME_MS) {
                Service.gI().sendThongBao(player, "Lựa chọn hết hạn. Hãy sử dụng lại hộp.");
                return;
            }
            Item box = choice.box.get();
            try {
                var result = new ConsignTransactions().redeemActivationBox(player, box, select);
                Service.gI().sendThongBao(player, result.message());
                InventoryService.gI().sendItemBags(player);
            } catch (RuntimeException failure) {
                Logger.logException(ActivationGiftBoxService.class, failure, "Activation gift-box redemption failed for " + player.id);
                Service.gI().sendThongBao(player, "Chưa mở được hộp. Hãy mở lại khi dữ liệu đã sẵn sàng.");
            }
        }
    }
}
