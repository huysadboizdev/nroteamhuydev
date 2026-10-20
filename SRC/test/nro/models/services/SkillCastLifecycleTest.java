package nro.models.services;

import nro.models.player.*;
import nro.models.skill.*;
import nro.models.player_system.Template;
import static nro.models.services.PinkFixture.*;

/** Exercises real skill services without production SQL or network sessions. */
public final class SkillCastLifecycleTest {
    private static final class CapturedSkills extends SkillService {
        final java.util.List<Runnable> queued = new java.util.ArrayList<>();
        int completions;
        int acceptedAttacks;
        boolean requirePlayerLock;
        boolean enteredWithoutPlayerLock;
        @Override protected void scheduleBomb(Runnable release) { queued.add(release); }
        @Override public void sendPlayerPrepareSkill(Player p,int durationMs) {}
        @Override public void sendPlayerPrepareBom(Player p,int durationMs) {}
        @Override public void affterUseSkill(Player p,int skillId) { completions++; }
        @Override public boolean canAttackPlayer(Player a,Player b) { return true; }
        @Override public boolean canUseSkillWithMana(Player p) { return true; }
        @Override public boolean canUseSkillWithCooldown(Player p) {
            if (requirePlayerLock && !Thread.holdsLock(p)) enteredWithoutPlayerLock=true;
            return true;
        }
        @Override public void useSkillAttack(Player p,Player target,nro.models.mob.Mob mob) {
            int skillId=p.playerSkill.skillSelect.template.id;
            if (skillId==Skill.QUA_CAU_KENH_KHI || skillId==Skill.MAKANKOSAPPO) {
                super.useSkillAttack(p,target,mob);
                return;
            }
            if (requirePlayerLock) {
                acceptedAttacks++;
                affterUseSkill(p,p.playerSkill.skillSelect.template.id);
            } else {
                super.useSkillAttack(p,target,mob);
            }
        }
    }
    private static final class DamageTarget extends Player {
        long received;
        @Override public int injured(Player p,long damage,boolean piercing,boolean mobAttack) {
            received=damage; return (int)damage;
        }
    }
    private static final class MovingMob extends nro.models.mob.Mob {
        nro.models.map.Zone destination;
        @Override public void injured(Player p,long damage,boolean dieWhenHpFull) { p.zone=destination; }
    }
    private static Skill skill(int id, int point) {
        Skill s = new Skill(); s.point = point;
        s.template = new Template.SkillTemplate(); s.template.id = (byte) id;
        return s;
    }
    private static Player caster(int id) throws Exception {
        Player p = player(id); p.nPoint = blank(NPoint.class);
        p.nPoint.hp = 100; p.nPoint.hpMax = 10000;
        p.location = blank(Location.class);
        p.effectSkill = blank(EffectSkill.class); p.setClothes = blank(SetClothes.class);
        p.effectSkin = blank(EffectSkin.class);
        p.playerSkill = new PlayerSkill(p); return p;
    }
    public static void main(String[] args) throws Exception {
        for (int i=0;i<3;i++) nro.models.server.Manager.NCLASS.add(blank(NClass.class));
        replace(Service.class,"instance",new Service() {
            @Override public void Send_Caitrang(Player p) {}
            @Override public void Send_Info_NV(Player p) {}
            @Override public void sendInfoPlayerEatPea(Player p) {}
            @Override public void point(Player p) {}
            @Override public void sendSpeedPlayer(Player p,int speed) {}
            @Override public void sendMessAllPlayerInMap(Player p,nro.models.network.Message m) {}
        });
        replace(PlayerService.class,"i",new PlayerService() {
            @Override public void sendInfoHpMp(Player p) {}
            @Override public void sendInfoHpMpMoney(Player p) {}
        });
        EffectSkillService effects = blank(EffectSkillService.class);
        replace(EffectSkillService.class,"instance",effects);
        CapturedSkills skills = new CapturedSkills();
        Player p = caster(901);
        if (args[0].equals("early-bomb")) {
            p.playerSkill.skillSelect=skill(Skill.TU_SAT,7);
            p.playerSkill.prepareTuSat=true;
            p.playerSkill.lastTimePrepareTuSat=System.currentTimeMillis();
            skills.useSkillAlone(p);
            check(p.playerSkill.prepareTuSat,"early bomb must retain the pending cast");
            check(p.playerSkill.skillSelect.lastTimeUseThisSkill==0,"early bomb must not consume cooldown");
        } else if (args[0].endsWith("ball") || args[0].endsWith("laser")) {
            p.nPoint.stamina=100; p.charms=blank(Charms.class);
            boolean ball=args[0].endsWith("ball");
            p.playerSkill.skillSelect=skill(ball?Skill.QUA_CAU_KENH_KHI:Skill.MAKANKOSAPPO,7);
            p.playerSkill.prepareQCKK=ball; p.playerSkill.prepareLaze=!ball;
            p.playerSkill.lastTimePrepareQCKK=p.playerSkill.lastTimePrepareLaze=System.currentTimeMillis();
            boolean early=args[0].startsWith("early-");
            if (!early) {
                p.playerSkill.lastTimePrepareQCKK-=4000;
                p.playerSkill.lastTimePrepareLaze-=3000;
            }
            skills.useSkillAttack(p,null,null);
            check((ball?p.playerSkill.prepareQCKK:p.playerSkill.prepareLaze)==early,"preparation must release only after the deadline");
            check(skills.completions==(early?0:1),"only a valid release may consume cooldown");
        } else if (args[0].equals("bomb-map-interleave")) {
            p.isPet=true; p.zone=zone(); p.location=blank(Location.class);
            var origin=p.zone; var destination=zone();
            MovingMob mob=blank(MovingMob.class);mob.location=blank(Location.class);mob.destination=destination;
            mob.zone=origin;
            mob.point=blank(nro.models.mob.MobPoint.class);mob.point.hp=1000;
            origin.mobs.add(mob);
            DamageTarget target=blank(DamageTarget.class);target.zone=destination;target.location=blank(Location.class);
            target.nPoint=blank(NPoint.class);target.nPoint.hp=target.nPoint.hpMax=10000;
            destination.addPlayer(target);
            p.playerSkill.skillSelect=skill(Skill.TU_SAT,7);p.playerSkill.prepareTuSat=true;
            p.playerSkill.lastTimePrepareTuSat=System.currentTimeMillis();
            skills.useSkillAlone(p);skills.queued.get(0).run();
            check(p.zone==destination,"fixture must transfer during mob hit");
            check(target.received==0,"a bomb committed in the old zone must not hit destination-zone players");
        } else if (args[0].equals("bomb-target-order")) {
            p.isPet=true; p.zone=zone(); p.location=blank(Location.class);
            p.playerSkill.skillSelect=skill(Skill.TU_SAT,7);
            p.playerSkill.prepareTuSat=true;
            p.playerSkill.lastTimePrepareTuSat=System.currentTimeMillis();
            DamageTarget[] targets=new DamageTarget[3];
            for (int i=0;i<targets.length;i++) {
                DamageTarget t=blank(DamageTarget.class); t.id=1000+i; t.isBoss=i<2;
                t.zone=p.zone; t.location=blank(Location.class); t.nPoint=blank(NPoint.class);
                t.nPoint.hp=t.nPoint.hpMax=10000; p.zone.addPlayer(t); targets[i]=t;
            }
            skills.useSkillAlone(p); skills.queued.get(0).run();
            check(targets[0].received==50&&targets[1].received==50&&targets[2].received==100,
                    "boss reductions must not reduce damage to later targets");
        } else if (args[0].startsWith("delayed-")) {
            p.isBoss=true; p.zone=zone();
            p.playerSkill.skillSelect=skill(Skill.TU_SAT,7);
            p.playerSkill.prepareTuSat=true;
            p.playerSkill.lastTimePrepareTuSat=System.currentTimeMillis();
            skills.useSkillAlone(p);
            skills.useSkillAlone(p);
            check(skills.queued.size()==1,"duplicate bomb must not schedule two explosions");
            if (args[0].equals("delayed-map")) p.zone=zone();
            if (args[0].equals("delayed-death")) p.nPoint.hp=0;
            if (args[0].equals("delayed-skill")) p.playerSkill.skillSelect=skill(Skill.GALICK,1);
            if (args[0].equals("delayed-cancel")) p.playerSkill.prepareTuSat=false;
            if (args[0].equals("delayed-dispose")) p.playerSkill=null;
            if (args[0].equals("delayed-recast")) p.playerSkill.lastTimePrepareTuSat++;
            var state=p.playerSkill;
            skills.queued.get(0).run();
            check(skills.completions==(args[0].equals("delayed-valid")?1:0),"invalid bomb must not deal damage or consume cooldown");
            if (state!=null) check(!state.delayedBombPending,"callback must clear pending ownership");
            if (args[0].equals("delayed-recast")) check(state.prepareTuSat,"stale callback must not clear a newer cast");
            skills.queued.get(0).run();
            check(skills.completions==(args[0].equals("delayed-valid")?1:0),"release callback must be idempotent");
        } else if (args[0].equals("map-clear")) {
            p.playerSkill.prepareTuSat=p.playerSkill.prepareQCKK=p.playerSkill.prepareLaze=true;
            nro.models.map.service.ChangeMapService.gI().exitMap(p);
            check(!p.playerSkill.prepareTuSat&&!p.playerSkill.prepareQCKK&&!p.playerSkill.prepareLaze,"map exit must invalidate old preparations");
        } else if (args[0].equals("select-cancel")) {
            p.playerSkill.skillSelect=skill(Skill.TU_SAT,7);
            p.playerSkill.skills.add(skill(Skill.GALICK,1));
            p.playerSkill.prepareTuSat=p.playerSkill.prepareQCKK=p.playerSkill.prepareLaze=true;
            skills.selectSkill(p,Skill.GALICK);
            check(!p.playerSkill.prepareTuSat&&!p.playerSkill.prepareQCKK&&!p.playerSkill.prepareLaze,"selecting another skill must invalidate old preparations");
        } else if (args[0].equals("serialized-use")) {
            p.playerSkill.skillSelect=skill(Skill.DRAGON,1);
            p.playerSkill.skillSelect.template.type=1;
            skills.requirePlayerLock=true;
            check(skills.useSkill(p,null,null,-1,null),"valid player skill must be handled");
            check(skills.acceptedAttacks==1,"one request must execute one attack");
            check(!skills.enteredWithoutPlayerLock,"player skill validation and damage must share a per-player lock");
        } else if (args[0].startsWith("boss-cast-")) {
            p.zone=zone(); p.location=blank(Location.class); p.location.x=100; p.location.y=100;
            p.playerSkill.skillSelect=skill(Skill.DRAGON,1); p.playerSkill.skillSelect.template.type=1;
            DamageTarget boss=new DamageTarget(); boss.isBoss=true; boss.zone=p.zone; boss.location=blank(Location.class);
            boss.location.x=100+(args[0].equals("boss-cast-range")?101:70); boss.location.y=100;
            skills.requirePlayerLock=true;
            boolean accepted=skills.useSkill(p,boss,null,-1,null);
            if (args[0].equals("boss-cast-range")) {
                check(!accepted,"out-of-range boss attack must be rejected before spending cooldown");
                check(skills.acceptedAttacks==0,"out-of-range boss attack must not execute with zero damage");
            } else {
                check(accepted,"in-range boss attack must still execute");
                check(skills.acceptedAttacks==1,"in-range boss attack must execute exactly once");
            }
        } else if (args[0].startsWith("mob-cast-")) {
            p.nPoint.stamina=100;
            p.zone=zone();
            p.location.x=100; p.location.y=100;
            Skill dragon=skill(Skill.DRAGON,1); dragon.skillId=1; dragon.template.type=1;
            Skill galick=skill(Skill.GALICK,1); galick.skillId=2; galick.template.type=1;
            p.playerSkill.skills.add(dragon); p.playerSkill.skills.add(galick);
            p.playerSkill.skillSelect=dragon;
            nro.models.mob.Mob target=new nro.models.mob.Mob();
            target.zone=p.zone; target.point.hp=target.point.maxHp=1000;
            target.location.x=100+(args[0].equals("mob-cast-range")?101:70); target.location.y=100;
            skills.requirePlayerLock=true;
            SkillService.MobCastResult result=skills.useSkillForMob(p,target,Skill.GALICK);
            if (args[0].equals("mob-cast-range")) {
                check(result==SkillService.MobCastResult.OUT_OF_RANGE,"out-of-range mob cast must be reported, not dealt as zero damage");
                check(skills.acceptedAttacks==0,"out-of-range cast must not execute or consume cooldown");
            } else {
                check(result==SkillService.MobCastResult.ACCEPTED,"in-range mob cast must execute");
                check(p.playerSkill.skillSelect==galick,"requested owned skill must be selected atomically with the cast");
                check(skills.acceptedAttacks==1,"accepted mob cast must execute exactly once");
                check(!skills.enteredWithoutPlayerLock,"mob skill selection, validation and execution must share a player lock");
            }
        } else if (args[0].equals("mob-cast-self-destruct-prepare")) {
            p.nPoint.stamina=100; p.zone=zone(); p.location.x=100; p.location.y=100;
            Skill bomb=skill(Skill.TU_SAT,7); bomb.template.type=3;
            p.playerSkill.skills.add(bomb); p.playerSkill.skillSelect=bomb;
            nro.models.mob.Mob target=new nro.models.mob.Mob(); target.zone=p.zone;
            target.point.hp=target.point.maxHp=1000; target.location.x=100; target.location.y=100;
            check(skills.useSkillForMob(p,target,Skill.TU_SAT)==SkillService.MobCastResult.PREPARING,
                    "offensive type-3 self-destruct must enter preparation instead of being rejected");
            check(p.playerSkill.prepareTuSat,"accepted self-destruct prepare must be retained on the server");
            check(skills.useSelfSkillForMob(p,7,Skill.TU_SAT)==SkillService.MobCastResult.PREPARING,
                    "confirmed self-centered cast must preserve its preparation result");
        } else if (args[0].equals("mob-cast-charge-prepare")) {
            p.nPoint.stamina=100; p.zone=zone(); p.location.x=100; p.location.y=100;
            Skill ball=skill(Skill.QUA_CAU_KENH_KHI,7); ball.template.type=1;
            p.playerSkill.skills.add(ball); p.playerSkill.skillSelect=ball;
            nro.models.mob.Mob target=new nro.models.mob.Mob(); target.zone=p.zone;
            target.point.hp=target.point.maxHp=1000; target.location.x=100; target.location.y=100;
            check(skills.useSkillForMob(p,target,Skill.QUA_CAU_KENH_KHI)==SkillService.MobCastResult.PREPARING,
                    "charged mob skill must report preparation rather than a completed attack");
            check(p.playerSkill.prepareQCKK,"server must retain charged skill preparation");
            check(skills.useSkillForMob(p,target,Skill.QUA_CAU_KENH_KHI)==SkillService.MobCastResult.PREPARING,
                    "early duplicate during charge must remain retryable preparation");
            check(skills.acceptedAttacks==0,"charge start must not be counted as a completed hit");
        } else if (args[0].equals("mob-retry-expired")) {
            Skill selected=skill(Skill.GALICK,1);
            selected.coolDown=500;
            selected.lastTimeUseThisSkill=System.currentTimeMillis()-1000;
            p.playerSkill.skillSelect=selected;
            check(skills.getMobCastRetryAfterMs(p,Skill.GALICK)==0,
                    "accepted cast whose cooldown expired before ACK must not add a retry delay");
        } else if (args[0].equals("mob-retry-active")) {
            Skill selected=skill(Skill.GALICK,1);
            selected.coolDown=1000;
            selected.lastTimeUseThisSkill=System.currentTimeMillis()-250;
            p.playerSkill.skillSelect=selected;
            int remaining=skills.getMobCastRetryAfterMs(p,Skill.GALICK);
            check(remaining>0&&remaining<=750,
                    "ACK must report only the server cooldown still remaining");
        } else {
            p.playerSkill.skillSelect=skill(Skill.BIEN_KHI,7);
            p.playerSkill.skills.add(p.playerSkill.skillSelect);
            effects.startUseSkillMonkey(p);
            if (args[0].equals("monkey-level") || args[0].equals("monkey-restart")) {
                p.playerSkill.skillSelect=skill(Skill.GALICK,1);
                if (args[0].equals("monkey-restart")) effects.startUseSkillMonkey(p);
                effects.finishUseMonkey(p);
                check(p.effectSkill.levelMonkey==7,"monkey must keep the level selected at cast start");
            } else {
                effects.finishUseMonkey(p);
                int hp=p.nPoint.hp;
                effects.finishUseMonkey(p);
                check(p.nPoint.hp==hp,"duplicate monkey completion must not double HP again");
            }
        }
        System.out.println("PASS "+args[0]);
    }
}
