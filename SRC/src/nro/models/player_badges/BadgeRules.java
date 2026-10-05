package nro.models.player_badges;

import java.util.*;
import java.time.*;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.item.Item;
import nro.models.services.ItemService;
import nro.models.task.*;

/** Shared eligibility and repair for title records loaded from older releases. */
public final class BadgeRules {
 private BadgeRules(){}
 public static int canonical(int effect){return switch(effect){case 1286->215;case 1287->216;case 1300->229;default->effect;};}
 public static BagesTemplate template(int effect){for(BagesTemplate t:Manager.BAGES_TEMPLATES)if(t.idEffect==effect)return t;return null;}
 public static boolean usable(Player p,BadgesData b){return b!=null&&b.timeofUseBadges>System.currentTimeMillis()&&template(b.idBadGes)!=null&&DragonBadgeRules.canUse(p,b.idBadGes);}
 public static boolean owned(Player p,int effect){for(BadgesData b:p.dataBadges)if(b.idBadGes==effect&&usable(p,b))return true;return false;}
 public static boolean refresh(Player p){
  synchronized(p){
   boolean changed=DragonBadgeRules.refresh(p);Map<Integer,BadgesData> unique=new LinkedHashMap<>();
   Iterator<BadgesData> it=p.dataBadges.iterator();
   while(it.hasNext()){
    BadgesData b=it.next();int id=canonical(b.idBadGes);if(id!=b.idBadGes){b.idBadGes=id;changed=true;}
    if(b.timeofUseBadges<=System.currentTimeMillis()||template(id)==null){it.remove();changed=true;continue;}
    BadgesData first=unique.putIfAbsent(id,b);
    if(first!=null){first.isUse|=b.isUse;first.timeofUseBadges=Math.max(first.timeofUseBadges,b.timeofUseBadges);it.remove();changed=true;}
   }
   boolean active=false;
   for(BadgesData b:p.dataBadges)if(b.isUse){if(active||!usable(p,b)){b.isUse=false;changed=true;}else active=true;}
   return changed;
  }
 }
 public static long day(){return LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).toEpochDay();}
 public static void ensureDay(Player p){
  synchronized(p){long today=day();if(p.badges.progressDay==Long.MIN_VALUE){
   p.badges.progressDay=today;
   if(p.firstTimeLogin!=null&&p.firstTimeLogin.toInstant().atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate().toEpochDay()!=today)resetCounters(p);
  }else if(p.badges.progressDay!=today){resetCounters(p);p.badges.progressDay=today;}
  for(BadgesTask t:p.dataTaskBadges)t.progressDay=today;
  }
 }
 private static void resetCounters(Player p){for(BadgesTask t:p.dataTaskBadges)if(t.id!=17&&t.id!=19)t.count=0;}
 public static void normalizeCatalog(){
  add(215,1286,"Kẻ thao túng sói");add(216,1287,"Nước anh bao");add(229,1300,"Thánh ở dơ");
  BagesTemplate kol=template(226);if(kol!=null){kol.options.removeIf(o->o.optionTemplate.id==10||o.optionTemplate.id==103);kol.options.add(new Item.ItemOption(103,10));}
  for(BadgesTaskTemplate t:Manager.TASKS_BADGES_TEMPLATE){t.idbadgesReward=canonical(t.idbadgesReward);if(t.id==8)t.name="Thắng Xinbato ở đại hội 23 đủ 5 lần";if(t.id==18)t.name="Hạ boss Mặt Trời 1 lần";
   if(t.id==1)t.name="Đổi số dư nạp tổng 1 triệu trong ngày";
   if(t.id==16)t.name="Đổi số dư nạp tổng 2 triệu trong ngày";
  }
  if(Manager.TASKS_BADGES_TEMPLATE.stream().noneMatch(t->t.id==19)){BadgesTaskTemplate t=new BadgesTaskTemplate();t.id=19;t.name="Hoàn thành 7 nhiệm vụ KOL tại Quy Lão";t.count=1;t.idbadgesReward=228;Manager.TASKS_BADGES_TEMPLATE.add(t);}
  for(BadgesTaskTemplate task:Manager.TASKS_BADGES_TEMPLATE){
   BagesTemplate title=template(task.idbadgesReward);
   if(title!=null && Manager.ITEM_TEMPLATES.size()>title.idItem)
    Manager.ITEM_TEMPLATES.get(title.idItem).description="Cách nhận: "+task.name+"\nThời hạn danh hiệu: 30 ngày";
  }
 }
 private static void add(int effect,int item,String name){
  if(template(effect)!=null)return;
  BagesTemplate t=new BagesTemplate();t.idEffect=effect;t.idItem=item;t.NAME=name;
  List<Item.ItemOption> options=ItemService.gI().getListOptionItemShop((short)item);
  if(options!=null)for(Item.ItemOption o:options)if(o.optionTemplate.id!=220)t.options.add(new Item.ItemOption(o.optionTemplate.id,o.param));
  if(t.options.stream().noneMatch(o->o.optionTemplate.id==93))t.options.add(new Item.ItemOption(93,30));
  Manager.BAGES_TEMPLATES.add(t);
 }
 public static void syncTasks(Player p){
  synchronized(p){
  ensureDay(p);
  for(BadgesTask t:p.dataTaskBadges)t.idBadgesReward=canonical(t.idBadgesReward);
  if(p.dataTaskBadges.stream().noneMatch(t->t.id==19)&&Manager.TASKS_BADGES_TEMPLATE.stream().anyMatch(t->t.id==19)){
   BadgesTask t=new BadgesTask();t.id=19;t.count=0;t.countMax=1;t.idBadgesReward=228;p.dataTaskBadges.add(t);
  }
  for(BadgesTask t:p.dataTaskBadges)if(t.id==19)t.count=p.kolQuestState!=null&&p.kolQuestState.valid()&&(p.kolQuestState.normalStage==8||p.kolQuestState.vipStage==8)?1:0;
  for(BadgesTask t:p.dataTaskBadges)if(t.id==5 && p.superRank!=null && p.superRank.rank==1)t.count=1;
  }
 }
}
