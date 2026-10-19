package nro.models.services;
import java.util.*;
import java.lang.reflect.*;
import nro.models.boss.*;
import nro.models.boss.PrisonBill.*;
import nro.models.consts.*;
import nro.models.player.*;
import nro.models.server.ServerNotify;
import static nro.models.services.PinkFixture.*;
public class BossAnnouncementTest {
 public static class Probe extends Boss {
  Probe()throws Exception{super(0,(BossData)null);}
  @Override public void rewardDefault(Player p){}
  void spawnNotice(){notifyJoinMap();}
 }
 static Probe boss(int id)throws Exception{
  Probe b=blank(Probe.class);b.id=id;b.name="Test "+id;b.zone=zone();b.zone.map.mapName="Map";b.nPoint=new NPoint(b);b.nPoint.hp=b.nPoint.hpMax=100;b.currentLevel=0;b.bossStatus=BossStatus.ACTIVE;
  return b;
 }
 static void set(Object b,Class<?> c,String n,Object v)throws Exception{Field f=c.getDeclaredField(n);f.setAccessible(true);f.set(b,v);}
 public static void main(String[] a){try{
  install();var notify=blank(ServerNotify.class);List<String> messages=new ArrayList<>();set(notify,ServerNotify.class,"notifies",messages);replace(ServerNotify.class,"i",notify);
  for(int id:new int[]{BossID.AN_TROM,BossID.MAT_TROI,BossID.O_DO}){
   Probe nuisance=boss(id);check(!nuisance.noticePublic(),"excluded identity "+id);nuisance.spawnNotice();nuisance.die(player(1));check(messages.isEmpty(),"excluded spawn and defeat never broadcast");
  }
  int[] twelveHour={BossID.DRABURA,BossID.BUI_BUI,BossID.YA_CON,BossID.MABU_12H,BossID.DRABURA_2,BossID.BUI_BUI_2,BossID.DRABURA_3,BossID.GOKU,BossID.CADIC};
  for(int id:twelveHour){Probe selected=boss(id);check(!selected.noticePublic(),"12h notice suppressed independent of location "+id);}
  Probe hiddenNotice=boss(BossID.DRABURA);BossNoticeService.gI().appeared(hiddenNotice);
  check(BossNoticeService.gI().rows(0).stream().anyMatch(r->r.semanticBossId()==BossID.DRABURA&&r.status().equals("ALIVE")),"12h boss remains visible in the Boss status board");
  check(messages.isEmpty(),"tracking a suppressed boss does not show a notice");
  hiddenNotice.nPoint.hp=0;hiddenNotice.die(player(9));
  check(BossNoticeService.gI().rows(0).stream().noneMatch(r->r.semanticBossId()==BossID.DRABURA),"dead 12h boss leaves the alive list");
  check(BossNoticeService.gI().rows(1).stream().anyMatch(r->r.semanticBossId()==BossID.DRABURA&&r.status().equals("DEFEATED")),"dead 12h boss remains visible in history");
  check(messages.isEmpty(),"defeating a suppressed boss does not show a notice");
  for(int id:new int[]{BossID.MABU,BossID.SUPERBU}){Probe selected=boss(id);selected.zone.map.type=ConstMap.MAP_MABU_14H;check(!selected.noticePublic(),"14h notice suppressed by map type "+id);}
  for(Class<? extends Boss> type:List.of(nro.models.boss.Boss_mini.AnTrom.class,nro.models.boss.Boss_mini.MatTroi.class,nro.models.boss.Boss_mini.Odo.class)){
   Boss mini=blank(type);mini.id=-2000123;mini.zone=zone();mini.isNotifyDisabled=false;
   check(!mini.noticePublic(),"dynamic mini boss excluded by class "+type.getSimpleName());
  }
  Probe equipment=boss(BossID.BABY);equipment.isNotifyDisabled=true;equipment.zone.map.mapId=114;
  check(BossAnnouncementPolicy.equipment(equipment),"equipment eligibility remains independent of notice suppression");
  check(equipment.noticePublic(),"allowed equipment boss bypasses legacy hidden/Mabu filters");
  equipment.spawnNotice();equipment.spawnNotice();BossNoticeService.gI().appeared(equipment);check(messages.size()==1,"spawn exactly once across callbacks");
  Player killer=player(2);killer.name="Winner";equipment.nPoint.hp=0;equipment.die(killer);equipment.die(killer);check(messages.size()==2,"defeat exactly once");
  check(BossNoticeService.gI().rows(0).isEmpty(),"dead boss removed from alive board");
  check(BossNoticeService.gI().rows(1).stream().anyMatch(r->r.semanticBossId()==BossID.BABY&&r.status().equals("DEFEATED")),"history records defeat");
  equipment.nPoint.hp=100;equipment.bossStatus=BossStatus.ACTIVE;equipment.spawnNotice();check(messages.size()==3,"new life gets a fresh notice");
  Probe task=boss(BossID.KUKU);check(task.noticePublic(),"normal task boss public");task.spawnNotice();check(messages.size()==4,"task boss broadcast");
  Probe hidden=boss(123);hidden.isNotifyDisabled=true;check(!hidden.noticePublic(),"unrelated hidden boss remains hidden");
  PrisonBillBoss k=new PrisonBillBoss(BossID.KAMIN);k.currentLevel=0;k.bossStatus=BossStatus.REST;set(k,Boss.class,"lastTimeRest",1000L);
  for(Boss child:k.getCompanionBosses()){child.zone=null;set(child,Boss.class,"lastTimeRest",2000L);}
  check(k.noticeNextSpawnAt()==302000L,"pair ETA uses later child rest and five minute cooldown");
  k.getCompanionBosses()[0].zone=zone();check(k.noticeNextSpawnAt()==0,"no ETA while companion still fighting");
  System.out.println("BOSS_ANNOUNCEMENT PASS exclusion/equipment/task/spawn-death-once/history/new-turn/pair-ETA");System.exit(0);
 }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
