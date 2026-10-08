package nro.models.event;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import nro.models.player.Player;
import nro.models.database.PlayerDAO;
import nro.models.services.LiveSyncCommandGate;
/** Retain detached logout objects until cleanup and the full save are durable. */
public final class EventCleanupLogoutService {
 private static final Map<Long,Player> RETIRING=new ConcurrentHashMap<>();
 public static void retainIfPending(Player player){
  if(EventCleanupService.pending(player.id))RETIRING.put(player.id,player);
 }
 public static boolean pending(long id){return RETIRING.containsKey(id);}
 public static boolean finalizing(Player player){return Thread.holdsLock(player)&&player.beforeDispose&&player.liveLogoutCleanupCompleted&&RETIRING.get(player.id)==player&&!EventCleanupService.pending(player.id);}
 interface Reconciler {boolean reconcile(Player player)throws Exception;}
 public static void drain()throws Exception {drain(EventCleanupService::reconcilePending,PlayerDAO::savePendingEventLogout);}
 static void drain(Reconciler reconciler,java.util.function.Predicate<Player> saver)throws Exception {
  for(Player player:new ArrayList<>(RETIRING.values()))synchronized(player){
   if(!player.liveLogoutCleanupCompleted||!reconciler.reconcile(player))continue;
   if(!saver.test(player))continue;
   int account=player.session.userId;
   RETIRING.remove(player.id,player);
   player.dispose();player.liveLogoutCompleted=true;
   LiveSyncCommandGate.retired(player.id,account);
  }
 }
 public static boolean empty(){return RETIRING.isEmpty();}
}
