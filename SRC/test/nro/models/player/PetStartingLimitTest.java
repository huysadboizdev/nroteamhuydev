package nro.models.player;

import java.lang.reflect.*;
import java.util.*;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.server.Manager;
import nro.models.services.OpenPowerService;
import nro.models.task.TaskMain;

/** Initial/load floor never bypasses paid upgrades from tier four to nine. */
public final class PetStartingLimitTest {
    private static final long FEE=500_000_000L;
    public static void main(String[] args)throws Exception {
        Method setup=Class.forName("nro.models.player.OpenPowerRequirementTest").getDeclaredMethod("setup");setup.setAccessible(true);setup.invoke(null);
        switch(args[0]){case "restore" -> restore();case "unlock" -> unlock();case "growth" -> growth();default -> throw new AssertionError();}
        System.out.println("PASS: Pet starting limit "+args[0]);
    }
    private static void check(boolean value,String text){if(!value)throw new AssertionError(text);}
    private static void normalize(Pet pet)throws Exception {
        try{Pet.class.getMethod("restoreMinimumPowerLimit").invoke(pet);}
        catch(NoSuchMethodException e){throw new AssertionError("Missing restoreMinimumPowerLimit API",e);}
    }
    private static Player owner(int type,int tier,long power) {
        Player p=new Player(){@Override public void sendMessage(Message m){}};
        p.nPoint.power=17_999_999_999L;p.inventory.gold=3_000_000_000L;
        Pet pet=new Pet(p);p.pet=pet;pet.typePet=(byte)type;pet.nPoint.limitPower=(byte)tier;pet.nPoint.power=power;
        pet.nPoint.hpg=pet.nPoint.mpg=400_000;pet.nPoint.dameg=20_000;pet.nPoint.tiemNang=777;
        for(int i=0;i<9;i++){p.inventory.itemsBody.add(new Item());pet.inventory.itemsBody.add(new Item());}
        for(Player target:new Player[]{p,pet}){target.playerTask.taskMain=new TaskMain();target.playerTask.taskMain.id=-1;}
        return p;
    }
    private static void restore()throws Exception {
        for(int type=2;type<=4;type++)for(int tier=0;tier<=9;tier++) {
            Player p=owner(type,tier,40_000_000_000L);Pet pet=p.pet;
            for(int i=0;i<3;i++)normalize(pet);
            check(pet.nPoint.limitPower==Math.max(4,tier),"Load normalization preserves paid tiers5-9 and corrects only low tiers");
            check(pet.nPoint.power==40_000_000_000L && pet.nPoint.tiemNang==777 && p.inventory.gold==3_000_000_000L,"Normalization never charges or resets stats/power");
            check(pet.nPoint.hpg==400_000 && pet.nPoint.mpg==400_000 && pet.nPoint.dameg==20_000,"Stored base stats preserved");
        }
        for(int type:new int[]{0,1,5})for(int tier=0;tier<=9;tier++) {
            Pet pet=owner(type,tier,40_000_000_000L).pet;normalize(pet);check(pet.nPoint.limitPower==tier,"Other pet types untouched");
        }
        for(int type=2;type<=4;type++) {
            Pet pet=owner(type,0,39_999_999_999L).pet;normalize(pet);check(pet.nPoint.limitPower==0,"Below40b has no starter floor");
        }
    }
    @FunctionalInterface private interface Action{void run()throws Exception;}
    private static void reject(Action action)throws Exception {
        try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}
        throw new AssertionError("Invalid/free/replayed upgrade must fail");
    }
    private static Item equipment(int level) {
        Item item=new Item();item.template=Manager.ITEM_TEMPLATES.get(level<6?555:level<8?650:1048);item.quantity=1;return item;
    }
    private static void unlock()throws Exception {
        OpenPowerService service=OpenPowerService.gI();
        for(int type=2;type<=4;type++) {
            Player p=owner(type,0,40_000_000_000L);normalize(p.pet);
            reject(()->service.execute(p));reject(()->service.prepare(p,p.pet));
            check(p.pet.nPoint.limitPower==4 && p.inventory.gold==3_000_000_000L,"No equipment/confirmation means no upgrade");
            for(int level=4;level<9;level++) {
                Player wearer=level%2==0?p:p.pet;wearer.inventory.itemsBody.set(0,equipment(level));
                long gold=p.inventory.gold;p.inventory.gold=FEE-1;reject(()->service.prepare(p,p.pet));
                check(p.pet.nPoint.limitPower==level && wearer.inventory.itemsBody.get(0).isNotNullItem(),"Insufficient gold keeps tier and gear");
                p.inventory.gold=gold;service.prepare(p,p.pet);service.choose(p,0);normalize(p.pet);
                check(p.pet.nPoint.limitPower==level && p.inventory.gold==gold,"Choosing/normalizing never auto-opens next tier");
                service.execute(p);
                check(p.pet.nPoint.limitPower==level+1 && p.inventory.gold==gold-FEE,"Every next tier costs exactly500m");
                check(!wearer.inventory.itemsBody.get(0).isNotNullItem(),"Selected worn gear consumed once");
                reject(()->service.execute(p));normalize(p.pet);
                check(p.pet.nPoint.limitPower==level+1 && p.inventory.gold==gold-FEE,"Replay/load preserves paid progress without charging again");
                check(p.pet.nPoint.power==40_000_000_000L,"Does not require pet power to reach each next cap");
            }
            check(p.inventory.gold==500_000_000L && p.pet.nPoint.getPowerLimit()==90_010_000_000L,"Full five paid upgrades cost2.5b and retain maximum");
            reject(()->service.prepare(p,p.pet));
        }
    }
    private static void growth()throws Exception {
        for(int type=2;type<=4;type++) {
            Player p=owner(type,0,40_000_000_000L);normalize(p.pet);Pet pet=p.pet;
            pet.nPoint.tiemNang=10_000_000_000L;pet.nPoint.powerUp(Long.MAX_VALUE);
            for(byte stat=0;stat<3;stat++)check(!pet.nPoint.tryIncreasePetPoint(stat,1),"Tier4 starter stats cannot grow before paid unlock");
            Method auto=Pet.class.getDeclaredMethod("increasePoint");auto.setAccessible(true);auto.invoke(pet);
            check(pet.nPoint.limitPower==4 && pet.nPoint.power==40_000_000_000L,"Power gain and auto stats never unlock for free");
            p.fusion.typeFusion=6;normalize(pet);check(pet.nPoint.limitPower==4,"Fusion does not auto-open higher tiers");
            p.inventory.itemsBody.set(0,equipment(4));OpenPowerService service=OpenPowerService.gI();
            service.prepare(p,pet);service.choose(p,0);service.execute(p);
            check(pet.nPoint.tryIncreasePetPoint((byte)0,1),"After paid unlock baseHP can increase");
            pet.nPoint.powerUp(Long.MAX_VALUE);check(pet.nPoint.power==49_999_999_999L,"Paid tier5 allows growth only to tier5 cap");
        }
    }
}
