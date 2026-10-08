package nro.models.map.phoban;
import nro.models.utils.Functions;
import nro.models.boss.Boss;
import nro.models.boss.doanh_trai.NinjaAoTim;
import nro.models.boss.doanh_trai.RobotVeSi;
import nro.models.boss.doanh_trai.TrungUyThep;
import nro.models.boss.doanh_trai.TrungUyTrang;
import nro.models.boss.doanh_trai.TrungUyXanhLo;
import nro.models.clan.Clan;
import nro.models.map.Zone;
import nro.models.mob.Mob;
import nro.models.player.Player;
import nro.models.map.service.ChangeMapService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import lombok.Data;
import nro.models.map.ItemMap;
import nro.models.server.Maintenance;
import nro.models.map.service.ItemMapService;
import nro.models.services.ItemTimeService;
import nro.models.map.service.MapService;
import nro.models.services.Service;
import nro.models.services.DungeonExitService;
import nro.models.utils.Util;

@Data
public class RedRibbonHQ implements Runnable {

    //bang hội đủ số người mới đc mở
    public static final int N_PLAYER_CLAN = 5;
    //số người đứng cùng khu
    public static final int N_PLAYER_MAP = 1;
    public static final int AVAILABLE = 50;
    public static final int TIME_DOANH_TRAI = 1800000;
    public static final int TIME_PICK_DOANH_TRAI = 300000;

    public int id;
    public final List<Zone> zones;
    private Clan clan;

    private long lastTimeOpen;
    public volatile boolean isOpened;
    public volatile boolean isInitializing;
    public volatile boolean isClosing;
    private boolean achievementRecorded;
    private boolean bossesRetired;
    public boolean isTimePicking;
    public long lastTimePick;
    public boolean winDT;
    public List<Boss> bosses = new ArrayList<>();

    public RedRibbonHQ(int id) {
        this.id = id;
        this.zones = new ArrayList<>();
    }

    public void addZone(Zone zone) {
        this.zones.add(zone);
    }

