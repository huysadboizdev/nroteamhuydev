package nro.models.boss.HuntingTrio;

import java.util.concurrent.atomic.AtomicBoolean;
import nro.models.boss.Boss;
import nro.models.boss.Boss_mini.MiniBossIdentity;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstPlayer;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Player;
import nro.models.services.BossNoticeService;
import nro.models.services.PlayerService;
import nro.models.skill.Skill;

public final class HuntingTrioBoss extends Boss {
    private final HuntingTrioLifecycle lifecycle;
    private final int member;
    private final AtomicBoolean finished=new AtomicBoolean();
    private final HuntingTrioLoot loot=new HuntingTrioLoot();
    public HuntingTrioBoss(int group,int member,HuntingTrioLifecycle lifecycle)throws Exception {
        super(MiniBossIdentity.nextId(),HuntingTrioCatalog.data(group,member));
        this.member=member;this.lifecycle=lifecycle;
    }
    public boolean combatReady() {
        return lifecycle!=null&&lifecycle.phase()==HuntingTrioLifecycle.Phase.ACTIVE
                &&!finished.get()&&bossStatus==BossStatus.ACTIVE&&zone!=null&&nPoint!=null&&nPoint.hp>0;
    }
    public void prepare(Zone destination,int x) {
        respawn();nPoint.hpMax=1000;nPoint.hp=1000;nPoint.dame=1;nPoint.dameg=1;nPoint.crit=0;
        nro.models.services.BossAdminConfig.applyAfterNativeSpawn(this);
        playerSkill.skills.removeIf(s->s.template.id==Skill.TAI_TAO_NANG_LUONG);
        int safeX=Math.max(0,Math.min(destination.map.mapWidth-1,x));
        int y=destination.map.yPhysicInTop(safeX,100);
        // Initial boss admission follows Boss.joinMap: changeMap requires a source zone.
        if(zone==null)zone=destination;
        ChangeMapService.gI().changeMap(this,destination,safeX,y);
        if(zone!=destination||!destination.getBosses().contains(this))throw new IllegalStateException("Hunting boss did not join its reserved zone");
        changeStatus(BossStatus.CHAT_S);
    }
    public void activateTogether() {
        PlayerService.gI().changeAndSendTypePK(this,ConstPlayer.PK_ALL);
        changeStatus(BossStatus.ACTIVE);
    }
    @Override public void update() {
        if(!combatReady())return;
        nro.models.services.BossAdminConfig.applyAtTick(this);
        // Only skill-effect expiry is needed. Never run regeneration or an independent spawn state machine.
        effectSkill.update();
        nPoint.mp=nPoint.mpg;
        if(effectSkill.isHaveEffectSkill())return;
        attack();
    }
    @Override public void joinMap(){} // Group owns admission and all respawns.
    @Override public void rest(){}
    @Override public void autoLeaveMap(){}
    @Override protected void notifyJoinMap(){} // Service announces each group once.
    @Override public synchronized int injured(Player attacker,long damage,boolean piercing,boolean mobAttack) {
        if(damage<=0||!combatReady()||attacker==null||attacker.beforeDispose||attacker.zone!=zone)return 0;
        nPoint.subHP(1);
        if(nPoint.hp<=0) { setDie(attacker);die(attacker); }
        return 1;
    }
    @Override public synchronized void die(Player killer) {
        if(!finished.compareAndSet(false,true))return;
        try {
            BossNoticeService.gI().defeated(this,killer);
            if(nPoint.hp<=0)reward(killer);
        } finally { bossStatus=BossStatus.DIE;lifecycle.defeated(member); }
    }
    @Override protected boolean rewardAllowed(Player killer){ return Thread.holdsLock(this) && finished.get() && nPoint != null && nPoint.hp <= 0; }
    @Override public void rewardDefault(Player killer){loot.drop(this,killer);}
    @Override public void leaveMap() {
        finished.set(true);
        if(zone!=null)ChangeMapService.gI().exitMap(this);
        bossStatus=BossStatus.REST;
    }
}
