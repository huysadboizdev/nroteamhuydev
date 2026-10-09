package nro.models.boss.Black_Goku;

import nro.models.boss.BossID;
import nro.models.boss.BossesData;

/** Independent future boss using Super Black Goku's combat template. */
public final class Zamasu extends BlackGoku {
    public Zamasu() throws Exception {
        super(BossID.ZAMASU, BossesData.ZAMASU);
    }

    @Override
    protected String getSpawnName() {
        return "Zamasu";
    }
}
