package nro.models.player;
import java.lang.reflect.Field;
import nro.models.player_badges.*;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.*;
import nro.models.services_func.UseItem;
import nro.models.task.*;
import nro.models.item.Item;
import nro.models.consts.ConstTask;

public final class BadgeSystemTest {
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static Player player(){Player p=new Player();p.isPlayer=true;return p;}
 static BadgesTask task(Player p,int id,int max){BadgesTask t=new BadgesTask();t.id=id;t.count=0;t.countMax=max;t.idBadgesReward=218;p.dataTaskBadges.add(t);return t;}
 static BadgesData data(Player p,long expiry){BadgesData b=new BadgesData(218,expiry,true);p.dataBadges.add(b);return b;}
 static void itemCatalog(){for(int id=0;id<1840;id++){Template.ItemTemplate i=new Template.ItemTemplate();i.id=(short)id;i.name="Item "+id;i.description="Test";i.type=0;i.gender=0;Manager.ITEM_TEMPLATES.add(i);}Manager.ITEM_TEMPLATES.get(190).type=9;}
 static void emptyBag(Player p){for(int i=0;i<100;i++)p.inventory.itemsBag.add(new Item());}
 public static void main(String[]args)throws Exception {
  for(int id=0;id<221;id++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"Option #",0));
  BagesTemplate t=new BagesTemplate();t.idEffect=218;t.idItem=1289;t.options.add(new Item.ItemOption(50,15));Manager.BAGES_TEMPLATES.add(t);
  Field f=Service.class.getDeclaredField("instance");f.setAccessible(true);f.set(null,new Service(){@Override public void sendThongBao(Player p,String s){} @Override public void sendMoney(Player p){} @Override public void point(Player p){} @Override public void sendBadgesPlayer(Player p,int sec,int id){}});
  switch(args[0]) {
   case "expiry": {Player p=player();data(p,0);check(BagesTemplate.sendListItemOption(p).isEmpty(),"Expired badge must contribute no stats immediately");check(BagesTemplate.listEffect(p).isEmpty(),"Expired badge must not appear in owned shop");break;}
   case "duplicates": {Player p=player();data(p,System.currentTimeMillis()+86400000);data(p,System.currentTimeMillis()+86400000);check(BagesTemplate.sendListItemOption(p).size()==1,"Duplicate records must apply once");p.autoSendBadges();check(p.dataBadges.size()==1,"Periodic refresh must repair duplicate records");break;}
   case "ownership": {Player p=player();BadgesData old=data(p,System.currentTimeMillis()+86400000);BadgesService.turnOnBadges(p,219);check(old.isUse,"Reject unowned title without disabling current one");break;}
   case "farmer": {Player p=player();BadgesTask ft=task(p,6,10);p.playerTask.sideTask.template=new SideTaskTemplate();p.playerTask.sideTask.count=1;p.playerTask.sideTask.maxCount=1;p.playerTask.sideTask.level=ConstTask.EASY;for(int i=0;i<3;i++)TaskService.gI().paySideTask(p);check(ft.count==0&&p.playerTask.sideTask.template!=null,"Failed easy payouts must not add farmer progress");break;}
   case "chest": {Player p=player();BadgesTask a=task(p,12,10),b=task(p,13,20),c=task(p,14,30);UseItem.gI().openRuongGo(p);check(a.count==0&&b.count==0&&c.count==0,"Missing chest must not advance any title");break;}
   case "stats": {t.options.clear();t.options.add(new Item.ItemOption(5,10));t.options.add(new Item.ItemOption(108,5));Player p=player();data(p,System.currentTimeMillis()+86400000);p.nPoint.calPoint();check(p.nPoint.tlSDCM==10&&p.nPoint.tlNeDon==5,"Crit damage and dodge title stats must be applied");p.nPoint.calPoint();check(p.nPoint.tlSDCM==10&&p.nPoint.tlNeDon==5,"Recalculating stats must not stack bonuses");break;}
   case "event": {Player p=player();BadgesTask a=task(p,15,500);p.event.addEventPoint(35);check(a.count==35,"Count event points, not item use attempts");p.event.subEventPoint(10);check(a.count==35,"Spending points must not reduce earned progress");break;}
   case "day": {Player p=player();BadgesTask a=task(p,9,500);a.count=80;p.badges.progressDay=BadgeRules.day()-1;BadgeRules.ensureDay(p);check(a.count==0,"Online new day resets progress");a.count=12;BadgeRules.ensureDay(p);check(a.count==12,"Same day never resets twice");check(a.progressDay==BadgeRules.day()&&a.toString().contains("progressDay"),"Reset day is serialized with task progress");break;}
   case "catalog": {BadgeRules.normalizeCatalog();for(int id:new int[]{1286,1287,1300})check(BagesTemplate.fineIdEffectbyIdItem(id)>0,"Missing title must map to real effect: "+id);check(Manager.TASKS_BADGES_TEMPLATE.stream().anyMatch(x->x.id==19&&x.idbadgesReward==228),"Fan title must have a task");int size=Manager.BAGES_TEMPLATES.size();BadgeRules.normalizeCatalog();check(Manager.BAGES_TEMPLATES.size()==size,"Catalog repair is idempotent");break;}
   case "fan": {BadgeRules.normalizeCatalog();Player p=player();p.kolQuestState=new KolQuestState();p.kolQuestState.normalStage=7;BadgeRules.syncTasks(p);BadgesTask a=p.dataTaskBadges.stream().filter(x->x.id==19).findFirst().orElseThrow();check(a.count==0,"Incomplete KOL chain does not unlock fan");p.kolQuestState.normalStage=8;BadgeRules.syncTasks(p);check(a.count==1,"Completed KOL chain unlocks fan");break;}
   case "combat": {Player p=player();p.nPoint.hpg=1000;p.nPoint.mpg=1000;p.nPoint.dameg=100;p.nPoint.calPoint();long hp=p.nPoint.hpMax,ki=p.nPoint.mpMax,sd=p.nPoint.dame;t.options.clear();t.options.add(new Item.ItemOption(77,10));t.options.add(new Item.ItemOption(103,10));t.options.add(new Item.ItemOption(50,10));data(p,System.currentTimeMillis()+86400000);p.nPoint.calPoint();check(p.nPoint.hpMax==hp+hp/10&&p.nPoint.mpMax==ki+ki/10&&p.nPoint.dame==sd+sd/10,"Title HP KI and damage apply to calculated combat stats");break;}
   case "farmer-success": {itemCatalog();Player p=player();emptyBag(p);BadgesTask a=task(p,6,10);p.playerTask.sideTask.template=new SideTaskTemplate();p.playerTask.sideTask.count=1;p.playerTask.sideTask.maxCount=1;p.playerTask.sideTask.level=ConstTask.HELL;TaskService.gI().paySideTask(p);check(a.count==1&&p.playerTask.sideTask.template==null,"Successful super hard payout adds exactly one point");TaskService.gI().paySideTask(p);check(a.count==1,"Repeated payout cannot advance again");break;}
   case "chest-success":case "chest-wrong": {itemCatalog();Player p=player();emptyBag(p);BadgesTask a=task(p,12,10),b=task(p,13,20),c=task(p,14,30);Item chest=ItemService.gI().createNewItem((short)570);chest.itemOptions.add(new Item.ItemOption(72,args[0].equals("chest-success")?12:11));p.inventory.itemsBag.set(0,chest);UseItem.gI().openRuongGo(p);int expected=args[0].equals("chest-success")?1:0;check(a.count==expected&&b.count==expected&&c.count==expected,"Only successfully opened level 12 chest advances each title once");break;}
   default:throw new AssertionError(args[0]);
  }
  System.out.println("PASS: Badge system "+args[0]);
 }
}
