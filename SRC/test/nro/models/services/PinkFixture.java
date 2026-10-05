package nro.models.services;

import java.lang.reflect.*;
import java.util.*;
import sun.misc.Unsafe;
import nro.models.player.*;
import nro.models.item.Item;
import nro.models.map.*;
import nro.models.network.Message;
import nro.models.player_system.Template;

public final class PinkFixture {
    public static final Unsafe U;
    static { try { Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);U=(Unsafe)f.get(null); } catch(Exception e){throw new RuntimeException(e);} }
    public static <T> T blank(Class<T> type)throws Exception{return type.cast(U.allocateInstance(type));}
    public static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void replace(Class<?> type,String name,Object value)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    public static class Owner extends Player { @Override public void sendMessage(Message message){} }
    public static Owner player(long id)throws Exception{Owner p=blank(Owner.class);p.id=id;p.isPlayer=true;p.inventory=new Inventory();p.event=new PlayerEvent(p);p.kolQuestState=new KolQuestState();return p;}
    public static Item item(int id,int quantity){Item i=new Item();i.template=template(id);i.quantity=quantity;return i;}
    public static Template.ItemTemplate template(int id){var t=new Template.ItemTemplate();t.id=(short)id;t.type=27;t.name="fixture";t.isUpToUp=true;return t;}
    public static class Terrain extends nro.models.map.Map {public Terrain(){super(5,"fixture",(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,null,null,240,240,List.of());}public int lastX;@Override public int yPhysicInTop(int x,int y){lastX=x;return 100+x;}}
    public static Zone zone()throws Exception{Terrain m=blank(Terrain.class);m.mapId=5;m.mapWidth=240;return new Zone(m,0,20);}
    public static void owner(ItemMap i,boolean keep)throws Exception{try{ItemMap.class.getField("keepOwnerUntilDespawn").setBoolean(i,keep);}catch(NoSuchFieldException oldVersion){}}
    public static boolean kept(ItemMap i)throws Exception{return ItemMap.class.getField("keepOwnerUntilDespawn").getBoolean(i);}
    public static final List<Item.ItemOption> OPTIONS=List.of(new Item.ItemOption(new Template.ItemOptionTemplate(),37));
    public static void install()throws Exception{
        replace(ItemService.class,"i",new ItemService(){@Override public Template.ItemTemplate getTemplate(int id){return template(id);}@Override public Template.ItemOptionTemplate getItemOptionTemplate(int id){var t=new Template.ItemOptionTemplate();t.id=id;t.name="fixture #";return t;}@Override public List<Item.ItemOption> getListOptionItemShop(short id){return OPTIONS;}@Override public Item createItemFromItemMap(ItemMap m){Item i=item(m.itemTemplate.id,m.quantity);return i;}});
        replace(Service.class,"instance",new Service(){@Override public void dropItemMap(Zone z,ItemMap m){}@Override public void sendThongBao(Player p,String s){}@Override public void sendMoney(Player p){}@Override public void sendToAntherMePickItem(Player p,int id){}@Override public void updatePlayerTotalDamage(Player p){}@Override public void sendMessAllPlayerInMap(Zone z,Message m){}});
        replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}@Override public boolean addItemBag(Player p,Item i){p.inventory.itemsBag.add(i);i.quantity=0;return true;}});
        replace(TaskService.class,"i",new TaskService(){@Override public void checkDoneTaskPickItem(Player p,ItemMap i){}@Override public void checkDoneSideTaskPickItem(Player p,ItemMap i){}@Override public void checkDoneClanTaskPickItem(Player p,ItemMap i){}@Override public void checkDoneTaskKillBoss(Player p,nro.models.boss.Boss b){}});
    }
}
