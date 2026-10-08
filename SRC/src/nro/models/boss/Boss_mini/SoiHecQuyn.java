package nro.models.boss.Boss_mini;

import nro.models.boss.Boss;
import nro.models.boss.BossesData;
import nro.models.boss.BossID;
import nro.models.consts.BossStatus;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.ConstTaskBadges;
import java.util.ArrayList;
import java.util.List;
import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.services.SkillService;
import nro.models.services.InventoryService;
import nro.models.services.MiniBossPinkCapsuleDrop;
import nro.models.item.Item;
import nro.models.map.service.ChangeMapService;
import nro.models.task.BadgesTaskService;
import nro.models.utils.Logger;
import nro.models.utils.Util;

public class SoiHecQuyn extends Boss {

    private nro.models.services.MiniBossPinkCapsuleDrop capsuleReward;

    @Override
    protected synchronized void resetBase() {
        super.resetBase();
        if (capsuleReward != null) capsuleReward.reset();
    }

    private long lastTimeDrop;
    private long st;
    private int timeLeave;
    private volatile boolean KiemTraNhatXuong = false;
    private long ThoiGianNhatXuong = 0;
    private long lastTimRestPawn;
    private long lastBoneNotice;
    private long lastBoneNoticeOwner;

    public SoiHecQuyn() throws Exception {
        super(MiniBossIdentity.nextId(), BossesData.SOI_HEC_QUYN);
    }

