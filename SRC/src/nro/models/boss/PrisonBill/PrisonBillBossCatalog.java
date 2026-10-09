package nro.models.boss.PrisonBill;

import nro.models.boss.*;
import nro.models.consts.AppearType;
import java.util.Arrays;

/** Dedicated combat identities and SQL part triples, separate from NPC/training templates. */
public final class PrisonBillBossCatalog {
    private PrisonBillBossCatalog() {}
    public static final int[] IDS = {BossID.HEART, BossID.KAMIN, BossID.OREN,
            BossID.BEERUS_BATTLE, BossID.WHIS_BATTLE, BossID.CHAMPA_BATTLE, BossID.VADOS_BATTLE};
    private static final String[] NAMES = {"Heart", "Kamin", "Oren", "Beerus", "Whis", "Champa", "Vados"};
    public static boolean contains(int id) { for (int candidate : IDS) if (candidate == id) return true; return false; }
    public static BossData data(int id) {
        int index = -1; for (int i = 0; i < IDS.length; i++) if (IDS[i] == id) index = i;
        if (index < 0) throw new IllegalArgumentException("Unknown Prison/Bill boss " + id);
        int part = 2099 + index * 3;
        BossData base = BossesData.SUPER_BLACK_GOKU;
        BossData result = new BossData(NAMES[index], base.getGender(),
                new short[]{(short)part, (short)(part+1), (short)(part+2), -1, -1, -1},
                base.getDame(), new int[]{id == BossID.KAMIN || id == BossID.OREN ? 1_000_000_000 : 2_000_000_000},
                new int[]{154,155}, Arrays.stream(base.getSkillTemp()).map(int[]::clone).toArray(int[][]::new),
                new String[]{}, new String[]{}, new String[]{}, 300,
                id == BossID.OREN ? AppearType.APPEAR_WITH_ANOTHER : AppearType.DEFAULT_APPEAR);
        if (id == BossID.KAMIN) result.setBossesAppearTogether(new int[]{BossID.OREN});
        return result;
    }
}
