package nro.models.services;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.Boss_mini.AnTrom;
import nro.models.boss.Boss_mini.MatTroi;
import nro.models.boss.Boss_mini.Odo;
import nro.models.boss.Boss_mini.SoiHecQuyn;
import nro.models.consts.BossStatus;
import nro.models.player.NPoint;
import nro.models.server.ServerNotify;
import static nro.models.services.PinkFixture.*;

public final class BossLookupCoverageTest {
    private static void field(Object target, Class<?> type, String name, Object value) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Boss boss(Class<? extends Boss> type, int id, String name) throws Exception {
        Boss boss = blank(type);
        boss.id = id;
        boss.name = name;
        boss.zone = zone();
        boss.zone.map.mapName = "Map";
        boss.nPoint = new NPoint(boss);
        boss.nPoint.hp = boss.nPoint.hpMax = 100;
        boss.currentLevel = 0;
        boss.bossStatus = BossStatus.ACTIVE;
        return boss;
    }

    public static void main(String[] args) throws Exception {
        install();
        ServerNotify notify = blank(ServerNotify.class);
        List<String> messages = new ArrayList<>();
        field(notify, ServerNotify.class, "notifies", messages);
        replace(ServerNotify.class, "i", notify);

        BossNoticeService notices = BossNoticeService.gI();
        Class<? extends Boss>[] types = new Class[]{AnTrom.class, Odo.class, MatTroi.class, SoiHecQuyn.class};
        int[] ids = {BossID.AN_TROM, BossID.O_DO1, BossID.MAT_TROI, BossID.SOI_HEC_QUYN1};
        String[] names = {"Ăn Trộm", "Ở Dơ", "Mặt Trời 12", "Sói hẹc quyn"};
        for (int i = 0; i < types.length; i++) {
            Boss boss = boss(types[i], -2_000_000 - i, names[i]);
            notices.appeared(boss);
            int semanticId = ids[i];
            check(notices.rows(0).stream().anyMatch(row -> row.bossId() == boss.id
                    && row.semanticBossId() == semanticId), "live lookup tracks " + names[i]);
            check(notices.rows(1).stream().anyMatch(row -> row.bossId() == boss.id
                    && row.semanticBossId() == semanticId), "history lookup tracks " + names[i]);
            if (i != 3) {
                check(messages.isEmpty(), "excluded mini boss does not broadcast " + names[i]);
                check(!BossAnnouncementPolicy.equipment(boss), "excluded mini boss does not gain equipment drops");
            }
            notices.defeated(boss, player(i + 1));
            check(notices.rows(1).stream().anyMatch(row -> row.bossId() == boss.id
                    && row.semanticBossId() == semanticId && row.status().equals("DEFEATED")),
                    "history lookup records defeat of " + names[i]);
            if (i != 3) check(messages.isEmpty(), "excluded mini boss defeat does not broadcast " + names[i]);
        }
        System.out.println("PASS boss lookup tracks four mini boss classes without changing excluded announcements");
    }
}
