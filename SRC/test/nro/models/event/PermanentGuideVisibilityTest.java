package nro.models.event;
import java.util.*;
import nro.models.player.*;
import nro.models.network.Message;
import nro.models.services.Service;
import nro.models.npc_list.TayDuGuide;
import static nro.models.services.PinkFixture.*;
public final class PermanentGuideVisibilityTest {
 static List<Integer> hidden=new ArrayList<>();
 static class Owner extends Player {int menus;@Override public void sendMessage(Message m){if(m.command==32)menus++;}}
 public static void main(String[] args)throws Exception {
  install();replace(Service.class,"instance",new Service(){@Override public void sendHideNpc(Player p,int id,boolean hide){if(hide)hidden.add(id);}});
  for(boolean on:new boolean[]{false,true})for(int map:new int[]{0,7,14,124}){
   Owner p=blank(Owner.class);p.id=9049;p.zone=zone();p.zone.map.mapId=map;p.idMark=new IDMark();p.location=new Location();p.location.x=100;p.location.y=100;p.nPoint=new NPoint(p);p.nPoint.hp=100;
   EventItemPolicy.publish(Map.of("hung_vuong",on,"summer",on,"lunar_new_year",on));hidden.clear();
   EventRuntimeService.gI().syncNpcVisibility(p);
   check(!hidden.contains(49),"Permanent Duong Tang guide hidden by seasonal state: map="+map+" events="+on);
   TayDuGuide guide=blank(TayDuGuide.class);guide.mapId=map;guide.tempId=49;guide.cx=100;guide.cy=100;
   guide.openBaseMenu(p);check(p.menus==1,"Permanent guide menu must remain available on map="+map);
  }
  check(EventCatalog.all().stream().noneMatch(d->Arrays.stream(d.npcIds()).anyMatch(id->id==49)),"Permanent guide cannot belong to seasonal NPC catalog");
  check(Arrays.equals(EventCatalog.get("hung_vuong").npcIds(),new int[]{52,66}),"Hung Vuong NPC ownership changed");
  check(Arrays.equals(EventCatalog.get("summer").npcIds(),new int[]{84}),"Summer NPC ownership changed");
  System.out.println("PermanentGuideVisibilityTest PASS: all four guide maps, seasonal OFF/ON, actual menus, existing seasonal ownership");
 }
}
