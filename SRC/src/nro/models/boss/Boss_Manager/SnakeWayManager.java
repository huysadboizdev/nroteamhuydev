package nro.models.boss.Boss_Manager;

import nro.models.boss.Boss;
import nro.models.server.Maintenance;
import nro.models.utils.Functions;

public class SnakeWayManager extends BossManager {

    private static SnakeWayManager instance;

    public static SnakeWayManager gI() {
        if (instance == null) {
            instance = new SnakeWayManager();
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
