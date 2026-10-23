package nro.models.combine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.IntUnaryOperator;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.item.SKHCapsuleCatalog;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.services_func.TransactionService;
import nro.models.utils.Util;

/** A preview is a single-use selection; all results are built before any payment. */
public final class NangSKHVip {
    private NangSKHVip() { }
    public static final long FEE_PER_PAIR = 1_000_000_000L;
    private static final String[] PLANETS = {"Trái Đất", "Namek", "Xayda"};

    public record Pair(int mainSlot, int fodderSlot, Item main, Item fodder,
            int planet, int type, String mainState, String fodderState) { }

    public record Selection(List<Pair> pairs, List<Item> bag, int[] slots) {
        public Selection {
            pairs = List.copyOf(pairs);
            slots = slots.clone();
        }
        @Override public int[] slots() { return slots.clone(); }
        public long fee() { return totalFee(pairs.size()); }
    }

    public record Prepared(List<Item> bag, long gold, NangSKHVipTichLuy.Progress progress,
            int pairs, int successes, List<String> bonusNames) {
        public Prepared { bonusNames = List.copyOf(bonusNames); }
    }

    public static long totalFee(int count) {
        if (count < 1 || count > 5) throw invalid("Chỉ nhận từ 1 đến 5 cặp");
        return FEE_PER_PAIR * count;
    }

