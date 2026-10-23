package nro.models.combine;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import nro.models.item.Item;
import nro.models.item.SKHCapsuleCatalog;
import nro.models.player.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

/** Offline merged pair accumulation, migration and fixed-planet reward coverage. */
public final class SkhVipIntegratedTest {
    public static void main(String[] args) throws Exception {
        call("setup",new Class<?>[0]);
        failuresCount(); thresholdBatch(); successGetsBoth(); mixedGetsBoth(); cyclesAndUnequalLegacy();
        migrationAndPersistence(); pendingClaim(); radarAndForeignMain(); immutableProgress();
        System.out.println("PASS: merged SKH VIP progress, rewards and migration");
    }
    private static void failuresCount() throws Exception {
        Player p=batch(1); Item main=p.inventory.itemsBag.get(0), fodder=p.inventory.itemsBag.get(1);
        NangSKHVip.Prepared first=NangSKHVip.prepare(p,b->99);
        check(first.successes()==0 && first.pairs()==1,"Failure still processed one pair");
        progress(first.progress(),1,1,List.of());
        check(first.bag().get(0)==main && !first.bag().get(1).isNotNullItem(),"Failure keeps exact main and consumes divine");
        check(p.inventory.gold==5_000_000_000L && p.inventory.itemsBag.get(1)==fodder,"Prepare does not publish inventory");
        progress(NangSKHVipTichLuy.current(p),0,0,List.of());
        apply(p,first); p.inventory.itemsBag.set(1,item(555)); preview(p);
        NangSKHVip.Prepared second=NangSKHVip.prepare(p,b->99);
        progress(second.progress(),2,2,List.of());
        check(second.bag().get(0)==main && second.gold()==3_000_000_000L,"Reused failed main counts another paid pair");
    }
    private static void thresholdBatch() throws Exception {
        Player p=batch(5); p.gender=2; setProgress(p,48,48,List.of()); preview(p);
        check(p.combineNew.skhVipSelection.pairs().get(1).planet()==1,"Fixture second main is Namek");
        NangSKHVip.Prepared result=NangSKHVip.prepare(p,b->b==100?99:0);
        progress(result.progress(),3,3,List.of());
        check(result.pairs()==5 && result.successes()==0 && result.gold()==0,"Five failures advance five and pay five fees");
        check(result.bonusNames().size()==1,"48 plus five awards exactly once");
        List<Item> bonuses=divines(result.bag()); check(bonuses.size()==1,"One Divine bonus item");
        check(bonuses.get(0).template.id==557,"Threshold planet comes from second pair, not first/player/last");
        bonus(bonuses.get(0),1);
        progress(NangSKHVipTichLuy.current(p),48,48,List.of());
    }
    private static void successGetsBoth() throws Exception {
        Player p=batch(1); setProgress(p,49,49,List.of());
        NangSKHVip.Prepared result=NangSKHVip.prepare(p,b->0);
        check(result.successes()==1 && result.bag().get(0).isSKH(),"Normal VIP success remains present");
        check(result.bag().get(0).template.id<555,"VIP success remains VIP template");
        check(divines(result.bag()).size()==1 && result.bonusNames().size()==1,"Milestone is an additional item");
        bonus(divines(result.bag()).get(0),0); progress(result.progress(),0,0,List.of());
    }
    private static void mixedGetsBoth() throws Exception {
        Player p=batch(5); p.gender=2; setProgress(p,48,48,List.of()); preview(p);
        Item second=p.inventory.itemsBag.get(1), fourth=p.inventory.itemsBag.get(3);
        int[] rolls={49,5,0,0,50,0,4,3,4,99,10,99,7,2,0,0}; int[] cursor={0};
        NangSKHVip.Prepared result=NangSKHVip.prepare(p,b->{check(cursor[0]<rolls.length,"Unexpected mixed roll");return rolls[cursor[0]++];});
        check(result.successes()==3 && result.bonusNames().size()==1,"Mixed batch retains three normal successes plus bonus");
        check(result.bag().get(1)==second && result.bag().get(3)==fourth,"Mixed failures retain their mains");
        for(int slot:new int[]{0,2,4})check(result.bag().get(slot).isSKH(),"Mixed normal output remains present");
        bonus(divines(result.bag()).get(0),1); progress(result.progress(),3,3,List.of());
    }
    private static void cyclesAndUnequalLegacy() {
        NangSKHVipTichLuy.Progress state=new NangSKHVipTichLuy.Progress(0,0,List.of());
        for(int i=0;i<151;i++) state=NangSKHVipTichLuy.advance(state,i%3);
        progress(state,1,1,List.of(1,0,2));
        state=new NangSKHVipTichLuy.Progress(40,20,List.of());
        for(int i=0;i<30;i++)state=NangSKHVipTichLuy.advance(state,2);
        progress(state,20,0,List.of(2));
    }
    private static void migrationAndPersistence() throws Exception {
        Player old=batch(1); old.gender=1; old.skhVipDivineProgress=50; old.skhVipDestroyerProgress=50; old.skhVipPaidPairs=50;
        NangSKHVipTichLuy.Progress converted=NangSKHVipTichLuy.current(old);
        progress(converted,0,0,List.of(1));
        check(old.skhVipDivineProgress==50 && old.skhVipRewardPlanets.isEmpty(),"Legacy normalization is staged, not published");
        NangSKHVipTichLuy.apply(old,converted); old.gender=2;
        JSONArray encoded=(JSONArray)JSONValue.parse(NangSKHVipTichLuy.encodeVip(old,converted));
        check(encoded.size()>=12,"Pending reward planets appended to legacy VIP array");
        Player loaded=batch(1); loaded.gender=0; NangSKHVipTichLuy.load(loaded,encoded);
        progress(NangSKHVipTichLuy.current(loaded),0,0,List.of(1));
        NangSKHVipTichLuy.load(loaded,encoded); progress(NangSKHVipTichLuy.current(loaded),0,0,List.of(1));
        JSONArray unequal=(JSONArray)JSONValue.parse("[0,0,false,false,false,0,0,0,70,20,20]");
        NangSKHVipTichLuy.load(loaded,unequal); progress(NangSKHVipTichLuy.current(loaded),70,20,List.of());
        JSONArray ready=(JSONArray)JSONValue.parse("[0,0,false,false,false,0,0,0,50,50,50]");
        loaded.gender=2; NangSKHVipTichLuy.load(loaded,ready);
        progress(NangSKHVipTichLuy.current(loaded),0,0,List.of(2));
    }
    private static void pendingClaim() throws Exception {
        Player p=batch(1); setProgress(p,3,3,List.of(1,2));
        long gold=p.inventory.gold; List<Item> original=new ArrayList<>(p.inventory.itemsBag);
        var full=NangSKHVipTichLuy.prepareClaim(p,b->{throw new AssertionError("Full bag must not roll rewards");});
        progress(full.progress(),3,3,List.of(1,2)); check(full.bonusNames().isEmpty(),"Full bag keeps pending queue");
        check(p.inventory.itemsBag.equals(original) && p.inventory.gold==gold,"Full claim preserves RAM/fee");
        p.inventory.itemsBag.set(1,new Item()); p.gender=0;
        var claim=NangSKHVipTichLuy.prepareClaim(p,b->0);
        progress(claim.progress(),3,3,List.of(2));
        check(claim.bonusNames().size()==1 && claim.bag().get(1).template.id==557,"FIFO claim uses persisted Namek planet");
        bonus(claim.bag().get(1),1);
        check(!p.inventory.itemsBag.get(1).isNotNullItem() && p.skhVipRewardPlanets.equals(List.of(1,2)),"Claim only stages item and queue removal");
        check(p.inventory.gold==gold,"Pending claim has no extra fee");
    }
    private static void radarAndForeignMain() throws Exception {
        for(int[] c:new int[][]{{656,2,2},{659,2,1},{650,2,0}}) {
            Player p=batch(1); p.gender=(byte)c[1]; p.inventory.itemsBag.set(0,item(c[0])); setProgress(p,49,49,List.of()); preview(p);
            NangSKHVip.Prepared result=NangSKHVip.prepare(p,b->b==100?99:0);
            bonus(divines(result.bag()).get(0),c[2]);
        }
    }
    private static void immutableProgress() {
        List<Integer> queue=new ArrayList<>(List.of(1));
        var p=new NangSKHVipTichLuy.Progress(1,2,queue); queue.add(2);
        check(p.rewards().equals(List.of(1)),"Progress defensively copies queue");
        try {p.rewards().add(0);throw new AssertionError("Mutable reward queue");}catch(UnsupportedOperationException expected){}
    }
    private static void bonus(Item item,int planet) {
        check(item.quantity==1 && item.isSKH(),"Divine reward activated");
        check(item.itemOptions.stream().anyMatch(o->o.optionTemplate.id==30),"Divine reward trade locked");
        check(item.itemOptions.stream().noneMatch(o->o.optionTemplate.id==107),"Divine reward has no five crystal slots");
        check(java.util.Arrays.stream(SKHCapsuleCatalog.sets(planet)).anyMatch(set->item.itemOptions.stream().anyMatch(o->o.optionTemplate.id==set)),"Reward set matches fixed planet");
        check(item.template.type==4 || item.template.gender==planet,"Reward template matches fixed planet");
    }
    private static List<Item> divines(List<Item> bag) {return bag.stream().filter(i->i!=null && i.template!=null && i.template.id>=555 && i.template.id<=567 && i.isSKH()).toList();}
    private static void setProgress(Player p,int a,int b,List<Integer> queue){NangSKHVipTichLuy.apply(p,new NangSKHVipTichLuy.Progress(a,b,queue));}
    private static void progress(NangSKHVipTichLuy.Progress p,int a,int b,List<Integer> queue){check(p.divine()==a && p.destroyer()==b && p.rewards().equals(queue),"Expected progress "+a+"/"+b+" queue "+queue+", got "+p);}
    private static void apply(Player p,NangSKHVip.Prepared result){p.inventory.itemsBag=new ArrayList<>(result.bag());p.inventory.gold=result.gold();NangSKHVipTichLuy.apply(p,result.progress());p.combineNew.clearItemCombine();}
    private static Player batch(int n)throws Exception{return (Player)call("batch",new Class<?>[]{int.class},n);}
    private static Item item(int id)throws Exception{return (Item)call("item",new Class<?>[]{int.class},id);}
    private static void preview(Player p)throws Exception{int n=p.inventory.itemsBag.size();int[] slots=java.util.stream.IntStream.range(0,n).toArray();call("preview",new Class<?>[]{Player.class,int[].class},p,slots);}
    private static Object call(String name,Class<?>[] types,Object...args)throws Exception{Method m=NangSKHVipTest.class.getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(null,args);}
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
}
