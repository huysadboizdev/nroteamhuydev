package nro.models.boss.HuntingTrio;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.LongSupplier;

/** Tick-owned group state; death callbacks never take the tick lock. */
public final class HuntingTrioLifecycle {
    public enum Phase { WAITING, TALKING, ACTIVE, COOLDOWN }
    public interface Backend {
        boolean spawn(); void chat(int member); void activate();
        void removeMember(int member); void removeAll();
        boolean playersPresent(); boolean intact();
    }
    private final Backend backend;
    private final LongSupplier clock;
    private final AtomicInteger defeated = new AtomicInteger();
    private volatile Phase phase = Phase.WAITING;
    private long nextAction, emptySince = -1;
    private int conversation, removed;
    public HuntingTrioLifecycle(Backend backend,LongSupplier clock) { this.backend=backend;this.clock=clock; }
    public Phase phase() { return phase; }
    public void defeated(int member) {
        if(member<0||member>2)throw new IllegalArgumentException("member");
        defeated.getAndUpdate(mask -> mask | (1<<member));
    }
    public synchronized void tick() {
        long now=clock.getAsLong();
        if(phase==Phase.WAITING||phase==Phase.COOLDOWN) {
            if(now<nextAction)return;
            try {
                if(!backend.spawn()) { nextAction=now+10_000;return; }
                defeated.set(0);removed=0;emptySince=-1;conversation=0;
                phase=Phase.TALKING;nextAction=now;
            } catch(RuntimeException failure) {
                phase=Phase.COOLDOWN;nextAction=now+10_000;
                try { backend.removeAll(); } catch(RuntimeException cleanup) { failure.addSuppressed(cleanup); }
                throw failure;
            }
            return;
        }
        if(phase==Phase.TALKING) {
            if(!backend.intact()) { close(now,10_000);return; }
            if(now<nextAction)return;
            if(conversation<3) { backend.chat(conversation++);nextAction=now+2_000; }
            else { backend.activate();phase=Phase.ACTIVE; }
            return;
        }
        int mask=defeated.get();
        for(int i=0;i<3;i++)if((mask&(1<<i))!=0&&(removed&(1<<i))==0) {
            backend.removeMember(i);removed|=1<<i;
        }
        if(mask==7) { close(now,600_000);return; }
        if(backend.playersPresent())emptySince=-1;
        else if(emptySince<0)emptySince=now;
        else if(now-emptySince>=300_000)close(now,600_000);
    }
    private void close(long now,long delay) {
        // Close admission before removing the first member.
        phase=Phase.COOLDOWN;nextAction=now+delay;backend.removeAll();
    }
}
