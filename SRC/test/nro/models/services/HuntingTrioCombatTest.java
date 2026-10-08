package nro.models.services;

import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongSupplier;
import nro.models.boss.Boss;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstPlayer;
import nro.models.player.*;
import nro.models.skill.PlayerSkill;
import static nro.models.services.PinkFixture.*;

public final class HuntingTrioCombatTest {
    static void field(Object o,Class<?> c,String name,Object value)throws Exception {
        Field f=c.getDeclaredField(name);f.setAccessible(true);f.set(o,value);
    }
    static final class Spy extends Player {
        long hit;boolean pierced;
        @Override public synchronized int injured(Player source,long damage,boolean piercing,boolean mob) {
            hit=damage;pierced=piercing;return (int)damage;
        }
    }
    static Boss boss()throws Exception {
        Class<?> type=Class.forName("nro.models.boss.HuntingTrio.HuntingTrioBoss");
        Class<?> life=Class.forName("nro.models.boss.HuntingTrio.HuntingTrioLifecycle");
        Class<?> backend=Class.forName(life.getName()+"$Backend");
        Object stub=Proxy.newProxyInstance(backend.getClassLoader(),new Class[]{backend},(p,m,a)->m.getReturnType()==boolean.class?true:null);
        Object engine=life.getConstructor(backend,LongSupplier.class).newInstance(stub,(LongSupplier)()->0L);
        @SuppressWarnings({"unchecked","rawtypes"}) Object active=Enum.valueOf((Class)Class.forName(life.getName()+"$Phase"),"ACTIVE");
        field(engine,life,"phase",active);
        Boss b=(Boss)blank(type);field(b,type,"lifecycle",engine);field(b,type,"finished",new AtomicBoolean());
        b.isBoss=true;b.bossStatus=BossStatus.ACTIVE;b.zone=zone();b.nPoint=blank(NPoint.class);b.nPoint.hp=1000;b.nPoint.hpMax=1000;
        b.playerSkill=new PlayerSkill(b);return b;
    }
    public static void main(String[] args)throws Exception {
        Boss b=boss();Spy small=blank(Spy.class);small.zone=b.zone;small.nPoint=blank(NPoint.class);small.nPoint.hpMax=100000;small.nPoint.hp=100000;
        Spy large=blank(Spy.class);large.zone=b.zone;large.nPoint=blank(NPoint.class);large.nPoint.hpMax=2000000000;large.nPoint.hp=1;
        check(BossNoticeService.injure(small,b,123,false,false)==5000&&small.pierced,"small target 5%, piercing");
        check(BossNoticeService.injure(large,b,123,false,false)==100000000&&large.pierced,"large target uses own MAX HP even at 1 current HP");
        check(BossNoticeService.injure(small,b,123,false,false)==5000,"target switch never reuses previous HP");
        check(BossNoticeService.injure(small,b,0,false,false)==0,"miss stays zero");
        large.zone=zone();check(BossNoticeService.injure(large,b,123,false,false)==0,"cannot attack another zone");
        small.beforeDispose=true;check(BossNoticeService.injure(small,b,123,false,false)==0,"cannot attack retiring target");small.beforeDispose=false;
        // Exercise actual Player.injured so armor, shielding and Giap Xen are really bypassed.
        Player real=new Player();real.zone=b.zone;real.nPoint.hpMax=100000;real.nPoint.hp=100000;
        real.nPoint.tlGiap=86;real.nPoint.def=999999;real.effectSkill.isShielding=true;real.itemTime.isUseGiapXen2=true;
        check(BossNoticeService.injure(real,b,123,false,false)==5000&&real.nPoint.hp==95000,"5% bypasses real armor/shield/Giap Xen");
        real.lastTimeRevived=System.currentTimeMillis();
        check(BossNoticeService.injure(real,b,123,false,false)==0&&real.nPoint.hp==95000,"recent revive immunity stays intact");
        real.lastTimeRevived=0;real.nPoint.hp=0;
        check(BossNoticeService.injure(real,b,123,false,false)==0,"dead target cannot receive another hit");
        Player attacker=player(91);attacker.zone=b.zone;
        check(b.injured(attacker,0,false,false)==0&&b.nPoint.hp==1000,"zero cannot consume boss hit");
        check(b.injured(attacker,Long.MAX_VALUE,true,false)==1&&b.nPoint.hp==999,"huge/critical/piercing damage still one HP");
        b.bossStatus=BossStatus.CHAT_S;check(b.injured(attacker,1,false,false)==0,"no damage during opening talk");
        b.bossStatus=BossStatus.ACTIVE;check(b.injured(null,1,false,false)==0,"null attacker does not crash or damage");
        Class<?> catalog=Class.forName("nro.models.boss.HuntingTrio.HuntingTrioCatalog");
        for(int g=0;g<2;g++)for(int i=0;i<3;i++) {
            var data=(nro.models.boss.BossData)catalog.getMethod("data",int.class,int.class).invoke(null,g,i);
            check(data.getHp()[0]==1000&&data.getSkillTemp().length==4,"1000HP and exact four Cell skills");
            for(int[] s:data.getSkillTemp())check(s[0]!=nro.models.skill.Skill.TAI_TAO_NANG_LUONG,"energy regeneration removed");
        }
        System.out.println("PASS hunting combat: target-specific5%/long math, actual armor/shield bypass, miss/zone/logout guards, one HP, talk protection, six fixedHP/no-regen skillsets");
    }
}
