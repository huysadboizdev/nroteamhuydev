package nro.models.combine;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntUnaryOperator;
import nro.models.consts.ConstNpc;
import nro.models.item.*;
import nro.models.network.Message;
import nro.models.npc_list.BaHatMit;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.*;
import nro.models.services_func.Trade;
import nro.models.shop.*;

/** Offline real inventory, fee, planet and NPC routing behavior. */
public final class NangSKHVipTest {
    private static final int[] MAINS = {650,653,657,662,656}, FODDERS = {555,558,564,567,561};
    private static final int[][] BASE = {{47},{6,27},{0},{7,28},{14}};
    private static TestNpc npc;
    public static void main(String[] args) throws Exception {
        setup(); String mode = args.length == 0 ? "validation" : args[0];
        switch (mode) {
            case "validation" -> validation(); case "random" -> random(); case "fees" -> fees();
            case "success" -> outcome(true); case "failure" -> outcome(false); case "mixed" -> mixed();
            case "inventory" -> inventory(); case "replay" -> replay(); case "menu" -> menu();
            case "normal-menu" -> normalMenu();
            default -> throw new AssertionError("Unknown mode " + mode);
        }
        System.out.println("PASS: SKH VIP " + mode);
    }
    private static void setup() throws Exception {
        npc = new TestNpc();
        replace(Service.class,"instance",new Service() {
            @Override public void sendMoney(Player p) { }
            @Override public void sendThongBao(Player p,String text) { }
        });
        replace(InventoryService.class,"I",new InventoryService() { @Override public void sendItemBags(Player p) { } });
        for (int id=0;id<=255;id++) Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"Option #",0));
        for (int id=0;id<=662;id++) { Template.ItemTemplate t=new Template.ItemTemplate(); t.id=(short)id; t.name="Equipment "+id; Manager.ITEM_TEMPLATES.add(t); }
        Shop shop=new Shop(); shop.id=1; TabShop tab=new TabShop(); shop.tabShops.add(tab); Manager.SHOPS.add(shop);
        for (int p=0;p<3;p++) for (int type=0;type<5;type++) {
            int[] ids=SKHCapsuleCatalog.vipItems(p,type);
            for (int i=0;i<ids.length;i++) { Template.ItemTemplate t=Manager.ITEM_TEMPLATES.get(ids[i]); t.type=(byte)type; t.gender=(byte)(type==4?3:p);
                ItemShop entry=new ItemShop(); entry.temp=t; for(int opt:BASE[type]) entry.options.add(new Item.ItemOption(opt,(i+1)*100+opt)); tab.itemShops.add(entry); }
        }
        for(int first:new int[]{555,650}) for(int i=0;i<13;i++) { Template.ItemTemplate t=Manager.ITEM_TEMPLATES.get(first+i);
            t.type=(byte)(i==6?4:i<6?i%2:2+(i-7)%2); t.gender=(byte)(i==6?3:i<6?i/2:(i-7)/2); }
    }
    private static Player player() { Player p=new Player() { @Override public void sendMessage(Message message) { } }; p.gender=0; p.inventory.gold=5_000_000_000L; p.combineNew.setTypeCombine(CombineService.NANG_SKH_VIP); return p; }
    private static Item item(int id) { Item item=new Item(); item.template=Manager.ITEM_TEMPLATES.get(id); item.quantity=1; item.itemOptions.add(new Item.ItemOption(107,9)); item.itemOptions.add(new Item.ItemOption(72,8)); return item; }
    private static Player batch(int n) { Player p=player(); for(int i=0;i<n;i++)p.inventory.itemsBag.add(item(MAINS[i])); for(int i=0;i<n;i++)p.inventory.itemsBag.add(item(FODDERS[i])); preview(p,slots(2*n)); return p; }
    private static void preview(Player p,int... slots) { p.combineNew.clearItemCombine(); p.combineNew.skhVipSelection=NangSKHVip.select(p,slots); for(int slot:slots)p.combineNew.itemsCombine.add(p.inventory.itemsBag.get(slot)); }
    private static int[] slots(int size) { return java.util.stream.IntStream.range(0,size).toArray(); }

    private static void validation() {
        for(int n=1;n<=5;n++)check(batch(n).combineNew.skhVipSelection.pairs().size()==n,"N pairs");
        Player p=player(); p.inventory.itemsBag.add(item(659)); p.inventory.itemsBag.add(item(555));
        NangSKHVip.Selection s=NangSKHVip.select(p,new int[]{0,1}); check(s.pairs().get(0).planet()==1 && s.pairs().get(0).type()==2,"Main controls planet/type, fodder arbitrary");
        for(int[] bad:new int[][]{{},{0},{0,1,0},{-1,1},{0,2},{0,0},{1,0}})reject(()->NangSKHVip.select(p,bad));
        for(int n:new int[]{11,12})reject(()->NangSKHVip.select(batch(5),slots(n)));
        Player duplicate=batch(2); duplicate.inventory.itemsBag.set(3,item(557)); reject(()->NangSKHVip.select(duplicate,slots(4)));
        Player sameType=batch(2); sameType.inventory.itemsBag.set(1,item(652)); check(NangSKHVip.select(sameType,slots(4)).pairs().size()==2,"Duplicate main types allowed");
        for(int id:new int[]{649,663,554,568}) { Item wrong=item(0); wrong.template=new Template.ItemTemplate(); wrong.template.id=(short)id; Player bad=batch(1); bad.inventory.itemsBag.set(id<600?1:0,wrong); reject(()->NangSKHVip.select(bad,new int[]{0,1})); }
        for(int q:new int[]{0,2}) { Player bad=batch(1); bad.inventory.itemsBag.get(0).quantity=q; reject(()->NangSKHVip.select(bad,new int[]{0,1})); }
        Player alias=batch(1); alias.inventory.itemsBag.add(alias.inventory.itemsBag.get(0)); reject(()->NangSKHVip.select(alias,new int[]{0,1}));
        Player empty=batch(1); empty.inventory.itemsBag.set(0,new Item()); reject(()->NangSKHVip.select(empty,new int[]{0,1}));
        for(int opt:new int[]{93,127,238}) { Player bad=batch(1); bad.inventory.itemsBag.get(1).itemOptions.add(new Item.ItemOption(opt,1)); reject(()->NangSKHVip.select(bad,new int[]{0,1})); }
        Player trading=batch(1); trading.tradeWVP=true; reject(()->NangSKHVip.select(trading,new int[]{0,1}));
        Player active=batch(1); Trade session=new Trade(active,player()); reject(()->NangSKHVip.select(active,new int[]{0,1})); session.cancelTrade();
        Player radar=player(); radar.inventory.itemsBag.add(item(656)); radar.inventory.itemsBag.add(item(555));
        for(int planet:new int[]{-1,3}) { radar.gender=(byte)planet; reject(()->NangSKHVip.select(radar,new int[]{0,1})); }
        int[] exposed=s.slots(); exposed[0]=99; check(s.slots()[0]==0,"Selection slots immutable");
    }
    private static void random() {
        for(int v:new int[]{0,49,50,99})check(NangSKHVip.rollSuccess(b->v)==(v<50),"50/50 boundary");
        for(int p=0;p<3;p++)for(int t=0;t<5;t++) { int[] ids=SKHCapsuleCatalog.vipItems(p,t);
            for(int group:new int[]{0,4,5,99})for(int i=0;i<(group<5?4:8);i++)check(NangSKHVip.rollTemplate(p,t,script(group,i))==ids[(group<5?8:0)+i],"5% high levels, all levels reachable");
            for(int i=0;i<5;i++)check(NangSKHVip.rollSet(p,script(i))==SKHCapsuleCatalog.sets(p)[i],"All five sets"); }
        for(int planet=0;planet<3;planet++)for(int set=0;set<5;set++) { Player p=player(); p.gender=(byte)planet; p.inventory.itemsBag.add(item(656)); p.inventory.itemsBag.add(item(557)); preview(p,0,1);
            check(execute(p,script(0,0,3,set))==1,"Radar succeeds"); Item radar=p.inventory.itemsBag.get(0); int primary=SKHCapsuleCatalog.sets(planet)[set];
            check(radar.template.id==281 && radar.template.type==4 && radar.template.gender==3,"Generic radar"); check(has(radar,primary),"Radar set follows player planet");
            for(int effect:ActivationSet.effectIds(primary))check(has(radar,effect),"Radar effects"); }
        for(int[] c:new int[][]{{0,659,1},{2,657,0}}) { Player p=player(); p.gender=(byte)c[0]; p.inventory.itemsBag.add(item(c[1])); p.inventory.itemsBag.add(item(555)); preview(p,0,1);
            execute(p,script(0,99,0,4)); Item out=p.inventory.itemsBag.get(0); check(out.template.type==2 && out.template.gender==c[2] && has(out,SKHCapsuleCatalog.sets(c[2])[4]),"Foreign main overrides character/fodder planet"); }
    }
    private static void fees() {
        for(int n=1;n<=5;n++) { check(NangSKHVip.totalFee(n)==n*1_000_000_000L,"long fee"); Player p=batch(n); p.inventory.gold=NangSKHVip.totalFee(n)-1;
            List<Item> before=new ArrayList<>(p.inventory.itemsBag); long gold=p.inventory.gold; reject(()->execute(p,b->99)); unchanged(p,before,gold);
            p.inventory.gold++; check(execute(p,b->99)==0 && p.inventory.gold==0,"Exact fee, failures charged"); }
        reject(()->NangSKHVip.totalFee(0)); reject(()->NangSKHVip.totalFee(6));
    }
    private static void outcome(boolean success) {
        for(int n=1;n<=5;n++) { Player p=batch(n); List<Item> before=new ArrayList<>(p.inventory.itemsBag);
            check(execute(p,b->success?0:99)==(success?n:0),"Result count"); check(p.inventory.gold==5_000_000_000L-n*1_000_000_000L,"All attempts charged once");
            for(int i=0;i<n;i++) { Item main=p.inventory.itemsBag.get(i); check(!p.inventory.itemsBag.get(i+n).isNotNullItem(),"Every fodder consumed");
                if(success)check(main!=before.get(i) && main.quantity==1 && main.isSKH() && !has(main,107) && !has(main,72),"Fresh success equipment");
                else check(main==before.get(i) && value(main,107)==9 && value(main,72)==8,"Exact failed main/options/stars"); }
            check(p.combineNew.skhVipSelection==null && p.combineNew.itemsCombine.isEmpty(),"Preview cleared"); }
    }
    private static void mixed() {
        Player p=batch(5); p.gender=2; preview(p,slots(10)); List<Item> before=new ArrayList<>(p.inventory.itemsBag);
        check(execute(p,script(49,5,0,0,50,0,4,3,4,99,10,99,7,2))==3,"Independent mixed batch"); check(p.inventory.gold==0,"5 billion");
        for(int i=0;i<5;i++)check(!p.inventory.itemsBag.get(i+5).isNotNullItem(),"Five fodders consumed");
        check(p.inventory.itemsBag.get(1)==before.get(1) && p.inventory.itemsBag.get(3)==before.get(3),"Failed mains unchanged");
        check(p.inventory.itemsBag.get(2).template.gender==0,"Earth glove stays Earth for Xayda player"); check(has(p.inventory.itemsBag.get(4),SKHCapsuleCatalog.sets(2)[2]),"Radar uses Xayda in mixed batch");
    }
    private static void inventory() {
        Player staged = batch(1);
        Item unrelated = item(0); staged.inventory.itemsBag.add(unrelated);
        List<Item> original = new ArrayList<>(staged.inventory.itemsBag);
        NangSKHVip.Prepared prepared = NangSKHVip.prepare(staged, b -> 0);
        unchanged(staged, original, 5_000_000_000L);
        check(prepared.bag().get(2) == unrelated, "Staged VIP preserves unrelated slots");
        Player full=batch(5); while(full.inventory.itemsBag.size()<80)full.inventory.itemsBag.add(item(0)); List<Item> before=new ArrayList<>(full.inventory.itemsBag); execute(full,b->0);
        for(int i=10;i<80;i++)check(full.inventory.itemsBag.get(i)==before.get(i),"Full bag untouched outside selection");
        for(int change=0;change<7;change++) { Player p=batch(5);
            switch(change) { case 0->p.inventory.itemsBag.set(0,item(650)); case 1->p.inventory.itemsBag.get(0).itemOptions.get(0).param++; case 2->p.inventory.itemsBag.get(5).quantity++;
                case 3->p.inventory.itemsBag=new ArrayList<>(p.inventory.itemsBag); case 4->p.combineNew.itemsCombine.set(0,p.inventory.itemsBag.get(1)); case 5->p.gender=1; case 6->p.inventory.itemsBag.get(5).quantityGD=1; }
            List<Item> snapshot=new ArrayList<>(p.inventory.itemsBag); long gold=p.inventory.gold; reject(()->execute(p,b->0)); unchanged(p,snapshot,gold); }
        // First output builds; missing stats on second must abort the entire batch.
        Player badData=batch(2); List<Item> snapshot=new ArrayList<>(badData.inventory.itemsBag);
        for(ItemShop e:Manager.SHOPS.get(0).tabShops.get(0).itemShops)if(e.temp.id==246)e.options.clear();
        reject(()->execute(badData,b->0)); unchanged(badData,snapshot,5_000_000_000L);
    }
    private static void replay() throws Exception {
        Player p=batch(1); execute(p,b->99); List<Item> snapshot=new ArrayList<>(p.inventory.itemsBag); reject(()->execute(p,b->0)); unchanged(p,snapshot,4_000_000_000L);
        p.inventory.itemsBag.set(1,item(555)); preview(p,0,1); reject(()->execute(p,b->0)); check(p.inventory.gold==4_000_000_000L,"Cooldown");
        Player concurrent=batch(5); AtomicInteger committed=new AtomicInteger(); AtomicReference<Throwable> error=new AtomicReference<>(); CountDownLatch start=new CountDownLatch(1);
        Runnable action=()-> { try { start.await(); execute(concurrent,b->99); committed.incrementAndGet(); } catch(IllegalArgumentException rejected) { } catch(Throwable t) { error.compareAndSet(null,t); } };
        Thread a=new Thread(action), b=new Thread(action); a.start(); b.start(); start.countDown(); a.join(5000); b.join(5000);
        check(!a.isAlive() && !b.isAlive() && error.get()==null && committed.get()==1 && concurrent.inventory.gold==0,"Concurrent confirm commits once");
        Player switched=batch(1); switched.combineNew.setTypeCombine(CombineService.PHA_LE_HOA_TRANG_BI); check(switched.combineNew.skhVipSelection==null,"Switch clears preview"); reject(()->execute(switched,x->0));
    }
    private static void menu() {
        Player p=batch(5); npc.openBaseMenu(p); check(npc.choices.length==6 && npc.choices[5].equals("Nâng SKH"),"Append base menu"); npc.confirmMenu(p,5);
        check(p.idMark.getIndexMenu()==ConstNpc.MENU_NANG_SKH && Arrays.equals(npc.choices,new String[]{"Nâng SKH Thường","Nâng SKH VIP","Đóng"}),"Both Normal and VIP must be available"); npc.confirmMenu(p,1); check(p.combineNew.typeCombine==CombineService.NANG_SKH_VIP && p.combineNew.skhVipSelection==null,"Open VIP route");
        CombineService.gI().showInfoCombine(p,slots(10)); check(p.combineNew.skhVipSelection.pairs().size()==5 && npc.choices.length==2,"10 item preview confirm/cancel");
        check(npc.text.contains("5%") && npc.text.contains("50%") && npc.text.contains("5"),"Preview percentages and fee"); List<Item> snapshot=new ArrayList<>(p.inventory.itemsBag); npc.confirmMenu(p,1); unchanged(p,snapshot,5_000_000_000L);
        check(p.combineNew.skhVipSelection==null,"Cancel cannot become x10");
        for(int[] bad:new int[][]{{-1,1},{0,80},{0,0}}) { CombineService.gI().showInfoCombine(p,bad); check(p.combineNew.skhVipSelection==null && p.idMark.getIndexMenu()==ConstNpc.IGNORE_MENU,"Packet bounds safe"); unchanged(p,snapshot,5_000_000_000L); }
        check(npc.choices.length != 3 || !Arrays.asList(npc.choices).contains("N?ng theo c?p"),
                "VIP must not expose an intermediate submenu");
        staleDeposit();
    }
    private static void staleDeposit() {
        Player stale=batch(1); List<Item> before=new ArrayList<>(stale.inventory.itemsBag);
        stale.combineNew.setTypeCombine(CombineService.NANG_SKH_VIP_TICH_LUY);
        stale.combineNew.itemsCombine.addAll(before);
        stale.idMark.setIndexMenu(ConstNpc.MENU_START_COMBINE); npc.confirmMenu(stale,0);
        unchanged(stale,before,5_000_000_000L);
        check(stale.skhVipDivineProgress==0 && stale.skhVipDestroyerProgress==0,
                "Old deposit callback cannot consume or accumulate");
        stale.idMark.setIndexMenu(ConstNpc.MENU_NANG_SKH_VIP); npc.confirmMenu(stale,1);
        unchanged(stale,before,5_000_000_000L);
        check(stale.skhVipDivineProgress==0 && stale.skhVipDestroyerProgress==0,
                "Old VIP submenu cannot deposit");
    }

    // Offline deterministic adapter: persistence/receipt publishing is tested separately.
    private static int execute(Player p, IntUnaryOperator random) {
        synchronized(p) { synchronized(p.inventory) {
            NangSKHVip.Prepared result=NangSKHVip.prepare(p,random);
            p.inventory.itemsBag=new ArrayList<>(result.bag()); p.inventory.gold=result.gold();
            NangSKHVipTichLuy.apply(p,result.progress());
            p.combineNew.clearItemCombine(); p.combineNew.clearParamCombine();
            long now=System.currentTimeMillis(); p.idMark.setLastTimeCombine(now);
            p.idMark.setIndexMenu(ConstNpc.IGNORE_MENU); p.combineNew.lastTimeCombine=now;
            return result.successes();
        } }
    }
    private static void normalMenu() {
        Player p = batch(5);
        List<Item> before = new ArrayList<>(p.inventory.itemsBag);
        npc.openBaseMenu(p); npc.confirmMenu(p,5); npc.confirmMenu(p,0);
        check(p.combineNew.typeCombine == CombineService.NANG_SKH_THUONG, "Normal button must open Normal, never VIP");
        int[] selected = {5,6,7,8,9};
        CombineService.gI().showInfoCombine(p,selected);
        check(p.combineNew.skhThuongSelection != null && p.combineNew.skhVipSelection == null,
                "Independent Normal preview");
        check(Arrays.equals(npc.choices,new String[]{"Nâng SKH Thường","Từ chối"}), "Normal confirm/cancel buttons");
        check(npc.text.contains("50% thành công, 50% xịt, độc lập") && npc.text.contains("0 đến 5") && npc.text.contains("2,5"),
                "Normal preview must explain independent rolls, zero-to-five results and 2.5 billion fee");
        npc.confirmMenu(p,1); unchanged(p,before,5_000_000_000L);
        check(p.combineNew.skhThuongSelection == null, "Normal cancel clears preview without charging");
        CombineService.gI().showInfoCombine(p,selected); npc.confirmMenu(p,0);
        check(p.inventory.gold == 2_500_000_000L && p.combineNew.skhThuongSelection == null, "Normal confirmation charges once");
        int outputs = 0;
        for (int i=0;i<5;i++) check(p.inventory.itemsBag.get(i) == before.get(i), "Normal must not touch unselected destruction equipment");
        for (int i=5;i<10;i++) {
            Item out = p.inventory.itemsBag.get(i);
            check(out != before.get(i), "Every selected divine input consumed");
            if (out.isNotNullItem()) { check(out.isSKH(), "Normal output is SKH"); outputs++; }
        }
        check(outputs >= 0 && outputs <= 5, "Five-item menu permits zero through five results");
        List<Item> after = new ArrayList<>(p.inventory.itemsBag);
        npc.openBaseMenu(p); npc.confirmMenu(p,5); npc.confirmMenu(p,2);
        unchanged(p,after,2_500_000_000L);
        check(p.idMark.getIndexMenu() == ConstNpc.IGNORE_MENU, "Submenu close is not an upgrade");
    }
    private static final class TestNpc extends BaHatMit {
        String text; String[] choices; TestNpc(){super(5,1,0,0,ConstNpc.BA_HAT_MIT,0);}
        @Override public boolean canOpenNpc(Player p){p.idMark.setNpcChose(this);return true;}
        @Override public void createOtherMenu(Player p,int index,String text,String... choices){p.idMark.setIndexMenu(index);this.text=text;this.choices=choices;}
        @Override public void npcChat(Player p,String text){ }
    }
    private static boolean has(Item item,int id){return item.itemOptions.stream().anyMatch(o->o.optionTemplate.id==id);}
    private static int value(Item item,int id){return item.itemOptions.stream().filter(o->o.optionTemplate.id==id).mapToInt(o->o.param).findFirst().orElse(-1);}
    private static IntUnaryOperator script(int... values){int[] i={0};return bound->{check(i[0]<values.length,"Unexpected roll");int v=values[i[0]++];check(v<bound,"Fixture bound");return v;};}
    private static void unchanged(Player p,List<Item> snapshot,long gold){check(p.inventory.gold==gold && p.inventory.itemsBag.size()==snapshot.size(),"Rejected preserves gold/bag");for(int i=0;i<snapshot.size();i++)check(p.inventory.itemsBag.get(i)==snapshot.get(i),"Rejected preserves slots");}
    private static void reject(Runnable action){try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}throw new AssertionError("Invalid accepted");}
    private static void check(boolean valid,String text){if(!valid)throw new AssertionError(text);}
    private static void replace(Class<?> type,String name,Object value)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
}
