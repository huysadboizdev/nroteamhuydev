package nro.models.services;
import nro.models.boss.Mabu.*;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.map.Zone;
import nro.models.player.Player;
import static nro.models.services.PinkFixture.*;
public final class FatMabuCleanupTest {
 static final class Blocked extends Zone {
  boolean blocked=true;Blocked(nro.models.map.Map map){super(map,0,20);}
  @Override public void removePlayer(Player p){if(blocked)throw new IllegalStateException("injected Mabu detach failure");super.removePlayer(p);}
 }
 public static void main(String[] args)throws Exception {try{
  install();Blocked z=new Blocked(zone().map);FatMabuTournament b=blank(FatMabuTournament.class);Player initialized=new Player();
  for(var f:Player.class.getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(f.getModifiers())){f.setAccessible(true);f.set(b,f.get(initialized));}
  b.id=-927;b.isBoss=true;b.isPlayer=false;b.zone=z;b.name="fixture";
  z.addPlayer(b);BossManager.gI().addBoss(b);FatMabuService service=FatMabuService.gI();var field=FatMabuService.class.getDeclaredField("boss");field.setAccessible(true);field.set(service,b);
  try{service.remove();throw new AssertionError("cleanup must report blocked detach");}catch(IllegalStateException expected){}
  check(service.current()==b&&b.zone==z&&z.getBosses().contains(b),"failed cleanup retains old generation");
  check(!BossManager.gI().getBosses().contains(b),"manager retirement independent from blocked map removal");
  try{service.spawn();throw new AssertionError("cannot replace attached old boss");}catch(IllegalStateException expected){}
  check(service.current()==b,"spawn never overwrites old generation");z.blocked=false;service.remove();
  check(service.current()==null&&b.zone==null&&!z.getBosses().contains(b)&&!z.getHumanoids().contains(b),"cleanup retry fully detaches old generation");
  System.out.println("PASS Mabu blocked detach, manager retirement, no overlap, cleanup retry");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
