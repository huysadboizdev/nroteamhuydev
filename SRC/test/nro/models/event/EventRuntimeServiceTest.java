package nro.models.event;
import java.util.*;
public final class EventRuntimeServiceTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static final class Actor implements EventRuntimeService.Handle {
  int updates,retired;boolean fail;
  public void update(){updates++;}
  public void retire() throws Exception {if(fail)throw new Exception("injected retirement failure");retired++;}
 }
 static final class Backend implements EventRuntimeService.Backend {
  int created;boolean ready=true;List<Actor> actors=new ArrayList<>();
  public boolean ready(String key){return ready;}
  public List<EventRuntimeService.Handle> create(String key){created++;Actor actor=new Actor();actors.add(actor);return List.of(actor);}
 }
 public static void main(String[] args)throws Exception {
  Backend b=new Backend();Set<String> enabled=new HashSet<>();
  EventRuntimeService runtime=new EventRuntimeService(b,enabled::contains);
  runtime.reconcile();check(b.created==0,"disabled default creates no bosses");
  enabled.add("hung_vuong");runtime.reconcile();runtime.reconcile();
  check(b.created==1,"reconcile is idempotent");runtime.tick();check(b.actors.get(0).updates==1,"event boss ticks");
  enabled.clear();runtime.tick();check(b.actors.get(0).updates==1,"disable stops ticking immediately");
  b.actors.get(0).fail=true;
  try{runtime.reconcile();throw new AssertionError("failure must propagate");}catch(Exception expected){}
  check(runtime.activeCount("hung_vuong")==1,"failed retirement remains tracked");
  b.actors.get(0).fail=false;runtime.reconcile();check(b.actors.get(0).retired==1,"retry retires once");
  enabled.add("hung_vuong");b.ready=false;
  try{runtime.reconcile();throw new AssertionError("preflight failure must propagate");}catch(Exception expected){}
  check(b.created==1,"missing resources do not spawn");
  b.ready=true;runtime.reconcile();check(b.created==2,"new activation creates one new pool");
  runtime.start();runtime.start();runtime.stop();runtime.stop();
  System.out.println("Event runtime behavior PASS");
 }
}
