package nro.models.boss.Boss_mini;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstPlayer;
import nro.models.consts.ConstRatio;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import nro.models.consts.ConstTaskBadges;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.server.Client;
import nro.models.services.EffectSkillService;
import nro.models.services.ItemTimeService;
import nro.models.services.Service;
import nro.models.services.SkillService;
import nro.models.map.service.ChangeMapService;
import nro.models.skill.Skill;
import nro.models.task.BadgesTaskService;
import nro.models.utils.Util;

public class Virut extends Boss {

    private final Map<Long, DelayedBossEffect> globalEffectTimers = new ConcurrentHashMap<>();
    private long st;

    public Virut() throws Exception {
        super(BossID.Virut, new BossData(
                "Virut " + Util.nextInt(1, 49),
                ConstPlayer.TRAI_DAT,
                new short[]{651, 778, 779, -1, -1, -1},
                10,
                new int[]{100},
                new int[]{5, 7, 0, 14},
                new int[][]{{Skill.DRAGON, 7, 1000}},
                new String[]{}, // Text chat 1
                new String[]{}, // Text chat 2
                new String[]{},
                600));
    }

    @Override
    public void die(Player plKill) {
        this.reward(plKill);
        this.changeStatus(BossStatus.DIE);
    }

    private void applyEffect(Player player) {
        synchronized (player) {
            DelayedBossEffect effect = new DelayedBossEffect(this, player, 300000, 7143);
            if (!effect.valid(this)) return;
            DelayedBossEffect previous = globalEffectTimers.get(player.id);
            if (previous != null && previous.valid(this)) return;
            globalEffectTimers.put(player.id, effect);
            ItemTimeService.gI().sendItemTime(player, 7143, 10);
            this.chat(player.name + " đã bị nhiễm Virut!");
        }
    }

    private void checkGlobalEffects() {
        long now = System.currentTimeMillis();
        globalEffectTimers.forEach((id, effect) -> {
            synchronized (effect.target) {
                if (!effect.valid(this) || now >= effect.deadline) {
                    if (globalEffectTimers.remove(id, effect)) effect.expire(this);
                }
            }
        });
    }

    private void updateOdo() {
        try {
            if (Util.isTrue(30, 100)) {
                List<Player> playersMap = this.zone.getNotBosses();
                for (Player pl : playersMap) {
                    if (pl != null && pl.nPoint != null && !this.equals(pl) && !pl.isBoss && !pl.isDie()
                            && Util.getDistance(this, pl) <= 200) {
                        applyEffect(pl);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 3000) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = this.getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }

                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));

                if (Util.getDistance(this, pl) <= 40) {
                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                    if (!globalEffectTimers.containsKey(pl.id)
                            || System.currentTimeMillis() >= globalEffectTimers.get(pl.id).deadline) {
                        this.updateOdo();
                    }
                } else {
                    this.moveToPlayer(pl);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void rewardDefault(Player plKill) {
        int[] itemIds = {1591, 1594};
        for (int i = 0; i < itemIds.length; i++) {
            int itemId = itemIds[i];
            ItemMap itemDrop = new ItemMap(
                    this.zone,
                    itemId,
                    1,
                    this.location.x + Util.nextInt(-30, 30),
                    this.zone.map.yPhysicInTop(this.location.x, this.location.y - 24),
                    plKill.id
            );
            Service.gI().dropItemMap(this.zone, itemDrop);
        }

        int diem = 5;
        plKill.event.addEventPoint(diem);
        Service.gI().sendThongBao(plKill, "+5 Point sự kiện từ Virut");
    }

    @Override
    public void joinMap() {
        globalEffectTimers.clear();
        this.name = "Virut " + Util.nextInt(1, 49);
        this.nPoint.hpMax = 100;
        this.nPoint.hp = this.nPoint.hpMax;
        this.nPoint.dameg = 1;
        this.nPoint.dame = 1;
        nro.models.services.BossAdminConfig.applyAfterNativeSpawn(this);
        this.joinMap2();
        st = System.currentTimeMillis();
    }

    public void joinMap2() {
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
                int zoneid = 0;
                this.zone = this.zone.map.zones.get(zoneid);
                ChangeMapService.gI().changeMap(this, this.zone, -1, -1);

                this.changeStatus(BossStatus.CHAT_S);
            } catch (Exception e) {
                this.changeStatus(BossStatus.REST);
            }
        } else {
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    @Override
    public void leaveMap() {
        globalEffectTimers.clear();
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
    }
    
    @Override
    public void active() {
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
        }
        this.attack();
        if (Util.canDoWithTime(st, 900000)) {
            this.changeStatus(BossStatus.LEAVE_MAP);
            this.checkGlobalEffects();
        }
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            int actualDamage = 1;
            this.nPoint.subHP(actualDamage);

            if (this.nPoint.hp <= 0) {
                this.die(plAtt);
            }

            return actualDamage;
        } else {
            return 0;
        }
    }

}
