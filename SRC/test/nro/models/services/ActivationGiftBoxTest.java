package nro.models.services;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.IntUnaryOperator;
import java.nio.file.Files;
import java.nio.file.Path;
import nro.models.item.*;
import nro.models.map.service.NpcService;
import nro.models.network.MySession;
import nro.models.player.*;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.shop.*;
import nro.models.shop_ky_gui.ConsignTransactions;
import static nro.models.services.PinkFixture.*;

/** Real reward builders; only network and persistence are isolated. */
public final class ActivationGiftBoxTest {
    private static final int[][] DIVINE={{555,556,562,563,561},{557,558,564,565,561},{559,560,566,567,561}};
    private static Class<?> feature;
    private static String[] buttons;

    public static void installData() throws Exception {
        install();
        Manager.ITEM_TEMPLATES.clear();Manager.ITEM_OPTION_TEMPLATES.clear();Manager.SHOPS.clear();
        for(int id=0;id<256;id++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"Option #",0));
        for(int id=0;id<=1538;id++){var t=template(id);t.isUpToUp=false;Manager.ITEM_TEMPLATES.add(t);}
        Shop shop=new Shop();shop.id=1;TabShop tab=new TabShop();shop.tabShops.add(tab);Manager.SHOPS.add(shop);
        int[][] bases={{47},{6,27},{0},{7,28},{14}};
        for(int planet=0;planet<3;planet++)for(int type=0;type<5;type++) {
            for(int id:SKHCapsuleCatalog.vipItems(planet,type)) {
                var t=Manager.ITEM_TEMPLATES.get(id);t.type=(byte)type;t.gender=(byte)(type==4?3:planet);
                ItemShop entry=new ItemShop();entry.temp=t;
                for(int opt:bases[type])entry.options.add(new Item.ItemOption(opt,100+id+opt));
                tab.itemShops.add(entry);
            }
            var t=Manager.ITEM_TEMPLATES.get(DIVINE[planet][type]);t.type=(byte)type;t.gender=(byte)(type==4?3:planet);
        }
        replace(ItemService.class,"i",new ItemService());
        replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}});
    }

    private static Item reward(int mode,int planet,int type,int level,int set)throws Exception {
        int[] rolls=mode==2?new int[]{planet,type,set}:new int[]{planet,type,level,set};int[] index={0};
        IntUnaryOperator random=bound->{int value=rolls[index[0]++];check(value>=0&&value<bound,"random bound");return value;};
        Method build=feature.getDeclaredMethod("createReward",int.class,IntUnaryOperator.class);build.setAccessible(true);
        try{return (Item)build.invoke(null,mode,random);}catch(InvocationTargetException e){throw (RuntimeException)e.getCause();}
    }
    public static void main(String[] args)throws Exception {
        try{feature=Class.forName("nro.models.services.ActivationGiftBoxService");}
        catch(ClassNotFoundException missing){throw new AssertionError("1538 activation gift-box feature is missing",missing);}
        installData();int checked=0;
        for(int mode=0;mode<3;mode++)for(int planet=0;planet<3;planet++)for(int type=0;type<5;type++) {
            int[] ids=mode==2?new int[]{DIVINE[planet][type]}:mode==0?
                    Arrays.stream(SKHCapsuleCatalog.legacyItems(planet,type)).distinct().toArray():SKHCapsuleCatalog.vipItems(planet,type);
            for(int level=0;level<ids.length;level++)for(int set=0;set<5;set++) {
                Item item=reward(mode,planet,type,level,set);int setId=SKHCapsuleCatalog.sets(planet)[set];
                check(item.quantity==1&&item.template.id==ids[level]&&item.template.type==type,"correct tier and slot");
                check(item.template.gender==(type==4?3:planet),"correct planet");
                check(value(item,107)==5&&value(item,102)<=0,"five empty crystal slots");
                check(value(item,30)==1&&item.itemOptions.stream().filter(o->o.optionTemplate.id==30).count()==1,"one trade lock");
                check(value(item,setId)==1,"activation name");
                for(int effect:ActivationSet.effectIds(setId))check(value(item,effect)==1,"all activation effects");
                check(item.itemOptions.stream().noneMatch(o->Set.of(72,93,86,87,34,35,36).contains(o.optionTemplate.id)),"no upgrade, time, consign or seal");
                check(!ConsignTransactions.eligible(item),"reward cannot be consigned");checked++;
            }
        }
        Item a=reward(0,0,0,0,0);a.itemOptions.get(0).param=-999;
        check(reward(0,0,0,0,0).itemOptions.get(0).param!=-999,"shop options are not shared");
        reject(()->reward(-1,0,0,0,0));reject(()->reward(3,0,0,0,0));
        Manager.SHOPS.clear();reject(()->reward(0,0,0,0,0));reject(()->reward(1,0,0,0,0));
        installData();menu();realSql();
        System.out.println("PASS ActivationGiftBox: "+checked+" reward combinations, empty stars, locks, stats isolation, missing data, menu cancellation/session/expiry");
        System.exit(0);
    }
    private static void realSql()throws Exception {
        Manager.SHOPS.clear();java.util.Map<Integer,Shop> shops=new HashMap<>();
        for(String line:Files.readAllLines(Path.of(".analysis/gold-economy-check/vip-sql-fixture.tsv"))) {
            String[] fields=line.split("\t");
            if(fields[0].equals("T")) {
                var t=Manager.ITEM_TEMPLATES.get(Integer.parseInt(fields[1]));
                t.type=Byte.parseByte(fields[2]);t.gender=Byte.parseByte(fields[3]);
            } else if(fields[0].equals("S")) {
                int sid=Integer.parseInt(fields[1]),tid=Integer.parseInt(fields[2]);
                Shop shop=shops.computeIfAbsent(sid,key->{Shop s=new Shop();s.id=key;Manager.SHOPS.add(s);return s;});
                TabShop tab=shop.tabShops.stream().filter(t->t.id==tid).findFirst().orElse(null);
                if(tab==null){tab=new TabShop();tab.id=tid;shop.tabShops.add(tab);}
                ItemShop entry=new ItemShop();entry.temp=Manager.ITEM_TEMPLATES.get(Integer.parseInt(fields[3]));tab.itemShops.add(entry);
                for(String option:fields[4].split(",")){String[] pair=option.split(":");entry.options.add(new Item.ItemOption(Integer.parseInt(pair[0]),Integer.parseInt(pair[1])));}
            }
        }
        for(int mode=0;mode<2;mode++)for(int planet=0;planet<3;planet++)for(int type=0;type<5;type++) {
            int size=mode==0?Arrays.stream(SKHCapsuleCatalog.legacyItems(planet,type)).distinct().toArray().length:12;
            for(int level=0;level<size;level++)for(int set=0;set<5;set++) {
                Item out=reward(mode,planet,type,level,set);check(value(out,107)==5&&value(out,30)==1,"real SQL shop shape");
            }
        }
    }
    private static void menu()throws Exception {
        replace(NpcService.class,"i",new NpcService(){@Override public void createMenuConMeo(Player p,int menu,int avatar,String text,String... options){p.idMark.setIndexMenu(menu);buttons=options;}});
        Player p=player(1538);p.idMark=new IDMark();p.session=blank(MySession.class);p.session.player=p;
        Item box=item(1538,2);p.inventory.itemsBag.add(box);p.inventory.itemsBag.add(new Item());
        feature.getMethod("show",Player.class,Item.class).invoke(null,p,box);
        check(buttons!=null&&buttons.length==4&&buttons[0].contains("Thường")&&buttons[1].contains("VIP")&&buttons[2].contains("Thần"),"three modes and close");
        feature.getMethod("choose",Player.class,int.class).invoke(null,p,3);
        feature.getMethod("choose",Player.class,int.class).invoke(null,p,0);
        check(box.quantity==2&&p.inventory.itemsBag.get(0)==box,"cancel then replay keeps box");
        feature.getMethod("show",Player.class,Item.class).invoke(null,p,box);p.session=blank(MySession.class);
        feature.getMethod("choose",Player.class,int.class).invoke(null,p,0);
        check(box.quantity==2,"new session cannot use old selection");
        feature.getMethod("show",Player.class,Item.class).invoke(null,p,box);
        Field field=feature.getDeclaredField("selections");field.setAccessible(true);java.util.Map<?,?> selections=(java.util.Map<?,?>)field.get(null);
        Object selected=selections.get(p);Constructor<?> ctor=selected.getClass().getDeclaredConstructors()[0];ctor.setAccessible(true);
        Object[] values=new Object[selected.getClass().getRecordComponents().length];int n=0;
        for(var part:selected.getClass().getRecordComponents()){Method getter=part.getAccessor();getter.setAccessible(true);values[n++]=getter.invoke(selected);}
        values[values.length-1]=System.currentTimeMillis()-121_000;
        @SuppressWarnings("unchecked") java.util.Map<Object,Object> mutable=(java.util.Map<Object,Object>)selections;mutable.put(p,ctor.newInstance(values));
        feature.getMethod("choose",Player.class,int.class).invoke(null,p,0);check(box.quantity==2,"expired menu keeps box");
    }
    public static int value(Item item,int id){return item.itemOptions.stream().filter(o->o.optionTemplate.id==id).mapToInt(o->o.param).findFirst().orElse(-1);}
    private interface Action{void run()throws Exception;}
    private static void reject(Action action)throws Exception{try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}throw new AssertionError("invalid or missing data accepted");}
}
