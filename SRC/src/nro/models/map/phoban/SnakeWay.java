package nro.models.map.phoban;

import nro.models.utils.Functions;
import nro.models.boss_con_duong_ran_doc.SAIBAMEN;
import nro.models.boss_con_duong_ran_doc.NADIC;
import nro.models.boss_con_duong_ran_doc.CADICH;
import nro.models.boss.Boss;
import nro.models.consts.BossStatus;
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
import java.util.concurrent.Executors;
import lombok.Data;
import nro.models.server.Maintenance;
import nro.models.map.service.ItemMapService;
import nro.models.utils.TimeUtil;

@Data
public class SnakeWay implements Runnable {

    public static final long POWER_CAN_GO_TO_CDRD = 2000000000;
    public static final int AVAILABLE = 50;
    public static final int TIME_CON_DUONG_RAN_DOC = 1800000;

    public int id;
    public byte level;
    public final List<Zone> zones;

    public Clan clan;
    public volatile boolean isOpened;
    public volatile boolean isClosing;
    private boolean achievementRecorded;
    private boolean bossesRetired;
    private long lastTimeOpen;
    private long lastTimeUpdateMessage;
    private boolean kickoutcdrd;
    private long timeKickOutCDRD;
    public List<Boss> bosses = new ArrayList<>();
    public boolean endCDRD;
    public boolean allMobsDead;

    public void addZone(Zone zone) {
        this.zones.add(zone);
    }

