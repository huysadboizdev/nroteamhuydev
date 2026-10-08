package nro.models.player;
import java.util.*;
import nro.models.player_badges.*;
import nro.models.services.*;
import nro.models.shop.*;
import nro.models.task.*;
import nro.models.server.Manager;
import nro.models.item.Item;
import nro.models.services.PinkFixture;

public final class GoDauTreShopTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static TabShop base(){TabShop t=new TabShop();int[] effects={242,243,240};for(int n=0;n<3;n++){
  BagesTemplate b=new BagesTemplate();b.idEffect=effects[n];b.idItem=1392+n;b.NAME="Gõ đầu trẻ";Manager.BAGES_TEMPLATES.add(b);
  ItemShop row=new ItemShop();row.temp=PinkFixture.template(1392+n);row.temp.gender=3;t.itemShops.add(row);
 }return t;}
 static BadgesData own(Player p,int effect,boolean active,int days){var b=new BadgesData(effect,System.currentTimeMillis()+days*86400000L,active);p.dataBadges.add(b);return b;}
 static void run(String mode)throws Exception{
  PinkFixture.install();for(int id=0;id<221;id++)Manager.ITEM_OPTION_TEMPLATES.add(new nro.models.player_system.Template.ItemOptionTemplate(id,"option #",0));
  Player p=new Player();p.isPlayer=true;var base=base();
  if(mode.equals("available")){var tab=new TabShopDanhHieu(base,p);check(tab.itemShops.size()==1,"one available row got"+tab.itemShops.size());check(new TabShopSoHuu(base,p).itemShops.isEmpty(),"unowned");}
  else if(mode.equals("legacy")){for(int effect:new int[]{242,243,240}){
   p.dataBadges.clear();own(p,effect,true,5);var owned=new TabShopSoHuu(base,p);check(owned.itemShops.size()==1,"legacy usable");check(BagesTemplate.fineIdEffectbyIdItem(owned.itemShops.get(0).temp.id)==effect,"preserve legacy id");check(new TabShopDanhHieu(base,p).itemShops.isEmpty(),"hide other variants when owned");
  }}
  else if(mode.equals("owned")){own(p,242,false,20);own(p,243,true,5);own(p,240,false,30);var tab=new TabShopSoHuu(base,p);check(tab.itemShops.size()==1&&tab.itemShops.get(0).temp.id==1393,"prefer equipped");check(p.dataBadges.size()==3,"retain ownership records");check(new TabShopDanhHieu(base,p).itemShops.isEmpty(),"single across both tabs");}
  else if(mode.equals("progress")){for(int n=0;n<3;n++){var task=new BadgesTask();task.id=12+n;task.idBadgesReward=new int[]{242,243,240}[n];task.count=n==1?20:1;task.countMax=20;p.dataTaskBadges.add(task);}var tab=new TabShopDanhHieu(base,p);check(tab.itemShops.size()==1&&tab.itemShops.get(0).temp.id==1393,"prefer unlocked progress");}
  else if(mode.equals("expired")){own(p,242,false,-1);var tab=new TabShopDanhHieu(base,p);check(tab.itemShops.size()==1&&new TabShopSoHuu(base,p).itemShops.isEmpty(),"expired not owned");}
  else throw new AssertionError(mode);
 }
 public static void main(String[] args){try{run(args[0]);System.out.println("GO_DAU_TRE "+args[0]+" PASS");System.exit(0);}catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
