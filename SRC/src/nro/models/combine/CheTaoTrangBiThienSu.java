package nro.models.combine;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.utils.Util;

/** Preview never rolls; confirmation publishes bag/body/ruby under the normal save lock. */
public final class CheTaoTrangBiThienSu {
    private CheTaoTrangBiThienSu() { }

    public static void showInfoCombine(Player player) {
        if (player == null || player.combineNew == null) return;
        synchronized (player) {
            player.combineNew.thienSuSelection = null;
            try {
                ThienSuCraftRecipe.Selection selection = ThienSuCraftRecipe.select(player);
                var result = ItemService.gI().getTemplate(selection.outputId());
                if (result == null) throw ThienSuCraftRecipe.invalid("Thiếu dữ liệu trang bị Thiên Sứ");
                String planet = new String[]{"Trái Đất", "Namek", "Xayda"}[(selection.outputId() - 1048) % 3];
                String body = selection.equipped().get(selection.bodySlot()).item().template.name;
                String text = "Chế tạo: " + result.name + " (" + planet + ")\n999 mảnh cùng loại + 1 công thức thường hoặc VIP"
                        + " + 1 đá nâng cấp + 1 đá may mắn.\nPhí: 50 hồng ngọc.\n"
                        + "Tỷ lệ thành công: " + probability(selection.successThreshold(), 101) + ".\n"
                        + "Chỉ số chính tăng 1–30% so với nền Thiên Sứ sau thưởng hành tinh; làm tròn có giới hạn.\n"
                        + "Tỷ lệ có dòng phụ: " + probability(selection.luckThreshold() + 1, 51) + ", có 1–3 dòng khác nhau, mỗi dòng 1–7%.\n"
                        + "Phải mặc đủ 5 món Hủy Diệt. Mất món đang mặc: " + body
                        + ", cùng nguyên liệu và phí, kể cả thất bại theo tỷ lệ.\nThành công nhận trang bị vào túi.";
                CombineService.gI().whis.createOtherMenu(player, CombineService.CHE_TAO_TRANG_BI_THIEN_SU,
                        text, "Chế tạo", "Từ chối");
                player.combineNew.thienSuSelection = selection;
            } catch (IllegalArgumentException | IllegalStateException ex) {
                Service.gI().sendThongBao(player, ex.getMessage());
            }
        }
    }

    private static String probability(int winning, int total) {
        return String.format(java.util.Locale.ROOT, "%.2f%%", winning * 100.0 / total);
    }

    public static void cancel(Player player) {
        if (player == null) return;
        synchronized (player) {
            if (player.combineNew != null) player.combineNew.clearItemCombine();
            if (player.idMark != null) player.idMark.setIndexMenu(ConstNpc.IGNORE_MENU);
        }
    }

