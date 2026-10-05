package nro.models.player;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.*;
import nro.models.services_func.UseItem;
import nro.models.skill.*;

/** Replacement lifecycle and reset defaults through real PetService and item routing. */
public final class PetReplacementTest {
    private static final AtomicInteger removals=new AtomicInteger();
    private static volatile Player greetingOwner;
    private static final List<Player> greetings=Collections.synchronizedList(new ArrayList<>());
    public static void main(String[] args)throws Exception {
        setup();
        switch(args[0]) {
            case "reset-normal" -> normal();case "reset-mabu" -> mabu();case "reset-special" -> special();
            case "explicit-limit" -> explicit();case "item-route" -> itemRoute();
            default -> throw new AssertionError("Unknown mode");
        }
        System.out.println("PASS: Pet replacement "+args[0]);
    }
    private static void replace(Class<?> c,String field,Object value)throws Exception {Field f=c.getDeclaredField(field);f.setAccessible(true);f.set(null,value);}
    private static void setup()throws Exception {
        replace(Service.class,"instance",new Service(){
            @Override public void chatJustForMe(Player owner,Player pet,String text) {if(owner==greetingOwner)greetings.add(pet);}
            @Override public void sendThongBao(Player p,String text) { }
        });
        replace(InventoryService.class,"I",new InventoryService(){
            @Override public void sendItemBags(Player p){}
            @Override public void removeItemBag(Player p,Item item){removals.incrementAndGet();super.removeItemBag(p,item);}
        });
        for(int planet=0;planet<3;planet++) {
            NClass c=new NClass();Manager.NCLASS.add(c);
            Template.SkillTemplate template=new Template.SkillTemplate();template.id=(byte)(planet*2);template.name="Basic attack";
            Skill skill=new Skill();skill.template=template;skill.point=1;skill.skillId=(short)planet;template.skillss.add(skill);c.skillTemplatess.add(template);
        }
        for(int id=0;id<=250;id++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"Option #",0));
    }
    private static final class OldPet extends Pet {
        boolean disposed;
        OldPet(Player p){super(p);}
        @Override public void dispose(){disposed=true;super.dispose();}
    }
    private static Player owner() {
        Player p=new Player(){@Override public void sendMessage(Message m){}};p.gender=1;p.id=123;p.inventory.gold=777;
        OldPet pet=new OldPet(p);pet.gender=2;pet.typePet=1;pet.nPoint.limitPower=9;pet.nPoint.power=40_000_000_000L;
        pet.nPoint.hpg=555_555;pet.nPoint.mpg=444_444;pet.nPoint.dameg=25_999;pet.nPoint.tiemNang=888_888;
        p.pet=pet;return p;
    }
    private static Pet await(Player p,Pet old)throws Exception {
        long end=System.nanoTime()+2_000_000_000L;
        while((p.pet==null || p.pet==old) && System.nanoTime()<end)Thread.sleep(10);
        check(p.pet!=null && p.pet!=old,"Replacement creates a new pet within two seconds");return p.pet;
    }
    private static void check(boolean b,String text){if(!b)throw new AssertionError(text);}
    private static void defaults(Player p,Pet current,int type) {
        check(current.typePet==type,"Correct replacement type "+type+", got "+current.typePet);
        check(current.master==p && current.isPet,"Replacement belongs to original master");
        check(current.nPoint.limitPower==(type>=2?4:0),"Special pets start at tier four; normal and Mabu start at zero");
        check(current.nPoint.power==(type==0?2000L:type==1?1_500_000L:40_000_000_000L),"Per-type default power preserved");
        check(current.nPoint.tiemNang==0,"Previous pet TN is not inherited");
        if(type>=2)check(current.nPoint.hpg==400_000 && current.nPoint.mpg==400_000 && current.nPoint.dameg==20_000,"Special default stats");
        else {
            check(current.nPoint.hpg>=800 && current.nPoint.hpg<=2100 && current.nPoint.hpg%20==0,"Normal/Mabu HP default range");
            check(current.nPoint.mpg>=800 && current.nPoint.mpg<=2100 && current.nPoint.mpg%20==0,"Normal/Mabu KI default range");
            check(current.nPoint.dameg>=(type==0?20:50) && current.nPoint.dameg<=(type==0?45:120),"Normal/Mabu damage default range");
        }
        check(current.nPoint.defg>=9 && current.nPoint.defg<=50 && current.nPoint.critg>=0 && current.nPoint.critg<=2,"Default defense/crit ranges");
        check(current.inventory.itemsBody.size()==(type>=2?9:7) && current.inventory.itemsBody.stream().noneMatch(Item::isNotNullItem),"Fresh empty body slots");
        check(current.playerSkill.skills.size()==7 && current.playerSkill.skills.get(0)!=null,"Fresh attack plus six empty skills");
        check(current.nPoint.stamina==1000 && current.nPoint.maxStamina==1000,"Default stamina");
        check(current.nPoint.hp>0 && current.nPoint.mp>0 && current.nPoint.hp==current.nPoint.hpMax && current.nPoint.mp==current.nPoint.mpMax,"New pet starts alive with full computed HP/KI");
        check(p.inventory.gold==777,"Replacement does not change master gold");
    }
    private static void cleaned(OldPet old) {
        check(old.disposed && old.master==null && old.nPoint==null,"Old pet is disposed and detached");
    }
    private static void normal()throws Exception {
        Player p=owner();OldPet old=(OldPet)p.pet;PetService.gI().changeNormalPet(p,0);defaults(p,await(p,old),0);check(p.pet.gender==0,"Explicit normal gender");cleaned(old);
        p=owner();old=(OldPet)p.pet;PetService.gI().changeNormalPet(p);defaults(p,await(p,old),0);cleaned(old);
    }
    private static void mabu()throws Exception {
        Player p=owner();OldPet old=(OldPet)p.pet;PetService.gI().changeMabuPet(p,1);defaults(p,await(p,old),1);check(p.pet.gender==1,"Explicit Mabu gender");cleaned(old);
        p=owner();old=(OldPet)p.pet;PetService.gI().changeMabuPet(p);defaults(p,await(p,old),1);cleaned(old);
    }
    private static void special()throws Exception {
        Player boss=owner();boss.isBoss=true;boss.nPoint.hpMax=999_999;boss.nPoint.dame=12_345;
        PetService.gI().createNormalPet(boss);
        check(boss.nPoint.hpMax==999_999 && boss.nPoint.dame==12_345,"Pet factory must preserve boss combat stats");
        for(int type=2;type<=4;type++) {
            Player p=owner();OldPet old=(OldPet)p.pet;
            switch(type){case 2 -> PetService.gI().changeUubPet(p);case 3 -> PetService.gI().changeKidBeerPet(p);case 4 -> PetService.gI().changeJirenPet(p);}
            defaults(p,await(p,old),type);check(p.pet.gender==p.gender,"Special replacement uses master gender");cleaned(old);
        }
        Player quick=owner();greetingOwner=quick;greetings.clear();PetService.gI().changeUubPet(quick);Pet superseded=quick.pet;
        PetService.gI().changeJirenPet(quick);Pet latest=quick.pet;
        check(latest!=superseded && superseded.master==null,"Rapid replacement cleans previous newly created pet");
        Thread.sleep(1500);
        check(greetings.size()==1 && greetings.get(0)==latest,"Delayed greeting skips superseded captured pet and greets latest exactly once");
        greetingOwner=null;
    }
    private static void explicit()throws Exception {
        for(int type=0;type<=4;type++) {
            Player p=owner();Pet previous=p.pet;
            switch(type) {
                case 0 -> PetService.gI().createNormalPet(p,0,(byte)9);
                case 1 -> PetService.gI().createMabuPet(p,1,(byte)9);
                case 2 -> PetService.gI().createUubPet(p,(byte)9);
                case 3 -> PetService.gI().createKidBeerPet(p,(byte)9);
                case 4 -> PetService.gI().createJirenPet(p,(byte)9);
            }
            defaults(p,await(p,previous),type);cleaned((OldPet)previous);
        }
        Player normal=owner();Pet prior=normal.pet;PetService.gI().createNormalPet(normal,new byte[]{9});defaults(normal,await(normal,prior),0);
        Player mabu=owner();prior=mabu.pet;PetService.gI().createMabuPet(mabu,new byte[]{9});defaults(mabu,await(mabu,prior),1);
    }
    private static void itemRoute()throws Exception {
        Field rng=UseItem.class.getDeclaredField("rand");rng.setAccessible(true);Random random=(Random)rng.get(null);
        Set<Integer> tested=new HashSet<>();
        for(int seed=0;tested.size()<3 && seed<100;seed++) {
            int type=2+new Random(seed).nextInt(3);if(!tested.add(type))continue;
            Player p=owner();OldPet old=(OldPet)p.pet;Item item=capsule();p.inventory.itemsBag.add(item);random.setSeed(seed);
            route(p,item);
            defaults(p,await(p,old),type);cleaned(old);check(p.inventory.itemsBag.stream().noneMatch(i->i==item && i.isNotNullItem()),"Valid special replacement consumes item1795");
            Pet current=p.pet;int used=removals.get();route(p,item);
            check(p.pet==current && removals.get()==used,"Consumed item replay is a no-op without exception");
        }
        check(tested.size()==3,"All three item1795 random routes exercised");
        for(int invalid=0;invalid<8;invalid++) {
            Player p=owner();OldPet old=(OldPet)p.pet;Item item=capsule();p.inventory.itemsBag.add(item);
            switch(invalid) {
                case 0 -> item.template.id=1794;
                case 1 -> p.inventory.itemsBag.clear();
                case 2 -> item.dispose();
                case 3 -> item.itemOptions.get(0).param=2999;
                case 4 -> old.typePet=0;
                case 5 -> old.nPoint.power=39_999_999_999L;
                case 6 -> item.quantity=0;
                case 7 -> { }
            }
            List<Item> bag=new ArrayList<>(p.inventory.itemsBag);Template.ItemTemplate template=item.template;
            int quantity=item.quantity,used=removals.get();route(p,invalid==7?null:item);
            check(p.pet==old && !old.disposed,"Invalid capsule request keeps original pet case "+invalid);
            check(p.inventory.itemsBag.equals(bag) && item.template==template && item.quantity==quantity && removals.get()==used,"Invalid capsule request keeps item case "+invalid);
        }
        Player missing=owner();missing.pet=null;Item unused=capsule();missing.inventory.itemsBag.add(unused);
        int beforeMissing=removals.get();route(missing,unused);
        check(missing.pet==null && missing.inventory.itemsBag.contains(unused) && unused.isNotNullItem()
                && removals.get()==beforeMissing,"Missing pet keeps capsule and does not create a replacement");
        Player p=owner();OldPet old=(OldPet)p.pet;Item item=capsule();p.inventory.itemsBag.add(item);
        CountDownLatch start=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();AtomicReference<Pet> published=new AtomicReference<>();int used=removals.get();
        Runnable request=()->{try{start.await();route(p,item);Pet replacement=p.pet;
            Pet first=published.get();if(first==null){published.compareAndSet(null,replacement);first=published.get();}
            check(first==replacement,"Concurrent duplicate routes retain the same published pet");
        }catch(Throwable t){error.compareAndSet(null,t);}};
        Thread a=new Thread(request),b=new Thread(request);a.start();b.start();start.countDown();a.join();b.join();
        if(error.get()!=null)throw new AssertionError("Concurrent capsule replacement",error.get());
        check(p.pet!=old && p.pet==published.get() && removals.get()==used+1,"Concurrent duplicate capsule requests replace and consume exactly once");
        cleaned(old);check(!item.isNotNullItem(),"Concurrent capsule consumed");
    }
    private static Item capsule() {
        Item item=new Item();item.template=new Template.ItemTemplate();item.template.id=1795;item.template.type=12;item.template.name="Special pet capsule";item.quantity=1;
        item.itemOptions.add(new Item.ItemOption(250,3000));return item;
    }
    private static void route(Player p,Item item)throws Exception {
        Method route=UseItem.class.getDeclaredMethod("changePetRamdom",Player.class,Item.class);route.setAccessible(true);
        try{route.invoke(UseItem.gI(),p,item);}catch(InvocationTargetException e){throw new AssertionError("Item route must complete without exception",e.getCause());}
    }
}
