package nro.models.combine;

import nro.models.player.Player;
import nro.models.item.Item;
import nro.models.services.*;
import static nro.models.services.PinkFixture.*;

public final class ThienSuCraftFlowTest {
    static final class LockedCleanupCombine extends Combine {
        final Player owner;
        int unlockedCleanup;
        LockedCleanupCombine(Player owner){this.owner=owner;}
        @Override public void clearParamCombine(){if(!Thread.holdsLock(owner))unlockedCleanup++;super.clearParamCombine();}
    }
    public static void main(String[] args) throws Exception {
        if(args.length>0&&args[0].equals("flow")){flow();return;}
        install();
        Player p=player(876543);
        p.combineNew=new Combine();p.combineNew.typeCombine=515;
        p.inventory.gold=1_000_000_000L;p.inventory.ruby=100;
        for(int id:new int[]{1066,1084,1074,1079})p.inventory.itemsBag.add(item(id,id==1066?999:1));
        p.inventory.itemsBag.add(new Item());
        long gold=p.inventory.gold;
        try {CombineService.gI().showInfoCombine(p,new int[]{0,1,2,3});} catch(Exception oldFlowFailure) { }
        check(p.inventory.gold==gold,"Preview must not charge gold");
        check(p.inventory.ruby==100,"Preview must not charge ruby");
        check(p.inventory.itemsBag.get(0).quantity==999,"Preview must not consume fragments");
        System.out.println("PASS preview is read only");
    }
    static void flow()throws Exception {
        ThienSuCraftFixture.installAll();
        Player untouched=ThienSuCraftFixture.ready(1066);
        Item existing=ThienSuCraftStatsTest.equipment(1049,4321);existing.id=123;existing.expire=987654321L;existing.quantityGD=0;
        existing.itemOptions.get(1).param=37;
        untouched.inventory.itemsBag.add(existing);
        untouched.inventory.itemsBag.get(0).quantity=1000;untouched.inventory.itemsBag.get(0).expire=123456789L;
        ThienSuCraftFixture.preview(untouched);ThienSuCraftFixture.execute(untouched,b->b==51?50:0);
        check(untouched.inventory.itemsBag.get(5)==existing&&existing.id==123&&existing.expire==987654321L&&existing.itemOptions.get(1).param==37,"Unrelated existing bag item identity/metadata/options untouched");
        check(untouched.inventory.itemsBag.get(0).quantity==1&&untouched.inventory.itemsBag.get(0).expire==123456789L,"Remaining ingredient stack expiry preserved");
        ThienSuCraftFixture.installAll();
        Player p=ThienSuCraftFixture.ready(1066);ThienSuCraftFixture.preview(p);
        check(ThienSuCraftFixture.factoryCalls==0&&p.inventory.ruby==100&&p.inventory.gold==12_345_678L,"Preview never creates output/rolls/charges");
        check(ThienSuCraftFixture.menus==1&&p.idMark.getIndexMenu()==515,"Confirmation menu515");
        check(ThienSuCraftFixture.menu.contains("50")&&ThienSuCraftFixture.menu.contains("1–30")&&ThienSuCraftFixture.menu.contains("1–7")&&ThienSuCraftFixture.menu.contains("thất bại"),"Menu discloses costs and loss");
        check(ThienSuCraftFixture.menu.contains("kết quả Thiên Sứ")&&ThienSuCraftFixture.menu.contains("Trái Đất"),"Preview names expected output and formula planet without creating item");
        int[] fragments={1066,1067,1070,1068,1069};
        for(int slot=0;slot<5;slot++)for(boolean success:new boolean[]{true,false}) {
            final boolean ok=success;p=ThienSuCraftFixture.ready(fragments[slot]);ThienSuCraftFixture.preview(p);
            var others=new java.util.ArrayList<>(p.inventory.itemsBody);
            check(ThienSuCraftFixture.execute(p,b->b==101?(ok?0:100):b==51?50:0)==success,"Craft outcome follows RNG");
            check(p.inventory.ruby==50&&p.inventory.gold==12_345_678L,"Only50ruby fee");
            for(int n=0;n<5;n++)check(n==slot?!p.inventory.itemsBody.get(n).isNotNullItem():p.inventory.itemsBody.get(n)==others.get(n),"Exact equipped slot consumed, other4identities preserved");
            check(p.inventory.itemsBag.stream().noneMatch(i->i.template!=null&&i.template.id>=1066&&i.template.id<=1086),"Exactly all4ingredients consumed; freedslot may holdoutput");
            long outputs=p.inventory.itemsBag.stream().filter(i->i.template!=null&&i.template.id>=1048&&i.template.id<=1062).count();
            check(outputs==(success?1:0),"Output only in bag");
            if(success){Item output=p.inventory.itemsBag.stream().filter(i->i.template!=null&&i.template.id>=1048&&i.template.id<=1062).findFirst().orElseThrow();
                check(output.itemOptions.stream().anyMatch(o->o.optionTemplate.id==21&&o.param==80),"New crafted item requires80b power");
                check(output.itemOptions.stream().anyMatch(o->o.optionTemplate.id==30&&o.param==0),"New crafted item remains trade locked");}
            try{ThienSuCraftFixture.execute(p,b->{throw new AssertionError("Duplicate rolled");});throw new AssertionError("Duplicate accepted");}catch(IllegalArgumentException expected){}
            try{ThienSuCraftRecipeTest.select(p);throw new AssertionError("Next craft without fullset accepted");}catch(IllegalArgumentException expected){}
        }
        for(int mode=0;mode<10;mode++) {
            p=ThienSuCraftFixture.ready(1066);ThienSuCraftFixture.preview(p);
            switch(mode) {
                case 0:p.inventory.itemsBody.set(0,item(650,1));p.inventory.itemsBody.get(0).template.type=0;break;
                case 1:p.inventory.itemsBag.get(0).quantity--;break;
                case 2:p.inventory.itemsBag.set(0,item(1066,999));break;
                case 3:p.inventory.ruby=49;break;
                case 4:p.inventory.itemsBag.set(4,item(6,1));break;
                case 5:p.tradeWVP=true;break;
                case 6:p.kolRewardPending=true;break;
                case 7:p.beforeDispose=true;break;
                case 8:ThienSuCraftFixture.failFactory=true;break;
                case 9:ThienSuCraftFixture.failGrant=true;break;
            }
            int ruby=p.inventory.ruby;var bag=p.inventory.itemsBag;var body=p.inventory.itemsBody;int quantity=bag.get(0).quantity;
            try{ThienSuCraftFixture.execute(p,b->b==51?50:0);throw new AssertionError("Invalid mode accepted:"+mode);}catch(IllegalArgumentException|IllegalStateException expected){}
            check(p.inventory.ruby==ruby&&p.inventory.itemsBag==bag&&p.inventory.itemsBody==body&&bag.get(0).quantity==quantity,"Rejected/technical craft is atomic:"+mode);
            ThienSuCraftFixture.failFactory=ThienSuCraftFixture.failGrant=false;
        }
        p=ThienSuCraftFixture.ready(1066);ThienSuCraftFixture.preview(p);final Player concurrent=p;
        java.util.concurrent.atomic.AtomicInteger completed=new java.util.concurrent.atomic.AtomicInteger();
        Runnable confirm=()->{try{ThienSuCraftFixture.execute(concurrent,b->b==51?50:0);completed.incrementAndGet();}catch(Exception rejected){}};
        Thread a=new Thread(confirm),b=new Thread(confirm);a.start();b.start();a.join();b.join();
        check(completed.get()==1&&p.inventory.ruby==50,"Concurrent confirmation committed once");
        p=ThienSuCraftFixture.ready(1066);ThienSuCraftFixture.preview(p);p.combineNew.clearItemCombine();
        try{ThienSuCraftFixture.execute(p,n->{throw new AssertionError("Cancel rolled");});throw new AssertionError("Cancel accepted");}catch(IllegalArgumentException expected){}
        check(p.inventory.ruby==100,"Cancellation does not charge");
        p=ThienSuCraftFixture.ready(1066);p.session=blank(nro.models.network.MySession.class);p.session.userId=654321;
        Player accountOwner=player(234568);accountOwner.session=blank(nro.models.network.MySession.class);accountOwner.session.userId=p.session.userId;
        Class<?> operation=Class.forName("nro.models.services.WalletPurchaseService$Operation");
        var ctor=operation.getDeclaredConstructors()[0];ctor.setAccessible(true);
        Object pending=ctor.newInstance(nro.models.services.WalletPurchaseService.gI(),accountOwner);
        var activeField=nro.models.services.WalletPurchaseService.class.getDeclaredField("ACTIVE");activeField.setAccessible(true);
        @SuppressWarnings("unchecked") java.util.Map<Player,Object> active=(java.util.Map<Player,Object>)activeField.get(null);
        active.put(accountOwner,pending);
        try{ThienSuCraftRecipeTest.rejected(p,"sameaccountpendingwallet");}finally{active.remove(accountOwner);}
        for(int upgrade=1074;upgrade<=1078;upgrade++)for(int draw:new int[]{90+upgrade-1073-1,90+upgrade-1073}) {
            p=ThienSuCraftFixture.ready(1066);p.inventory.itemsBag.get(2).template.id=(short)upgrade;ThienSuCraftFixture.preview(p);
            final int result=draw;check(ThienSuCraftFixture.execute(p,bound->bound==101?result:bound==51?50:0)==(draw<90+upgrade-1073),"Success strict boundary preserves0..100 distribution");
        }
        p=ThienSuCraftFixture.ready(1066);ThienSuCraftFixture.preview(p);
        int previousBodies=ThienSuCraftFixture.bodyPackets,previousBags=ThienSuCraftFixture.bagPackets,previousPoints=ThienSuCraftFixture.points,previousMoney=ThienSuCraftFixture.moneyPackets;
        p.setClothes=new nro.models.player.SetClothes(p);
        while(p.inventory.itemsBody.size()<13)p.inventory.itemsBody.add(new Item());
        p.setClothes.setDHD=5;
        CheTaoTrangBiThienSu.CheTaoTS(p);
        check(p.inventory.ruby==50&&p.setClothes.setDHD!=5,"Committed body updates equipment set");
        check(ThienSuCraftFixture.bodyPackets>previousBodies&&ThienSuCraftFixture.bagPackets>previousBags&&ThienSuCraftFixture.points>previousPoints&&ThienSuCraftFixture.moneyPackets>previousMoney,"Body/bag/points/HPKI/money refresh dispatched");
        long ruby=p.inventory.ruby;CheTaoTrangBiThienSu.CheTaoTS(p);check(p.inventory.ruby==ruby,"Notification wrapper cannot replay payment");
        p=ThienSuCraftFixture.ready(1066);
        LockedCleanupCombine lockedCombine=new LockedCleanupCombine(p);lockedCombine.typeCombine=515;p.combineNew=lockedCombine;
        ThienSuCraftFixture.preview(p);
        CombineService.gI().startCombine(p);
        check(p.inventory.ruby==50,"Real combine dispatcher completes craft");
        check(lockedCombine.unlockedCleanup==0,"Dispatcher must not perform trailing cleanup outside the crafting player monitor");
        System.out.println("PASS flow:preview/5slots/success/failure/stale/guards/outputfail/cancel/duplicate/concurrent");
    }
}
