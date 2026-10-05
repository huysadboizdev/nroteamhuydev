package nro.models.services;

import java.sql.SQLException;
import nro.models.database.FreeMembershipDAO;
import nro.models.player.Player;
import nro.models.task.*;
import nro.models.network.MySession;
import static nro.models.services.PinkFixture.*;

public final class FreeMembershipTest {
    public static class Connected extends MySession { public Connected()throws Exception{super(null);}@Override public boolean isConnected(){return true;} }
    static class Dao extends FreeMembershipDAO {
        int writes;boolean fail;
        @Override public boolean activate(long player,int account,String task)throws SQLException{if(fail)throw new SQLException("fixture rollback");writes++;return true;}
    }
    static Player owner()throws Exception {
        Player p=player(100);p.session=blank(Connected.class);p.session.player=p;p.session.userId=200;
        p.playerTask=new TaskPlayer();p.playerTask.taskMain=new TaskMain();p.playerTask.taskMain.subTasks.add(new SubTaskMain());
        p.inventory.gold=123;p.inventory.gem=456;p.inventory.ruby=789;p.session.vnd=123456;return p;
    }
    public static void main(String[] args)throws Exception {
        install();var dao=new Dao();var service=new FreeMembershipService(dao);Player p=owner();
        for(int id:new int[]{0,1}){p.playerTask.taskMain.id=id;check(!service.open(p).success()&&dao.writes==0,"task not finished");}
        p.playerTask.taskMain.id=1;p.playerTask.taskMain.index=1;
        check(!service.open(p).success()&&dao.writes==0,"must report task first");p.playerTask.taskMain.index=0;
        p.playerTask.taskMain.id=2;p.kolRewardPending=true;
        check(!service.open(p).success()&&dao.writes==0,"pending gate");p.kolRewardPending=false;
        dao.fail=true;check(!service.open(p).success()&&!p.session.actived,"rollback does not activate RAM");dao.fail=false;
        check(service.open(p).success()&&p.session.actived&&dao.writes==1,"activate after task2");
        check(service.open(p).success()&&dao.writes==1,"repeat active no write");
        check(p.inventory.gold==123&&p.inventory.gem==456&&p.inventory.ruby==789&&p.session.vnd==123456,"all currencies unchanged");
        p=owner();p.playerTask.taskMain.id=30;check(service.open(p).success()&&p.session.actived,"old advanced player eligible");
        p=owner();p.playerTask.taskMain.id=2;p.session.player=null;
        check(!service.open(p).success(),"stale session denied");
        System.out.println("PASS FreeMembership: task boundary, report required, pending, SQL failure, immediate activation, advanced player, repeat, no fees");System.exit(0);
    }
}
