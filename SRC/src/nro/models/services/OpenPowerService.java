package nro.models.services;

import java.util.ArrayList;
import java.util.List;
import nro.models.item.Item;
import nro.models.player.NPoint;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.services_func.TransactionService;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 * 
 */

public class OpenPowerService {

    public static final int COST_OPEN_LIMIT_POWER = 500_000_000;
    public static final int COST_SPEED_OPEN_LIMIT_POWER = COST_OPEN_LIMIT_POWER;
    public static final long COST_EQUIPMENT_OPEN_LIMIT_POWER = COST_OPEN_LIMIT_POWER;
    public static final long MIN_MASTER_POWER = 17_999_999_999L;

    public record Selection(Player target, Player wearer, Item item, int slot, byte limit,
                            long nextPower, String state, String description) { }

    private Player ownerOf(Player target) {
        return target instanceof Pet pet ? pet.master : target;
    }

    private void validateTarget(Player owner, Player target) {
        if (owner == null || target == null || owner.isPet || owner.nPoint == null
                || target.nPoint == null || owner.inventory == null || target.inventory == null
                || (target != owner && (target != owner.pet || !(target instanceof Pet)
                    || ((Pet) target).master != owner))) {
            throw invalid("Không tìm thấy bản thân hoặc đệ tử cần mở giới hạn");
        }
        if (owner.tradeWVP || TransactionService.isTrading(owner)
                || owner.idMark.getPlayerTrade() != null || owner.idMark.getPlayerTradeId() != -1)
            throw invalid("Hãy kết thúc giao dịch trước khi mở giới hạn");
    }

    private void validatePower(Player owner, Player target) {
        if (target.nPoint.limitPower < 0 || target.nPoint.limitPower >= NPoint.MAX_LIMIT)
            throw invalid("Đã đạt giới hạn sức mạnh tối đa");
        if (owner.nPoint.power < MIN_MASTER_POWER)
            throw invalid("Sư phụ cần đạt 17.999.999.999 sức mạnh để mở giới hạn");
    }

    public void checkEligibility(Player owner, Player target) {
        synchronized (owner) {
            validateTarget(owner, target);
            validatePower(owner, target);
        }
    }

    public String requiredEquipment(byte level) {
        return switch (level) {
            case 4, 5 -> "thần linh";
            case 6, 7 -> "hủy diệt";
            case 8 -> "thiên sứ";
            default -> throw invalid("Bậc này không mở bằng trang bị");
        };
    }

    private boolean eligible(Player target, Item item, int slot) {
        if (item == null || !item.isNotNullItem() || item.quantity != 1
                || slot < 0 || slot > 4 || item.template.type != slot
                || item.itemOptions == null || item.quantityGD != 0) return false;
        return switch (target.nPoint.limitPower) {
            case 4, 5 -> item.isDTL();
            case 6, 7 -> item.isDHD();
            case 8 -> item.isDTS();
            default -> false;
        };
    }

    private String itemState(Item item) {
        StringBuilder state = new StringBuilder().append(item.template.id).append(':')
                .append(item.template.type).append(':').append(item.template.gender).append(':')
                .append(item.quantity).append(':').append(item.quantityGD).append(':')
                .append(item.createTime).append(':').append(item.expire).append(':').append(item.id);
        for (Item.ItemOption option : item.itemOptions) {
            if (option == null || option.optionTemplate == null) throw invalid("Trang bị không hợp lệ");
            state.append(';').append(option.optionTemplate.id).append('=').append(option.param);
        }
        return state.toString();
    }

    public List<Selection> prepare(Player owner, Player target) {
        synchronized (owner) {
            cancel(owner);
            validateTarget(owner, target);
            validatePower(owner, target);
            String required = requiredEquipment(target.nPoint.limitPower);
            if (owner.inventory.gold < COST_EQUIPMENT_OPEN_LIMIT_POWER)
                throw invalid("Cần 500.000.000 vàng để mở giới hạn");
            List<Selection> choices = new ArrayList<>();
            String[] slots = {"Áo", "Quần", "Găng", "Giày", "Rada"};
            List<Player> wearers = target == owner ? List.of(owner) : List.of(target, owner);
            for (Player wearer : wearers) {
            for (int slot = 0; slot < Math.min(5, wearer.inventory.itemsBody.size()); slot++) {
                Item item = wearer.inventory.itemsBody.get(slot);
                if (eligible(target, item, slot)) {
                    String description = (wearer == owner ? "Bản thân (sư phụ)" : "Đệ tử")
                            + " - " + slots[slot] + ": " + item.template.name;
                    for (Item.ItemOption option : item.itemOptions) {
                        if (option == null || option.optionTemplate == null) throw invalid("Trang bị không hợp lệ");
                        if (option.optionTemplate.id == 107) description += " (" + option.param + " sao)";
                        if (option.optionTemplate.id == 72) description += " (cấp " + option.param + ")";
                    }
                    choices.add(new Selection(target, wearer, item, slot, target.nPoint.limitPower,
                            target.nPoint.getPowerNextLimit(), itemState(item), description));
                }
            }
            }
            if (choices.isEmpty()) throw invalid("Cần mặc 1 món " + required
                    + (target == owner ? " trên bản thân" : " trên sư phụ hoặc đệ tử"));
            List<Selection> immutable = List.copyOf(choices);
            owner.idMark.setPowerChoices(immutable);
            return immutable;
        }
    }

