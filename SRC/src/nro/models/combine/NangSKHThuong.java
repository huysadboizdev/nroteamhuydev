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
import nro.models.utils.Util;

/** Single-use divine equipment preview and atomic ordinary SKH conversion. */
public final class NangSKHThuong {
    private NangSKHThuong() { }
    public static final long FEE_PER_ITEM = 500_000_000L;
    private static final int[][] SETS = {{233,127,128,129,245},{233,132,237,130,131},{134,233,133,135,241}};
    private static final String[] PLANETS = {"Trái Đất", "Namek", "Xayda"};

    public record Entry(int slot, Item item, int planet, int type, String state) { }
    public record Selection(List<Entry> entries, List<Item> bag, int[] slots) {
        public Selection { entries = List.copyOf(entries); slots = slots.clone(); }
        @Override public int[] slots() { return slots.clone(); }
        public long fee() { return totalFee(entries.size()); }
    }
    public static long totalFee(int count) {
        if (count < 1 || count > 5) throw invalid("Chỉ nhận từ 1 đến 5 món thần linh");
        return FEE_PER_ITEM * count;
    }
    public static Selection select(Player player, int[] slots) {
        if (player == null || player.inventory == null || player.inventory.itemsBag == null
                || slots == null || slots.length < 1 || slots.length > 5) {
            throw invalid("Chọn từ 1 đến 5 món thần linh, không trùng loại trang bị");
        }
        NangSKHVip.ensureNotTrading(player);
        List<Item> bag = player.inventory.itemsBag;
        boolean[] used = new boolean[bag.size()];
        Set<Item> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        int types = 0;
        List<Entry> entries = new ArrayList<>();
        for (int slot : slots) {
            if (slot < 0 || slot >= bag.size() || used[slot]) throw invalid("Ô nguyên liệu không hợp lệ hoặc trùng");
            used[slot] = true;
            Item item = bag.get(slot);
            NangSKHVip.validateItem(item, false);
            if (!seen.add(item) || Collections.frequency(bag, item) != 1) throw invalid("Nguyên liệu bị trùng trong hành trang");
            int type = item.template.type;
            if ((types & (1 << type)) != 0) throw invalid("Đồ thần không được trùng loại trang bị");
            types |= 1 << type;
            int planet = item.template.id == 561 ? player.gender : item.template.gender;
            if (planet < 0 || planet > 2) throw invalid("Hành tinh đầu ra không hợp lệ");
            entries.add(new Entry(slot, item, planet, type, NangSKHVip.state(item)));
        }
        return new Selection(entries, bag, slots);
    }
    static int rollSet(int planet, IntUnaryOperator random) {
        if (planet < 0 || planet > 2) throw invalid("Hành tinh đầu ra không hợp lệ");
        int value = roll(random, 100);
        int index = value < 30 ? 0 : value < 60 ? 1 : value < 90 ? 2 : value < 95 ? 3 : 4;
        return SETS[planet][index];
    }
    static boolean[] successfulIndexes(int count, IntUnaryOperator random) {
        totalFee(count);
        boolean[] success = new boolean[count];
        for (int i = 0; i < count; i++) success[i] = NangSKHVip.rollSuccess(random);
        return success;
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
                player.combineNew.skhThuongSelection = selection;
                for (Entry entry : selection.entries()) player.combineNew.itemsCombine.add(entry.item());
                StringBuilder text = new StringBuilder("Nâng SKH Thường: đồ thần linh thành SKH cấp 1, chỉ số tối đa.\n");
                for (Entry entry : selection.entries()) text.append(entry.item().template.name).append(" → ")
                        .append(PLANETS[entry.planet()]).append('\n');
                text.append("Mỗi món: 50% thành công, 50% xịt, độc lập.\n")
                        .append("Có thể nhận từ 0 đến ").append(selection.entries().size()).append(" món SKH.\n")
                        .append("Tất cả đồ thần đã chọn đều bị tiêu hao, kể cả khi xịt.\n")
                        .append("Set theo hành tinh của từng món; rada theo hành tinh người chơi.\n")
                        .append("Phí: ").append(Util.numberToMoney(selection.fee())).append(" vàng, không hoàn khi xịt.");
                if (player.inventory.gold < selection.fee()) throw invalid("Không đủ " + Util.numberToMoney(selection.fee()) + " vàng");
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, text.toString(), "Nâng SKH Thường", "Từ chối");
            } catch (IllegalArgumentException | IllegalStateException ex) {
                player.combineNew.clearItemCombine();
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, ex.getMessage(), "Đóng");
            }
        }
    }
    /** Build every replacement before mutating any selected slot or charging gold. */
    static int execute(Player player, IntUnaryOperator random) {
        synchronized (player) {
            if (player.combineNew == null || player.combineNew.typeCombine != CombineService.NANG_SKH_THUONG
                    || player.combineNew.skhThuongSelection == null) throw invalid("Hãy chọn lại nguyên liệu SKH Thường");
            if (player.idMark == null || !Util.canDoWithTime(player.idMark.getLastTimeCombine(), 500)) throw invalid("Vui lòng chờ trước khi nâng tiếp");
            synchronized (player.inventory) {
                Selection old = player.combineNew.skhThuongSelection;
                Selection current = select(player, old.slots());
                if (old.bag() != current.bag() || player.combineNew.itemsCombine.size() != current.entries().size()) throw invalid("Nguyên liệu đã thay đổi, hãy chọn lại");
                for (int i = 0; i < current.entries().size(); i++) {
                    Entry before = old.entries().get(i), after = current.entries().get(i);
                    if (before.item() != after.item() || before.slot() != after.slot() || before.type() != after.type()
                            || before.planet() != after.planet() || !before.state().equals(after.state())
                            || player.combineNew.itemsCombine.get(i) != after.item()) throw invalid("Lựa chọn đã thay đổi, hãy chọn lại");
                }
                if (player.inventory.gold < current.fee()) throw invalid("Không đủ vàng để nâng SKH Thường");
                boolean[] success = successfulIndexes(current.entries().size(), random);
                List<Item> outputs = new ArrayList<>();
                int successes = 0;
                for (int i = 0; i < success.length; i++) {
                    Entry entry = current.entries().get(i);
                    Item output = new Item();
                    if (success[i]) {
                        int template = SKHCapsuleCatalog.vipItems(entry.planet(), entry.type())[0];
                        output = ItemService.gI().createItemSKHVipMax(template, rollSet(entry.planet(), random), entry.planet());
                        if (output == null || output.template == null || output.quantity != 1) throw invalid("Thiếu dữ liệu trang bị SKH Thường");
                        successes++;
                    }
                    outputs.add(output);
                }
                for (int i = 0; i < outputs.size(); i++) current.bag().set(current.entries().get(i).slot(), outputs.get(i));
                player.inventory.gold -= current.fee();
                player.combineNew.clearItemCombine();
                player.combineNew.clearParamCombine();
                long now = System.currentTimeMillis();
                player.idMark.setLastTimeCombine(now);
                player.idMark.setIndexMenu(ConstNpc.IGNORE_MENU);
                player.combineNew.lastTimeCombine = now;
                return successes;
            }
        }
    }
    public static void upgrade(Player player) {
        synchronized (player) {
            int count = player.combineNew.skhThuongSelection == null ? 0 : player.combineNew.skhThuongSelection.entries().size();
            int successes;
            try { successes = execute(player, Util::nextInt); }
            catch (IllegalArgumentException | IllegalStateException ex) {
                player.combineNew.clearItemCombine();
                Service.gI().sendThongBao(player, ex.getMessage());
                return;
            }
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendMoney(player);
            if (successes > 0) CombineService.gI().sendEffectSuccessCombine(player);
            else CombineService.gI().sendEffectFailCombine(player);
            CombineService.gI().reOpenItemCombine(player);
            Service.gI().sendThongBao(player, "Nâng SKH Thường: " + successes + " món thành công, " + (count - successes)
                    + " món xịt. Đã dùng " + count + " món thần và " + Util.numberToMoney(totalFee(count)) + " vàng.");
        }
    }
    public static void cancel(Player player) {
        synchronized (player) { player.combineNew.clearItemCombine(); player.idMark.setIndexMenu(ConstNpc.IGNORE_MENU); }
    }
    private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
}
