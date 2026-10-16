package nro.models.shop;

import nro.models.consts.ConstAchievement;
import nro.models.item.Item;
import nro.models.player.Inventory;
import nro.models.player.Player;
import nro.models.shop.ItemShop;
import nro.models.shop.Shop;
import nro.models.shop.TabShop;
import nro.models.network.Message;
import nro.models.item.Item.ItemOption;
import java.util.ArrayList;
import nro.models.server.Manager;
import nro.models.services.InventoryService;
import nro.models.utils.Logger;
import nro.models.utils.Util;
import java.util.List;
import nro.models.npc.MagicTree;
import nro.models.player_badges.BadgesData;
import nro.models.player_badges.BadgesService;
import nro.models.player_badges.BagesTemplate;
import nro.models.services.AchievementService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import nro.models.services_func.Input;
import nro.models.services_func.BuyBackService;
import nro.models.services_func.UseItem;
import nro.models.map.service.NpcService;
import nro.models.task.BadgesTaskService;
import nro.models.utils.SkillUtil;
import nro.models.utils.TimeUtil;

/**
 *
 * @author By Mr Blue
 *
 */
public class ShopService {

    private static final byte COST_GOLD = 0;
    private static final byte COST_GEM = 1;
    private static final byte COST_RUBY = 3;
    private static final byte COST_COUPON = 4;

    private static final byte NORMAL_SHOP = 0;
    private static final byte SPEC_SHOP = 3;
    private static final byte KINANG_SHOP = 1;
    private int eventPointPrice;

    private static ShopService I;

    public static ShopService gI() {
        if (ShopService.I == null) {
            ShopService.I = new ShopService();
        }
        return ShopService.I;
    }

    public void opendShop(Player player, String tagName, boolean allGender) {
        synchronized(player) { synchronized(nro.models.services.LiveSyncCatalogCommands.LOCK) {
            if(nro.models.services.LiveSyncMutationGate.blocked(player))return;
            openCatalogShop(player,tagName,allGender);
        }}
    }

    private void openCatalogShop(Player player, String tagName, boolean allGender) {
        if (tagName.equals("ITEMS_LUCKY_ROUND")) {
            openShopType4(player, tagName, player.inventory.itemsBoxCrackBall);
            return;
        } else if (tagName.equals("ITEMS_DABAN")) {
            openShopType8(player, tagName, player.inventory.itemsDaBan);
            return;
        }
        try {
            Shop shop = this.getShop(tagName);
            for (TabShop tabShop : shop.tabShops) {
                for (ItemShop item : tabShop.itemShops) {
                    switch (item.temp.id) {
                        case 1627:// hành trang
                            if (player.inventory.itemsBag.size() >= 35) {
                                item.cost = ((player.inventory.itemsBag.size() - 35) + 1) * 2;
                            } else {
                                item.cost = 1;
                            }
                            break;
                    }
                }
            }
            shop = this.resolveShop(player, shop, allGender);
            switch (shop.typeShop) {
                case KINANG_SHOP:
                    openShopType1(player, shop);
                    break;
                case NORMAL_SHOP:
                    openShopType0(player, shop);
                    break;
                case SPEC_SHOP:
                    openShopType3(player, shop);
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Service.gI().sendThongBao(player, ex.getMessage());
        }
    }

    private Shop getShop(String tagName) throws Exception {
        for (Shop s : Manager.SHOPS) {
            if (s.tagName != null && s.tagName.equals(tagName)) {
                return s;
            }
        }
        throw new Exception("Shop " + tagName + " không tồn tại!");
    }

    private Shop resolveShop(Player player, Shop shop, boolean allGender) {
        if (shop.tagName != null && (shop.tagName.equals("BUA_1H") || shop.tagName.equals("BUA_8H") || shop.tagName.equals("BUA_1M"))) {
            return this.resolveShopBua(player, new Shop(shop));
        }
        Shop personal = allGender ? new Shop(shop) : new Shop(shop, player);
        if (!nro.models.services.AdvancedPetExchange.hasEligiblePet(player)) {
            for (TabShop tab : personal.tabShops) {
                tab.itemShops.removeIf(row -> row.temp.id == nro.models.services.AdvancedPetExchange.ITEM_ID);
            }
        }
        for(TabShop tab:personal.tabShops) tab.itemShops.removeIf(row -> row.temp!=null && nro.models.event.EventItemPolicy.isBlocked(row.temp.id));
        return personal;
    }

    private Shop resolveShopBua(Player player, Shop s) {
        for (TabShop tabShop : s.tabShops) {
            for (ItemShop item : tabShop.itemShops) {
                long min = 0;
                switch (item.temp.id) {
                    case 213:
                        long timeTriTue = player.charms.tdTriTue;
                        long current = System.currentTimeMillis();
                        min = (timeTriTue - current) / 60000;

                        break;
                    case 214:
                        min = (player.charms.tdManhMe - System.currentTimeMillis()) / 60000;
                        break;
                    case 215:
                        min = (player.charms.tdDaTrau - System.currentTimeMillis()) / 60000;
                        break;
                    case 216:
                        min = (player.charms.tdOaiHung - System.currentTimeMillis()) / 60000;
                        break;
                    case 217:
                        min = (player.charms.tdBatTu - System.currentTimeMillis()) / 60000;
                        break;
                    case 218:
                        min = (player.charms.tdDeoDai - System.currentTimeMillis()) / 60000;
                        break;
                    case 219:
                        min = (player.charms.tdThuHut - System.currentTimeMillis()) / 60000;
                        break;
                    case 522:
                        min = (player.charms.tdDeTu - System.currentTimeMillis()) / 60000;
                        break;
                    case 671:
                        min = (player.charms.tdTriTue3 - System.currentTimeMillis()) / 60000;
                        break;
                    case 672:
                        min = (player.charms.tdTriTue4 - System.currentTimeMillis()) / 60000;
                        break;
                }
                if (min > 0) {
                    item.options.clear();
                    if (min >= 1440) {
                        item.options.add(new Item.ItemOption(63, (int) min / 1440));
                    } else if (min >= 60) {
                        item.options.add(new Item.ItemOption(64, (int) min / 60));
                    } else {
                        item.options.add(new Item.ItemOption(65, (int) min));
                    }
                }
            }
        }
        return s;
    }

    private void openShopType0(Player player, Shop shop) {
        if (shop != null) {
            player.idMark.setShopOpen(shop);
            player.idMark.setTagNameShop(shop.tagName);
            Message msg = null;
            try {
                msg = new Message(-44);
                msg.writer().writeByte(NORMAL_SHOP);
                msg.writer().writeByte(shop.tabShops.size());
                for (TabShop tab : shop.tabShops) {
                    msg.writer().writeUTF(tab.name);
                    msg.writer().writeByte(tab.itemShops.size());
                    for (ItemShop itemShop : tab.itemShops) {
                        msg.writer().writeShort(itemShop.temp.id);
                        if (itemShop.typeSell == COST_GOLD) {
                            msg.writer().writeInt(itemShop.cost);
                            msg.writer().writeInt(0);
                        } else if (itemShop.typeSell == COST_GEM) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt(itemShop.cost);
                        } else if (itemShop.typeSell == COST_RUBY) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt(itemShop.cost);
                        } else if (itemShop.typeSell == COST_COUPON) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt(itemShop.cost);
                        }
                        msg.writer().writeByte(itemShop.options.size());
                        for (Item.ItemOption option : itemShop.options) {
                            msg.writer().writeByte(option.optionTemplate.id);
                            nro.models.network.ItemOptionWire.write(msg, player, option.param);
                        }
                        msg.writer().writeByte(itemShop.isNew ? 1 : 0);
                        if (itemShop.temp.type == 5) {
                            msg.writer().writeByte(1);
                            msg.writer().writeShort(itemShop.temp.head);
                            msg.writer().writeShort(itemShop.temp.body);
                            msg.writer().writeShort(itemShop.temp.leg);
                            msg.writer().writeShort(-1);
                        } else {
                            msg.writer().writeByte(0);
                        }
                    }
                }
                player.sendMessage(msg);
            } catch (Exception e) {
                Logger.logException(ShopService.class, e);
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
    }

