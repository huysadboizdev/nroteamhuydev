package nro.models.services;
import java.lang.reflect.*;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import nro.models.player.*;
import nro.models.network.MySession;
import nro.models.server.*;
import nro.models.npc.*;
import nro.models.consts.ConstNpc;
import nro.models.database.RubyGiftDAO;
import nro.models.map.service.ChangeMapService;
import static nro.models.services.PinkFixture.*;
public class AdminKickConcurrencyTest {
 static class FixtureStop extends Error {}
 static Player online(int id)throws Exception{Player p=player(id);p.name="fixture"+id;p.idMark=new IDMark();p.session=blank(MySession.class);p.session.userId=id;p.session.player=p;p.zone=zone();p.effectSkill=new EffectSkill(p);p.effectSkin=blank(EffectSkin.class);p.idNRNM=-1;Client.gI().put(p);return p;}
 static Npc menu(Player admin,Player target){NpcFactory.createNpcConMeo();Npc npc=Manager.NPCS.get(Manager.NPCS.size()-1);admin.idMark.setIndexMenu(ConstNpc.MENU_FIND_PLAYER);NpcFactory.PLAYERID_OBJECT.put(admin.id,target);return npc;}
 static void pendingGift(Player low,Player high)throws Exception {
  RubyGiftService owner=new RubyGiftService(new RubyGiftDAO(){@Override public Receipt reconcile(String token,Snapshot sender,Snapshot target,int amount)throws SQLException{throw new SQLException("fixture gift outcome unavailable");}},p->true,p->false);
  Class<?> pending=Class.forName("nro.models.services.RubyGiftService$Pending"),operation=Class.forName("nro.models.services.RubyGiftService$Operation"),finalization=Class.forName("nro.models.services.RubyGiftService$Finalization");Constructor<?> pendingCtor=pending.getDeclaredConstructors()[0];pendingCtor.setAccessible(true);Object proposal=pendingCtor.newInstance(low,high,low.session,high.session,100,"fixture-kick",System.currentTimeMillis());Constructor<?> stateCtor=finalization.getDeclaredConstructor();stateCtor.setAccessible(true);Object state=stateCtor.newInstance();Constructor<?> opCtor=operation.getDeclaredConstructors()[0];opCtor.setAccessible(true);Object op=opCtor.newInstance(owner,proposal,null,null,null,null,null,state);Field active=RubyGiftService.class.getDeclaredField("ACTIVE");active.setAccessible(true);@SuppressWarnings("unchecked") Map<Player,Object> registry=(Map<Player,Object>)active.get(null);registry.put(low,op);registry.put(high,op);
 }
 public static void main(String[] args)throws Exception {
  Thread.setDefaultUncaughtExceptionHandler((t,e)->{e.printStackTrace();System.exit(1);});install();Player admin=online(100),low=online(201),high=online(202);admin.session.isAdmin=true;Npc npc=menu(admin,high);AtomicReference<Throwable> failure=new AtomicReference<>();
  if(args[0].equals("revoked")) {
   Thread kick=new Thread(()->{try{npc.confirmMenu(admin,4);}catch(Throwable e){failure.set(e);}},"revoked kick");boolean blocked;
   synchronized(high){kick.start();long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);while(kick.getState()!=Thread.State.BLOCKED&&System.nanoTime()<deadline)Thread.sleep(1);blocked=kick.getState()==Thread.State.BLOCKED;admin.session.isAdmin=false;}
   kick.join(3000);check(blocked&&!kick.isAlive()&&failure.get()==null&&Client.gI().getPlayer(high.id)==high&&!high.beforeDispose,"revoked role kicked captured target");
  } else {
   pendingGift(low,high);CountDownLatch lowerHeld=new CountDownLatch(1),kickEntered=new CountDownLatch(1);
   replace(Service.class,"instance",new Service(){@Override public void sendThongBao(Player p,String s){} @Override public void sendMessAnotherNotMeInMap(Player p,nro.models.network.Message ignored){if(p!=high)throw new AssertionError("unexpected logout target");kickEntered.countDown();try{check(lowerHeld.await(3,TimeUnit.SECONDS),"gift lower monitor not held");}catch(InterruptedException e){throw new AssertionError(e);}RubyGiftService.gI().reconcilePending(high);throw new FixtureStop();}});
   Thread recovery=new Thread(()->{try{synchronized(low){lowerHeld.countDown();check(kickEntered.await(3,TimeUnit.SECONDS),"kick did not enter actual Client cleanup");RubyGiftService.gI().reconcilePending(high);}}catch(Throwable e){failure.set(e);}},"pending gift recovery");Thread kick=new Thread(()->{try{npc.confirmMenu(admin,4);}catch(FixtureStop stop){}catch(Throwable e){failure.set(e);}},"admin high-ID kick");recovery.setDaemon(true);kick.setDaemon(true);recovery.start();check(lowerHeld.await(3,TimeUnit.SECONDS),"recovery did not start");kick.start();kick.join(3000);recovery.join(3000);check(!kick.isAlive()&&!recovery.isAlive()&&failure.get()==null,"admin high-ID kick deadlocked with pending gift recovery");check(high.beforeDispose&&Client.gI().getPlayer(high.id)==null&&Client.gI().getPlayer(low.id)==low,"kick registry cleanup incorrect");
  }
  System.out.println("PASS AdminKickConcurrencyTest "+args[0]);System.exit(0);
 }
}
