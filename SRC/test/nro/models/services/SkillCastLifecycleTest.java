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
        @Override protected void scheduleBomb(Runnable release) { queued.add(release); }
        @Override public void affterUseSkill(Player p,int skillId) { completions++; }
        @Override public boolean canAttackPlayer(Player a,Player b) { return true; }
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
