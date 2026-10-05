package nro.models.combine;

import java.util.Collections;
import java.util.List;
import nro.models.consts.ConstNpc;
import nro.models.item.ActivationSet;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.services_func.TransactionService;

/** Shared, single-use conversion preview and payment rules. */
public final class ChuyenHoaTrangBiRules {
    public static final long GOLD_FEE = 2_000_000_000L;
    public static final int GEM_FEE = 5000;
    private ChuyenHoaTrangBiRules() { }

    public record Selection(List<Item> bag, int sourceSlot, int targetSlot, Item source,
            Item target, String sourceState, String targetState, boolean gems) { }

    private static String state(Item item) {
        return item.template.level + ":" + NangSKHVip.state(item);
    }

    private static int value(Item item, int id) {
        int result = 0, count = 0;
        for (Item.ItemOption o : item.itemOptions) {
            if (o.optionTemplate.id == id) { result = o.param; count++; }
        }
        if (count > 1) throw invalid("Trang bị có dữ liệu cấp hoặc sao bị trùng");
        return result;
    }

    private static void validateItem(Item item) {
        if (item == null || item.template == null || item.quantity != 1 || item.quantityGD != 0
                || item.itemOptions == null || item.template.type < 0 || item.template.type > 4) {
            throw invalid("Chỉ nhận hai món trang bị riêng lẻ còn trong hành trang");
        }
        if (item.expire != 0) throw invalid("Không nhận trang bị có hạn sử dụng");
        for (Item.ItemOption o : item.itemOptions) {
            if (o == null || o.optionTemplate == null || o.param < 0 || o.optionTemplate.id == 93) {
                throw invalid("Trang bị có hạn sử dụng hoặc dữ liệu option không hợp lệ");
            }
        }
        // Presence of a set name OR an effect is enough; never trust its parameter.
        // Shared validation runs for both inputs at preview and again at commit.
        if (item.isSKH()) {
            throw invalid("Không được chuyển hóa trang bị kích hoạt, kể cả đồ nguồn và đồ Thần Linh nhận");
        }
    }

    private static void validate(Player p, boolean gems) {
        if (p.inventory == null || p.inventory.itemsBag == null || p.combineNew == null
                || p.combineNew.itemsCombine.size() != 2) {
            throw invalid("Chọn đồ bậc 11 hoặc 12 từ +4 trước, rồi chọn đồ Thần Linh chưa nâng cấp");
        }
        if (p.tradeWVP || TransactionService.isTrading(p) || p.idMark.getPlayerTrade() != null) {
            throw invalid("Hãy kết thúc giao dịch trước khi chuyển hóa");
        }
        int expected = gems ? CombineService.CHUYEN_HOA_TRANG_BI_NGOC : CombineService.CHUYEN_HOA_TRANG_BI_VANG;
        if (p.combineNew.typeCombine != expected) throw invalid("Hãy mở lại chức năng chuyển hóa");
        Item source = p.combineNew.itemsCombine.get(0), target = p.combineNew.itemsCombine.get(1);
        validateItem(source); validateItem(target);
        if (source == target || Collections.frequency(p.inventory.itemsBag, source) != 1
                || Collections.frequency(p.inventory.itemsBag, target) != 1) {
            throw invalid("Hai món phải khác nhau và còn trong hành trang");
        }
        if (source.template.level < 11 || source.template.level > 12) {
            throw invalid("Trang bị nguồn phải là đồ bậc 11 hoặc 12");
        }
        int level = value(source, 72);
        if (level < 4 || level > CombineService.MAX_LEVEL_ITEM) throw invalid("Trang bị nguồn phải từ +4 đến +8");
        if (target.template.id < 555 || target.template.id > 567 || target.template.level != 13) {
            throw invalid("Trang bị nhận phải là đồ Thần Linh");
        }
        if (source.template.type != target.template.type || source.template.gender != target.template.gender) {
            throw invalid("Hai trang bị phải cùng loại và cùng hành tinh");
        }
        if (value(target, 72) != 0 || value(target, 102) != 0 || value(target, 209) != 0) {
            throw invalid("Đồ Thần Linh nhận phải chưa nâng cấp và chưa ép sao pha lê");
        }
        for (Item item : new Item[]{source, target}) {
            int stars = value(item, 102), holes = value(item, 107);
            if (stars < 0 || stars > holes || holes > CombineService.MAX_STAR_ITEM) {
                throw invalid("Số sao và số lỗ pha lê không hợp lệ");
            }
            value(item, 209);
        }
    }

    private static boolean baseStat(int type, int id) {
        return switch (type) {
            case 0 -> id == 47;
            case 1 -> id == 6 || id == 22 || id == 27;
            case 2 -> id == 0;
            case 3 -> id == 7 || id == 23 || id == 28;
            case 4 -> id == 14;
            default -> false;
        };
    }

    private static boolean percentage(Item.ItemOption o) {
        String name = o.optionTemplate.name;
        return ActivationSet.primaryId(o.optionTemplate.id) == -1 && name != null
                && name.contains("#") && name.contains("%") && !name.startsWith("$");
    }

