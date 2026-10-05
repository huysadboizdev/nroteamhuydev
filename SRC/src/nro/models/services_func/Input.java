package nro.models.services_func;

import nro.models.clan.Clan;
import nro.models.clan.ClanMember;
import nro.models.data.LocalManager;
import nro.models.consts.ConstNpc;
import nro.models.database.PlayerDAO;
import nro.models.item.Item;
import nro.models.map.Zone;
import nro.models.npc.Npc;
import nro.models.map.service.NpcManager;
import nro.models.player.Player;
import nro.models.network.Message;
import nro.models.interfaces.ISession;
import nro.models.item.Item.ItemOption;
import nro.models.server.Client;
import nro.models.services.Service;
import nro.models.services.AdminCommandService;
import nro.models.services.AdminPlayerActions;
import nro.models.utils.Logger;
import nro.models.services.GiftCodeService;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.map.service.NpcService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nro.models.Bot.Bot;
import nro.models.Bot.BotAttackplayer;
import nro.models.Bot.BotManager;
import nro.models.Bot.NewBot;
import nro.models.Bot.BotGiaoDich;
import nro.models.consts.ConstTaskBadges;
import nro.models.player.Inventory;
import nro.models.server.Manager;
import nro.models.services.ClanService;
import nro.models.map.service.ChangeMapService;
import nro.models.services.PlayerService;
import nro.models.server.ServerLog;
import nro.models.task.BadgesTaskService;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 *
 */
public class Input {

    private static final Map<Long, Object> PLAYER_ID_OBJECT = new java.util.concurrent.ConcurrentHashMap<>();

    public static final int CHANGE_PASSWORD = 500;
    public static final int GIFT_CODE = 501;
    public static final int FIND_PLAYER = 502;
    public static final int FIND_PLAYER_NAME = 502992;
    public static final int CHANGE_NAME = 503;
    public static final int CHOOSE_LEVEL_BDKB = 504;
    public static final int NAP_THE = 505;
    public static final int CHANGE_NAME_BY_ITEM = 506;
    public static final int GIVE_IT = 507;
    public static final int GET_IT = 508;
    public static final int DANGKY = 509;
    public static final int TRADE_GOLD = 5100;
    public static final int TRADE_GEM = 5101;
    public static final int CHOOSE_LEVEL_KGHD = 510;
    public static final int CHOOSE_LEVEL_CDRD = 511;
    public static final int DISSOLUTION_CLAN = 513;
    public static final int TANG_NGOC_HONG = 62001;
    public static final int RUBY_GIFT_CONFIRM = 62002;
    public static final int SELECT_LUCKYNUMBER = 514;
    public static final byte NUMERIC = 0;
    public static final byte ANY = 1;
    public static final byte NUMBER = 81;
    public static final byte PASSWORD = 2;
    public static final byte MBV = 23;
    public static final byte BANSLL = 24;
    public static final byte BANGHOI = 25;
    public static final int CON_SO_MAY_MAN_NGOC = 26;
    public static final int CON_SO_MAY_MAN_VANG = 27;
    public static final int TRONG_DUA = 28;
    public static final int SEND_ITEM_OP = 29;
    public static final int BOTQUAI = 30;
    public static final int BOTITEM = 31;
    public static final int BOTBOSS = 32;
    public static final int BOTATTACKPLAYER = 33;
    public static final int FIND_PLAYER_GIFT_RUBY = 34;

    private static Input intance;

    private Input() {

    }

    public static Input gI() {
        if (intance == null) {
            intance = new Input();
        }
        return intance;
    }

