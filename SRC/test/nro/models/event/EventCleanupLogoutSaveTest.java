package nro.models.event;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import nro.models.player.Player;
import nro.models.network.MySession;
import nro.models.services.PinkFixture;
import nro.models.services.LiveSyncCommandGate;
public final class EventCleanupLogoutSaveTest {
 public static final class Owner extends Player {boolean disposed;public void dispose(){disposed=true;}}
 static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
 @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception {
  Owner player=PinkFixture.blank(Owner.class);player.id=8042;player.beforeDispose=true;
  player.session=PinkFixture.blank(MySession.class);player.session.userId=8043;
  Field field=EventCleanupLogoutService.class.getDeclaredField("RETIRING");field.setAccessible(true);
  Map<Long,Player> retiring=(Map<Long,Player>)field.get(null);retiring.put(player.id,player);
  LiveSyncCommandGate.retire(player.id,8043);AtomicInteger saves=new AtomicInteger();
  EventCleanupLogoutService.drain(p->true,p->{saves.incrementAndGet();return true;});
  check(saves.get()==0&&!player.disposed,"wait for logout map cleanup");
  player.liveLogoutCleanupCompleted=true;
  EventCleanupLogoutService.drain(p->false,p->{saves.incrementAndGet();return true;});
  check(saves.get()==0&&LiveSyncCommandGate.isRetiringId(player.id),"unresolved commit cannot save stale inventory");
  EventCleanupLogoutService.drain(p->true,p->{saves.incrementAndGet();return false;});
  check(saves.get()==1&&!player.disposed&&EventCleanupLogoutService.pending(player.id),"failed save retains detached writer");
  EventCleanupLogoutService.drain(p->true,p->{check(EventCleanupLogoutService.finalizing(p),"save under player monitor");saves.incrementAndGet();return true;});
  check(saves.get()==2&&player.disposed&&player.liveLogoutCompleted&&!EventCleanupLogoutService.pending(player.id),"successful save disposes once");
  check(!LiveSyncCommandGate.isRetiringId(player.id)&&!LiveSyncCommandGate.isRetiringAccount(8043),"reconnect released only after save");
  EventCleanupLogoutService.drain(p->true,p->{throw new AssertionError("duplicate save");});
  System.out.println("EventCleanupLogoutSaveTest PASS");
 }
}
