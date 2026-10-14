package nro.models.services;

import java.lang.reflect.Method;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services_func.UseItem;

public final class KuramaUseTest {
    static void use(Player p,Item i)throws Exception {
        p.inventory.itemsBag.set(0,i);
        Method m=UseItem.class.getDeclaredMethod("useItem",Player.class,Item.class,int.class);m.setAccessible(true);m.invoke(UseItem.gI(),p,i,0);
    }
    public static void main(String[] args) {
        try {
            DiscipleCompanionTest.fixture();
            for(int id=Manager.ITEM_TEMPLATES.size();id<=2019;id++) {var t=new Template.ItemTemplate();t.id=(short)id;t.type=27;t.gender=3;t.name="Fixture";Manager.ITEM_TEMPLATES.add(t);}
            Player p=DiscipleCompanionTest.owner();Item fox=DiscipleCompanionTest.item(2019,77);
            use(p,fox);PinkFixture.check(p.inventory.itemsBody.get(7)==fox,"Use Kurama equips slot 7");
            PinkFixture.check(p.newPet!=null && p.newPet.getHead()==2129,"Use summons correct Kurama actor");
            var first=p.newPet;Item cat=DiscipleCompanionTest.item(892,77);use(p,cat);
            PinkFixture.check(p.inventory.itemsBag.get(0)==fox && p.newPet.getHead()==882 && first.master==null,"Replacement returns item and retires Kurama");
            p.nPoint.power=1;fox.template.strRequire=100;use(p,fox);
            PinkFixture.check(p.inventory.itemsBag.get(0)==fox && p.inventory.itemsBody.get(7)==cat && p.newPet.getHead()==882,"Power rejection cannot summon ghost Kurama");
            InventoryService.gI().itemBodyToBag(p,7);PinkFixture.check(p.newPet==null,"Unequip clears actor");
            System.out.println("PASS Kurama use");System.exit(0);
        }catch(Throwable t){t.printStackTrace();System.exit(1);}
    }
}
