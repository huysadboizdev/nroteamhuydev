package nro.models.services;

import java.util.*;
import nro.models.player.*;
import nro.models.skill.*;
import nro.models.player_system.Template;
import static nro.models.services.PinkFixture.*;

/** Uses real EffectSkill.update and real cooldown release, with network/SQL omitted. */
public final class IntrinsicCooldownLifecycleTest {
    static Skill skill(int id,long used) {
        Skill s=new Skill(); s.template=new Template.SkillTemplate(); s.template.id=(byte)id;
        s.skillId=(short)id; s.coolDown=10000; s.lastTimeUseThisSkill=used; return s;
    }
    public static void main(String[] args)throws Exception {
        for(int i=0;i<3;i++) nro.models.server.Manager.NCLASS.add(new NClass());
        List<Skill> released=new ArrayList<>();
        var releasing=new java.util.concurrent.CountDownLatch(1);
        var continueRelease=new java.util.concurrent.CountDownLatch(1);
        replace(Service.class,"instance",new Service(){
            @Override public void releaseCooldownSkill(Player p,Skill s){
                if(args[0].equals("interleave")) {
                    releasing.countDown();
                    try { check(continueRelease.await(3,java.util.concurrent.TimeUnit.SECONDS),"release latch timeout"); }
                    catch(InterruptedException e){throw new RuntimeException(e);}
                }
                released.add(s);super.releaseCooldownSkill(p,s);
            }
        });
        replace(PlayerService.class,"i",new PlayerService(){@Override public void sendInfoHpMpMoney(Player p){}});
        Player p=player(990); p.nPoint=blank(NPoint.class); p.nPoint.mpMax=500;
        p.playerSkill=new PlayerSkill(p); p.effectSkill=new EffectSkill(p);
        long now=System.currentTimeMillis();
        Skill first=skill(Skill.MASENKO,now-1),second=skill(Skill.MAKANKOSAPPO,now-1);
        p.playerSkill.skills.add(first);p.playerSkill.skills.add(second);p.playerSkill.skillSelect=first;
        EffectSkillService effects=blank(EffectSkillService.class);
        replace(EffectSkillService.class,"instance",effects);
        effects.setIntrinsic(p,first.template.id,10000,now-20000);
        switch(args[0]) {
            case "interleave":
                p.playerIntrinsic=blank(nro.models.intrinsic.IntrinsicPlayer.class);
                p.playerIntrinsic.intrinsic=new nro.models.intrinsic.Intrinsic();
                p.setClothes=blank(SetClothes.class);
                var method=SkillService.class.getDeclaredMethod("setLastTimeUseSkill",Player.class,int.class);
                method.setAccessible(true);
                var castDone=new java.util.concurrent.CountDownLatch(1);
                var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
                Thread updater=new Thread(()->{try{p.effectSkill.update();}catch(Throwable t){failure.set(t);}});
                Thread caster=new Thread(()->{try{method.invoke(new SkillService(),p,(int)first.template.id);}catch(Throwable t){failure.set(t);}finally{castDone.countDown();}});
                updater.start();check(releasing.await(3,java.util.concurrent.TimeUnit.SECONDS),"timer never released");
                caster.start();castDone.await(150,java.util.concurrent.TimeUnit.MILLISECONDS);
                continueRelease.countDown();updater.join(3000);caster.join(3000);
                check(!updater.isAlive()&&!caster.isAlive()&&failure.get()==null,"concurrent cooldown paths must finish without deadlock: "+failure.get());
                check(first.lastTimeUseThisSkill>System.currentTimeMillis()-1000,"intrinsic release must not erase a concurrently committed cast");
                break;
            case "overwrite":
                effects.setIntrinsic(p,second.template.id,10000,now-20000);
                p.effectSkill.update();
                check(released.size()==2&&released.contains(first)&&released.contains(second),"both intrinsic cooldowns must finish; second skill must not overwrite first");
                check(p.nPoint.mp==p.nPoint.mpMax,"preserve existing intrinsic mana refill");
                break;
            case "future":
                effects.setIntrinsic(p,second.template.id,10000,now+60000);
                p.effectSkill.update();
                check(released.equals(List.of(first)),"future second skill must not delay first cooldown");
                check(p.effectSkill.isIntrinsic,"future cooldown must remain pending");
                break;
            case "stale":
                first.lastTimeUseThisSkill=now+1;
                p.effectSkill.update();
                check(released.isEmpty(),"old intrinsic timer must not clear a newer cast");
                break;
            case "replace":
                p.playerSkill.skills.set(0,skill(first.template.id,now));
                p.effectSkill.update();
                check(released.isEmpty(),"timer must not reset a replacement skill");
                break;
            case "same-skill":
                first.lastTimeUseThisSkill=now+1;
                effects.setIntrinsic(p,first.template.id,10000,now+60000);
                p.effectSkill.update();
                check(released.isEmpty()&&p.effectSkill.isIntrinsic,"recasting same skill must replace its previous timer");
                break;
            case "duplicate":
                p.effectSkill.update();p.effectSkill.update();
                check(released.equals(List.of(first)),"completed intrinsic cooldown must release once");
                break;
            case "dispose":
                p.effectSkill.dispose();
                effects.releaseCooldownSkillByIntrinsic(p);
                check(released.isEmpty()&&!p.effectSkill.isIntrinsic,"dispose must discard pending cooldowns");
                break;
            default: throw new AssertionError(args[0]);
        }
        System.out.println("PASS intrinsic "+args[0]);
    }
}