    @Override
    public void joinMap() {
        this.ThoiGianNhatXuong = 0;
        this.KiemTraNhatXuong = false;
        st = System.currentTimeMillis();
        timeLeave = Util.nextInt(100000, 300000);
        if (zoneFinal != null) {
            joinMapByZone(zoneFinal);
            this.changeStatus(BossStatus.CHAT_S);
            this.wakeupAnotherBossWhenAppear();
            this.ThoiGianNhatXuong = 0;
            this.KiemTraNhatXuong = false;
            return;
        }

        if (this.zone == null) {
            if (this.parentBoss != null) {
                this.zone = parentBoss.zone;
            } else if (this.lastZone == null) {
                this.zone = getMapJoin();
            } else {
                this.zone = this.lastZone;
            }
        }

        if (this.zone != null) {
            try {
                List<Zone> availableZones = new ArrayList<>();

                // Lọc các zone thỏa mãn điều kiện: số lượng người chơi <= 10 và không có boss
                for (Zone zone : this.zone.map.zones) {
                    if (zone.getNumOfPlayers() <= 10 && BossManager.gI().getBosses().stream()
                            .noneMatch(boss -> boss != this && boss instanceof SoiHecQuyn
                                    && boss.zone == zone && !boss.isDie())) {
                        availableZones.add(zone);
                    }
                }

                if (!availableZones.isEmpty()) {
                    // Random chọn một zone hợp lệ
                    int randomIndex = Util.nextInt(availableZones.size());
                    this.zone = availableZones.get(randomIndex);
                    ChangeMapService.gI().changeMap(this, this.zone, Util.nextInt(100, 500), this.zone.map.yPhysicInTop(this.location.x,
                            this.location.y - 24));
                    this.changeStatus(BossStatus.CHAT_S);
                    st = System.currentTimeMillis();
                    timeLeave = Util.nextInt(100000, 300000);
                } else {
                    this.leaveMapNew();
                    return;
                }
            } catch (Exception e) {
                Logger.error(this.data[0].getName() + ": Lỗi đang tiến hành REST\n");
                this.changeStatus(BossStatus.REST);
            }
        } else {
            Logger.error(this.data[0].getName() + ": Lỗi map đang tiến hành RESPAWN\n");
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    @Override
    public void chatM() {
        if (this.data[this.currentLevel].getTextM().length == 0) {
            return;
        }
        if (!Util.canDoWithTime(this.lastTimeChatM, this.timeChatM)) {
            return;
        }
        String textChat = this.data[this.currentLevel].getTextM()[Util.nextInt(0, this.data[this.currentLevel].getTextM().length - 1)];
        int prefix = Integer.parseInt(textChat.substring(1, textChat.lastIndexOf("|")));
        textChat = textChat.substring(textChat.lastIndexOf("|") + 1);
        this.chat(prefix, textChat);
        this.lastTimeChatM = System.currentTimeMillis();
        this.timeChatM = Util.nextInt(3000, 20000);
    }

    @Override
    public void active() {
        this.attack();
    }

    @Override
    public void autoLeaveMap() {
        if (KiemTraNhatXuong && Util.canDoWithTime(ThoiGianNhatXuong, 5000)
                || !KiemTraNhatXuong && Util.canDoWithTime(st, timeLeave)) {
            this.leaveMapNew();
        }
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);

    }

    public void NhatXuong() {
        ThoiGianNhatXuong = System.currentTimeMillis();
        KiemTraNhatXuong = true;
    }

    public boolean KiemTraNhatXuong() {
        return KiemTraNhatXuong;
    }

    @Override
    public void attack() {
        if (KiemTraNhatXuong) {
            return;
        }
        if (Util.canDoWithTime(this.lastTimeAttack, 100)) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.location == null || pl.isDie()) {
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                if (Util.getDistance(this, pl) <= this.getRangeCanAttackWithSkillSelect()) {
                    if (Util.isTrue(5, 20) && Util.getDistance(this, pl) > 50) {
                        if (Util.isTrue(5, 20)) {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 200)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 70));
                        } else {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(10, 40)), pl.location.y);
                        }
                    }
                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                } else {
                    if (Util.isTrue(1, 2)) {
                        this.moveToPlayer(pl);
                    }
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        Player owner = MiniBossPinkCapsuleDrop.creditedPlayer(plAtt);
        if (owner == null) {
            synchronized (this) {
                return KiemTraNhatXuong || damage <= 0 ? 0 : super.injured(plAtt, damage, piercing, isMobAttack);
            }
        }
        // Feeding and attacks use the same owner -> boss lock order.
        synchronized (owner) {
            synchronized (this) {
                if (KiemTraNhatXuong || damage <= 0 || isDie()) return 0;
                int bonePlace = bonePlace(owner);
                if (bonePlace != 0) {
                    if (lastBoneNoticeOwner != owner.id || Util.canDoWithTime(lastBoneNotice, 2000)) {
                        Service.gI().sendThongBao(owner, bonePlace == 2
                                ? "Bạn có Cục xương trong rương. Hãy lấy ra hành trang và dùng để cho Sói hẹc quyn ăn."
                                : "Hãy dùng Cục xương trong hành trang để cho Sói hẹc quyn ăn.");
                        lastBoneNotice = System.currentTimeMillis();
                        lastBoneNoticeOwner = owner.id;
                    }
                    return 0;
                }
                return super.injured(plAtt, damage, piercing, isMobAttack);
            }
        }
    }

    private int bonePlace(Player owner) {
        if (owner.inventory == null) return 0;
        if (containsBone(owner.inventory.itemsBag)) return 1;
        return containsBone(owner.inventory.itemsBox) ? 2 : 0;
    }

    private boolean containsBone(List<Item> items) {
        if (items == null) return false;
        for (Item item : items) {
            if (item != null && item.template != null && item.template.id == 460
                    && InventoryService.gI().isBillIngredientUsable(item)) return true;
        }
        return false;
    }

    @Override
    public void reward(Player plKill) {
        plKill = nro.models.services.MiniBossPinkCapsuleDrop.creditedPlayer(plKill);
        if (plKill == null) return;
        synchronized (plKill) {
            synchronized (this) {
                if (capsuleReward == null) capsuleReward = new nro.models.services.MiniBossPinkCapsuleDrop();
                if (!capsuleReward.drop(this, plKill)) return;
                try {
                    int[] itemDropIds = {1591, 1594};
                    for (int itemId : itemDropIds) {
                        int x = this.location.x + Util.nextInt(-20, 20);
                        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);

                        ItemMap itemMap = new ItemMap(this.zone, itemId, 1, x, y, plKill.id);
                        Service.gI().dropItemMap(this.zone, itemMap);
                    }
                    BadgesTaskService.updateCountBagesTask(plKill, ConstTaskBadges.KE_THAO_TUNG_SOI, 1);
                    int diem = 5;
                    plKill.event.addEventPoint(diem);
                    Service.gI().sendThongBao(plKill, "+5 Point");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
