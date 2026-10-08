package nro.models.combine;

import java.lang.reflect.Field;
import nro.models.player.*;
import nro.models.item.Item;
import nro.models.services.*;
import nro.models.npc.Npc;
import static nro.models.services.PinkFixture.*;

final class ThienSuCraftFixture {
    static boolean failFactory, failGrant;
    static int factoryCalls,menus,bagPackets,bodyPackets,points,moneyPackets;
    static String menu;
    public static class CaptureNpc extends Npc {
        public CaptureNpc(){super(154,1,0,0,56,0);}
        @Override public void openBaseMenu(Player p){}
        @Override public void confirmMenu(Player p,int select){}
        @Override public void createOtherMenu(Player p,int index,String text,String... options){menus++;menu=text;p.idMark.setIndexMenu(index);}
    }
    static void installAll()throws Exception {
        install();
        failFactory=false;failGrant=false;factoryCalls=menus=bagPackets=bodyPackets=points=moneyPackets=0;
        replace(ItemService.class,"i",new ItemService(){
            @Override public nro.models.player_system.Template.ItemTemplate getTemplate(int id){var t=template(id);if(id>=1048&&id<=1062)t.name="kết quả Thiên Sứ";return t;}
            @Override public nro.models.player_system.Template.ItemOptionTemplate getItemOptionTemplate(int id){var t=new nro.models.player_system.Template.ItemOptionTemplate();t.id=id;t.name="fixture #";return t;}
            @Override public Item DoThienSu(int id,int gender){factoryCalls++;if(failFactory)throw new IllegalStateException("output failed");Item i=ThienSuCraftStatsTest.equipment(id,1000);i.template.gender=(byte)((id-1048)%3);i.itemOptions.get(1).param=30;return i;}
        });
        replace(InventoryService.class,"I",new InventoryService(){
            @Override public void sendItemBags(Player p){bagPackets++;}
            @Override public void sendItemBody(Player p){bodyPackets++;}
            @Override public boolean addItemList(java.util.List<Item> bag,Item i){return !failGrant&&super.addItemList(bag,i);}
        });
        replace(Service.class,"instance",new Service(){
            @Override public void sendThongBao(Player p,String text){}
            @Override public void sendMoney(Player p){moneyPackets++;}
            @Override public void point(Player p){points++;}
            @Override public void Send_Caitrang(Player p){}
        });
        CombineService service=blank(CombineService.class);
        Field f=CombineService.class.getDeclaredField("whis");f.setAccessible(true);f.set(service,blank(CaptureNpc.class));
        replace(CombineService.class,"instance",service);
    }
    static Player ready(int fragment)throws Exception {
        Player p=ThienSuCraftRecipeTest.ready(fragment,1084);p.idMark=new IDMark();
        p.inventory.gold=12_345_678L;p.inventory.ruby=100;
        return p;
    }
    static void preview(Player p){CombineService.gI().showInfoCombine(p,new int[]{0,1,2,3});}
    static boolean execute(Player p,java.util.function.IntUnaryOperator random)throws Exception {
        try{return (boolean)CheTaoTrangBiThienSu.class.getMethod("execute",Player.class,java.util.function.IntUnaryOperator.class).invoke(null,p,random);}
        catch(java.lang.reflect.InvocationTargetException e){throw (RuntimeException)e.getCause();}
    }
}
