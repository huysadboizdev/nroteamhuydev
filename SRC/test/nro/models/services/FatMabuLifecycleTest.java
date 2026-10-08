package nro.models.services;

import java.lang.reflect.*;
import java.util.function.LongSupplier;
import static nro.models.services.PinkFixture.*;

public final class FatMabuLifecycleTest {
    public static void main(String[] args)throws Exception {
        Class<?> type;
        try {type=Class.forName("nro.models.boss.Mabu.FatMabuLifecycle");}
        catch(ClassNotFoundException absent){throw new AssertionError("missing Mabu deadline/cooldown controller",absent);}
        Class<?> api=Class.forName(type.getName()+"$Backend");
        long[] now={0};boolean[] present={true},alive={true},available={true};int[] count=new int[4];
        Object backend=Proxy.newProxyInstance(api.getClassLoader(),new Class[]{api},(p,m,a)->{
            return switch(m.getName()) {
                case "spawn" -> {count[0]++;yield available[0];}
                case "chat" -> {count[1]++;yield null;}
                case "activate" -> {count[2]++;yield null;}
                case "remove" -> {count[3]++;yield null;}
                case "alive" -> alive[0];case "playersPresent" -> present[0];default -> null;
            };
        });
        Object state=type.getConstructor(api,LongSupplier.class).newInstance(backend,(LongSupplier)()->now[0]);
        Method tick=type.getMethod("tick"),phase=type.getMethod("phase");
        tick.invoke(state);check(phase.invoke(state).toString().equals("TALKING"),"spawn protected talk");
        now[0]=2000;tick.invoke(state);check(phase.invoke(state).toString().equals("ACTIVE"),"activate after talk");
        now[0]=1_201_999;tick.invoke(state);check(count[3]==0,"no early deadline");
        now[0]=1_202_000;tick.invoke(state);check(count[3]==1&&phase.invoke(state).toString().equals("COOLDOWN"),"hard20min even while occupied");
        now[0]+=599_999;tick.invoke(state);check(count[0]==1,"cooldown not shortened");
        now[0]++;tick.invoke(state);check(count[0]==2,"new turn after10min");
        now[0]+=2000;tick.invoke(state);present[0]=false;tick.invoke(state);
        now[0]+=299_999;tick.invoke(state);check(count[3]==1,"empty timeout not early");
        present[0]=true;tick.invoke(state);present[0]=false;tick.invoke(state);now[0]+=300_000;tick.invoke(state);
        check(count[3]==2,"continuous empty5min exits without kill");
        now[0]+=600_000;tick.invoke(state);now[0]+=2000;tick.invoke(state);alive[0]=false;tick.invoke(state);
        check(count[3]==3,"death closes once");tick.invoke(state);check(count[3]==3,"no double cleanup");
        alive[0]=true;available[0]=false;now[0]+=600_000;tick.invoke(state);int attempts=count[0];
        now[0]+=9999;tick.invoke(state);check(count[0]==attempts,"missing zone retry delayed");
        available[0]=true;now[0]++;tick.invoke(state);check(count[0]==attempts+1,"missing zone retry10s");
        System.out.println("PASS Mabu lifecycle: talk, occupied hard20min, empty5min reset, death, cooldown10min, retry10s");
    }
}