    private void openShopDanhHieu(Player player, Shop shop) {
        if (shop != null) {
            player.idMark.setShopOpen(shop);
            player.idMark.setTagNameShop(shop.tagName);
            Message msg = null;
            try {
                msg = new Message(-44);
                msg.writer().writeByte(NORMAL_SHOP);
                msg.writer().writeByte(2);
                for (TabShop tab : shop.tabShops) {
                    msg.writer().writeUTF(tab.name);
                    msg.writer().writeByte(tab.itemShops.size());
                    for (ItemShop itemShop : tab.itemShops) {
                        msg.writer().writeShort(itemShop.temp.id);
                        if (itemShop.typeSell == COST_GOLD) {
                            msg.writer().writeInt(itemShop.cost);
                            msg.writer().writeInt(0);
                        } else if (itemShop.typeSell == COST_GEM) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt(itemShop.cost);
                        } else if (itemShop.typeSell == COST_RUBY) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt(itemShop.cost);
                        } else if (itemShop.typeSell == COST_COUPON) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt(itemShop.cost);
                        }
                        msg.writer().writeByte(itemShop.options.size());
                        for (Item.ItemOption option : itemShop.options) {
                            msg.writer().writeByte(option.optionTemplate.id);
                            nro.models.network.ItemOptionWire.write(msg, player, option.param);
                        }
                        msg.writer().writeByte(itemShop.isNew ? 1 : 0);
                        if (itemShop.temp.type == 5) {
                            msg.writer().writeByte(1);
                            msg.writer().writeShort(itemShop.temp.head);
                            msg.writer().writeShort(itemShop.temp.body);
                            msg.writer().writeShort(itemShop.temp.leg);
                            msg.writer().writeShort(-1);
                        } else {
                            msg.writer().writeByte(0);
                        }
                    }

                }
                player.sendMessage(msg);
            } catch (Exception e) {
                Logger.logException(ShopService.class, e);
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
    }

    private void openShopType1(Player player, Shop shop) {
        if (shop != null) {
            player.idMark.setShopOpen(shop);
            player.idMark.setTagNameShop(shop.tagName);
            Message msg = null;
            try {
                msg = new Message(-44);
                msg.writer().writeByte(KINANG_SHOP);
                msg.writer().writeByte(shop.tabShops.size());
                for (TabShop tab : shop.tabShops) {
                    msg.writer().writeUTF(tab.name);
                    msg.writer().writeByte(tab.itemShops.size());
                    for (ItemShop itemShop : tab.itemShops) {
                        msg.writer().writeShort(itemShop.temp.id);
                        String[] subName = itemShop.temp.name.split("");
                        byte level = Byte.parseByte(subName[subName.length - 1]);

                        var skillTemplateId = SkillUtil.getTempSkillSkillByItemID(itemShop.temp.id);
                        var costPotential = SkillUtil.findSkillTemplate(skillTemplateId).skillss.stream()
                                .filter(s -> s.point == level)
                                .findFirst()
                                .map(s -> (int) s.powRequire) // Ép kiểu Long -> int
                                .orElse(0); // Giá trị mặc định là int
                        msg.writer().writeLong(costPotential);

                        msg.writer().writeByte(itemShop.options.size());
                        for (Item.ItemOption option : itemShop.options) {
                            msg.writer().writeByte(option.optionTemplate.id);
                            nro.models.network.ItemOptionWire.write(msg, player, option.param);
                        }
                        msg.writer().writeByte(itemShop.isNew ? 1 : 0);

                        msg.writer().writeByte(0);

                    }
                }
                player.sendMessage(msg);
            } catch (Exception e) {
                Logger.logException(ShopService.class, e);
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
    }

    private void openShopType3(Player player, Shop shop) {
        player.idMark.setShopOpen(shop);
        player.idMark.setTagNameShop(shop.tagName);
        if (shop != null) {
            Message msg = null;
            try {
                msg = new Message(-44);
                msg.writer().writeByte(SPEC_SHOP);
                msg.writer().writeByte(shop.tabShops.size());
                for (TabShop tab : shop.tabShops) {
                    msg.writer().writeUTF(tab.name);
                    msg.writer().writeByte(tab.itemShops.size());
                    for (ItemShop itemShop : tab.itemShops) {
                        msg.writer().writeShort(itemShop.temp.id);
                        msg.writer().writeShort(itemShop.iconSpec);
                        msg.writer().writeInt(itemShop.cost);
                        msg.writer().writeByte(itemShop.options.size());
                        for (Item.ItemOption option : itemShop.options) {
                            msg.writer().writeByte(option.optionTemplate.id);
                            nro.models.network.ItemOptionWire.write(msg, player, option.param);
                        }
                        msg.writer().writeByte(itemShop.isNew ? 1 : 0);
                        if (itemShop.temp.type == 5) {
                            msg.writer().writeByte(1);
                            msg.writer().writeShort(itemShop.temp.head);
                            msg.writer().writeShort(itemShop.temp.body);
                            msg.writer().writeShort(itemShop.temp.leg);
                            msg.writer().writeShort(-1);
                        } else {
                            msg.writer().writeByte(0);
                        }
                    }
                }
                player.sendMessage(msg);
            } catch (Exception e) {
                Logger.logException(ShopService.class, e);
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
    }

    private void openShopType4(Player player, String tagName, List<Item> items) {
        if (items == null) {
            return;
        }
        player.idMark.setTagNameShop(tagName);
        Message msg = null;
        try {
            msg = new Message(-44);
            msg.writer().writeByte(4);
            msg.writer().writeByte(1);
            msg.writer().writeUTF("Phần\nthưởng");
            msg.writer().writeByte(items.size());
            for (Item item : items) {
                msg.writer().writeShort(item.template.id);
                msg.writer().writeUTF("|7| LUCKY REWARD");
                msg.writer().writeByte(item.itemOptions.size() + 1);
                for (Item.ItemOption io : item.itemOptions) {
                    msg.writer().writeByte(io.optionTemplate.id);
                    nro.models.network.ItemOptionWire.write(msg, player, io.param);
                }
                //số lượng
                msg.writer().writeByte(31);
                nro.models.network.ItemOptionWire.write(msg, player, item.quantity);
                //
                msg.writer().writeByte(1);
                if (item.template.type == 5) {
                    msg.writer().writeByte(1);
                    msg.writer().writeShort(item.template.head);
                    msg.writer().writeShort(item.template.body);
                    msg.writer().writeShort(item.template.leg);
                    msg.writer().writeShort(-1);
                } else {
                    msg.writer().writeByte(0);
                }
            }
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    private void openShopType8(Player player, String tagName, List<Item> items) {
        if (items == null) {
            return;
        }
        player.idMark.setTagNameShop(tagName);
        Message msg = null;
        try {
            msg = new Message(-44);
            msg.writer().writeByte(8);
            msg.writer().writeByte(1);
            msg.writer().writeUTF("Mua lại");
            msg.writer().writeByte(items.size());
            for (Item item : items) {
                int giamualaingoc = item.template.gem / 2;
                int giamualaivang = giamualaingoc == 0 ? (int) item.template.gold / 2 > 0 ? (int) item.template.gold / 2 : item.quantity * 100 : 0;
                msg.writer().writeShort(item.template.id);
                msg.writer().writeInt(giamualaivang);
                msg.writer().writeInt(giamualaingoc);
                msg.writer().writeInt(item.quantity);
                msg.writer().writeByte(item.itemOptions.size());
                for (Item.ItemOption io : item.itemOptions) {
                    msg.writer().writeByte(io.optionTemplate.id);
                    nro.models.network.ItemOptionWire.write(msg, player, io.param);
                }
                msg.writer().writeByte(0);
                if (item.template.type == 5) {
                    msg.writer().writeByte(1);
                    msg.writer().writeShort(item.template.head);
                    msg.writer().writeShort(item.template.body);
                    msg.writer().writeShort(item.template.leg);
                    msg.writer().writeShort(-1);
                } else {
                    msg.writer().writeByte(0);
                }
            }
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void takeItem(Player player, byte type, int tempId) {
        if (nro.models.item.DisabledItemRules.disabled(tempId)) {
            Service.gI().sendThongBao(player, "Vật phẩm này đang tạm ngưng bán.");
            return;
        }
        synchronized(player) { synchronized(nro.models.services.LiveSyncCatalogCommands.LOCK) {
            if(!currentShop(player))return;
            takeCurrentItem(player,type,tempId);
        }}
    }

    private boolean currentShop(Player player) {
        if(nro.models.services.LiveSyncMutationGate.blocked(player))return false;
        Shop shop=player.idMark.getShopOpen();
        if(shop!=null&&shop.catalogVersion!=nro.models.services.LiveSyncCatalogCommands.shopVersion()) {
            player.idMark.setShopOpen(null);
            Service.gI().sendThongBao(player,"Cửa hàng đã được cập nhật. Vui lòng mở lại để xem giá mới.");
            return false;
        }
        return true;
    }

    private void takeCurrentItem(Player player, byte type, int tempId) {
        String tagName = player.idMark.getTagNameShop();
        if (tagName == null || tagName.length() <= 0) {
            return;
        }
        if (tagName.equals("ITEMS_LUCKY_ROUND")) {
            getItemSideBoxLuckyRound(player, player.inventory.itemsBoxCrackBall, type, tempId);
            return;
        } else if (tagName.equals("ITEMS_REWARD")) {
            return;
        } else if (tagName.equals("ITEMS_DABAN")) {
            buyItemDaBan(player, player.inventory.itemsDaBan, type, tempId);
            return;
        } else if (tagName.equals("BILL")) {
            buyItemHD(player, tempId);
            return;
        }

        if (player.idMark.getShopOpen() == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        if (tagName.equals("BUA_1H") || tagName.equals("BUA_8H") || tagName.equals("BUA_1M")) {
            buyItemBua(player, tempId);
        } else if (tagName.equals("SANTA_HEAD")) {
            Item itS = ItemService.gI().createNewItem((short) tempId);
            player.head = (short) itS.template.head;
            Service.gI().Send_Caitrang(player);
            Service.gI().point(player);
            Service.gI().sendThongBao(player, "Đổi kiểu tóc thành công");
        } else {
            buyItem(player, tempId);
        }
        Service.gI().sendMoney(player);
    }

    private boolean subMoneyByItemShop(Player player, ItemShop is) {
        if(is.cost<0)return false;
        int gold = 0;
        int gem = 0;
        int ruby = 0;
        int coupon = 0;
        switch (is.typeSell) {
            case COST_GOLD ->
                gold = is.cost;
            case COST_GEM ->
                gem = is.cost;
            case COST_RUBY ->
                ruby = is.cost;
            case COST_COUPON ->
                coupon = is.cost;
            default -> { return false; }
        }
        if (player.inventory.gold < gold) {
            Service.gI().sendThongBao(player, "Bạn không có đủ vàng");
            return false;
        } else if (player.inventory.gem < gem) {
            Service.gI().sendThongBao(player, "Bạn không có đủ ngọc");
            return false;
        } else if (player.inventory.ruby < ruby) {
            Service.gI().sendThongBao(player, "Bạn không có đủ ngọc");
            return false;
        } else if (player.inventory.coupon < coupon) {
            Service.gI().sendThongBao(player, "Bạn không có đủ điểm");
            return false;
        }
        player.inventory.gold -= gold;
        player.inventory.gem -= gem;
        player.inventory.ruby -= ruby;
        player.inventory.coupon -= coupon;
        return true;
    }

    private boolean subMoneyByItemShopV2(Player player, ItemShop is) {
        if(is.cost<0)return false;
        int gold = 0;
        int gem = 0;
        int ruby = 0;
        int coupon = 0;
        switch (is.typeSell) {
            case COST_GOLD ->
                gold = is.cost;
            case COST_GEM ->
                gem = is.cost;
            case COST_RUBY ->
                ruby = is.cost;
            case COST_COUPON ->
                coupon = is.cost;
            default -> { return false; }
        }
        if (player.inventory.gold < gold) {
            Service.gI().sendThongBaoOK(player, "Bạn không đủ vàng, còn thiếu " + Util.numberToMoney(player.inventory.gold - gold));
            return false;
        } else if (player.inventory.gem < gem) {
            Service.gI().sendThongBaoOK(player, "Bạn không đủ ngọc, còn thiếu " + Util.numberToMoney(player.inventory.gem - gem));
            return false;
        } else if (player.inventory.ruby < ruby) {
            Service.gI().sendThongBaoOK(player, "Bạn không đủ ngọc, còn thiếu " + Util.numberToMoney(player.inventory.ruby - ruby));
            return false;
        } else if (player.inventory.coupon < coupon) {
            Service.gI().sendThongBaoOK(player, "Bạn không đủ điểm, còn thiếu " + Util.numberToMoney(player.inventory.coupon - coupon));
            return false;
        }
        player.inventory.gold -= gold;
        player.inventory.gem -= gem;
        player.inventory.ruby -= ruby;
        player.inventory.coupon -= coupon;
        Service.gI().sendMoney(player);
        return true;
    }

    /**
     * Mua bùa
     *
     * @param player người chơi
     * @param itemTempId id template vật phẩm
     */
    private void buyItemBua(Player player, int itemTempId) {
        Shop shop = player.idMark.getShopOpen();
        ItemShop is = shop.getItemShop(itemTempId);
        if (is == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        if (!subMoneyByItemShop(player, is)) {
            return;
        }
        InventoryService.gI().addItemBag(player, ItemService.gI().createItemFromItemShop(is));
        InventoryService.gI().sendItemBags(player);
        opendShop(player, shop.tagName, true);
    }

    /**
     * Mua vật phẩm trong cửa hàng
     *
     * @param player người chơi
     * @param itemTempId id template vật phẩm
     */
    private void learnKyNang(Player pl, ItemShop is) {

        if (pl.nPoint.power < is.temp.strRequire) {
            Service.gI().sendThongBao(pl, "Sức mạnh của bạn không đủ");
            return;
        }
        if (pl.nPoint.tiemNang < is.cost) {
            Service.gI().sendThongBao(pl, "Bạn không đủ tiềm năng để học chiêu thức này");
            return;
        }
        var skillPlayer = pl.playerSkill.getSkillbyId(SkillUtil.getSkillByItemID(pl, is.temp.id).template.id);
        String[] subName = is.temp.name.split("");
        byte level = Byte.parseByte(subName[subName.length - 1]);
        if (skillPlayer != null) {

            if (skillPlayer.point >= level) {
                Service.gI().sendThongBao(pl, "Bạn đã học kỹ năng này rồi");
                return;

            }
            if (level - skillPlayer.point != 1) {
                Service.gI().sendThongBao(pl, "Bạn chưa thể học kỹ năng này");
                return;
            }
        }
        if (pl.BoughtSkill.contains(is.temp.id)) {
            Service.gI().sendThongBao(pl, "Bạn đã học kỹ năng này rồi");
            return;
        }
        ArrayList<String> menu = new ArrayList<>();
        menu.add("Yes");
        menu.add("No");
        String[] menus = menu.toArray(String[]::new);
        long[] time = new long[]{900000, 1800000, 3600000, 86400000, 259200000, 604800000, 1296000000};
        var timeStudy = "";
        var timeLong = time[level - 1];
        switch (level) {
            case 0:
            case 1:
            case 2:
                timeStudy = TimeUtil.convertMillisecondToMinute(timeLong);
                break;
            case 3:
                timeStudy = TimeUtil.convertMillisecondToHour(timeLong);
                break;
            default:
                timeStudy = TimeUtil.convertMillisecondToDay(timeLong);
                break;
        }
        var skillTemplateId = SkillUtil.getTempSkillSkillByItemID(is.temp.id);

        var potential = SkillUtil.findSkillTemplate(skillTemplateId).skillss.stream()
                .filter(s -> s.point == level)
                .findFirst()
                .map(s -> (int) s.powRequire) // Ép kiểu Long -> int
                .orElse(0); // Giá trị mặc định là int
        String text = "Con có muốn học kỹ năng " + SkillUtil.findSkillTemplate(SkillUtil.getTempSkillSkillByItemID(is.temp.id)).name + " cấp " + level + "\nCần " + potential + " điểm tiềm năng và thời gian học là " + timeStudy;
        pl.LearnSkill.ItemTemplateSkillId = is.temp.id;
        pl.LearnSkill.Time = -1;
        pl.LearnSkill.Potential = potential;

        NpcService.gI().createMenuConMeo(pl, 671, NpcService.gI().getAvatar(13 + pl.gender), text, menus);
    }

    public void buyItem(Player player, int itemTempId) {
        if (nro.models.item.DisabledItemRules.disabled(itemTempId)) {
            Service.gI().sendThongBao(player, "Vật phẩm này đang tạm ngưng bán.");
            return;
        }
        synchronized(player) { try(nro.models.event.EventItemPolicy.Permit permit=nro.models.event.EventItemPolicy.admit()) { synchronized(nro.models.services.LiveSyncCatalogCommands.LOCK) {
            if(!currentShop(player))return;
            buyCurrentItem(player,itemTempId);
        }}}
    }

    private void buyCurrentItem(Player player, int itemTempId) {
        if (nro.models.event.EventItemPolicy.isBlocked(itemTempId)) {
            Service.gI().sendThongBao(player, "Sự kiện đã đóng."); return;
        }

        synchronized(player) {
        Shop shop = player.idMark.getShopOpen();
        if (shop == null) return;
        if ("BILL".equals(shop.tagName) || "BILL".equals(player.idMark.getTagNameShop())) {
            BeerusDestroyerTrade.purchase(player, itemTempId);
            return;
        }
        if ("SANTA_HONG_NGOC".equals(shop.tagName)
                || "SANTA_HONG_NGOC".equals(player.idMark.getTagNameShop())) {
            buySantaRubyItem(player, shop, itemTempId);
            return;
        }
        ItemShop is = shop.getItemShop(itemTempId);
        int[][] listDauThan = {{13, 293}, {60, 294}, {61, 295}, {62, 296}, {63, 297}, {64, 298}, {65, 299}, {352, 596}, {523, 597}};
        if (itemTempId == nro.models.services.AdvancedPetExchange.ITEM_ID
                && (!nro.models.services.AdvancedPetExchange.hasEligiblePet(player)
                || !"SANTA".equals(shop.tagName) || is == null
                || is.typeSell != COST_GEM || is.cost != 100)) {
            Service.gI().sendThongBao(player, "Cần có đệ Uub, Berus nhí hoặc Jiren để mua Đổi đệ tử 2.");
            return;
        }

        if (is == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }

        // Đổi bằng phiếu giảm giá
        if (is.tabShop.id == 30) {
            Item pGG = InventoryService.gI().findItem(player.inventory.itemsBag, 459);
            if (pGG != null) {
                Item item = ItemService.gI().createItemFromItemShop(is);
                InventoryService.gI().subQuantityItemsBag(player, pGG, 1);
                InventoryService.gI().addItemBag(player, item);
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendThongBao(player, "Đổi thành công " + is.temp.name);
            } else {
                Service.gI().sendThongBao(player, "Bạn không có phiếu giảm giá!");
            }
            return;
        }

        // Đổi danh hiệu
        if (is.tabShop.id == 44) {
            buyDanhHieu(player, is);
            return;
        }

        // Đổi danh hiệu khác
        if (is.tabShop.id == 45) {
            changeDanhHieu(player, is);
            return;
        }

        // Shop chipi
        if (is.tabShop.id == 49) {
            Item item = ItemService.gI().createItemFromItemShop(is);
            if (Util.isTrue(5, 100)) {
                item.itemOptions.add(new ItemOption(73, 0)); // HSD vĩnh viễn
            } else {
                item.itemOptions.add(new ItemOption(93, Util.nextInt(3, 7))); // HSD 3-7 ngày
            }
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            return;
        }

        // Shop kỹ năng
        if (shop.typeShop == ShopService.KINANG_SHOP) {
            learnKyNang(player, is);
            return;
        }

        Item prepared = ItemService.gI().createItemFromItemShop(is);
        prepared = buyMagicPean(player, listDauThan, prepared);
        if(prepared.template.id==1523||prepared.template.id==1524){
            prepared=ItemService.gI().createNewItem((short)521);
            for(Item.ItemOption opt:is.options)prepared.itemOptions.add(new Item.ItemOption(opt));
        }
        // A full bag is allowed when the final purchased item fits an existing stack.
        if (!InventoryService.gI().canAddItemBag(player, prepared)) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy");
            return;
        }

        // Yêu cầu có cải trang Quy Lão Kame
        if (itemTempId == 711 && !InventoryService.gI().findItemSkinQuyLaoKame(player)) {
            Service.gI().sendThongBao(player, "Bạn phải có cải trang thành Quy Lão Kame mới có thể đổi.");
            return;
        }

        // Kiểm tra auto train
        if (itemTempId == 1524 || itemTempId == 1523 || itemTempId == 521) {
            if (!checkAutoTrainPurchase(player, itemTempId)) {
                return;
            }
        }

        // Đổi bằng điểm sự kiện
        if (is.tabShop.id == 59) {
            int eventPointPrice = 0;

            switch (is.temp.id) {
                case 1567:
                case 1731:
                    eventPointPrice = 999;
                    break;
                case 1711:
                    eventPointPrice = 750;
                    break;
                case 1840:
                    eventPointPrice = 99;
                    break;
                case 1713:
                    eventPointPrice = 499;
                    break;
                case 1608:
                    eventPointPrice = 9;
                    break;
                case 1682:
                    eventPointPrice = 499;
                    break;
                case 1698:
                    eventPointPrice = 499;
                    break;
                case 1821:
                    eventPointPrice = 199;
                    break;
                case 1757:
                    eventPointPrice = 99;
                    break;
                case 1592:
                    eventPointPrice = 99;
                    break;
                default:
            }

            if (player.event.getEventPoint() < eventPointPrice) {
                Service.gI().sendThongBao(player, "Không đủ điểm sự kiện để mua vật phẩm này!");
                return;
            }

            player.event.subEventPoint(eventPointPrice);
            Item item = ItemService.gI().createItemFromItemShop(is);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Đã đổi " + is.temp.name + " bằng " + eventPointPrice + " điểm sự kiện.");
            return;
        }
        // Đổi bằng điểm Capsule Bang
        if (is.tabShop.id == 60 || is.tabShop.id == 61 || is.tabShop.id == 62) {
            int capsuleClanPointPrice = 0;

            switch (is.temp.id) {
                case 1794:
                case 1204:
                case 1423:
                case 1438:
                case 1439:
                case 987:
                case 1635:
                    capsuleClanPointPrice = 1;
                    break;
                case 1790:
                    capsuleClanPointPrice = 2;
                    break;
                case 1791:
                    capsuleClanPointPrice = 3;
                    break;
                case 1792:
                    capsuleClanPointPrice = 4;
                    break;
                case 1634:
                    capsuleClanPointPrice = 10;
                    break;
                case 1620:
                case 1748:
                case 1750:
                case 1729:
                case 1727:
                case 1714:
                case 1683:
                case 1682:
                case 1668:
                case 1629:
                case 1630:
                case 1631:
                case 1573:
                case 1550:
                case 1551:
                case 1541:
                case 1563:
                case 1724:
                case 1733:
                case 1734:
                case 1749:
                    capsuleClanPointPrice = 50;
                    break;
                default:
                    capsuleClanPointPrice = -1;
                    break;
            }

            if (capsuleClanPointPrice <= 0) {
                Service.gI().sendThongBao(player, "Vật phẩm này không thể mua bằng điểm Capsule Bang.");
                return;
            }

            if (player.clan == null) {
                Service.gI().sendThongBao(player, "Bạn không có trong bang hội!");
                return;
            }

            if (player.clan.capsuleClan < capsuleClanPointPrice) {
                Service.gI().sendThongBao(player, "Bang hội không đủ điểm Capsule Bang để mua vật phẩm này!");
                return;
            }

            player.clan.capsuleClan -= capsuleClanPointPrice;

            Item item = ItemService.gI().createItemFromItemShop(is);
            InventoryService.gI().addItemBag(player, item);
            InventoryService.gI().sendItemBags(player);

            Service.gI().sendThongBao(player, "Đã đổi " + is.temp.name + " bằng " + capsuleClanPointPrice + " điểm Capsule Bang của bang hội.");
            return;
        }

        long goldBefore=player.inventory.gold;int gemBefore=player.inventory.gem,rubyBefore=player.inventory.ruby,couponBefore=player.inventory.coupon;
        List<Item> bagBefore=shop.typeShop==ShopService.SPEC_SHOP?InventoryService.gI().copyItemsBag(player):null;
        // Shop thường
        if (shop.typeShop == ShopService.NORMAL_SHOP) {
            if (!subMoneyByItemShop(player, is)) {
                return;
            }
        } // Shop đặc biệt
        else if (shop.typeShop == ShopService.SPEC_SHOP) {
            if (!this.subIemByItemShop(player, is)) {
                return;
            }
        }

        // Preflight and grant run under the same player lock.
        Item item = prepared;
        if(!InventoryService.gI().addItemBag(player,item)) {
            player.inventory.gold=goldBefore;player.inventory.gem=gemBefore;
            player.inventory.ruby=rubyBefore;player.inventory.coupon=couponBefore;
            if(bagBefore!=null)player.inventory.itemsBag=bagBefore;
            Service.gI().sendThongBao(player,"Không thể nhận vật phẩm, chưa trừ phí mua.");
            return;
        }
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendThongBao(player, "Mua thành công " + is.temp.name);

        if (itemTempId == 1523 || itemTempId == 1524 || itemTempId == 521) {
            updateAutoTrainPurchase(player, itemTempId);
            Service.gI().sendThongBao(player, "Mua thành công " + is.temp.name);
        }
            }
    }

    /** Called under the player's monitor, also used by inventory and ruby gifts. */
    private void buySantaRubyItem(Player player, Shop shop, int itemTempId) {
        if (!"SANTA_HONG_NGOC".equals(shop.tagName)
                || !"SANTA_HONG_NGOC".equals(player.idMark.getTagNameShop())
                || shop.typeShop != SPEC_SHOP || shop.npcId != 39) return;
        if (nro.models.services.RubyGiftService.hasPending(player)) {
            Service.gI().sendThongBao(player, "Giao dịch hồng ngọc đang chờ đối chiếu.");
            return;
        }
        ItemShop row = shop.getItemShop(itemTempId);
        if (row == null || row.typeSell != COST_RUBY || row.cost <= 0
                || row.iconSpec != 7743 || (itemTempId != 710 && itemTempId != 711
                && itemTempId != 1710 && itemTempId != 1869 && itemTempId != 1775
                && itemTempId != 2042 && itemTempId != 2043
                && itemTempId != 880 && itemTempId != 881 && itemTempId != 882
                && !nro.models.item.AnimeEquipment.contains(itemTempId)
                && nro.models.shop_ky_gui.ConsignTransactions.rubyMaterialPrice(itemTempId)==0)) return;
        if (itemTempId == 2042 && row.cost != 100) return;
        if (itemTempId == 2043 && row.cost != 2000) return;
        if ((itemTempId >= 880 && itemTempId <= 882) && row.cost != 5) return;
        if(nro.models.item.AnimeEquipment.contains(itemTempId)&&row.cost!=200)return;
        int materialPrice=nro.models.shop_ky_gui.ConsignTransactions.rubyMaterialPrice(itemTempId);
        if(materialPrice>0) {
            if(row.cost!=materialPrice)return;
            var result=new nro.models.shop_ky_gui.ConsignTransactions().purchaseRubyMaterial(player,itemTempId);
            Service.gI().sendThongBao(player,result.message());
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendMoney(player);
            return;
        }
        if(itemTempId==1775) {
            if(row.cost!=50_000)return;
            var result=new nro.models.shop_ky_gui.ConsignTransactions().purchaseDivineBox(player);
            Service.gI().sendThongBao(player,result.message());
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendMoney(player);
            return;
        }
        int predecessor = itemTempId == 711 ? 710 : itemTempId == 1710 ? 711 : -1;
        if (predecessor != -1 && !ownsSantaPredecessor(player, predecessor)) {
            Service.gI().sendThongBao(player, "Bạn phải đang sở hữu cải trang "
                    + (predecessor == 710 ? "Quy lão Kame" : "Jacky Chun") + " để mua vật phẩm này.");
            return;
        }
        if (player.inventory.ruby < row.cost) {
            Service.gI().sendThongBao(player, "Bạn không đủ hồng ngọc để mua vật phẩm.");
            return;
        }
        Item prepared = ItemService.gI().createItemFromItemShop(row);
        nro.models.item.AnimeEquipment.freshPurchase(prepared, java.util.concurrent.ThreadLocalRandom.current());
        if (!InventoryService.gI().canAddItemBag(player, prepared)) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy.");
            return;
        }
        List<Item> previousBag = new ArrayList<>(player.inventory.itemsBag);
        int[] quantities = new int[previousBag.size()];
        for (int i = 0; i < quantities.length; i++)
            quantities[i] = previousBag.get(i) == null ? 0 : previousBag.get(i).quantity;
        int previousRuby = player.inventory.ruby;
        boolean granted = false;
        try {
            player.inventory.ruby -= row.cost;
            granted = InventoryService.gI().addItemBag(player, prepared);
        } catch (RuntimeException failure) {
            // Restore below, including a grant that modified the bag before throwing.
            Logger.logException(ShopService.class, failure,
                    "Santa ruby grant failed: player=" + player.id + ", item=" + itemTempId);
        } finally {
            if (!granted) {
                player.inventory.ruby = previousRuby;
                player.inventory.itemsBag.clear();
                player.inventory.itemsBag.addAll(previousBag);
                for (int i = 0; i < quantities.length; i++)
                    if (previousBag.get(i) != null) previousBag.get(i).quantity = quantities[i];
            }
        }
        if (!granted) {
            Service.gI().sendThongBao(player, "Không thể cấp vật phẩm, hồng ngọc đã được hoàn lại.");
            return;
        }
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        Service.gI().sendThongBao(player, "Mua thành công " + row.temp.name + " bằng hồng ngọc.");
    }

    private boolean ownsSantaPredecessor(Player player, int templateId) {
        long now = System.currentTimeMillis();
        for (List<Item> items : java.util.Arrays.asList(player.inventory.itemsBody,
                player.inventory.itemsBag, player.inventory.itemsBox)) {
            for (Item item : items) {
                if (item != null && item.isNotNullItem() && item.quantity > 0
                        && item.template.id == templateId
                        && (item.expire <= 0 || item.expire > now)
                        && !ItemService.gI().isEquippedOutOfDateTime(item, now)) return true;
            }
        }
        return false;
    }

    private boolean checkAutoTrainPurchase(Player player, int itemTempId) {
        int autoTrainState = player.autoTrainState;
        if (itemTempId == 1524 && autoTrainState != 2) {
            Service.gI().sendThongBao(player, "Bạn cần mua Tự động luyện tập 2 trước!");
            return false;
        } else if (itemTempId == 1523 && autoTrainState != 1) {  // sửa lại thành 'else if'
            Service.gI().sendThongBao(player, "Bạn cần mua Tự động luyện tập 1 trước!");
            return false;
        }

        return true;
    }

    private void updateAutoTrainPurchase(Player player, int itemTempId) {

        if (itemTempId == 1524) {
            player.autoTrainState = 0;

        } // Nếu mua Tự động luyện tập 1 (với ID 521)
        else if (itemTempId == 521) {
            // Nếu player đã có autoTrainState = 2, không thay đổi trạng thái nữa
            if (player.autoTrainState != 2) {
                player.autoTrainState = 1;
            }
        } else if (itemTempId == 1523) {
            player.autoTrainState = 2;
        }
    }

    private void buyDanhHieu(Player pl, ItemShop is) {
        int idEffect = BagesTemplate.fineIdEffectbyIdItem(is.temp.id);
        int percent = BadgesTaskService.sendPercenBadgesTask(pl, idEffect);

        if (percent < 100) {
            Service.gI().sendThongBao(pl, "Bạn chưa mở khóa danh hiệu này");
            return;
        }

        for (BadgesData badge : pl.dataBadges) {
            if (badge.idBadGes == idEffect) {
                Service.gI().sendThongBao(pl, "Bạn đã sở hữu danh hiệu này rồi");
                return;
            }
        }

        if(nro.models.player_badges.BadgeRules.template(idEffect)==null) return;
        new BadgesData(pl, idEffect, 30);

        BagesTemplate template = BagesTemplate.fineBadgesbyIdItem(is.temp.id);
        String badgeName = template != null ? template.NAME : "không rõ";

        Service.gI().sendThongBao(pl, "Bạn đã nhận được danh hiệu " + badgeName);
    }

    private void changeDanhHieu(Player pl, ItemShop is) {
        if(!nro.models.player_badges.BadgeRules.owned(pl,BagesTemplate.fineIdEffectbyIdItem(is.temp.id))) {
            Service.gI().sendThongBao(pl,"Bạn chưa sở hữu danh hiệu còn hạn này.");return;
        }
        if (!nro.models.player_badges.DragonBadgeRules.canUse(pl, BagesTemplate.fineIdEffectbyIdItem(is.temp.id))) {
            Service.gI().sendThongBao(pl, "Bạn cần đủ 7 loại rồng nhí vĩnh viễn để dùng danh hiệu Mẹ Rồng.");
            return;
        }
        if (pl.lastTimeChangeBadges - System.currentTimeMillis() > 0) {
            Service.gI().sendThongBao(pl, "Vui lòng đợi " + (pl.lastTimeChangeBadges - System.currentTimeMillis()) / 1000 + " giây nữa");
            return;
        }
        if (pl.badges.idBadges == BagesTemplate.fineIdEffectbyIdItem(is.temp.id)) {
            Service.gI().sendThongBao(pl, "Danh hiệu đang được sữ dụng, hãy chọn danh hiệu khác");
            pl.lastTimeChangeBadges = System.currentTimeMillis() + 3000;
            return;
        }
        BadgesService.turnOnBadges(pl, BagesTemplate.fineIdEffectbyIdItem(is.temp.id));
        Service.gI().sendThongBao(pl, "Đã đổi danh hiệu sang " + is.temp.name);
        pl.lastTimeChangeBadges = System.currentTimeMillis() + 3000;
        pl.nPoint.calPoint();
        pl.nPoint.setHp(pl.nPoint.hpMax);
        pl.nPoint.setMp(pl.nPoint.mpMax);
        pl.nPoint.setDame(pl.nPoint.dame);
        Service.gI().point(pl);
        Service.gI().Send_Info_NV(pl);
    }

    private boolean subIemByItemShop(Player pl, ItemShop itemShop) {
        if ("THIEN_SU".equals(pl.idMark.getTagNameShop())) {
            if (itemShop.typeSell != COST_RUBY || itemShop.cost != 10 || itemShop.iconSpec != 7743
                    || !"THIEN_SU".equals(pl.idMark.getShopOpen().tagName)) return false;
            if (nro.models.services.RubyGiftService.hasPending(pl) || pl.inventory.ruby < 10) {
                Service.gI().sendThongBao(pl, "Bạn không đủ hồng ngọc hoặc có giao dịch đang chờ.");
                return false;
            }
            pl.inventory.ruby -= 10;
            return true;
        }
        boolean isBuy = false;
        short itSpec = ItemService.gI().getItemIdByIcon((short) itemShop.iconSpec);
        int buySpec = itemShop.cost;
        Item itS = ItemService.gI().createNewItem(itSpec);
        switch (itS.template.id) {
            case 76:
            case 188:
            case 189:
            case 190:
                if (pl.inventory.gold >= buySpec) {
                    pl.inventory.gold -= buySpec;
                    isBuy = true;
                } else {
                    Service.gI().sendThongBao(pl, "Bạn Không Đủ Vàng Để Mua Vật Phẩm");
                    isBuy = false;
                }
                break;
            case 77:
                if (pl.inventory.gem >= buySpec) {
                    isBuy = true;
                } else {
                    Service.gI().sendThongBao(pl, "Bạn Không Đủ Ngọc Để Mua Vật Phẩm");
                    isBuy = false;
                }
                break;
            default:
                if (InventoryService.gI().findItemBag(pl, itSpec) == null || !InventoryService.gI().findItemBag(pl, itSpec).isNotNullItem()) {
                    Service.gI().sendThongBao(pl, "Không tìm thấy " + itS.template.name);
                    isBuy = false;
                } else if (InventoryService.gI().findItemBag(pl, itSpec).quantity < buySpec) {
                    Service.gI().sendThongBao(pl, "Bạn không có đủ " + buySpec + " " + itS.template.name);
                    isBuy = false;
                } else {
                    InventoryService.gI().subQuantityItemsBag(pl, InventoryService.gI().findItemBag(pl, itSpec), buySpec);
                    isBuy = true;
                }
                break;
        }
        return isBuy;
    }

    public void showConfirmSellItem(Player pl, int where, int index) {
        Item item = null;
        if (where == 0) {
            if (index < 0 || index >= pl.inventory.itemsBody.size()) {
                Service.gI().sendThongBao(pl, "Không thể thực hiện");
                return;
            }
            item = pl.inventory.itemsBody.get(index);
        } else {
            if (pl.getSession().version < 220) {
                index -= (pl.inventory.itemsBody.size() - 7);
            }
            if (index < 0 || index >= pl.inventory.itemsBag.size()) {
                Service.gI().sendThongBao(pl, "Không thể thực hiện");
                return;
            }
            item = pl.inventory.itemsBag.get(index);
        }
        if (item != null && item.isNotNullItem()) {
            if (item.template.id == 570) {
                Service.gI().sendThongBao(pl, "Bạn không thể bán vật phẩm này");
                return;
            }
            int quantity = item.quantity;
            if (quantity <= 0) {
                return;
            }
            long cost = item.template.gold;
            if (item.template.id == 457) {
                Input.gI().createFormBanSLL(pl);
                return;
            } else {
                cost /= 4;
            }
            if (cost == 0) {
                cost = 1;
            }
            cost *= quantity;

            String text = "Bạn có muốn bán\nx" + quantity
                    + " " + item.template.name + "\nvới giá là " + Util.numberToMoney(cost) + " vàng?";
            Message msg = null;
            try {
                msg = new Message(7);
                msg.writer().writeByte(where);
                msg.writer().writeShort(index);
                msg.writer().writeUTF(text);
                pl.sendMessage(msg);
            } catch (Exception e) {
            e.printStackTrace();
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
    }

    public void sellItem(Player pl, int where, int index) {
        synchronized (pl) {
            if(nro.models.services.LiveSyncMutationGate.blocked(pl))return;
            synchronized (pl.inventory) {
                sellItemLocked(pl, where, index);
            }
        }
    }

    private void sellItemLocked(Player pl, int where, int index) {
        if (pl.idMark.getShopOpen() == null || pl.idMark.getTagNameShop() == null) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            return;
        }
        if (index < 0 || index >= (where == 0 ? pl.inventory.itemsBody.size() : pl.inventory.itemsBag.size())) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            return;
        }
        Item item = null;
        if (where == 0) {
            item = pl.inventory.itemsBody.get(index);
        } else {
            item = pl.inventory.itemsBag.get(index);
        }
        if (item != null && item.isNotNullItem() && item.quantity > 0) {
            if (item.template.id == 570) {
                Service.gI().sendThongBao(pl, "Bạn không thể bán vật phẩm này");
                return;
            }
            if (InventoryService.gI().getParam(pl, 93, item.template.id) > 0) {
                Service.gI().sendThongBao(pl, "Bạn không thể bán vật phẩm có hạn sử dụng");
                return;
            }
            int quantity = item.quantity;
            long cost = item.template.gold;
            if (item.template.id == 457) {
                quantity = 1;
                cost = Inventory.GOLD_BAR_SELL_PRICE;
            } else {
                cost /= 4;
            }
            if (cost == 0) {
                cost = 1;
            }
            cost *= quantity;

            if (cost < 0 || cost > Inventory.LIMIT_GOLD - pl.inventory.gold) {
                Service.gI().sendThongBao(pl, "Vàng sau khi bán vượt quá giới hạn");
                return;
            }
            pl.inventory.gold += cost;
            Service.gI().sendMoney(pl);
            Service.gI().sendThongBao(pl, "Đã bán " + item.template.name
                    + " thu được " + Util.numberToMoney(cost) + " vàng");

            //Add vật phẩm đã bán
            if (item.template.id != 457) {
                BuyBackService.gI().addItem(pl, item);
            }
            if (where == 0) {
                InventoryService.gI().subQuantityItemsBody(pl, item, quantity);
                InventoryService.gI().sendItemBody(pl);
                Service.gI().Send_Caitrang(pl);
            } else {
                InventoryService.gI().subQuantityItemsBag(pl, item, quantity);
                InventoryService.gI().sendItemBags(pl);
            }
            if ("BUNMA".equals(pl.idMark.getTagNameShop())
                    || "DENDE".equals(pl.idMark.getTagNameShop())
                    || "APPULE".equals(pl.idMark.getTagNameShop())) {
                AchievementService.gI().checkDoneTask(pl, ConstAchievement.TRUM_NHAT_VE_CHAI, quantity);
            }
        } else {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        }
    }

    private void getItemSideBoxLuckyRound(Player player, List<Item> items, byte type, int index) {
        synchronized (player) {
        if (items == null) {
            return;
        }
        if (index < 0 || index >= items.size()) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        Item item = items.get(index);
        switch (type) {
            case 0: //nhận
                if (item.isNotNullItem()) {
                    if (InventoryService.gI().canAddItemBag(player, item)
                            && InventoryService.gI().addItemBag(player, item)) {
                        Service.gI().sendThongBao(player,
                                "Bạn nhận được " + (item.template.id == 189
                                        ? Util.numberToMoney(item.quantity) + " vàng" : item.template.name));
                        InventoryService.gI().sendItemBags(player);
                        items.remove(index);
                    } else {
                        Service.gI().sendThongBao(player, "Hành trang đã đầy");
                    }
                } else {
                    Service.gI().sendThongBao(player, "Không thể thực hiện");
                }
                break;
            case 1: //xóa
                items.remove(index);
                Service.gI().sendThongBao(player, "Xóa vật phẩm thành công");
                break;
            case 2: //nhận hết
                for (int i = items.size() - 1; i >= 0; i--) {
                    item = items.get(i);
                    if (InventoryService.gI().addItemBag(player, item)) {
                        Service.gI().sendThongBao(player,
                                "Bạn nhận được " + (item.template.id == 189
                                        ? Util.numberToMoney(item.quantity) + " vàng" : item.template.name));
                        items.remove(i);
                    }
                }
                InventoryService.gI().sendItemBags(player);
                break;
        }
        openShopType4(player, player.idMark.getTagNameShop(), items);
        }
    }

    private void buyItemDaBan(Player player, List<Item> items, byte type, int index) {
        if (items == null) {
            return;
        }
        if (index >= items.size()) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        Item item = items.get(index);
        int giamualaingoc = item.template.gem / 2;
        int giamualaivang = giamualaingoc == 0 ? (int) item.template.gold / 2 > 0 ? (int) item.template.gold / 2 : item.quantity * 100 : 0;
        if (giamualaivang > 0 && player.inventory.gold < giamualaivang) {
            Service.gI().sendThongBao(player, "Bạn không có đủ vàng!");
            return;
        }
        if (giamualaingoc > 0 && player.inventory.gem < giamualaingoc) {
            Service.gI().sendThongBao(player, "Bạn không có đủ ngọc xanh!");
            return;
        }
        player.inventory.gem -= giamualaingoc;
        player.inventory.gold -= giamualaivang;
        Service.gI().sendMoney(player);
        if (item.isNotNullItem()) {
            if (InventoryService.gI().getCountEmptyBag(player) != 0) {
                InventoryService.gI().addItemBag(player, item);
                Service.gI().sendThongBao(player,
                        "Bạn nhận được " + (item.template.id == 189
                                ? Util.numberToMoney(item.quantity) + " vàng" : item.template.name));
                InventoryService.gI().sendItemBags(player);
                items.remove(index);
            } else {
                Service.gI().sendThongBao(player, "Hành trang đã đầy");
            }
        } else {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
        }
        openShopType8(player, player.idMark.getTagNameShop(), items);
    }

    private void buyItemHD(Player player, int itemTempId) {
        BeerusDestroyerTrade.purchase(player, itemTempId);
    }

    private Item buyMagicPean(Player player, int[][] listDauThan, Item item) {
        for (int i = 0; i < listDauThan.length; i++) {
            if (item.template.id == listDauThan[i][1]) {
                item = ItemService.gI().createNewItem((short) listDauThan[i][0]);
                item.itemOptions.add(new Item.ItemOption(player.magicTree.level - 1 > 1 ? 2 : 48, MagicTree.PEA_PARAM[player.magicTree.level - 1]));
                item.quantity = 30;
                return item;
            }
        }
        return item;
    }

}
