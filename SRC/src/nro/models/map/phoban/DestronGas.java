package nro.models.map.phoban;

import nro.models.utils.Functions;
import nro.models.boss.Boss;
import nro.models.boss.khi_gas.DrLychee;
import nro.models.clan.Clan;
import nro.models.map.Zone;
import nro.models.mob.Mob;
import nro.models.player.Player;
import nro.models.services.ItemTimeService;
import nro.models.map.service.MapService;
import nro.models.services.Service;
import nro.models.services.DungeonExitService;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.Util;

import java.util.ArrayList;
import java.util.List;
import nro.models.utils.OwnedWorker;
import nro.models.services.ServerMaintenanceGate;
import nro.models.utils.Logger;
import lombok.Data;
import nro.models.server.Maintenance;
import nro.models.map.service.ItemMapService;
import nro.models.utils.TimeUtil;

@Data
public class DestronGas implements Runnable {
    private final OwnedWorker updateWorker = new OwnedWorker();

    public static final long POWER_CAN_GO_TO_KHI_GAS_HUY_DIET = 2000000000;
    public static final int AVAILABLE = 50;
    public static final int TIME_KHI_GAS_HUY_DIET = 1800000;
    public static final int N_PLAYER_CLAN = 0;

    public int id;
    public byte level;
    public final List<Zone> zones;

    public Clan clan;
    public volatile boolean isOpened;
    public volatile boolean isInitializing;
    public volatile boolean isClosing;
    private boolean achievementRecorded;
    private boolean bossesRetired;
    private long lastTimeOpen;
    private long lastTimeUpdateMessage;
    private boolean kickoutkghd;
    private long timeKickOutKGHD;
    public List<Boss> bosses = new ArrayList<>();
    private boolean callBoss;
    public boolean hatchiyatchDead;

    public DestronGas(int id) {
        this.id = id;
        this.zones = new ArrayList<>();
    }

    @Override
    public void run() {
        long lastErrorLog = 0;
        while (!Thread.currentThread().isInterrupted() && !Maintenance.isRunning && isOpened) {
            long startTime = System.currentTimeMillis();
            try (ServerMaintenanceGate.Lease permit = ServerMaintenanceGate.enter()) {
                if (permit.allowed()) update();
            } catch (Exception e) {
                if (System.currentTimeMillis() - lastErrorLog >= 30000) {
                    lastErrorLog = System.currentTimeMillis();
                    Logger.logException(DestronGas.class, e, "Dungeon tick failed");
                }
            } finally {
                Functions.sleep(Math.max(150 - (System.currentTimeMillis() - startTime), 10));
            }
        }
    }

    public synchronized void update() {
        try (ServerMaintenanceGate.Lease permit = ServerMaintenanceGate.enter()) {
            if (!permit.allowed()) return;
            updateActive();
        }
    }

    private void updateActive() {
        if (isInitializing) return;
        if (isOpened) {
            if (isClosing || Util.canDoWithTime(lastTimeOpen, TIME_KHI_GAS_HUY_DIET) || (kickoutkghd && Util.canDoWithTime(timeKickOutKGHD, 60000))) {
                isClosing = true;
                finish();
                if (!hasPlayersRemaining()) dispose();
                return;
            }

            boolean allCharactersDead = true;
            for (Zone zone : zones) {
                for (Mob mob : zone.mobs) {
                    if (!mob.isDie()) {
                        allCharactersDead = false;
                        break;
                    }
                }
            }

            if (allCharactersDead && !callBoss) {
                try {
                    long bossDamage = (1000 * level);
                    long bossMaxHealth = (15000000 * level);
                    bossDamage = Math.min(bossDamage, 200000000L);
                    bossMaxHealth = Math.min(bossMaxHealth, 2000000000L);
                    bosses.add(new DrLychee(
                            getMapById(148),
                            clan,
                            level,
                            (int) bossDamage,
                            (int) bossMaxHealth
                    ));
                    callBoss = true;
                } catch (Exception exception) {
                }
            }

            if (!kickoutkghd && (hatchiyatchDead || Util.canDoWithTime(lastTimeOpen, TIME_KHI_GAS_HUY_DIET - 60000))) {
                kickoutkghd = true;
                timeKickOutKGHD = System.currentTimeMillis();
                for (Zone zone : zones) {
                    List<Player> players = snapshotPlayers(zone);
                    for (Player pl : players) {
                        Service.gI().sendThongBao(pl, "Nơi này sắp nổ tung mau chạy đi");
                    }

                }
            }
            if (kickoutkghd && Util.canDoWithTime(lastTimeUpdateMessage, 10000)) {
                lastTimeUpdateMessage = System.currentTimeMillis();
                for (Zone zone : zones) {
                    List<Player> players = snapshotPlayers(zone);
                    for (Player pl : players) {
                        Service.gI().sendThongBao(pl, "Về làng Aru sau " + TimeUtil.getTimeLeft(timeKickOutKGHD, 60) + " nữa");
                    }
                }
            }

        }
    }