    public Selection choose(Player owner, int index) {
        synchronized (owner) {
            List<Selection> choices = owner.idMark.getPowerChoices();
            if (index < 0 || index >= choices.size()) {
                cancel(owner);
                throw invalid("Lựa chọn trang bị không còn hợp lệ");
            }
            Selection selected = choices.get(index);
            validateSelection(owner, selected);
            owner.idMark.setPowerConfirmation(selected);
            owner.idMark.setPowerChoices(List.of());
            return selected;
        }
    }

    private void validateSelection(Player owner, Selection selected) {
        if (selected == null) throw invalid("Hãy chọn trang bị và xác nhận lại");
        Player target = selected.target();
        validateTarget(owner, target);
        validatePower(owner, target);
        Player wearer = selected.wearer();
        if (target.nPoint.limitPower != selected.limit()
                || target.nPoint.getPowerNextLimit() != selected.nextPower()
                || (wearer != owner && wearer != target)
                || selected.slot() < 0 || selected.slot() >= wearer.inventory.itemsBody.size()
                || wearer.inventory.itemsBody.get(selected.slot()) != selected.item()
                || !eligible(target, selected.item(), selected.slot())
                || !itemState(selected.item()).equals(selected.state()))
            throw invalid("Trang bị hoặc giới hạn đã thay đổi, hãy chọn lại");
        if (owner.inventory.gold < COST_EQUIPMENT_OPEN_LIMIT_POWER)
            throw invalid("Cần 500.000.000 vàng để mở giới hạn");
    }

    public void execute(Player owner) {
        synchronized (owner) {
            synchronized (owner.inventory) {
            Selection selected = owner.idMark.getPowerConfirmation();
            cancel(owner);
            validateSelection(owner, selected);
            Item empty = ItemService.gI().createItemNull();
            selected.wearer().inventory.itemsBody.set(selected.slot(), empty);
            owner.inventory.gold -= COST_EQUIPMENT_OPEN_LIMIT_POWER;
            selected.target().nPoint.limitPower++;
            selected.target().itemTime.isOpenPower = false;
            }
        }
    }

    public void cancel(Player owner) {
        synchronized (owner) {
            owner.idMark.setPowerChoices(List.of());
            owner.idMark.setPowerConfirmation(null);
        }
    }

    public void notifyUpgrade(Player owner, Player target, Player wearer) {
        wearer.setClothes.setup();
        if (target != wearer) target.setClothes.setup();
        Service.gI().point(owner);
        InventoryService.gI().sendItemBody(owner);
        Service.gI().sendMoney(owner);
        Service.gI().Send_Caitrang(target);
        if (wearer != target) Service.gI().Send_Caitrang(wearer);
        if (target != owner) Service.gI().showInfoPet(owner);
        Service.gI().sendThongBao(owner, "Đã mở giới hạn "
                + (target == owner ? "bản thân" : "đệ tử") + " lên "
                + Util.numberToMoney(target.nPoint.getPowerLimit())
                + ". Đã tiêu hao 1 trang bị đang mặc và 500.000.000 vàng.");
    }

    public void finishPowerBasic(Player target) {
        Player owner = ownerOf(target);
        synchronized (owner) {
            if (!target.itemTime.isOpenPower) return;
            target.itemTime.isOpenPower = false;
            Service.gI().sendThongBao(owner, "Hãy đến Quốc Vương để mở giới hạn với phí 500.000.000 vàng mỗi bậc");
        }
    }

    private static IllegalArgumentException invalid(String text) { return new IllegalArgumentException(text); }

    private static final OpenPowerService INSTANCE = new OpenPowerService();
    private OpenPowerService() { }
    public static OpenPowerService gI() { return INSTANCE; }

    public boolean openPowerBasic(Player target) {
        Player owner = ownerOf(target);
        synchronized (owner) {
            target.itemTime.isOpenPower = false;
            Service.gI().sendThongBao(owner, "Hãy mở giới hạn tại Quốc Vương với phí 500.000.000 vàng mỗi bậc");
            return false;
        }
    }

    public boolean openPowerSpeed(Player target) {
        Player owner = ownerOf(target);
        synchronized (owner) {
            synchronized (owner.inventory) {
            try {
                validateTarget(owner, target);
                validatePower(owner, target);
                if (target.nPoint.limitPower >= 4)
                    throw invalid("Hãy mở giới hạn tại Quốc Vương bằng trang bị và vàng");
                if (owner.inventory.gold < COST_OPEN_LIMIT_POWER)
                    throw invalid("Cần 500.000.000 vàng để mở giới hạn");
                owner.inventory.gold -= COST_OPEN_LIMIT_POWER;
                target.nPoint.limitPower++;
                target.itemTime.isOpenPower = false;
                Service.gI().sendThongBao(owner, "Giới hạn sức mạnh của "
                        + (target == owner ? "bạn" : "đệ tử") + " đã được tăng lên 1 bậc");
                return true;
            } catch (IllegalArgumentException ex) {
                Service.gI().sendThongBao(owner, ex.getMessage());
                return false;
            }
            }
        }
    }
}
