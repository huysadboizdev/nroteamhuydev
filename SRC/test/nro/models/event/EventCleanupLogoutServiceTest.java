package nro.models.event;
import java.lang.reflect.*;
import java.util.*;
import nro.models.player.Player;
import nro.models.services.LiveSyncCommandGate;
/** Isolated identity/retention guards; never enters SQL cleanup or save. */
public final class EventCleanupLogoutServiceTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 @SuppressWarnings("unchecked") static <T>T staticField(Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return(T)f.get(null);}
 static Player player(long id)throws Exception{
  Class<?> unsafeType=Class.forName("sun.misc.Unsafe");Field f=unsafeType.getDeclaredField("theUnsafe");f.setAccessible(true);
  Player p=(Player)unsafeType.getMethod("allocateInstance",Class.class).invoke(f.get(null),Player.class);p.id=id;p.beforeDispose=true;return p;
 }
 public static void main(String[]args)throws Exception{
  Map<Long,Object> cleanup=staticField(EventCleanupService.class,"PENDING");
  Map<Long,Player> retiring=staticField(EventCleanupLogoutService.class,"RETIRING");
  long id=9_100_080_001L;int account=9_100_001;Player old=player(id),replacement=player(id);
  Class<?> pendingType=Class.forName("nro.models.event.EventCleanupService$Pending");Constructor<?> ctor=pendingType.getDeclaredConstructors()[0];ctor.setAccessible(true);
  Object pending=ctor.newInstance("isolated-operation",account,Map.of(),old);
  try{
   EventCleanupLogoutService.retainIfPending(old);check(!EventCleanupLogoutService.pending(id),"normal logout not retained");
   cleanup.put(id,pending);LiveSyncCommandGate.mark(id,account);LiveSyncCommandGate.retire(id,account);
   EventCleanupLogoutService.retainIfPending(old);check(EventCleanupLogoutService.pending(id),"uncertain SQL cleanup object retained");
   EventCleanupLogoutService.drain();check(retiring.get(id)==old&&!old.liveLogoutCompleted,"unfinished logout not disposed or retired");
   old.dispose();check(retiring.get(id)==old&&LiveSyncCommandGate.hasPendingId(id),"pending receipt protects disposal");
   synchronized(old){check(!EventCleanupLogoutService.finalizing(old),"logout cleanup must finish before save");old.liveLogoutCleanupCompleted=true;check(!EventCleanupLogoutService.finalizing(old),"receipt must resolve before final save");}
   cleanup.remove(id);
   LiveSyncCommandGate.clear(id,account);
   old.inventory=new nro.models.player.Inventory();
   var preservedInventory=old.inventory;
   old.dispose();
   check(old.inventory==preservedInventory,"receipt recovery cannot dispose the retained logout inventory before checked full save");
   check(!EventCleanupLogoutService.finalizing(old),"final save requires original player monitor");
   synchronized(old){check(EventCleanupLogoutService.finalizing(old),"original resolved object can request checked save");}
   replacement.liveLogoutCleanupCompleted=true;
   synchronized(replacement){check(!EventCleanupLogoutService.finalizing(replacement),"replacement identity cannot save stale logout state");}
   check(!nro.models.database.PlayerDAO.savePendingEventLogout(replacement),"unguarded replacement save is denied without SQL");
   check(LiveSyncCommandGate.isRetiringAccount(account),"login remains fenced until successful save");
   System.out.println("Event cleanup logout identity guards PASS");
  }finally{cleanup.remove(id);retiring.remove(id);LiveSyncCommandGate.clear(id,account);LiveSyncCommandGate.retired(id,account);}
 }
}
