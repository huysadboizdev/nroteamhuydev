package nro.models.services;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.io.DataInputStream;
import nro.models.player.*;
import nro.models.network.*;
import nro.models.server.*;
import nro.models.services_func.*;
import nro.models.item.Item;
import static nro.models.services.PinkFixture.*;
public class AdminRenameQuarantineTest {
 static void retain(Player p)throws Exception {
  Class<?> type=Class.forName("nro.models.services.AdminPlayerActions$RenamePending");Constructor<?> ctor=type.getDeclaredConstructor(AdminPlayerActions.class,Player.class,int.class,String.class,String.class,Item.class);ctor.setAccessible(true);Object op=ctor.newInstance(new AdminPlayerActions(()->{throw new AssertionError("no SQL in quarantine fixture");},x->false),p,p.session.userId,"before1","after12",null);Field registry=AdminPlayerActions.class.getDeclaredField("RENAME_PENDING");registry.setAccessible(true);@SuppressWarnings("unchecked") Map<Player,Object> pending=(Map<Player,Object>)registry.get(null);pending.put(p,op);
 }
 static Player participant(int id)throws Exception{Player p=player(id);p.name="player"+id;p.idMark=new IDMark();p.session=blank(MySession.class);p.session.userId=id;p.session.player=p;return p;}
 public static void main(String[] args)throws Exception {
  Thread.setDefaultUncaughtExceptionHandler((t,e)->{e.printStackTrace();System.exit(1);});install();Player a=participant(969),b=participant(970);
  if(args[0].equals("queued")){
   AtomicInteger reads=new AtomicInteger();AtomicReference<Throwable> error=new AtomicReference<>();Message packet=new Message(6){@Override public DataInputStream reader(){reads.incrementAndGet();throw new AssertionError("inventory dispatch read quarantined packet");}};Thread worker=new Thread(()->{try{Controller.gI().onMessage(a.session,packet);}catch(Throwable e){error.set(e);}},"queued inventory packet");boolean blocked;
   synchronized(a){worker.start();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(worker.getState()!=Thread.State.BLOCKED&&System.nanoTime()<deadline)Thread.sleep(1);blocked=worker.getState()==Thread.State.BLOCKED;retain(a);}
   worker.join(5000);check(blocked&&!worker.isAlive(),"inventory packet contention was not established");check(reads.get()==0&&error.get()==null,"queued inventory packet bypassed rename quarantine");
  } else if(args[0].equals("trade")) {
   a.inventory.itemsBag.add(item(2006,2));List<Item> reservedBag=a.inventory.itemsBag;Trade trade=new Trade(a,b);retain(a);
   replace(PlayerService.class,"i",new PlayerService(){@Override public void sendInfoHpMpMoney(Player p){throw new AssertionError("fixture stops before HistoryTransactionDAO production write");}});
   try{trade.acceptTrade();trade.acceptTrade();}catch(AssertionError expected){}
   check(a.inventory.itemsBag==reservedBag,"trade counterpart overwrote rename-reserved bag snapshot");
   trade.dispose();
   try{new Trade(a,participant(971));throw new AssertionError("pending target entered new trade");}catch(IllegalStateException expected){}
  }
  System.out.println("PASS AdminRenameQuarantineTest "+args[0]);System.exit(0);
 }
}
