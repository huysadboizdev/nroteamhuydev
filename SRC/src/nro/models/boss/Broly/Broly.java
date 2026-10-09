package nro.models.boss.Broly;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.BossStatus;
import static nro.models.consts.BossType.BROLY;
import nro.models.consts.ConstPlayer;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.services.SkillService;
import nro.models.map.service.ChangeMapService;
import nro.models.skill.Skill;
import nro.models.utils.SkillUtil;
import nro.models.utils.Util;

public class Broly extends Boss {
    private long lastTimeAttack;
    private BrolyZoneCoordinator.Lease lease;
    private boolean transforming;
    private long lastTransformationFailure;

    public Broly() throws Exception {

        super(BROLY, BossID.BROLY, new BossData(
                "Broly", //name
                ConstPlayer.XAYDA, //gender
                new short[]{291, 292, 293, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
                100, //dame
                new int[]{500}, //hp
                new int[]{5, 13, 20, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38}, //map join
                new int[][]{
                    {Skill.TAI_TAO_NANG_LUONG, 1, 1000}, {Skill.TAI_TAO_NANG_LUONG, 2, 1000}, {Skill.TAI_TAO_NANG_LUONG, 3, 1000}, {Skill.TAI_TAO_NANG_LUONG, 4, 1000}, {Skill.TAI_TAO_NANG_LUONG, 5, 1000}, {Skill.TAI_TAO_NANG_LUONG, 6, 1000}, {Skill.TAI_TAO_NANG_LUONG, 7, 100},
                    {Skill.DRAGON, 1, 1000}, {Skill.DRAGON, 2, 1000}, {Skill.DRAGON, 3, 1000}, {Skill.DRAGON, 4, 1000}, {Skill.DRAGON, 5, 1000}, {Skill.DRAGON, 6, 1000}, {Skill.DRAGON, 7, 1000},
                    {Skill.DEMON, 1, 1000}, {Skill.DEMON, 2, 1000}, {Skill.DEMON, 3, 1000}, {Skill.DEMON, 4, 1000}, {Skill.DEMON, 5, 1000}, {Skill.DEMON, 6, 1000}, {Skill.DEMON, 7, 1000},
                    {Skill.GALICK, 1, 1000}, {Skill.GALICK, 2, 1000}, {Skill.GALICK, 3, 1000}, {Skill.GALICK, 4, 1000}, {Skill.GALICK, 5, 1000}, {Skill.GALICK, 6, 1000}, {Skill.GALICK, 7, 1000},
                    {Skill.KAMEJOKO, 1, 1000}, {Skill.KAMEJOKO, 2, 1000}, {Skill.KAMEJOKO, 3, 1000}, {Skill.KAMEJOKO, 4, 1000}, {Skill.KAMEJOKO, 5, 1000}, {Skill.KAMEJOKO, 6, 1000}, {Skill.KAMEJOKO, 7, 1000},
                    {Skill.MASENKO, 1, 1000}, {Skill.MASENKO, 2, 1000}, {Skill.MASENKO, 3, 1000}, {Skill.MASENKO, 4, 1000}, {Skill.MASENKO, 5, 1000}, {Skill.MASENKO, 6, 1000}, {Skill.MASENKO, 7, 1000},
                    {Skill.ANTOMIC, 1, 1000}, {Skill.ANTOMIC, 2, 1000}, {Skill.ANTOMIC, 3, 1000}, {Skill.ANTOMIC, 4, 1000}, {Skill.ANTOMIC, 5, 1000}, {Skill.ANTOMIC, 6, 1000}, {Skill.ANTOMIC, 7, 1000},}, //skill
                new String[]{}, //text chat 1
                new String[]{"|-1|Haha! ta sẽ giết hết các ngươi",
                    "|-1|Sức mạnh của ta là tuyệt đối",
                    "|-1|Vào hết đây!!!",}, //text chat 2
                new String[]{"|-1|Các ngươi giỏi lắm. Ta sẽ quay lại."}, //text chat 3
                600//type appear
        ));
        nro.models.boss.Boss_Manager.BrolyManager.gI().publish(this);
    }

    @Override
    public synchronized void active() {
        if (transformToSuper()) return;
        if (zone!=null && !isDie() && bossStatus==BossStatus.ACTIVE) super.active();
        transformToSuper();
    }

    @Override
    public void joinMap() {
        this.name = "Broly " + Util.nextInt(10, 100);
        this.nPoint.hpMax = Util.nextInt(500, 100000);
        this.nPoint.hp = this.nPoint.hpMax;
        this.nPoint.dame = this.nPoint.hpMax / 100;
        this.nPoint.crit = Util.nextInt(50);
        nro.models.services.BossAdminConfig.applyAfterNativeSpawn(this);
        this.joinMap2();
        st = System.currentTimeMillis();
    }

    public synchronized void joinMap2() {
        if(lease!=null && coordinator().owns(lease))return;
        Zone candidate=zone!=null?zone:parentBoss!=null?parentBoss.zone:lastZone!=null?lastZone:getMapJoin();
        zone=null;
        if(candidate!=null && candidate.map.zones.size()>2) {
            int count=candidate.map.zones.size()-2;
            int start=Util.nextInt(count);
            for(int i=0;i<count;i++) {
                Zone target=candidate.map.zones.get(2+(start+i)%count);
                lease=coordinator().reserve(target,this);
                if(lease==null)continue;
                try {
                    enterZone(target,-1,-1);
                    if(zone!=target || !target.getBosses().contains(this))throw new IllegalStateException("Broly map admission rejected");
                    changeStatus(BossStatus.CHAT_S);return;
                } catch(Exception failure) {
                    cleanupZone();coordinator().release(lease);lease=null;
                    nro.models.utils.Logger.logException(getClass(),failure,"Broly spawn map="+target.map.mapId+" zone="+target.zoneId);
                    break;
                }
            }
        }
        lastZone=null;lastTimeRest=System.currentTimeMillis();changeStatus(BossStatus.REST);
    }

    private BrolyZoneCoordinator coordinator() {return nro.models.boss.Boss_Manager.BrolyManager.gI().zones();}
    public synchronized void claimExistingZone() {if(zone!=null)lease=coordinator().reserve(zone,this);}
    protected void enterZone(Zone target,int x,int y) {
        // changeMap expects a boss's destination to be selected before admission.
        if(zone==null)zone=target;
        ChangeMapService.gI().changeMap(this,target,x,y);
    }
    protected void exitZone() {ChangeMapService.gI().exitMap(this);}
    private void cleanupZone() {
        Zone old=zone;
        try {exitZone();} finally {if(old!=null)old.removePlayer(this);zone=null;}
    }
    protected SuperBroly createSuperBroly(Zone target,int x,int y) throws Exception {return new SuperBroly(target,x,y);}

    public synchronized boolean transformToSuper() {
        if(transforming || zone==null || nPoint==null || isDie() || bossStatus!=BossStatus.ACTIVE
                || nPoint.hpMax<1_500_000 || System.currentTimeMillis()-lastTransformationFailure<30_000)return false;
        Zone old=zone;int x=location.x,y=location.y;
        if(lease==null)lease=coordinator().reserve(old,this);
        if(!coordinator().owns(lease))return false;
        transforming=true;
        SuperBroly next=null;
        BrolyZoneCoordinator.Lease nextLease=null;
        boolean detached=false;
        try {
            next=createSuperBroly(old,x,y);
            // The normal boss remains owner while construction can fail.
            cleanupZone();detached=true;
            nextLease=coordinator().transfer(lease,next);
            if(nextLease==null)throw new IllegalStateException("Broly transformation ownership changed");
            next.acceptLease(nextLease);
            next.joinMap();
            if(next.zone!=old || !old.getBosses().contains(next))throw new IllegalStateException("Super Broly map admission rejected");
            lease=null;lastZone=null;lastTimeRest=System.currentTimeMillis();changeStatus(BossStatus.REST);
            return true;
        } catch(Exception failure) {
            if(next!=null)next.abortSpawn();
            if(nextLease!=null)lease=coordinator().transfer(nextLease,this);
            if(detached || zone!=old) {
                try {enterZone(old,x,y);if(zone!=old || !old.getBosses().contains(this))throw new IllegalStateException("Broly rollback admission rejected");changeStatus(BossStatus.ACTIVE);}
                catch(Exception rollback) {
                    failure.addSuppressed(rollback);old.addPlayer(this);zone=old;location.x=x;location.y=y;bossStatus=BossStatus.ACTIVE;
                    try {changeStatus(BossStatus.ACTIVE);} catch(Exception notice) {failure.addSuppressed(notice);}
                }
            }
            lastTransformationFailure=System.currentTimeMillis();
            nro.models.utils.Logger.logException(getClass(),failure,"Broly transformation map="+old.map.mapId+" zone="+old.zoneId);
            return false;
        } finally {transforming=false;}
    }

    private long st;

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!transforming && zone!=null && bossStatus!=BossStatus.REST && !this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            if (Util.isTrue(1, 10)) {
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, 6));
                this.tangChiSo();
                SkillService.gI().useSkill(this, null, null, -1, null);
            }
            damage = this.nPoint.subDameInjureWithDeff(damage);
            boolean suicide=plAtt!=null && plAtt.playerSkill!=null && plAtt.playerSkill.skillSelect!=null
                    && plAtt.playerSkill.skillSelect.template!=null && plAtt.playerSkill.skillSelect.template.id==Skill.TU_SAT;
            if (!piercing && !suicide && damage > this.nPoint.hpMax / 100) {
                damage = this.nPoint.hpMax / 100;
            }
            this.nPoint.subHP(damage);
            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }
            return (int) damage;
        } else {
            return 0;
        }
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 100) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(7, this.playerSkill.skills.size() - 1));
                if (Util.getDistance(this, pl) <= this.getRangeCanAttackWithSkillSelect()) {
                    if (Util.isTrue(5, 20)) {
                        if (SkillUtil.isUseSkillChuong(this)) {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 200)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 70));
                        } else {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(10, 40)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 50));
                        }
                    }
                    if (Util.isTrue(1, 100)) {
                        this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, 6));
                        this.tangChiSo();
                    }

                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                } else {
                    if (Util.isTrue(1, 2)) {
                        this.moveToPlayer(pl);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void die(Player plKill) {
        nro.models.services.BossAdminDropService.rewardWithoutDefault(this, plKill);
        this.changeStatus(BossStatus.DIE);
    }

    private void tangChiSo() {
        int hpMax = this.nPoint.hpMax;
        int rand = Util.nextInt(10, 100);
        hpMax = hpMax + hpMax / rand < 16_070_777 ? hpMax + hpMax / rand : 16_070_777;
        this.nPoint.hpMax = nro.models.services.BossAdminConfig.nativeHp(this, hpMax);
        this.nPoint.dame = nro.models.services.BossAdminConfig.nativeDamage(this, this.nPoint.hpMax / 10);
    }

    @Override
    public synchronized void leaveMap() {
        try {cleanupZone();} finally {
            coordinator().release(lease);lease=null;
            this.lastZone=null;this.lastTimeRest=System.currentTimeMillis();changeStatus(BossStatus.REST);
        }
    }
    @Override public synchronized void dispose() {
        leaveMap();nro.models.boss.Boss_Manager.BrolyManager.gI().removeBoss(this);super.dispose();
    }
}
