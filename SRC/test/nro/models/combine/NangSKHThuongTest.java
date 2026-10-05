package nro.models.combine;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import nro.models.consts.ConstNpc;
import nro.models.item.*;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.shop.ItemShop;

/** Real inventory and catalog behavior, without a server or database. */
public final class NangSKHThuongTest {
    private static final int[] INPUTS={555,558,564,567,561};
    private static final int[][] SETS={{233,127,128,129,245},{233,132,237,130,131},{134,233,133,135,241}};
    public static void main(String[] args)throws Exception {
        try { Class.forName("nro.models.combine.NangSKHThuong"); }
        catch(ClassNotFoundException e){throw new AssertionError("Ordinary SKH implementation is missing",e);}
        Method setup=NangSKHVipTest.class.getDeclaredMethod("setup"); setup.setAccessible(true); setup.invoke(null);
        String mode=args.length==0?"selection":args[0];
        switch(mode){case "selection"->selection();case "random"->random();case "outcomes"->outcomes();case "fees"->fees();case "inventory"->inventory();case "replay"->replay();default->throw new AssertionError(mode);}
        System.out.println("PASS: SKH Thuong "+mode);
    }
    private static Player player()throws Exception {Method m=NangSKHVipTest.class.getDeclaredMethod("player");m.setAccessible(true);Player p=(Player)m.invoke(null);p.combineNew.setTypeCombine(CombineService.NANG_SKH_THUONG);return p;}
    private static Item item(int id){Item i=new Item();i.template=Manager.ITEM_TEMPLATES.get(id);i.quantity=1;i.itemOptions.add(new Item.ItemOption(107,9));i.itemOptions.add(new Item.ItemOption(72,8));return i;}
    private static Player batch(int n)throws Exception{Player p=player();for(int i=0;i<n;i++)p.inventory.itemsBag.add(item(INPUTS[i]));preview(p,slots(n));return p;}
    private static int[] slots(int n){return java.util.stream.IntStream.range(0,n).toArray();}
    private static void preview(Player p,int... slots){p.combineNew.clearItemCombine();p.combineNew.skhThuongSelection=NangSKHThuong.select(p,slots);for(int slot:slots)p.combineNew.itemsCombine.add(p.inventory.itemsBag.get(slot));}
    private static void selection()throws Exception{
        for(int n=1;n<=5;n++)check(batch(n).combineNew.skhThuongSelection.entries().size()==n,"1..5 accepted");
        Player p=batch(5);for(int[] bad:new int[][]{{},{0,1,2,3,4,5},{-1},{5},{0,0}})reject(()->NangSKHThuong.select(p,bad));
        for(int id:new int[]{554,568,650}){Player bad=batch(1);Item wrong=item(0);wrong.template=new Template.ItemTemplate();wrong.template.id=(short)id;bad.inventory.itemsBag.set(0,wrong);reject(()->NangSKHThuong.select(bad,new int[]{0}));}
        Player duplicate=batch(2);duplicate.inventory.itemsBag.set(1,item(557));reject(()->NangSKHThuong.select(duplicate,new int[]{0,1}));
        Player alias=batch(1);alias.inventory.itemsBag.add(alias.inventory.itemsBag.get(0));reject(()->NangSKHThuong.select(alias,new int[]{0}));
        for(int option:new int[]{93,127,238}){Player bad=batch(1);bad.inventory.itemsBag.get(0).itemOptions.add(new Item.ItemOption(option,1));reject(()->NangSKHThuong.select(bad,new int[]{0}));}
        for(int q:new int[]{0,2}){Player bad=batch(1);bad.inventory.itemsBag.get(0).quantity=q;reject(()->NangSKHThuong.select(bad,new int[]{0}));}
        Player trading=batch(1);trading.tradeWVP=true;reject(()->NangSKHThuong.select(trading,new int[]{0}));
        Player radar=player();radar.inventory.itemsBag.add(item(561));for(int g:new int[]{-1,3}){radar.gender=(byte)g;reject(()->NangSKHThuong.select(radar,new int[]{0}));}
        int[] exposed=p.combineNew.skhThuongSelection.slots();exposed[0]=99;check(p.combineNew.skhThuongSelection.slots()[0]==0,"Cloned slots");
    }
    private static void random()throws Exception{
        for(int p=0;p<3;p++)for(int v=0;v<100;v++){int index=v<30?0:v<60?1:v<90?2:v<95?3:4;final int roll=v;check(NangSKHThuong.rollSet(p,b->roll)==SETS[p][index],"Every weighted boundary");}
        check(Arrays.equals(NangSKHThuong.successfulIndexes(5,script(49,50,0,99,49)),
                new boolean[]{true,false,true,false,true}),"All five items must roll independently");
        for(int n=1;n<=5;n++)for(int v:new int[]{0,49,50,99}){final int roll=v;boolean[] picked=NangSKHThuong.successfulIndexes(n,b->roll);for(boolean success:picked)check(success==(v<50),"Independent 50% bounds");}
        for(int mask=0;mask<32;mask++) {
            int[] rolls=new int[5]; boolean[] expected=new boolean[5];
            for(int i=0;i<5;i++){expected[i]=(mask&(1<<i))!=0;rolls[i]=expected[i]?49:50;}
            check(Arrays.equals(NangSKHThuong.successfulIndexes(5,script(rolls)),expected),"Every five-item success/failure combination is reachable");
        }
        for(int planet=0;planet<3;planet++)for(int set=0;set<5;set++){Player p=player();p.gender=(byte)planet;p.inventory.itemsBag.add(item(561));preview(p,0);check(NangSKHThuong.execute(p,script(0,new int[]{0,30,60,90,95}[set]))==1,"Radar success");Item out=p.inventory.itemsBag.get(0);check(out.template.id==12 && out.template.gender==3 && has(out,SETS[planet][set]),"All fifteen radar choices");for(int effect:ActivationSet.effectIds(SETS[planet][set]))check(has(out,effect),"Set effects");}
        Player foreign=player();foreign.gender=2;foreign.inventory.itemsBag.add(item(564));preview(foreign,0);NangSKHThuong.execute(foreign,script(0,95));check(foreign.inventory.itemsBag.get(0).template.gender==1 && has(foreign.inventory.itemsBag.get(0),131),"Foreign item planet retained");
        reject(()->NangSKHThuong.rollSet(0,b->100));reject(()->NangSKHThuong.successfulIndexes(1,b->-1));
    }
    private static void outcomes()throws Exception{
        for(int n=1;n<=5;n++){Player p=batch(n);check(NangSKHThuong.execute(p,b->0)==n,"All success");for(int i=0;i<n;i++){Item out=p.inventory.itemsBag.get(i);int planet=i==4?0:Manager.ITEM_TEMPLATES.get(INPUTS[i]).gender;int type=Manager.ITEM_TEMPLATES.get(INPUTS[i]).type;check(out.template.id==SKHCapsuleCatalog.vipItems(planet,type)[0] && out.isSKH() && !has(out,107) && !has(out,72),"Fresh level-one output");for(ItemShop entry:Manager.SHOPS.get(0).tabShops.get(0).itemShops)if(entry.temp.id==out.template.id)for(Item.ItemOption base:entry.options)check(value(out,base.optionTemplate.id)==base.param,"Maximum exact base stats");}check(p.combineNew.skhThuongSelection==null && p.combineNew.itemsCombine.isEmpty() && p.idMark.getIndexMenu()==ConstNpc.IGNORE_MENU,"State cleared");}
        for(int n=1;n<=5;n++){Player p=batch(n);check(NangSKHThuong.execute(p,b->99)==0,"All fail");for(Item out:p.inventory.itemsBag)check(!out.isNotNullItem(),"Every failed input consumed");}
        Player mixed=batch(4);check(NangSKHThuong.execute(mixed,script(0,50,49,99,0,0))==2,"Independent mixed success");check(!mixed.inventory.itemsBag.get(1).isNotNullItem()&&!mixed.inventory.itemsBag.get(3).isNotNullItem(),"Mixed failures consumed");
        for(int successes=0;successes<=5;successes++) {
            Player five=batch(5);List<Item> originals=new ArrayList<>(five.inventory.itemsBag);
            int[] rolls=new int[5+successes];Arrays.fill(rolls,0,5,99);Arrays.fill(rolls,0,successes,49);
            check(NangSKHThuong.execute(five,script(rolls))==successes,"Five-item batch may yield zero through five outputs");
            check(five.inventory.gold==2_500_000_000L,"Charge all five attempts, including failures");
            for(int i=0;i<5;i++){Item out=five.inventory.itemsBag.get(i);check(out!=originals.get(i),"Every selected input consumed");check(i<successes?out.isSKH():!out.isNotNullItem(),"Selected success/failure matches independent roll");}
        }
    }
    private static void fees()throws Exception{
        for(int n=1;n<=5;n++){check(NangSKHThuong.totalFee(n)==500_000_000L*n,"Long fee up to 2.5b");Player p=batch(n);p.inventory.gold=NangSKHThuong.totalFee(n)-1;List<Item> before=new ArrayList<>(p.inventory.itemsBag);long gold=p.inventory.gold;reject(()->NangSKHThuong.execute(p,b->0));unchanged(p,before,gold);p.inventory.gold++;NangSKHThuong.execute(p,b->0);check(p.inventory.gold==0,"Exact fee charged");}
        Player fail=batch(4);fail.inventory.gold=2_000_000_000L;NangSKHThuong.execute(fail,b->99);check(fail.inventory.gold==0,"Failure fee not refunded");reject(()->NangSKHThuong.totalFee(0));reject(()->NangSKHThuong.totalFee(6));
    }
    private static void inventory()throws Exception{
        Player p=batch(1);Item pickup=item(0);p.inventory.itemsBag=new ArrayList<>(p.inventory.itemsBag){@Override public Item set(int i,Item item){if(i==0)super.set(1,pickup);return super.set(i,item);}};p.inventory.itemsBag.add(new Item());preview(p,0);NangSKHThuong.execute(p,b->0);check(p.inventory.itemsBag.get(1)==pickup,"Unselected pickup preserved");
        for(int change=0;change<7;change++){Player bad=batch(5);switch(change){case 0->bad.inventory.itemsBag.set(0,item(555));case 1->bad.inventory.itemsBag.get(0).itemOptions.get(0).param++;case 2->bad.inventory.itemsBag.get(0).quantityGD=1;case 3->bad.inventory.itemsBag=new ArrayList<>(bad.inventory.itemsBag);case 4->bad.combineNew.itemsCombine.set(0,bad.inventory.itemsBag.get(1));case 5->bad.gender=1;case 6->bad.inventory.itemsBag.get(0).expire=1;}List<Item> before=new ArrayList<>(bad.inventory.itemsBag);reject(()->NangSKHThuong.execute(bad,b->0));unchanged(bad,before,5_000_000_000L);}
        Player cancelled=batch(1);List<Item> before=new ArrayList<>(cancelled.inventory.itemsBag);NangSKHThuong.cancel(cancelled);check(cancelled.combineNew.skhThuongSelection==null,"Cancel clears");unchanged(cancelled,before,5_000_000_000L);
        Player missing=batch(2);before=new ArrayList<>(missing.inventory.itemsBag);for(ItemShop e:Manager.SHOPS.get(0).tabShops.get(0).itemShops)if(e.temp.id==7)e.options.clear();List<Item> snap=before;reject(()->NangSKHThuong.execute(missing,b->0));unchanged(missing,snap,5_000_000_000L);
    }
    private static void replay()throws Exception{
        Player p=batch(1);NangSKHThuong.execute(p,b->99);List<Item> before=new ArrayList<>(p.inventory.itemsBag);reject(()->NangSKHThuong.execute(p,b->0));unchanged(p,before,4_500_000_000L);p.inventory.itemsBag.set(0,item(555));preview(p,0);reject(()->NangSKHThuong.execute(p,b->0));check(p.inventory.gold==4_500_000_000L,"Cooldown protects");
        Player switched=batch(1);switched.combineNew.setTypeCombine(CombineService.NANG_SKH_VIP);check(switched.combineNew.skhThuongSelection==null,"Switch clears");reject(()->NangSKHThuong.execute(switched,b->0));
        Player concurrent=batch(5);AtomicInteger commits=new AtomicInteger();CountDownLatch start=new CountDownLatch(1);Runnable action=()->{try{start.await();NangSKHThuong.execute(concurrent,b->0);commits.incrementAndGet();}catch(IllegalArgumentException expected){}catch(InterruptedException e){throw new AssertionError(e);}};Thread a=new Thread(action),b=new Thread(action);a.start();b.start();start.countDown();a.join(5000);b.join(5000);check(!a.isAlive()&&!b.isAlive()&&commits.get()==1&&concurrent.inventory.gold==2_500_000_000L,"Concurrent confirmation commits once");
    }
    private static int count(boolean[] picked){int n=0;for(boolean b:picked)if(b)n++;return n;}
    private static boolean has(Item i,int id){return i.itemOptions.stream().anyMatch(o->o.optionTemplate.id==id);}
    private static int value(Item i,int id){return i.itemOptions.stream().filter(o->o.optionTemplate.id==id).mapToInt(o->o.param).findFirst().orElse(-1);}
    private static IntUnaryOperator script(int... values){int[] index={0};return bound->{check(index[0]<values.length,"Unexpected roll");int v=values[index[0]++];check(v>=0&&v<bound,"Fixture bound");return v;};}
    private static void unchanged(Player p,List<Item> before,long gold){check(p.inventory.gold==gold&&p.inventory.itemsBag.size()==before.size(),"Rejected preserves gold/bag");for(int i=0;i<before.size();i++)check(p.inventory.itemsBag.get(i)==before.get(i),"Rejected preserves slot");}
    private static void reject(Runnable action){try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}throw new AssertionError("Invalid accepted");}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
