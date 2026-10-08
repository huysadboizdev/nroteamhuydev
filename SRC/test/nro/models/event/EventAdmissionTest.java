package nro.models.event;
import java.util.*;
import java.util.concurrent.*;
public final class EventAdmissionTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static void queuedPublisherCannotTrapEntityReaders() throws Exception {
  Object entityA=new Object(),entityB=new Object();
  CountDownLatch aEntered=new CountDownLatch(1),bHeld=new CountDownLatch(1),bAcquire=new CountDownLatch(1),aDone=new CountDownLatch(1),bDone=new CountDownLatch(1);
  Thread a=new Thread(()->{synchronized(entityA){try(var permit=EventItemPolicy.admit()) {
   aEntered.countDown();try{bHeld.await();bAcquire.await();}catch(InterruptedException e){throw new RuntimeException(e);}
   synchronized(entityB){}aDone.countDown();
  }}});
  Thread b=new Thread(()->{synchronized(entityB){bHeld.countDown();try{bAcquire.await();}catch(InterruptedException e){throw new RuntimeException(e);}
   try(var permit=EventItemPolicy.admit()){}bDone.countDown();
  }});
  a.setDaemon(true);b.setDaemon(true);a.start();check(aEntered.await(2,TimeUnit.SECONDS),"A admitted");b.start();check(bHeld.await(2,TimeUnit.SECONDS),"B held");
  Thread writer=new Thread(()->EventItemPolicy.publish(Map.of("summer",false)));writer.setDaemon(true);writer.start();
  long deadline=System.nanoTime()+2_000_000_000L;
  while(writer.getState()!=Thread.State.WAITING && System.nanoTime()<deadline)Thread.yield();
  check(writer.getState()==Thread.State.WAITING,"publisher queued behind A");bAcquire.countDown();
  check(bDone.await(1,TimeUnit.SECONDS)&&aDone.await(1,TimeUnit.SECONDS),"queued publisher cannot deadlock two entity monitors");writer.join(2000);check(!writer.isAlive(),"publisher drains");
 }
 public static void main(String[]args)throws Exception{
  queuedPublisherCannotTrapEntityReaders();
  EventItemPolicy.publish(Map.of("summer",true));
  CountDownLatch started=new CountDownLatch(1),published=new CountDownLatch(1);
  Thread publisher=new Thread(()->{started.countDown();EventItemPolicy.publish(Map.of("summer",false));published.countDown();});
  try(var permit=EventItemPolicy.admit()){
   publisher.start();check(started.await(2,TimeUnit.SECONDS),"publisher started");
   check(!published.await(100,TimeUnit.MILLISECONDS),"publish waits admitted exchange");
   check(EventItemPolicy.isEnabled("summer")&&!EventItemPolicy.isBlocked(1614),"exchange sees stable inventory gates");
   try{EventItemPolicy.publish(Map.of());throw new AssertionError("upgrade must fail without deadlock");}catch(IllegalStateException expected){}
  }
  check(published.await(2,TimeUnit.SECONDS),"publish drains after transaction");publisher.join();
  try(var permit=EventItemPolicy.admit()){check(EventItemPolicy.isBlocked(1614),"stale callback denied after cutoff");}
  nro.models.item.ItemTime timers=new nro.models.item.ItemTime(null);
  timers.isUseNuocMia1=true;timers.isUseNuocMia2=true;timers.isUseNuocMia3=true;timers.isUseCuongNo=true;
  check(EventRuntimeService.clearTemporarySummerBuffs(timers),"temporary water bonus removed");
  check(!timers.isUseNuocMia1&&!timers.isUseNuocMia2&&!timers.isUseNuocMia3&&timers.isUseCuongNo,"ordinary item buffs preserved");
  check(!EventRuntimeService.clearTemporarySummerBuffs(timers),"clear is idempotent");
  System.out.println("Event admission behavior PASS");
 }
}
