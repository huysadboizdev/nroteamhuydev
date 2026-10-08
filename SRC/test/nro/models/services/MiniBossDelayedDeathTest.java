package nro.models.services;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import nro.models.boss.Boss;
import nro.models.boss.Boss_mini.*;
import nro.models.consts.BossStatus;
import nro.models.map.Zone;
import nro.models.player.*;
import nro.models.server.Client;
import static nro.models.services.PinkFixture.*;

public final class MiniBossDelayedDeathTest {
    public static class Target extends Player {
        int hits;
        @Override public synchronized int injured(Player source,long damage,boolean piercing,boolean mob) {
            hits++; nPoint.hp -= (int)damage; return (int)damage;
        }
    }
    public static class Sun extends MatTroi {
        public Sun() throws Exception { super(); }
        @Override public void chat(String message) {}
    }
    public static class Virus extends Virut {
        public Virus() throws Exception { super(); }
        @Override public void chat(String message) {}
    }
    static void field(Object o,Class<?> c,String name,Object value)throws Exception {
        Field f=c.getDeclaredField(name);f.setAccessible(true);f.set(o,value);
    }
    static void call(Boss b,Class<?> type,String method,Target p)throws Exception {
        Method m=p==null?type.getDeclaredMethod(method):type.getDeclaredMethod(method,Player.class);
        m.setAccessible(true);if(p==null)m.invoke(b);else m.invoke(b,p);
    }
    @SuppressWarnings("unchecked")
    static void run(Class<? extends Boss> type,Class<? extends Boss> fixture,String mode)throws Exception {
        Boss b=blank(fixture);b.zone=zone();b.nPoint=blank(NPoint.class);b.nPoint.hp=100;b.bossStatus=BossStatus.ACTIVE;
        field(b,type,"globalEffectTimers",new ConcurrentHashMap<>());
        Target p=blank(Target.class);p.id=99;p.isPlayer=true;p.zone=b.zone;p.nPoint=blank(NPoint.class);p.nPoint.hp=1000;
        Client client=blank(Client.class);field(client,Client.class,"players_id",new ConcurrentHashMap<>(Map.of(p.id,p)));replace(Client.class,"instance",client);
        boolean invalid=!mode.equals("valid");
        for(int attempt=0;attempt<40;attempt++) {
            b.nPoint.hp=100;p.nPoint.hp=1000;Zone original=b.zone;p.zone=original;p.beforeDispose=false;
            call(b,type,"applyEffect",p);
            Field f=type.getDeclaredField("globalEffectTimers");f.setAccessible(true);
            Map<Long,Object> timers=(Map<Long,Object>)f.get(b);Object effect=timers.get(p.id);
            if(effect instanceof Long)timers.put(p.id,0L);else field(effect,effect.getClass(),"deadline",0L);
            if(mode.equals("other-zone"))p.zone=zone();
            if(mode.equals("exit-return")){original.removePlayer(p);p.zone=original;}
            if(mode.equals("revived"))p.lastTimeRevived++;
            if(mode.equals("logout"))p.beforeDispose=true;
            if(mode.equals("boss-dead"))b.nPoint.hp=0;
            call(b,type,"checkGlobalEffects",null);
            check(timers.isEmpty(),"processed or invalid effect must be removed");
        }
        check(invalid?p.hits==0:p.hits>0,type.getSimpleName()+" "+mode+" hits="+p.hits);
    }
    public static void main(String[] args)throws Exception {
        replace(ItemTimeService.class,"i",new ItemTimeService(){@Override public void sendItemTime(Player p,int icon,int seconds){}});
        replace(PlayerService.class,"i",new PlayerService(){@Override public void sendInfoHpMpMoney(Player p){}});
        for(String mode:List.of("other-zone","exit-return","revived","logout","boss-dead","valid")) {
            run(MatTroi.class,Sun.class,mode);run(Virut.class,Virus.class,mode);
        }
        System.out.println("PASS delayed effects: zone change, leave/return, revive, logout, boss death, legitimate execution for both bosses");
    }
}
