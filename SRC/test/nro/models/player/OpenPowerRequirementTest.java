package nro.models.player;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import nro.models.consts.ConstNpc;
import nro.models.item.*;
import nro.models.intrinsic.Intrinsic;
import nro.models.network.Message;
import nro.models.npc_list.*;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.*;
import nro.models.services_func.Trade;

/** Offline real service and NPC routing tests. New APIs use reflection for old-JAR RED checks. */
public final class OpenPowerRequirementTest {
    private static final long FEE=500_000_000L;
    private static final long[] CAPS={17_999_999_999L,19_999_999_999L,24_999_999_999L,29_999_999_999L,
        39_999_999_999L,49_999_999_999L,59_999_999_999L,69_999_999_999L,79_999_999_999L,90_010_000_000L};
    public static void main(String[] args) throws Exception {
        setup(); String mode=args.length==0?"thresholds":args[0];
        switch(mode) {
            case "thresholds" -> thresholds(); case "equipment" -> equipment(); case "fees" -> fees();
            case "confirmation" -> confirmation(); case "replay" -> replay(); case "legacy" -> legacy();
            case "pet" -> pet(); case "menu" -> menu(); case "timer" -> timer(); case "growth" -> growth();
            case "master-gate" -> masterGate(); case "master-gear" -> masterGear();
            case "uniform-fees" -> uniformFees();
            default -> throw new AssertionError("Unknown mode "+mode);
        }
        System.out.println("PASS: Open power "+mode);
    }
    private static void setup() throws Exception {
        replace(Service.class,"instance",new Service(){
            @Override public void sendThongBao(Player p,String s) { }
            @Override public void sendMoney(Player p) { }
            @Override public void point(Player p) { }
            @Override public void showInfoPet(Player p) { }
        });
        replace(InventoryService.class,"I",new InventoryService(){
            @Override public void sendItemBody(Player p) { }
        });
        replace(ItemTimeService.class,"i",new ItemTimeService(){
            @Override public void sendAllItemTime(Player p) { }
        });
        for(int id=0;id<=255;id++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"Option #",0));
        for(int id=0;id<=1062;id++) {
            Template.ItemTemplate t=new Template.ItemTemplate();t.id=(short)id;t.type=12;t.name="Equipment "+id;
            Manager.ITEM_TEMPLATES.add(t);
        }
        for(int first:new int[]{555,650})for(int i=0;i<13;i++) {
            Template.ItemTemplate t=Manager.ITEM_TEMPLATES.get(first+i);
            t.type=(byte)(i==6?4:i<6?i%2:2+(i-7)%2);
        }
        for(int i=0;i<15;i++)Manager.ITEM_TEMPLATES.get(1048+i).type=(byte)(i/3);
    }
    private static void replace(Class<?> type,String field,Object value)throws Exception {
        Field f=type.getDeclaredField(field);f.setAccessible(true);f.set(null,value);
    }
    private static Player player(int level) {
        Player p=new Player(){@Override public void sendMessage(Message m) { }};
        initialize(p,level);return p;
    }
    private static void initialize(Player p,int level) {
        p.playerIntrinsic.intrinsic=new Intrinsic();p.nPoint.limitPower=(byte)level;p.nPoint.power=CAPS[level];
        p.playerTask.taskMain=new nro.models.task.TaskMain();p.playerTask.taskMain.id=-1;
        p.inventory.gold=2_000_000_000L;
        for(int i=0;i<12;i++)p.inventory.itemsBody.add(new Item());
    }
    private static Item item(int id) {
        Item i=new Item();i.template=Manager.ITEM_TEMPLATES.get(id);i.quantity=1;
        i.itemOptions.add(new Item.ItemOption(107,9));i.itemOptions.add(new Item.ItemOption(72,8));return i;
    }
    private static Player equipped(int level) {
        Player p=player(level);p.inventory.itemsBody.set(0,item(level<6?555:level<8?650:1048));return p;
    }
    private static Object call(String name,Class<?>[] types,Object... args)throws Exception {
        try {return OpenPowerService.class.getMethod(name,types).invoke(OpenPowerService.gI(),args);}
        catch(NoSuchMethodException e){throw new AssertionError("Missing required OpenPowerService API: "+name,e);}
        catch(InvocationTargetException e){
            if(e.getCause() instanceof RuntimeException r)throw r;
            if(e.getCause() instanceof Error r)throw r;
            throw e;
        }
    }
    private static List<?> prepare(Player owner,Player target)throws Exception {
        return (List<?>)call("prepare",new Class<?>[]{Player.class,Player.class},owner,target);
    }
    private static Object choose(Player owner,int index)throws Exception {
        return call("choose",new Class<?>[]{Player.class,int.class},owner,index);
    }
    private static void execute(Player owner)throws Exception {call("execute",new Class<?>[]{Player.class},owner);}
    private static void cancel(Player owner)throws Exception {call("cancel",new Class<?>[]{Player.class},owner);}
    private static Object property(Object selection,String name)throws Exception {return selection.getClass().getMethod(name).invoke(selection);}
    private static void select(Player owner,Player target,int index)throws Exception {prepare(owner,target);choose(owner,index);}
    @FunctionalInterface private interface Action {void run()throws Exception;}
    private static void rejected(Action action)throws Exception {
        try{action.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}
        throw new AssertionError("Invalid request must be rejected");
    }
    private static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
    private static void unchanged(Player owner,Player target,int limit,long gold,List<Item> body) {
        check(target.nPoint.limitPower==limit,"Rejected request cannot change limit");
        check(owner.inventory.gold==gold,"Rejected request cannot charge gold");
        check(target.inventory.itemsBody.equals(body),"Rejected request cannot change body");
    }
    private static void thresholds() {
        Player p = new Player();
        long[] caps = CAPS;
        for (int level=0;level<10;level++) {
            p.nPoint.limitPower=(byte)level;
            if(p.nPoint.getPowerLimit()!=caps[level])throw new AssertionError("Current cap level "+level+": expected "+caps[level]+", got "+p.nPoint.getPowerLimit());
            if(level<9 && p.nPoint.getPowerNextLimit()!=caps[level+1])throw new AssertionError("Next cap level "+level+": expected "+caps[level+1]+", got "+p.nPoint.getPowerNextLimit());
        }
        for(int level=4;level<9;level++)for(int offset:new int[]{-1,0,1}) {
            p.nPoint.limitPower=(byte)level;p.nPoint.power=CAPS[level]+offset;
            check(p.nPoint.canOpenPower()==(offset>=0),"Exact power threshold level "+level);
        }
    }
    private static void equipment()throws Exception {
        for(int level=4;level<9;level++) {
            Player p=equipped(level);List<?> options=prepare(p,p);
            check(options.size()==1,"Correct worn tier offered at "+level);
            Object s=options.get(0);
            check(property(s,"target")==p && property(s,"item")==p.inventory.itemsBody.get(0),"Selection identity");
            check(((Number)property(s,"slot")).intValue()==0 && ((Number)property(s,"limit")).intValue()==level,"Selection slot/limit");
            check(((Number)property(s,"nextPower")).longValue()==CAPS[level+1],"Selection next cap");
            select(p,p,0);execute(p);check(p.nPoint.limitPower==level+1,"All tiers open");
            check(!p.inventory.itemsBody.get(0).isNotNullItem(),"Exact item consumed");
            Player low=equipped(level);low.nPoint.power=CAPS[0]-1;List<Item> lowBefore=new ArrayList<>(low.inventory.itemsBody);
            rejected(()->select(low,low,0));rejected(()->execute(low));
            unchanged(low,low,level,2_000_000_000L,lowBefore);
            Player over=equipped(level);over.nPoint.power+=10_000_001L;long savedPower=over.nPoint.power;
            select(over,over,0);execute(over);check(over.nPoint.limitPower==level+1 && over.nPoint.power==savedPower,"Old over-cap power remains eligible and unchanged");
        }
        for(int level:new int[]{4,5,6,7,8}) {
            int first=level<6?555:level<8?650:1048,last=level<6?567:level<8?662:1062;
            for(int id=first;id<=last;id++) {
                Player p=player(level);Item i=item(id);int slot=i.template.type;p.inventory.itemsBody.set(slot,i);
                List<?> offered=prepare(p,p);check(offered.size()==1,"Every valid tier template is offered: "+id);
                check(((Number)property(offered.get(0),"slot")).intValue()==slot,"All five body slots accepted");
            }
        }
        for(int bad=0;bad<7;bad++) {
            Player p=equipped(4);Item original=p.inventory.itemsBody.get(0);p.inventory.itemsBody.set(0,new Item());
            switch(bad) {
                case 0 -> p.inventory.itemsBag.add(original);case 1 -> p.inventory.itemsBox.add(original);
                case 2 -> p.inventory.itemsBody.set(5,original);case 3 -> p.inventory.itemsBody.set(0,item(650));
                case 4 -> {original.quantity=2;p.inventory.itemsBody.set(0,original);}
                case 5 -> {original.quantity=0;p.inventory.itemsBody.set(0,original);}
                case 6 -> p.inventory.itemsBody.set(1,original);
            }
            final List<Item> before=new ArrayList<>(p.inventory.itemsBody);
            try{check(prepare(p,p).isEmpty(),"Invalid equipment cannot be offered: "+bad);}catch(IllegalArgumentException|IllegalStateException expected){ }
            rejected(()->execute(p));unchanged(p,p,4,2_000_000_000L,before);
        }
        Player max=equipped(9);rejected(()->prepare(max,max));rejected(()->execute(max));check(max.nPoint.limitPower==9,"Maximum unchanged");
    }
    private static void fees()throws Exception {
        Player poor=equipped(4);poor.inventory.gold=FEE-1;List<Item> before=new ArrayList<>(poor.inventory.itemsBody);
        rejected(()->select(poor,poor,0));rejected(()->execute(poor));unchanged(poor,poor,4,FEE-1,before);
        Player exact=equipped(4);exact.inventory.gold=FEE;select(exact,exact,0);execute(exact);
        check(exact.inventory.gold==0 && exact.nPoint.limitPower==5,"Exact fee succeeds");
        Player rich=equipped(4);rich.inventory.gold=5_000_000_000L;select(rich,rich,0);execute(rich);
        check(rich.inventory.gold==4_500_000_000L,"Gold arithmetic uses long");
    }
    private static void confirmation()throws Exception {
        Player p=equipped(4);List<Item> before=new ArrayList<>(p.inventory.itemsBody);
        rejected(()->execute(p));unchanged(p,p,4,2_000_000_000L,before);
        prepare(p,p);rejected(()->execute(p));unchanged(p,p,4,2_000_000_000L,before);
        rejected(()->choose(p,0));prepare(p,p);choose(p,0);cancel(p);rejected(()->execute(p));unchanged(p,p,4,2_000_000_000L,before);
        for(int index:new int[]{-1,10}) {prepare(p,p);rejected(()->choose(p,index));rejected(()->execute(p));unchanged(p,p,4,2_000_000_000L,before);}
        p.inventory.itemsBody.set(1,item(556));Item kept=p.inventory.itemsBody.get(0);
        List<?> choices=prepare(p,p);check(choices.size()==2,"Player gets all matching worn choices");
        Object selected=choose(p,1);check(property(selected,"item")==p.inventory.itemsBody.get(1),"User picks second item");
        String description=(String)property(selected,"description");
        check(description.contains("556") && description.contains("9") && description.contains("8"),"Description identifies whole decorated item");
        execute(p);check(p.inventory.itemsBody.get(0)==kept && !p.inventory.itemsBody.get(1).isNotNullItem(),"Only chosen item consumed");
        for(int change=0;change<11;change++) {
            Player stale=equipped(4);select(stale,stale,0);
            switch(change) {
                case 0 -> stale.inventory.itemsBody.set(0,item(555));
                case 1 -> stale.inventory.itemsBody.get(0).itemOptions.get(0).param++;
                case 2 -> stale.inventory.itemsBody.get(0).quantity++;
                case 3 -> stale.inventory.itemsBody.set(0,new Item());
                case 4 -> stale.inventory.gold=FEE-1;case 5 -> stale.nPoint.power=CAPS[0]-1;
                case 6 -> stale.nPoint.limitPower++;case 7 -> stale.tradeWVP=true;
                case 8 -> {Item i=stale.inventory.itemsBody.get(0);stale.inventory.itemsBody.set(0,new Item());stale.inventory.itemsBag.add(i);}
                case 9 -> stale.inventory.itemsBody.get(0).itemOptions.add(new Item.ItemOption(0,123));
                case 10 -> stale.inventory.itemsBody.get(0).template=Manager.ITEM_TEMPLATES.get(650);
            }
            List<Item> snapshot=new ArrayList<>(stale.inventory.itemsBody);long gold=stale.inventory.gold;int limit=stale.nPoint.limitPower;
            rejected(()->execute(stale));unchanged(stale,stale,limit,gold,snapshot);
        }
        Player trading=equipped(4);select(trading,trading,0);Trade trade=new Trade(trading,player(4));
        List<Item> snapshot=new ArrayList<>(trading.inventory.itemsBody);rejected(()->execute(trading));
        unchanged(trading,trading,4,2_000_000_000L,snapshot);trade.cancelTrade();
    }
    private static void replay()throws Exception {
        Player p=equipped(4);select(p,p,0);execute(p);rejected(()->execute(p));
        check(p.nPoint.limitPower==5 && p.inventory.gold==1_500_000_000L,"Duplicate confirmation charged once");
        Player concurrent=equipped(4);select(concurrent,concurrent,0);CountDownLatch start=new CountDownLatch(1);
        AtomicReference<Throwable> failure=new AtomicReference<>();
        AtomicInteger successes=new AtomicInteger(),rejections=new AtomicInteger();
        Runnable work=()->{try{start.await();try{execute(concurrent);successes.incrementAndGet();}catch(IllegalArgumentException|IllegalStateException expected){rejections.incrementAndGet();}}catch(Throwable t){failure.compareAndSet(null,t);}};
        Thread a=new Thread(work),b=new Thread(work);a.start();b.start();start.countDown();a.join();b.join();
        if(failure.get()!=null)throw new AssertionError("Concurrent confirmations",failure.get());
        check(successes.get()==1 && rejections.get()==1,"Exactly one concurrent confirmation succeeds");
        check(concurrent.nPoint.limitPower==5 && concurrent.inventory.gold==1_500_000_000L,"Concurrent confirmation charged once");
        Player old=equipped(4);select(old,old,0);prepare(old,old);rejected(()->execute(old));
        check(old.nPoint.limitPower==4 && old.inventory.gold==2_000_000_000L,"New preview invalidates old confirmation");
    }
    private static void legacy() {
        for(int level=4;level<9;level++) {
            Player p=equipped(level);
            check(!OpenPowerService.gI().openPowerBasic(p) && !p.itemTime.isOpenPower,"High level basic bypass blocked");
            check(!OpenPowerService.gI().openPowerSpeed(p) && p.nPoint.limitPower==level && p.inventory.gold==2_000_000_000L,"High level speed bypass blocked without charge");
        }
        for(int level=0;level<4;level++) {
            Player low=player(level);low.nPoint.power=CAPS[0]-1;
            check(!OpenPowerService.gI().openPowerSpeed(low) && low.nPoint.limitPower==level,"Speed requires master gate");
            check(!OpenPowerService.gI().openPowerBasic(low),"Basic requires master gate");
            Player full=player(level);full.nPoint.power=CAPS[0];check(!OpenPowerService.gI().openPowerBasic(full) && !full.itemTime.isOpenPower,"Low level free timer blocked");
            Player fast=player(level);fast.nPoint.power=CAPS[0];check(OpenPowerService.gI().openPowerSpeed(fast) && fast.nPoint.limitPower==level+1 && fast.inventory.gold==1_500_000_000L,"Low level service charges 500m centrally");
        }
    }
    private static void pet()throws Exception {
        for(int level=4;level<9;level++) {
            Player master=equipped(level);Pet companion=new Pet(master);initialize(companion,level);master.pet=companion;
            companion.inventory.itemsBody.set(0,item(level<6?555:level<8?650:1048));companion.inventory.gold=77;
            Item masterItem=master.inventory.itemsBody.get(0);select(master,companion,0);execute(master);
            check(companion.nPoint.limitPower==level+1 && master.nPoint.limitPower==level,"Pet alone upgraded level "+level);
            check(master.inventory.gold==1_500_000_000L && companion.inventory.gold==77,"Master pays pet fee");
            check(master.inventory.itemsBody.get(0)==masterItem && !companion.inventory.itemsBody.get(0).isNotNullItem(),"Pet wears sacrificed item");
        }
        Player owner=equipped(4);Pet pet=new Pet(owner);initialize(pet,4);owner.pet=pet;pet.inventory.itemsBody.set(0,item(555));
        Player stranger=equipped(4);rejected(()->prepare(stranger,pet));rejected(()->execute(stranger));
        check(stranger.nPoint.limitPower==4 && stranger.inventory.gold==2_000_000_000L,"Other master's pet rejected");
        Player stale=player(4);Pet previous=new Pet(stale);initialize(previous,4);previous.inventory.itemsBody.set(0,item(555));stale.pet=previous;
        select(stale,previous,0);stale.pet=new Pet(stale);rejected(()->execute(stale));
        check(previous.nPoint.limitPower==4 && stale.inventory.gold==2_000_000_000L,"Pet ownership revalidated");
    }
    private static void timer() {
        for(int level=0;level<10;level++) {
            Player p=player(level);p.itemTime.isOpenPower=true;
            p.itemTime.lastTimeOpenPower=System.currentTimeMillis()-ItemTime.TIME_OPEN_POWER-1000;
            p.itemTime.update();check(p.nPoint.limitPower==level,"Old timer level "+level+" never gives free upgrade");
            check(!p.itemTime.isOpenPower,"Old timer cleared level "+level);
        }
    }
    private static void growth() {
        Player master=player(4);Pet pet=new Pet(master);initialize(pet,5);master.pet=pet;
        pet.nPoint.power=CAPS[5]-1;Service.gI().addSMTN(pet,(byte)2,2,true);
        check(pet.nPoint.power==CAPS[5] && master.nPoint.power==CAPS[4],"Real pet addSMTN clamps pet without changing capped master");
        Service.gI().addSMTN(pet,(byte)2,2,true);check(pet.nPoint.power==CAPS[5],"Real pet addSMTN at cap remains unchanged");
        pet.nPoint.power=CAPS[5]+10_000_001L;Service.gI().addSMTN(pet,(byte)2,2,true);
        check(pet.nPoint.power==CAPS[5]+10_000_001L,"Real pet addSMTN preserves stored over-cap power");
        for(int level=0;level<10;level++)for(boolean isPet:new boolean[]{false,true}) {
            Player owner=player(level),target=isPet?new Pet(owner):owner;
            if(isPet){initialize(target,level);owner.pet=(Pet)target;}
            long cap=CAPS[level];target.nPoint.power=cap-1;target.nPoint.powerUp(2);
            check(target.nPoint.power==cap,"Positive gain clamps at cap level "+level+" pet="+isPet);
            target.nPoint.powerUp(1);check(target.nPoint.power==cap,"At cap gain unchanged");
            target.nPoint.power=cap+10_000_001L;target.nPoint.powerUp(1);
            check(target.nPoint.power==cap+10_000_001L,"Saved over-cap power stays unchanged");
            target.nPoint.powerUp(-1);check(target.nPoint.power==cap+10_000_000L,"Existing negative power adjustment preserved");
            target.nPoint.power=cap-1;target.nPoint.powerUp(Long.MAX_VALUE);
            check(target.nPoint.power==cap,"Huge positive gain cannot overflow cap");
        }
    }
    private static void uniformFees()throws Exception {
        for(int level=0;level<9;level++)for(boolean usePet:new boolean[]{false,true})for(long gold:new long[]{FEE-1,FEE,5_000_000_000L}) {
            Player owner=player(level);owner.nPoint.power=CAPS[0];owner.inventory.gold=gold;
            Player target=owner;
            if(usePet){Pet pet=new Pet(owner);initialize(pet,level);pet.nPoint.power=0;owner.pet=pet;target=pet;}
            if(level>=4)target.inventory.itemsBody.set(0,item(level<6?555:level<8?650:1048));
            List<Item> before=new ArrayList<>(target.inventory.itemsBody);
            if(level<4) {
                boolean result=OpenPowerService.gI().openPowerSpeed(target);
                check(result==(gold>=FEE),"Low tier requires exact 500m: level "+level+" pet="+usePet+" gold="+gold);
            } else if(gold<FEE) {
                Player selectedTarget=target;rejected(()->select(owner,selectedTarget,0));rejected(()->execute(owner));
            } else {select(owner,target,0);execute(owner);}
            if(gold<FEE)unchanged(owner,target,level,gold,before);
            else check(target.nPoint.limitPower==level+1 && owner.inventory.gold==gold-FEE,"One uniform fee charged tier "+level+" pet="+usePet);
        }
    }
    private static void masterGate()throws Exception {
        for(int level=4;level<9;level++) {
            Player owner=equipped(level);owner.nPoint.power=CAPS[0];select(owner,owner,0);execute(owner);
            check(owner.nPoint.limitPower==level+1 && owner.nPoint.power==CAPS[0],"Master fixed gate opens tier "+level);
            Player master=player(0);Pet pet=new Pet(master);initialize(pet,level);master.pet=pet;pet.nPoint.power=0;
            pet.inventory.itemsBody.set(0,item(level<6?555:level<8?650:1048));select(master,pet,0);execute(master);
            check(pet.nPoint.limitPower==level+1 && pet.nPoint.power==0 && master.nPoint.power==CAPS[0],"Pet power is irrelevant tier "+level);
            Player insufficient=equipped(level);insufficient.nPoint.power=CAPS[0]-1;
            rejected(()->prepare(insufficient,insufficient));check(insufficient.nPoint.limitPower==level,"Gate minus one fails");
        }
        Player stale=equipped(4);stale.nPoint.power=CAPS[0];select(stale,stale,0);stale.nPoint.power--;
        List<Item> body=new ArrayList<>(stale.inventory.itemsBody);rejected(()->execute(stale));unchanged(stale,stale,4,2_000_000_000L,body);
        for(boolean usePet:new boolean[]{false,true}) {
            Player owner=player(0);owner.nPoint.power=CAPS[0];owner.inventory.gold=5_000_000_000L;
            Player target=owner;if(usePet){Pet p=new Pet(owner);initialize(p,0);p.nPoint.power=0;owner.pet=p;target=p;}
            Item[] materials={item(555),item(556),item(650),item(651),item(1048)};
            King king=new King();
            for(int level=0;level<9;level++) {
                if(level>=4){Item material=materials[level-4];target.inventory.itemsBody.set(material.template.type,material);}
                king.openBaseMenu(owner);king.confirmMenu(owner,usePet?1:0);
                if(level>=4){king.confirmMenu(owner,0);king.confirmMenu(owner,0);}else{king.confirmMenu(owner,0);}
                check(target.nPoint.limitPower==level+1,"Sequential opening tier "+level+" pet="+usePet);
                check(owner.nPoint.power==CAPS[0] && target.nPoint.power==(usePet?0:CAPS[0]),"Sequential opening needs no additional power");
                if(level>=4)check(!materials[level-4].isNotNullItem() || !target.inventory.itemsBody.contains(materials[level-4]),"Exactly provisioned tier item consumed");
            }
            check(owner.inventory.gold==500_000_000L,"Full sequence costs nine 500m fees");
        }
    }
    private static void masterGear()throws Exception {
        for(int level=4;level<9;level++) {
            Player owner=player(0);Pet pet=new Pet(owner);initialize(pet,level);pet.nPoint.power=0;owner.pet=pet;
            Item source=item(level<6?555:level<8?650:1048);owner.inventory.itemsBody.set(0,source);
            List<?> choices=prepare(owner,pet);check(choices.size()==1,"Pet can use worn master gear tier "+level);
            Object choice=choose(owner,0);check(property(choice,"target")==pet && property(choice,"wearer")==owner,"Selection separates target and wearer");
            check(((String)property(choice,"description")).toLowerCase().contains("bản thân"),"Master gear label identifies wearer");
            execute(owner);check(pet.nPoint.limitPower==level+1 && owner.nPoint.limitPower==0,"Pet tier controls source material");
            check(!owner.inventory.itemsBody.get(0).isNotNullItem() && owner.inventory.gold==1_500_000_000L,"Master equipment and gold consumed");
        }
        Player owner=equipped(4);owner.nPoint.power=CAPS[0];Pet pet=new Pet(owner);initialize(pet,4);pet.nPoint.power=0;owner.pet=pet;
        Item own=item(555);pet.inventory.itemsBody.set(0,own);Item masterItem=owner.inventory.itemsBody.get(0);
        List<?> choices=prepare(owner,pet);check(choices.size()==2,"Both wearers offered");
        check(property(choices.get(0),"wearer")==pet && property(choices.get(1),"wearer")==owner,"Pet gear first, master gear second");
        check(((String)property(choices.get(0),"description")).toLowerCase().contains("đệ tử"),"Pet gear label identifies wearer");
        choose(owner,1);execute(owner);check(pet.inventory.itemsBody.get(0)==own && !owner.inventory.itemsBody.get(0).isNotNullItem(),"Explicit master choice preserves pet gear");
        prepare(owner,pet);choose(owner,0);execute(owner);check(pet.nPoint.limitPower==6 && !pet.inventory.itemsBody.get(0).isNotNullItem(),"Own gear remains available for next tier");
        for(int change=0;change<3;change++) {
            Player master=equipped(4);Pet companion=new Pet(master);initialize(companion,4);companion.nPoint.power=0;master.pet=companion;
            select(master,companion,0);
            if(change==0)master.inventory.itemsBody.set(0,new Item());
            else if(change==1)master.inventory.itemsBody.get(0).itemOptions.get(0).param++;
            else master.nPoint.power=CAPS[0]-1;
            List<Item> body=new ArrayList<>(master.inventory.itemsBody);rejected(()->execute(master));
            unchanged(master,master,4,2_000_000_000L,body);check(companion.nPoint.limitPower==4,"Stale source/gate cannot upgrade pet");
        }
    }
    private static final class King extends QuocVuong {
        String text;String[] choices;
        King(){super(0,0,0,0,0,0);}
        @Override public boolean canOpenNpc(Player p){p.idMark.setNpcChose(this);return true;}
        @Override public void createOtherMenu(Player p,int index,String text,String... choices){p.idMark.setIndexMenu(index);this.text=text;this.choices=choices;}
    }
    private static final class Kaio extends ToSuKaio {
        String[] choices;
        Kaio(){super(0,0,0,0,0,0);}
        @Override public boolean canOpenNpc(Player p){p.idMark.setNpcChose(this);return true;}
        @Override public void createOtherMenu(Player p,int index,String text,String... choices){p.idMark.setIndexMenu(index);this.choices=choices;}
    }
    private static void menu()throws Exception {
        for(int level=0;level<4;level++) {
            King king=new King();Player p=player(level);p.nPoint.power=CAPS[0];
            king.openBaseMenu(p);king.confirmMenu(p,0);
            check(king.choices.length==2 && king.choices[0].contains("500"),"Low master menu has paid 500m open and cancel");
            king.confirmMenu(p,1);check(p.nPoint.limitPower==level && p.inventory.gold==2_000_000_000L,"Low master cancel has no fee");
            king.openBaseMenu(p);king.confirmMenu(p,0);king.confirmMenu(p,0);
            check(p.nPoint.limitPower==level+1 && p.inventory.gold==1_500_000_000L && !p.itemTime.isOpenPower,"Low master NPC paid step charges only once");
        }
        for(int level=4;level<9;level++) {
            King king=new King();Player p=equipped(level);king.openBaseMenu(p);king.confirmMenu(p,0);
            check(p.idMark.getIndexMenu()!=ConstNpc.IGNORE_MENU,"King offers all five levels");
            king.confirmMenu(p,0);check(p.nPoint.limitPower==level,"Selection must show confirmation before consumption");
            check(king.text.contains("500") && king.text.toLowerCase().contains("vàng"),"Confirmation names gold fee");
            king.confirmMenu(p,0);check(p.nPoint.limitPower==level+1 && p.inventory.gold==1_500_000_000L,"King confirms selected upgrade");
        }
        for(int level=0;level<9;level++) {
            King king=new King();Player owner=equipped(4);Pet pet=new Pet(owner);initialize(pet,level);owner.pet=pet;
            if(level>=4)pet.inventory.itemsBody.set(0,item(level<6?555:level<8?650:1048));
            king.openBaseMenu(owner);king.confirmMenu(owner,1);
            check(owner.nPoint.limitPower==4,"Pet menu leaves master limit unchanged");
            if(level>=4) {
                king.confirmMenu(owner,0);check(pet.nPoint.limitPower==level,"Pet choice requires confirmation");
                check(king.text.contains("500") && king.text.toLowerCase().contains("vàng"),"Pet confirmation names gold");
                king.confirmMenu(owner,0);
            } else {
                check(owner.idMark.getIndexMenu()==ConstNpc.OPEN_POWER_PET,"Legacy pet menu");
                check(king.choices[0].contains("500") && king.choices[0].toLowerCase().contains("vàng"),"Legacy pet fee uses 500m gold");
                king.confirmMenu(owner,0);
            }
            check(pet.nPoint.limitPower==level+1 && owner.inventory.gold==2_000_000_000L-FEE,"Pet NPC correct level/fee at "+level);
        }
        Kaio kaio=new Kaio();Player p=equipped(5);kaio.openBaseMenu(p);
        for(String choice:kaio.choices)check(!choice.toLowerCase().contains("giới hạn"),"Kaio has no limit menu");
        for(int menu:new int[]{ConstNpc.MENU_NANG_GIOI_HAN,ConstNpc.OPEN_POWER_MYSEFT,ConstNpc.OPEN_POWER_PET}) {
            p.idMark.setIndexMenu(menu);kaio.confirmMenu(p,0);kaio.confirmMenu(p,1);
            check(p.nPoint.limitPower==5 && p.inventory.gold==2_000_000_000L && !p.itemTime.isOpenPower,"Kaio old menu replay blocked");
        }
    }
}
