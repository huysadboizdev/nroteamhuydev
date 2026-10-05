package nro.models.shop_ky_gui;

import java.io.*;
import java.util.*;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.services.*;
import static nro.models.services.PinkFixture.*;

/** Reads the shop packet in the same order as PC Controller -44, then buys its wire ID. */
public final class ConsignBuyWireTest {
    public static class Viewer extends Player {
        byte[] shop;
        @Override public void sendMessage(Message m){if(m.command==-44)shop=m.getData();}
    }
    public static void main(String[] args)throws Exception {
        install();
        replace(ItemService.class,"i",new ItemService(){
            @Override public nro.models.player_system.Template.ItemTemplate getTemplate(int id){var t=template(id);t.type=1;t.isUpToUp=false;return t;}
            @Override public nro.models.player_system.Template.ItemOptionTemplate getItemOptionTemplate(int id){var t=new nro.models.player_system.Template.ItemOptionTemplate();t.id=id;t.name="fixture #";return t;}
        });
        replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}});
        var manager=new ConsignShopManager(){@Override public void save(){}};replace(ConsignShopManager.class,"instance",manager);
        Viewer p=blank(Viewer.class);Player source=ConsignRegressionTest.owner(100);
        p.id=source.id;p.inventory=source.inventory;p.event=source.event;p.kolQuestState=source.kolQuestState;
        p.nPoint=source.nPoint;p.session=source.session;p.session.player=p;p.inventory.ruby=10_000;
        p.session.version=243;p.session.inventory200=true;p.session.itemOptionsInt32=true;
        var listing=new ConsignItem(4,(short)556,1367,(byte)0,-1,10_000,1,(byte)0,new ArrayList<>(),false);
        listing.currency="ruby";listing.createTime=12345;
        for(int[] o:new int[][]{{6,100000},{35,1},{103,7},{30,1},{87,1}})listing.options.add(new nro.models.item.Item.ItemOption(o[0],o[1]));
        manager.listItem.add(listing);
        var service=new ConsignShopService();var dao=new ConsignRegressionTest.MemoryDAO();service.transactions=new ConsignTransactions(dao,x->true);
        service.openShopKyGui(p);check(p.shop!=null,"shop packet sent");
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(p.shop));
        check(in.readByte()==2&&in.readUnsignedByte()==5,"five consignment tabs");int alias=0;
        for(int tab=0;tab<5;tab++){
            in.readUTF();in.readUnsignedByte();int count=tab==4?in.readUnsignedShort():in.readUnsignedByte();
            for(int n=0;n<count;n++){
                check(in.readShort()==556,"template");alias=in.readShort();
                check(in.readInt()==-1&&in.readInt()==10000&&in.readByte()==0&&in.readInt()==1&&in.readByte()==0,"ruby buyer fields");
                int options=in.readUnsignedByte();for(int k=0;k<options;k++){in.readUnsignedByte();in.readInt();}
                in.readByte();check(in.readByte()==0,"no costume payload");
            }
        }
        check(in.available()==0&&alias>0,"packet parsed completely, valid wire alias");
        service.buyItem(p,alias);
        check(dao.writes==1&&p.inventory.ruby==0&&manager.listItem.get(0).isBuy,"purchase displayed sealed listing");
        check(p.inventory.itemsBag.stream().anyMatch(i->i.template!=null&&i.template.id==556&&i.itemOptions.stream().anyMatch(o->o.optionTemplate.id==103&&o.param==7)),"seal and bonus retained");
        System.out.println("PASS ConsignBuyWire: PC packet parsed, displayed seal listing purchased once with ruby, bonus retained");System.exit(0);
    }
}
