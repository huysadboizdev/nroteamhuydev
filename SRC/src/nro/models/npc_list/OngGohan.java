package nro.models.npc_list;

import nro.models.consts.ConstMap;
import nro.models.consts.ConstNpc;
import nro.models.database.PlayerDAO;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.PetService;
import nro.models.services.Service;
import nro.models.services.TaskService;
import nro.models.services_func.Input;

public class OngGohan extends Npc {
    public OngGohan(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    protected String getHomeGreeting() {
        return "Con cố gắng theo Quy Lão Kame học thành tài, đừng lo lắng cho ta.";
    }

    private boolean isHomeNpc() {
        return (mapId == ConstMap.NHA_GOHAN && tempId == ConstNpc.ONG_GOHAN)
                || (mapId == ConstMap.NHA_MOORI && tempId == ConstNpc.ONG_MOORI)
                || (mapId == ConstMap.NHA_BROLY && tempId == ConstNpc.ONG_PARAGUS);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (player == null || player.zone == null || !isHomeNpc() || !canOpenNpc(player)) {
            return;
        }
        if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
            createOtherMenu(player, ConstNpc.HOME_SUPPORT_MENU,
                    getHomeGreeting() + "\nKhi có dưới 10 triệu ngọc xanh, con được nhận thêm 10 triệu."
                            + "\nChỉ nhận đệ tử thường khi chưa có đệ tử."
                            + "\nHoàn thành nhiệm vụ Mộc Nhân được mở thành viên miễn phí.",
                    "Nhận 10 triệu\nngọc xanh", "Nhận đệ tử\nthường", "Nhập giftcode", "Mở thành viên\nfree", "Đóng");
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (player == null || player.zone == null || !isHomeNpc() || !canOpenNpc(player)) {
            return;
        }
        synchronized (player) {
            if (player.idMark.getIndexMenu() != ConstNpc.HOME_SUPPORT_MENU) {
                return;
            }
            switch (select) {
                case 0 -> {
                    if (player.inventory.gem < 0) {
                        Service.gI().sendThongBao(player, "Số dư ngọc xanh không hợp lệ.");
                        return;
                    }
                    if (player.inventory.gem >= 10_000_000) {
                        Service.gI().sendThongBao(player,
                                "Bạn chỉ được nhận thêm khi có dưới 10 triệu ngọc xanh.");
                        return;
                    }
                    player.inventory.gem += 10_000_000;
                    PlayerDAO.updatePlayer(player);
                    Service.gI().sendMoney(player);
                    Service.gI().sendThongBao(player, "Bạn nhận được 10 triệu ngọc xanh.");
                }
                case 1 -> {
                    if (player.pet != null) {
                        Service.gI().sendThongBao(player, "Bạn đã có đệ tử rồi.");
                        return;
                    }
                    PetService.gI().createNormalPet(player);
                    PlayerDAO.updatePlayer(player);
                    Service.gI().sendThongBao(player, "Bạn nhận được một đệ tử thường.");
                }
                case 2 -> Input.gI().createFormGiftCode(player);
                case 3 -> Service.gI().sendThongBao(player,
                        nro.models.services.FreeMembershipService.gI().open(player).message());
                default -> { }
            }
        }
    }
}
