package nro.models.boss.Mabu;

import nro.models.boss.*;
import nro.models.consts.*;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Player;
import nro.models.services.*;
import nro.models.skill.Skill;
import nro.models.utils.Util;

/** Public tournament hunting boss, independent of the timed Mabu transformation chain. */
public final class FatMabuTournament extends Boss {
    public static final int EGG_RATE=5,BOTTLE_RATE=3;
    private volatile boolean finished;
    private final MabuRareLoot rare=new MabuRareLoot();
    private final SealedBossEquipmentLoot equipment=new SealedBossEquipmentLoot(90,8);
    public FatMabuTournament()throws Exception {super(BossID.FAT_MABU_TOURNAMENT,configuration());}
    public static BossData configuration(){
        return new BossData("Mabu mập",ConstPlayer.XAYDA,new short[]{297,298,299,-1,-1,-1},1,
                new int[]{300_000_000},new int[]{52},
                new int[][]{{Skill.KAMEJOKO,7,10_000},{Skill.DICH_CHUYEN_TUC_THOI,7,20_000},
                    {Skill.GALICK,7,1000},{Skill.THAI_DUONG_HA_SAN,7,50_000},{Skill.KHIEN_NANG_LUONG,7,100_000}},
                new String[]{},new String[]{},new String[]{},600);
    }
    public synchronized void prepare(Zone destination){
        respawn();nPoint.hpMax=300_000_000;nPoint.hp=300_000_000;nPoint.dame=1;nPoint.dameg=1;nPoint.crit=0;
        int x=Math.max(0,Math.min(destination.map.mapWidth-1,destination.map.mapWidth*3/4));
        int y=destination.map.yPhysicInTop(x,100);
        if(y<0)throw new IllegalStateException("Mabu terrain unavailable");
        if(zone==null)zone=destination;
        ChangeMapService.gI().changeMap(this,destination,x,y);
        if(zone!=destination||!destination.getBosses().contains(this))throw new IllegalStateException("Mabu admission rejected");
        changeStatus(BossStatus.CHAT_S);
    }
    public synchronized void activate(){
        if(finished||zone==null||isDie())return;
        PlayerService.gI().changeAndSendTypePK(this,ConstPlayer.PK_ALL);changeStatus(BossStatus.ACTIVE);
    }
    public boolean combatReady(){return !finished&&!beforeDispose&&bossStatus==BossStatus.ACTIVE&&zone!=null&&nPoint!=null&&nPoint.hp>0;}
    public long damageFor(Player target,long original){
        if(original<=0||!combatReady()||target==null||target==this||target.isBoss||target.beforeDispose
                ||target.nPoint==null||target.isDie()||target.zone!=zone)return 0;
        return target.nPoint.hpMax<=0?0:Math.max(1,(long)target.nPoint.hpMax*10/100);
    }
    @Override public synchronized void update(){
        if(!combatReady())return;
        effectSkill.update();nPoint.mp=nPoint.mpg;
        if(effectSkill.isHaveEffectSkill())return;
        // Cast the self skill explicitly, so a distant target cannot prevent the shield.
        if(!effectSkill.isShielding){
            Skill shield=playerSkill.skills.get(4);playerSkill.skillSelect=shield;
            if(SkillService.gI().canUseSkillWithCooldown(this)){
                SkillService.gI().useSkill(this,null,null,-1,null);
            }
        }
        attack();
    }
    @Override public synchronized void attack(){
        if(!combatReady()||!Util.canDoWithTime(lastTimeAttack,100)||typePk!=ConstPlayer.PK_ALL)return;
        lastTimeAttack=System.currentTimeMillis();Player target=getPlayerAttack();
        if(target==null||target.isDie()||target.zone!=zone)return;
        playerSkill.skillSelect=playerSkill.skills.get(Util.nextInt(0,3));
        if(Util.getDistance(this,target)<=getRangeCanAttackWithSkillSelect())SkillService.gI().useSkill(this,target,null,-1,null);
        else moveToPlayer(target);
    }
    @Override public synchronized int injured(Player attacker,long damage,boolean piercing,boolean mobAttack){
        if(damage<=0||!combatReady()||attacker==null||attacker.beforeDispose||attacker.zone!=zone)return 0;
        if(!piercing&&Util.isTrue(nPoint.tlNeDon,1000))return 0;
        if(!piercing&&!mobAttack&&effectSkill.isShielding){
            if(damage>nPoint.hpMax)EffectSkillService.gI().breakShield(this);
            damage=1;
        }
        int applied=(int)Math.min(damage,nPoint.hp);nPoint.subHP(applied);
        if(isDie()){setDie(attacker);die(attacker);}return applied;
    }
    @Override public synchronized void die(Player killer){
        if(finished||nPoint==null||nPoint.hp>0)return;
        finished=true;
        try{BossNoticeService.gI().defeated(this,killer);reward(killer);}
        finally{bossStatus=BossStatus.DIE;}
    }
    @Override public void reward(Player killer){
        // Only die holds this lock and grants rewards; external reward calls cannot mint loot.
        if(!Thread.holdsLock(this)||!finished||nPoint==null||nPoint.hp>0)return;
        rare.drop(this,killer,EGG_RATE,BOTTLE_RATE);equipment.drop(this,killer);
    }
    @Override public void joinMap(){}
    @Override public void rest(){}
    @Override public void autoLeaveMap(){}
    @Override protected void notifyJoinMap(){}
    @Override public synchronized void leaveMap(){
        finished=true;BossNoticeService.gI().departed(this);
        if(zone!=null)ChangeMapService.gI().exitMap(this);bossStatus=BossStatus.REST;
    }
}