    public void openKhiGasHuyDiet(Player plOpen, Clan clan, byte level) {
        try {
            this.level = level;
            this.lastTimeOpen = System.currentTimeMillis();
            this.clan = clan;
            this.clan.lastTimeOpenKhiGasHuyDiet = this.lastTimeOpen;
            this.clan.playerOpenKhiGasHuyDiet = plOpen;
            this.clan.KhiGasHuyDiet = this;
            this.callBoss = false;
            this.isClosing = false;
            this.achievementRecorded = false;
            this.bossesRetired = false;
            this.callBoss = false;
            for (Zone zone : zones) if (zone != null) zone.dungeonClosing = false;
            this.isOpened = true;
            this.init();
            this.isInitializing = false;
            sendTextKhiGasHuyDiet();
        } catch (Exception e) {
            clan.lastTimeOpenKhiGasHuyDiet = 0;
            synchronized (this) {
                this.isClosing = true;
                this.isInitializing = false;
                this.dispose();
            }
        }
    }

    public void sendThanhTichKhiGas(Player pl) {
        if (pl == null || pl.clan == null || pl.clan.KhiGasHuyDiet != this) {
            return;
        }
        long timeDoneKhiGas = System.currentTimeMillis() - pl.clan.lastTimeOpenKhiGasHuyDiet;
        int levelDoneKhiGas = pl.clan.KhiGasHuyDiet.level;

        if (levelDoneKhiGas > pl.clan.levelDoneKhiGas) {
            pl.clan.levelDoneKhiGas = levelDoneKhiGas;
            pl.clan.thoiGianHoanThanhKhiGas = timeDoneKhiGas;
        } else if (levelDoneKhiGas == pl.clan.levelDoneKhiGas) {
            if (timeDoneKhiGas < pl.clan.thoiGianHoanThanhKhiGas) {
                pl.clan.thoiGianHoanThanhKhiGas = timeDoneKhiGas;
            }
        }

        if (levelDoneKhiGas >= 70) {
            pl.destronGas70CompletionCount++;
        }

        pl.clan.updatethanhTichKhiGasForLeader();
    }

    private void init() {
        //Hồi sinh quái
        for (Zone zone : this.zones) {
            List<Mob> mobs = zone.mobs;
            for (int i = 0; i < mobs.size(); i++) {
                Mob mob = mobs.get(i);
                if (((i == 0) && zone.map.mapId == 147)
                        || // Quái 76 xuất hiện thứ 1 ở Map 147 (Sa mạc)
                        ((i == 7) && zone.map.mapId == 149)
                        || // Quái 76 xuất hiện thứ 8 ở Map 149 (Thành phố Santa)
                        ((i == 0) && zone.map.mapId == 151)
                        || // Quái 76 xuất hiện thứ 1 ở Map 151 (Hành tinh bóng tối)
                        ((i == 0) && zone.map.mapId == 152)
                        || // Quái 76 xuất hiện thứ 1 ở Map 152 (Vùng đất băng giá)
                        ((i == 33) && zone.map.mapId == 152)) { // Quái 76 xuất hiện thứ 34 ở Map 152 (Vùng đất băng giá)

                    mob.lvMob = 1;
                    mob.point.dame = (int) Math.min((long) level * 31 * 5 * mob.tempId * 10, 2_147_483_647);
                    mob.point.maxHp = (int) Math.min((long) level * 3 * 6700 * mob.tempId * 10, 2_147_483_647);
                    mob.hoiSinh();
                    mob.hoiSinhMobPhoBan();
                } else {
                    mob.lvMob = mob.tempId == 76 ? 1 : 0;
                    mob.point.dame = (int) Math.min((long) level * 31 * 5 * mob.tempId, 2_147_483_647);
                    mob.point.maxHp = (int) Math.min((long) level * 5 * 45 * mob.tempId, 2_147_483_647);
                    mob.hoiSinh();
                    mob.hoiSinhMobPhoBan();
                }
            }
        }
        updateWorker.start("Khí Gas Hủy Diệt: " + this.clan.name, this, () -> !Maintenance.isRunning && isOpened);
    }