    public SnakeWay(int id) {
        this.id = id;
        this.zones = new ArrayList<>();
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning && isOpened) {
            try {
                long startTime = System.currentTimeMillis();
                update();
                Functions.sleep(Math.max(150 - (System.currentTimeMillis() - startTime), 10));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void update() {
        if (isOpened) {
            if (isClosing || Util.canDoWithTime(lastTimeOpen, TIME_CON_DUONG_RAN_DOC) || (kickoutcdrd && Util.canDoWithTime(timeKickOutCDRD, 60000))) {
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
            if (allCharactersDead) {
                allMobsDead = true;
            }

            if (!kickoutcdrd && (endCDRD || Util.canDoWithTime(lastTimeOpen, TIME_CON_DUONG_RAN_DOC - 60000))) {
                kickoutcdrd = true;
                timeKickOutCDRD = System.currentTimeMillis();
            }
            if (kickoutcdrd && Util.canDoWithTime(lastTimeUpdateMessage, 10000)) {
                lastTimeUpdateMessage = System.currentTimeMillis();
                for (Zone zone : zones) {
                    List<Player> players = snapshotPlayers(zone);
                    for (Player pl : players) {
                        Service.gI().sendThongBao(pl, "Trận chiến với người Xayda sẽ kết thúc sau " + TimeUtil.getTimeLeft(timeKickOutCDRD, 60) + " nữa");
                    }

                }
            }

        }
    }

    public void openConDuongRanDoc(Player plOpen, Clan clan, byte level) {
        try {
            this.level = level;
            this.lastTimeOpen = System.currentTimeMillis();
            this.clan = clan;
            this.clan.lastTimeOpenConDuongRanDoc = this.lastTimeOpen;
            this.clan.playerOpenConDuongRanDoc = plOpen;
            this.clan.ConDuongRanDoc = this;
            this.isClosing = false;
            this.achievementRecorded = false;
            this.bossesRetired = false;
            for (Zone zone : zones) if (zone != null) zone.dungeonClosing = false;
            this.isOpened = true;
            this.init();
            sendTextConDuongRanDoc();
        } catch (Exception e) {
            plOpen.clan.lastTimeOpenConDuongRanDoc = 0;
            this.dispose();
        }
    }

    public void sendThanhTichCDRD(Player pl) {
        if (pl == null || pl.clan == null || pl.clan.ConDuongRanDoc != this) {
            return;
        }
        long timeDoneCDRD = System.currentTimeMillis() - pl.clan.lastTimeOpenConDuongRanDoc;
        int levelDoneCDRD = pl.clan.ConDuongRanDoc.level;

        if (levelDoneCDRD > pl.clan.levelDoneCDRD) {
            pl.clan.levelDoneCDRD = levelDoneCDRD;
            pl.clan.thoiGianHoanThanhCDRD = timeDoneCDRD;
        } else if (levelDoneCDRD == pl.clan.levelDoneCDRD) {
            if (timeDoneCDRD < pl.clan.thoiGianHoanThanhCDRD) {
                pl.clan.thoiGianHoanThanhCDRD = timeDoneCDRD;
            }
        }

        pl.clan.updatethanhTichCDRDForLeader();
    }

    private void init() {
        //Hồi sinh quái
        for (Zone zone : this.zones) {
            List<Mob> mobs = zone.mobs;
            for (int i = 0; i < mobs.size(); i++) {
                Mob mob = mobs.get(i);
                if (i == 5) {
                    mob.lvMob = 1;
                    mob.point.dame = (int) level * 100 * mob.tempId * 12;
                    mob.point.maxHp = (int) level * 1000 * mob.tempId * 12;
                    mob.hoiSinh();
                    mob.hoiSinhMobPhoBan();
                } else {
                    mob.lvMob = 0;
                    mob.point.dame = (int) level * 10 * mob.tempId;
                    mob.point.maxHp = (int) level * 100 * mob.tempId;
                    mob.hoiSinh();
                    mob.hoiSinhMobPhoBan();
                }
            }

            if (zone.map.mapId == 144) {
                try {
                    long bossDamage = (200000 * level);
                    long bossMaxHealth = (2000000 * level);
                    for (int i = 6; i > 0; i--) {
                        bossDamage = Math.min(bossDamage, 200000000L);
                        bossMaxHealth = Math.min(bossMaxHealth, 2000000000L);
                        bosses.add(new SAIBAMEN(
                                zone,
                                clan,
                                i,
                                (int) bossDamage,
                                (int) bossMaxHealth
                        ));
                    }
                    bossDamage = Math.min(bossDamage * 5, 200000000L);
                    bossMaxHealth = Math.min(bossMaxHealth * 5, 2000000000L);
                    bosses.add(new NADIC(
                            zone,
                            clan,
                            (int) bossDamage,
                            (int) bossMaxHealth
                    ));
                    bossDamage = Math.min(bossDamage * 10, 200000000L);
                    bossMaxHealth = Math.min(bossMaxHealth * 10, 2000000000L);
                    bosses.add(new CADICH(
                            zone,
                            clan,
                            (int) bossDamage,
                            (int) bossMaxHealth
                    ));
                } catch (Exception exception) {
                }
            }
        }
        Executors.newSingleThreadExecutor().submit(this, "Con Đường Rắn Độc: " + this.clan.name);
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
                    if (recordAchievement && pl.clan != null && pl.clan.ConDuongRanDoc == this) {
                        recordAchievement = false;
                        achievementRecorded = true;
                        sendThanhTichCDRD(pl);
                    }
                    DungeonExitService.exit(pl, zone, 5, 1038, 5);
                } catch (Exception failure) {
                    nro.models.utils.Logger.logException(SnakeWay.class, failure);
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
                    nro.models.boss.Boss_Manager.SnakeWayManager.gI().removeBoss(retiring);
                }
                bosses.remove(retiring);
            } catch (Exception failure) {
                complete = false;
                if (!bosses.contains(retiring)) bosses.add(retiring);
                nro.models.utils.Logger.logException(SnakeWay.class, failure);
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

    private void sendTextConDuongRanDoc() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().sendTextConDuongRanDoc(pl);
        }
    }

    private void removeTextConDuongRanDoc() {
        if (this.clan == null) return;
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().removeTextConDuongRanDoc(pl);
        }
    }

    public long getNumBossAlive() {
        return bosses.stream().filter(boss -> boss.bossStatus != BossStatus.REST).count();
    }

    public void dispose() {
        if (hasPlayersRemaining()) { isClosing = true; return; }
        retireBosses();
        if (!bossesRetired) { isClosing = true; return; }
        // remove bosses
        for (Zone zone : zones) {
            if (zone == null) continue;
            for (int i = zone.items.size() - 1; i >= 0; i--) {
                if (i < zone.items.size()) {
                    ItemMapService.gI().removeItemMap(zone.items.get(i));
                }
            }
        }
        this.removeTextConDuongRanDoc();
        this.bosses.clear();
        this.allMobsDead = false;
        this.endCDRD = false;
        this.isOpened = false;
        if (this.clan != null && this.clan.ConDuongRanDoc == this) this.clan.ConDuongRanDoc = null;
        this.clan = null;
        this.kickoutcdrd = false;
    }
}
