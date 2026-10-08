package nro.models.boss;

import java.lang.reflect.*;
import nro.models.boss.Boss_mini.*;
import nro.models.player.*;
import nro.models.services.*;
import nro.models.consts.ConstPlayer;
import static nro.models.services.PinkFixture.*;

public final class MiniBossLifecycleTest {
    public static class Thief extends AnTrom {
        public Thief() throws Exception { super(); }
        @Override public void joinMap2() { }
    }
    public static class Odor extends Odo {
        public Odor() throws Exception { super(); }
        @Override public Player getPlayerAttack() { return null; }
    }
    static void timing() throws Exception {
        Odo b = new Odo();
        check(b.data[0].getSecondsRest() == 600, "Odo rest is 600 seconds, not milliseconds");
    }
    static void hp() throws Exception {
        Thief b = blank(Thief.class); b.nPoint = blank(NPoint.class);
        for (int n=0; n<10000; n++) { b.joinMap(); check(b.nPoint.hp >= 1 && b.nPoint.hp <= 100, "thief must spawn alive with 1..100 HP"); }
    }
    static void injury() throws Exception {
        for (Class<? extends Boss> type : new Class[]{AnTrom.class,Odo.class}) {
            Method m=type.getDeclaredMethod("injured",Player.class,long.class,boolean.class,boolean.class);
            check(Modifier.isSynchronized(m.getModifiers()), "fixed-hit death must share one injury monitor: "+type.getSimpleName());
            Boss b=blank(type); b.nPoint=blank(NPoint.class); b.nPoint.hp=100000;
            check(b.injured(null,0,false,false)==0 && b.nPoint.hp==100000, "zero incoming damage must not become a fixed hit");
            check(b.injured(null,-1,false,false)==0 && b.nPoint.hp==100000, "negative incoming damage must not become a fixed hit");
        }
    }
    static void regen() throws Exception {
        final int[] calls={0};
        replace(PlayerService.class,"i",new PlayerService(){ @Override public void hoiPhuc(Player b,long hp,long mp){calls[0]++;} });
        replace(Service.class,"instance",new Service(){ @Override public void chat(Player p,String s){} });
        Odor b=blank(Odor.class); b.typePk=ConstPlayer.PK_ALL; b.nPoint=blank(NPoint.class); b.nPoint.hp=100; b.nPoint.hpMax=500000;
        b.attack(); check(calls[0]==1,"no combat target must not skip due regeneration");
    }
    public static void main(String[] args) throws Exception {
        switch(args[0]) { case "timing" -> timing(); case "hp" -> hp(); case "injury" -> injury(); case "regen" -> regen(); default -> throw new AssertionError(args[0]); }
        System.out.println("PASS MiniBossLifecycle "+args[0]);
    }
}
