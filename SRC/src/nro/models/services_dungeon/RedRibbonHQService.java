package nro.models.services_dungeon;

import nro.models.map.phoban.RedRibbonHQ;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.map.service.ChangeMapService;
import java.util.ArrayList;
import java.util.List;
import nro.models.utils.Util;

/**
*
* @author By Mr Blue
*
*/

public class RedRibbonHQService {

    private static RedRibbonHQService instance;

    public static RedRibbonHQService gI() {
        if (RedRibbonHQService.instance == null) {
            RedRibbonHQService.instance = new RedRibbonHQService();
        }
        return RedRibbonHQService.instance;
    }

    public List<RedRibbonHQ> doanhTrais;

    private RedRibbonHQService() {
        this.doanhTrais = new ArrayList<>();
        for (int i = 0; i < RedRibbonHQ.AVAILABLE; i++) {
            this.doanhTrais.add(new RedRibbonHQ(i));
        }
    }

    public void addMapDoanhTrai(int id, Zone zone) {
        this.doanhTrais.get(id).getZones().add(zone);
    }

    public void joinDoanhTrai(Player pl) {
        RedRibbonHQ reserved;
        boolean existing;
        nro.models.clan.Clan owner = pl.clan;
        nro.models.clan.ClanMember member = pl.clanMember;
        synchronized (DungeonAllocationLock.LOCK) {
            if (owner != null && owner.doanhTrai != null && owner.doanhTrai.isClosing) return;
            if (owner != null && owner.doanhTrai != null && owner.doanhTrai.isInitializing) return;
            if (owner == null || member == null || pl.clan != owner) {
                Service.gI().sendThongBao(pl, "Không thể thực hiện");
                return;
            }
            if (owner.haveGoneDoanhTrai && !Util.isAfterMidnight(owner.lastTimeOpenDoanhTrai)) {
                Service.gI().sendThongBao(pl, "Vui lòng chờ đến ngày mai");
                return;
            }
            if (member.getNumDateFromJoinTimeToToday() < 1) {
                return;
            }
            if (owner.doanhTrai != null) {
                reserved = owner.doanhTrai;
                existing = true;
            } else {
                RedRibbonHQ doanhTrai = null;
                for (RedRibbonHQ dt : this.doanhTrais) {
                    if (dt.getClan() == null && !dt.isOpened && !dt.isInitializing) {
                        doanhTrai = dt;
                        break;
                    }
                }
                if (doanhTrai == null) {
                    Service.gI().sendThongBao(pl, "Doanh trại đã đầy, hãy quay lại vào lúc khác!");
                    return;
                }
                doanhTrai.isInitializing = true;
                doanhTrai.setClan(owner);
                owner.doanhTrai = doanhTrai;
                reserved = doanhTrai;
                existing = false;
            }
        }
        if (existing && pl.clan == owner) {
            pl.lastTimeJoinDT = System.currentTimeMillis();
            reserved.updateHPDame();
            ChangeMapService.gI().changeMapInYard(pl, 53, -1, 60);
        } else if (!existing) reserved.openDoanhTrai(pl);
    }
}
