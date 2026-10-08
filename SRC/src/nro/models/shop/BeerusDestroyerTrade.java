package nro.models.shop;

import java.util.ArrayList;
import java.util.List;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.*;
import nro.models.shop_ky_gui.ConsignTransactions;
import nro.models.services_func.TransactionService;
import nro.models.utils.Logger;

/** A checked, atomic exchange of equipped divine gear, bag food and ruby. */
public final class BeerusDestroyerTrade {
    private static final byte SPEC_SHOP = 3, COST_RUBY = 3;
    private static final int[] DIVINE = {555,556,557,558,559,560,561,562,563,564,565,566,567};
    private BeerusDestroyerTrade() {}

    public static void purchase(Player player, int itemId) {
        if (player == null || player.inventory == null || player.idMark == null) return;
        synchronized (player) {
            Shop shop = player.idMark.getShopOpen();
            if (shop == null || !"BILL".equals(shop.tagName)
                    || !"BILL".equals(player.idMark.getTagNameShop())
                    || shop.npcId != 55 || shop.typeShop != SPEC_SHOP
                    || itemId < 650 || itemId > 662) { notice(player,"Không thể thực hiện"); return; }
            ItemShop row = shop.getItemShop(itemId);
            if (row == null || row.temp == null || row.temp.id != itemId || row.temp.level != 14
                    || row.temp.type < 0 || row.temp.type > 4 || row.cost != 100
                    || row.typeSell != COST_RUBY || row.iconSpec != 7743) {
                notice(player,"Cấu hình đổi đồ Hủy Diệt không hợp lệ."); return;
            }
            InventoryService inventory = InventoryService.gI();
            int missing = 5 - inventory.validDivineBodySlots(player);
            if (missing > 0) { notice(player,"Thiếu " + missing + " món Thần trên người, không được mua."); return; }
            Item divine = player.inventory.itemsBody.get(row.temp.type);
            if ((row.temp.gender != 3 && row.temp.gender != player.gender)
                    || divine.template.id != DIVINE[itemId - 650] || divine.quantity != 1) {
                notice(player,"Cần đúng món Thần đang mặc tương ứng với món Hủy Diệt."); return;
            }
            if (player.baovetaikhoan || player.tradeWVP || TransactionService.isTrading(player)
                    || RubyGiftService.hasPending(player) || RubyGiftService.hasPendingId(player.id)
                    || CommesonChallengeService.hasPendingId(player.id)
                    || ConsignTransactions.hasPendingId(player.id) || player.walletPurchasePending != null
                    || (player.session != null && (ConsignTransactions.hasPendingAccount(player.session.userId)
                    || RubyGiftService.hasPendingAccount(player.session.userId)))) {
                notice(player,"Không thể đổi đồ khi tài khoản được bảo vệ hoặc đang có giao dịch."); return;
            }
            int foodId = inventory.findBillFoodId(player);
            if (foodId < 0) { notice(player,"Cần 99 thức ăn cùng loại trong hành trang."); return; }
            if (player.inventory.ruby < 100) { notice(player,"Cần 100 hồng ngọc để đổi đồ."); return; }
            Item reward;
            try { reward = ItemService.gI().createItemFromItemShop(new ItemShop(row)); DestroyerBonusRoll.apply(reward); }
            catch (RuntimeException failure) { notice(player,"Cấu hình đổi đồ Hủy Diệt không hợp lệ."); return; }
            Item.ItemOption bonus = reward.itemOptions.get(reward.itemOptions.size() - 1);
            String bonusName = switch (bonus.optionTemplate.id) { case 77 -> "HP"; case 103 -> "KI"; default -> "Sức đánh"; };
            String successMessage = "Nhận thành công " + row.temp.name + ": " + bonusName + "+" + bonus.param + "%.";
            BagSnapshot body = new BagSnapshot(player.inventory.itemsBody);
            BagSnapshot bag = new BagSnapshot(player.inventory.itemsBag);
            int ruby = player.inventory.ruby;
            boolean committed = false;
            try {
                int remaining = 99;
                for (Item food : new ArrayList<>(player.inventory.itemsBag)) {
                    if (!inventory.isBillIngredientUsable(food) || food.template.id != foodId) continue;
                    int quantity = Math.min(remaining, food.quantity);
                    int before = food.quantity;
                    inventory.subQuantityItemsBag(player, food, quantity);
                    if (food.quantity != before - quantity) throw new IllegalStateException("Food debit skipped");
                    remaining -= quantity;
                    if (remaining == 0) break;
                }
                if (remaining != 0) throw new IllegalStateException("Food debit incomplete");
                inventory.subQuantityItemsBody(player, divine, 1);
                if (player.inventory.itemsBody.get(row.temp.type).isNotNullItem()) throw new IllegalStateException("Equipped debit incomplete");
                player.inventory.ruby -= 100;
                if (!inventory.canAddItemBag(player, reward) || !inventory.addItemBag(player, reward)) return;
                committed = true;
            } catch (RuntimeException failure) {
                Logger.logException(BeerusDestroyerTrade.class, failure,"Beerus grant failed: player=" + player.id + ", item=" + itemId);
            } finally {
                if (!committed) {
                    body.restore(player.inventory.itemsBody);
                    bag.restore(player.inventory.itemsBag);
                    player.inventory.ruby = ruby;
                    notice(player,"Không thể nhận đồ Hủy Diệt, nguyên liệu và hồng ngọc đã được hoàn lại.");
                }
            }
            if (!committed) return;
            inventory.sendItemBody(player);
            Service.gI().point(player);
            Service.gI().Send_Caitrang(player);
            inventory.sendItemBags(player);
            Service.gI().sendMoney(player);
            notice(player,successMessage);
        }
    }

    private static void notice(Player player,String message) { Service.gI().sendThongBao(player,message); }

    /** Restore existing identities and every mutable Item field, without normalization. */
    private static final class BagSnapshot {
        final List<Item> original;
        final List<Item> values = new ArrayList<>();
        BagSnapshot(List<Item> items) {
            original = new ArrayList<>(items);
            for (Item item : items) values.add(item == null ? null : copy(item));
        }
        void restore(List<Item> items) {
            for (int index = 0; index < original.size(); index++) {
                Item item = original.get(index);
                if (item != null) assign(item,values.get(index));
            }
            items.clear(); items.addAll(original);
        }
        private static Item copy(Item item) { Item result = new Item(); assign(result,item); return result; }
        private static void assign(Item target,Item source) {
            target.template=source.template; target.info=source.info; target.content=source.content;
            target.quantity=source.quantity; target.quantityGD=source.quantityGD; target.createTime=source.createTime;
            target.id=source.id; target.text=source.text; target.expire=source.expire; target.options=source.options;
            target.itemOptions=source.itemOptions==null?null:new ArrayList<>();
            if(source.itemOptions!=null)for(Item.ItemOption option:source.itemOptions)
                target.itemOptions.add(option==null?null:new Item.ItemOption(option));
        }
    }
}
