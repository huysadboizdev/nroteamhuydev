package nro.models.services;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntUnaryOperator;
import nro.models.boss.Boss;
import nro.models.boss.Boss_mini.Odo;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.player.*;
import nro.models.player_system.Template;
import static nro.models.services.PinkFixture.*;

public final class HuntingTrioLootTest {
    static final Set<Integer> DIVINE = Set.of(555,556,557,558,559,560,561,562,563,564,565,566,567);
    static int missing = -1;
    static final Map<Integer,Map<Integer,Integer>> BASE = Map.ofEntries(
        Map.entry(555,Map.of(47,800)),Map.entry(557,Map.of(47,850)),Map.entry(559,Map.of(47,900)),
        Map.entry(556,Map.of(22,52,27,2600)),Map.entry(558,Map.of(22,50,27,2500)),Map.entry(560,Map.of(22,48,27,2400)),
        Map.entry(562,Map.of(0,4400)),Map.entry(564,Map.of(0,4300)),Map.entry(566,Map.of(0,4500)),
        Map.entry(563,Map.of(23,48,28,2400)),Map.entry(565,Map.of(23,50,28,2500)),Map.entry(567,Map.of(23,46,28,6900)),Map.entry(561,Map.of(14,14)));
    static int gender(int id) { if(id>=230&&id<=277)return ((id-230)%12)/4;
        return switch(id){case 555,556,562,563 -> 0;case 557,558,564,565 -> 1;case 559,560,566,567 -> 2;default -> 3;}; }
    static int type(int id) { if(id>=230&&id<=281)return (id-230)/12;
        return switch(id){case 555,557,559 -> 0;case 556,558,560 -> 1;case 562,564,566 -> 2;case 563,565,567 -> 3;case 561 -> 4;default -> 27;}; }
    static void setup() throws Exception {
        install(); missing=-1;
        replace(ItemService.class,"i",new ItemService(){
            @Override public Template.ItemTemplate getTemplate(int id){if(id==missing)return null;var t=template(id);t.type=(byte)type(id);t.gender=(byte)gender(id);return t;}
            @Override public Template.ItemOptionTemplate getItemOptionTemplate(int id){var t=new Template.ItemOptionTemplate();t.id=id;t.name="fixture #";return t;}
            @Override public List<Item.ItemOption> getListOptionItemShop(short id){return DIVINE.contains((int)id)?List.of():List.of(new Item.ItemOption(47,37));}
        });
    }
    static Object helper() throws Exception {
        try{return Class.forName("nro.models.boss.HuntingTrio.HuntingTrioLoot").getConstructor().newInstance();}
        catch(ClassNotFoundException absent){throw new AssertionError("hunting trio guaranteed equipment loot is missing",absent);}
    }
    static Boss boss() throws Exception {Boss b=blank(Odo.class);b.zone=zone();b.location=blank(Location.class);b.location.x=2;b.location.y=200;return b;}
    static IntUnaryOperator rolls(int... values) {return new IntUnaryOperator(){int index;public int applyAsInt(int bound){check(index<values.length,"unexpected random call "+bound);int r=values[index++];check(r>=0&&r<bound,"roll within bound "+bound);return r;}};}
    static boolean drop(Object h,Boss b,Player p,IntUnaryOperator r)throws Exception {
        Method m=h.getClass().getDeclaredMethod("drop",Boss.class,Player.class,IntUnaryOperator.class);m.setAccessible(true);
        try{return (boolean)m.invoke(h,b,p,r);}catch(InvocationTargetException e){if(e.getCause() instanceof RuntimeException re)throw re;throw e;}
    }
    // Random order: category, template, optional seal, gold, capsules, stone chance/id/qty, dragon chance/id.
    static IntUnaryOperator standard(int roll,int index,int seal){return roll<60||roll>=90?rolls(roll,index,seal,0,0,70,30):rolls(roll,index,0,0,70,30);}
    static ItemMap equipment(Boss b){List<ItemMap> e=b.zone.items.stream().filter(i->type(i.itemTemplate.id)<=4).toList();check(e.size()==1,"exactly one equipment per kill");return e.get(0);}
    static long seals(ItemMap i){return i.options.stream().filter(o->o.optionTemplate.id>=34&&o.optionTemplate.id<=36).count();}
    static void boundaries()throws Exception {
        setup();for(int g=0;g<3;g++)for(int roll:new int[]{0,59,60,89,90,99})for(int seal=0;seal<3;seal++){
            Boss b=boss();Player p=player(777);p.gender=(byte)g;
            check(drop(helper(),b,p,standard(roll,0,seal)),"valid kill rewarded");ItemMap i=equipment(b);
            check((i.itemTemplate.id<555)==(roll<60),"60/30/10 category boundaries");
            check(seals(i)==(roll>=60&&roll<90?0:1),"exactly one seal in sealed categories");
            int sealId=34+seal;if(seals(i)==1)check(i.options.stream().anyMatch(o->o.optionTemplate.id==sealId&&o.param==1),"seal convention");
            check(i.options.stream().noneMatch(o->o.optionTemplate.id>=127&&o.optionTemplate.id<=135),"no activation set");
            check(!i.options.isEmpty()&&i.options.stream().anyMatch(o->o.optionTemplate.id<34||o.optionTemplate.id>36),"base stats retained");
            check(b.zone.items.size()==3,"equipment and two guaranteed extras");
        }
    }
    static void pools()throws Exception {
        setup();Set<Integer> all=new HashSet<>();for(int g=0;g<3;g++)for(int index=0;index<20;index++){
            Boss b=boss();Player p=player(777);p.gender=(byte)g;drop(helper(),b,p,standard(0,index,0));ItemMap i=equipment(b);int id=i.itemTemplate.id;
            check(id>=230&&id<=281,"shop level9-12 only");check(gender(id)==g||gender(id)==3,"shop matches owner planet");all.add(id);
            check(i.options.stream().anyMatch(o->o.optionTemplate.id==47&&o.param==37),"shop base options unchanged");
        }check(all.size()==52,"all52confirmed shop templates reachable");
        Set<Integer> divine=new HashSet<>();for(int g=0;g<3;g++)for(int index=0;index<5;index++){
            Boss b=boss();Player p=player(777);p.gender=(byte)g;drop(helper(),b,p,standard(60,index,0));ItemMap i=equipment(b);int id=i.itemTemplate.id;
            check(DIVINE.contains(id)&&(gender(id)==g||id==561),"divine matches owner planet and common ring");divine.add(id);
            for(var expected:BASE.get(id).entrySet())check(i.options.stream().filter(o->o.optionTemplate.id==expected.getKey()&&o.param==expected.getValue()).count()==1,"canonical base option exact for divine"+id);
            check(i.options.size()==BASE.get(id).size()+1&&i.options.stream().anyMatch(o->o.optionTemplate.id==21&&o.param==15),"normal divine has canonical base plus power requirement");
        }check(divine.equals(DIVINE),"all13divine templates reachable");
    }
    static void extras()throws Exception {
        setup();Boss b=boss();Player p=player(777);drop(helper(),b,p,rolls(0,19,2,20000,2,69,4,1,29,6));
        check(b.zone.items.size()==5,"both optional extras appear on upper successful boundaries");
        for(ItemMap i:b.zone.items){check(i.playerId==777&&kept(i),"all reward ownership private through expiry");check(i.x>=0&&i.x<240&&i.y==100+i.x,"clamped terrain position");}
        check(b.zone.items.stream().anyMatch(i->i.itemTemplate.id==189&&i.quantity==30000),"gold upper inclusive30000");
        check(b.zone.items.stream().anyMatch(i->i.itemTemplate.id==722&&i.quantity==3),"capsule upper3");
        check(b.zone.items.stream().anyMatch(i->i.itemTemplate.id==224&&i.quantity==2),"stone70percent and1-2");
        check(b.zone.items.stream().anyMatch(i->i.itemTemplate.id==20&&i.quantity==1),"ordinary dragon30percent");
        Boss lower=boss();drop(helper(),lower,p,rolls(0,0,0,0,0,0,0,0,0,0));
        check(lower.zone.items.stream().anyMatch(i->i.itemTemplate.id==189&&i.quantity==10000),"gold lower10000");
        check(lower.zone.items.stream().anyMatch(i->i.itemTemplate.id==220&&i.quantity==1),"stone lower");
        check(lower.zone.items.stream().anyMatch(i->i.itemTemplate.id==14),"ordinary dragon lower");
    }
    static void guards()throws Exception {
        setup();Object h=helper();Boss b=boss();Player p=player(777);
        check(!drop(h,b,null,rolls()),"nullcredit rejected");var loc=b.location;b.location=null;check(!drop(h,b,p,rolls()),"missinglocation rejected");b.location=loc;
        missing=722;check(!drop(h,b,p,standard(0,0,0))&&b.zone.items.isEmpty(),"missingtemplate publishes nothing and preserves guard");missing=-1;
        p.gender=3;check(!drop(h,b,p,rolls()),"invalid owner gender rejected");p.gender=0;
        replace(Service.class,"instance",new Service(){@Override public void dropItemMap(nro.models.map.Zone z,ItemMap i){throw new IllegalStateException("packet failure");}});
        check(drop(h,b,p,standard(0,0,0)),"packetfailure after publication still grants");
        check(!drop(h,b,p,rolls())&&b.zone.items.size()==3,"repeated callback never duplicates");
        h.getClass().getMethod("reset").invoke(h);check(drop(h,b,p,standard(60,0,0))&&b.zone.items.size()==6,"new life resets guard");
        setup();Pet pet=blank(Pet.class);pet.isPet=true;pet.master=player(778);pet.master.gender=2;Boss pb=boss();check(drop(helper(),pb,pet,standard(60,0,0)),"pet credited");
        check(equipment(pb).playerId==778&&gender(equipment(pb).itemTemplate.id)==2,"master determines planet and owner");
    }
    static void pickup()throws Exception {
        setup();Boss b=boss();Player p=player(777);p.zone=b.zone;drop(helper(),b,p,standard(0,0,2));ItemMap e=equipment(b);
        Item item=ItemService.gI().createItemFromItemMap(e);check(item.itemOptions.size()==e.options.size(),"real pickup conversion retains options");
        check(item.itemOptions.stream().anyMatch(o->o.optionTemplate.id==36)&&item.itemOptions.stream().anyMatch(o->o.optionTemplate.id==47&&o.param==37),"pickup retains seal and base");
        replace(InventoryService.class,"I",new InventoryService(){@Override public boolean addItemBag(Player owner,Item i){return false;}});
        b.zone.pickItem(p,e.itemMapId);check(b.zone.items.contains(e)&&!e.isPickedUp,"fullbag preserves ground item");
    }
    static void concurrent()throws Exception {
        setup();Object h=helper();Boss b=boss();Player p=player(777);CountDownLatch go=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();
        Runnable action=()->{try{go.await();drop(h,b,p,standard(0,0,0));}catch(Throwable t){error.set(t);}};Thread a=new Thread(action),c=new Thread(action);a.start();c.start();go.countDown();a.join(3000);c.join(3000);
        check(!a.isAlive()&&!c.isAlive()&&error.get()==null&&b.zone.items.size()==3,"simultaneous kill callbacks reward once");
    }
    public static final class ImmediateObserverZone extends Zone {
        final List<Item> snapshots=new ArrayList<>();
        ImmediateObserverZone(nro.models.map.Map map){super(map,0,20);}
        @Override public synchronized void addItem(ItemMap item){
            if(type(item.itemTemplate.id)<=4)snapshots.add(ItemService.gI().createItemFromItemMap(item));
            super.addItem(item);
        }
    }
    static void publication()throws Exception {
        setup();Boss b=boss();ImmediateObserverZone z=new ImmediateObserverZone(b.zone.map);b.zone=z;
        drop(helper(),b,player(777),standard(0,0,2));
        check(z.snapshots.size()==1,"equipment observed during publication");Item snapshot=z.snapshots.get(0);
        check(snapshot.itemOptions.stream().anyMatch(o->o.optionTemplate.id==36),"seal must exist before addItem exposes equipment");
        check(snapshot.itemOptions.stream().anyMatch(o->o.optionTemplate.id==47&&o.param==37),"base stats must exist before publication");
        List<Item.ItemOption> input=new ArrayList<>();input.add(new Item.ItemOption(47,800));input.add(new Item.ItemOption(35,1));
        Constructor<ItemMap> constructor;
        try {constructor=ItemMap.class.getConstructor(Zone.class,int.class,int.class,int.class,int.class,long.class,boolean.class,List.class);}
        catch(NoSuchMethodException missing){throw new AssertionError("complete-options publication constructor missing",missing);}
        ItemMap i=constructor.newInstance(z,555,1,1,100,777L,true,input);input.get(0).param=1;input.clear();
        check(i.options.size()==2&&i.options.get(0).param==800,"constructor deepcopies incoming option list and params");
        i.options.get(0).param=2;check(z.snapshots.get(1).itemOptions.get(0).param==800,"publication snapshot independently owns options");
    }
    static void metadata()throws Exception {
        setup();ItemService delegate=ItemService.gI();replace(ItemService.class,"i",new ItemService(){
            @Override public Template.ItemTemplate getTemplate(int id){return delegate.getTemplate(id);}
            @Override public Template.ItemOptionTemplate getItemOptionTemplate(int id){return delegate.getItemOptionTemplate(id);}
            @Override public List<Item.ItemOption> getListOptionItemShop(short id){return List.of();}
            @Override public Item createNewItem(short id){Item item=super.createNewItem(id);item.itemOptions.add(new Item.ItemOption(21,15));return item;}
        });Boss b=boss();drop(helper(),b,player(777),standard(60,0,0));ItemMap i=equipment(b);
        check(i.options.stream().anyMatch(o->o.optionTemplate.id==47&&o.param==800),"metadata-only defaults must not suppress divine base stats");
        check(i.options.stream().anyMatch(o->o.optionTemplate.id==21&&o.param==15),"factory metadata preserved");
    }
    static void shopDivine()throws Exception {
        setup();ItemService delegate=ItemService.gI();replace(ItemService.class,"i",new ItemService(){
            @Override public Template.ItemTemplate getTemplate(int id){return delegate.getTemplate(id);}
            @Override public Template.ItemOptionTemplate getItemOptionTemplate(int id){return delegate.getItemOptionTemplate(id);}
            @Override public List<Item.ItemOption> getListOptionItemShop(short id){return switch(id){case 556->List.of(new Item.ItemOption(6,500),new Item.ItemOption(77,500),new Item.ItemOption(141,0));case 563->List.of(new Item.ItemOption(7,500),new Item.ItemOption(103,500),new Item.ItemOption(141,0));default->List.of();};}
        });for(int index:new int[]{1,3}) {Boss b=boss();drop(helper(),b,player(777),standard(60,index,0));ItemMap i=equipment(b);
            check(i.options.size()==3&&i.options.stream().anyMatch(o->o.optionTemplate.id==21&&o.param==15),"divine stats and requirement without custom shop stats");
            check(i.options.stream().noneMatch(o->o.optionTemplate.id==6||o.optionTemplate.id==7||o.optionTemplate.id==77||o.optionTemplate.id==103||o.optionTemplate.id==141),"custom shop stats excluded from ordinary divine drop");
            for(var expected:BASE.get((int)i.itemTemplate.id).entrySet())check(i.options.stream().anyMatch(o->o.optionTemplate.id==expected.getKey()&&o.param==expected.getValue()),"divine receives canonical base");
        }
    }
    public static void main(String[] args)throws Exception {try{switch(args[0]){case "boundaries"->boundaries();case "pools"->pools();case "extras"->extras();case "guards"->guards();case "pickup"->pickup();case "concurrent"->concurrent();case "publication"->publication();case "metadata"->metadata();case "shopDivine"->shopDivine();default->throw new AssertionError(args[0]);}System.out.println("PASS HuntingTrioLoot "+args[0]);System.exit(0);}catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