    public static Selection select(Player player, int[] slots) {
        if (player == null || player.inventory == null || player.inventory.itemsBag == null
                || slots == null || slots.length < 2 || slots.length > 10 || slots.length % 2 != 0) {
            throw invalid("Chọn 1–5 món hủy diệt trước, rồi cùng số món thần phía sau");
        }
        ensureNotTrading(player);
        List<Item> bag = player.inventory.itemsBag;
        Set<Item> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        boolean[] used = new boolean[bag.size()];
        int count = slots.length / 2;
        int seenTypes = 0;
        for (int i = 0; i < slots.length; i++) {
            int slot = slots[i];
            if (slot < 0 || slot >= bag.size() || used[slot]) throw invalid("Ô nguyên liệu không hợp lệ hoặc trùng");
            used[slot] = true;
            Item item = bag.get(slot);
            validateItem(item, i < count);
            if (!seen.add(item) || Collections.frequency(bag, item) != 1) {
                throw invalid("Nguyên liệu bị trùng trong hành trang");
            }
            if (i >= count) {
                int type = item.template.type;
                if ((seenTypes & (1 << type)) != 0) throw invalid("Đồ thần không được trùng loại trang bị");
                seenTypes |= 1 << type;
            }
        }
        List<Pair> pairs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Item main = bag.get(slots[i]);
            Item fodder = bag.get(slots[i + count]);
            pairs.add(new Pair(slots[i], slots[i + count], main, fodder,
                    outputPlanet(main, player.gender), main.template.type, state(main), state(fodder)));
        }
        return new Selection(pairs, bag, slots);
    }

    static void validateItem(Item item, boolean main) {
        if (item == null || item.template == null || item.quantity != 1 || item.quantityGD != 0
                || item.itemOptions == null || item.template.type < 0 || item.template.type > 4) {
            throw invalid("Nguyên liệu không hợp lệ, chỉ nhận trang bị riêng lẻ trong túi");
        }
        int id = item.template.id;
        if (main ? id < 650 || id > 662 : id < 555 || id > 567) {
            throw invalid(main ? "Nhóm đầu phải là đồ hủy diệt" : "Nhóm sau phải là đồ thần linh");
        }
        for (Item.ItemOption option : item.itemOptions) {
            if (option == null || option.optionTemplate == null || option.optionTemplate.id == 93) {
                throw invalid("Không nhận trang bị hạn sử dụng hoặc dữ liệu option lỗi");
            }
        }
        if (item.expire != 0 || item.isSKH()) throw invalid("Không nhận trang bị hạn sử dụng hoặc đã có set kích hoạt");
        // IDs are whitelisted; also reject corrupted type/planet metadata.
        int expectedType = id == 656 || id == 561 ? 4
                : id < (main ? 657 : 562) ? ((id - (main ? 650 : 555)) % 2)
                : 2 + ((id - (main ? 657 : 562)) % 2);
        int expectedPlanet = expectedType == 4 ? 3
                : id < (main ? 657 : 562) ? (id - (main ? 650 : 555)) / 2
                : (id - (main ? 657 : 562)) / 2;
        if (item.template.type != expectedType || item.template.gender != expectedPlanet) {
            throw invalid("Dữ liệu loại trang bị hoặc hành tinh không hợp lệ");
        }
    }

    static int outputPlanet(Item main, int playerPlanet) {
        int planet = main.template.id == 656 ? playerPlanet : main.template.gender;
        if (planet < 0 || planet > 2) throw invalid("Hành tinh đầu ra không hợp lệ");
        return planet;
    }

    static void ensureNotTrading(Player p) {
        if (p.tradeWVP || TransactionService.isTrading(p)
                || (p.idMark != null && p.idMark.getPlayerTrade() != null)) {
            throw invalid("Hãy kết thúc giao dịch trước khi nâng SKH VIP");
        }
    }

    static String state(Item item) {
        StringBuilder state = new StringBuilder().append(item.template.id).append(':')
                .append(item.template.type).append(':').append(item.template.gender).append(':')
                .append(item.quantity).append(':').append(item.quantityGD).append(':')
                .append(item.createTime).append(':').append(item.expire).append(':').append(item.id);
        for (Item.ItemOption option : item.itemOptions) {
            state.append(';').append(option.optionTemplate.id).append('=').append(option.param);
        }
        return state.toString();
    }

    static boolean rollSuccess(IntUnaryOperator random) { return roll(random, 100) < 50; }

    static int rollTemplate(int planet, int type, IntUnaryOperator random) {
        int[] items = SKHCapsuleCatalog.vipItems(planet, type);
        return roll(random, 100) < 5 ? items[8 + roll(random, 4)] : items[roll(random, 8)];
    }

    static int rollSet(int planet, IntUnaryOperator random) {
        int[] sets = SKHCapsuleCatalog.sets(planet);
        return sets[roll(random, sets.length)];
    }

    private static int roll(IntUnaryOperator random, int bound) {
        int value = random.applyAsInt(bound);
        if (value < 0 || value >= bound) throw invalid("Dữ liệu ngẫu nhiên không hợp lệ");
        return value;
    }

    public static void showInfoCombine(Player player, int[] slots) {
        synchronized (player) {
            player.combineNew.clearItemCombine();
            try {
                Selection selection = select(player, slots);
                player.combineNew.skhVipSelection = selection;
                for (int slot : selection.slots()) player.combineNew.itemsCombine.add(selection.bag().get(slot));
                StringBuilder text = new StringBuilder("Nâng SKH VIP: hủy diệt là đồ chính, thần là phôi.\n");
                for (Pair pair : selection.pairs()) {
                    text.append(pair.main().template.name).append(" + ").append(pair.fodder().template.name)
                            .append(" → ").append(PLANETS[pair.planet()]).append('\n');
                }
                text.append("Thành công 50% mỗi cặp; random cấp 1–12, chỉ số tối đa theo cấp.\n")
                        .append("Cấp 9–12 chiếm tổng 5% trong các món thành công.\n")
                        .append("Thành công đổi cả hai lấy đồ mới; xịt chỉ mất thần, giữ hủy diệt.\n")
                        .append("Rada lấy set theo hành tinh người chơi.\n")
                        .append("Càng kiên trì, con càng có cơ duyên nhận bảo vật.\n")
                        .append("Phí: ").append(Util.numberToMoney(selection.fee())).append(" vàng, không hoàn khi xịt.");
                if (player.inventory.gold < selection.fee()) throw invalid("Không đủ " + Util.numberToMoney(selection.fee()) + " vàng");
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                        text.toString(), "Nâng SKH VIP", "Từ chối");
            } catch (IllegalArgumentException | IllegalStateException ex) {
                player.combineNew.clearItemCombine();
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, ex.getMessage(), "Đóng");
            }
        }
    }

    /** Build a complete transaction snapshot. Never charge or publish live inventory here. */
    public static Prepared prepare(Player player, IntUnaryOperator random) {
        synchronized (player) {
            if (player.combineNew == null || player.combineNew.typeCombine != CombineService.NANG_SKH_VIP
                    || player.combineNew.skhVipSelection == null) throw invalid("Hãy chọn lại nguyên liệu SKH VIP");
            if (player.idMark == null || !Util.canDoWithTime(player.idMark.getLastTimeCombine(), 500)) {
                throw invalid("Vui lòng chờ trước khi nâng tiếp");
            }
            synchronized (player.inventory) {
                Selection old = player.combineNew.skhVipSelection;
                Selection current = select(player, old.slots());
                if (old.bag() != current.bag() || player.combineNew.itemsCombine.size() != old.slots().length) {
                    throw invalid("Nguyên liệu đã thay đổi, hãy chọn lại");
                }
                for (int i = 0; i < old.slots().length; i++) {
                    if (player.combineNew.itemsCombine.get(i) != old.bag().get(old.slots()[i])) {
                        throw invalid("Lựa chọn đã thay đổi, hãy chọn lại");
                    }
                }
                for (int i = 0; i < old.pairs().size(); i++) {
                    Pair before = old.pairs().get(i), after = current.pairs().get(i);
                    if (before.main() != after.main() || before.fodder() != after.fodder()
                            || before.planet() != after.planet() || before.type() != after.type()
                            || !before.mainState().equals(after.mainState()) || !before.fodderState().equals(after.fodderState())) {
                        throw invalid("Nguyên liệu hoặc hành tinh đã thay đổi, hãy chọn lại");
                    }
                }
                if (player.inventory.gold < current.fee()) throw invalid("Không đủ vàng để nâng SKH VIP");
                List<Item> outputs = new ArrayList<>();
                NangSKHVipTichLuy.Progress progress = NangSKHVipTichLuy.current(player);
                int successes = 0;
                for (Pair pair : current.pairs()) {
                    Item output = null;
                    if (rollSuccess(random)) {
                        output = ItemService.gI().createItemSKHVipMax(rollTemplate(pair.planet(), pair.type(), random),
                                rollSet(pair.planet(), random), pair.planet());
                        if (output == null || output.template == null || output.quantity != 1) {
                            throw invalid("Thiếu dữ liệu trang bị SKH VIP");
                        }
                        successes++;
                    }
                    outputs.add(output);
                    progress = NangSKHVipTichLuy.advance(progress, pair.planet());
                }
                List<Item> bag = new ArrayList<>(current.bag());
                Item[] emptyFodders = new Item[current.pairs().size()];
                for (int i = 0; i < emptyFodders.length; i++) emptyFodders[i] = new Item();
                for (int i = 0; i < current.pairs().size(); i++) {
                    Pair pair = current.pairs().get(i);
                    if (outputs.get(i) != null) bag.set(pair.mainSlot(), outputs.get(i));
                    bag.set(pair.fodderSlot(), emptyFodders[i]);
                }
                var rewards = NangSKHVipTichLuy.prepareRewards(bag, progress, random);
                return new Prepared(rewards.bag(), player.inventory.gold - current.fee(), rewards.progress(),
                        current.pairs().size(), successes, rewards.bonusNames());
            }
        }
    }

    public static void upgrade(Player player) {
        synchronized (player) {
            nro.models.shop_ky_gui.ConsignTransactions.Result result;
            try { result = new nro.models.shop_ky_gui.ConsignTransactions().upgradeSkhVip(player, Util::nextInt); }
            catch (IllegalArgumentException | IllegalStateException ex) {
                synchronized (player) { player.combineNew.clearItemCombine(); }
                Service.gI().sendThongBao(player, ex.getMessage());
                return;
            }
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendMoney(player);
            CombineService.gI().reOpenItemCombine(player);
            Service.gI().sendThongBao(player, result.message());
        }
    }

    public static void cancel(Player player) {
        synchronized (player) {
            player.combineNew.clearItemCombine();
            player.idMark.setIndexMenu(ConstNpc.IGNORE_MENU);
        }
    }

    private static IllegalArgumentException invalid(String text) { return new IllegalArgumentException(text); }
}
