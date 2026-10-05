package nro.models.network;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import sun.misc.Unsafe;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.*;
import nro.models.shop.ShopService;

public final class LargeItemQuantityTest {
    static class Owner extends Player {
        byte[] packet;
        @Override public void sendMessage(Message m){packet=m.getData();}
    }
    static Unsafe unsafe;
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);unsafe=(Unsafe)f.get(null);
        for(int id=0;id<=935;id++){
            Template.ItemTemplate t=new Template.ItemTemplate();t.id=(short)id;t.name="Item "+id;t.type=27;t.isUpToUp=true;
            Manager.ITEM_TEMPLATES.add(t);
        }
        for(int id=0;id<256;id++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"Count #",0));
        switch(args[0]){case "stack"->stack();case "packets"->packets();default->throw new AssertionError();}
        System.out.println("PASS: large item "+args[0]);
    }
    static Item item(int id,int quantity,int option,int param){Item i=new Item((short)id);i.quantity=quantity;i.itemOptions.add(new Item.ItemOption(option,param));return i;}
    static void stack(){
        for(int quantity:new int[]{100000,1000000,10000000,99999999}){
            List<Item> bag=new ArrayList<>(List.of(item(933,1,73,0)));
            Item reward=item(933,quantity,73,0);
            check(InventoryService.gI().addItemList(bag,reward),"Existing stack can receive "+quantity+" without an empty slot");
            check(bag.get(0).quantity==quantity+1 && reward.quantity==0,"Full additive count");
            bag=new ArrayList<>(List.of(item(934,1,31,1)));
            reward=item(934,1,31,quantity);
            check(InventoryService.gI().addItemList(bag,reward) && bag.get(0).itemOptions.get(0).param==quantity+1,"Option quantity adds exactly");
        }
        List<Item> bag=new ArrayList<>(List.of(item(933,Integer.MAX_VALUE-5,73,0)));
        Item reward=item(933,10,73,0);
        check(!InventoryService.gI().addItemList(bag,reward),"Insufficient capacity rejected");
        check(bag.get(0).quantity==Integer.MAX_VALUE-5 && reward.quantity==10,"No partial loss on failure");
        bag.add(new Item());check(InventoryService.gI().addItemList(bag,reward),"Overflow remainder fits free slot");
        check(bag.get(0).quantity==Integer.MAX_VALUE && bag.get(1).quantity==5,"No integer overflow");
        bag=new ArrayList<>(List.of(item(934,1,31,Integer.MAX_VALUE-5)));reward=item(934,1,31,10);
        check(!InventoryService.gI().addItemList(bag,reward),"Option overflow rejected");
        check(bag.get(0).itemOptions.get(0).param==Integer.MAX_VALUE-5 && reward.quantity==1,"Option not corrupted on overflow");
    }
    static Owner owner(boolean extended)throws Exception{
        Owner p=new Owner();p.session=(MySession)unsafe.allocateInstance(MySession.class);
        p.session.itemOptionsInt32=extended;p.session.version=248;return p;
    }
    static int param(DataInputStream in,boolean extended)throws Exception{return extended?in.readInt():in.readUnsignedShort();}
    static void packets()throws Exception{
        for(boolean extended:new boolean[]{false,true}){
            Owner p=owner(extended);p.inventory.itemsBoxCrackBall.add(item(935,99999999,73,0));
            ShopService.gI().opendShop(p,"ITEMS_LUCKY_ROUND",false);
            DataInputStream in=new DataInputStream(new ByteArrayInputStream(p.packet));
            check(in.readByte()==4 && in.readByte()==1,"Reward shop type/tabs");in.readUTF();
            check(in.readByte()==1 && in.readShort()==935,"Reward item");in.readUTF();
            check(in.readByte()==2,"Two reward options");
            check(in.readUnsignedByte()==73 && param(in,extended)==0,"Normal option");
            check(in.readUnsignedByte()==31 && param(in,extended)==(extended?99999999:65535),"Synthetic quantity option uses correct width");
            check(in.readByte()==1 && in.readByte()==0 && in.available()==0,"Reward shop packet aligned");
            Service.gI().openFlagUI(p);in=new DataInputStream(new ByteArrayInputStream(p.packet));
            check(in.readByte()==0,"Flag menu action");int count=in.readUnsignedByte();
            check(count==Service.flagTempId.length,"Flag count");
            for(int n=0;n<count;n++){
                int id=in.readShort();check(in.readUnsignedByte()==1,"One flag option");
                int opt=in.readUnsignedByte();int value=param(in,extended);
                check(opt==(id==363?73:88) && value==(id==363?0:id==371?10:5),"Flag packet option "+id);
            }
            check(in.available()==0,"Flag packet aligned");
        }
    }
}
