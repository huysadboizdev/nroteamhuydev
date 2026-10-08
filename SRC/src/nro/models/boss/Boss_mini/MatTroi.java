package nro.models.boss.Boss_mini;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstPlayer;
import nro.models.consts.ConstTaskBadges;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Player;
import nro.models.server.Client;
import nro.models.services.ItemTimeService;
import nro.models.services.Service;
import nro.models.services.SkillService;
import nro.models.skill.Skill;
import nro.models.task.BadgesTaskService;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 */
public class MatTroi extends Boss {

    private nro.models.services.MiniBossPinkCapsuleDrop capsuleReward;

    @Override
    protected synchronized void resetBase() {
        super.resetBase();
        if (capsuleReward != null) capsuleReward.reset();
    }

    private final Map<Long, DelayedBossEffect> globalEffectTimers = new ConcurrentHashMap<>();
    private long st;

    public MatTroi() throws Exception {
        super(MiniBossIdentity.nextId(), new BossData(
                "Mặt Trời " + Util.nextInt(1, 49),
                ConstPlayer.TRAI_DAT,
                new short[]{1501, 1502, 1503, -1, -1, -1},
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
        try { this.reward(plKill); }
        finally { this.changeStatus(BossStatus.DIE); }
    }

    private void applyEffect(Player player) {
        synchronized (player) {
            DelayedBossEffect effect = new DelayedBossEffect(this, player, 30000, 12953);
            if (!effect.valid(this)) return;
            DelayedBossEffect previous = globalEffectTimers.get(player.id);
            if (previous != null && previous.valid(this)) return;
            globalEffectTimers.put(player.id, effect);
            ItemTimeService.gI().sendItemTime(player, 12953, 30);
            this.chat(player.name + " đã bị bỏng nhiệt!");
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
    public synchronized void reward(Player plKill) {
        plKill = nro.models.services.MiniBossPinkCapsuleDrop.creditedPlayer(plKill);
        if (capsuleReward == null) capsuleReward = new nro.models.services.MiniBossPinkCapsuleDrop();
        if (!capsuleReward.drop(this, plKill)) return;
        BadgesTaskService.updateCountBagesTask(plKill, ConstTaskBadges.KOL, 1);
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);
        if (Util.isTrue(50, 100)) {
            int[] costumes = {1562};
            int costumeId = costumes[Util.nextInt(costumes.length)];

            ItemMap itemMap = new ItemMap(this.zone, costumeId, 1, x, y, plKill.id);

            itemMap.options.add(new Item.ItemOption(50, Util.nextInt(7, 10)));
            itemMap.options.add(new Item.ItemOption(77, Util.nextInt(7, 10)));
            itemMap.options.add(new Item.ItemOption(103, Util.nextInt(7, 10)));
            itemMap.options.add(new Item.ItemOption(30, 0));
            itemMap.options.add(new Item.ItemOption(93, Util.nextInt(2, 5)));

            Service.gI().dropItemMap(this.zone, itemMap);
        }
    }

    @Override
    public void joinMap() {
        globalEffectTimers.clear();
        this.name = "Mặt Trời " + Util.nextInt(1, 49);
        this.nPoint.hpMax = 100;
        this.nPoint.hp = this.nPoint.hpMax;
        this.nPoint.dameg = 1;
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
            // Selection and admission are one operation for concurrent respawns.
            synchronized (MatTroi.class) {
                try {
                    nro.models.map.Zone selected = null;
                    for (nro.models.map.Zone candidate : this.zone.map.zones) {
                        boolean occupied = new java.util.ArrayList<>(candidate.getBosses()).stream()
                                .anyMatch(boss -> boss != this && boss instanceof MatTroi);
                        if (!occupied) {
                            selected = candidate;
                            break;
                        }
                    }
                    if (selected == null) {
                        this.zone = null;
                        this.lastZone = null;
                        this.changeStatus(BossStatus.RESPAWN);
                        return;
                    }
                    this.joinMapByZone(selected);
                    this.changeStatus(BossStatus.CHAT_S);
                } catch (Exception e) {
                    this.zone = null;
                    this.lastZone = null;
                    this.changeStatus(BossStatus.RESPAWN);
                }
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
        this.checkGlobalEffects();
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
        }
        this.attack();
        if (Util.canDoWithTime(st, 900000)) {
            this.changeStatus(BossStatus.LEAVE_MAP);
        }
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (damage <= 0) return 0;
        if (!this.isDie()) {
            int actualDamage = 1;
            this.nPoint.subHP(actualDamage);

            if (this.nPoint.hp <= 0) {
                this.setDie(plAtt);
                this.die(plAtt);
            }

            return actualDamage;
        } else {
            return 0;
        }
    }
}
