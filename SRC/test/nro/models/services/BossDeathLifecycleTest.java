package nro.models.services;
import nro.models.boss.*;
import nro.models.consts.BossStatus;
import nro.models.player.NPoint;
import static nro.models.services.PinkFixture.*;
public final class BossDeathLifecycleTest {
    public static class Parent extends Boss {
        public Parent()throws Exception{super(0,(BossData)null);}
        @Override public boolean noticePublic(){return false;}
    }
    static Parent parent(int hp,BossStatus state)throws Exception{
        Parent p=blank(Parent.class);p.nPoint=blank(NPoint.class);p.nPoint.hp=hp;p.bossStatus=state;p.currentLevel=0;p.bossAppearTogether=new Boss[][]{new Boss[0]};return p;
    }
    static void attach(Boss child,Boss parent)throws Exception{
        var field=Boss.class.getDeclaredField("parentBoss");field.setAccessible(true);field.set(child,parent);
    }
    static void callback(Class<? extends Boss> type)throws Exception{
        Boss child=blank(type);Parent dead=parent(0,BossStatus.DIE);attach(child,dead);child.doneChatE();
        check(dead.bossStatus==BossStatus.DIE,type.getSimpleName()+" must not reactivate a dead parent");
        Parent live=parent(100,BossStatus.AFK);attach(child,live);child.doneChatE();check(live.bossStatus==BossStatus.ACTIVE,"living waiting parent activates normally");
    }
    public static void main(String[] args)throws Exception{
        callback(nro.models.boss.tieu_doi_sat_thu.SO1.class);callback(nro.models.boss.tieu_doi_sat_thu.SO2.class);
        callback(nro.models.boss.tieu_doi_sat_thu_namek.SO1_NM.class);callback(nro.models.boss.tieu_doi_sat_thu_namek.SO2_NM.class);
        for(BossStatus phase:new BossStatus[]{BossStatus.DIE,BossStatus.CHAT_E,BossStatus.LEAVE_MAP,BossStatus.REST}){
            Parent p=parent(0,phase);for(BossStatus combat:new BossStatus[]{BossStatus.ACTIVE,BossStatus.AFK,BossStatus.CHAT_S}){p.changeStatus(combat);check(p.bossStatus==phase,"dead boss preserves lifecycle "+phase+" on "+combat);}
        }
        Parent p=parent(0,BossStatus.REST);p.changeStatus(BossStatus.RESPAWN);check(p.bossStatus==BossStatus.RESPAWN,"respawn with old zero HP stays permitted");
        p.nPoint.hp=100;p.changeStatus(BossStatus.CHAT_S);check(p.bossStatus==BossStatus.CHAT_S,"spawn restored HP can enter chat");
        Parent concurrent=parent(100,BossStatus.AFK);var ready=new java.util.concurrent.CountDownLatch(1);
        Thread callback=new Thread(()->{ready.countDown();concurrent.changeStatus(BossStatus.ACTIVE);});
        boolean blocked;
        synchronized(concurrent){
            callback.start();check(ready.await(1,java.util.concurrent.TimeUnit.SECONDS),"callback started");
            long deadline=System.nanoTime()+1_000_000_000L;
            while(callback.isAlive()&&callback.getState()!=Thread.State.BLOCKED&&System.nanoTime()<deadline)Thread.sleep(1);
            blocked=callback.getState()==Thread.State.BLOCKED;
            concurrent.nPoint.hp=0;concurrent.bossStatus=BossStatus.DIE;
        }
        callback.join(2000);check(blocked&&!callback.isAlive()&&concurrent.bossStatus==BossStatus.DIE,"callback guard/assignment share the injury monitor and cannot race death");
        System.out.println("PASS Boss death lifecycle: actual SO1/SO2 normal/Namek callbacks, dead parent never reactivated, death/leave/rest preserved, legitimate spawn retained");
    }
}
