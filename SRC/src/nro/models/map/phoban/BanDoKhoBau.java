package nro.models.map.phoban;

import nro.models.utils.Functions;
import nro.models.boss.Boss;
import nro.models.boss.ban_do_kho_bau.TrungUyXanhLo;
import nro.models.clan.Clan;
import nro.models.map.TrapMap;
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
public class BanDoKhoBau implements Runnable {
    private final OwnedWorker updateWorker = new OwnedWorker();

    public static final long POWER_CAN_GO_TO_DBKB = 2000000000;
    public static final int AVAILABLE = 50;
    public static final int TIME_BAN_DO_KHO_BAU = 1800000;
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
    private boolean kickoutbdkb;
    private long timeKickOutBDKB;
    private Boss boss;
    private long lastTimeSendNotify;
    private boolean allCharactersDead;

    public void addZone(Zone zone) {
        this.zones.add(zone);
    }

    public BanDoKhoBau(int id) {
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
                    Logger.logException(BanDoKhoBau.class, e, "Dungeon tick failed");
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
            if (isClosing || Util.canDoWithTime(lastTimeOpen, TIME_BAN_DO_KHO_BAU) || (kickoutbdkb && Util.canDoWithTime(timeKickOutBDKB, 60000))) {
                isClosing = true;
                finish();
                if (!hasPlayersRemaining()) dispose();
                return;
            }

            allCharactersDead = true;
            for (Zone zone : zones) {

                if (zone.map.mapId == 135) {
                    for (Player pl : zone.getNotBosses()) {
                        if (pl != null) {
                            TrapMap trap = zone.isInTrap(pl);
                            if (trap != null) {
                                trap.doPlayer(pl);
                            }
                        }
                    }
                }

                for (Mob mob : zone.mobs) {
                    if (!mob.isDie()) {
                        allCharactersDead = false;
                        break;
                    }
                }

                if (allCharactersDead) {
                    for (Player cBoss : zone.getBosses()) {
                        if (!cBoss.isDie()) {
                            allCharactersDead = false;
                            break;
                        }
                    }
                }
            }

            if (!kickoutbdkb && (allCharactersDead || Util.canDoWithTime(lastTimeOpen, TIME_BAN_DO_KHO_BAU - 60000))) {
                kickoutbdkb = true;
                timeKickOutBDKB = System.currentTimeMillis();
            }

            if (kickoutbdkb && Util.canDoWithTime(lastTimeSendNotify, 10000)) {
                for (Zone zone : zones) {
                    List<Player> players = snapshotPlayers(zone);
                    for (Player pl : players) {
                        Service.gI().sendThongBao(pl, "Cái hang này sắp sập rồi, chúng ta phải rời khỏi đây ngay " + TimeUtil.getTimeLeft(timeKickOutBDKB, 60) + " nữa");
                    }
                    lastTimeSendNotify = System.currentTimeMillis();
                }
            }

        }
    }

    public void openBanDoKhoBau(Player plOpen, Clan clan, byte level) {
        openBanDoKhoBau(plOpen, clan, level, () -> true);
    }

    public boolean openBanDoKhoBau(Player plOpen, Clan clan, byte level, java.util.function.BooleanSupplier charge) {
        try {
            this.level = level;
            this.lastTimeOpen = System.currentTimeMillis();
            this.clan = clan;
            this.clan.lastTimeOpenBanDoKhoBau = this.lastTimeOpen;
            this.clan.playerOpenBanDoKhoBau = plOpen;
            this.clan.BanDoKhoBau = this;
            this.kickoutbdkb = false;
            this.isClosing = false;
            this.achievementRecorded = false;
            this.bossesRetired = false;
            for (Zone zone : zones) if (zone != null) zone.dungeonClosing = false;
            this.isOpened = true;
            this.allCharactersDead = false;
            this.init();
            if (!charge.getAsBoolean()) throw new IllegalStateException("Treasure admission payment was not committed");
            this.isInitializing = false;
        } catch (Exception e) {
            e.printStackTrace();
            clan.lastTimeOpenBanDoKhoBau = 0;
            synchronized (this) {
                this.isClosing = true;
                this.isInitializing = false;
                this.dispose();
            }
            return false;
        }
        try {
            if (plOpen.clan == clan) ChangeMapService.gI().goToDBKB(plOpen);
            sendTextBanDoKhoBau();
        } catch (Exception failure) {
            nro.models.utils.Logger.logException(BanDoKhoBau.class, failure);
        }
        return true;
    }

    public void sendThanhTichBanDoKhoBau(Player pl) {
        if (pl == null || pl.clan == null || pl.clan.BanDoKhoBau != this) {
            return;
        }

        long timeDone = System.currentTimeMillis() - pl.clan.timeOpenBanDoKhoBau;
        int levelDone = pl.clan.BanDoKhoBau.level;
        if (levelDone > pl.clan.levelDoneBanDoKhoBau) {
            pl.clan.levelDoneBanDoKhoBau = levelDone;
            pl.clan.thoiGianHoanThanhBDKB = timeDone;
        } else if (levelDone == pl.clan.levelDoneBanDoKhoBau
                && timeDone < pl.clan.thoiGianHoanThanhBDKB) {
            pl.clan.thoiGianHoanThanhBDKB = timeDone;
        }

        pl.clan.updatethanhTichBDKBForLeader();
        pl.clan.updateThongTinLeader(pl.clan.id);
    }

    private void init() {
        //Hồi sinh quái
        for (Zone zone : this.zones) {
            for (TrapMap trap : zone.trapMaps) {
                trap.dame = this.level * 100000;
            }

            if (zone.map.mapId == 135 || zone.map.mapId == 136 || zone.map.mapId == 137) {
                List<Mob> mobs = zone.mobs;
                for (int i = 0; i < mobs.size(); i++) {
                    Mob mob = mobs.get(i);
                    if (((i == 5 || i == 10) && zone.map.mapId == 135) || (i == 5 && zone.map.mapId == 136) || (i == 5 && zone.map.mapId == 137)) {
                        mob.lvMob = 1;
                        mob.point.dame = (int) Math.min((long) level * 600 * mob.tempId * 10, 2_147_483_647);
                        mob.point.maxHp = (int) Math.min((long) level * 469799 * mob.tempId, 2_147_483_647);
                        mob.hoiSinh();
                        mob.hoiSinhMobPhoBan();
                    } else {
                        mob.lvMob = 0;
                        mob.point.dame = (int) Math.min((long) level * 200 * mob.tempId, 2_147_483_647);
                        mob.point.maxHp = (int) Math.min((long) level * 469799 * mob.tempId, 2_147_483_647);
                        mob.hoiSinh();
                        mob.hoiSinhMobPhoBan();
                    }
                }
            } else {
                for (Mob mob : zone.mobs) {
                    mob.point.dame = (int) Math.min((long) level * 31 * 50 * mob.tempId, 2_147_483_647);
                    mob.point.maxHp = (int) Math.min((long) level * 310799 * 50 * mob.tempId, 2_147_483_647);
                    mob.hoiSinh();
                    mob.hoiSinhMobPhoBan();
                }
            }

            if (zone.map.mapId == 137) {
                try {
                    long bossDamage = (200000 * level);
                    long bossMaxHealth = (20_000_000L * level);
                    bossDamage = Math.min(bossDamage, 200000000L);
                    bossMaxHealth = Math.min(bossMaxHealth, 2000000000L);
                    boss = new TrungUyXanhLo(
                            zone,
                            level,
                            (int) bossDamage,
                            (int) bossMaxHealth
                    );
                } catch (Exception exception) {
                }
            }
        }
        updateWorker.start("Bản Đồ Kho Báu: " + this.clan.name, this, () -> !Maintenance.isRunning && isOpened);
    }

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
                    if (recordAchievement && pl.clan != null && pl.clan.BanDoKhoBau == this) {
                        recordAchievement = false;
                        achievementRecorded = true;
                        sendThanhTichBanDoKhoBau(pl);
                    }
                    DungeonExitService.exit(pl, zone, 5, 1038, 5);
                } catch (Exception failure) {
                    nro.models.utils.Logger.logException(BanDoKhoBau.class, failure);
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
        if (boss != null) pending.add(boss);
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
                    nro.models.boss.Boss_Manager.TreasureUnderSeaManager.gI().removeBoss(retiring);
                }
                if (boss == retiring) boss = null;
            } catch (Exception failure) {
                complete = false;
                boss = retiring;
                nro.models.utils.Logger.logException(BanDoKhoBau.class, failure);
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

    private void sendTextBanDoKhoBau() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().sendTextBanDoKhoBau(pl);
        }
    }

    private void removeTextBanDoKhoBau() {
        if (this.clan == null) return;
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().removeTextBanDoKhoBau(pl);
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
        this.removeTextBanDoKhoBau();
        this.allCharactersDead = false;
        this.boss = null;
        synchronized (nro.models.services_dungeon.DungeonAllocationLock.LOCK) {
        this.isOpened = false;
        if (this.clan != null && this.clan.BanDoKhoBau == this) this.clan.BanDoKhoBau = null;
        this.clan = null;
        this.kickoutbdkb = false;
        this.isInitializing = false;
        }
    }
}
