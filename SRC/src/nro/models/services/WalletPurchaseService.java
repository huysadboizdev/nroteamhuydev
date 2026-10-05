package nro.models.services;

import java.util.UUID;
import nro.models.database.KolRewardDAO;
import nro.models.player.Player;

/** Keeps the original purchase while a wallet commit awaits reconciliation. */
public final class WalletPurchaseService {
    private static final WalletPurchaseService INSTANCE=new WalletPurchaseService(new KolRewardDAO());
    private final KolRewardDAO dao;

    public record Pending(String receiptId,int cost,String kind,Runnable grant) {}
    public enum Result { SUCCESS, RECOVERED, INSUFFICIENT, PENDING, FAILED, GRANT_FAILED }

    public WalletPurchaseService(KolRewardDAO dao) { this.dao=dao; }
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
            if(player.walletPurchasePending==null)return true;
            Result result=recover(player);
            if(result==Result.GRANT_FAILED) {
                try { nro.models.database.PlayerDAO.notifyWalletPurchaseResult(player,result); }
                catch(Exception packetFailure) { /* Callback failure was already logged; never replay it. */ }
            }
            return player.walletPurchasePending==null;
        }
    }

    public Result purchase(Player player,String kind,int cost,Runnable grant) {
        synchronized(player) {
            if(AdminPlayerActions.hasRenamePendingId(player.id) || (player.session!=null && AdminPlayerActions.hasRenamePendingAccount(player.session.userId)))return Result.PENDING;
            if(player.kolProgressPending || KolQuestService.hasProgressPendingId(player.id))return Result.PENDING;
            if(player.walletPurchasePending!=null)return recover(player);
            if(player.kolRewardPending && !KolQuestService.gI().reconcilePending(player))return Result.PENDING;
            Pending pending=new Pending(UUID.randomUUID().toString(),cost,kind,grant);
            player.walletPurchasePending=pending;
            try {
                var result=dao.debit(player.getSession().userId,cost,kind,pending.receiptId(),null);
                return apply(player,pending,result,false);
            } catch(KolRewardDAO.CommitUncertain unknownOutcome) {
                return Result.PENDING;
            } catch(Exception rejected) {
                player.walletPurchasePending=null;
                return Result.FAILED;
            }
        }
    }
}
