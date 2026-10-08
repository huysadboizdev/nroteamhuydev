package nro.models.services;

import java.lang.reflect.*;
import java.util.concurrent.atomic.AtomicBoolean;
import nro.models.boss.Boss;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.boss.Boss_Manager.BrolyManager;
import nro.models.map.Zone;
import nro.models.map.service.MapService;
import nro.models.player.Player;
import static nro.models.services.PinkFixture.*;

/** Executes real HuntingTrioBoss.leaveMap / ChangeMapService.exitMap against injected zone failures. */
public final class HuntingTrioCleanupTest {
    static class FailingZone extends Zone {
        boolean failOnce=true,blocked;
        int exitAttempts;
        FailingZone(nro.models.map.Map map){super(map,0,20);}
        @Override public void removePlayer(Player p){exitAttempts++;if(blocked||failOnce){failOnce=false;throw new IllegalStateException("injected exitMap detach failure");}super.removePlayer(p);}
    }
    static Object backend()throws Exception {Class<?> c=Class.forName("nro.models.boss.HuntingTrio.HuntingTrioService$Backend");Constructor<?> ctor=c.getDeclaredConstructor(int.class);ctor.setAccessible(true);return ctor.newInstance(0);}
    static Field field(Object o,String name)throws Exception {Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f;}
    static Object call(Object o,String name,Object... arguments)throws Exception {
        Method m=o.getClass().getDeclaredMethod(name,arguments.length==0?new Class<?>[0]:new Class<?>[]{int.class});m.setAccessible(true);
        try{return m.invoke(o,arguments);}catch(InvocationTargetException failed){throw (RuntimeException)failed.getCause();}
    }
    static Boss boss(int id,Zone zone)throws Exception {
        Class<?> type=Class.forName("nro.models.boss.HuntingTrio.HuntingTrioBoss");Boss b=(Boss)blank(type);Player initialized=new Player();
        for(Field f:Player.class.getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())){f.setAccessible(true);f.set(b,f.get(initialized));}
        field(b,"finished").set(b,new AtomicBoolean());b.isBoss=true;b.isPlayer=false;b.id=id;b.name="cleanup";b.zone=zone;zone.addPlayer(b);BossManager.gI().addBoss(b);return b;
    }
    static Boss[] members(Object backend)throws Exception {return (Boss[])field(backend,"members").get(backend);}
    static void reserve(Object backend,Zone zone)throws Exception {field(backend,"zone").set(backend,zone);check(BrolyManager.gI().zones().tryReserveExternal(zone,backend),"test owns reservation");}
    static void failure(Object backend,String method,Object...args)throws Exception {try{call(backend,method,args);throw new AssertionError("injected failure must be reported");}catch(IllegalStateException expected){check(expected.getMessage().contains("injected")||expected.getSuppressed().length>0,"original cleanup failure retained");}}
    static void exitFailure()throws Exception {
        install();Object backend=backend();FailingZone zone=new FailingZone(zone().map);reserve(backend,zone);Boss b=boss(-301,zone);members(backend)[0]=b;
        Boss second=boss(-303,zone),third=boss(-304,zone);members(backend)[1]=second;members(backend)[2]=third;
        failure(backend,"removeAll");check(zone.exitAttempts>=2,"forced detach runs after real exitMap throws");
        check(!zone.getBosses().contains(b)&&!zone.getHumanoids().contains(b)&&b.zone==null,"faulty exit cannot strand finished boss");
        check(!BossManager.gI().getBosses().contains(b),"manager removal independent from failed map exit");
        check(b.name==null,"dispose still runs after failed exit");
        for(Boss member:new Boss[]{b,second,third})check(member.zone==null&&member.name==null&&!BossManager.gI().getBosses().contains(member),"all three members retire despite first cleanup fault");
        check(members(backend)[0]==null&&field(backend,"zone").get(backend)==null,"cleaned references cleared");
        check(!BrolyManager.gI().zones().isReserved(zone),"reservation released after successful forced detach");
        replace(MapService.class,"i",new MapService(){@Override public nro.models.map.Map getMapById(int id){return null;}});
        check(!(boolean)call(backend,"spawn"),"next spawn waits for map capacity without retaining finished boss");
        check(zone.getBosses().isEmpty()&&BossManager.gI().getBosses().isEmpty(),"no old generation remains at next spawn");
    }
    static void blockedDetach()throws Exception {
        install();Object backend=backend();FailingZone zone=new FailingZone(zone().map);zone.blocked=true;reserve(backend,zone);Boss b=boss(-302,zone);members(backend)[0]=b;
        failure(backend,"removeAll");check(zone.getBosses().contains(b)&&b.zone==zone,"failed forced detach retains accurate attachment");
        check(members(backend)[0]==b&&field(backend,"zone").get(backend)==zone,"retain member and zone for cleanup retry");
        check(BrolyManager.gI().zones().isReserved(zone),"never release occupied reservation");
        check(!BossManager.gI().getBosses().contains(b),"manager retirement independent of blocked detach");
        replace(MapService.class,"i",new MapService(){@Override public nro.models.map.Map getMapById(int id){throw new AssertionError("new spawn cannot start until leaked generation detached");}});
        check(!(boolean)call(backend,"spawn"),"next spawn blocked by old attached member");check(members(backend)[0]==b,"spawn never overwrites retained member");
        zone.blocked=false;call(backend,"removeAll");check(zone.getBosses().isEmpty()&&members(backend)[0]==null,"retry detaches old generation");
        check(!BrolyManager.gI().zones().isReserved(zone),"retry releases reservation only after detach");
    }
    public static void main(String[] args)throws Exception {try{if(args[0].equals("exitFailure"))exitFailure();else if(args[0].equals("blockedDetach"))blockedDetach();else throw new AssertionError(args[0]);System.out.println("PASS HuntingTrioCleanup "+args[0]);System.exit(0);}catch(Throwable failed){failed.printStackTrace();System.exit(1);}}
}
