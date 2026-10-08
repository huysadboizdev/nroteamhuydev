package nro.models.services;

import java.sql.SQLException;
import java.util.function.Predicate;
import nro.models.database.KolRewardDAO;
import nro.models.network.MySession;
import nro.models.player.Player;
import static nro.models.services.PinkFixture.*;

public final class WalletPendingLogoutTest {
    static final class ReceiptDAO extends KolRewardDAO {
        int debits, reads; boolean available;
        @Override public WalletDebit debit(int account,int cost,String kind,String receipt,Integer active)throws SQLException {
            debits++; throw new CommitUncertain(new SQLException("connection lost after commit"));
        }
        @Override public WalletDebit reconcileDebit(int account,int cost,String kind,String receipt)throws SQLException {
            reads++; if(!available)throw new SQLException("database unavailable");
            return new WalletDebit(true,900);
        }
    }
    static boolean gate(int account)throws Exception {
        try {return (boolean)WalletPurchaseService.class.getMethod("hasPendingAccount",int.class).invoke(null,account);}
        catch(NoSuchMethodException missing){return false;}
    }
    public static void main(String[] args)throws Exception {
        ReceiptDAO dao=new ReceiptDAO();int[] grants={0},saves={0};boolean[] saveReady={false};
        Predicate<Player> saver=p->{saves[0]++;check(gateUnchecked(p.session.userId),"login stays gated during full save");return saveReady[0];};
        WalletPurchaseService service;
        try {service=WalletPurchaseService.class.getConstructor(KolRewardDAO.class,Predicate.class).newInstance(dao,saver);}
        catch(NoSuchMethodException missing){service=new WalletPurchaseService(dao);}
        Player p=player(98531);p.session=blank(MySession.class);p.session.userId=98531;p.session.player=p;p.session.vnd=1000;
        check(service.purchase(p,"vip",100,()->grants[0]++)==WalletPurchaseService.Result.PENDING,"uncertain debit stays pending");
        check(gate(98531),"uncertain wallet receipt must retain account gate");
        p.beforeDispose=true;
        check(!WalletPurchaseService.gI().reconcilePending(p),"offline recovery delegates to receipt owner");
        p.dispose();
        check(p.session!=null&&grants[0]==0,"offline owner survives while receipt unreadable");
        dao.available=true;
        check(!WalletPurchaseService.gI().reconcilePending(p),"failed logout save retains operation");
        check(grants[0]==1&&p.session.vnd==900&&gate(98531),"confirmed receipt applies once while account remains gated");
        check(!service.reconcilePending(p)&&grants[0]==1,"save retry never replays grant");
        check(service.purchase(p,"vip",100,()->grants[0]++)==WalletPurchaseService.Result.PENDING,"no new purchase during retained logout");
        saveReady[0]=true;
        check(service.reconcilePending(p)&&!gate(98531),"account gate clears only after checked save");
        check(service.reconcilePending(p)&&grants[0]==1&&dao.debits==1&&saves[0]==3,"repeated recovery cannot duplicate debit or grant");
        System.out.println("PASS WalletPendingLogout");
    }
    static boolean gateUnchecked(int account){try{return gate(account);}catch(Exception e){throw new RuntimeException(e);}}
}
