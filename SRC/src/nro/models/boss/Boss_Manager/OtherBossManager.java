package nro.models.boss.Boss_Manager;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import nro.models.boss.Boss;
import nro.models.server.Maintenance;
import nro.models.utils.Functions;

public class OtherBossManager extends BossManager {

    private static OtherBossManager instance;

    public static OtherBossManager gI() {
        if (instance == null) {
            instance = new OtherBossManager();
        }
        return instance;
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                long st = System.currentTimeMillis();
                updateBosses();
                Functions.sleep(Math.max(150 - (System.currentTimeMillis() - st), 10));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}