    public void doInput(Player player, Message msg) {
        try {
            int type = player.idMark.getTypeInput();
            if (isAdminInput(type) && !AdminCommandService.requireAdmin(player)) return;
            int count = msg.reader().readUnsignedByte();
            int expected = expectedAdminFields(type);
            if (count > 32 || (expected >= 0 && count != expected))
                throw new IllegalArgumentException("Số trường nhập không hợp lệ");
            String[] text = new String[count];
            for (int i = 0; i < text.length; i++) {
                text[i] = msg.reader().readUTF();
            }
            switch (type) {
                case BOTITEM -> {
                    int slot = botCount(text[0]);
                    int idBan = Integer.parseInt(text[1]);
                    int idTraoDoi = Integer.parseInt(text[2]);
                    int slot_TraoDoi = Integer.parseInt(text[3]);
                    AdminCommandService.validateItem(idBan,1);
                    AdminCommandService.validateItem(idTraoDoi,slot_TraoDoi);
                    BotGiaoDich bs = new BotGiaoDich(idBan, idTraoDoi, slot_TraoDoi);
                    launchAdminBots(player,1,bs,slot);
                }
                case BOTBOSS -> {
                    int slot = botCount(text[0]);
                    launchAdminBots(player,2,null,slot);
                }
                case BOTQUAI -> {
                    int slot = botCount(text[0]);
                    launchAdminBots(player,0,null,slot);
                }
                case BOTATTACKPLAYER -> {
                    botCount(text[0]);
                    if (AdminCommandService.requireAdmin(player))
                        Service.gI().sendThongBao(player,"Bot tấn công người chơi chưa được hỗ trợ.");
                }

                case TRADE_GOLD -> {
                    synchronized(player) {
                        if (nro.models.services.RubyGiftService.hasPending(player)) return;
                        if(PlayerDAO.resumeWalletPurchase(player))break;
                        int cuantity1 = Integer.parseInt(text[0]);
                        if (!player.getSession().actived) {
                            Service.gI().sendThongBao(player, "Vui lòng kích hoạt tài khoản!");
                            break;
                        }
                        if (cuantity1 < 10000 || cuantity1 > 5_000_000) {
                            Service.gI().sendThongBao(player, "Tối thiểu 10.000Đ và tối đa 5.000.000Đ");
                            break;
                        }
                        if (player.getSession().vnd < cuantity1) {
                            Service.gI().sendThongBao(player, "Số dư không đủ, vui lòng nạp thêm");
                        } else {
                            var purchaseResult=PlayerDAO.purchase(player,"gold_exchange",cuantity1,()-> {

                                int soLuongThoiVang = (cuantity1 / 1000) * 4;
                                Item item457 = ItemService.gI().createNewItem((short) 457, soLuongThoiVang);
                                InventoryService.gI().addItemBag(player, item457);

                                int soLuongVe = (cuantity1 / 10000) * 10;
                                Item item718 = ItemService.gI().createNewItem((short) 718, soLuongVe);
                                InventoryService.gI().addItemBag(player, item718);

                                BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.DAI_GIA_MOI_NHU, cuantity1);
                                BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.EM_XINH_EM_DEP, cuantity1);

                                int eventPointBonus = (cuantity1 / 10000) * 50;
                                player.event.addEventPoint(eventPointBonus);
                                InventoryService.gI().sendItemBags(player);
                                Service.gI().sendMoney(player);

                                Service.gI().sendThongBao(player, "Bạn nhận thêm " + eventPointBonus + " điểm sự kiện!");
                                Service.gI().sendThongBao(player, "Đã đổi thành công. Bạn nhận được " + soLuongThoiVang + " thỏi vàng và " + soLuongVe + " vé tặng ngọc.");
                            });
                            PlayerDAO.notifyWalletPurchaseResult(player,purchaseResult);
                        }
                    }
                }

                case TRADE_GEM -> {
                    synchronized(player) {
                        if (nro.models.services.RubyGiftService.hasPending(player)) return;
                        if(PlayerDAO.resumeWalletPurchase(player))break;
                        int quantity = Integer.parseInt(text[0]);
                        if (quantity < 10000 || quantity > 5_000_000) {
                            Service.gI().sendThongBao(player, "Tối thiểu 10.000Đ và tối đa 5.000.000Đ");
                            break;
                        }
                        if (player.getSession().vnd < quantity) {
                            Service.gI().sendThongBao(player, "Số dư không đủ, vui lòng nạp thêm");
                        } else {
                            var purchaseResult=PlayerDAO.purchase(player,"gem_exchange",quantity,()-> {

                                player.inventory.gem += quantity;
                                int soLuongVe = (quantity / 10000) * 10;
                                Item item718 = ItemService.gI().createNewItem((short) 718, soLuongVe);
                                InventoryService.gI().addItemBag(player, item718);

                                BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.DAI_GIA_MOI_NHU, quantity);
                                BadgesTaskService.updateCountBagesTask(player, ConstTaskBadges.EM_XINH_EM_DEP, quantity);

                                int eventPointBonus = (quantity / 10000) * 50;
                                player.event.addEventPoint(eventPointBonus);
                                InventoryService.gI().sendItemBags(player);
                                Service.gI().sendMoney(player);

                                Service.gI().sendThongBao(player, "Bạn nhận thêm " + eventPointBonus + " điểm sự kiện!");
                                Service.gI().sendThongBao(player, "Đã nạp thành công. Bạn nhận được " + quantity + " ngọc và " + soLuongVe + " vé tặng ngọc.");
                            });
                            PlayerDAO.notifyWalletPurchaseResult(player,purchaseResult);
                        }
                    }
                }

