package nro.models.boss.Boss_mini;
import java.util.*;
import nro.models.boss.*;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.BossStatus;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.player.*;
import static nro.models.services.PinkFixture.*;
public final class MiniSunZoneTest {
 static class Sun extends MatTroi {
  Sun() throws Exception {super();}
  @Override public void changeStatus(BossStatus status){bossStatus=status;}
  @Override public void joinMapByZone(Zone z){zone=z;z.getBosses().add(this);}
 }
 static Sun sun(Zone zone)throws Exception {Sun s=blank(Sun.class);s.zone=zone;s.nPoint=new NPoint(s);s.nPoint.hp=100;BossManager.gI().addBoss(s);return s;}
 public static void main(String[] args)throws Exception {
  install();replace(BossManager.class,"instance",new BossManager());
  Zone base=zone();java.lang.reflect.Field zones=nro.models.map.Map.class.getDeclaredField("zones");zones.setAccessible(true);zones.set(base.map,new ArrayList<Zone>());
  for(int i=0;i<20;i++){Zone z=zone();z.map=base.map;z.zoneId=i;base.map.zones.add(z);}
  String mode=args[0];
  if(mode.equals("spread")){for(int i=0;i<20;i++)sun(base).joinMap2();check(BossManager.gI().getBosses().stream().map(b->b.zone).distinct().count()==20,"20 Suns must occupy 20 distinct zones");}
  else if(mode.equals("full")){for(Zone z:base.map.zones){Sun s=sun(z);z.getBosses().add(s);}Sun extra=sun(base);extra.joinMap2();check(extra.zone==null&&extra.bossStatus==BossStatus.RESPAWN,"full map must retry rather than duplicate Suns");}
  else if(mode.equals("other")){Boss other=blank(Odo.class);other.zone=base.map.zones.get(0);other.zone.getBosses().add(other);Sun first=sun(base);first.joinMap2();check(first.zone.zoneId==0,"other boss types may share the zone");Sun second=sun(base);second.joinMap2();check(second.zone.zoneId==1,"second Sun must skip occupied zone zero");}
  else if(mode.equals("concurrent")){List<Sun> suns=new ArrayList<>();for(int i=0;i<20;i++)suns.add(sun(base));List<Thread> threads=new ArrayList<>();for(Sun s:suns){Thread t=new Thread(s::joinMap2);threads.add(t);t.start();}for(Thread t:threads)t.join(3000);check(threads.stream().noneMatch(Thread::isAlive),"spawn threads finish");check(suns.stream().map(s->s.zone).distinct().count()==20,"concurrent spawns reserve distinct zones");}
  else throw new AssertionError(mode);
  System.out.println("MINI_SUN_ZONE PASS "+mode);System.exit(0);
 }
}
