package nro.models.combine;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.Service;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 */
public class PhaLeHoaTrangBi {

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() != 1) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Hãy chọn 1 vật phẩm để pha lê hóa", "Đóng");
            return;
        }

        Item item = player.combineNew.itemsCombine.get(0);
        if (!CombineSystem.isTrangBiPhaLeHoa(item)) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Vật phẩm này không thể đục lỗ", "Đóng");
            return;
        }

        int star = 0;
        for (Item.ItemOption io : item.itemOptions) {
            if (io.optionTemplate.id == 107) {
                star = io.param;
            }
        }
        if (star < 0 || star >= CombineService.MAX_STAR_ITEM) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, "Vật phẩm đã đạt tối đa sao pha lê", "Đóng");
            return;
        }

        QuyTrinh(player, item, star);
    }

    private static void QuyTrinh(Player player, Item item, int star) {
        player.combineNew.goldCombine = CombineSystem.getGoldPhaLeHoa(star);
        player.combineNew.gemCombine = CombineSystem.getGemPhaLeHoa(star);
        player.combineNew.ratioCombine = getRatio(star);

        String npcSay = item.template.name + "\n|2|";
        for (Item.ItemOption io : item.itemOptions) {
            if (io.optionTemplate.id != 102) {
                npcSay += io.getOptionString() + "\n";
            }
        }
        npcSay += "|7|Tỉ lệ thành công: " + player.combineNew.ratioCombine + "%" + "\n";

        if (player.combineNew.goldCombine <= player.inventory.gold) {
            npcSay += "|1|Cần " + Util.numberToMoney(player.combineNew.goldCombine) + " vàng";
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                    "Nâng cấp\ncần " + player.combineNew.gemCombine + " ngọc", "Nâng cấp 10 lần", "Nâng cấp 100 lần");
        } else {
            npcSay += "Còn thiếu " + Util.numberToMoney(player.combineNew.goldCombine - player.inventory.gold) + " vàng";
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, npcSay, "Đóng");
        }
    }

    public static float getRatio(int star) {
        return CombineSystem.getRatioPhaLeHoa(star);
    }

    static boolean rollSuccess(int star) {
        // Keep fractional percentages; the float overload truncates rates >= 1%.
        return Util.isTrue(Math.round(getRatio(star) * 1000f), 100_000L);
    }

    public static void phaLeHoa(Player player, int... numm) {
        // Cooldown, payment and the single-star update must be atomic per player.
        synchronized (player) {
            if (player.idMark == null
                    || !Util.canDoWithTime(player.idMark.getLastTimeCombine(), 500)) {
                return;
            }
            int n = numm.length > 0 ? numm[0] : 1;
            if ((n != 1 && n != 10 && n != 100)
                    || player.combineNew.itemsCombine.size() != 1) {
                Service.gI().sendThongBao(player, "Hãy chọn đúng 1 trang bị để pha lê hóa");
                return;
            }
            Item item = player.combineNew.itemsCombine.get(0);
            if (!CombineSystem.isTrangBiPhaLeHoa(item)
                    || !player.inventory.itemsBag.contains(item)) {
                Service.gI().sendThongBao(player, "Trang bị không hợp lệ hoặc không còn trong hành trang");
                return;
            }
            int star = 0;
            Item.ItemOption optionStar = null;
            for (Item.ItemOption io : item.itemOptions) {
                if (io.optionTemplate.id == 107) {
                    if (optionStar != null) {
                        Service.gI().sendThongBao(player, "Trang bị có dữ liệu sao pha lê không hợp lệ");
                        return;
                    }
                    star = io.param;
                    optionStar = io;
                }
            }
            if (star < 0 || star >= CombineService.MAX_STAR_ITEM) {
                Service.gI().sendThongBao(player, "Trang bị đã đạt tối đa 9 sao hoặc dữ liệu sao không hợp lệ");
                return;
            }
            // Derive costs from the item, never from a stale or cleared menu cache.
            int gold = CombineSystem.getGoldPhaLeHoa(star);
            int gem = CombineSystem.getGemPhaLeHoa(star);
            if (player.inventory.gold < gold || player.inventory.gem < gem) {
                Service.gI().sendThongBao(player, "Không đủ vàng hoặc ngọc để thực hiện");
                return;
            }
            player.idMark.setLastTimeCombine(System.currentTimeMillis());
            player.combineNew.goldCombine = gold;
            player.combineNew.gemCombine = gem;
            boolean success = false;
            for (int i = 0; i < n; i++) {
                if (player.inventory.gold < gold || player.inventory.gem < gem) {
                    break;
                }
                player.inventory.gold -= gold;
                player.inventory.gem -= gem;
                if (rollSuccess(star)) {
                    success = true;
                    // Batch buttons retry failures only; stop on the first success.
                    break;
                }
            }

            if (success) {
                star = star + 1;
                if (item != null) {
                    if (optionStar == null) {
                        item.itemOptions.add(new Item.ItemOption(107, star));
                    } else {
                        optionStar.param = star;
                    }
                    //  ChatGlobalService.gI().ThongBaoDapDo(player, "Chúc mừng " + player.name + " vừa pha lê hóa thành công " + item.template.name + " lên " + star + " sao pha lê");
                }
                CombineService.gI().sendEffectSuccessCombine(player);
                CombineService.gI().baHatMit.npcChat(player, "Chúc mừng con nhé");
            } else {
                CombineService.gI().sendEffectFailCombine(player);

                String[] failMessages = {
                    "Tay run à, đập kiểu gì thế?",
                    "Lại xịt rồi, hahaha...",
                    "Ngon bắt được con lợn béo rồi...!",
                    "Làm lại đi, biết đâu lần sau đỏ!",
                    "Lần sau nhớ khấn trước khi đập!",
                    "Kỹ năng quá kém?",
                    "Hên xui thôi mà, đừng cay!",
                    "Còn vàng còn ngọc, đập tiếp đi!"
                };
                String msg = failMessages[Util.nextInt(failMessages.length)];
                CombineService.gI().baHatMit.npcChat(player, msg);
            }

            InventoryService.gI().sendItemBags(player);
            Service.gI().sendMoney(player);
            CombineService.gI().reOpenItemCombine(player);
        }
    }
}
