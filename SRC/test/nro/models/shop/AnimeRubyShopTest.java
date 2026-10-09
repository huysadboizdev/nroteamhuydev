package nro.models.shop;

import java.util.*;
import java.util.concurrent.*;
import nro.models.item.Item;
import nro.models.player.*;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.*;
import static nro.models.services.PinkFixture.*;

public final class AnimeRubyShopTest {
    static String lastNotice;
    static final class MenuSanta extends nro.models.npc_list.Santa {
        String[] menu;
        MenuSanta(){super(5,0,0,0,39,0);}
        @Override public boolean canOpenNpc(Player p){return true;}
        @Override public void createOtherMenu(Player p,int index,String text,String... menus){menu=menus;p.idMark.setIndexMenu(index);}
    }
    static InventoryService inventory;
    static Player buyer(int ruby) throws Exception {
        Player p=player(System.nanoTime());p.idMark=new IDMark();p.inventory.ruby=ruby;
        p.inventory.gem=100000;p.inventory.gold=100000000;
        for(int i=0;i<8;i++)p.inventory.itemsBag.add(ItemService.gI().createItemNull());
        Shop shop=new Shop();shop.tagName="SANTA_HONG_NGOC";shop.typeShop=3;shop.npcId=39;
        TabShop tab=new TabShop();tab.id=80;tab.shop=shop;shop.tabShops.add(tab);
        int[] ids={2019,2026,2027,2030,2039},prices={200,200,200,200,200};
        for(int i=0;i<ids.length;i++){ItemShop row=new ItemShop();row.temp=Manager.ITEM_TEMPLATES.get(ids[i]);row.cost=prices[i];row.typeSell=3;row.iconSpec=7743;row.tabShop=tab;tab.itemShops.add(row);}
        p.idMark.setShopOpen(shop);p.idMark.setTagNameShop(shop.tagName);return p;
    }
    static void buy(Player p,int id){ShopService.gI().takeItem(p,(byte)0,id);}
    static long count(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i!=null&&i.template!=null&&i.template.id==id).mapToLong(i->i.quantity).sum();}
    static Item costume(int id){Item i=new Item((short)id);i.quantity=1;return i;}
    public static void main(String[] args) throws Exception {
        for(int id=0;id<2040;id++){Template.ItemTemplate t=template(id);t.type=5;t.isUpToUp=false;t.gender=3;Manager.ITEM_TEMPLATES.add(t);}
        Manager.ITEM_TEMPLATES.get(861).iconID=7743;
        for(int id=0;id<256;id++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"Option #",0));
        replace(Service.class,"instance",new Service(){@Override public void sendThongBao(Player p,String s){lastNotice=s;}@Override public void sendMoney(Player p){}});
        inventory=new InventoryService(){@Override public void sendItemBags(Player p){}};
        replace(InventoryService.class,"I",inventory);
        switch(args[0]) {
            case "profiles":{for(int id:new int[]{2019,2026,2027,2030,2039}){Player p=buyer(200);buy(p,id);check(p.inventory.ruby==0&&count(p,id)==1,"exact 200 ruby for "+id);Item it=p.inventory.itemsBag.stream().filter(i->i.template!=null&&i.template.id==id).findFirst().orElseThrow();check(it.itemOptions.stream().anyMatch(o->o.optionTemplate.id==30),"nontrade grant");check(it.itemOptions.stream().filter(o->o.optionTemplate.id==93).allMatch(o->o.param>=7&&o.param<=30),"valid purchase duration");}break;}
            case "wallet":{Player p=buyer(200);p.inventory.gem=0;buy(p,2027);check(p.inventory.ruby==0&&count(p,2027)==1,"ruby wallet buys without gems");check(p.inventory.gold==100000000,"gold unchanged");break;}
            case "funds":{Player p=buyer(199);buy(p,2027);check(p.inventory.ruby==199&&count(p,2027)==0&&p.inventory.gem==100000,"gems cannot cover missing ruby");break;}
            case "chain":{for(int location=0;location<3;location++){for(int target:new int[]{711,12027}){Player p=buyer(10000);buy(p,target);check(count(p,target)==0&&p.inventory.ruby==10000,"missing predecessor rejects "+target);Item predecessor=costume(target==711?2027:711);(location==0?p.inventory.itemsBody:location==1?p.inventory.itemsBag:p.inventory.itemsBox).add(predecessor);buy(p,target);check(count(p,target)==1&&predecessor.quantity==1,"owned predecessor preserved in location "+location);check(p.inventory.ruby==10000-(target==711?1500:2000),"exact chain price");}}break;}
            case "expired":{for(int location=0;location<3;location++){Player p=buyer(10000);Item i=costume(2027);i.itemOptions.add(new Item.ItemOption(93,-1));(location==0?p.inventory.itemsBody:location==1?p.inventory.itemsBag:p.inventory.itemsBox).add(i);buy(p,711);check(count(p,711)==0&&p.inventory.ruby==10000,"expired predecessor cannot unlock");i.itemOptions.clear();i.quantity=0;buy(p,711);check(count(p,711)==0,"zero quantity cannot unlock");}break;}
            case "armor":{Player p=buyer(2500);buy(p,1869);check(p.inventory.ruby==0&&count(p,1869)==1,"armor independent and exact price");break;}
            case "full":{Player p=buyer(10000);p.inventory.itemsBag.clear();p.inventory.itemsBag.add(costume(933));buy(p,2027);check(p.inventory.ruby==10000&&count(p,2027)==0,"full bag no charge");break;}
            case "forged":{for(int scenario=0;scenario<5;scenario++){Player p=buyer(10000);ItemShop row=p.idMark.getShopOpen().getItemShop(2027);if(scenario==0)row.cost=199;if(scenario==1)row.typeSell=1;if(scenario==2)p.idMark.setTagNameShop("SANTA");if(scenario==3)p.idMark.getShopOpen().typeShop=0;buy(p,scenario==4?933:2027);check(p.inventory.ruby==10000&&count(p,2027)==0,"invalid shop row or context rejected "+scenario);}break;}
            case "rollback":{for(boolean throwsError:new boolean[]{false,true}){Player p=buyer(10000);replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}@Override public boolean addItemBag(Player p,Item i){super.addItemBag(p,i);if(throwsError)throw new IllegalStateException("grant failure");return false;}});buy(p,2027);check(p.inventory.ruby==10000&&count(p,2027)==0,"failed grant rolls back both bag and wallet");}break;}
            case "concurrent":{Player p=buyer(200);ExecutorService e=Executors.newFixedThreadPool(2);try{Future<?> a=e.submit(()->buy(p,2027)),b=e.submit(()->buy(p,2027));a.get();b.get();check(p.inventory.ruby==0&&count(p,2027)==1,"concurrent purchases charge and grant once");}finally{e.shutdownNow();}break;}
            case "notices":{Player p=buyer(0);buy(p,2027);check(lastNotice.contains("hồng ngọc"),"ruby notice names currency");p.inventory.ruby=10000;buy(p,711);check(lastNotice.contains("Quy lão Kame"),"first predecessor name");buy(p,12027);check(lastNotice.contains("Jacky Chun"),"second predecessor name");break;}
            case "pending":{Player p=buyer(200);java.lang.reflect.Field field=RubyGiftService.class.getDeclaredField("ACTIVE");field.setAccessible(true);@SuppressWarnings("unchecked") java.util.Map<Object,Object> active=(java.util.Map<Object,Object>)field.get(null);active.put(p,new Object());try{buy(p,2027);check(p.inventory.ruby==200&&count(p,2027)==0,"pending ruby transaction blocks shop");}finally{active.remove(p);}break;}
            case "menu":{String[] opened={null};replace(ShopService.class,"I",new ShopService(){@Override public void opendShop(Player p,String tag,boolean all){opened[0]=tag;}});for(int map:new int[]{5,13,20})for(boolean discount:new boolean[]{false,true}){MenuSanta santa=blank(MenuSanta.class);santa.mapId=map;Player p=buyer(10000);if(discount)p.inventory.itemsBag.add(costume(459));santa.openBaseMenu(p);check(santa.menu.length==(discount?8:7),"new menu appended");check(santa.menu[santa.menu.length-1].equals("Cửa hàng\nhồng ngọc"),"ruby menu label");santa.confirmMenu(p,santa.menu.length-1);check("SANTA_HONG_NGOC".equals(opened[0]),"ruby route at map "+map);santa.confirmMenu(p,discount?6:5);check("SANTA_DANH_HIEU".equals(opened[0]),"existing title route preserved");santa.confirmMenu(p,1);check((discount?"SANTA_GIAM_GIA_1":"SANTA_MO_RONG_HANH_TRANG").equals(opened[0]),"existing discount route preserved");}break;}
            default:throw new AssertionError(args[0]);
        }
        System.out.println("PASS SantaRubyShop "+args[0]);
    }
}
