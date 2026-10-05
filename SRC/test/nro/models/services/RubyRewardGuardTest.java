package nro.models.services;
import java.lang.reflect.Field;
import java.util.concurrent.*;
import nro.models.managers.GiftCodeManager;
import nro.models.player.Player;
import nro.models.player_system.GiftCode;
import static nro.models.services.PinkFixture.*;

public final class RubyRewardGuardTest {
    public static void main(String[] args)throws Exception {
        install();Player p=player(895);CountDownLatch entered=new CountDownLatch(1),called=new CountDownLatch(1);
        Field f=GiftCodeManager.class.getDeclaredField("instance");f.setAccessible(true);
        f.set(null,new GiftCodeManager(){@Override public GiftCode checkUseGiftCode(Player owner,String code){called.countDown();return null;}});
        ExecutorService pool=Executors.newSingleThreadExecutor();
        try {
            Future<?> future;
            synchronized(p){future=pool.submit(()->{entered.countDown();GiftCodeService.gI().giftCode(p,"fixture");});check(entered.await(2,TimeUnit.SECONDS),"giftcode starts");check(!called.await(150,TimeUnit.MILLISECONDS),"giftcode redemption must wait for ruby transaction monitor");}
            future.get(3,TimeUnit.SECONDS);check(called.getCount()==0,"giftcode resumes after ruby transfer unlock");
            System.out.println("PASS RubyRewardGuard");System.exit(0);
        }catch(Throwable e){e.printStackTrace();System.exit(1);}finally{pool.shutdownNow();}
    }
}
