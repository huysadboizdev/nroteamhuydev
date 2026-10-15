package nro.models.services;

import nro.models.boss.BossID;
import nro.models.consts.ConstMap;
import nro.models.boss.Boss_mini.AnTrom;
import nro.models.boss.Boss_mini.Odo;
import nro.models.boss.Boss_mini.MatTroi;
import nro.models.boss.Boss_mini.SoiHecQuyn;

public final class BossScreenNoticePolicyTest {

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        int[] twelveHourBosses = {
            BossID.DRABURA,
            BossID.BUI_BUI,
            BossID.YA_CON,
            BossID.MABU_12H,
            BossID.DRABURA_2,
            BossID.BUI_BUI_2,
            BossID.DRABURA_3,
            BossID.GOKU,
            BossID.CADIC
        };
        for (int bossId : twelveHourBosses) {
            check(BossScreenNoticePolicy.hide(bossId, 114, ConstMap.MAP_NORMAL),
                    "12-hour boss is hidden in Oshin map 114: " + bossId);
            check(BossScreenNoticePolicy.hide(bossId, 120, ConstMap.MAP_NORMAL),
                    "12-hour boss is hidden in Oshin map 120: " + bossId);
            check(!BossScreenNoticePolicy.hide(bossId, 5, ConstMap.MAP_NORMAL),
                    "12-hour boss remains visible outside Oshin (map 5): " + bossId);
            check(!BossScreenNoticePolicy.hide(bossId, 1, ConstMap.MAP_NORMAL),
                    "12-hour boss remains visible outside Oshin (map 1): " + bossId);
        }
        check(!BossScreenNoticePolicy.hide(BossID.DRABURA, 113, ConstMap.MAP_NORMAL),
                "12-hour boss remains visible below Oshin range (map 113)");
        check(!BossScreenNoticePolicy.hide(BossID.DRABURA, 121, ConstMap.MAP_NORMAL),
                "12-hour boss remains visible above Oshin range (map 121)");

        check(BossScreenNoticePolicy.hide(BossID.MABU, 127, ConstMap.MAP_MABU_14H),
                "Mabu 14-hour encounter is hidden");
        check(BossScreenNoticePolicy.hide(BossID.SUPERBU, 128, ConstMap.MAP_MABU_14H),
                "Super Bu 14-hour encounter is hidden");
        check(!BossScreenNoticePolicy.hide(BossID.MABU, 127, ConstMap.MAP_NORMAL),
                "Mabu outside 14-hour map remains visible");

        int[] dungeonMapTypes = {
            ConstMap.MAP_DOANH_TRAI,
            ConstMap.MAP_BAN_DO_KHO_BAU,
            ConstMap.MAP_CON_DUONG_RAN_DOC,
            ConstMap.MAP_KHI_GAS_HUY_DIET
        };
        for (int mapType : dungeonMapTypes) {
            check(BossScreenNoticePolicy.hide(12345, 1, mapType),
                    "boss is hidden in dungeon map type: " + mapType);
        }

        int[] specialBosses = {
            BossID.AN_TROM,
            BossID.O_DO,
            BossID.SOI_HEC_QUYN1,
            BossID.MAT_TROI
        };
        for (int bossId : specialBosses) {
            check(BossScreenNoticePolicy.hide(bossId, 1, ConstMap.MAP_NORMAL),
                    "special boss is hidden: " + bossId);
        }
        for (Class<?> bossClass : new Class<?>[]{AnTrom.class, Odo.class, MatTroi.class, SoiHecQuyn.class}) {
            check(BossScreenNoticePolicy.hide(bossClass, -2_000_000, 1, ConstMap.MAP_NORMAL),
                    "dynamic mini boss class is hidden: " + bossClass.getSimpleName());
        }
        check(!BossScreenNoticePolicy.hide(Object.class, -2_000_000, 1, ConstMap.MAP_NORMAL),
                "dynamic ID alone does not hide an unrelated boss");

        check(!BossScreenNoticePolicy.hide(12345, 1, ConstMap.MAP_NORMAL),
                "ordinary boss remains visible");
        check(!BossScreenNoticePolicy.hide(12345, 1, ConstMap.MAP_NORMAL + 100),
                "unknown map type fails open");
        System.out.println("PASS BossScreenNoticePolicy");
    }
}
