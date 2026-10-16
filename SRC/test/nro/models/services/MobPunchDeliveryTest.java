package nro.models.services;

import java.nio.ByteBuffer;

import nro.models.mob.Mob;
import nro.models.player.*;
import nro.models.skill.*;
import nro.models.player_system.Template;
import nro.models.network.Message;
import static nro.models.services.PinkFixture.*;

/** Runs the actual punch validation, damage, mob HP packet and EXP dispatch without SQL. */
public final class MobPunchDeliveryTest {
    private static int hitPackets, lastMobDamage, hitDamage = 100, expAwards, finished, resourceSyncs;
    private static long exp;
    public static final class Points extends NPoint {
        public Points(Player p) { super(p); }
        @Override public int getDameAttack(boolean mob) { return hitDamage; }
        @Override public short getTileHutHp(boolean mob) { return 0; }
        @Override public short getTiLeHutMp() { return 0; }
    }
    public static final class Target extends Mob {
        @Override public long getTiemNangForPlayer(Player p,long damage) { return damage; }
    }
    private static final class Skills extends SkillService {
        @Override public void affterUseSkill(Player p,int id) {
            finished++;
            p.playerSkill.skillSelect.lastTimeUseThisSkill=System.currentTimeMillis();
        }
    }
    private static Skill skill(int id,int cooldown) {
        Skill s=new Skill();s.template=new Template.SkillTemplate();s.template.id=(byte)id;
        s.template.type=1;s.template.manaUseType=0;s.skillId=(short)id;s.coolDown=cooldown;s.manaUse=1;
        return s;
    }
    public static void main(String[] args)throws Exception {
        replace(Service.class,"instance",new Service() {
            @Override public void sendMessAllPlayerInMap(Player p,Message m) {}
            @Override public void sendMessAllPlayerInMap(nro.models.map.Zone z,Message m) {
                if(m.command==-9) { hitPackets++; lastMobDamage=ByteBuffer.wrap(m.getData()).getInt(5); }
            }
            @Override public void addSMTN(Player p,byte type,long amount,boolean ori) {expAwards++;exp+=amount;}
            @Override public void updatePlayerTotalDamage(Player p) {}
        });
        replace(PlayerService.class,"i",new PlayerService() {
            @Override public void sendInfoHpMp(Player p) {resourceSyncs++;}
            @Override public void sendCurrentStamina(Player p) {resourceSyncs++;}
        });
        replace(nro.models.services_dungeon.TrainingService.class,"instance",new nro.models.services_dungeon.TrainingService() {
            @Override public void tangTnsmLuyenTap(Player p,long amount) {}
        });
        Player p=player(123);p.name="fixture";p.nPoint=new Points(p);p.nPoint.hp=p.nPoint.hpMax=1000;
        p.nPoint.mp=p.nPoint.mpMax=1000;p.nPoint.stamina=100;
        p.playerSkill=blank(PlayerSkill.class);p.playerSkill.skills=new java.util.ArrayList<>();
        p.effectSkill=blank(EffectSkill.class);p.effectSkin=blank(EffectSkin.class);p.charms=blank(Charms.class);
        p.location=new Location();p.location.x=100;p.location.y=100;p.zone=zone();
        Target mob=new Target();mob.id=1;mob.tempId=0;mob.zone=p.zone;mob.point.hp=mob.point.maxHp=100000;
        mob.location.x=130;mob.location.y=100;p.zone.mobs.add(mob);
        Skills skills=new Skills();
        for(int id:new int[]{Skill.DRAGON,Skill.DEMON,Skill.GALICK}) {
            Skill s=skill(id,id==Skill.DEMON?400:500);p.playerSkill.skills.add(s);p.playerSkill.skillSelect=s;
            for(int n=0;n<30;n++) {
                s.lastTimeUseThisSkill=System.currentTimeMillis()-s.coolDown;
                int hp=mob.point.hp,packets=hitPackets,awards=expAwards;
                check(skills.useSkillForMob(p,mob,id)==SkillService.MobCastResult.ACCEPTED,"ready punch must be accepted id="+id);
                check(mob.point.hp==hp-100,"accepted punch must remove HP id="+id);
                check(hitPackets==packets+1&&expAwards==awards+1,"each accepted punch sends damage and EXP id="+id);
                check(skills.useSkillForMob(p,mob,id)==SkillService.MobCastResult.COOLDOWN,"early punch must report cooldown");
                check(mob.point.hp==hp-100&&hitPackets==packets+1,"early duplicate cannot deal damage twice");
            }
        }
        check(finished==90&&expAwards==90&&exp==9000,"90 accepted punches must deliver 90 hits and EXP awards");
        Skill jitterSkill=p.playerSkill.skillSelect;
        long jitterNow=System.currentTimeMillis();
        jitterSkill.lastTimeUseThisSkill=jitterNow-(jitterSkill.coolDown-75L);
        int jitterHp=mob.point.hp,jitterPackets=hitPackets;
        check(skills.useSkillForMob(p,mob,jitterSkill.template.id)==SkillService.MobCastResult.ACCEPTED,
                "melee hit arriving 25ms inside the client/server timing window must tolerate packet jitter");
        check(mob.point.hp==jitterHp-100&&hitPackets==jitterPackets+1,
                "jitter-tolerant melee hit must still remove HP and emit the mob damage packet");
        check(skills.useSkillForMob(p,mob,jitterSkill.template.id)==SkillService.MobCastResult.COOLDOWN,
                "jitter tolerance must not allow a duplicate attack immediately after an accepted hit");
        Target lowHpTrainingMob=new Target();lowHpTrainingMob.id=2;lowHpTrainingMob.tempId=nro.models.consts.ConstMob.MOC_NHAN;
        lowHpTrainingMob.zone=p.zone;lowHpTrainingMob.point.hp=lowHpTrainingMob.point.maxHp=2;
        lowHpTrainingMob.location.x=130;lowHpTrainingMob.location.y=100;p.zone.mobs.add(lowHpTrainingMob);
        hitDamage=1;Skill active=p.playerSkill.skillSelect;active.lastTimeUseThisSkill=0;
        int hitBefore=hitPackets;
        check(skills.useSkillForMob(p,lowHpTrainingMob,active.template.id)==SkillService.MobCastResult.ACCEPTED,
                "valid one-damage hit against a low-HP training mob must be accepted");
        check(lowHpTrainingMob.point.hp==1&&hitPackets==hitBefore+1&&lastMobDamage==1,
                "one-damage hit must not round down to zero at the 10-percent training-mob cap");
        Target lowHpLevelMob=new Target();lowHpLevelMob.id=3;lowHpLevelMob.tempId=1;lowHpLevelMob.lvMob=1;
        lowHpLevelMob.zone=p.zone;lowHpLevelMob.point.hp=lowHpLevelMob.point.maxHp=2;
        lowHpLevelMob.location.x=130;lowHpLevelMob.location.y=100;p.zone.mobs.add(lowHpLevelMob);
        active.lastTimeUseThisSkill=0;hitBefore=hitPackets;
        check(skills.useSkillForMob(p,lowHpLevelMob,active.template.id)==SkillService.MobCastResult.ACCEPTED,
                "valid one-damage hit against a low-HP level mob must be accepted");
        check(lowHpLevelMob.point.hp==1&&hitPackets==hitBefore+1&&lastMobDamage==1,
                "one-damage hit must not round down to zero at the level-mob 10-percent calculation");
        hitDamage=100;
        active=p.playerSkill.skillSelect;active.lastTimeUseThisSkill=0;mob.location.x=201;
        check(skills.useSkillForMob(p,mob,active.template.id)==SkillService.MobCastResult.OUT_OF_RANGE,"101px must request repositioning");
        mob.location.x=130;active.lastTimeUseThisSkill=0;
        p.nPoint.stamina=0;
        check(skills.useSkillForMob(p,mob,active.template.id)==SkillService.MobCastResult.REJECTED,"exhausted player must not be falsely accepted");
        p.nPoint.stamina=100;p.nPoint.mp=0;
        check(skills.useSkillForMob(p,mob,active.template.id)==SkillService.MobCastResult.REJECTED,"no MP must be a nonretryable rejection");
        p.nPoint.mp=1000;active.lastTimeUseThisSkill=System.currentTimeMillis();
        p.playerSkill.skillSelect=p.playerSkill.skills.get(0);
        check(skills.getMobCastRetryAfterMs(p,active.template.id)>0,"switching selected skill must preserve the requested skill's cooldown result");
        replace(SkillService.class,"instance",skills);
        active.lastTimeUseThisSkill=0;mob.location.x=201;
        Service.gI().attackMobConfirmed(p,mob.id,1,active.template.id);
        check(resourceSyncs==2,"rejected visible punch must restore resources from authoritative server state");
        System.out.println("PASS MobPunchDeliveryTest: 90 standard hits; low-HP one-damage packet; early duplicates; range; exhaustion; mana; selection switch; resource sync");
    }
}
