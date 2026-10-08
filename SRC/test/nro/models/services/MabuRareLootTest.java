package nro.models.services;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.IntUnaryOperator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import nro.models.boss.Boss;
import nro.models.map.*;
import nro.models.player.*;
import nro.models.npc.MabuEgg;
import static nro.models.services.PinkFixture.*;

public final class MabuRareLootTest {
    static Object helper() throws Exception {
        try { return Class.forName("nro.models.services.MabuRareLoot").getConstructor().newInstance(); }
        catch(ClassNotFoundException e) { throw new AssertionError("Mabu rare loot helper missing",e); }
    }
    static boolean drop(Object h,Boss b,Player p,int egg,int bottle,IntUnaryOperator r)throws Exception {
        Method m=h.getClass().getDeclaredMethod("drop",Boss.class,Player.class,int.class,int.class,IntUnaryOperator.class);
        m.setAccessible(true);return (boolean)m.invoke(h,b,p,egg,bottle,r);
    }
    static Boss boss()throws Exception{return HuntingTrioLootTest.boss();}
    static void boundaries()throws Exception {
        install();for(int egg:new int[]{5,30})for(int bottle:new int[]{3,15})for(int a=0;a<100;a++)for(int c:new int[]{0,bottle-1,bottle,99}) {
            Boss b=boss();Player p=player(7);p.inventory.itemsBox.add(item(568,1));
            check(drop(helper(),b,p,egg,bottle,HuntingTrioLootTest.rolls(a,c)),"valid callback handled");
            check(b.zone.items.stream().filter(i->i.itemTemplate.id==568).count()==(a<egg?1:0),"egg exact percent boundary");
            check(b.zone.items.stream().filter(i->i.itemTemplate.id==1795).count()==(c<bottle?1:0),"bottle independent exact boundary");
        }
    }
    static void eligibility()throws Exception {
        install();for(int mode=0;mode<7;mode++) {
            Boss b=boss();Player p=player(7);
            if(mode==1)p.inventory.itemsBag.add(item(568,1));
            if(mode==2)p.inventory.itemsBox.add(item(568,1));
            if(mode==3)p.mabuEgg=blank(MabuEgg.class);
            if(mode==4||mode==5){p.pet=blank(Pet.class);p.pet.typePet=(byte)(mode==4?1:2);}
            if(mode==6)p.inventory.itemsBag.add(item(568,0));
            Player killer=p;if(mode==2){Pet pet=blank(Pet.class);pet.master=p;pet.isPet=true;killer=pet;}
            check(drop(helper(),b,killer,5,3,HuntingTrioLootTest.rolls(0,0)),"handled");
            check(b.zone.items.stream().filter(i->i.itemTemplate.id==1795).count()==(mode>=1&&mode<=4?1:0),"prekill eligibility source "+mode);
            check(b.zone.items.stream().filter(i->i.itemTemplate.id==568).count()==1,"egg unconditional");
            for(ItemMap i:b.zone.items)check(i.playerId==7&&kept(i),"private to credited master");
            if(mode==1)check(p.inventory.itemsBag.get(0).quantity==1,"egg not consumed");
        }
        Boss b=boss();Player p=player(7);IntUnaryOperator mutating=new IntUnaryOperator(){int n;public int applyAsInt(int bound){if(n++==0)p.inventory.itemsBag.add(item(568,1));return 0;}};
        drop(helper(),b,p,30,15,mutating);check(b.zone.items.size()==1,"eligibility frozen before first random roll");
    }
    static class Observer extends Zone {
        Observer(nro.models.map.Map m){super(m,0,20);}
        @Override public synchronized void addItem(ItemMap i){if(i.itemTemplate.id==1795)check(i.options.size()==1&&i.options.get(0).optionTemplate.id==250&&i.options.get(0).param==0,"bottle charge zero before publication");super.addItem(i);}
    }
    static void once()throws Exception {
        install();Boss b=boss();b.zone=new Observer(b.zone.map);Player p=player(7);p.inventory.itemsBag.add(item(568,1));Object h=helper();
        check(!drop(h,b,null,5,3,HuntingTrioLootTest.rolls()),"null killer guarded");
        check(drop(h,b,p,5,3,HuntingTrioLootTest.rolls(0,0)),"first reward");
        check(!drop(h,b,p,5,3,HuntingTrioLootTest.rolls())&&b.zone.items.size()==2,"repeat guarded");
        h.getClass().getMethod("reset").invoke(h);drop(h,b,p,5,3,HuntingTrioLootTest.rolls(99,99));
        check(!drop(h,b,p,5,3,HuntingTrioLootTest.rolls()),"misses still consume callback");
    }
    static void concurrent()throws Exception {
        install();Boss b=boss();Player p=player(7);p.inventory.itemsBox.add(item(568,1));Object h=helper();
        CountDownLatch go=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();
        Runnable reward=()->{try{go.await();drop(h,b,p,30,15,HuntingTrioLootTest.rolls(0,0));}catch(Throwable t){error.set(t);}};
        Thread a=new Thread(reward),c=new Thread(reward);a.start();c.start();go.countDown();a.join(3000);c.join(3000);
        check(!a.isAlive()&&!c.isAlive()&&error.get()==null&&b.zone.items.size()==2,"concurrent callbacks publish each rare item once");
    }
    static void guards()throws Exception {
        install();Boss b=boss();Player p=player(7);p.inventory.itemsBox.add(item(568,1));Object h=helper();
        var saved=b.location;b.location=null;check(!drop(h,b,p,30,15,HuntingTrioLootTest.rolls()),"missing location consumes no rolls");b.location=saved;
        ItemService delegate=ItemService.gI();replace(ItemService.class,"i",new ItemService(){
            @Override public nro.models.player_system.Template.ItemTemplate getTemplate(int id){return delegate.getTemplate(id);}
            @Override public nro.models.player_system.Template.ItemOptionTemplate getItemOptionTemplate(int id){return id==250?null:delegate.getItemOptionTemplate(id);}
        });
        check(!drop(h,b,p,30,15,HuntingTrioLootTest.rolls(0,0))&&b.zone.items.isEmpty(),"invalid bottle option publishes neither rare item");
        replace(ItemService.class,"i",delegate);
        replace(Service.class,"instance",new Service(){@Override public void dropItemMap(Zone z,ItemMap i){throw new IllegalStateException("packet failure");}});
        check(drop(h,b,p,30,15,HuntingTrioLootTest.rolls(0,0)),"valid retry after validation failure");
        check(!drop(h,b,p,30,15,HuntingTrioLootTest.rolls())&&b.zone.items.size()==2,"packet failure never duplicates loot");
        for(ItemMap i:b.zone.items)check(i.x>=0&&i.x<240&&i.y==100+i.x,"safe clamped terrain");
    }
    public static void main(String[] args)throws Exception {try{switch(args[0]){case "boundaries"->boundaries();case "eligibility"->eligibility();case "once"->once();case "concurrent"->concurrent();case "guards"->guards();default->throw new AssertionError(args[0]);}System.out.println("PASS MabuRareLoot "+args[0]);System.exit(0);}catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
