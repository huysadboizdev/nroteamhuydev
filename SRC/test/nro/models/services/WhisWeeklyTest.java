package nro.models.services;

import java.lang.reflect.*;
import java.time.*;
import nro.models.player.Player;

public final class WhisWeeklyTest {
    private static void check(boolean ok, String text) { if (!ok) throw new AssertionError(text); }
    public static void main(String[] args) throws Exception {
        Class<?> policy;
        try { policy = Class.forName("nro.models.services.WhisWeeklyPolicy"); }
        catch (ClassNotFoundException e) { throw new AssertionError("Whis weekly policy is missing", e); }
        Method week = policy.getMethod("weekStart", long.class);
        long sunday = Instant.parse("2026-10-11T16:59:59Z").toEpochMilli();
        long monday = sunday + 1000;
        check(week.invoke(null, sunday).equals(LocalDate.of(2026,10,5)), "Sunday remains old week");
        check(week.invoke(null, monday).equals(LocalDate.of(2026,10,12)), "Monday midnight UTC+7 starts new week");
        check(week.invoke(null, Instant.parse("2026-12-31T20:00:00Z").toEpochMilli()).equals(LocalDate.of(2026,12,28)), "Year crossing retains Monday key");
        Method percent = policy.getMethod("bonusPercent", int.class);
        for (int rank = 0; rank <= 5; rank++) check((int)percent.invoke(null, rank) == (rank==1?5:rank==2?4:rank==3?3:0), "Exact rank percentage");
        Method bonus = policy.getMethod("applyBonus", long.class, int.class);
        check((long)bonus.invoke(null, 10000L, 5)==10500, "Five percent total stat");
        check((long)bonus.invoke(null, (long)Integer.MAX_VALUE,5)==Integer.MAX_VALUE, "Integer clamp");
        Class<?> guard=Class.forName("nro.models.services.WhisFightGuard");
        Method reserve=guard.getMethod("reserve",long.class), release=guard.getMethod("release",long.class,Object.class);
        Object a=reserve.invoke(null,42L);check(a!=null,"First fight admitted");
        check(reserve.invoke(null,42L)==null,"Duplicate pending/live fight rejected");
        release.invoke(null,42L,new Object());check(reserve.invoke(null,42L)==null,"Wrong token cannot release fight");
        release.invoke(null,42L,a);Object b=reserve.invoke(null,42L);check(b!=null,"New fight after correct release");
        release.invoke(null,42L,a);check(reserve.invoke(null,42L)==null,"Old leave cannot release new fight");release.invoke(null,42L,b);
        System.out.println("WHIS_WEEKLY_POLICY_GUARD PASS");
    }
}