                case SEND_ITEM_OP -> grantForm(player, text, false, true);
                case CON_SO_MAY_MAN_NGOC -> {
                    int CSMM2 = Integer.parseInt(text[0]);
                    if (CSMM2 >= MiniGame.gI().MiniGame_S1_Gem.min && CSMM2 <= MiniGame.gI().MiniGame_S1_Gem.max && MiniGame.gI().MiniGame_S1_Gem.second > 10) {
                        MiniGame.gI().MiniGame_S1_Gem.newData(player, CSMM2);
                    } else {
                        Service.gI().sendThongBao(player, "Số bạn chọn không hợp lệ hoặc đã hết thời gian đặt cược.");
                    }
                }
                case CON_SO_MAY_MAN_VANG -> {
                    int CSMM2 = Integer.parseInt(text[0]);
                    if (CSMM2 >= MiniGame.gI().MiniGame_S1_Gold.min && CSMM2 <= MiniGame.gI().MiniGame_S1_Gold.max && MiniGame.gI().MiniGame_S1_Gold.second > 10) {
                        MiniGame.gI().MiniGame_S1_Gold.newData(player, CSMM2);
                    } else {
                        Service.gI().sendThongBao(player, "Số bạn chọn không hợp lệ hoặc đã hết thời gian đặt cược.");
                    }
                }
                case GIVE_IT -> grantForm(player, text, false, false);
                case GET_IT -> grantForm(player, text, true, false);
                case CHANGE_PASSWORD ->
                    Service.gI().changePassword(player, text[0], text[1], text[2]);
                case GIFT_CODE ->
                    GiftCodeService.gI().giftCode(player, text[0]);
                case FIND_PLAYER -> {
                    Player pl = Client.gI().getPlayer(text[0]);
                    if (AdminPlayerActions.isOnline(pl) && AdminCommandService.requireAdmin(player)) {
                        NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_FIND_PLAYER, -1, "Ngài muốn..?",
                                new String[]{"Đi tới\n" + pl.name, "Gọi " + pl.name + "\ntới đây", "Đổi tên", "Ban", "Kick"},
                                pl);
                    } else {
                        Service.gI().sendThongBao(player, "Người chơi không tồn tại hoặc đang offline");
                    }
                }
                case FIND_PLAYER_NAME -> {
                    Player target = Client.gI().getPlayer(text[0]);
                    if (target != null) {
                        player.menuPlayer = target;
                        Input.gI().createForm(player, FIND_PLAYER_GIFT_RUBY, "Nhập số ngọc bạn muốn tặng cho " + target.name,
                                new SubInput("Số ngọc", ANY));
                    } else {
                        Service.gI().sendThongBao(player, "Người chơi không tồn tại hoặc đang offline");
                    }
                }
                case FIND_PLAYER_GIFT_RUBY -> {
                    Player target = player.menuPlayer;
                    if (target != null) {
                        Player first = player.id < target.id ? player : target;
                        Player second = first == player ? target : player;
                        synchronized (first) {
                        synchronized (second) {
                        if (!AdminPlayerActions.isOnline(player) || !AdminPlayerActions.isOnline(target)) {
                            Service.gI().sendThongBao(player,"Người chơi đã offline hoặc phiên không hợp lệ."); return;
                        }
                        if (AdminPlayerActions.hasPending(player) || AdminPlayerActions.hasPending(target)) {
                            Service.gI().sendThongBao(player,"Người chơi có giao dịch đang chờ đối soát."); return;
                        }
                        try {
                            int soGem = Integer.parseInt(text[0]);
                            if (soGem <= 0) {
                                Service.gI().sendThongBao(player, "Số ngọc xanh không hợp lệ");
                                return;
                            }

                            int phi = (int) (soGem * 0.1); // Tính phí 10%
                            int tongGem = soGem + phi;     // Tổng cần trừ

                            if (player.inventory.gem < tongGem) {
                                Service.gI().sendThongBao(player, "Bạn cần " + tongGem + " ngọc xanh để tặng (bao gồm phí 10%)");
                                return;
                            }

                            Item item718 = InventoryService.gI().findItemBag(player, 718);
                            if (item718 == null || item718.quantity < 1) {
                                Service.gI().sendThongBao(player, "Bạn cần 1 vé để tặng ngọc xanh");
                                return;
                            }

                            player.inventory.gem -= tongGem;
                            InventoryService.gI().subQuantityItemsBag(player, item718, 1);
                            InventoryService.gI().sendItemBags(player);

                            int gemNhan = soGem;
                            int gemThucNhan = (int) (gemNhan * 0.9);
                            target.inventory.gem += gemThucNhan;

                            Service.gI().sendMoney(player);
                            Service.gI().sendMoney(target);

                            Service.gI().sendThongBao(player, "Bạn đã tặng " + gemThucNhan + " ngọc xanh cho " + target.name + " (đã trừ phí " + phi + ")");
                            Service.gI().sendThongBao(target, player.name + " vừa tặng bạn " + gemThucNhan + " ngọc xanh");
                        } catch (Exception e) {
                            Service.gI().sendThongBao(player, "Lỗi định dạng số lượng ngọc xanh");
                        }
                        }
                        }
                    } else {
                        Service.gI().sendThongBao(player, "Người chơi không hợp lệ hoặc đã offline");
                    }
                }
                case CHANGE_NAME -> {
                    Player target = (Player) PLAYER_ID_OBJECT.remove(player.id);
                    AdminPlayerActions.gI().rename(player, target, text[0], false);
                }
                case CHANGE_NAME_BY_ITEM -> AdminPlayerActions.gI().rename(player, player, text[0], true);
                case CHOOSE_LEVEL_BDKB -> {
                    int level = Integer.parseInt(text[0]);
                    if (level >= 1 && level <= 110) {
                        Npc npc = NpcManager.getByIdAndMap(ConstNpc.QUY_LAO_KAME, player.zone.map.mapId);
                        if (npc != null) {
                            npc.createOtherMenu(player, ConstNpc.MENU_ACCEPT_GO_TO_BDKB,
                                    "Con có chắc muốn đến\nhang kho báu cấp độ " + level + " ?",
                                    new String[]{"Đồng ý", "Từ chối"}, level);
                        }
                    } else {
                        Service.gI().sendThongBao(player, "Không thể thực hiện");
                    }
                }
                case CHOOSE_LEVEL_KGHD -> {
                    int level = Integer.parseInt(text[0]);
                    if (level >= 1 && level <= 110) {
                        Npc npc = NpcManager.getByIdAndMap(ConstNpc.MR_POPO, player.zone.map.mapId);
                        if (npc != null) {
                            npc.createOtherMenu(player, 2,
                                    "Cậu có chắc muốn đến\nDestron Gas cấp độ " + level + " ?",
                                    new String[]{"Đồng ý", "Từ chối"}, level);
                        }
                    }
                }
                case CHOOSE_LEVEL_CDRD -> {
                    int level = Integer.parseInt(text[0]);
                    if (level >= 1 && level <= 110) {
                        Npc npc = NpcManager.getByIdAndMap(ConstNpc.THAN_VU_TRU, player.zone.map.mapId);
                        if (npc != null) {
                            npc.createOtherMenu(player, 3,
                                    "Con có chắc muốn đến\ncon đường rắn độc cấp độ " + level + " ?",
                                    new String[]{"Đồng ý", "Từ chối"}, level);
                        }
                    }
                }
                case MBV -> {
                    int mbv = Integer.parseInt(text[0]);
                    int nmbv = Integer.parseInt(text[1]);
                    int rembv = Integer.parseInt(text[2]);
                    if ((mbv + "").length() != 6 || (nmbv + "").length() != 6 || (rembv + "").length() != 6) {
                        Service.gI().sendThongBao(player, "Trêu bố mày à?");
                    } else if (player.mbv == 0) {
                        Service.gI().sendThongBao(player, "Bạn chưa cài mã bảo vệ!");
                    } else if (player.mbv != mbv) {
                        Service.gI().sendThongBao(player, "Mã bảo vệ không đúng");
                    } else if (nmbv != rembv) {
                        Service.gI().sendThongBao(player, "Mã bảo vệ không trùng khớp");
                    } else {
                        player.mbv = nmbv;
                        Service.gI().sendThongBao(player, "Đổi mã bảo vệ thành công!");
                    }
                }
                case BANSLL -> {
                    int sltv;
                    try {
                        sltv = Integer.parseInt(text[0].trim());
                    } catch (NumberFormatException e) {
                        Service.gI().sendThongBao(player, "Số lượng thỏi vàng không hợp lệ");
                        return;
                    }
                    if (sltv <= 0) {
                        Service.gI().sendThongBao(player, "Số lượng thỏi vàng phải lớn hơn 0");
                        return;
                    }
                    synchronized (player) {
                        if (nro.models.services.RubyGiftService.hasPending(player)) return;
                        long cost = (long) sltv * Inventory.GOLD_BAR_SELL_PRICE;
                        Item thoiVang = InventoryService.gI().findItemBag(player, 457);
                        if (thoiVang == null) {
                            Service.gI().sendThongBao(player, "Bạn không có thỏi vàng để bán");
                        } else if (thoiVang.quantity < sltv) {
                            Service.gI().sendThongBao(player, "Bạn chỉ có " + thoiVang.quantity + " Thỏi vàng");
                        } else if (cost > Inventory.LIMIT_GOLD - player.inventory.gold) {
                            long slban = Math.max(0, (Inventory.LIMIT_GOLD - player.inventory.gold)
                                    / Inventory.GOLD_BAR_SELL_PRICE);
                            Service.gI().sendThongBao(player, slban == 0
                                    ? "Vàng sau khi bán vượt quá giới hạn"
                                    : "Bạn chỉ có thể bán tối đa " + slban + " Thỏi vàng");
                        } else {
                            InventoryService.gI().subQuantityItemsBag(player, thoiVang, sltv);
                            player.inventory.gold += cost;
                            InventoryService.gI().sendItemBags(player);
                            Service.gI().sendMoney(player);
                            Service.gI().sendThongBao(player, "Đã bán " + sltv + " Thỏi vàng thu được " + Util.numberToMoney(cost) + " vàng");
                        }
                    }
                }
                case TANG_NGOC_HONG -> {
                    nro.models.services.RubyGiftService.gI().cancel(player);
                    if (text.length != 2) return;
                    try {
                        long amount = Long.parseLong(text[1].trim());
                        if (amount < 2 || amount > Integer.MAX_VALUE) {
                            Service.gI().sendThongBao(player, "Số hồng ngọc phải từ 2 đến " + Integer.MAX_VALUE);
                            return;
                        }
                        Player recipient = Client.gI().getPlayer(text[0].trim());
                        var quote = nro.models.services.RubyGiftService.gI().prepare(player, recipient, (int) amount);
                        if (!quote.success()) {
                            Service.gI().sendThongBao(player, quote.message());
                            return;
                        }
                        createForm(player, RUBY_GIFT_CONFIRM,
                                "Tặng hồng ngọc cho " + recipient.name + "\nBạn mất: " + quote.amount()
                                + "\nPhí 10%: " + quote.fee() + "\nNgười nhận được: " + quote.net()
                                + "\nTiêu hao 1 vé. Nhập 1 để xác nhận, 0 để hủy.",
                                new SubInput("Xác nhận (1/0)", NUMERIC));
                    } catch (NumberFormatException invalid) {
                        Service.gI().sendThongBao(player, "Số hồng ngọc không hợp lệ");
                    }
                }
                case RUBY_GIFT_CONFIRM -> {
                    player.idMark.setTypeInput(-1);
                    if (text.length != 1 || !"1".equals(text[0].trim())) {
                        nro.models.services.RubyGiftService.gI().cancel(player);
                        Service.gI().sendThongBao(player, "Đã hủy tặng hồng ngọc");
                        return;
                    }
                    var result = nro.models.services.RubyGiftService.gI().confirm(player);
                    Service.gI().sendThongBao(player, result.message());
                }
                case BANGHOI -> {
                    Clan clan = player.clan;
                    if (clan != null) {
                        ClanMember cm = clan.getClanMember((int) player.id);
                        if (clan.isLeader(player)) {
                            if (clan.canUpdateClan(player)) {
                                String tenvt = text[0];
                                if (!Util.haveSpecialCharacter(tenvt) && tenvt.length() > 1 && tenvt.length() < 5) {
                                    clan.name2 = tenvt;
                                    clan.update();
                                    Service.gI().sendThongBao(player, "[" + tenvt + "] OK");
                                } else {
                                    Service.gI().sendThongBaoOK(player, "Chỉ chấp nhận các ký tự a-z, 0-9 và chiều dài từ 2 đến 4 ký tự");
                                }
                            }
                        }
                    }
                }
                case DISSOLUTION_CLAN -> {
                    String xacNhan = text[0];
                    Clan clan;
                    if (xacNhan.equalsIgnoreCase("OK")) {
                        clan = player.clan;
                        if (clan.isLeader(player)) {
                            clan.deleteDB(clan.id);
                            Manager.CLANS.remove(clan);
                            player.clan = null;
                            player.clanMember = null;
                            ClanService.gI().sendMyClan(player);
                            ClanService.gI().sendClanId(player);
                            Service.gI().sendThongBao(player, "Bang hội đã giải tán thành công.");
                        }
                    }
                }
            }
        } catch (IllegalArgumentException | java.io.IOException e) {
            Service.gI().sendThongBao(player, "Dữ liệu nhập không hợp lệ: " + e.getMessage());
        } catch (Exception e) {
            Logger.logException(Input.class, e);
            Service.gI().sendThongBao(player, "Không thể xử lý dữ liệu nhập. Hãy thử lại.");
        }
    }

    private static int botCount(String value) {
        int count=Integer.parseInt(value);
        if(count<=0 || count>1000)throw new IllegalArgumentException("Số bot phải từ 1 đến 1000");
        return count;
    }
    private void launchAdminBots(Player player,int type,BotGiaoDich shop,int count) {
        new Thread(()->{
            if (!AdminCommandService.requireAdmin(player)) return;
            try { NewBot.gI().runBot(type,shop,count); }
            catch(Exception failure){Logger.logException(Input.class,failure);Service.gI().sendThongBao(player,"Không thể tạo bot. Hãy kiểm tra dữ liệu bot.");}
        },"Admin bot launch").start();
    }

    public static boolean isAdminInput(int type) {
        return expectedAdminFields(type) >= 0;
    }
    private static int expectedAdminFields(int type) {
        return switch (type) {
            case GIVE_IT, SEND_ITEM_OP -> 5;
            case GET_IT, BOTITEM -> 4;
            case FIND_PLAYER, CHANGE_NAME, BOTQUAI, BOTBOSS, BOTATTACKPLAYER -> 1;
            default -> -1;
        };
    }
    private void grantForm(Player actor, String[] text, boolean self, boolean currency) {
        int offset = self ? 0 : 1;
        int id = Integer.parseInt(text[offset]);
        int option = Integer.parseInt(text[offset+1]);
        int parameter = Integer.parseInt(text[offset+2]);
        int quantity = Integer.parseInt(text[offset+3]);
        if (quantity <= 0) throw new IllegalArgumentException("Số lượng phải dương");
        var validatedOption = AdminCommandService.validateOption(option, parameter);
        if (!currency || id >= 0) AdminCommandService.validateItem(id, quantity);
        else if (id < -3) throw new IllegalArgumentException("ID tiền không hợp lệ");
        Player recipient = self ? actor : Client.gI().getPlayer(text[0]);
        if (!AdminPlayerActions.isOnline(recipient)) {
            Service.gI().sendThongBao(actor,"Người nhận không online"); return;
        }
        long accepted;
        synchronized (recipient) {
            if (!AdminCommandService.requireAdmin(actor) || !AdminPlayerActions.requireOnline(actor, recipient)) return;
            if (AdminPlayerActions.hasPending(recipient)) {
                Service.gI().sendThongBao(actor,"Người nhận có giao dịch chờ đối soát."); return;
            }
            if (currency && id < 0) accepted = AdminPlayerActions.gI().grantCurrency(actor, recipient, id, quantity);
            else {
                List<Item.ItemOption> options = new java.util.ArrayList<>();
                for (var defaultOption : ItemService.gI().getListOptionItemShop((short)id)) options.add(new Item.ItemOption(defaultOption));
                options.add(validatedOption);
                accepted = AdminCommandService.grantItems(actor, recipient, id, quantity, options);
            }
        }
        String result = "Đã nhận " + accepted + "/" + quantity + " (ID " + id + ")";
        Service.gI().sendThongBao(actor, result + " cho " + recipient.name);
        if (accepted > 0 && recipient != actor) Service.gI().sendThongBao(recipient,result + " từ " + actor.name);
    }

    public void createForm(Player pl, int typeInput, String title, SubInput... subInputs) {
        if (isAdminInput(typeInput) && !AdminCommandService.requireAdmin(pl)) return;
        pl.idMark.setTypeInput(typeInput);
        Message msg = null;
        try {
            msg = new Message(-125);
            msg.writer().writeUTF(title);
            msg.writer().writeByte(subInputs.length);
            for (SubInput si : subInputs) {
                msg.writer().writeUTF(si.name);
                msg.writer().writeByte(si.typeInput);
            }
            pl.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void createForm(ISession session, int typeInput, String title, SubInput... subInputs) {
        if (isAdminInput(typeInput) && (!(session instanceof nro.models.network.MySession s) || !AdminCommandService.requireAdmin(s.player))) return;
        Message msg = null;
        try {
            msg = new Message(-125);
            msg.writer().writeUTF(title);
            msg.writer().writeByte(subInputs.length);
            for (SubInput si : subInputs) {
                msg.writer().writeUTF(si.name);
                msg.writer().writeByte(si.typeInput);
            }
            session.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void createFormChangePassword(Player pl) {
        createForm(pl, CHANGE_PASSWORD, "Đổi mật khẩu", new SubInput("Mật khẩu cũ", PASSWORD),
                new SubInput("Mật khẩu mới", PASSWORD),
                new SubInput("Nhập lại mật khẩu mới", PASSWORD));
    }

    public void createFormGiveItem(Player pl) {
        createForm(pl, GIVE_IT, "Tặng vật phẩm", new SubInput("Tên", ANY), new SubInput("Id Item", ANY), new SubInput("ID OPTION", ANY), new SubInput("PARAM", ANY), new SubInput("Số lượng", ANY));
    }

    public void createFormGetItem(Player pl) {
        createForm(pl, GET_IT, "Get vật phẩm", new SubInput("Id Item", ANY), new SubInput("ID OPTION", ANY), new SubInput("PARAM", ANY), new SubInput("Số lượng", ANY));
    }

    public void createFormGiftCode(Player pl) {
        createForm(pl, GIFT_CODE, "Giftcode", new SubInput("Gift-code", ANY));
    }

    public void createFormMBV(Player pl) {
        createForm(pl, MBV, "Đồ ngu! Đồ ăn hại! Cút mẹ mày đi!", new SubInput("Nhập Mã Bảo Vệ Đã Quên", NUMERIC), new SubInput("Nhập Mã Bảo Vệ Mới", NUMERIC), new SubInput("Nhập Lại Mã Bảo Vệ Mới", NUMERIC));
    }

    public void createFormBangHoi(Player pl) {
        createForm(pl, BANGHOI, "Nhập tên viết tắt bang hội", new SubInput("Tên viết tắt từ 2 đến 4 kí tự", ANY));
    }

    public void createFormFindPlayer(Player pl) {
        createForm(pl, FIND_PLAYER, "Tìm kiếm người chơi", new SubInput("Tên người chơi", ANY));
    }

    public void createFormFindPlayer1(Player pl) {
        Input.gI().createForm(pl, FIND_PLAYER_NAME, "Nhập tên người chơi bạn muốn tặng ngọc", new SubInput("Tên người chơi", ANY));

    }

    public void createFormNapThe(Player pl, byte loaiThe) {
        pl.idMark.setLoaiThe(loaiThe);
        createForm(pl, NAP_THE, "Nạp thẻ", new SubInput("Mã thẻ", ANY), new SubInput("Seri", ANY));
    }

    public void createFormTangRuby(Player pl) {
        nro.models.services.RubyGiftService.gI().cancel(pl);
        createForm(pl, TANG_NGOC_HONG, "Tặng hồng ngọc (phí 10%, trừ trong số tặng)", new SubInput("Tên nhân vật", ANY),
                new SubInput("Tổng hồng ngọc bạn sẽ mất", NUMERIC));
    }

    public void createFormTradeGold(Player pl) {
        createForm(pl, TRADE_GOLD, "Tỉ lệ quy đổi: 10.000 vnđ = 40 Thỏi vàng \n Số dư hiện tại: " + pl.getSession().vnd, new SubInput("Số lượng", NUMERIC));
    }

    public void createFormTradeGem(Player pl) {
        createForm(pl, TRADE_GEM, "Tỉ lệ quy đổi: 10.000 vnđ = 10000 ngọc \n Số dư hiện tại: " + pl.getSession().vnd, new SubInput("Số lượng", NUMERIC));
    }

    public void createFormChangeName(Player pl, Player plChanged) {
        if (!AdminCommandService.requireAdmin(pl) || !AdminPlayerActions.requireOnline(pl, plChanged)) return;
        PLAYER_ID_OBJECT.put(pl.id, plChanged);
        createForm(pl, CHANGE_NAME, "Đổi tên " + plChanged.name, new SubInput("Tên mới", ANY));
    }

    public void createFormChangeNameByItem(Player pl) {
        createForm(pl, CHANGE_NAME_BY_ITEM, "Đổi tên " + pl.name, new SubInput("Tên mới", ANY));
    }

    public void createFormChooseLevelBDKB(Player pl) {
        createForm(pl, CHOOSE_LEVEL_BDKB, "Hãy chọn cấp độ hang kho báu từ 1-110", new SubInput("Cấp độ", NUMERIC));
    }

    public void createFormChooseLevelCDRD(Player pl) {
        createForm(pl, CHOOSE_LEVEL_CDRD, "Hãy chọn cấp độ từ 1-110", new SubInput("Cấp độ", NUMERIC));
    }

    public void createFormChooseLevelKGHD(Player pl) {
        createForm(pl, CHOOSE_LEVEL_KGHD, "Hãy chọn cấp độ từ 1-110", new SubInput("Cấp độ", NUMERIC));
    }

    public void createFormSenditem1(Player pl) {
        createForm(pl, SEND_ITEM_OP, "SEND Vật Phẩm Option",
                new SubInput("Tên người chơi", ANY),
                new SubInput("ID Trang Bị", NUMERIC),
                new SubInput("ID Option", NUMERIC),
                new SubInput("Param", NUMERIC),
                new SubInput("Số lượng", NUMERIC));
    }

    public void createFormBanSLL(Player pl) {
        createForm(pl, BANSLL, "Bạn muốn bán bao nhiêu [Thỏi vàng]?\nMỗi thỏi bán được "
                + Util.numberToMoney(Inventory.GOLD_BAR_SELL_PRICE) + " vàng", new SubInput("Số lượng", NUMERIC));
    }

    public void createFormGiaiTanBangHoi(Player pl) {
        createForm(pl, DISSOLUTION_CLAN, "Nhập OK để xác nhận giải tán bang hội.", new SubInput("", ANY));
    }

    public void createFormConSoMayMan_Gold(Player pl) {
        createForm(pl, CON_SO_MAY_MAN_VANG, "Hãy chọn 1 số từ 0 đến 99 giá 1.000.000 vàng", new SubInput("Số bạn chọn", NUMERIC));
    }

    public void createFormConSoMayMan_Gem(Player pl) {
        createForm(pl, CON_SO_MAY_MAN_NGOC, "Hãy chọn 1 số từ 0 đến 99 giá 5 ngọc", new SubInput("Số bạn chọn", NUMERIC));
    }

    public void createFormTrongDua(Player player) {
        createForm(player, TRONG_DUA, "Trồng Dưa Hấu", new SubInput("Cần có hạt dưa để trồng", NUMERIC));
    }

    public void createFormBotQuai(Player pl) {
        createForm(pl, BOTQUAI, "Buff Bot Quái",
                new SubInput("số lượng bot", NUMERIC));
    }

    public void createFormBotBoss(Player pl) {
        createForm(pl, BOTBOSS, "Buff Bot Boss",
                new SubInput("số lượng bot", NUMERIC));
    }

    public void createFormBotItem(Player pl) {
        createForm(pl, BOTITEM, "Buff Bot Item",
                new SubInput("số lượng bot", NUMERIC),
                new SubInput("id item cần bán", NUMERIC),
                new SubInput("id item trao đổi", NUMERIC),
                new SubInput("số lượng yêu cầu trao đổi", NUMERIC));
    }

    public void createFormBotAttackPlayer(Player pl) {
        createForm(pl, BOTATTACKPLAYER, "Buff Bot Tấn Công Người",
                new SubInput("số lượng bot", NUMERIC));
    }

    public static class SubInput {

        private String name;
        private byte typeInput;

        public SubInput(String name, byte typeInput) {
            this.name = name;
            this.typeInput = typeInput;
        }
    }

}
