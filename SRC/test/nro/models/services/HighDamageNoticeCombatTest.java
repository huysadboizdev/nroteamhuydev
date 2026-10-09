package nro.models.services;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.*;
import nro.models.player.*;
import nro.models.skill.*;
import nro.models.mob.*;
import nro.models.boss.Black_Goku.BlackGoku;
import static nro.models.services.PinkFixture.*;

public final class HighDamageNoticeCombatTest {
    private static final List<String> world=new ArrayList<>(), info=new ArrayList<>();
    private static final AtomicLong clock=new AtomicLong(10_000);
    public static class Target extends Player {
        public long reduction; public boolean negate,reflect,change;
        @Override public int injured(Player attacker,long damage,boolean piercing,boolean mob) {
            if(negate)return 0;
            if(reflect)attacker.nPoint.subHP(25_000_000);
            if(change){attacker.playerSkill.skillSelect.template=new nro.models.player_system.Template.SkillTemplate();attacker.playerSkill.skillSelect.template.id=2;}
            long applied=Math.max(0,damage-reduction);nPoint.subHP(applied);return (int)applied;
        }
    }
    public static class QuietMob extends Mob {
        @Override public long getTiemNangForPlayer(Player player,long damage) {return 0;}
    }
    private static NPoint points(Player p,int hp)throws Exception { NPoint n=blank(NPoint.class);Field f=NPoint.class.getDeclaredField("player");f.setAccessible(true);f.set(n,p);n.hp=hp;n.hpMax=200_000_000;return n; }
    private static void next() {clock.addAndGet(5_000);}
    public static void main(String[] args)throws Exception {
        Class<?> service;
        try {service=Class.forName("nro.models.services.HighDamageNoticeService");}catch(ClassNotFoundException missing){throw new AssertionError("Missing direct combat damage announcement hooks");}
        Object notices=service.getConstructor(LongSupplier.class,Consumer.class,Consumer.class).newInstance((LongSupplier)clock::get,(Consumer<String>)world::add,(Consumer<String>)info::add);
        Class<?> holder=Class.forName("nro.models.services.HighDamageNoticeService$Holder");Field singleton=holder.getDeclaredField("INSTANCE");singleton.setAccessible(true);singleton.get(null);U.putObjectVolatile(U.staticFieldBase(singleton),U.staticFieldOffset(singleton),notices);
        NClass catalog=new NClass();for(int id:new int[]{1,2}){var t=new nro.models.player_system.Template.SkillTemplate();t.id=(byte)id;t.name=id==1?"Kamejoko":"Masenko";catalog.skillTemplatess.add(t);}nro.models.server.Manager.NCLASS.add(catalog);
        Player p=player(10);p.name="abc";p.nPoint=points(p,200_000_000);p.playerSkill=blank(PlayerSkill.class);p.playerSkill.skillSelect=new Skill();p.playerSkill.skillSelect.template=catalog.getSkillTemplate(1);
        Target t=blank(Target.class);t.nPoint=points(t,100_000_000);
        BossNoticeService.injure(t,p,25_000_000,false,false,1);
        check(t.nPoint.hp==75_000_000&&world.size()==1,"direct real HP subtraction hook");
        next();t.reduction=10_000_000;BossNoticeService.injure(t,p,25_000_000,false,false,1);check(world.size()==1&&t.nPoint.hp==60_000_000,"reduced actual damage below threshold");
        next();t.reduction=0;t.nPoint.hp=20_000_000;BossNoticeService.injure(t,p,100_000_000,false,false,1);check(t.nPoint.hp==0&&world.size()==1,"overkill capped by remaining HP");
        next();t.nPoint.hp=100_000_000;t.reflect=true;t.change=true;BossNoticeService.injure(t,p,25_000_000,false,false,1);check(world.size()==2&&world.get(1).contains("Kamejoko")&&!world.get(1).contains("Masenko"),"executing skill snapshot and reflection excluded");
        next();p.nPoint.subHP(25_000_000);check(world.size()==2,"environment/context absent");
        next();t.negate=true;BossNoticeService.injure(t,p,100_000_000,false,false,1);check(world.size()==2,"immune hit");
        next();t.negate=false;t.reflect=false;t.change=false;BossNoticeService.injure(t,p,25_000_000,false,false,null);check(world.size()==2,"skillless hit");
        next();BlackGoku boss=blank(BlackGoku.class);boss.nPoint=points(boss,100_000_000);boss.effectSkill=blank(EffectSkill.class);boss.currentLevel=0;
        int actual=BossNoticeService.injure(boss,p,25_100_000,true,false,1);check(world.size()==3&&world.get(2).endsWith(String.format(Locale.ROOT,"%,d",actual).replace(',','.')+"."),"real BlackGoku override after defense/random adjustment actual="+actual+" notices="+world);
        next();boss.effectSkill.isShielding=true;BossNoticeService.injure(boss,p,25_100_000,false,false,1);check(world.size()==3,"real boss shield");
        replace(Service.class,"instance",new Service(){
            @Override public void sendMessAllPlayerInMap(nro.models.map.Zone zone,nro.models.network.Message msg) {}
            @Override public void addSMTN(Player player,byte type,long param,boolean ori) {}
            @Override public void updatePlayerTotalDamage(Player player) {}
        });
        replace(nro.models.services_dungeon.TrainingService.class,"instance",new nro.models.services_dungeon.TrainingService(){
            @Override public void tangTnsmLuyenTap(Player player,long tnsm) {}
        });
        QuietMob mob=new QuietMob();mob.tempId=1;mob.zone=zone();mob.point.maxHp=100_000_000;mob.point.hp=100_000_000;p.zone=mob.zone;
        next();HighDamageNoticeService.injureMob(mob,p,25_000_000,true,1);check(mob.point.hp==75_000_000&&world.size()==4,"real mob HP subtraction before later damage reassignment");
        next();mob.point.hp=mob.point.maxHp=20_000_000;HighDamageNoticeService.injureMob(mob,p,100_000_000,false,1);check(mob.point.hp==1&&world.size()==4,"real mob full HP survival cap excludes overkill");
        next();mob.point.hp=mob.point.maxHp=300_000_000;mob.tempId=nro.models.consts.ConstMob.MOC_NHAN;HighDamageNoticeService.injureMob(mob,p,100_000_000,false,1);check(mob.point.hp==270_000_000&&world.size()==5&&world.get(4).endsWith("30.000.000."),"real mob training dummy damage cap");
        next();
        try(var outer=HighDamageNoticeService.begin(p,t.nPoint,1)) {
            try(var inner=HighDamageNoticeService.begin(p,boss.nPoint,2)) {boss.nPoint.subHP(25_000_000);}
            check(world.size()==5,"nested notice waits for outer combat monitor exit");
            t.nPoint.subHP(25_000_000);
        }
        check(world.size()==6&&world.get(5).contains("Masenko"),"nested target context restored with one notice per cast");
        next();t.nPoint.hp=150_000_000;
        try(var cast=HighDamageNoticeService.begin(p,null,1)) {
            BossNoticeService.injure(t,p,25_000_000,false,false,1);
            clock.addAndGet(10_000);
            BossNoticeService.injure(t,p,30_000_000,false,false,1);
            check(world.size()==6,"no notice while cast is executing");
        }
        check(world.size()==7&&world.get(6).endsWith("25.000.000."),"one notice for long AOE cast even beyond cooldown");
        int hp=t.nPoint.hp;next();
        HighDamageNoticeService failed=new HighDamageNoticeService(clock::get,s->{throw new IllegalStateException("world failure");},s->{throw new IllegalStateException("info failure");});
        U.putObjectVolatile(U.staticFieldBase(singleton),U.staticFieldOffset(singleton),failed);
        BossNoticeService.injure(t,p,25_000_000,false,false,1);check(t.nPoint.hp==hp-25_000_000,"both sender failures preserve combat subtraction");
        U.putObjectVolatile(U.staticFieldBase(singleton),U.staticFieldOffset(singleton),notices);
        check(world.equals(info),"combat emits identical both channels");
        System.out.println("PASS HighDamageNoticeCombatTest real subtraction, defenses, overkill, reflection, skill snapshot, immunity, skillless exclusion, real BlackGoku/shield");
    }
}
