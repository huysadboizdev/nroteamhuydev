package nro.models.player;

import java.lang.reflect.*;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import nro.models.intrinsic.Intrinsic;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.services.Service;
import nro.models.task.TaskMain;
import nro.models.utils.Util;

/** Real pet stat allocation, cost and concurrency paths with network output omitted. */
public final class PetAutoPointTest {
    private static final AtomicInteger notices=new AtomicInteger(),packets=new AtomicInteger();
    public static void main(String[] args)throws Exception {
        Field service=Service.class.getDeclaredField("instance");service.setAccessible(true);
        service.set(null,new Service(){
            @Override public void sendThongBao(Player p,String s){notices.incrementAndGet();}
            @Override public void sendThongBaoOK(Player p,String s){notices.incrementAndGet();}
            @Override public void point(Player p){packets.incrementAndGet();}
        });
        Field random=Util.class.getDeclaredField("rand");random.setAccessible(true);((Random)random.get(null)).setSeed(0);
        switch(args[0]) {
            case "distribution" -> distribution();case "batch" -> batch();case "costs" -> costs();
            case "caps" -> caps();case "blocked" -> blocked();case "player" -> player();case "concurrent" -> concurrent();
            default -> throw new AssertionError("Unknown mode");
        }
        System.out.println("PASS: Pet auto point "+args[0]);
    }
    private static Player master() {
        Player p=new Player(){@Override public void sendMessage(Message m){}};initialize(p);return p;
    }
    private static void initialize(Player p) {
        p.name="Pet auto point fixture";
        p.playerIntrinsic.intrinsic=new Intrinsic();p.playerTask.taskMain=new TaskMain();p.playerTask.taskMain.id=-1;
        p.nPoint.limitPower=9;p.nPoint.hpg=p.nPoint.mpg=1000;p.nPoint.dameg=10;p.nPoint.defg=9;p.nPoint.critg=0;
        for(int i=0;i<12;i++)p.inventory.itemsBody.add(new Item());
    }
    private static Pet pet(long tn) {Player owner=master();Pet p=new Pet(owner);initialize(p);owner.pet=p;p.nPoint.tiemNang=tn;return p;}
    private static void auto(Pet p)throws Exception {
        Method method=Pet.class.getDeclaredMethod("increasePoint");method.setAccessible(true);
        Field clock=Pet.class.getDeclaredField("lastTimeIncreasePoint");clock.setAccessible(true);clock.setLong(p,0);
        method.invoke(p);
    }
    private static Object call(NPoint point,String name,Class<?>[] types,Object... args)throws Exception {
        try{return NPoint.class.getMethod(name,types).invoke(point,args);}
        catch(NoSuchMethodException e){throw new AssertionError("Missing pet point API: "+name,e);}
        catch(InvocationTargetException e){if(e.getCause() instanceof RuntimeException r)throw r;throw e;}
    }
    private static long cost(NPoint point,int type,int count)throws Exception {
        return (Long)call(point,"getPointCost",new Class<?>[]{byte.class,int.class},(byte)type,count);
    }
    private static boolean increase(NPoint point,int type,int count)throws Exception {
        return (Boolean)call(point,"tryIncreasePetPoint",new Class<?>[]{byte.class,int.class},(byte)type,count);
    }
    private static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
    private static long expected(int type,long base,int n){return type<2?n*(base+1000)+10L*n*(n-1):n*base*100+50L*n*(n-1);}
    private static void distribution()throws Exception {
        Pet p=pet(Long.MAX_VALUE/4);Player owner=p.master;long tn=owner.nPoint.tiemNang;
        auto(p);int hp=(p.nPoint.hpg-1000)/20,ki=(p.nPoint.mpg-1000)/20,dame=p.nPoint.dameg-10;
        check(p.nPoint.defg==9 && p.nPoint.critg==0,"Pet auto allocation must never increase defense or critical");
        check(hp==ki && hp==dame && hp>0 && hp<=20_000,"Equal HP/KI/damage point counts, at most twenty 1000-point cycles");
        check(owner.nPoint.hpg==1000 && owner.nPoint.mpg==1000 && owner.nPoint.dameg==10 && owner.nPoint.tiemNang==tn,"Master stats and TN unchanged");
        check(packets.get()<=1 && notices.get()==0,"Auto update sends at most one point packet and no error notices");
        Pet partial=pet(2000);auto(partial);check(partial.nPoint.hpg==1020 && partial.nPoint.mpg==1000 && partial.nPoint.dameg==10,"Partial cycle starts at HP");
        partial.nPoint.tiemNang=4000;auto(partial);
        check(partial.nPoint.hpg==1020 && partial.nPoint.mpg==1020 && partial.nPoint.dameg==11,"Partial cycle cursor persists across updates and balances next eligible stats");
    }
    private static void batch()throws Exception {
        Pet p=pet(Long.MAX_VALUE/4);long before=p.nPoint.tiemNang;auto(p);
        int hp=(p.nPoint.hpg-1000)/20,ki=(p.nPoint.mpg-1000)/20,dame=p.nPoint.dameg-10;
        check(hp>20 && ki>20 && dame>20,"Batch allocation advances more than twenty single points per stat");
        check(before-p.nPoint.tiemNang==expected(0,1000,hp)+expected(1,1000,ki)+expected(2,10,dame),"Batch spends exact sum of sequential point costs");
    }
    private static void costs()throws Exception {
        for(int type=0;type<3;type++)for(int n:new int[]{1,2,1000}) {
            Pet p=pet(0);if(type==2)p.nPoint.dameg=26_000;
            long base=type==0?p.nPoint.hpg:type==1?p.nPoint.mpg:p.nPoint.dameg;
            long fee=expected(type,base,n);check(cost(p.nPoint,type,n)==fee,"Exact long sequential cost type="+type+" n="+n);
            // Cost calculation is independent of headroom; use a low base for the purchase test.
            p.nPoint.dameg=10;base=type<2?1000:10;fee=expected(type,base,n);p.nPoint.tiemNang=fee-1;
            check(!increase(p.nPoint,type,n) && p.nPoint.tiemNang==fee-1,"One TN below cost fails atomically");
            check(p.nPoint.hpg==1000 && p.nPoint.mpg==1000 && p.nPoint.dameg==10,"Failed batch leaves stats unchanged");
            p.nPoint.tiemNang=fee;check(increase(p.nPoint,type,n) && p.nPoint.tiemNang==0,"Exact TN buys once");
            check((type==0?p.nPoint.hpg:type==1?p.nPoint.mpg:p.nPoint.dameg)==base+n*(type<2?20:1),"Batch exact stat increment");
        }
        Pet invalid=pet(1_000_000_000L);
        for(int type:new int[]{-1,3,4,5})check(!increase(invalid.nPoint,type,1),"Only HP/KI/damage accepted");
        for(int n:new int[]{0,-1,1001})check(!increase(invalid.nPoint,0,n),"Batch size validated");
        check(invalid.nPoint.tiemNang==1_000_000_000L && packets.get()==0 && notices.get()==0,"Direct quiet APIs do not notify or spend on invalid inputs");
    }
    private static void caps()throws Exception {
        Pet p=pet(Long.MAX_VALUE/4);int cap=p.nPoint.getHpMpLimit();p.nPoint.hpg=cap-21;p.nPoint.mpg=cap;p.nPoint.dameg=p.nPoint.getDameLimit()-2;
        auto(p);check(p.nPoint.hpg==cap-1 && p.nPoint.mpg==cap && p.nPoint.dameg==p.nPoint.getDameLimit(),"Partial headroom skips capped stats without overshooting");
        check(p.nPoint.defg==9 && p.nPoint.critg==0,"Caps do not redirect points to defense/critical");
        Pet direct=pet(Long.MAX_VALUE/4);direct.nPoint.hpg=cap-1;long tn=direct.nPoint.tiemNang;
        check(!increase(direct.nPoint,0,1) && direct.nPoint.hpg==cap-1 && direct.nPoint.tiemNang==tn,"Sub-20 HP headroom rejects whole point");
    }
    private static void blocked()throws Exception {
        Pet none=pet(0);auto(none);check(none.nPoint.hpg==1000 && none.nPoint.mpg==1000 && none.nPoint.dameg==10,"Zero TN stops quietly");
        Pet capped=pet(Long.MAX_VALUE/4);capped.nPoint.hpg=capped.nPoint.mpg=capped.nPoint.getHpMpLimit();capped.nPoint.dameg=capped.nPoint.getDameLimit();
        long tn=capped.nPoint.tiemNang;auto(capped);check(capped.nPoint.tiemNang==tn,"All caps preserve TN");
        Pet low=pet(999);auto(low);check(low.nPoint.tiemNang==999,"Unaffordable one-point stats stop quietly");
        check(notices.get()==0 && packets.get()==0,"Blocked updates emit no messages or point packets");
        // HP/KI cost 2000, damage cost 1000: unaffordable stats must not block damage.
        Pet partial=pet(1000);auto(partial);check(partial.nPoint.dameg==11 && partial.nPoint.hpg==1000 && partial.nPoint.mpg==1000 && partial.nPoint.tiemNang==0,"Affordable eligible stat progresses alone");
    }
    private static void player()throws Exception {
        Pet p=pet(100_000_000L);p.nPoint.increasePoint((byte)3,(short)1);p.nPoint.increasePoint((byte)4,(short)1);
        check(p.nPoint.defg==9 && p.nPoint.critg==0 && p.nPoint.tiemNang==100_000_000L && notices.get()==0 && packets.get()==0,"Legacy pet defense/critical requests blocked quietly");
        Player owner=master();owner.nPoint.tiemNang=100_000_000L;
        owner.nPoint.increasePoint((byte)3,(short)1);owner.nPoint.increasePoint((byte)4,(short)1);
        check(owner.nPoint.defg==10 && owner.nPoint.critg==1 && owner.nPoint.tiemNang==48_600_000L,"Player defense/critical behavior preserved");
        long tn=owner.nPoint.tiemNang;check(!increase(owner.nPoint,0,1) && owner.nPoint.tiemNang==tn,"Pet API rejects normal player");
    }
    private static void concurrent()throws Exception {
        Pet p=pet(4010);CountDownLatch start=new CountDownLatch(1);AtomicInteger bought=new AtomicInteger();AtomicReference<Throwable> error=new AtomicReference<>();
        Runnable action=()->{try{start.await();if(increase(p.nPoint,0,1))bought.incrementAndGet();}catch(Throwable t){error.compareAndSet(null,t);}};
        Thread a=new Thread(action),b=new Thread(action);a.start();b.start();start.countDown();a.join();b.join();
        if(error.get()!=null)throw new AssertionError("Concurrent purchase",error.get());
        check(bought.get()==1 && p.nPoint.hpg==1020 && p.nPoint.tiemNang==2010,"Concurrent exact updated-cost check prevents second unaffordable purchase");
        Pet enough=pet(4020);CountDownLatch gate=new CountDownLatch(1);AtomicReference<Throwable> failed=new AtomicReference<>();
        Runnable both=()->{try{gate.await();check(increase(enough.nPoint,0,1),"Both sequential purchases affordable");}catch(Throwable t){failed.compareAndSet(null,t);}};
        Thread c=new Thread(both),d=new Thread(both);c.start();d.start();gate.countDown();c.join();d.join();
        if(failed.get()!=null)throw new AssertionError("Concurrent paid points",failed.get());
        check(enough.nPoint.hpg==1040 && enough.nPoint.tiemNang==0,"Concurrent purchases charge 2000 then 2020 exactly");
    }
}
