package nro.models.services;

import java.io.*;
import java.lang.reflect.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player.Pet;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.network.Message;
import nro.models.npc_list.ChiChi;
import nro.models.map.Map;
import nro.models.map.Zone;
import sun.misc.Unsafe;

/** Offline real inventory/NPC tests, reflection permits RED against the previous JAR. */
public class TimedBagCleanupTest {
    static final long NOW=Instant.parse("2026-10-04T16:59:59Z").toEpochMilli();
    static int saves, bags, bodies;
    static boolean saveOk=true;
    static final List<String> notices=new ArrayList<>();
    static class Owner extends Player {
        final List<byte[]> menus=new ArrayList<>();
        @Override public void sendMessage(Message m) {if(m.command==32)menus.add(m.getData());}
    }
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    static void replace(Class<?> c,String n,Object o)throws Exception {Field f=c.getDeclaredField(n);f.setAccessible(true);f.set(null,o);}
    static Object service()throws Exception {
        Class<?> c;
        try{c=Class.forName("nro.models.services.TimedBagCleanupService");}
        catch(ClassNotFoundException e){throw new AssertionError("Missing timed bag cleanup feature",e);}
        return c.getConstructor(Predicate.class).newInstance((Predicate<Player>)p->{saves++;return saveOk;});
    }
    static Object call(Object s,String n,Class<?>[] types,Object...a)throws Exception {
        try{return s.getClass().getMethod(n,types).invoke(s,a);}
        catch(InvocationTargetException e){if(e.getCause() instanceof Error x)throw x;throw e;}
    }
    static String preview(Object s,Player p)throws Exception{return (String)call(s,"prepare",new Class[]{Player.class,long.class},p,NOW);}
    static String confirm(Object s,Player p)throws Exception{return (String)call(s,"confirm",new Class[]{Player.class,long.class},p,NOW);}
    static Item item(int days,int quantity){Item i=new Item();i.template=Manager.ITEM_TEMPLATES.get(0);i.quantity=quantity;i.createTime=NOW;if(days>=0)i.itemOptions.add(new Item.ItemOption(93,days));return i;}
    static Owner owner(){Owner p=new Owner();p.id=123;p.name="cleanup-test";p.isPlayer=true;p.idMark.setLoadedAllDataPlayer(true);p.inventory.itemsBag.add(item(0,3));p.inventory.itemsBag.add(item(7,2));p.inventory.itemsBag.add(item(-1,5));p.inventory.itemsBody.add(item(0,1));p.inventory.itemsBox.add(item(0,4));p.pet=new Pet(p);p.pet.inventory.itemsBody.add(item(0,1));return p;}
    static void scope()throws Exception {
        Object s=service();Owner p=owner();Item body=p.inventory.itemsBody.get(0),box=p.inventory.itemsBox.get(0),pet=p.pet.inventory.itemsBody.get(0),permanent=p.inventory.itemsBag.get(2);
        String text=preview(s,p);check(text.equals("Bạn có muốn bỏ đồ có hạn sử dụng trong hành trang không?"),"Confirmation is one short question without item list");check(saves==0&&p.inventory.itemsBag.get(0).quantity==3,"Preview never mutates inventory");
        confirm(s,p);check(!p.inventory.itemsBag.get(0).isNotNullItem()&&!p.inventory.itemsBag.get(1).isNotNullItem(),"Delete zero-day and positive-day entire stacks");
        check(p.inventory.itemsBag.get(2)==permanent&&p.inventory.itemsBody.get(0)==body&&p.inventory.itemsBox.get(0)==box&&p.pet.inventory.itemsBody.get(0)==pet,"Other containers and permanent items preserved");check(saves==1&&bags==1,"One save and refresh");
        confirm(s,p);check(saves==1,"Replay does not save or delete again");
    }
    static void stale()throws Exception {
        for(int mode=0;mode<4;mode++){
            Object s=service();Owner p=owner();preview(s,p);Item original=p.inventory.itemsBag.get(0);
            if(mode==0){p.inventory.itemsBody.set(0,original);p.inventory.itemsBag.set(0,item(-1,1));}
            if(mode==1)original.quantity++;
            if(mode==2)original.getOptionById(93).param++;
            if(mode==3)p.inventory.itemsBag.add(item(2,1));
            String result=confirm(s,p);check(result.contains("thay đổi"),"Stale preview rejected: "+mode);check(saves==0&&p.inventory.itemsBag.get(1).isNotNullItem(),"No partial removal on stale data");
        }
    }
    static void guards()throws Exception {
        Object s=service();Owner p=owner();preview(s,p);call(s,"cancel",new Class[]{Player.class},p);confirm(s,p);check(saves==0,"Cancellation clears confirmation");
        Owner empty=owner();empty.inventory.itemsBag.clear();check(preview(s,empty).contains("không có"),"Empty inventory message");confirm(s,empty);check(saves==0,"Empty preview cannot confirm");
        p.idMark.setPlayerTrade(new Player());preview(s,p);confirm(s,p);check(saves==0,"Block while trading");p.idMark.setPlayerTrade(null);
        preview(s,p);p.beforeDispose=true;confirm(s,p);check(saves==0,"Block during logout");
        Owner unloaded=owner();unloaded.idMark.setLoadedAllDataPlayer(false);preview(s,unloaded);confirm(s,unloaded);check(saves==0,"Never save unloaded player");
        Owner expired=owner();preview(s,expired);call(s,"confirm",new Class[]{Player.class,long.class},expired,NOW+120_001);check(saves==0&&expired.inventory.itemsBag.get(0).isNotNullItem(),"Expired confirmation cannot delete");
        Owner alias=owner();alias.inventory.itemsBody.set(0,alias.inventory.itemsBag.get(0));preview(s,alias);confirm(s,alias);check(alias.inventory.itemsBag.get(0).isNotNullItem()&&alias.inventory.itemsBody.get(0).isNotNullItem(),"Even aliased equipment is protected");
    }
    static void failure()throws Exception {
        Object s=service();Owner p=owner();Item a=p.inventory.itemsBag.get(0),b=p.inventory.itemsBag.get(1);preview(s,p);saveOk=false;
        check(confirm(s,p).contains("lưu"),"Save failure is visible");check(p.inventory.itemsBag.get(0)==a&&p.inventory.itemsBag.get(1)==b&&a.quantity==3&&a.template!=null,"Save failure restores originals without dispose");check(bags==0,"No success packet on failed save");
    }
    static void calendar()throws Exception {
        Method m;
        try{m=ItemService.class.getMethod("isEquippedOutOfDateTime",Item.class,long.class);}catch(NoSuchMethodException e){throw new AssertionError("Missing calendar-day equipped expiration",e);}
        Item zero=item(0,1);check(!(boolean)m.invoke(ItemService.gI(),zero,NOW),"Zero-day equipped survives current day");check((boolean)m.invoke(ItemService.gI(),zero,NOW+1000),"Zero-day equipped expires at next midnight, not after 24h");
        Item one=item(1,1);check(!(boolean)m.invoke(ItemService.gI(),one,NOW+1000)&&one.getOptionById(93).param==0,"One-day becomes zero at midnight");check(!(boolean)m.invoke(ItemService.gI(),one,NOW+2000),"Repeated check keeps zero-day equipment");check((boolean)m.invoke(ItemService.gI(),one,NOW+86_401_000),"Expires after zero-day has ended");
        Item future=item(0,1);future.createTime=NOW+86_400_000;check(!(boolean)m.invoke(ItemService.gI(),future,NOW),"Future timestamp never deducts days");
        Item permanent=item(-1,1);check(!(boolean)m.invoke(ItemService.gI(),permanent,NOW+999_999_999L),"Permanent equipment never expires");
    }
    static void online()throws Exception {
        Object s=service();Owner p=owner();p.pet=null;Item bag=p.inventory.itemsBag.get(0),box=p.inventory.itemsBox.get(0);
        call(s,"updateEquipped",new Class[]{Player.class,long.class},p,NOW);check(p.inventory.itemsBody.get(0).isNotNullItem(),"Worn zero-day remains today");
        call(s,"updateEquipped",new Class[]{Player.class,long.class},p,NOW+1000);check(!p.inventory.itemsBody.get(0).isNotNullItem(),"Online expiry removes worn item");check(p.inventory.itemsBag.get(0)==bag&&p.inventory.itemsBox.get(0)==box,"Online worn expiration never sweeps bag or house box");check(bodies>0,"Refresh equipped items");
        Owner petOwner=owner();call(s,"updateEquipped",new Class[]{Player.class,long.class},petOwner,NOW+1000);check(!petOwner.pet.inventory.itemsBody.get(0).isNotNullItem(),"Pet worn expiry follows same calendar rule");
        Owner daily=owner();daily.pet=null;daily.inventory.itemsBody.set(0,item(1,1));notices.clear();call(s,"updateEquipped",new Class[]{Player.class,long.class},daily,NOW+1000);check(daily.inventory.itemsBody.get(0).getOptionById(93).param==0&&notices.isEmpty(),"One to zero refresh never claims item was removed");
    }
    static void concurrent()throws Exception {
        Object s=service();Owner p=owner();preview(s,p);ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch latch=new CountDownLatch(1);
        Callable<String> action=()->{latch.await();return confirm(s,p);};Future<String>a=pool.submit(action),b=pool.submit(action);latch.countDown();a.get();b.get();pool.shutdown();check(saves==1,"Concurrent confirmations delete/save once");
    }
    static void menu()throws Exception {
        Object s=service();replace(s.getClass(),"instance",s);Owner p=owner();Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);Unsafe u=(Unsafe)f.get(null);Map map=(Map)u.allocateInstance(Map.class);map.mapId=5;p.zone=new Zone(map,0,20);
        ChiChi n=new ChiChi(5,1,100,100,81,0);n.openBaseMenu(p);DataInputStream in=new DataInputStream(new ByteArrayInputStream(p.menus.get(0)));in.readShort();in.readUTF();check(in.readByte()==6,"Six Chi Chi menu buttons");for(int i=0;i<4;i++)in.readUTF();check(in.readUTF().contains("hạn"),"Cleanup button before close");check(in.readUTF().equals("Đóng"),"Close remains last");
        n.confirmMenu(p,4);check(p.menus.size()==2&&saves==0,"NPC shows confirmation before removal");
        DataInputStream dialog=new DataInputStream(new ByteArrayInputStream(p.menus.get(1)));dialog.readShort();check(dialog.readUTF().equals("Bạn có muốn bỏ đồ có hạn sử dụng trong hành trang không?"),"Short confirmation text on wire");check(dialog.readByte()==2&&dialog.readUTF().equals("Có")&&dialog.readUTF().equals("Không"),"Only yes and no buttons");
        n.confirmMenu(p,1);check(saves==0,"NPC cancel keeps items");n.openBaseMenu(p);n.confirmMenu(p,4);n.confirmMenu(p,0);check(saves==1&&!p.inventory.itemsBag.get(0).isNotNullItem(),"NPC confirm routes to bag cleanup");n.confirmMenu(p,0);check(saves==1,"NPC replay guarded");
    }
    public static void main(String[]args)throws Exception {
        for(int i=0;i<220;i++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(i,"Option #",0));Template.ItemTemplate t=new Template.ItemTemplate();t.id=0;t.type=0;t.name="Áo thử";Manager.ITEM_TEMPLATES.add(t);
        replace(Service.class,"instance",new Service(){@Override public void sendThongBao(Player p,String s){notices.add(s);}@Override public void point(Player p){}@Override public void Send_Caitrang(Player p){}@Override public void sendFlagBag(Player p){}@Override public void showInfoPet(Player p){}});
        replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){bags++;}@Override public void sendItemBody(Player p){bodies++;}});
        switch(args[0]){case "scope"->scope();case "stale"->stale();case "guards"->guards();case "failure"->failure();case "calendar"->calendar();case "online"->online();case "concurrent"->concurrent();case "menu"->menu();default->throw new AssertionError("Unknown mode");}
        System.out.println("PASS: Timed bag cleanup "+args[0]);
    }
}
