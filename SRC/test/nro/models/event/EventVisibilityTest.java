package nro.models.event;
import java.util.*;
import nro.models.player.*;
import nro.models.npc.DuaHauEgg;
import nro.models.npc_list.ChiChi;
import nro.models.services.*;
import nro.models.network.Message;
import nro.models.consts.ConstNpc;
import static nro.models.services.PinkFixture.*;
public final class EventVisibilityTest {
 static int hidden, broadcast;
 static class Owner extends Player {int eggUpdates;public void sendMessage(Message m){if(m.command==-122)eggUpdates++;}}
 static class Menu extends ChiChi {
  String[] shown;
  Menu(){super(5,1,0,0,1,0);}
  public boolean canOpenNpc(Player p){return true;}
  public void createOtherMenu(Player p,int index,String text,String...items){shown=items;p.idMark.setIndexMenu(index);}
 }
 public static void main(String[]args)throws Exception {
  install();replace(Service.class,"instance",new Service(){
   public void sendHideNpc(Player p,int id,boolean hide){if(id==51&&hide)hidden++;}
   public void sendMessAllPlayerInMap(Player p,Message m){broadcast++;}
  });
  Owner p=new Owner();p.id=9001;p.idMark=new IDMark();p.zone=zone();
  p.DuaHauEgg=new DuaHauEgg(p,System.currentTimeMillis(),1000);
  EventItemPolicy.publish(Map.of("hung_vuong",false,"summer",false));
  p.DuaHauEgg.sendDuaHauEgg();
  check(hidden==1&&p.eggUpdates==0,"OFF must hide melon instead of sending its countdown");
  EventRuntimeService.gI().syncNpcVisibility(p);check(hidden==2,"live OFF refresh includes special melon NPC51");
  Menu menu=blank(Menu.class);menu.mapId=5;menu.openBaseMenu(p);
  check(Arrays.equals(menu.shown,new String[]{"Cửa hàng","Bỏ đồ\ncó hạn sử dụng","Đóng"}),"OFF ChiChi hides all event top entries");
  int closedMenu=p.idMark.getIndexMenu();check(closedMenu!=ConstNpc.BASE_MENU,"snapshot menu layout retained for action mapping");
  menu.confirmMenu(p,2);check(p.idMark.getIndexMenu()==closedMenu,"close cannot open an event submenu");
  p.idMark.setIndexMenu(ConstNpc.BASE_MENU);menu.shown=null;menu.confirmMenu(p,0);
  check(menu.shown==null,"stale open event menu is rejected after OFF");
  p.idMark.setIndexMenu(ConstNpc.PHAO_BONG);menu.confirmMenu(p,0);
  EventItemPolicy.publish(Map.of("hung_vuong",true,"summer",true));
  menu.openBaseMenu(p);check(menu.shown.length==6,"ON restores event menus");
  p.DuaHauEgg.sendDuaHauEgg();check(p.eggUpdates==1&&broadcast==0,"melon data belongs only to its owner");
  System.out.println("EventVisibilityTest PASS");
 }
}
