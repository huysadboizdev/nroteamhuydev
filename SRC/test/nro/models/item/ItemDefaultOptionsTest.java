package nro.models.item;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.ItemService;
import nro.models.shop.ItemShop;

/** SQL catalog -> creation -> restoration: no double stats and no timer refill. */
public final class ItemDefaultOptionsTest {
    private static final int[][] ROWS = {
        {710,50,22},{710,77,20},{710,103,20},{710,159,3},{710,160,35},
        {711,50,23},{711,77,21},{711,103,21},{711,159,4},{711,160,50},
        {1710,50,24},{1710,77,22},{1710,103,22},{1710,159,5},{1710,160,60},
        {1869,77,15},{1869,103,15},{1869,94,5},{1869,47,50000},{1869,9,1000},{1869,251,0}
    };
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    static Connection connection() {
        int[] index={-1};
        ResultSet rs=(ResultSet)Proxy.newProxyInstance(ItemDefaultOptionsTest.class.getClassLoader(),new Class[]{ResultSet.class},(p,m,a)->{
            if(m.getName().equals("next"))return ++index[0]<ROWS.length;
            if(m.getName().equals("getInt"))return ROWS[index[0]][Map.of("item_id",0,"option_id",1,"param",2).get(a[0])];
            if(m.getName().equals("close"))return null;
            throw new UnsupportedOperationException(m.getName());
        });
        Statement st=(Statement)Proxy.newProxyInstance(ItemDefaultOptionsTest.class.getClassLoader(),new Class[]{Statement.class},(p,m,a)->{
            if(m.getName().equals("executeQuery")){index[0]=-1;return rs;}
            if(m.getName().equals("close"))return null;
            throw new UnsupportedOperationException(m.getName());
        });
        return (Connection)Proxy.newProxyInstance(ItemDefaultOptionsTest.class.getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
            if(m.getName().equals("createStatement"))return st;
            throw new UnsupportedOperationException(m.getName());
        });
    }
    static int param(Item item,int id){return item.itemOptions.stream().filter(o->o.optionTemplate.id==id).findFirst().orElseThrow().param;}
    public static void main(String[] args)throws Exception {
        for(int id=0;id<2001;id++){Template.ItemTemplate t=new Template.ItemTemplate();t.id=(short)id;t.name="Item";t.description="Test";t.type=(byte)(id==1869?32:5);Manager.ITEM_TEMPLATES.add(t);}
        for(int id=0;id<252;id++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"Option #",0));
        Class<?> catalog=null;
        try{catalog=Class.forName("nro.models.item.ItemDefaultOptions");catalog.getMethod("loadFromDatabase",Connection.class).invoke(null,connection());}
        catch(ClassNotFoundException missing){/* Baseline must fail the real creation assertion below. */}
        Item a=ItemService.gI().createNewItem((short)1869);
        check(a.itemOptions.size()==6,"New armor must have 6 configured options; got "+a.itemOptions.size());
        check(param(a,47)==50000&&param(a,9)==1000,"Int32 armor and initial duration");
        Item b=ItemService.gI().createNewItem((short)1869);a.itemOptions.stream().filter(o->o.optionTemplate.id==9).findFirst().orElseThrow().param=12;
        check(param(b,9)==1000,"New items cannot share mutable timer options");
        for(int id:new int[]{710,711,1710})check(ItemService.gI().createNewItem((short)id).itemOptions.size()==5,"Costume has exactly five options "+id);
        Method restore=ItemService.class.getMethod("createItemForRestore",short.class,int.class);
        Item old=(Item)restore.invoke(ItemService.gI(),(short)1869,1);
        old.itemOptions.add(new Item.ItemOption(9,12));old.itemOptions.add(new Item.ItemOption(47,60000));old.itemOptions.add(new Item.ItemOption(93,7));
        catalog.getMethod("complete",Item.class).invoke(null,old);
        catalog.getMethod("complete",Item.class).invoke(null,old);
        check(param(old,9)==12&&param(old,47)==60000&&param(old,93)==7&&old.itemOptions.size()==7,"Restore preserves timer, upgrades, other options and is idempotent");
        ItemShop shop=new ItemShop();shop.temp=Manager.ITEM_TEMPLATES.get(1869);
        shop.options.add(new Item.ItemOption(9,8));shop.options.add(new Item.ItemOption(77,18));
        Item bought=ItemService.gI().createItemFromItemShop(shop);
        check(param(bought,9)==8&&param(bought,77)==18&&bought.itemOptions.size()==6,"Explicit shop overrides survive without duplicated defaults");
        check(ItemService.gI().getPercentTrainArmor(a)==50,"Armor removal bonus 50");
        check((int)ItemService.class.getMethod("getTrainArmorWearReduction",Item.class).invoke(ItemService.gI(),a)==40,"Armor wearing reduction 40");
        check(ItemService.gI().isTrainArmor(a),"Armor is recognized for timer countdown");
        System.out.println("ITEM_DEFAULT_OPTIONS=PASS creation, isolated timer, restoration, upgrades, shop, armor 40/50");
    }
}
