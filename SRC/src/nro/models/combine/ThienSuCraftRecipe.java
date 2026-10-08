package nro.models.combine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.*;
import nro.models.services_func.TransactionService;
import nro.models.shop_ky_gui.ConsignTransactions;

/** One recipe shared by preview and confirmation; selection is an ownership snapshot. */
public final class ThienSuCraftRecipe {
    public static final int RUBY_FEE = 50;
    private ThienSuCraftRecipe() { }
    public record Ingredient(int slot, Item item, int cost, String state) { }
    public record Equipped(Item item, String state) { }
    public record Selection(List<Item> bag, List<Item> body, List<Ingredient> ingredients,
            List<Equipped> equipped, int bodySlot, int outputId, int factoryGender,
            int successThreshold, int luckThreshold) {
        public Selection {
            ingredients = List.copyOf(ingredients);
            equipped = List.copyOf(equipped);
        }
    }

    static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }

    static void ensureAvailable(Player p) {
        if (p.beforeDispose || p.isOffline || p.isPet || !p.isPlayer
                || LiveSyncMutationGate.blocked(p) || CommesonChallengeService.hasPendingId(p.id)
                || ConsignTransactions.hasPendingId(p.id) || RubyGiftService.hasPendingId(p.id)
                || WalletPurchaseService.hasPendingId(p.id) || p.walletPurchasePending != null
                || p.kolProgressPending || p.kolRewardPending || KolQuestService.hasProgressPendingId(p.id)
                || AdminPlayerActions.hasRenamePendingId(p.id)
                || (p.session != null && (ConsignTransactions.hasPendingAccount(p.session.userId)
                    || WalletPurchaseService.hasPendingAccount(p.session.userId)
                    || RubyGiftService.hasPendingAccount(p.session.userId)
                    || AdminPlayerActions.hasRenamePendingAccount(p.session.userId)
                    || KolQuestService.hasProgressPendingAccount(p.session.userId)))) {
            throw invalid("Tài sản đang chờ đối soát, hãy thử lại sau");
        }
        if (p.tradeWVP || TransactionService.isTrading(p)
                || (p.idMark != null && p.idMark.getPlayerTrade() != null)) {
            throw invalid("Hãy kết thúc giao dịch trước khi chế tạo Thiên Sứ");
        }
    }

    public static Selection select(Player p) {
        if (p == null || p.inventory == null || p.inventory.itemsBag == null
                || p.inventory.itemsBody == null || p.combineNew == null
                || p.combineNew.itemsCombine == null || p.combineNew.itemsCombine.size() != 4) {
            throw invalid("Chọn đúng 4 nhóm: 999 mảnh, 1 công thức thường hoặc VIP, 1 đá nâng cấp, 1 đá may mắn");
        }
        ensureAvailable(p);
        if (p.inventory.ruby < RUBY_FEE) throw invalid("Cần 50 hồng ngọc để chế tạo");
        List<Item> bag = p.inventory.itemsBag, body = p.inventory.itemsBody;
        if (bag.stream().anyMatch(java.util.Objects::isNull)
                || bag.stream().noneMatch(i -> !i.isNotNullItem())) throw invalid("Cần ít nhất 1 ô trống hành trang");
        Set<Item> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        Item fragment = null, formula = null, upgrade = null, lucky = null;
        List<Ingredient> ingredients = new ArrayList<>();
        for (Item item : p.combineNew.itemsCombine) {
            checkItem(item);
            if (!seen.add(item) || Collections.frequency(bag, item) != 1 || body.contains(item)) {
                throw invalid("Nguyên liệu bị trùng hoặc không còn trong hành trang");
            }
            int id = item.template.id, cost;
            if (id >= 1066 && id <= 1070 && fragment == null) { fragment = item; cost = 999; }
            else if (((id >= 1071 && id <= 1073) || (id >= 1084 && id <= 1086)) && formula == null) { formula = item; cost = 1; }
            else if (id >= 1074 && id <= 1078 && upgrade == null) { upgrade = item; cost = 1; }
            else if (id >= 1079 && id <= 1083 && lucky == null) { lucky = item; cost = 1; }
            else throw invalid("Mỗi nhóm nguyên liệu phải có đúng 1 món");
            if (item.quantity < cost) throw invalid("Không đủ nguyên liệu: cần 999 mảnh và mỗi món khác 1 cái");
            ingredients.add(new Ingredient(bag.indexOf(item), item, cost, NangSKHVip.state(item)));
        }
        if (fragment == null || formula == null || upgrade == null || lucky == null) throw invalid("Thiếu nhóm nguyên liệu Thiên Sứ");
        if (body.size() < 5) throw invalid("Phải mặc đủ 5 món Hủy Diệt");
        List<Equipped> equipped = new ArrayList<>();
        Set<Item> worn = Collections.newSetFromMap(new IdentityHashMap<>());
        for (int slot = 0; slot < 5; slot++) {
            Item item = body.get(slot);checkItem(item);
            int id = item.template.id;
            int type = id == 656 ? 4 : id < 657 ? (id - 650) % 2 : 2 + (id - 657) % 2;
            if (id < 650 || id > 662 || type != slot || item.template.type != slot
                    || item.quantity != 1 || !worn.add(item) || Collections.frequency(body, item) != 1 || bag.contains(item)) {
                throw invalid("Phải mặc đủ 5 món Hủy Diệt đúng ô trang bị");
            }
            equipped.add(new Equipped(item, NangSKHVip.state(item)));
        }
        int gender = formula.template.gender > 2 ? p.gender : formula.template.gender;
        if (gender < 0 || gender > 2) throw invalid("Hành tinh công thức không hợp lệ");
        int slot = fragment.typeIdManh();
        return new Selection(bag, body, ingredients, equipped, slot, 1048 + slot * 3 + gender,
                formula.template.gender, 90 + upgrade.template.id - 1073, 5 + 5 * (lucky.template.id - 1078));
    }

    private static void checkItem(Item item) {
        if (item == null || item.template == null || item.quantity <= 0 || item.quantityGD != 0 || item.itemOptions == null) {
            throw invalid("Thiếu nguyên liệu hoặc món Hủy Diệt đang mặc; vật phẩm đang giao dịch không hợp lệ");
        }
        for (Item.ItemOption option : item.itemOptions) {
            if (option == null || option.optionTemplate == null) throw invalid("Dữ liệu vật phẩm không hợp lệ");
        }
    }

    static void validateSnapshot(Selection old, Selection current) {
        if (old.bag() != current.bag() || old.body() != current.body()
                || old.bodySlot() != current.bodySlot() || old.outputId() != current.outputId()
                || old.factoryGender() != current.factoryGender()
                || !old.ingredients().equals(current.ingredients()) || !old.equipped().equals(current.equipped())) {
            throw invalid("Nguyên liệu hoặc trang bị đã thay đổi, hãy chọn lại để xác nhận");
        }
    }
}
