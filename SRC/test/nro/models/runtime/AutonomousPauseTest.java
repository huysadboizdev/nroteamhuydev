package nro.models.runtime;

import java.lang.reflect.Field;
import nro.models.services.ServerMaintenanceGate;

/** Paused workers must retain their loop and perform no round/manager mutation. */
public final class AutonomousPauseTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static void minigame(String name, String counter) throws Exception {
        Class<?> type=Class.forName("nro.models.minigame."+name);
        var ctor=type.getDeclaredConstructor();ctor.setAccessible(true);
        Object game=ctor.newInstance();
        Field field=type.getField(counter);long before=field.getLong(game);
        Thread worker=new Thread((Runnable)game,"pause-"+name);worker.setDaemon(true);
        ServerMaintenanceGate.pause();worker.start();
        try {
            Thread.sleep(180);
            check(field.getLong(game)==before,name+" mutated its round while paused");
            check(worker.isAlive(),name+" must keep loop alive during maintenance");
            ServerMaintenanceGate.resume();
            long until=System.nanoTime()+2_000_000_000L;
            while(field.getLong(game)==before&&System.nanoTime()<until)Thread.sleep(10);
            check(field.getLong(game)!=before,name+" did not resume");
        } finally {worker.interrupt();ServerMaintenanceGate.resume();worker.join(1500);}
    }
    static void manager(String name) throws Exception {
        Class<?> type=Class.forName(name);
        Field uf=Class.forName("sun.misc.Unsafe").getDeclaredField("theUnsafe");uf.setAccessible(true);
        Object unsafe=uf.get(null);
        Object instance=unsafe.getClass().getMethod("allocateInstance",Class.class).invoke(unsafe,type);
        Field last=type.getDeclaredField("lastUpdate");last.setAccessible(true);
        ServerMaintenanceGate.pause();
        try {type.getMethod("update").invoke(instance);check(last.getLong(instance)==0,name+" ticks during pause");}
        finally {ServerMaintenanceGate.resume();}
        type.getMethod("update").invoke(instance);check(last.getLong(instance)>0,name+" must resume");
    }
    public static void main(String[] args) throws Exception {
        if(args.length==0||args[0].equals("minigame")) {
            minigame("ConSoMayManGold","second");minigame("ConSoMayManGem","second");
            minigame("ChonAiDay_Gold","lastTimeEnd");minigame("ChonAiDay_Gem","lastTimeEnd");
        }
        if(args.length==0||args[0].equals("manager")) {
            manager("nro.models.services.shenron.Shenron_Manager");
            manager("nro.models.managers.ShenronEventManager");
        }
        System.out.println("AUTONOMOUS_PAUSE PASS");
    }
}
