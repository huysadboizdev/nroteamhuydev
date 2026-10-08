package nro.models.services;

import java.util.*;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.map.ItemMap;
import nro.models.map.Zone;

public final class MillionItemStackTest {
    static final int LIMIT=1_000_000;
    static InventoryService inventory;
    static Item item(int quantity) {
        Item i=PinkFixture.item(663,quantity);
        i.itemOptions.add(new Item.ItemOption(73,0));
        return i;
    }
    static List<Item> slots(Item... items){return new ArrayList<>(Arrays.asList(items));}
    static void check(boolean value,String message){PinkFixture.check(value,message);}
    public static void main(String[] args)throws Exception {
        PinkFixture.install();inventory=new InventoryService();
        PinkFixture.replace(InventoryService.class,"I",inventory);
        check(InventoryService.class.getField("MAX_STACK_QUANTITY").getInt(null)==LIMIT,"stack limit must be 1,000,000");
        var bag=slots(item(99));Item incoming=item(1);
        check(inventory.canAddItemList(bag,incoming)&&inventory.addItemList(bag,incoming),"full bag accepts compatible stack above99");
        check(bag.get(0).quantity==100&&incoming.quantity==0,"99 becomes100");
        bag=slots(item(LIMIT-1));incoming=item(1);
        check(inventory.addItemList(bag,incoming)&&bag.get(0).quantity==LIMIT,"exact million fits");
        incoming=item(1);
        check(!inventory.canAddItemList(bag,incoming)&&!inventory.addItemList(bag,incoming),"full million rejects extra");
        check(bag.get(0).quantity==LIMIT&&incoming.quantity==1,"rejected grant unchanged");
        bag=slots(item(LIMIT-1),new Item());incoming=item(3);
        check(inventory.addItemList(bag,incoming)&&bag.get(0).quantity==LIMIT&&bag.get(1).quantity==2,"overflow fills then spills");
        bag=slots(new Item(),new Item());incoming=item(LIMIT+7);
        check(inventory.addItemList(bag,incoming)&&bag.get(0).quantity==LIMIT&&bag.get(1).quantity==7&&incoming.quantity==0,"large grant split across empty slots");
        bag=slots(item(LIMIT-1),new Item());incoming=item(LIMIT+2);
        check(!inventory.canAddItemList(bag,incoming)&&!inventory.addItemList(bag,incoming),"preflight rejects insufficient total capacity");
        check(bag.get(0).quantity==LIMIT-1&&!bag.get(1).isNotNullItem()&&incoming.quantity==LIMIT+2,"no partial grant");
        bag=slots(item(LIMIT+20),new Item());incoming=item(3);
        check(inventory.addItemList(bag,incoming)&&bag.get(0).quantity==LIMIT+20&&bag.get(1).quantity==3,"legacy oversized stack preserved");
        bag=slots(item(99));incoming=item(1);incoming.itemOptions.add(new Item.ItemOption(50,5));
        check(!inventory.addItemList(bag,incoming)&&bag.get(0).quantity==99,"different options do not merge");
        bag=slots(item(99));bag.get(0).template.isUpToUp=false;incoming=item(1);incoming.template.isUpToUp=false;
        check(!inventory.addItemList(bag,incoming),"nonstackable equipment cannot merge");
        bag=slots(new Item());incoming=item(LIMIT+5);incoming.template.isUpToUp=false;
        check(inventory.addItemList(bag,incoming)&&bag.get(0).quantity==LIMIT+5,"nonstackable legacy behavior unchanged");
        Player p=PinkFixture.player(99111);p.inventory.itemsBox=slots(item(99));
        check(inventory.addItemBox(p,item(1))&&p.inventory.itemsBox.get(0).quantity==100,"box shares limit");
        p.inventory.itemsBag=slots(item(99));Zone z=PinkFixture.zone();p.zone=z;
        ItemMap drop=new ItemMap(z,663,1,40,100,p.id);z.addItem(drop);
        z.pickItem(p,drop.itemMapId);
        check(p.inventory.itemsBag.get(0).quantity==100&&drop.isPickedUp&&!z.items.contains(drop),"actual pickup over99 in full bag");
        p.inventory.itemsBag=slots(item(LIMIT));drop=new ItemMap(z,663,1,40,100,p.id);z.addItem(drop);
        z.pickItem(p,drop.itemMapId);
        check(p.inventory.itemsBag.get(0).quantity==LIMIT&&!drop.isPickedUp&&z.items.contains(drop),"full rejected drop remains on map");
        System.out.println("PASS million stacks, atomic overflow, compatibility, legacy, box and actual pickup");
    }
}
