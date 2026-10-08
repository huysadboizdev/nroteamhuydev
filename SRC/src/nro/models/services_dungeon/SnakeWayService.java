package nro.models.services_dungeon;

import nro.models.map.phoban.SnakeWay;
import nro.models.clan.Clan;
import nro.models.clan.ClanMember;
import java.util.ArrayList;
import java.util.List;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.TimeUtil;
import nro.models.utils.Util;

/**
*
* @author By Mr Blue
*
*/

public class SnakeWayService {

    private static SnakeWayService instance;

    public static SnakeWayService gI() {
        if (instance == null) {
            instance = new SnakeWayService();
        }
        return instance;
    }

    public List<SnakeWay> conDuongRanDocs;

    private SnakeWayService() {
        this.conDuongRanDocs = new ArrayList<>();
        for (int i = 0; i < SnakeWay.AVAILABLE; i++) {
            this.conDuongRanDocs.add(new SnakeWay(i));
        }
    }

    public void addMapConDuongRanDoc(int id, Zone zone) {
        this.conDuongRanDocs.get(id).getZones().add(zone);
    }

    public void openConDuongRanDoc(Player player, byte level) {
        SnakeWay reserved = null;
        boolean mayJoin = false;
        Clan clan = player.clan;
        synchronized (DungeonAllocationLock.LOCK) {
            if (player.clan != clan) return;
            if (player.clan == clan && clan != null && clan.ConDuongRanDoc != null && clan.ConDuongRanDoc.isClosing) return;
            if (player.clan == clan && clan != null && clan.ConDuongRanDoc != null && clan.ConDuongRanDoc.isInitializing) return;

            if (clan != null) {
                ClanMember cm = clan.getClanMember((int) player.id);
                if (cm != null) {
                    mayJoin = true;
                    if (cm.getNumDateFromJoinTimeToToday() < 2) {
                        return;
                    }
                    boolean currentAttempt = player.joinCDRD && clan.ConDuongRanDoc != null
                            && player.lastTimeJoinCDRD == clan.lastTimeOpenConDuongRanDoc;
                    if (!currentAttempt && !Util.isTimeDifferenceGreaterThanNDays(player.lastTimeJoinCDRD, 7)) {
                        Service.gI().sendThongBao(player, "Vui lòng đợi " + TimeUtil.getDateLeft(player.lastTimeJoinCDRD, 7 * 24 * 60 * 60) + " nữa");
                        return;
                    } else if (!currentAttempt) {
                        player.talkToThuongDe = false;
                        player.talkToThanMeo = false;
                    }
                    if (clan.ConDuongRanDoc == null) {
                        if (level >= 1 && level <= 110) {
                            SnakeWay conDuongRanDoc = null;
                            for (SnakeWay cdrd : this.conDuongRanDocs) {
                                if (!cdrd.isOpened && !cdrd.isInitializing && cdrd.clan == null) {
                                    conDuongRanDoc = cdrd;
                                    break;
                                }
                            }
                            if (conDuongRanDoc != null) {
                                conDuongRanDoc.isInitializing = true;
                                conDuongRanDoc.clan = clan;
                                clan.ConDuongRanDoc = conDuongRanDoc;
                                reserved = conDuongRanDoc;
                            } else {
                                Service.gI().sendThongBao(player, "Con đường rắn độc đã đầy, hãy quay lại sau 30 phút");
                                return;
                            }
                        } else {
                            Service.gI().sendThongBao(player, "Không thể thực hiện");
                            return;
                        }
                    }
                }
            }
        }
        if (!mayJoin) return;
        if (reserved != null) reserved.openConDuongRanDoc(player, clan, level);
        if (player.clan == clan && clan != null && clan.ConDuongRanDoc != null
        && !clan.ConDuongRanDoc.isInitializing && !clan.ConDuongRanDoc.isClosing) {
            player.joinCDRD = true;
            player.lastTimeJoinCDRD = clan.lastTimeOpenConDuongRanDoc;
            ChangeMapService.gI().changeMap(player, 143, -1, 1055 + (Util.nextInt(-10, 10)), 0);
        }
    }
}
