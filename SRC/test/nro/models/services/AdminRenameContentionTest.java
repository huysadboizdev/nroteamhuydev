package nro.models.services;
import java.lang.reflect.*;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import nro.models.player.Player;
import nro.models.item.Item;
import static nro.models.services.PinkFixture.*;
public class AdminRenameContentionTest {
 public static void main(String[] args)throws Exception {
  Thread.setDefaultUncaughtExceptionHandler((t,e)->{e.printStackTrace();System.exit(1);});
  Player player=player(959);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger saves=new AtomicInteger();AtomicReference<Throwable> failure=new AtomicReference<>();AtomicInteger successes=new AtomicInteger();
  AdminPlayerActions actions=new AdminPlayerActions(()->{throw new AssertionError("applied operation must not reread SQL");},p->{int call=saves.incrementAndGet();if(call==1){entered.countDown();try{check(release.await(5,TimeUnit.SECONDS),"saver release timeout");}catch(InterruptedException e){throw new AssertionError(e);}}return true;});
  Class<?> type=Class.forName("nro.models.services.AdminPlayerActions$RenamePending");Constructor<?> ctor=type.getDeclaredConstructor(AdminPlayerActions.class,Player.class,int.class,String.class,String.class,Item.class);ctor.setAccessible(true);Object op=ctor.newInstance(actions,player,959,"old959","new959",null);Field applied=type.getDeclaredField("applied");applied.setAccessible(true);applied.setBoolean(op,true);Field registry=AdminPlayerActions.class.getDeclaredField("RENAME_PENDING");registry.setAccessible(true);@SuppressWarnings("unchecked") Map<Player,Object> pending=(Map<Player,Object>)registry.get(null);pending.put(player,op);
  Runnable recover=()->{try{if(actions.reconcileRename(player))successes.incrementAndGet();}catch(Throwable e){failure.set(e);}};Thread first=new Thread(recover,"first recovery"),second=new Thread(recover,"contending recovery");first.start();check(entered.await(5,TimeUnit.SECONDS),"first did not enter saver");second.start();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(second.getState()!=Thread.State.BLOCKED&&System.nanoTime()<deadline)Thread.sleep(1);boolean blocked=second.getState()==Thread.State.BLOCKED;release.countDown();first.join(5000);second.join(5000);
  check(blocked&&!first.isAlive()&&!second.isAlive()&&failure.get()==null,"recovery contention not established or thread failed");check(successes.get()==2&&!AdminPlayerActions.hasRenamePending(player),"resolved contender incorrectly suppressed save/disposal");check(saves.get()==1,"resolved contender invoked full saver twice");System.out.println("PASS AdminRenameContentionTest one finalizing save, both recovery callers succeed");System.exit(0);
 }
}