    /** False denotes a legitimate RNG failure; rejection throws before payment. */
    public static boolean execute(Player player, IntUnaryOperator random) {
        if (player == null) throw ThienSuCraftRecipe.invalid("Hãy chọn lại nguyên liệu Thiên Sứ");
        synchronized (player) {
            if (player.combineNew == null || player.combineNew.typeCombine != CombineService.CHE_TAO_TRANG_BI_THIEN_SU
                    || player.combineNew.thienSuSelection == null || player.idMark == null
                    || player.idMark.getIndexMenu() != CombineService.CHE_TAO_TRANG_BI_THIEN_SU) {
                throw ThienSuCraftRecipe.invalid("Hãy chọn lại nguyên liệu để xác nhận chế tạo");
            }
            ThienSuCraftRecipe.Selection old = player.combineNew.thienSuSelection;
            player.combineNew.thienSuSelection = null;
            player.idMark.setIndexMenu(ConstNpc.IGNORE_MENU);
            synchronized (player.inventory) {
                ThienSuCraftRecipe.Selection current = ThienSuCraftRecipe.select(player);
                ThienSuCraftRecipe.validateSnapshot(old, current);
                // Only copy selected stacks. Generic copyItemsBag normalizes old equipment
                // and drops IDs/expiry; untouched assets retain their exact instances.
                List<Item> bag = new ArrayList<>(player.inventory.itemsBag);
                List<Item> body = new ArrayList<>(player.inventory.itemsBody);
                for (ThienSuCraftRecipe.Ingredient ingredient : current.ingredients()) {
                    Item copied = copyIngredient(ingredient.item());
                    copied.quantity -= ingredient.cost();
                    if (copied.quantity == 0) bag.set(ingredient.slot(), new Item());
                    else bag.set(ingredient.slot(), copied);
                }
                body.set(current.bodySlot(), new Item());
                int balance = player.inventory.ruby - ThienSuCraftRecipe.RUBY_FEE;
                boolean success = ThienSuCraftStats.roll(random, 101) < current.successThreshold();
                if (success) {
                    Item output = ItemService.gI().DoThienSu(current.outputId(), current.factoryGender());
                    if (output == null || output.template == null || output.template.id != current.outputId() || output.quantity != 1
                            || current.bag().contains(output) || current.body().contains(output)) {
                        throw ThienSuCraftRecipe.invalid("Không thể chuẩn bị trang bị Thiên Sứ; chưa trừ tài sản");
                    }
                    ThienSuCraftStats.apply(output, current.luckThreshold(), random);
                    nro.models.item.ItemDefaultOptions.complete(output);
                    if (!InventoryService.gI().addItemList(bag, output)) {
                        throw ThienSuCraftRecipe.invalid("Không thể nhận trang bị vào túi; chưa trừ tài sản");
                    }
                }
                // PlayerDAO serializes the whole state under the player monitor.
                player.inventory.itemsBag = bag;
                player.inventory.itemsBody = body;
                player.inventory.ruby = balance;
                player.combineNew.clearItemCombine();
                player.combineNew.clearParamCombine();
                long now = System.currentTimeMillis();
                player.idMark.setLastTimeCombine(now);
                player.combineNew.lastTimeCombine = now;
                return success;
            }
        }
    }

    private static Item copyIngredient(Item original) {
        Item copy = new Item();
        copy.template = original.template;copy.quantity = original.quantity;copy.quantityGD = original.quantityGD;
        copy.id = original.id;copy.expire = original.expire;copy.createTime = original.createTime;
        copy.info = original.info;copy.content = original.content;copy.text = original.text;
        for (Item.ItemOption option : original.itemOptions) copy.itemOptions.add(new Item.ItemOption(option));
        return copy;
    }

    public static void CheTaoTS(Player player) {
        if (player == null) return;
        synchronized (player) {
            boolean success;
            try { success = execute(player, Util::nextInt); }
            catch (Exception rejected) {
                cancel(player);
                try { Service.gI().sendThongBao(player, rejected.getMessage() == null
                        ? "Không thể chế tạo; chưa trừ tài sản" : rejected.getMessage()); }
                catch (Exception packetFailure) { }
                return;
            }
            // Committed outcome cannot be replayed after a refresh/packet failure.
            try { if (player.setClothes != null) player.setClothes.setup(); } catch (Exception refreshFailure) { }
            try { Service.gI().point(player); } catch (Exception refreshFailure) { }
            try { InventoryService.gI().sendItemBody(player); } catch (Exception packetFailure) { }
            try { Service.gI().Send_Caitrang(player); } catch (Exception packetFailure) { }
            try { InventoryService.gI().sendItemBags(player); } catch (Exception packetFailure) { }
            try { Service.gI().sendMoney(player); } catch (Exception packetFailure) { }
            try {
                if (success) CombineService.gI().sendEffectSuccessCombine(player);
                else CombineService.gI().sendEffectFailCombine(player);
                CombineService.gI().reOpenItemCombine(player);
            } catch (Exception packetFailure) { }
        }
    }
}
