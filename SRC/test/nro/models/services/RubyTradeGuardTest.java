package nro.models.services;
import java.util.concurrent.*;
import nro.models.player.*;
import nro.models.services_func.*;
import static nro.models.services.PinkFixture.*;

public final class RubyTradeGuardTest {
    public static void main(String[] args)throws Exception {
        install();Player a=player(880),b=player(890);a.idMark=new IDMark();b.idMark=new IDMark();
        ExecutorService executor=Executors.newSingleThreadExecutor();CountDownLatch entered=new CountDownLatch(1);
        try {
            Future<Trade> result;
            synchronized(a){
                result=executor.submit(()->{entered.countDown();return new Trade(b,a);});
                check(entered.await(2,TimeUnit.SECONDS),"trade thread entered");Thread.sleep(150);
                check(!result.isDone(),"trade snapshot must wait for ruby transaction participant monitor");
            }
            Trade trade=result.get(3,TimeUnit.SECONDS);
            check(TransactionService.isTrading(a)&&TransactionService.isTrading(b),"both participants publish trade after unlocking");
            trade.dispose();
            check(!TransactionService.isTrading(a)&&!TransactionService.isTrading(b),"dispose removes both trade markers");
            a.beforeDispose=true;
            try{new Trade(a,b);throw new AssertionError("offline player must not start trade");}catch(IllegalStateException expected){}
            System.out.println("PASS RubyTradeGuard");System.exit(0);
        }catch(Throwable e){e.printStackTrace();System.exit(1);}finally{executor.shutdownNow();}
    }
}
