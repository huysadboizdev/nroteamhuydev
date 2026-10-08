package nro.models.event;

import java.util.Map;
import java.util.concurrent.*;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.services.*;

/** Pending SQL publication can begin after the controller's first check. */
public final class EventNpcPendingRaceTest {
    public static void main(String[]args)throws Exception {
        EventItemPolicy.publish(Map.of("hung_vuong",true,"summer",true));
        ExecutorService worker=Executors.newSingleThreadExecutor();
        try {
            for(Class<? extends Npc> type:java.util.List.of(VuaHung.class,NoiBanh.class,XeNuocMia.class)) {
                Npc npc=PinkFixture.blank(type);Player player=new Player();player.id=991;
                player.inventory.gold=1234;player.inventory.gem=567;
                CountDownLatch requestStarted=new CountDownLatch(1);Future<?> action;
                synchronized(player) {
                    action=worker.submit(()->{requestStarted.countDown();npc.confirmMenu(player,0);});
                    if(!requestStarted.await(2,TimeUnit.SECONDS))throw new AssertionError("NPC request did not start");
                    // Request is waiting for the player monitor when commit becomes unresolved.
                    LiveSyncCommandGate.mark(player.id,0);
                }
                try {
                    action.get(2,TimeUnit.SECONDS);
                    // No map was loaded: even canOpenNpc must be skipped while pending.
                    if(player.inventory.gold!=1234||player.inventory.gem!=567)
                        throw new AssertionError("Pending NPC charged currency: "+type.getSimpleName());
                }finally{LiveSyncCommandGate.clear(player.id,0);}
            }
            System.out.println("EventNpcPendingRaceTest passed");
        }finally{worker.shutdownNow();}
    }
}
