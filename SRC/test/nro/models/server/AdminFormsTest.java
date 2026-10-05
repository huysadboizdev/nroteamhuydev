package nro.models.server;
import java.util.*;
import nro.models.services.*;
import nro.models.services_func.Input;
import nro.models.player.*;
import nro.models.network.*;
import static nro.models.services.PinkFixture.*;
public class AdminFormsTest {
 static List<String> notices=new ArrayList<>();
 static Player actor()throws Exception{Player p=player(991);p.name="admin1";p.idMark=new IDMark();p.session=blank(MySession.class);p.session.player=p;p.session.userId=991;return p;}
 static void submit(Player p,int type,String... fields)throws Exception {p.idMark.setTypeInput(type);Message wire=new Message(-125);wire.writer().writeByte(fields.length);for(String s:fields)wire.writer().writeUTF(s);Message input=new Message((byte)-125,wire.getData());Input.gI().doInput(p,input);}
 public static void main(String[] args)throws Exception {
  Thread.setDefaultUncaughtExceptionHandler((t,e)->{e.printStackTrace();System.exit(1);});AdminCommandTest.setup();replace(Service.class,"instance",new Service(){@Override public void sendThongBao(Player p,String s){notices.add(s);}@Override public void sendMoney(Player p){}});
  Player p=actor();String mode=args[0];
  if(mode.equals("open")){for(int t:new int[]{Input.GIVE_IT,Input.GET_IT,Input.SEND_ITEM_OP,Input.FIND_PLAYER,Input.CHANGE_NAME,Input.BOTITEM,Input.BOTBOSS,Input.BOTQUAI,Input.BOTATTACKPLAYER}){p.idMark.setTypeInput(-1);Input.gI().createForm(p,t,"test");check(p.idMark.getTypeInput()==-1,"ordinary opened privileged form "+t);}}
  if(mode.equals("malformed")){p.session.isAdmin=true;submit(p,Input.GIVE_IT,"x");check(!notices.isEmpty(),"wrong field count silently dropped");notices.clear();submit(p,Input.GET_IT,"bad","0","0","1");check(!notices.isEmpty(),"invalid numeric input silently dropped");}
  if(mode.equals("forged")){submit(p,Input.GIVE_IT,"other","722","0","0","1");check(notices.stream().anyMatch(s->s.contains("admin")),"forged form was not denied before target lookup");}
  if(mode.equals("currency")){p.session.isAdmin=true;Client.gI().put(p);p.inventory.gem=1999999999;submit(p,Input.SEND_ITEM_OP,p.name,"-2","0","0","100");check(p.inventory.gem==2000000000,"currency overflow/clamp");check(notices.stream().anyMatch(s->s.contains("1/100")),"accepted currency count not reported");notices.clear();submit(p,Input.SEND_ITEM_OP,p.name,"-2","0","0","-1");check(p.inventory.gem==2000000000,"negative currency accepted");}
  if(mode.equals("menus")){
   nro.models.npc.NpcFactory.createNpcConMeo();var npc=Manager.NPCS.get(Manager.NPCS.size()-1);
   for(int menu:new int[]{nro.models.consts.ConstNpc.MENU_ADMIN,nro.models.consts.ConstNpc.MENU_FIND_PLAYER,nro.models.consts.ConstNpc.BAN_PLAYER,nro.models.consts.ConstNpc.BUFF_PET,206783}){p.idMark.setIndexMenu(menu);notices.clear();npc.confirmMenu(p,0);check(notices.stream().anyMatch(x->x.contains("admin")),"forged privileged menu "+menu);p.idMark.setIndexMenu(-1);nro.models.map.service.NpcService.gI().createMenuConMeo(p,menu,-1,"test","yes");check(p.idMark.getIndexMenu()==-1,"ordinary privileged menu open "+menu);}
   notices.clear();SubMenuService.gI().controller(p,42,SubMenuService.BAN);check(notices.stream().anyMatch(x->x.contains("admin")),"ordinary BAN submenu");
   Input.gI().createFormFindPlayer1(p);check(p.idMark.getTypeInput()==Input.FIND_PLAYER_NAME,"ordinary gift form gated");Input.gI().createForm(p,Input.FIND_PLAYER_GIFT_RUBY,"gift");check(p.idMark.getTypeInput()==Input.FIND_PLAYER_GIFT_RUBY,"ordinary gift amount gated");Input.gI().createFormChangeNameByItem(p);check(p.idMark.getTypeInput()==Input.CHANGE_NAME_BY_ITEM,"ordinary item rename form gated");
   p.session.isAdmin=true;p.idMark.setIndexMenu(nro.models.consts.ConstNpc.MENU_ADMIN);p.session.isAdmin=false;notices.clear();npc.confirmMenu(p,0);check(notices.stream().anyMatch(x->x.contains("admin")),"revoked menu role");
  }
  if(mode.equals("grants")){
   p.session.isAdmin=true;Client.gI().put(p);List<nro.models.item.Item> grants=new ArrayList<>();replace(InventoryService.class,"I",new InventoryService(){@Override public boolean addItemBag(Player x,nro.models.item.Item i){grants.add(i);return true;}@Override public void sendItemBags(Player x){}});
   for(String[] invalid:new String[][]{{"65536","0","0","1"},{"432","251","0","1"},{"432","0","-1","1"},{"432","0","0","-1"}})submit(p,Input.GET_IT,invalid);check(grants.isEmpty(),"invalid grant mutated");
   submit(p,Input.GET_IT,"432","50","10","2");check(grants.size()==2&&grants.get(0).itemOptions.get(0)!=grants.get(1).itemOptions.get(0),"nonstack form options shared");grants.clear();p.kolRewardPending=true;submit(p,Input.GET_IT,"432","0","0","1");check(grants.isEmpty(),"pending KOL grant");p.kolRewardPending=false;p.session.player=player(992);submit(p,Input.GET_IT,"432","0","0","1");check(grants.isEmpty(),"unlinked forged grant");
  }
  if(mode.equals("bot-malformed")){p.session.isAdmin=true;submit(p,Input.BOTATTACKPLAYER,"bad");check(!notices.isEmpty(),"invalid bot number silently dropped");}
  if(mode.equals("truncated")){p.session.isAdmin=true;p.idMark.setTypeInput(Input.GET_IT);Message wire=new Message(-125);wire.writer().writeByte(4);wire.writer().writeUTF("1");Input.gI().doInput(p,new Message((byte)-125,wire.getData()));check(!notices.isEmpty(),"truncated UTF silently dropped");}
  System.out.println("PASS AdminFormsTest "+mode);System.exit(0);
 }
}
