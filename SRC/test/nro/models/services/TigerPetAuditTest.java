package nro.models.services;

import java.lang.reflect.Method;
import nro.models.item.Item;
import nro.models.intrinsic.Intrinsic;
import nro.models.map.Zone;
import nro.models.player.*;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services_func.UseItem;
import nro.models.task.TaskMain;

/** Real equip/use/stat/summon/dispose; fixture replaces only packet output. */
public final class TigerPetAuditTest {
    static class QuietZone extends Zone {
        QuietZone(nro.models.map.Map map){super(map,0,20);}
        @Override public void load_Me_To_Another(Player p){}
        @Override public void load_Another_To_Me(Player p){}
    }
    static void model(NewPet pet){
        PinkFixture.check(pet!=null && pet.getHead()==966 && pet.getBody()==967 && pet.getLeg()==968,"tiger model 966/967/968");
        PinkFixture.check(pet.isNewPet && !pet.isPl(),"companion actor does not become main player");
        PinkFixture.check(pet.nPoint.hpMax>0 && pet.nPoint.mpMax>0 && !pet.isDie(),"summon initializes positive HP/KI");
    }
    public static void main(String[] args){
        try {
            for(int id=0;id<=250;id++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"Fixture #",0));
            for(int id=0;id<=1000;id++){
                var template=new Template.ItemTemplate();template.id=(short)id;template.gender=3;template.type=27;template.name="Fixture";
                if(id==942){template.iconID=8768;template.name="Ho map vang";}
                Manager.ITEM_TEMPLATES.add(template);
            }
            PinkFixture.replace(Service.class,"instance",new Service(){
                @Override public void point(Player p){p.nPoint.calPoint();}
                @Override public void Send_Caitrang(Player p){}
                @Override public void player(Player p){}
                @Override public void sendFlagBag(Player p){}
                @Override public void sendThongBao(Player p,String message){}
                @Override public void sendMessAnotherNotMeInMap(Player p,nro.models.network.Message message){}
            });
            PinkFixture.replace(InventoryService.class,"I",new InventoryService(){
                @Override public void sendItemBags(Player p){}
                @Override public void sendItemBody(Player p){}
            });
            var p=new Player();p.id=991942;p.isPlayer=true;p.playerIntrinsic.intrinsic=new Intrinsic();p.playerTask.taskMain=new TaskMain();p.playerTask.taskMain.id=-1;
            p.nPoint.hpg=10000;p.nPoint.mpg=10000;p.nPoint.dameg=1000;p.nPoint.limitPower=9;
            for(int i=0;i<12;i++)p.inventory.itemsBody.add(new Item());
            p.inventory.itemsBag.add(new Item());p.inventory.itemsBag.add(new Item());
            p.zone=new QuietZone(PinkFixture.zone().map);p.zone.addPlayer(p);p.nPoint.calPoint();
            int hp=p.nPoint.hpMax,ki=p.nPoint.mpMax,damage=p.nPoint.dame;
            Method use=UseItem.class.getDeclaredMethod("useItem",Player.class,Item.class,int.class);use.setAccessible(true);
            for(int option:new int[]{77,103,50}){
                Item tiger=ItemService.gI().createNewItem((short)942);tiger.itemOptions.clear();tiger.itemOptions.add(new Item.ItemOption(option,10));tiger.itemOptions.add(new Item.ItemOption(30,0));tiger.quantity=1;
                p.inventory.itemsBag.set(0,tiger);
                use.invoke(UseItem.gI(),p,tiger,0);
                PinkFixture.check(p.inventory.itemsBody.get(7)==tiger && !p.inventory.itemsBag.get(0).isNotNullItem(),"use moves exact tiger item bag to body7");
                model(p.newPet);
                for(int i=0;i<10;i++)p.nPoint.calPoint();
                PinkFixture.check(p.nPoint.hpMax==(option==77?hp*110/100:hp) && p.nPoint.mpMax==(option==103?ki*110/100:ki) && p.nPoint.dame==(option==50?damage*110/100:damage),"equipped tiger applies generated option exactly once " + option);
                NewPet first=p.newPet;first.joinMapMaster();PinkFixture.check(first.zone==p.zone && p.zone.getHumanoids().contains(first),"summon joins actual owner zone");
                p.sendNewPet();PinkFixture.check(p.newPet==first,"login/setup method is idempotent with existing summon");
                PetService.Pet2(p,966,967,968);model(p.newPet);
                PinkFixture.check(first.master==null && first.zone==null && !p.zone.getHumanoids().contains(first),"repeated summon fully retires prior actor");
                var replaced=p.newPet;replaced.dispose();p.newPet=null;p.sendNewPet();model(p.newPet);
                PinkFixture.check(p.newPet!=replaced && p.newPet.master==p,"equipped item reconstructs tiger on login/setup");
                p.newPet.joinMapMaster();var unequipped=p.newPet;
                InventoryService.gI().itemBodyToBag(p,7);
                PinkFixture.check(!p.inventory.itemsBody.get(7).isNotNullItem() && p.inventory.itemsBag.contains(tiger) && p.newPet==null,"unequip returns exact tiger and clears owner summon");
                PinkFixture.check(unequipped.master==null && unequipped.zone==null && !p.zone.getHumanoids().contains(unequipped),"unequip removes actor from zone");
                PinkFixture.check(p.nPoint.hpMax==hp && p.nPoint.mpMax==ki && p.nPoint.dame==damage,"unequip restores baseline stats");
                p.sendNewPet();PinkFixture.check(p.newPet==null,"empty body7 never recreates tiger");
                System.out.println("PASS tiger942 option="+option+" equip/use/stat/summon/repeat/setup/unequip");
            }
            System.out.println("TIGER_PET_AUDIT PASS 3 legitimate stat profiles; no server-side tiger lifecycle failure reproduced");System.exit(0);
        }catch(Throwable e){e.printStackTrace();System.exit(1);}
    }
}
