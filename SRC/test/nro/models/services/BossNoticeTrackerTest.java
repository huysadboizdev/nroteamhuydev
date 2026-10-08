package nro.models.services;
import java.util.*;
import nro.models.boss.notice.*;
public final class BossNoticeTrackerTest {
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    public static void main(String[] args){
        long[] now={1_000_000};List<BossNoticeRow> events=new ArrayList<>();
        BossNoticeTracker t=new BossNoticeTracker("run",()->now[0],events::add);
        Object first=new Object(),second=new Object();
        t.appear(first,1,0,"Fide","Núi");t.appear(first,1,0,"Fide","Núi");check(events.size()==1,"spawn dedup");
        t.appear(second,1,0,"Fide","Núi");check(t.current().size()==2&&!events.get(0).spawnId().equals(events.get(1).spawnId()),"same names distinct instances");
        now[0]+=60_000;t.defeat(first,"admin");t.defeat(first,"other");check(events.size()==3&&t.current().stream().anyMatch(r->r.killerName().equals("admin")),"one killer per death");
        t.depart(first);check(events.size()==3,"death departure not extra disappearance");
        t.rest(first,now[0]+300_000,"COOLDOWN");check(t.current().stream().anyMatch(r->r.nextSpawnAt()==now[0]+300_000),"runtime rest ETA");
        t.appear(first,1,1,"Fide 2","Núi");check(events.size()==5&&t.history().size()==3,"next form new cycle");
        t.depart(second);check(t.current().stream().anyMatch(r->r.status().equals("DEPARTED")&&r.killerName().isEmpty()),"despawn never fake defeat");
        t.rest(first,0,"CONDITION");check(t.current().stream().anyMatch(r->r.etaKind().equals("CONDITION")&&r.nextSpawnAt()==0),"unknown ETA remains unknown");
        Object support=new Object();t.appear(support,-999999,0,"Kuku","N?i",nro.models.boss.BossID.KUKU,true,7);
        var quest=t.current().stream().filter(r->r.bossId()==-999999).findFirst().orElseThrow();check(quest.category()==1&&quest.questSupport()&&quest.zoneId()==7,"support semantic category and zone");
        t.appear(support,-999999,0,"Kuku","N?i",nro.models.boss.BossID.KUKU,true,8);quest=t.current().stream().filter(r->r.bossId()==-999999).findFirst().orElseThrow();check(quest.zoneId()==8&&quest.version()==2,"move snapshot persists zone");
        t.defeat(support,"admin");quest=t.current().stream().filter(r->r.bossId()==-999999).findFirst().orElseThrow();check(quest.category()==1&&quest.questSupport()&&quest.zoneId()==8,"classification survives transition");
        System.out.println("PASS BossNotice tracker: cycle/instance dedup, killer, forms, disappearance, runtime ETA, no zones");
    }
}
