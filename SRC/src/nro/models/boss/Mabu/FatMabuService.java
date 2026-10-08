package nro.models.boss.Mabu;

import java.util.*;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.map.Zone;
import nro.models.map.service.MapService;
import nro.models.server.ServerNotify;

/** Retains a failed generation until map, manager and object cleanup all complete. */
public final class FatMabuService implements FatMabuLifecycle.Backend {
    private static class Holder{static final FatMabuService INSTANCE=new FatMabuService();}
    public static FatMabuService gI(){return Holder.INSTANCE;}
    private final FatMabuLifecycle life=new FatMabuLifecycle(this,System::currentTimeMillis);
    private volatile FatMabuTournament boss;
    public void tick(){life.tick();}
    public FatMabuTournament current(){return boss;}
    public FatMabuLifecycle.Phase phase(){return life.phase();}
    public boolean spawn(){
        if(boss!=null){remove();if(boss!=null)return false;}
        var map=MapService.gI().getMapById(52);
        if(map==null||map.mapWidth<=0)return false;
        List<Zone> choices=new ArrayList<>(map.zones);Collections.shuffle(choices);
        for(Zone candidate:choices){
            if(candidate.getNumOfPlayers()>=candidate.maxPlayer||!candidate.getBosses().isEmpty())continue;
            try{
                boss=new FatMabuTournament();boss.prepare(candidate);
                ServerNotify.gI().notify("Mabu mập đã xuất hiện tại "+map.mapName);return true;
            }catch(Exception failure){throw new IllegalStateException("Mabu spawn failed",failure);}
        }
        return false;
    }
    public void chat(){boss.chat("Mabu tới rồi! Ai muốn thử sức nào?");}
    public void activate(){boss.activate();}
    public boolean alive(){FatMabuTournament b=boss;return b!=null&&!b.beforeDispose&&b.zone!=null&&!b.isDie();}
    public boolean playersPresent(){FatMabuTournament b=boss;return b!=null&&b.zone!=null&&b.zone.getNumOfPlayers()>0;}
    public void remove(){
        FatMabuTournament old=boss;if(old==null)return;
        synchronized(old){
            Zone source=old.zone;RuntimeException failure=null;
            try{old.leaveMap();}catch(RuntimeException ex){failure=ex;}
            try{if(source!=null)source.removePlayer(old);if(old.zone!=null&&old.zone!=source)old.zone.removePlayer(old);old.zone=null;}
            catch(RuntimeException ex){if(failure==null)failure=ex;else failure.addSuppressed(ex);}
            try{BossManager.gI().removeBoss(old);}catch(RuntimeException ex){if(failure==null)failure=ex;else failure.addSuppressed(ex);}
            if(old.zone==null&&(source==null||!source.getHumanoids().contains(old)&&!source.getBosses().contains(old))){
                try{old.dispose();if(!BossManager.gI().getBosses().contains(old))boss=null;}
                catch(RuntimeException ex){if(failure==null)failure=ex;else failure.addSuppressed(ex);}
            }
            if(boss!=null&&failure==null)failure=new IllegalStateException("Mabu cleanup incomplete");
            if(failure!=null)throw failure;
        }
    }
}