    //kết thúc khí gas hủy diệt
    public synchronized void finish() {
        isClosing = true;
        for (Zone zone : zones) if (zone != null) zone.dungeonClosing = true;
        retireBosses();
        boolean recordAchievement = !achievementRecorded;
        for (Zone zone : zones) {
            if (zone == null) continue;
            for (Player pl : snapshotPlayers(zone)) {
                if (pl == null || pl.zone != zone || !pl.isPl()) continue;
                try {
                    if (recordAchievement && pl.clan != null && pl.clan.KhiGasHuyDiet == this) {
                        recordAchievement = false;
                        achievementRecorded = true;
                        sendThanhTichKhiGas(pl);
                    }
                    DungeonExitService.exit(pl, zone, 0, -1, 5);
                } catch (Exception failure) {
                    nro.models.utils.Logger.logException(DestronGas.class, failure);
                }
            }
        }
    }

    private List<Player> snapshotPlayers(Zone zone) {
        synchronized (zone) {
            return new ArrayList<>(zone.getPlayers());
        }
    }

    private boolean hasPlayersRemaining() {
        for (Zone zone : zones) {
            if (zone == null) continue;
            for (Player pl : snapshotPlayers(zone))
                if (pl != null && pl.zone == zone && pl.isPl()) return true;
        }
        return false;
    }

    private void retireBosses() {
        List<Boss> pending = new ArrayList<>();
        pending.addAll(bosses);
        for (Zone zone : zones) {
            if (zone == null) continue;
            for (Player occupant : new ArrayList<>(zone.getBosses()))
                if (occupant instanceof Boss && !pending.contains(occupant)) pending.add((Boss) occupant);
        }
        boolean complete = true;
        for (Boss retiring : pending) {
            if (retiring == null) continue;
            try {
                synchronized (retiring) {
                    retiring.changeStatus(nro.models.consts.BossStatus.REST);
                    ChangeMapService.gI().exitMap(retiring);
                    if (retiring.zone != null) throw new IllegalStateException("Dungeon boss has not left its zone");
                    nro.models.boss.Boss_Manager.GasDestroyManager.gI().removeBoss(retiring);
                }
                bosses.remove(retiring);
            } catch (Exception failure) {
                complete = false;
                if (!bosses.contains(retiring)) bosses.add(retiring);
                nro.models.utils.Logger.logException(DestronGas.class, failure);
            }
        }
        bossesRetired = complete;
    }

    public Zone getMapById(int mapId) {
        for (Zone zone : this.zones) {
            if (zone.map.mapId == mapId) {
                return zone;
            }
        }
        return null;
    }

    private void sendTextKhiGasHuyDiet() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().sendTextKhiGasHuyDiet(pl);
        }
    }

    private void removeTextKhiGasHuyDiet() {
        if (this.clan == null) return;
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().removeTextKhiGasHuyDiet(pl);
        }
    }

    public void dispose() {
        if (hasPlayersRemaining()) { isClosing = true; return; }
        retireBosses();
        if (!bossesRetired) { isClosing = true; return; }
        for (Zone zone : zones) {
            if (zone == null) continue;
            for (int i = zone.items.size() - 1; i >= 0; i--) {
                if (i < zone.items.size()) {
                    ItemMapService.gI().removeItemMap(zone.items.get(i));
                }
            }
        }
        this.removeTextKhiGasHuyDiet();
        this.bosses.clear();
        synchronized (nro.models.services_dungeon.DungeonAllocationLock.LOCK) {
        this.isOpened = false;
        if (this.clan != null && this.clan.KhiGasHuyDiet == this) this.clan.KhiGasHuyDiet = null;
        this.clan = null;
        this.kickoutkghd = false;
        this.hatchiyatchDead = false;
        this.callBoss = false;
        this.isInitializing = false;
        }
    }
}
