package nro.models.services;

import java.lang.reflect.Method;
import nro.models.player.*;
import nro.models.item.Item;
import nro.models.shop.*;
import static nro.models.services.PinkFixture.*;

public class AdvancedPetExchangeTest {
    public static void main(String[] args) throws Exception {
        install();
        Class<?> rule=Class.forName("nro.models.services.AdvancedPetExchange");
        Method eligible=rule.getMethod("hasEligiblePet",Player.class);
        Method exchange=rule.getMethod("exchange",Player.class,Item.class);
        Owner p=player(100);
        check(!(boolean)eligible.invoke(null,p),"no pet hidden");
        p.pet=blank(Pet.class);p.pet.inventory=new Inventory();
        replace(PetService.class,"instance",new PetService(){
            private void next(Player owner,int type){try {owner.pet=blank(Pet.class);owner.pet.typePet=(byte)type;owner.pet.inventory=new Inventory();}catch(Exception e){throw new RuntimeException(e);}}
            @Override public void changeUubPet(Player owner){next(owner,2);}
            @Override public void changeKidBeerPet(Player owner){next(owner,3);}
            @Override public void changeJirenPet(Player owner){next(owner,4);}
        });
        for(int type=0;type<=5;type++) {
            p.pet.typePet=(byte)type;
            check((boolean)eligible.invoke(null,p)==(type>=2&&type<=4),"eligibility type "+type);
        }
        for(int type=2;type<=4;type++) {
            p.pet.typePet=(byte)type;
            Item token=item(1890,2);p.inventory.itemsBag.clear();p.inventory.itemsBag.add(token);
            exchange.invoke(null,p,token);
            check(p.pet.typePet>=2&&p.pet.typePet<=4&&p.pet.typePet!=type,"different advanced pet");
            check(token.quantity==1,"consume exactly one");
        }
        Item token=item(1890,2);p.inventory.itemsBag.clear();p.inventory.itemsBag.add(token);
        p.pet.inventory.itemsBody.add(item(650,1));Pet before=p.pet;
        exchange.invoke(null,p,token);
        check(p.pet==before&&token.quantity==2,"equipped pet preserved");
        p.pet.inventory.itemsBody.clear();p.inventory.itemsBag.clear();
        exchange.invoke(null,p,token);
        check(p.pet==before&&token.quantity==2,"stale item rejected");
        p.inventory.itemsBag.add(token);p.pet.typePet=1;
        exchange.invoke(null,p,token);
        check(p.pet==before&&token.quantity==2,"normal/Mabu pet unchanged");
        Shop original=new Shop();original.tagName="SANTA";
        TabShop tab=new TabShop();tab.id=17;tab.name="fixture";tab.shop=original;
        ItemShop row=new ItemShop();row.temp=template(1890);row.tabShop=tab;tab.itemShops.add(row);original.tabShops.add(tab);
        Method resolve=ShopService.class.getDeclaredMethod("resolveShop",Player.class,Shop.class,boolean.class);resolve.setAccessible(true);
        for(boolean allGender:new boolean[]{false,true}) {
            // Avoid Santa's unrelated bean-level constructor in this fixture.
            tab.id=0;p.pet.typePet=1;
            Shop personal=(Shop)resolve.invoke(ShopService.gI(),p,original,allGender);
            check(personal.getItemShop(1890)==null,"hidden without advanced pet");
            p.pet.typePet=2;
            personal=(Shop)resolve.invoke(ShopService.gI(),p,original,allGender);
            check(personal.getItemShop(1890)!=null,"visible with advanced pet");
        }
        check(original.getItemShop(1890)!=null,"shared catalog unchanged");
        replace(ItemService.class,"i",new ItemService(){
            @Override public Item createItemFromItemShop(ItemShop item){throw new AssertionError("Rejected request reached item preparation");}
        });
        p.idMark=new IDMark();p.idMark.setShopOpen(original);p.idMark.setTagNameShop("SANTA");
        p.inventory.gem=1000;p.inventory.itemsBag.clear();row.typeSell=1;row.cost=100;
        p.pet.typePet=1;ShopService.gI().buyItem(p,1890);
        check(p.inventory.gem==1000&&p.inventory.itemsBag.isEmpty(),"stale open shop cannot buy without advanced pet");
        p.pet.typePet=2;row.typeSell=3;ShopService.gI().buyItem(p,1890);
        check(p.inventory.gem==1000&&p.inventory.itemsBag.isEmpty(),"wrong currency rejected");
        row.typeSell=1;row.cost=1;ShopService.gI().buyItem(p,1890);
        check(p.inventory.gem==1000&&p.inventory.itemsBag.isEmpty(),"forged price rejected");
        row.cost=100;original.tagName="OTHER";ShopService.gI().buyItem(p,1890);
        check(p.inventory.gem==1000&&p.inventory.itemsBag.isEmpty(),"wrong shop rejected");
        System.out.println("AdvancedPetExchangeTest PASS eligibility, alternate forms, item guards, equipment safety, personal shop filtering");
    }
}