    private static Item result(Item source, Item target, boolean gems) {
        Item out = ItemService.gI().copyItem(target);
        out.itemOptions.clear();
        int level = value(source, 72), drops = value(source, 209);
        double factor = Math.pow(1.1, level) * Math.pow(0.9, drops);
        boolean hasBase = false;
        for (Item.ItemOption o : target.itemOptions) {
            int id = o.optionTemplate.id;
            if (id == 72 || id == 209 || id == 102 || (gems && id == 107)) continue;
            int param = o.param;
            if (baseStat(target.template.type, id)) {
                hasBase = true;
                double upgraded = param * factor;
                if (!Double.isFinite(upgraded) || upgraded > Integer.MAX_VALUE || upgraded < 0) {
                    throw invalid("Chỉ số sau chuyển hóa vượt giới hạn");
                }
                param = (int) upgraded;
            }
            out.itemOptions.add(new Item.ItemOption(o.optionTemplate, param));
        }
        if (!hasBase) throw invalid("Đồ Thần Linh thiếu chỉ số nền, không thể chuyển hóa");
        for (Item.ItemOption o : source.itemOptions) {
            int id = o.optionTemplate.id;
            if (id == 72 || id == 209 || (gems && (id == 102 || id == 107))
                    || (!baseStat(source.template.type, id) && percentage(o))) {
                out.itemOptions.add(new Item.ItemOption(o));
            }
        }
        out.info = out.getInfo(); out.content = out.getContent();
        return out;
    }

    public static void preview(Player p, boolean gems) {
        synchronized (p) {
            p.combineNew.chuyenHoaSelection = null;
            p.idMark.setIndexMenu(ConstNpc.IGNORE_MENU);
            try {
                validate(p, gems);
                Item source = p.combineNew.itemsCombine.get(0), target = p.combineNew.itemsCombine.get(1);
                Item out = result(source, target, gems);
                Selection selection = new Selection(p.inventory.itemsBag, p.inventory.itemsBag.indexOf(source),
                        p.inventory.itemsBag.indexOf(target), source, target, state(source), state(target), gems);
                StringBuilder text = new StringBuilder("|2|Sau chuyển hóa: ").append(out.getName()).append('\n');
                for (Item.ItemOption o : out.itemOptions) text.append("|0|").append(o.getOptionString()).append('\n');
                text.append(gems ? "|1|Giữ cấp, mọi dòng % và sao pha lê\n|2|Cần 5.000 ngọc"
                        : "|1|Giữ cấp và mọi dòng %. Không chuyển số sao pha lê của nguồn\n|2|Cần 2 tỷ vàng");
                p.combineNew.chuyenHoaSelection = selection;
                CombineService.gI().baHatMit.createOtherMenu(p, ConstNpc.MENU_START_COMBINE,
                        text.toString(), "Chuyển hóa", "Từ chối");
            } catch (IllegalArgumentException e) { Service.gI().sendThongBaoOK(p, e.getMessage()); }
        }
    }

    public static void convert(Player p, boolean gems) {
        synchronized (p) {
            if (p.combineNew == null) return;
            Selection q = p.combineNew.chuyenHoaSelection;
            p.combineNew.chuyenHoaSelection = null; // consume even a rejected confirmation
            if (q == null) return;
            try {
                validate(p, gems);
                if (q.gems != gems || q.bag != p.inventory.itemsBag
                        || q.sourceSlot >= q.bag.size() || q.targetSlot >= q.bag.size()
                        || q.bag.get(q.sourceSlot) != q.source || q.bag.get(q.targetSlot) != q.target
                        || p.combineNew.itemsCombine.get(0) != q.source || p.combineNew.itemsCombine.get(1) != q.target
                        || !state(q.source).equals(q.sourceState) || !state(q.target).equals(q.targetState)) {
                    throw invalid("Nguyên liệu đã thay đổi, hãy chọn lại để xem kết quả");
                }
                if (gems ? p.inventory.gem < GEM_FEE : p.inventory.gold < GOLD_FEE) {
                    throw invalid(gems ? "Không đủ 5.000 ngọc" : "Không đủ 2 tỷ vàng");
                }
                Item out = result(q.source, q.target, gems);
                Item empty = ItemService.gI().createItemNull();
                long gold = p.inventory.gold; int gem = p.inventory.gem;
                try {
                    q.bag.set(q.targetSlot, out);
                    q.bag.set(q.sourceSlot, empty);
                    if (gems) p.inventory.subGem(GEM_FEE); else p.inventory.gold -= GOLD_FEE;
                } catch (RuntimeException e) {
                    q.bag.set(q.sourceSlot, q.source); q.bag.set(q.targetSlot, q.target);
                    p.inventory.gold = gold; p.inventory.gem = gem;
                    throw e;
                }
                q.source.quantity = 0; q.target.quantity = 0;
                p.combineNew.clearItemCombine(); p.combineNew.clearParamCombine();
                p.idMark.setIndexMenu(ConstNpc.IGNORE_MENU);
                Service.gI().sendMoney(p);
                InventoryService.gI().sendItemBags(p);
                CombineService.gI().reOpenItemCombine(p);
                CombineService.gI().sendEffectSuccessCombine(p);
            } catch (IllegalArgumentException e) { Service.gI().sendThongBaoOK(p, e.getMessage()); }
        }
    }

    public static void cancel(Player p) {
        synchronized (p) {
            p.combineNew.clearItemCombine(); p.combineNew.clearParamCombine();
            p.idMark.setIndexMenu(ConstNpc.IGNORE_MENU);
        }
    }
    private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
}
