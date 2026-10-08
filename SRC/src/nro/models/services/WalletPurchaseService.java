package nro.models.services;

import java.util.UUID;
import nro.models.database.KolRewardDAO;
import nro.models.player.Player;

/** Keeps the original purchase while a wallet commit awaits reconciliation. */
public final class WalletPurchaseService {
    private static final WalletPurchaseService INSTANCE=new WalletPurchaseService(new KolRewardDAO());
    private final KolRewardDAO dao;
    private final java.util.function.Predicate<Player> logoutSaver;
    private static final java.util.Map<Player,Operation> ACTIVE=new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ScheduledExecutorService RECOVERY=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(task->{Thread t=new Thread(task,"Wallet purchase reconciliation");t.setDaemon(true);return t;});
    private static final class Operation {
        final WalletPurchaseService owner; final Player player; final int account;
        boolean finalizing;
        Operation(WalletPurchaseService owner,Player player){this.owner=owner;this.player=player;this.account=player.session.userId;}
    }
    public static boolean hasPending(Player player){return ACTIVE.containsKey(player);}
    public static boolean hasPendingId(long id){return ACTIVE.keySet().stream().anyMatch(p->p.id==id);}
    public static boolean hasPendingAccount(int account){return ACTIVE.values().stream().anyMatch(op->op.account==account);}
    public static boolean isFinalizingLogout(Player player){Operation op=ACTIVE.get(player);return op!=null&&op.finalizing&&Thread.holdsLock(player);}
    private void schedule(Operation op){
        RECOVERY.schedule(()->{
            if(ACTIVE.get(op.player)!=op)return;
            if(!op.owner.reconcilePending(op.player)){schedule(op);return;}
            if(op.player.beforeDispose)op.player.dispose();
        },3,java.util.concurrent.TimeUnit.SECONDS);
    }

    public record Pending(String receiptId,int cost,String kind,Runnable grant) {}
    public enum Result { SUCCESS, RECOVERED, INSUFFICIENT, PENDING, FAILED, GRANT_FAILED }

    public WalletPurchaseService(KolRewardDAO dao) { this(dao,nro.models.database.PlayerDAO::savePendingWalletLogout); }
    public WalletPurchaseService(KolRewardDAO dao,java.util.function.Predicate<Player> logoutSaver) { this.dao=dao;this.logoutSaver=logoutSaver; }
    public static WalletPurchaseService gI() { return INSTANCE; }

    private Result apply(Player player,Pending pending,KolRewardDAO.WalletDebit result,boolean recovered) {
        player.walletPurchasePending=null;
        player.getSession().vnd=result.vnd();
        if(!result.committed())return recovered?Result.RECOVERED:(result.insufficient()?Result.INSUFFICIENT:Result.FAILED);
        // Clear before invoking the callback: a packet exception must never replay rewards.
        try { pending.grant().run(); }
        catch(Exception grantFailure) {
            nro.models.utils.Logger.logException(WalletPurchaseService.class,grantFailure,"Wallet grant callback failed after confirmed debit");
            return Result.GRANT_FAILED;
        }
        return recovered?Result.RECOVERED:Result.SUCCESS;
    }

    private Result recover(Player player) {
        Pending pending=player.walletPurchasePending;
        if(pending==null)return Result.RECOVERED;
        try {
            var result=dao.reconcileDebit(player.getSession().userId,pending.cost(),pending.kind(),pending.receiptId());
            return apply(player,pending,result,true);
        } catch(Exception unavailable) { return Result.PENDING; }
    }

    public boolean reconcilePending(Player player) {
        synchronized(player) {
            Operation op=ACTIVE.get(player);
            if(op!=null&&op.owner!=this)return op.owner.reconcilePending(player);
            if(player.walletPurchasePending==null&&op==null)return true;
            if(op==null){op=new Operation(this,player);ACTIVE.put(player,op);if(this==INSTANCE)schedule(op);}
            Result result=player.walletPurchasePending==null?Result.RECOVERED:recover(player);
            if(result==Result.GRANT_FAILED && !player.beforeDispose) {
                try { nro.models.database.PlayerDAO.notifyWalletPurchaseResult(player,result); }
                catch(Exception packetFailure) { /* Callback failure was already logged; never replay it. */ }
            }
            if(player.walletPurchasePending!=null)return false;
            if(player.beforeDispose){
                op.finalizing=true;
                try{if(!logoutSaver.test(player))return false;player.walletLogoutSaved=true;}
                catch(Exception saveFailure){return false;}
                finally{op.finalizing=false;}
            }
            ACTIVE.remove(player,op);
            return true;
        }
    }

    public Result purchase(Player player,String kind,int cost,Runnable grant) {
        synchronized(player) {
            if(LiveSyncMutationGate.blocked(player))return Result.PENDING;
            if(player.beforeDispose || (hasPendingId(player.id)&&!hasPending(player))
                    || (player.session!=null&&hasPendingAccount(player.session.userId)&&!hasPending(player)))return Result.PENDING;
            if(AdminPlayerActions.hasRenamePendingId(player.id) || (player.session!=null && AdminPlayerActions.hasRenamePendingAccount(player.session.userId)))return Result.PENDING;
            if(player.kolProgressPending || KolQuestService.hasProgressPendingId(player.id))return Result.PENDING;
            if(player.walletPurchasePending!=null || hasPending(player)) {
                return reconcilePending(player)?Result.RECOVERED:Result.PENDING;
            }
            if(player.kolRewardPending && !KolQuestService.gI().reconcilePending(player))return Result.PENDING;
            Pending pending=new Pending(UUID.randomUUID().toString(),cost,kind,grant);
            player.walletPurchasePending=pending;
            Operation op=new Operation(this,player);ACTIVE.put(player,op);
            try {
                var result=dao.debit(player.getSession().userId,cost,kind,pending.receiptId(),null);
                Result outcome=apply(player,pending,result,false);
                ACTIVE.remove(player,op);
                return outcome;
            } catch(KolRewardDAO.CommitUncertain unknownOutcome) {
                if(this==INSTANCE)schedule(op);
                return Result.PENDING;
            } catch(Exception rejected) {
                player.walletPurchasePending=null;
                ACTIVE.remove(player,op);
                return Result.FAILED;
            }
        }
    }
}
