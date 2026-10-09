package nro.models.services;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import nro.models.database.AchievementRewardDAO;
import nro.models.network.MySession;
import nro.models.player.Player;
import nro.models.player_system.Template.AchievementTemplate;

/** Service/model transactions are real; this backend isolates persistence failure scenarios. */
public final class AchievementRewardTest {
    static void check(boolean ok,String message){AchievementAuditTest.check(ok,message);}
    static class Backend extends AchievementRewardDAO {
        final Map<String,Snapshot> stored=new HashMap<>(); int commits; int fail; boolean uncertainCommitted;
        @Override public Result claim(Snapshot s)throws SQLException {
            if(fail==1){fail=0;throw new SQLException("database unavailable");}
            String key=s.id()+":"+s.index();
            if(stored.containsKey(key))return new Result(false,stored.get(key).achievements());
            if(fail==2){fail=0;if(uncertainCommitted){stored.put(key,s);commits++;}throw new CommitUncertain(new SQLException("lost commit acknowledgement"));}
            stored.put(key,s);commits++;return new Result(true,s.achievements());
        }
    }
    static AchievementAuditTest.Owner owner()throws Exception {
        var p=AchievementAuditTest.owner();p.session=PinkFixture.blank(MySession.class);p.session.userId=100;p.session.player=p;
        return p;
    }
    static void allRewards()throws Exception {
        Backend backend=new Backend();var service=new AchievementService(backend,p->true);var p=owner();p.inventory.gem=99;
        p.inventory.itemsBag.set(0,PinkFixture.item(74,1));
        for(int i=0;i<20;i++){
            long count=i==8?36_000_000L:10;p.achievement.done(i,count);
            service.confirmAchievement(p,(byte)i);service.confirmAchievement(p,(byte)i);
            check(p.inventory.ruby==(i+1)*1000&&p.inventory.gem==99&&p.achievement.isRecieve(i),"mission "+i+" gives exactly1000 ruby even with full bag");
        }
        check(backend.commits==20,"20 durable claims for20missions");
        var restored=owner();restored.achievement.load(p.achievement.toJson());
        for(int i=0;i<20;i++)check(restored.achievement.isRecieve(i)&&!restored.achievement.canReward(i),"claimed marker survives round-trip "+i);
    }
    static void concurrency()throws Exception {
        Backend backend=new Backend();var service=new AchievementService(backend,p->true);var p=owner();p.achievement.done(4,10);
        var pool=Executors.newFixedThreadPool(8);List<Future<?>> futures=new ArrayList<>();
        for(int i=0;i<100;i++)futures.add(pool.submit(()->service.confirmAchievement(p,(byte)4)));
        for(var future:futures)future.get();pool.shutdown();
        check(p.inventory.ruby==1000&&backend.commits==1,"100 concurrent claims grant once");
    }
    static void failure()throws Exception {
        Backend backend=new Backend();var service=new AchievementService(backend,p->true);var p=owner();p.achievement.done(4,10);backend.fail=1;
        service.confirmAchievement(p,(byte)4);
        check(p.inventory.ruby==0&&!p.achievement.isRecieve(4)&&!LiveSyncCommandGate.hasPendingId(p.id),"failed transaction keeps reward available");
        service.confirmAchievement(p,(byte)4);check(p.inventory.ruby==1000&&backend.commits==1,"retry succeeds once");
        for(boolean committed:new boolean[]{false,true}){
            backend=new Backend();service=new AchievementService(backend,q->true);p=owner();p.achievement.done(4,10);backend.fail=2;backend.uncertainCommitted=committed;
            service.confirmAchievement(p,(byte)4);
            check(LiveSyncCommandGate.hasPendingId(p.id)&&p.inventory.ruby==0&&!p.achievement.isRecieve(4),"ambiguous commit freezes publication until reconciliation");
            service.confirmAchievement(p,(byte)4);check(p.inventory.ruby==0,"pending request cannot claim again");
            check(service.reconcilePending(p)&&p.inventory.ruby==1000&&p.achievement.isRecieve(4)&&backend.commits==1&&!LiveSyncCommandGate.hasPendingId(p.id),"resolve committed/uncommitted acknowledgement once "+committed);
        }
        backend=new Backend();service=new AchievementService(backend,q->false);p=owner();p.achievement.done(4,10);backend.fail=2;backend.uncertainCommitted=true;
        service.confirmAchievement(p,(byte)4);p.beforeDispose=true;
        check(!service.reconcilePending(p)&&LiveSyncCommandGate.hasPendingId(p.id)&&p.inventory.ruby==1000,"failed logout save retains identity and credited state");
        p.beforeDispose=false;check(service.reconcilePending(p)&&p.inventory.ruby==1000,"logout retry cannot add ruby twice");
    }
    static void bounds()throws Exception {
        Backend backend=new Backend();var service=new AchievementService(backend,p->true);var p=owner();p.achievement.done(4,10);p.inventory.ruby=Integer.MAX_VALUE-999;
        service.confirmAchievement(p,(byte)4);check(!p.achievement.isRecieve(4)&&backend.commits==0,"overflow leaves reward unclaimed");
        p.inventory.ruby=Integer.MAX_VALUE-1000;service.confirmAchievement(p,(byte)4);check(p.inventory.ruby==Integer.MAX_VALUE&&p.achievement.isRecieve(4),"exact wallet boundary grants safely");
    }
    static void models()throws Exception {
        var p=owner();p.achievement.load("[\"[123,true]\",\"[broken,true]\",\"broken\",\"[0,false]\"]");
        check(p.achievement.getAchievementList().size()==20&&p.achievement.isRecieve(0),"missing legacy slots filled and valid claim preserved");
        p.achievement.done(19,1);check(p.achievement.getCompleted(19)==1,"later missions work after a broken legacy row");
        var opponent=owner();opponent.id=p.id+1;
        p.achievement.recordWin(opponent);p.achievement.recordWin(opponent);check(p.achievement.getCompleted(3)==1,"same opponent counted once");
        String saved=p.achievement.toJson();p.achievement.load(saved);p.achievement.recordWin(opponent);check(p.achievement.getCompleted(3)==1,"distinct opponents survive reconnect");
        opponent.id++;p.achievement.recordWin(opponent);check(p.achievement.getCompleted(3)==2,"new opponent counts");
        p.achievement.updateOnlineTime(1000);p.achievement.updateOnlineTime(3601000);check(p.achievement.getCompleted(8)==1,"online time uses elapsed hours");
        p.achievement.recordFlight(3);p.achievement.recordFlight(-7);p.achievement.recordFlight(100);check(p.achievement.getCompleted(5)==10,"small and large flight steps count safely up to target");
        p.nPoint.power=20;p.achievement.getCompleted(16);p.nPoint.power=0;check(p.achievement.canReward(16),"earned power achievement stays completed after power decrease");
    }
    public static void main(String[] args){try{AchievementAuditTest.fixture();allRewards();concurrency();failure();bounds();models();System.out.println("ACHIEVEMENT_REWARD PASS20missions/concurrency/failure/uncertain/bounds/roundtrip/distinct/time/flight");System.exit(0);}catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
