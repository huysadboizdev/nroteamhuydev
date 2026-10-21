package nro.models.services;

import java.lang.reflect.Field;
import java.util.List;
import nro.models.boss.BossID;
import nro.models.boss.notice.BossNoticeRow;
import nro.models.boss.notice.BossNoticeTracker;

public final class TimedMabuNoticeTest {
    static <T> T blank(Class<T> type)throws Exception {
        Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);
        return type.cast(((sun.misc.Unsafe)f.get(null)).allocateInstance(type));
    }
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static void field(Object o,String name,Object value)throws Exception {
        Field f=BossNoticeService.class.getDeclaredField(name);f.setAccessible(true);f.set(o,value);
    }
    static BossNoticeRow row(int id,long time) {
        return new BossNoticeRow("old-"+id,"old-run",id,0,"boss","map",time,0,"","ALIVE",0,"UNKNOWN",1,time);
    }
    public static void main(String[] args)throws Exception {
        BossNoticeService service=blank(BossNoticeService.class);
        long now=System.currentTimeMillis();
        field(service,"run","run");
        BossNoticeTracker tracker=new BossNoticeTracker("run",()->now,r->{});
        field(service,"tracker",tracker);
        int[] hidden={BossID.DRABURA,BossID.DRABURA_2,BossID.DRABURA_3,BossID.BUI_BUI,BossID.BUI_BUI_2,
                BossID.YA_CON,BossID.MABU_12H,BossID.GOKU,BossID.CADIC,BossID.MABU,BossID.SUPERBU};
        var stored=new java.util.ArrayList<BossNoticeRow>();
        for(int id:hidden) {
            nro.models.boss.Boss boss=blank(nro.models.boss.Boss.class);boss.id=id;
            var eligible=BossNoticeService.class.getDeclaredMethod("boardEligible",nro.models.boss.Boss.class);
            eligible.setAccessible(true);
            check(!(boolean)eligible.invoke(null,boss),"hidden live boss must not enter tracker "+id);
            check(!boss.noticePublic(),"hidden boss must not broadcast "+id);
            stored.add(row(id,now));
            tracker.appearNew(new Object(),-900000000L+id,0,"hidden","map",id,false,0);
        }
        stored.add(row(BossID.FIDE,now));
        tracker.appearNew(new Object(),BossID.FIDE,0,"Fide","map",BossID.FIDE,false,0);
        field(service,"stored",List.copyOf(stored));
        check(service.rows(0).size()==1,"current board must hide all timed Mabu bosses");
        check(service.rows(1).size()==2,"history must hide stored and live timed Mabu rows");
        check(service.rows(1).stream().allMatch(r->r.semanticBossId()==BossID.FIDE),"unrelated Fide kept");
        for(int id:hidden) check(BossAnnouncementPolicy.suppressedNoticeId(id,-1),"ticker blocked without zone "+id);
        System.out.println("PASS timed Mabu: 11 identities hidden in live and historical rows; ticker suppressed; Fide kept");
    }
}
