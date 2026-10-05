package nro.models.map.phoban;
import nro.models.utils.Functions;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import lombok.Data;
import nro.models.map.Zone;
import nro.models.map.MaBuHold;
import nro.models.player.Player;
import nro.models.services.DungeonExitService;
import nro.models.server.Maintenance;
import nro.models.map.service.MapService;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.TimeUtil;

@Data
public final class MajinBuu14H implements Runnable {

    public static final int AVAILABLE = 7;
    public int id;
    public final List<Zone> zones;

    public MajinBuu14H(int id) {
        this.id = id;
        this.zones = new ArrayList<>();
        this.init();
    }

    public void init() {
        Executors.newSingleThreadExecutor().submit(this, "MajinBuu 14H - Id : " + id);
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                long startTime = System.currentTimeMillis();
                update();
                Functions.sleep(Math.max(150 - (System.currentTimeMillis() - startTime), 10));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void update() {
        if (!TimeUtil.isMabu14HOpen()) {
            finish();
            return;
        }
        for (int j = zones.size() - 1; j >= 0; j--) {
            Zone zone = zones.get(j);
            for (MaBuHold hold : zone.maBuHolds) {
                if (hold.player != null && hold.player.maBuHold == null && hold.player.zone != null) {
                    hold.player = null;
                }
            }
        }
    }

    public MaBuHold getMaBuHold() {
        for (Zone zone : this.zones) {
            if (zone.map.mapId == 128) {
                for (MaBuHold hold : zone.maBuHolds) {
                    if (hold.player == null) {
                        return hold;
                    }
                }
            }
        }
        return null;
    }

    public Zone getMapById(int mapId) {
        for (Zone zone : this.zones) {
            if (zone.map.mapId == mapId) {
                return zone;
            }
        }
        return null;
    }

    private void finish() {
        for (Zone zone : zones) {
            if (zone == null) continue;
            for (Player pl : new ArrayList<>(zone.getPlayers())) {
                if (pl != null && pl.zone == zone && pl.isPl() && !pl.isAdmin())
                    DungeonExitService.exit(pl, zone, pl.gender + 21, 336, 5);
            }
        }
    }

}
