package nro.models.boss.notice;
import java.util.*;
import java.util.function.*;

public final class BossNoticeTracker {
    private final String run;private final LongSupplier clock;private final Consumer<BossNoticeRow> sink;
    private final Map<Object,BossNoticeRow> current=new WeakHashMap<>();
    private final LinkedHashMap<String,BossNoticeRow> recent=new LinkedHashMap<>();
    public BossNoticeTracker(String run,LongSupplier clock,Consumer<BossNoticeRow> sink){this.run=run;this.clock=clock;this.sink=sink;}
    private void put(Object key,BossNoticeRow r){
        current.put(key,r);recent.put(r.spawnId(),r);while(recent.size()>1000)recent.remove(recent.keySet().iterator().next());sink.accept(r);
    }
    public synchronized void appear(Object key,long id,int form,String name,String map){
        appear(key,id,form,name,map,(int)id,false,-1);
    }
    public synchronized void appear(Object key,long id,int form,String name,String map,int semanticId,boolean support,int zone){
        BossNoticeRow old=current.get(key);long now=clock.getAsLong();
        if(old!=null&&old.status().equals("ALIVE")&&old.form()==form&&old.bossName().equals(name)){
            if(!old.mapName().equals(map)||old.zoneId()!=zone)put(key,old.location(map,zone,now));return;
        }
        if(old!=null&&old.status().equals("ALIVE"))depart(key);
        put(key,new BossNoticeRow(UUID.randomUUID().toString(),run,id,form,name,map,now,0,"","ALIVE",0,"UNKNOWN",1,now,BossNoticeCatalog.category(semanticId,form),semanticId,support,zone,2));
    }
    public synchronized void defeat(Object key,String killer){
        BossNoticeRow old=current.get(key);if(old==null||!old.status().equals("ALIVE"))return;
        long now=clock.getAsLong();put(key,old.change(old.mapName(),now,killer,"DEFEATED",0,"UNKNOWN",now));
    }
    public synchronized void depart(Object key){
        BossNoticeRow old=current.get(key);if(old==null||!old.status().equals("ALIVE"))return;
        long now=clock.getAsLong();put(key,old.change(old.mapName(),0,"","DEPARTED",0,"UNKNOWN",now));
    }
    public synchronized void rest(Object key,long next,String kind){
        BossNoticeRow old=current.get(key);if(old==null)return;
        if(old.status().equals("ALIVE")){depart(key);old=current.get(key);}
        if(old.nextSpawnAt()==next&&old.etaKind().equals(kind))return;
        put(key,old.change(old.mapName(),old.defeatedAt(),old.killerName(),old.status(),next,kind,clock.getAsLong()));
    }
    public synchronized List<BossNoticeRow> current(){return new ArrayList<>(current.values());}
    public synchronized List<BossNoticeRow> history(){return new ArrayList<>(recent.values());}
}
