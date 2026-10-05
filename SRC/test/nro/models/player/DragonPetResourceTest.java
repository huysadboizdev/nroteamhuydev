package nro.models.player;

import java.lang.reflect.*;
import nro.models.item.Item;
import nro.models.player_system.Template;
import nro.models.services.*;
import nro.models.services_func.UseItem;

/** Real equip and login restore paths for all seven dragon variants, offline. */
public class DragonPetResourceTest {
    static void replace(Class<?> c,String field,Object v)throws Exception {
        Field f=c.getDeclaredField(field);f.setAccessible(true);f.set(null,v);
    }
    static Player owner(int id) {
        Player p=new Player();p.isPlayer=true;p.nPoint.power=100000000000L;
        for(int i=0;i<12;i++)p.inventory.itemsBody.add(new Item());
        Item it=new Item();it.template=new Template.ItemTemplate();
        it.template.id=(short)id;it.template.type=27;it.template.gender=3;it.quantity=1;
        p.inventory.itemsBag.add(it);
        return p;
    }
    static void check(Player p,int id,String mode) {
        int head=1662+(id-1765)*3;
        if(p.newPet==null || p.newPet.getHead()!=head || p.newPet.getBody()!=head+1 || p.newPet.getLeg()!=head+2)
            throw new AssertionError(mode+" pet "+id+": expected "+head+","+(head+1)+","+(head+2)+
                "; got "+(p.newPet==null?"null":p.newPet.getHead()+","+p.newPet.getBody()+","+p.newPet.getLeg()));
    }
    public static void main(String[] args)throws Exception {
        replace(Service.class,"instance",new Service(){@Override public void point(Player p){}});
        replace(InventoryService.class,"I",new InventoryService(){
            @Override public void itemBagToBody(Player p,int i){p.inventory.itemsBody.set(7,p.inventory.itemsBag.get(i));}
            @Override public void sendItemBags(Player p){}
        });
        replace(TaskService.class,"i",new TaskService(){@Override public void checkDoneTaskUseItem(Player p,Item it){}});
        Method use=UseItem.class.getDeclaredMethod("useItem",Player.class,Item.class,int.class);use.setAccessible(true);
        int count=0;
        for(int id=1765;id<=1771;id++) {
            Player p=owner(id);
            use.invoke(UseItem.gI(),p,p.inventory.itemsBag.get(0),0);check(p,id,"equip");count++;
            p=owner(id);p.inventory.itemsBody.set(7,p.inventory.itemsBag.get(0));
            p.sendNewPet();check(p,id,"login");count++;
            Item it=p.inventory.itemsBag.get(0);RewardService.gI().initChiSoItem(it);
            if(!it.itemOptions.isEmpty())throw new AssertionError("Unexpected base options for pet "+id);
        }
        if(nro.models.data.DataGame.vsItem<13)throw new AssertionError("Item catalog version must be at least 13 to refresh dragon collection descriptions");
        System.out.println("DRAGON_EQUIP_AND_LOGIN="+count+"/14 PASS; INIT_BASE_OPTIONS_EMPTY=7/7; ITEM_VERSION="+nro.models.data.DataGame.vsItem);
    }
}
