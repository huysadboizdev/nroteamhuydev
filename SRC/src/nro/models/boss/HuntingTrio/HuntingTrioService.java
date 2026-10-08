package nro.models.boss.HuntingTrio;

import java.util.*;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.boss.Boss_Manager.BrolyManager;
import nro.models.map.Zone;
import nro.models.map.service.MapService;
import nro.models.server.ServerNotify;
import nro.models.utils.Logger;
import nro.models.utils.Util;

public final class HuntingTrioService {
    private static class Holder {static final HuntingTrioService INSTANCE=new HuntingTrioService();}
    public static HuntingTrioService gI(){return Holder.INSTANCE;}
    private final List<HuntingTrioLifecycle> groups=new ArrayList<>();
    private HuntingTrioService() {
        for(int i=0;i<2;i++) {
            Backend backend=new Backend(i);HuntingTrioLifecycle state=new HuntingTrioLifecycle(backend,System::currentTimeMillis);
            backend.state=state;groups.add(state);
        }
    }
    public void tick() {
        for(HuntingTrioLifecycle group:groups)try{group.tick();}
        catch(Exception failure){Logger.logException(HuntingTrioService.class,failure,"Hunting group update");}
    }
    public List<HuntingTrioLifecycle.Phase> phases(){return groups.stream().map(HuntingTrioLifecycle::phase).toList();}
    private static final class Backend implements HuntingTrioLifecycle.Backend {
        final int kind;
        final HuntingTrioBoss[] members=new HuntingTrioBoss[3];
        HuntingTrioLifecycle state;
        Zone zone;
        Backend(int kind){this.kind=kind;}
        public boolean spawn() {
            // A failed close may leave this generation attached. Retire it before allocating replacements.
            if(zone!=null||Arrays.stream(members).anyMatch(Objects::nonNull)) {
                try {removeAll();}
                catch(RuntimeException failure) {
                    Logger.logException(HuntingTrioService.class,failure,"Hunting group cleanup retry");
                    return false;
                }
                if(zone!=null||Arrays.stream(members).anyMatch(Objects::nonNull))return false;
            }
            List<Zone> choices=new ArrayList<>();
            for(int id:HuntingTrioCatalog.MAPS) {
                var map=MapService.gI().getMapById(id);
                if(map!=null&&map.mapWidth>0)for(Zone candidate:map.zones)
                    if(candidate.getBosses().isEmpty())choices.add(candidate);
            }
            Collections.shuffle(choices);
            for(Zone candidate:choices)if(BrolyManager.gI().zones().tryReserveExternal(candidate,this)) {zone=candidate;break;}
            if(zone==null)return false;
            try {
                for(int i=0;i<3;i++)members[i]=new HuntingTrioBoss(kind,i,state);
                int width=zone.map.mapWidth;
                int center=width>240?Util.nextInt(100,width-100):width/2;
                for(int i=0;i<3;i++)members[i].prepare(zone,center+(i-1)*40);
                ServerNotify.gI().notify((kind==0?"Bộ ba Sói":"Tiểu đội Pi Láp")+" đã xuất hiện tại "+zone.map.mapName);
                return true;
            } catch(Exception failure) {
                try {removeAll();} catch(RuntimeException cleanup) {failure.addSuppressed(cleanup);}
                throw new IllegalStateException("Failed to prepare hunting trio",failure);
            }
        }
        public void chat(int index){members[index].chat(HuntingTrioCatalog.chat(kind,index));}
        public void activate(){for(HuntingTrioBoss member:members)member.activateTogether();}
        public boolean intact(){return zone!=null&&Arrays.stream(members).allMatch(m->m!=null&&m.zone==zone&&!m.isDie());}
        public boolean playersPresent(){return zone!=null&&zone.getNumOfPlayers()>0;}
        public void removeMember(int index) {
            HuntingTrioBoss member=members[index];
            if(member==null)return;
            synchronized(member) {
                RuntimeException failure=null;
                Zone old=member.zone;
                try {member.leaveMap();}
                catch(RuntimeException problem) {failure=problem;}
                finally {
                    if(old!=null)failure=detach(old,member,failure);
                    Zone current=member.zone;
                    if(current!=null&&current!=old)failure=detach(current,member,failure);
                }
                // Map, manager and object cleanup are independent: one failure must not skip the others.
                try {BossManager.gI().removeBoss(member);}
                catch(RuntimeException problem) {failure=append(failure,problem);}
                boolean detached=member.zone==null&&!contains(old,member);
                boolean disposed=false;
                if(detached)try {member.dispose();disposed=true;}
                catch(RuntimeException problem) {failure=append(failure,problem);}
                if(detached&&disposed&&!BossManager.gI().getBosses().contains(member))members[index]=null;
                if(!detached&&failure==null)failure=new IllegalStateException("Hunting member remains attached after cleanup");
                if(failure!=null)throw failure;
            }
        }
        private static RuntimeException detach(Zone old,HuntingTrioBoss member,RuntimeException failure) {
            try {old.removePlayer(member);}
            catch(RuntimeException problem) {failure=append(failure,problem);}
            finally {if(!contains(old,member)&&member.zone==old)member.zone=null;}
            return failure;
        }
        private static boolean contains(Zone zone,HuntingTrioBoss member) {
            return zone!=null&&(zone.getBosses().contains(member)||zone.getHumanoids().contains(member)
                    ||zone.getNotBosses().contains(member)||zone.getPlayers().contains(member));
        }
        private static RuntimeException append(RuntimeException first,RuntimeException next) {
            if(first==null)return next;
            if(first!=next)first.addSuppressed(next);
            return first;
        }
        public void removeAll() {
            RuntimeException failure=null;
            for(int i=0;i<3;i++)try{removeMember(i);}catch(RuntimeException problem){if(failure==null)failure=problem;else failure.addSuppressed(problem);}
            boolean attached=false;
            for(HuntingTrioBoss member:members)if(member!=null&&(member.zone!=null||contains(zone,member)))attached=true;
            if(zone!=null&&!attached) {BrolyManager.gI().zones().releaseExternal(zone,this);zone=null;}
            if(failure!=null)throw failure;
        }
    }
}
