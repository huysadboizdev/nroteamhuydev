package nro.models.services;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.Path;
import nro.models.boss.Boss;
import nro.models.boss.notice.*;
import nro.models.database.BossNoticeDAO;
import nro.models.network.Message;
import nro.models.network.MySession;
import nro.models.player.Player;
import nro.models.player.Pet;
import nro.models.utils.Logger;

public final class BossNoticeService {
    public static final int COMMAND=-58,PAGE_SIZE=20;
    public static final long RETENTION=7L*24*60*60*1000;
    private static class Holder{static final BossNoticeService INSTANCE=new BossNoticeService();}
    public static BossNoticeService gI(){return Holder.INSTANCE;}
    private final String run=UUID.randomUUID().toString();
    private final BossNoticeDAO dao=new BossNoticeDAO();
    private final BossNoticeOutbox outbox=new BossNoticeOutbox(Path.of("data/boss-notice-outbox"));
    private final ConcurrentMap<String,BossNoticeRow> unstaged=new ConcurrentHashMap<>();
    private final BossNoticeTracker tracker=new BossNoticeTracker(run,System::currentTimeMillis,this::persist);
    private volatile List<BossNoticeRow> stored=List.of();
    private volatile boolean historyReady;
    private long lastLog,lastPrune;
    private record Attack(Boss boss,Player attacker){}
    private static final ThreadLocal<Attack> ATTACK=new ThreadLocal<>();
    public static Player deathAttacker(Boss b){Attack a=ATTACK.get();return a!=null&&a.boss()==b?a.attacker():null;}
    public static int injure(Player target,Player attacker,long damage,boolean piercing,boolean mobAttack){
        if(!(target instanceof Boss b))return target.injured(attacker,damage,piercing,mobAttack);
        Attack previous=ATTACK.get();ATTACK.set(new Attack(b,attacker));
        try{return target.injured(attacker,damage,piercing,mobAttack);}finally{try{b.ensureDeathTransition();}finally{if(previous==null)ATTACK.remove();else ATTACK.set(previous);}}
    }
    private BossNoticeService(){
        var worker=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"Boss notice persistence");t.setDaemon(true);return t;});
        worker.scheduleWithFixedDelay(this::flush,1,2,TimeUnit.SECONDS);
    }
    private void persist(BossNoticeRow row){
        try{outbox.stage(row);}catch(Exception failure){unstaged.merge(row.spawnId(),row,(a,b)->a.version()>b.version()?a:b);log(failure);}
    }
    private void log(Exception failure){long now=System.currentTimeMillis();if(now-lastLog>30_000){lastLog=now;Logger.logException(BossNoticeService.class,failure,"Boss notice persistence waiting for recovery");}}
    private void flush(){
        try{
            for(var entry:unstaged.entrySet()){outbox.stage(entry.getValue());unstaged.remove(entry.getKey(),entry.getValue());}
            outbox.drain(dao,64);long now=System.currentTimeMillis();
            stored=List.copyOf(dao.history(now-RETENTION));historyReady=true;
            if(now-lastPrune>3_600_000){dao.prune(now-RETENTION);lastPrune=now;}
        }catch(Exception failure){log(failure);}
    }
    private static String clean(String s,int max){if(s==null)return "";s=s.replaceAll("[\\r\\n\\t]"," ").trim();return s.length()>max?s.substring(0,max):s;}
    public void appeared(Boss b){
        try{if(b.noticePublic()&&b.zone!=null&&!b.isDie())tracker.appear(b,b.id,b.currentLevel,clean(b.name,100),clean(b.zone.map.mapName,100));}
        catch(Exception failure){log(failure);}
    }
    public void defeated(Boss b,Player killer){
        try{
            if(!b.noticePublic())return;
            if(killer instanceof Pet pet&&pet.master!=null)killer=pet.master;
            if(killer instanceof nro.models.player.NewPet pet&&pet.master!=null)killer=pet.master;
            String name=killer!=null&&killer.isPl()&&!killer.isBot?clean(killer.name,80):"Không xác định";
            tracker.defeat(b,name);
        }catch(Exception failure){log(failure);}
    }
    public void departed(Boss b){try{tracker.depart(b);}catch(Exception failure){log(failure);}}
    public void resting(Boss b){try{tracker.rest(b,b.noticeNextSpawnAt(),b.noticeNextSpawnAt()>0?"COOLDOWN":"CONDITION");}catch(Exception failure){log(failure);}}
    public List<BossNoticeRow> rows(int kind){
        long since=System.currentTimeMillis()-RETENTION;
        Map<String,BossNoticeRow> rows=new HashMap<>();
        if(kind==1)for(var r:stored)if(r.appearedAt()>=since)rows.put(r.spawnId(),r);
        for(var r:kind==0?tracker.current():tracker.history())if(kind==0||r.appearedAt()>=since)rows.merge(r.spawnId(),r,(a,b)->a.version()>b.version()?a:b);
        return rows.values().stream().sorted(Comparator.comparingLong((BossNoticeRow r)->Math.max(r.appearedAt(),r.defeatedAt())).reversed().thenComparing(BossNoticeRow::spawnId)).limit(10_000).toList();
    }
    public void request(MySession s,int request,int kind,int page){
        Player p=s.player;long now=System.currentTimeMillis();
        if(!s.bossNoticeBoard||p==null||p.session!=s||!s.isConnected()||p.beforeDispose||p.isOffline||request<0||kind<0||kind>1||page<0||page>499)return;
        synchronized(s){if(now-s.lastBossNoticeRequest<2000)return;s.lastBossNoticeRequest=now;}
        List<BossNoticeRow> all=rows(kind);int pages=Math.max(1,(all.size()+PAGE_SIZE-1)/PAGE_SIZE);page=Math.min(page,pages-1);
        List<BossNoticeRow> selected=all.subList(page*PAGE_SIZE,Math.min(all.size(),(page+1)*PAGE_SIZE));
        Message m=null;try{
            m=new Message(COMMAND);m.writer().writeInt(request);m.writer().writeByte(kind);m.writer().writeShort(page);m.writer().writeShort(pages);
            m.writer().writeLong(now);m.writer().writeUTF(kind==1&&!historyReady?"Lịch sử SQL đang tải; đang hiển thị dữ liệu gần đây.":"");m.writer().writeShort(selected.size());
            for(BossNoticeRow row:selected)write(m,row,run);
            p.sendMessage(m);
        }catch(Exception failure){log(failure);}finally{if(m!=null)m.cleanup();}
    }
    public static void write(Message m,BossNoticeRow row,String run)throws java.io.IOException{
        var w=m.writer();w.writeUTF(row.spawnId());w.writeUTF(row.bossName());w.writeUTF(row.mapName());w.writeUTF(row.killerName());
        boolean priorRun=!row.serverRun().equals(run);
        w.writeUTF(priorRun&&row.status().equals("ALIVE")?"PREVIOUS_RUN":row.status());
        w.writeLong(row.appearedAt());w.writeLong(row.defeatedAt());w.writeLong(priorRun?0:row.nextSpawnAt());w.writeUTF(priorRun?"UNKNOWN":row.etaKind());
    }
}
