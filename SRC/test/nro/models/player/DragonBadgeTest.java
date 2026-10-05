package nro.models.player;

import java.lang.reflect.Field;
import java.util.*;
import nro.models.item.Item;
import nro.models.player_badges.*;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.Service;
import nro.models.task.*;

public final class DragonBadgeTest {
    static int cleared;
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    static Player player(){Player p=new Player();p.isPlayer=true;p.nPoint=new NPoint(p){@Override public void update(){}};BadgesTask t=new BadgesTask();t.id=17;t.countMax=7;t.idBadgesReward=256;p.dataTaskBadges.add(t);return p;}
    static Item dragon(int id){Item i=new Item();i.template=new Template.ItemTemplate();i.template.id=(short)id;i.quantity=1;return i;}
    static void full(Player p){for(int id=1765;id<=1771;id++)p.inventory.itemsBag.add(dragon(id));}
    static BadgesData badge(int id){return new BadgesData(id,System.currentTimeMillis()+86_400_000,true);}
    static void missing(){Player p=player();full(p);p.inventory.itemsBag.remove(4);p.inventory.itemsBag.add(dragon(1765));p.dataTaskBadges.get(0).count=7;BadgesTaskService.updateDoneTask(p);check(p.dataBadges.isEmpty(),"Duplicates cannot unlock missing dragon type");check(p.dataTaskBadges.get(0).count==6,"Progress counts six distinct permanent types");check(BadgesTaskService.sendPercenBadgesTask(p,256)==85,"Incomplete collection progress is not 100%");}
    static void legacy(){Player p=player();full(p);p.inventory.itemsBag.remove(4);BadgesData b=badge(256);p.dataBadges.add(b);p.dataBadges.add(b);p.badges.idBadges=256;p.autoSendBadges();check(p.badges.idBadges==-1&&cleared==1,"Clear cached legacy title when collection incomplete");check(!b.isUse,"Deactivate ineligible legacy title");check(p.dataBadges.size()==1,"Deduplicate legacy badge records");b.isUse=true;check(BagesTemplate.sendListItemOption(p).isEmpty(),"Ineligible badge contributes no stats even before periodic refresh");check(BagesTemplate.listEffect(p).isEmpty(),"Ineligible badge hidden in available badge list");BadgesService.turnOnBadges(p,256);check(!b.isUse,"Cannot reactivate without all types");}
    static void grant(){Player p=player();full(p);p.inventory.itemsBox.add(p.inventory.itemsBag.remove(0));p.inventory.itemsBody.add(p.inventory.itemsBag.remove(0));BadgesTaskService.updateDoneTask(p);check(p.dataBadges.size()==1,"Full collection grants exactly one badge");check(p.dataBadges.get(0).idBadGes==256&&p.dataTaskBadges.get(0).count==7,"Grant dragon title from ownership");BadgesTaskService.updateDoneTask(p);check(p.dataBadges.size()==1,"Repeated ticks never grant duplicate records");p.autoSendBadges();check(p.badges.idBadges==256,"Complete collection displays title");check(BagesTemplate.sendListItemOption(p).size()==1,"Badge stats applied once");BadgesTask other=new BadgesTask();other.id=3;other.count=1;other.countMax=1;other.idBadgesReward=257;p.dataTaskBadges.add(other);BadgesTaskService.updateDoneTask(p);check(p.dataBadges.size()==2,"Existing dragon badge does not prevent other rewards");}
    static void lifetime(){Player p=player();full(p);BadgesTaskService.updateDoneTask(p);p.autoSendBadges();p.inventory.itemsBag.remove(4);p.autoSendBadges();check(p.badges.idBadges==-1&&BagesTemplate.sendListItemOption(p).isEmpty(),"Losing required pet revokes display and stats");p.inventory.itemsBag.add(dragon(1769));BadgesTaskService.updateDoneTask(p);BadgesService.turnOnBadges(p,256);p.autoSendBadges();check(p.badges.idBadges==256,"Restored full collection can reactivate title");p.dataBadges.get(0).timeofUseBadges=0;p.autoSendBadges();check(p.badges.idBadges==-1,"Expired badge clears cached display");}
    static void restrictions(){for(int mode=0;mode<3;mode++){Player p=player();full(p);Item last=p.inventory.itemsBag.get(6);if(mode==0)last.itemOptions.add(new Item.ItemOption(93,0));if(mode==1)last.quantity=0;if(mode==2){p.pet=new Pet(p);p.pet.inventory.itemsBody.add(p.inventory.itemsBag.remove(6));}p.dataTaskBadges.get(0).count=7;BadgesTaskService.updateDoneTask(p);check(p.dataBadges.isEmpty(),"Timed/zero quantity/pet equipment never completes collection: "+mode);}Player other=player();BadgesData b=badge(257);other.dataBadges.add(b);BadgesService.turnOnBadges(other,256);check(b.isUse,"Rejected title selection preserves other active title");other.autoSendBadges();check(other.badges.idBadges==257&&BagesTemplate.sendListItemOption(other).size()==1,"Other titles keep behavior");}
    public static void main(String[]args)throws Exception {
        for(int id=0;id<=93;id++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"Option #",0));
        for(int id=256;id<=257;id++){BagesTemplate t=new BagesTemplate();t.idEffect=id;t.idItem=2000+id-256;t.options.add(new Item.ItemOption(50,1));Manager.BAGES_TEMPLATES.add(t);}
        Field f=Service.class.getDeclaredField("instance");f.setAccessible(true);f.set(null,new Service(){@Override public void point(Player p){}@Override public void sendBadgesPlayer(Player p,int sec,int id){if(sec==0)cleared++;}});
        switch(args[0]){case "missing"->missing();case "legacy"->legacy();case "grant"->grant();case "lifetime"->lifetime();case "restrictions"->restrictions();default->throw new AssertionError("Unknown mode");}System.out.println("PASS: Dragon badge "+args[0]);
    }
}
