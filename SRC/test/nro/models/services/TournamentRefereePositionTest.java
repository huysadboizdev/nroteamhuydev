package nro.models.services;
import java.util.ArrayList;
import nro.models.map.Zone;
import nro.models.npc.NonInteractiveNPC;
import nro.models.server.Manager;
import static nro.models.services.PinkFixture.*;
public final class TournamentRefereePositionTest {
 public static void main(String[] args)throws Exception {try{
  install();var map=zone().map;map.mapId=52;map.zones=new ArrayList<>();
  for(int id:new int[]{0,1,9})map.zones.add(new Zone(map,id,15));Manager.MAPS.clear();Manager.MAPS.add(map);
  new NonInteractiveNPC().initNonInteractiveNPC();
  for(Zone z:map.zones){var referee=z.getNpc();check(referee!=null&&referee.location.x==373&&referee.location.y==336,"map52 referee fixed at373/336 in zone"+z.zoneId);}
  System.out.println("PASS tournament referee same position in zones0/1/9");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
