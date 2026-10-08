package nro.models.boss.Mabu;

import java.util.function.LongSupplier;

/** Single tick owns admission and deadlines; death callbacks never acquire this lock. */
public final class FatMabuLifecycle {
    public enum Phase { WAITING,TALKING,ACTIVE,COOLDOWN }
    public interface Backend {
        boolean spawn();void chat();void activate();void remove();
        boolean alive();boolean playersPresent();
    }
    private final Backend backend;
    private final LongSupplier clock;
    private volatile Phase phase=Phase.WAITING;
    private long next,deadline,emptySince=-1;
    private boolean cleanupPending;
    public FatMabuLifecycle(Backend backend,LongSupplier clock){this.backend=backend;this.clock=clock;}
    public Phase phase(){return phase;}
    public synchronized void tick(){
        long now=clock.getAsLong();
        if(cleanupPending){backend.remove();cleanupPending=false;next=now+600_000;return;}
        if(phase==Phase.WAITING||phase==Phase.COOLDOWN){
            if(now<next)return;
            try {
                if(!backend.spawn()){next=now+10_000;return;}
                phase=Phase.TALKING;next=now+2000;emptySince=-1;backend.chat();
            } catch(RuntimeException failure){
                phase=Phase.COOLDOWN;cleanupPending=true;
                try{backend.remove();cleanupPending=false;next=now+10_000;}catch(RuntimeException cleanup){failure.addSuppressed(cleanup);}
                throw failure;
            }
            return;
        }
        if(!backend.alive()){close(now);return;}
        if(phase==Phase.TALKING){
            if(now>=next){backend.activate();phase=Phase.ACTIVE;deadline=now+1_200_000;}
            return;
        }
        if(now>=deadline){close(now);return;}
        if(backend.playersPresent())emptySince=-1;
        else if(emptySince<0)emptySince=now;
        else if(now-emptySince>=300_000)close(now);
    }
    private void close(long now){
        phase=Phase.COOLDOWN;cleanupPending=true;
        backend.remove();cleanupPending=false;next=now+600_000;
    }
}
