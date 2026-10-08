package nro.models.services;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.LongSupplier;
import static nro.models.services.PinkFixture.check;

public final class HuntingTrioLifecycleTest {
    static final class Fixture {
        long now; int spawns,chats,activations,removed,all; boolean spawn=true,present=true,intact=true;
        final Object lifecycle;
        Fixture()throws Exception {
            Class<?> type=Class.forName("nro.models.boss.HuntingTrio.HuntingTrioLifecycle");
            Class<?> backend=Class.forName(type.getName()+"$Backend");
            Object delegate=Proxy.newProxyInstance(backend.getClassLoader(),new Class[]{backend},(p,m,a)->{
                switch(m.getName()) {
                    case "spawn":spawns++;return spawn;
                    case "chat":chats++;return null;
                    case "activate":activations++;return null;
                    case "removeMember":removed++;return null;
                    case "removeAll":all++;return null;
                    case "playersPresent":return present;
                    case "intact":return intact;
                    default:throw new AssertionError(m.getName());
                }
            });
            lifecycle=type.getConstructor(backend,LongSupplier.class).newInstance(delegate,(LongSupplier)()->now);
        }
        void tick(long at)throws Exception{now=at;call("tick");}
        Object call(String name)throws Exception{return lifecycle.getClass().getMethod(name).invoke(lifecycle);}
        void down(int i)throws Exception{lifecycle.getClass().getMethod("defeated",int.class).invoke(lifecycle,i);}
        String phase()throws Exception{return call("phase").toString();}
        void active()throws Exception{tick(0);tick(0);tick(2000);tick(4000);check(activations==0,"all three stay protected while talking");tick(6000);check(activations==1&&chats==3&&phase().equals("ACTIVE"),"three lines then one joint unlock");}
    }
    public static void main(String[] args)throws Exception {
        Fixture f=new Fixture();f.active();f.down(0);f.down(0);f.tick(7000);
        check(f.removed==1&&f.phase().equals("ACTIVE"),"one death removed once, group does not respawn");
        f.down(1);f.down(2);f.tick(8000);check(f.removed==3&&f.all==1&&f.phase().equals("COOLDOWN"),"all three end the cycle");
        f.tick(607999);check(f.spawns==1,"wait complete ten minutes");f.tick(608000);check(f.spawns==2,"respawn after ten minutes");
        Fixture noZone=new Fixture();noZone.spawn=false;noZone.tick(0);noZone.tick(9999);check(noZone.spawns==1,"missing zone cannot spin");noZone.tick(10000);check(noZone.spawns==2,"retry missing zone later");
        Fixture broken=new Fixture();broken.tick(0);broken.intact=false;broken.tick(1);check(broken.all==1&&broken.activations==0,"partial spawn cannot unlock");
        Fixture empty=new Fixture();empty.active();empty.present=false;empty.tick(7000);empty.tick(306999);check(empty.all==0,"allow five empty minutes");empty.tick(307000);check(empty.all==1&&empty.phase().equals("COOLDOWN"),"empty group retires without combat reward");
        Fixture returning=new Fixture();returning.active();returning.present=false;returning.tick(7000);returning.present=true;returning.tick(300000);returning.present=false;returning.tick(400000);returning.tick(699999);check(returning.all==0,"return resets empty window");
        System.out.println("PASS Hunting trio lifecycle: joint chat/unlock, individual death idempotence, whole-group cooldown, missing zone, partial spawn, empty/return");
    }
}
