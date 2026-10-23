package nro.models.shop_ky_gui;

import java.lang.reflect.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.*;
import static nro.models.services.PinkFixture.*;

public final class ActivationBoxTransactionTest {
    private static ConsignTransactions.Result open(ConsignTransactions tx,Player p,Item box,int mode)throws Exception {
        try{return (ConsignTransactions.Result)ConsignTransactions.class.getMethod("redeemActivationBox",Player.class,Item.class,int.class).invoke(tx,p,box,mode);}
        catch(NoSuchMethodException missing){throw new AssertionError("1538 redemption is missing",missing);}
        catch(InvocationTargetException e){throw new RuntimeException(e.getCause());}
    }
    private static Player owner(int id)throws Exception{return DivineBoxTransactionTest.owner(id);}
    private static Item box(Player p,int count){Item box=item(1538,count);p.inventory.itemsBag.set(0,box);return box;}
    public static void main(String[] args)throws Exception {
        ActivationGiftBoxTest.installData();
        replace(ConsignShopManager.class,"instance",new ConsignShopManager(){@Override public void save(){}});
        var dao=new ConsignRegressionTest.MemoryDAO();var tx=new ConsignTransactions(dao,p->true);
        for(int mode=0;mode<3;mode++) {
            Player p=owner(15380+mode);Item box=box(p,2);check(open(tx,p,box,mode).success(),"open mode "+mode);
            check(p.inventory.itemsBag.get(0).quantity==1&&p.inventory.itemsBag.get(1).isSKH(),"one box for one item");
            check(ActivationGiftBoxTest.value(p.inventory.itemsBag.get(1),107)==5,"five empty slots persisted");
            check(p.inventory.gold==1000&&p.inventory.gem==100&&p.inventory.ruby==100_000,"no fee");
            int writes=dao.writes;check(!open(tx,p,box,mode).success()&&dao.writes==writes,"old box reference cannot grant twice");
        }
        Player p=owner(15390);Item box=box(p,2);for(int n=1;n<6;n++)p.inventory.itemsBag.set(n,item(200,1));
        check(!open(tx,p,box,0).success()&&box.quantity==2,"full bag unchanged");
        p=owner(15391);box=box(p,1);dao.fail=true;
        check(!open(tx,p,box,2).success()&&p.inventory.itemsBag.get(0)==box&&box.quantity==1,"SQL rollback preserves box");dao.fail=false;
        p=owner(15392);box=box(p,1);p.tradeWVP=true;
        check(!open(tx,p,box,0).success()&&box.quantity==1,"active trade rejected");p.tradeWVP=false;
        check(!open(tx,p,box,3).success()&&box.quantity==1,"bad mode rejected");
        box.quantityGD=1;check(!open(tx,p,box,0).success(),"reserved box rejected");box.quantityGD=0;
        Item foreign=item(1538,1);check(!open(tx,p,foreign,0).success(),"foreign box rejected");
        p=owner(15393);box=box(p,2);dao.uncertain=true;
        check(!open(tx,p,box,1).success()&&ConsignTransactions.hasPending(p)&&box.quantity==2,"unknown commit gated");
        check(!open(tx,p,box,1).success(),"pending commit cannot replay");
        dao.outage=false;dao.uncertain=false;check(ConsignTransactions.reconcilePending(p),"receipt recovery");
        check(p.inventory.itemsBag.get(0).quantity==1&&p.inventory.itemsBag.stream().filter(Item::isSKH).count()==1,"recovery once");
        check(ConsignTransactions.reconcilePending(p)&&p.inventory.itemsBag.stream().filter(Item::isSKH).count()==1,"recovery replay harmless");
        Player concurrent=owner(15394);Item selected=box(concurrent,3);AtomicInteger successes=new AtomicInteger();CountDownLatch go=new CountDownLatch(1);
        ExecutorService workers=Executors.newFixedThreadPool(2);
        Callable<Void> attempt=()->{go.await();if(open(tx,concurrent,selected,0).success())successes.incrementAndGet();return null;};
        Future<Void> one=workers.submit(attempt),two=workers.submit(attempt);go.countDown();one.get();two.get();workers.shutdown();
        check(successes.get()==1&&concurrent.inventory.itemsBag.get(0).quantity==2,"concurrent redemption once");
        System.out.println("PASS ActivationBox transactions: three modes, no fee, one-box debit, full bag, rollback, trade/invalid/stale, uncertain commit and concurrency");System.exit(0);
    }
}