    public Zone getMapById(int mapId) {
        for (Zone zone : this.zones) {
            if (zone.map.mapId == mapId) {
                return zone;
            }
        }
        return null;
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

    public void openDoanhTrai(Player player) {
        Clan owner = this.clan != null ? this.clan : player.clan;
        try {
            this.lastTimeOpen = System.currentTimeMillis();
            this.clan = owner;
            owner.doanhTrai = this;
            owner.playerOpenDoanhTrai = player;
            owner.lastTimeOpenDoanhTrai = this.lastTimeOpen;
            owner.haveGoneDoanhTrai = false;
            sendTextDoanhTrai();
            //Khởi tạo quái, boss
            this.isClosing = false;
            this.achievementRecorded = false;
            this.bossesRetired = false;
            for (Zone zone : zones) if (zone != null) zone.dungeonClosing = false;
            this.isOpened = true;
            this.init();
            this.isInitializing = false;
        } catch (Exception e) {
            e.printStackTrace();
            owner.lastTimeOpenDoanhTrai = 0;
            owner.haveGoneDoanhTrai = false;
            synchronized (this) {
                this.isClosing = true;
                this.isInitializing = false;
                this.dispose();
            }
            return;
        }
        if (player.clan != owner) return;
        List<Player> plJoinDT = new ArrayList();
        //Đưa thành viên vào doanh trại
        for (Player pl : snapshotPlayers(player.zone)) {
            if (pl != null && !pl.equals(player) && pl.clan != null
                    && pl.clan.equals(owner) && pl.location.x >= 1285
                    && pl.location.x <= 1645) {
                plJoinDT.add(pl);
            }
        }
        for (Player pl : plJoinDT) {
            if (pl.clanMember.getNumDateFromJoinTimeToToday() < 1 || pl.isDie()) {
                continue;
            }
            pl.lastTimeJoinDT = System.currentTimeMillis();
            ChangeMapService.gI().changeMapInYard(pl, 53, -1, 60);
        }
        player.lastTimeJoinDT = System.currentTimeMillis();
        ChangeMapService.gI().changeMapInYard(player, 53, -1, 60);
    }

    private void init() {
        long totalDamage = 0;
        long totalHp = 0;

        for (Player player : this.clan.membersInGame) {
            if (player != null && player.nPoint != null) {
                totalDamage += player.nPoint.dame;
                totalHp += player.nPoint.hpMax;
            }
        }

        // Hồi sinh quái
        for (Zone zone : this.zones) {
            for (Mob mob : zone.mobs) {
                long mobTempId = mob.tempId;
                mob.point.dame = (int) ((mobTempId != 0) ? Math.min(totalHp / mobTempId, 2_147_483_647L) : 0);
                mob.point.maxHp = (int) ((mobTempId != 0) ? Math.min(totalDamage * mobTempId, 2_147_483_647) : 0);
                mob.lvMob = 0;
                mob.hoiSinh();
                mob.hoiSinhMobPhoBan();
            }

            long dame = totalHp / 20;
            long hp = totalDamage * 50;

            if (zone.map.mapId == 59) {
                try {
                    long bossDamage = Math.min(dame, 200_000_000L);
                    long bossMaxHealth = Math.min(hp, 2_147_483_647L);
                    bosses.add(new TrungUyTrang(
                            zone,
                            (int) bossDamage,
                            (int) bossMaxHealth
                    ));
                } catch (Exception e) {
                }
            }
            if (zone.map.mapId == 62) {
                try {
                    long bossDamage = Math.min((long) (dame * 1.1), 200_000_000L);
                    long bossMaxHealth = Math.min((long) (hp * 1.1), 2_147_483_647);
                    bosses.add(new TrungUyXanhLo(
                            zone,
                            (int) bossDamage,
                            (int) bossMaxHealth
                    ));
                } catch (Exception e) {
                }
            }
            if (zone.map.mapId == 55) {
                try {
                    long bossDamage = Math.min((long) (dame * 1.15), 200_000_000L);
                    long bossMaxHealth = Math.min((long) (hp * 1.15), 2_147_483_647L);
                    bosses.add(new TrungUyThep(
                            zone,
                            (int) bossDamage,
                            (int) bossMaxHealth
                    ));
                } catch (Exception e) {
                }
            }
            if (zone.map.mapId == 54) {
                try {
                    long bossDamage = Math.min((long) (dame * 1.2), 200_000_000L);
                    long bossMaxHealth = Math.min((long) (hp * 1.2), 2_147_483_647);
                    bosses.add(new NinjaAoTim(
                            zone,
                            clan,
                            (int) bossDamage,
                            (int) bossMaxHealth
                    ));
                } catch (Exception e) {
                }
            }

            if (zone.map.mapId == 57) {
                try {
                    long bossDamage = Math.min((long) (dame * 1.3), 200_000_000L);
                    long bossMaxHealth = Math.min((long) (hp * 1.3), 2_147_483_647);
                    for (int i = 0; i < 4; i++) {
                        bosses.add(new RobotVeSi(
                                zone,
                                i,
                                (int) bossDamage,
                                (int) bossMaxHealth
                        ));
                    }
                } catch (Exception e) {
                }
            }
        }
        Executors.newSingleThreadExecutor().submit(this, "Doanh Trại: " + this.clan.name);
    }

    public synchronized void update() {
        if (isInitializing) return;
        if (isOpened) {
            if (isClosing || (!isTimePicking && Util.canDoWithTime(lastTimeOpen, TIME_DOANH_TRAI)) || (isTimePicking && Util.canDoWithTime(lastTimePick, TIME_PICK_DOANH_TRAI))) {
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

                if (allCharactersDead) {
                    for (Player boss : zone.getBosses()) {
                        if (!boss.isDie()) {
                            allCharactersDead = false;
                            break;
                        }
                    }
                }
            }
            if (allCharactersDead && !winDT) {
                winDT = true;
                for (Zone zone : zones) {
                    List<Player> players = snapshotPlayers(zone);
                    for (Player pl : players) {
                        Service.gI().sendThongBao(pl, "Mau đi tìm Độc Nhãn");
                    }
                }
            }

        }
    }

    public ItemMap NR(Zone zone) {
        int x = Util.nextInt(100, zone.map.mapWidth - 100);
        int y = zone.map.yPhysicInTop(x, 100);
        int nr = Util.isTrue(1, 500) ? Util.nextInt(14, 18) : Util.nextInt(18, 20);
        ItemMap it = new ItemMap(zone, nr, 1, x, y, -1);
        return it;
    }

    public void randomNR() {
        if (isClosing || !isOpened) return;
        for (Zone zone : zones) {
            Service.gI().dropItemMap(zone, NR(zone));
            Service.gI().dropItemMap(zone, NR(zone));
            Service.gI().dropItemMap(zone, NR(zone));
            if (Util.isTrue(1, 2)) {
                Service.gI().dropItemMap(zone, NR(zone));
            }
            if (Util.isTrue(1, 3)) {
                Service.gI().dropItemMap(zone, NR(zone));
            }
            if (Util.isTrue(1, 4)) {
                Service.gI().dropItemMap(zone, NR(zone));
            }
            if (Util.isTrue(1, 5)) {
                Service.gI().dropItemMap(zone, NR(zone));
            }
        }
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
                    DungeonExitService.exit(pl, zone, 21 + pl.gender, -1, 5);
                } catch (Exception failure) {
                    nro.models.utils.Logger.logException(RedRibbonHQ.class, failure);
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
                    nro.models.boss.Boss_Manager.RedRibbonHQManager.gI().removeBoss(retiring);
                }
                bosses.remove(retiring);
            } catch (Exception failure) {
                complete = false;
                if (!bosses.contains(retiring)) bosses.add(retiring);
                nro.models.utils.Logger.logException(RedRibbonHQ.class, failure);
            }
        }
        bossesRetired = complete;
    }

    private void sendTextDoanhTrai() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().sendTextDoanhTrai(pl);
        }
    }

    public void sendTextTimePickDoanhTrai() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().sendTextTimePickDoanhTrai(pl);
        }
    }

    private void removeTextDoanhTrai() {
        if (this.clan == null) return;
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().removeTextDoanhTrai(pl);
        }
    }

    public void updateHPDame() {
        if (isClosing || !isOpened || clan == null) return;
        long totalDame = 0;
        long totalHp = 0;
        for (Player pl : this.clan.membersInGame) {
            if (pl != null && pl.nPoint != null) {
                totalDame += pl.nPoint.dame;
                totalHp += pl.nPoint.hpMax;
            }
        }

        //update hp dame quái
        for (Zone zone : this.zones) {
            for (Mob mob : zone.mobs) {
                if (mob.isDie()) {
                    continue;
                }
                mob.point.dame = (int) (totalHp / mob.tempId < 2_147_483_647 ? totalHp / mob.tempId : 2_147_483_647);
                mob.point.maxHp = (int) (totalDame/3 * mob.tempId < 2_147_483_647 ? totalDame * mob.tempId : 2_147_483_647);
                mob.point.hp = mob.point.maxHp;
                mob.setTiemNang();
            }
        }

        long dame = totalHp / 20;
        long hp = totalDame * 50;
        for (Boss boss : bosses) {
            if (boss.isDie()) {
                continue;
            }
            if (boss.zone.map.mapId == 59) {
                try {
                    long bossDamage = (dame);
                    long bossMaxHealth = (hp);
                    bossDamage = Math.min(bossDamage, 200000000L);
                    bossMaxHealth = Math.min(bossMaxHealth, 2000000000L);
                    boss.nPoint.hpMax = (int) bossMaxHealth;
                    boss.nPoint.dame = (int) bossDamage;
                    boss.nPoint.hp = boss.nPoint.hpMax;
                } catch (Exception exception) {
                }
            }
            if (boss.zone.map.mapId == 62) {
                try {
                    long bossDamage = (long) (dame * 1.1);
                    long bossMaxHealth = (long) (hp * 1.1);
                    bossDamage = Math.min(bossDamage, 200000000L);
                    bossMaxHealth = Math.min(bossMaxHealth, 2000000000L);
                    boss.nPoint.hpMax = (int) bossMaxHealth;
                    boss.nPoint.dame = (int) bossDamage;
                    boss.nPoint.hp = boss.nPoint.hpMax;
                } catch (Exception exception) {
                }
            }
            if (boss.zone.map.mapId == 55) {
                try {
                    long bossDamage = (long) (dame * 1.15);
                    long bossMaxHealth = (long) (hp * 1.15);
                    bossDamage = Math.min(bossDamage, 200000000L);
                    bossMaxHealth = Math.min(bossMaxHealth, 2000000000L);
                    boss.nPoint.hpMax = (int) bossMaxHealth;
                    boss.nPoint.dame = (int) bossDamage;
                    boss.nPoint.hp = boss.nPoint.hpMax;
                } catch (Exception exception) {
                }
            }
            if (boss.zone.map.mapId == 54) {
                try {
                    long bossDamage = (long) (dame * 1.2);
                    long bossMaxHealth = (long) (hp * 1.2);
                    bossDamage = Math.min(bossDamage, 200000000L);
                    bossMaxHealth = Math.min(bossMaxHealth, 2000000000L);
                    if (boss.id >= -14 && boss.id <= -9) {
                        bossDamage /= 10;
                        bossMaxHealth /= 10;
                    }
                    boss.nPoint.hpMax = (int) bossMaxHealth;
                    boss.nPoint.dame = (int) bossDamage;
                    boss.nPoint.hp = boss.nPoint.hpMax;
                } catch (Exception exception) {
                }
            }

            if (boss.zone.map.mapId == 57) {
                try {
                    long bossDamage = (long) (dame * 1.3);
                    long bossMaxHealth = (long) (hp * 1.3);
                    bossDamage = Math.min(bossDamage, 200000000L);
                    bossMaxHealth = Math.min(bossMaxHealth, 2000000000L);
                    boss.nPoint.hpMax = (int) bossMaxHealth;
                    boss.nPoint.dame = (int) bossDamage;
                    boss.nPoint.hp = boss.nPoint.hpMax;
                } catch (Exception exception) {
                }
            }
        }

    }

    private void dispose() {
        if (hasPlayersRemaining()) { isClosing = true; return; }
        retireBosses();
        if (!bossesRetired) { isClosing = true; return; }
        this.removeTextDoanhTrai();
        // remove bosses
        // remove itemmap
        for (Zone zone : zones) {
            if (zone == null) continue;
            for (int i = zone.items.size() - 1; i >= 0; i--) {
                if (i < zone.items.size()) {
                    ItemMapService.gI().removeItemMap(zone.items.get(i));
                }
            }
        }
        this.bosses.clear();
        this.winDT = false;
        synchronized (nro.models.services_dungeon.DungeonAllocationLock.LOCK) {
        this.isOpened = false;
        if (this.clan != null) this.clan.haveGoneDoanhTrai = true;
        if (this.clan != null && this.clan.doanhTrai == this) this.clan.doanhTrai = null;
        this.clan = null;
        this.isTimePicking = false;
        this.isInitializing = false;
        }
    }
}
