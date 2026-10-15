package nro.models.services;

import java.util.Set;
import nro.models.boss.BossID;
import nro.models.boss.Boss;
import nro.models.boss.Boss_mini.AnTrom;
import nro.models.boss.Boss_mini.Odo;
import nro.models.boss.Boss_mini.MatTroi;
import nro.models.boss.Boss_mini.SoiHecQuyn;
import nro.models.consts.ConstMap;

/** Pure rule for suppressing selected boss notices from the screen panel. */
public final class BossScreenNoticePolicy {

    private static final Set<Integer> TWELVE_HOUR_BOSSES = Set.of(
            BossID.DRABURA,
            BossID.BUI_BUI,
            BossID.YA_CON,
            BossID.MABU_12H,
            BossID.DRABURA_2,
            BossID.BUI_BUI_2,
            BossID.DRABURA_3,
            BossID.GOKU,
            BossID.CADIC);

    private static final Set<Integer> SPECIAL_BOSSES = Set.of(
            BossID.AN_TROM,
            BossID.O_DO,
            BossID.SOI_HEC_QUYN1,
            BossID.MAT_TROI);

    private static final Set<Integer> DUNGEON_MAP_TYPES = Set.of(
            (int) ConstMap.MAP_DOANH_TRAI,
            (int) ConstMap.MAP_BAN_DO_KHO_BAU,
            (int) ConstMap.MAP_CON_DUONG_RAN_DOC,
            (int) ConstMap.MAP_KHI_GAS_HUY_DIET);

    /** Oshin/MaBu 12h maps; mirrors MapService.isMapMaBu (114-120). */
    public static final int OSHIN_MAP_ID_MIN = 114;
    public static final int OSHIN_MAP_ID_MAX = 120;

    private BossScreenNoticePolicy() {
    }

    public static boolean hide(Boss boss) {
        if (boss == null || boss.zone == null || boss.zone.map == null) {
            return false;
        }
        return hide(boss.getClass(), boss.questBossId(), boss.zone.map.mapId, boss.zone.map.type);
    }

    static boolean hide(Class<?> bossClass, int semanticBossId, int mapId, int mapType) {
        return bossClass == AnTrom.class || bossClass == Odo.class
                || bossClass == MatTroi.class || bossClass == SoiHecQuyn.class
                || hide(semanticBossId, mapId, mapType);
    }

    public static boolean hide(int semanticBossId, int mapId, int mapType) {
        boolean mabu14h = (semanticBossId == BossID.MABU || semanticBossId == BossID.SUPERBU)
                && mapType == ConstMap.MAP_MABU_14H;
        return SPECIAL_BOSSES.contains(semanticBossId)
                || (TWELVE_HOUR_BOSSES.contains(semanticBossId) && mapId >= OSHIN_MAP_ID_MIN && mapId <= OSHIN_MAP_ID_MAX)
                || mabu14h
                || DUNGEON_MAP_TYPES.contains(mapType);
    }
}
