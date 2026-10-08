package nro.models.event;
import java.util.*;
import nro.models.item.Item;
/** Immutable reads do not borrow SQL or take lifecycle locks. */
public final class EventItemPolicy {
 private static volatile Map<String,Boolean> enabled=Map.of();
 private static final java.util.concurrent.locks.ReentrantReadWriteLock ADMISSION = new java.util.concurrent.locks.ReentrantReadWriteLock(true);
 public static final class Permit implements AutoCloseable {
  private boolean closed;
  private Permit(){if(!ADMISSION.readLock().tryLock())ADMISSION.readLock().lock();}
  public void close(){if(!closed){closed=true;ADMISSION.readLock().unlock();}}
 }
 /** Acquire inside the existing player monitor. Publishers never acquire player monitors. */
 public static Permit admit(){return new Permit();}
 public static void publish(Map<String,Boolean> states){
  if(ADMISSION.getReadHoldCount()>0)throw new IllegalStateException("Cannot publish from an admitted mutation");
  Map<String,Boolean> next=Map.copyOf(states);
  ADMISSION.writeLock().lock();try{enabled=next;}finally{ADMISSION.writeLock().unlock();}
 }
 public static boolean isEnabled(String key){return Boolean.TRUE.equals(enabled.get(key));}
 public static String eventForConsumable(int id){for(var d:EventCatalog.all())for(int c:d.consumables())if(c==id)return d.key();return null;}
 public static boolean isBlocked(int id){String key=eventForConsumable(id);return key!=null&&!isEnabled(key);}
 public static boolean isBlocked(Item item){return item!=null&&item.template!=null&&isBlocked(item.template.id);}
 public static Set<Integer> blockedIds(){Set<Integer> out=new HashSet<>();for(var d:EventCatalog.all())if(!isEnabled(d.key()))for(int id:d.consumables())out.add(id);return Set.copyOf(out);}
}
