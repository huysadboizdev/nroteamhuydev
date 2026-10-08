package nro.models.services;
public final class LiveSyncGateTest {
 public static void main(String[] args) throws Exception {
  if(LiveSyncCommandGate.hasPendingId(41))throw new AssertionError("initial gate");
  LiveSyncCommandGate.mark(41,52);
  if(!LiveSyncCommandGate.hasPendingId(41)||!LiveSyncCommandGate.hasPendingAccount(52))throw new AssertionError("uncertain write must gate login/save");
  LiveSyncCommandGate.clear(41,52);
  if(LiveSyncCommandGate.hasPendingId(41)||LiveSyncCommandGate.hasPendingAccount(52))throw new AssertionError("resolved gate");
  if(!LiveSyncService.supports("UPDATE_PET")||!LiveSyncService.supports("RELOAD_SHOP")||LiveSyncService.supports("TEST_LIVE_SYNC"))throw new AssertionError("strict supported commands");
  nro.models.network.MySession session=PinkFixture.blank(nro.models.network.MySession.class);
  java.util.concurrent.atomic.AtomicInteger owners=new java.util.concurrent.atomic.AtomicInteger();
  java.util.List<Thread> threads=new java.util.ArrayList<>();
  for(int i=0;i<20;i++){Thread t=new Thread(()->{if(session.claimLogout())owners.incrementAndGet();});threads.add(t);t.start();}
  for(Thread t:threads)t.join();
  if(owners.get()!=1)throw new AssertionError("exactly one logout owner across disconnect/kick");
  nro.models.player.Player old=PinkFixture.blank(nro.models.player.Player.class);old.beforeDispose=true;old.liveLogoutCompleted=true;
  nro.models.database.PlayerDAO.updatePlayer(old); // Must return before touching disposed fields or SQL.
  System.out.println("PASS live command gates and command allowlist");
 }
}
