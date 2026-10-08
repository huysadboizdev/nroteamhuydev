package nro.models.npc_list;

import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.time.Duration;

public class ToriBot extends Npc {

    private static final LocalDateTime VIP_SEASON_START_DATE = LocalDateTime.of(LocalDate.now().getYear(), Month.JUNE, 5, 0, 0, 0);
    private static final LocalDateTime VIP_SEASON_END_DATE = LocalDateTime.of(LocalDate.now().getYear(), Month.DECEMBER, 5, 23, 59, 59);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public ToriBot(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        LocalDateTime currentDateTime = LocalDateTime.now();
        if (currentDateTime.isAfter(VIP_SEASON_END_DATE)) {
            this.createOtherMenu(player, 0, "Ngươi tìm ta có việc gì?", "Đóng");
        } else {
            int currentPurchases = player.vipPurchaseCount;
            String vipInfoMessage = "Lưu ý: nâng cấp VIP chỉ được tối đa 4 lần mỗi mùa (Bạn đã mua: " + currentPurchases + "/4 lần)";
            String startDateStr = VIP_SEASON_START_DATE.format(DATE_TIME_FORMATTER);
            String endDateStr = VIP_SEASON_END_DATE.format(DATE_TIME_FORMATTER);
            Duration remainingDuration = Duration.between(currentDateTime, VIP_SEASON_END_DATE);
            long days = remainingDuration.toDays();
            long hours = remainingDuration.toHours() % 24;
            long minutes = remainingDuration.toMinutes() % 60;
            long seconds = remainingDuration.getSeconds() % 60;

            String remainingTimeString;
            if (remainingDuration.isNegative() || remainingDuration.isZero()) {
                remainingTimeString = "Mùa VIP sắp kết thúc!";
            } else if (days == 0 && hours == 0 && minutes == 0) {
                remainingTimeString = "Còn " + seconds + " giây!";
            } else if (days == 0 && hours == 0) {
                remainingTimeString = "Còn " + minutes + " phút " + seconds + " giây!";
            } else if (days == 0) {
                remainingTimeString = "Còn " + hours + " giờ " + minutes + " phút " + seconds + " giây!";
            } else {
                remainingTimeString = "Còn " + days + " ngày " + hours + " giờ " + minutes + " phút " + seconds + " giây!";
            }

            this.createOtherMenu(player, 0,
                    "Trong thời gian mùa VIP diễn ra\n(Từ " + startDateStr + " đến hết " + endDateStr + ")\n"
                    + "Thời gian còn lại: " + remainingTimeString + "\n"
                    + // Thêm dòng thời gian còn lại
                    "Tạo nhân vật mới sẽ được X2 Kinh nghiệm toàn mùa\n Nếu nâng cấp VIP sẽ được nhận\n nhiều ưu đãi hơn nữa.\n" + vipInfoMessage,
                    "Vip 1", "Vip 2", "Vip 3", "Vip 4");
        }
    }

    @Override
    public void confirmMenu(Player pl, int select) {
        if (!canOpenNpc(pl)) {
            return;
        }
        LocalDateTime currentDateTime = LocalDateTime.now();
        if (currentDateTime.isAfter(VIP_SEASON_END_DATE)) {
            Service.gI().sendThongBao(pl, "Mùa VIP đã kết thúc. Bạn không thể mua VIP vào lúc này.");
           // openBaseMenu(pl);
            return;
        }
        switch (pl.idMark.getIndexMenu()) {
            case 0 -> {
                switch (select) {
                    case 0 ->
                        createOtherMenu(pl, 1, nro.models.services.VipRewardCatalog.description(1),
                                "50.000\nđiểm mùa [" + pl.getSession().vnd + "]",
                                "Đóng");
                    case 1 ->
                        createOtherMenu(pl, 2, nro.models.services.VipRewardCatalog.description(2),
                                "100.000\nđiềm mùa [" + pl.getSession().vnd + "]",
                                "Đóng");
                    case 2 ->
                        createOtherMenu(pl, 3, nro.models.services.VipRewardCatalog.description(3),
                                "150.000\nđiềm mùa [" + pl.getSession().vnd + "]",
                                "Đóng");
                    case 3 ->
                        createOtherMenu(pl, 4, nro.models.services.VipRewardCatalog.description(4),
                                "200.000\nđiềm mùa [" + pl.getSession().vnd + "]",
                                "Đóng");
                }
            }

            case 1 ->
                { if (select == 0) BuyVip(pl, 1, 50000); }
            case 2 ->
                { if (select == 0) BuyVip(pl, 2, 100000); }
            case 3 ->
                { if (select == 0) BuyVip(pl, 3, 150000); }
            case 4 ->
                { if (select == 0) BuyVip(pl, 4, 200000); }
        }
    }

    private void BuyVip(Player pl, int vipLevel, int cost) {
        synchronized(pl) {
            if(nro.models.database.PlayerDAO.resumeWalletPurchase(pl))return;
            LocalDateTime currentDateTime = LocalDateTime.now();
            if (currentDateTime.isAfter(VIP_SEASON_END_DATE)) {
                Service.gI().sendThongBao(pl, "Mùa VIP đã kết thúc. Bạn không thể mua VIP vào lúc này.");
                openBaseMenu(pl); // Mở lại menu mặc định
                return;
            }

            if (pl.vipPurchaseCount >= 4) {
                Service.gI().sendThongBao(pl, "Bạn đã mua tối đa 4 lượt VIP mùa này rồi!");
                return;
            }
            if (pl.vip >= vipLevel) {
                Service.gI().sendThongBao(pl, "Bạn đã có VIP cấp " + pl.vip + ". Bạn vẫn có thể mua gói này để nhận lại phần thưởng.");
            }

            if (pl.getSession().vnd < cost) {
                Service.gI().sendThongBao(pl, "Không đủ tiền (" + (cost / 1000) + "k VND) để mua VIP!");
                return;
            }

            var rewards=nro.models.services.VipRewardCatalog.prepare(vipLevel);
            if(!rewards.fits(pl)) {
                Service.gI().sendThongBao(pl,"Hành trang không đủ chỗ nhận toàn bộ quà VIP hoặc số hồng ngọc vượt giới hạn. Chưa trừ tiền.");
                return;
            }
            var purchaseResult=nro.models.database.PlayerDAO.purchase(pl,"vip_"+vipLevel,cost,()-> {
                rewards.grant(pl);
                InventoryService.gI().sendItemBags(pl);
                Service.gI().sendMoney(pl);
                Service.gI().sendThongBao(pl,"Mua VIP "+vipLevel+" thành công! Đã nhận đủ quà, lượt mua "+pl.vipPurchaseCount+"/4."
                    +(rewards.storedInChest()?" Hành trang đầy; nhận quà vật phẩm tại Thượng Đế → Quay số → Rương phụ.":""));
                openBaseMenu(pl);
            });
            nro.models.database.PlayerDAO.notifyWalletPurchaseResult(pl,purchaseResult);
        }
    }

}
