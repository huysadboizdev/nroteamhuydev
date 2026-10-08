package nro.models.event;

import java.util.*;
import java.util.function.Predicate;
import java.io.*;
import java.nio.file.*;
import nro.models.boss.*;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.map.service.MapService;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.services.Service;
import nro.models.utils.Logger;

/** Single owner of fully constructed seasonal trees; reconcile and ticks never overlap. */
public final class EventRuntimeService {
    public interface Handle { void update(); void retire() throws Exception; }
    public interface Backend {
        boolean ready(String key);
        List<Handle> create(String key) throws Exception;
        default void visibility() { }
    }
    private static final Map<nro.models.map.ItemMap,String> GROUND = Collections.synchronizedMap(new WeakHashMap<>());
    public static boolean canPick(nro.models.map.ItemMap item) {
        if(item==null||item.itemTemplate==null||EventItemPolicy.isBlocked(item.itemTemplate.id))return false;
        String key=GROUND.get(item);
        return key==null||EventItemPolicy.isEnabled(key);
    }
    public static void dropSeasonal(nro.models.map.Zone zone,nro.models.map.ItemMap item,String key) {
        GROUND.put(item,key);
        if(!EventItemPolicy.isEnabled(key)) { nro.models.map.service.ItemMapService.gI().removeItemMapAndSendClient(item); return; }
        Service.gI().dropItemMap(zone,item);
    }
    private static void removeDisabledGround() {
        for(var map:Manager.MAPS)for(var zone:map.zones)for(var item:zone.getItemsSnapshot())
            if(item.itemTemplate!=null && EventItemPolicy.isBlocked(item.itemTemplate.id))
                nro.models.map.service.ItemMapService.gI().removeItemMapAndSendClient(item);
        Map<nro.models.map.ItemMap,String> snapshot;
        synchronized(GROUND){snapshot=new IdentityHashMap<>(GROUND);}
        for(var entry:snapshot.entrySet()) if(!EventItemPolicy.isEnabled(entry.getValue())) {
            var item=entry.getKey();
            if(item.zone!=null && item.itemTemplate!=null && item.zone.getItemsSnapshot().contains(item))
                nro.models.map.service.ItemMapService.gI().removeItemMapAndSendClient(item);
            GROUND.remove(item);
        }
    }
    private static final List<String> THEMES = List.of("hung_vuong","summer","halloween","christmas","lunar_new_year","trung_thu");
    private static final EventRuntimeService INSTANCE = new EventRuntimeService(new LiveBackend(), EventItemPolicy::isEnabled);
    private final Backend backend;
    private final Predicate<String> enabled;
    private final Map<String,List<Handle>> active = new LinkedHashMap<>();
    private volatile boolean running;
    private Thread worker;
    public EventRuntimeService(Backend backend, Predicate<String> enabled) { this.backend=backend; this.enabled=enabled; }
    public static EventRuntimeService gI() { return INSTANCE; }
    public boolean ready(String key) { return THEMES.contains(key) && backend.ready(key); }
    public synchronized void reconcile() throws Exception { reconcileAgainst(enabled); }
    public synchronized void reconcile(Map<String,Boolean> desired) throws Exception {
        Map<String,Boolean> snapshot=Map.copyOf(desired);
        reconcileAgainst(key->Boolean.TRUE.equals(snapshot.get(key)));
    }
    public void refreshVisibility() { backend.visibility(); }
    private void reconcileAgainst(Predicate<String> requested) throws Exception {
        for (String key: THEMES) {
            if (!requested.test(key)) {
                List<Handle> handles=active.get(key);
                if(handles!=null) { retire(handles); active.remove(key); }
            } else if (!active.containsKey(key)) {
                if (!ready(key)) throw new IllegalStateException("Event resources not ready: "+key);
                List<Handle> handles=backend.create(key);
                active.put(key,new ArrayList<>(handles));
            }
        }
        removeDisabledGround();
    }
    private void retire(List<Handle> handles) throws Exception {
        Exception failure=null;
        for(Iterator<Handle> it=handles.iterator();it.hasNext();) {
            try { it.next().retire(); it.remove(); } catch(Exception e) { if(failure==null)failure=e; else failure.addSuppressed(e); }
        }
        if(failure!=null)throw failure;
    }
    public synchronized int activeCount(String key) { return active.getOrDefault(key,List.of()).size(); }
    public synchronized void tick() {
        for(var entry: active.entrySet()) if(enabled.test(entry.getKey()))
            for(Handle handle:entry.getValue()) try { handle.update(); } catch(Exception e) { Logger.logException(getClass(),e,"Seasonal boss update"); }
    }
    public synchronized void start() {
        if(running)return; running=true;
        worker=new Thread(()->{while(running){tick();try{Thread.sleep(250);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}}},"seasonal-runtime");
        worker.setDaemon(true);worker.start();
    }
    public void stop() {
        Thread stopped;
        synchronized(this){running=false;stopped=worker;worker=null;}
        if(stopped!=null){stopped.interrupt();try{stopped.join(3000);}catch(InterruptedException e){Thread.currentThread().interrupt();}}
    }
    public static boolean clearTemporarySummerBuffs(nro.models.item.ItemTime timers) {
        if(timers==null)return false;
        boolean changed=timers.isUseNuocMia1||timers.isUseNuocMia2||timers.isUseNuocMia3;
        timers.isUseNuocMia1=false;timers.isUseNuocMia2=false;timers.isUseNuocMia3=false;
        return changed;
    }
    public void syncNpcVisibility(Player player) {
        if(player==null||player.zone==null)return;
        Service.gI().sendHideNpc(player,51,!EventItemPolicy.isEnabled("hung_vuong"));
        if(EventItemPolicy.isEnabled("hung_vuong")&&player.DuaHauEgg!=null&&player.zone.map.mapId==21+player.gender)player.DuaHauEgg.sendDuaHauEgg();
        synchronized(player) {
            if(!EventItemPolicy.isEnabled("summer") && clearTemporarySummerBuffs(player.itemTime) && player.nPoint!=null) {
                player.nPoint.calPoint();Service.gI().point(player);
            }
        }
        for(var definition:EventCatalog.all()) for(int npcId:definition.npcIds())
            Service.gI().sendHideNpc(player,npcId,!EventItemPolicy.isEnabled(definition.key()));
    }
    private static BossData[] data(String key) {
        return switch(key) {
            case "hung_vuong" -> new BossData[]{BossesData.THUY_TINH,BossesData.SON_TINH};
            case "halloween" -> new BossData[]{BossesData.BI_MA,BossesData.MA_TROI,BossesData.DOI};
            case "christmas" -> new BossData[]{BossesData.ONG_GIA_NOEL};
            case "lunar_new_year" -> new BossData[]{BossesData.LAN_CON};
            default -> new BossData[0];
        };
    }
    private static Map<Integer,Integer> partTypes() throws IOException {
        Map<Integer,Integer> types=new HashMap<>();
        try(DataInputStream in=new DataInputStream(Files.newInputStream(Path.of("data/update_data/part")))) {
            int count=in.readUnsignedShort();
            for(int id=0;id<count;id++) {
                int type=in.readUnsignedByte();int frames=switch(type){case 0->3;case 1->17;case 2->14;default->throw new IOException("Invalid part type");};
                types.put(id,type);in.skipNBytes(frames*4L);
            }
        }
        return types;
    }
    private static final class LiveBackend implements Backend {
        public boolean ready(String key) {
            EventCatalog.Definition definition=EventCatalog.get(key);
            if(definition==null||!definition.supported())return false;
            try {
                for(int id:definition.keptRewards())if(Manager.ITEM_TEMPLATES.stream().noneMatch(t->t.id==id))return false;
                for(int id:definition.consumables())if(Manager.ITEM_TEMPLATES.stream().noneMatch(t->t.id==id))return false;
                for(int id:definition.npcIds()) {
                    if(Manager.NPC_TEMPLATES.stream().noneMatch(t->t.id==id))return false;
                    if(Manager.MAPS.stream().noneMatch(m->m.npcs.stream().anyMatch(n->n.tempId==id)))return false;
                }
                Map<Integer,Integer> types=partTypes();
                for(BossData boss:data(key)) {
                    for(int id:boss.getMapJoin()){var map=MapService.gI().getMapById(id);if(map==null||map.zones.isEmpty()||map.mapWidth<=0)return false;}
                    short[] outfit=boss.getOutfit();
                    if(outfit.length<3||!Objects.equals(types.get((int)outfit[0]),0)||!Objects.equals(types.get((int)outfit[1]),1)||!Objects.equals(types.get((int)outfit[2]),2))return false;
                }
                return !key.equals("trung_thu");
            }catch(Exception e){return false;}
        }
        public List<Handle> create(String key) throws Exception {
            List<Handle> result=new ArrayList<>();
            try {
                var definition=EventCatalog.get(key);
                for(int id:definition.bossIds()) {
                    int count=key.equals("christmas")?30:10;
                    for(int i=0;i<count;i++) {
                        Boss root=BossManager.gI().createBoss(id);
                        if(root==null)throw new IllegalStateException("Cannot construct seasonal boss "+id);
                        Set<Boss> members=Collections.newSetFromMap(new IdentityHashMap<>()); collect(root,members);
                        for(Boss boss:members){boss.bindSeasonalEvent(key); result.add(new Handle(){public void update(){synchronized(boss){try(EventItemPolicy.Permit permit=EventItemPolicy.admit()){boss.update();}}}public void retire(){boss.retireSeasonalEvent();}});}
                    }
                }
                return result;
            }catch(Exception e){for(Handle h:result)try{h.retire();}catch(Exception cleanup){e.addSuppressed(cleanup);}throw e;}
        }
        private void collect(Boss boss,Set<Boss> members){if(boss==null||!members.add(boss))return;if(boss.bossAppearTogether!=null)for(Boss[] level:boss.bossAppearTogether)if(level!=null)for(Boss child:level)collect(child,members);}
        public void visibility(){for(var map:Manager.MAPS)for(var zone:map.zones)for(Player player:zone.getPlayersSnapshot())INSTANCE.syncNpcVisibility(player);}
    }
}
