package nro.models.npc_list;

import java.util.List;
import nro.models.consts.ConstNpc;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.OpenPowerService;
import nro.models.services.OpenPowerService.Selection;
import nro.models.services.Service;
import nro.models.utils.Util;

public class QuocVuong extends Npc {

    public QuocVuong(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        synchronized (player) {
            OpenPowerService.gI().cancel(player);
            if (canOpenNpc(player)) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "Con muốn nâng giới hạn sức mạnh cho bản thân hay đệ tử?",
                        "Bản thân", "Đệ tử", "Từ chối");
            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        synchronized (player) {
            if (!canOpenNpc(player)) {
                OpenPowerService.gI().cancel(player);
                return;
            }
            try {
                int menu = player.idMark.getIndexMenu();
                if (player.idMark.isBaseMenu()) {
                    OpenPowerService.gI().cancel(player);
                    switch (select) {
                        case 0 -> showUpgradeMenu(player, player);
                        case 1 -> showUpgradeMenu(player, player.pet);
                    }
                } else if (menu == ConstNpc.POWER_SELECT_ITEM) {
                    List<Selection> choices = player.idMark.getPowerChoices();
                    if (choices != null && select == choices.size()) {
                        closeUpgradeMenu(player);
                        return;
                    }
                    Selection selection = OpenPowerService.gI().choose(player, select);
                    this.createOtherMenu(player, ConstNpc.POWER_CONFIRM,
                            "Mở giới hạn sức mạnh " + targetName(player, selection.target())
                            + " lên " + Util.numberToMoney(selection.nextPower())
                            + "\nTiêu hao: " + selection.description()
                            + "\nTrang bị đang mặc của " + (selection.wearer() == player ? "sư phụ" : "đệ tử") + "."
                            + "\n" + selection.item().getInfo()
                            + "\nPhí: 500.000.000 vàng của con."
                            + "\nMón đang mặc sẽ mất vĩnh viễn, gồm toàn bộ sao, cấp nâng và tùy chọn.",
                            "Mở giới hạn", "Hủy");
                } else if (menu == ConstNpc.POWER_CONFIRM) {
                    if (select != 0) {
                        closeUpgradeMenu(player);
                        return;
                    }
                    Selection confirmation = player.idMark.getPowerConfirmation();
                    if (confirmation == null) {
                        throw new IllegalStateException("Xác nhận đã hết hiệu lực, hãy chọn lại món đồ.");
                    }
                    OpenPowerService.gI().execute(player);
                    player.idMark.setIndexMenu(ConstNpc.IGNORE_MENU);
                    OpenPowerService.gI().notifyUpgrade(player, confirmation.target(), confirmation.wearer());
                } else if (menu == ConstNpc.OPEN_POWER_MYSEFT) {
                    if (select == 0) {
                        openLegacySpeed(player, player);
                    } else {
                        closeUpgradeMenu(player);
                    }
                } else if (menu == ConstNpc.OPEN_POWER_PET) {
                    if (select == 0) {
                        openLegacySpeed(player, player.pet);
                    } else {
                        closeUpgradeMenu(player);
                    }
                }
            } catch (IllegalArgumentException | IllegalStateException e) {
                closeUpgradeMenu(player);
                Service.gI().sendThongBao(player, e.getMessage());
            }
        }
    }

    private void showUpgradeMenu(Player owner, Player target) {
        OpenPowerService.gI().checkEligibility(owner, target);
        if (target.nPoint.limitPower >= 4) {
            List<Selection> choices = OpenPowerService.gI().prepare(owner, target);
            String[] buttons = new String[choices.size() + 1];
            for (int i = 0; i < choices.size(); i++) {
                buttons[i] = choices.get(i).description();
            }
            buttons[choices.size()] = "Hủy";
            this.createOtherMenu(owner, ConstNpc.POWER_SELECT_ITEM,
                    "Giới hạn hiện tại " + targetName(owner, target) + ": "
                    + Util.numberToMoney(target.nPoint.getPowerLimit())
                    + "\nGiới hạn sau mở: " + Util.numberToMoney(target.nPoint.getPowerNextLimit())
                    + (target == owner
                            ? "\nChọn một món đang mặc của sư phụ để tiêu hao cùng 500.000.000 vàng."
                            : "\nChọn một món đang mặc của sư phụ hoặc đệ tử để tiêu hao cùng 500.000.000 vàng.")
                    + "\nMón đã chọn sẽ mất vĩnh viễn, gồm toàn bộ sao, cấp nâng và tùy chọn.", buttons);
        } else if (target == owner) {
            this.createOtherMenu(owner, ConstNpc.OPEN_POWER_MYSEFT,
                    "Ta sẽ mở giới hạn sức mạnh của con lên "
                    + Util.numberToMoney(target.nPoint.getPowerNextLimit())
                    + "\nCó thể mở ngay, không cần đạt mốc sức mạnh từng bậc.",
                    "Mở ngay\n500.000.000 vàng", "Đóng");
        } else {
            this.createOtherMenu(owner, ConstNpc.OPEN_POWER_PET,
                    "Ta sẽ mở giới hạn sức mạnh đệ tử lên "
                    + Util.numberToMoney(target.nPoint.getPowerNextLimit())
                    + "\nCó thể mở ngay, không cần đạt mốc sức mạnh từng bậc.",
                    "Mở ngay\n500.000.000 vàng", "Đóng");
        }
    }

    private void openLegacySpeed(Player owner, Player target) {
        synchronized (owner.inventory) {
        requireLegacyTarget(target);
        long cost = OpenPowerService.COST_SPEED_OPEN_LIMIT_POWER;
        if (owner.inventory.gold < cost) {
            throw new IllegalArgumentException("Con không đủ vàng, còn thiếu "
                    + Util.numberToMoney(cost - owner.inventory.gold) + " vàng.");
        }
        if (OpenPowerService.gI().openPowerSpeed(target)) {
            Service.gI().sendMoney(owner);
            closeUpgradeMenu(owner);
        }
        }
    }

    private void requireLegacyTarget(Player target) {
        if (target == null) {
            throw new IllegalArgumentException("Con chưa có đệ tử để mở giới hạn.");
        }
        if (target.nPoint.limitPower < 0 || target.nPoint.limitPower >= 4) {
            throw new IllegalArgumentException("Hãy mở lại menu để chọn món đang mặc theo giới hạn hiện tại.");
        }
    }

    private void closeUpgradeMenu(Player owner) {
        OpenPowerService.gI().cancel(owner);
        owner.idMark.setIndexMenu(ConstNpc.IGNORE_MENU);
    }

    private String targetName(Player owner, Player target) {
        return target == owner ? "của con" : "của đệ tử";
    }
}
