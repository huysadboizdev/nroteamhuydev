package nro.models.boss.Broly;

import nro.models.boss.Boss;
import nro.models.boss.Boss_Manager.BrolyManager;
import nro.models.boss.BossData;
import nro.models.boss.BossID;
import nro.models.consts.BossStatus;
import static nro.models.consts.BossType.BROLY;
import nro.models.consts.ConstPlayer;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.services.PetService;
import nro.models.services.MabuRareLoot;
import nro.models.services.MiniBossPinkCapsuleDrop;
import nro.models.services.SkillService;
import nro.models.map.service.ChangeMapService;
import nro.models.skill.Skill;
import nro.models.services.PlayerService;
import nro.models.utils.SkillUtil;
import nro.models.utils.Util;

public class SuperBroly extends Boss {
    private BrolyZoneCoordinator.Lease lease;
    private boolean joined;
    private boolean retired;
    private boolean rewardGiven;
    private MabuRareLoot rareLoot;
    private boolean handoff;

    public SuperBroly(Zone zone, int x, int y) throws Exception {

        super(BROLY, BossID.SUPER_BROLY, false, false, new BossData(
                "Super Broly", //name
                ConstPlayer.XAYDA, //gender
                new short[]{294, 295, 296, -1, -1, -1}, //outfit {head, body, leg, bag, aura, eff}
                100, //dame
                new int[]{1000}, //hp
                new int[]{5, 13, 20, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38}, //map join
                new int[][]{
                    {Skill.TAI_TAO_NANG_LUONG, 1, 1000}, {Skill.TAI_TAO_NANG_LUONG, 2, 1000}, {Skill.TAI_TAO_NANG_LUONG, 3, 1000}, {Skill.TAI_TAO_NANG_LUONG, 4, 1000}, {Skill.TAI_TAO_NANG_LUONG, 5, 1000}, {Skill.TAI_TAO_NANG_LUONG, 6, 1000}, {Skill.TAI_TAO_NANG_LUONG, 7, 1000},
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
                1//type appear
        ));
        this.zone = zone;
        this.location.x = x;
        this.location.y = y;
        currentLevel=0;
        initBase();
        this.name = "Super Broly " + Util.nextInt(10, 100);
        this.nPoint.hpMax = Util.nextInt(1_500_000, 16_070_777);
        this.nPoint.hp = this.nPoint.hpMax;
        this.nPoint.dame = this.nPoint.hpMax / 100;
        this.nPoint.crit = Util.nextInt(50);
        nro.models.services.BossAdminConfig.applyAfterNativeSpawn(this);
    }

    void acceptLease(BrolyZoneCoordinator.Lease lease) {this.lease=lease;this.handoff=true;}
    private BrolyZoneCoordinator coordinator() {return BrolyManager.gI().zones();}
    public synchronized void claimExistingZone() {
        if(zone!=null){lease=coordinator().reserve(zone,this);joined=coordinator().owns(lease);st=System.currentTimeMillis();}
    }
    protected void enterZone(Zone target,int x,int y) {
        if(zone==null)zone=target;
        ChangeMapService.gI().changeMap(this,target,x,y);
    }
    protected void exitZone() {ChangeMapService.gI().exitMap(this);}
    protected void createCompanion() {PetService.gI().createNormalPet(this);}
    protected void clearCompanion() {
        if(pet!=null) {try {ChangeMapService.gI().exitMap(pet);} finally {if(pet.zone!=null)pet.zone.removePlayer(pet);pet.zone=null;pet.dispose();pet=null;}}
    }
    /** Failure cleanup keeps the handoff lease until the caller restores its owner. */
    synchronized void abortSpawn() {
        Zone old=zone;
        try {exitZone();} catch(Exception error) {nro.models.utils.Logger.logException(getClass(),error,"Super Broly abort map exit");}
        finally {
            if(old!=null)old.removePlayer(this);zone=null;
            try {clearCompanion();} catch(Exception error) {nro.models.utils.Logger.logException(getClass(),error,"Super Broly companion cleanup");}
            BrolyManager.gI().removeBoss(this);joined=false;retired=true;bossStatus=BossStatus.REST;
        }
    }

    @Override
    protected boolean rewardAllowed(Player killer) { return !retired && super.rewardAllowed(killer); }

    @Override
    public synchronized void rewardDefault(Player plKill) {
        if(rewardGiven || retired)return;
        Player owner = MiniBossPinkCapsuleDrop.creditedPlayer(plKill);
        if (owner == null) return;
        rewardGiven=true;
        if (rareLoot == null) rareLoot = new MabuRareLoot();
        rareLoot.drop(this,owner,5,3);
        if (owner.pet == null) {
            PetService.gI().createNormalPet(owner);
        }
    }

    @Override
    public void active() {
        super.active();
    }

    @Override
    public synchronized void joinMap() {
        if(joined || retired)return;
        Zone target=zone;
        if(lease==null)lease=coordinator().reserve(target,this);
        if(!coordinator().owns(lease))throw new IllegalStateException("Super Broly zone unavailable");
        try {
            createCompanion();
            enterZone(target,location.x,location.y);
            if(zone!=target || !target.getBosses().contains(this))throw new IllegalStateException("Super Broly map admission rejected");
            joined=true;st=System.currentTimeMillis();
            changeStatus(BossStatus.CHAT_S);
            notifyJoinMap();
            BrolyManager.gI().publish(this);
        } catch(RuntimeException failure) {abortSpawn();if(!handoff){coordinator().release(lease);lease=null;}throw failure;}
    }

    private long st;

    @Override
    public synchronized void autoLeaveMap() {
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        } else if (Util.canDoWithTime(st, 300_000)) {
            this.leaveMap();
        }
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if(damage<=0||plAtt==null||plAtt.beforeDispose||zone==null||plAtt.zone!=zone||isMobAttack)return 0;
        boolean reflected=plAtt==this&&piercing;
        Skill executing=plAtt.playerSkill==null?null:plAtt.playerSkill.skillSelect;
        if(!reflected&&plAtt.newSkill!=null&&plAtt.newSkill.isStartSkillSpecial)executing=plAtt.newSkill.skillSelect;
        Integer attributedSkill=nro.models.services.BossNoticeService.damageSkillFor(this);
        int skillId=attributedSkill!=null?attributedSkill:executing!=null&&executing.template!=null?executing.template.id:-1;
        boolean suicide=!reflected&&skillId==Skill.TU_SAT;
        boolean melee=switch(skillId){case Skill.DRAGON,Skill.DEMON,Skill.GALICK,Skill.LIEN_HOAN,Skill.KAIOKEN -> true;default -> false;};
        if(!reflected&&!suicide&&!melee)return 0;
        if (!retired && joined && !this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            if (Util.isTrue(1, 30)) {
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, 6));
                this.tangChiSo();
                SkillService.gI().useSkill(this, null, null, -1, null);
            }
            damage = suicide ? 10_000 : this.nPoint.subDameInjureWithDeff(damage);
            if (!piercing && !suicide && damage > this.nPoint.hpMax / 100) {
                damage = this.nPoint.hpMax / 100;
            }
            damage=Math.min(damage,this.nPoint.hp);
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

    private long lastTimeAttack;

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

    private void tangChiSo() {
        int hpMax = this.nPoint.hpMax;
        int rand = Util.nextInt(80, 100);
        hpMax = hpMax + hpMax / rand < 16_070_777 ? hpMax + hpMax / rand : 16_070_777;
        this.nPoint.hpMax = nro.models.services.BossAdminConfig.nativeHp(this, hpMax);
        this.nPoint.dame = nro.models.services.BossAdminConfig.nativeDamage(this, this.nPoint.hpMax / 10);
    }

    @Override
    public synchronized void leaveMap() {
        if(retired)return;
        try {abortSpawn();} finally {
            coordinator().release(lease);lease=null;
            lastZone=null;lastTimeRest=System.currentTimeMillis();changeStatus(BossStatus.REST);
        }
    }
    @Override public synchronized void dispose() {leaveMap();BrolyManager.gI().removeBoss(this);super.dispose();}
}
