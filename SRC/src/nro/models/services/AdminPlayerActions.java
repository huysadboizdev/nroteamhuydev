package nro.models.services;

import java.sql.*;
import java.util.Locale;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player.Inventory;
import nro.models.server.Client;
import nro.models.utils.Logger;

/** Narrow online actions shared by admin forms and ordinary rename cards. */
public final class AdminPlayerActions {
    @FunctionalInterface public interface Connections { Connection open() throws SQLException; }
    public static final Object NAME_WRITE_LOCK=new Object();
    private static final AdminPlayerActions INSTANCE = new AdminPlayerActions(LocalManager::getConnection);
    private static final java.util.Map<Player, RenamePending> RENAME_PENDING = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ScheduledExecutorService RECOVERY = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(task -> {
        Thread t=new Thread(task,"Rename reconciliation");t.setDaemon(true);return t;
    });
    private static final class RenamePending {
        final AdminPlayerActions owner; final Player player; final int account; final String oldName,newName; final Item card;
        boolean applied,finalizing;
        RenamePending(AdminPlayerActions owner,Player p,int account,String oldName,String newName,Item card) {
            this.owner=owner;this.player=p;this.account=account;this.oldName=oldName;this.newName=newName;this.card=card;
        }
    }
    public static boolean hasRenamePending(Player player) { return player!=null && RENAME_PENDING.containsKey(player); }
    public static boolean hasRenamePendingId(long id) { return RENAME_PENDING.keySet().stream().anyMatch(p -> p.id==id); }
    public static boolean hasRenamePendingAccount(int account) { return RENAME_PENDING.values().stream().anyMatch(op -> op.account==account); }
    public static boolean isFinalizingRenameLogout(Player p) {
        RenamePending op=RENAME_PENDING.get(p);return op!=null&&op.finalizing&&Thread.holdsLock(p);
    }
    private final Connections connections;
    private final java.util.function.Predicate<Player> logoutSaver;
    public AdminPlayerActions(Connections connections) { this(connections,nro.models.database.PlayerDAO::savePendingRenameLogout); }
    public AdminPlayerActions(Connections connections,java.util.function.Predicate<Player> logoutSaver) {
        this.connections=connections;this.logoutSaver=logoutSaver;
    }
    private void retainRename(Player p,int account,String oldName,String newName,Item card) {
        RenamePending op=new RenamePending(this,p,account,oldName,newName,card);RENAME_PENDING.put(p,op);
        if(this==INSTANCE)schedule(op);
    }
    private void schedule(RenamePending op) {
        RECOVERY.schedule(()->{
            if(RENAME_PENDING.get(op.player)!=op)return;
            if(!op.owner.reconcileRename(op.player)){schedule(op);return;}
            if(op.player.beforeDispose)op.player.dispose();
        },3,java.util.concurrent.TimeUnit.SECONDS);
    }
    public boolean reconcileRename(Player p) {
        if(p==null)return true;
        RenamePending op=RENAME_PENDING.get(p);
        if(op==null)return true;
        if(op.owner!=this)return op.owner.reconcileRename(p);
        synchronized(p) {
            if(RENAME_PENDING.get(p)!=op)return !hasRenamePending(p);
            try {
                if(!op.applied) {
                    String stored;
                    try(Connection c=connections.open();PreparedStatement q=c.prepareStatement("SELECT name FROM player WHERE id=? AND account_id=?")) {
                        q.setLong(1,p.id);q.setInt(2,op.account);
                        try(ResultSet r=q.executeQuery()){if(!r.next())return false;stored=r.getString(1);}
                    }
                    if(!op.oldName.equals(stored)&&!op.newName.equals(stored))return false;
                    if(op.newName.equals(stored)) {
                        if(op.card!=null&&(!p.inventory.itemsBag.contains(op.card)||op.card.quantity<1))return false;
                        if(p.beforeDispose)p.name=stored;
                        else if(!Client.gI().renameOnline(p,stored,()->true))return false;
                        if(op.card!=null)InventoryService.gI().subQuantityItemsBag(p,op.card,1);
                    }
                    op.applied=true;
                }
                op.finalizing=true;
                try{if(!logoutSaver.test(p))return false;}finally{op.finalizing=false;}
                RENAME_PENDING.remove(p,op);return true;
            } catch(Exception failure) {Logger.logException(AdminPlayerActions.class,failure);return false;}
        }
    }
    public static AdminPlayerActions gI() { return INSTANCE; }
    public static boolean isOnline(Player p) {
        return p != null && !p.beforeDispose && p.session != null && p.session.player == p
                && p.session.userId > 0 && Client.gI().getPlayer(p.id) == p
                && Client.gI().getPlayerByUser(p.session.userId) == p;
    }
    public static boolean requireOnline(Player actor, Player target) {
        if (isOnline(target) && !hasRenamePending(target)) return true;
        if (hasRenamePending(target)) {
            Service.gI().sendThongBao(actor,"Đổi tên đang chờ xác minh dữ liệu. Hãy đăng nhập lại sau khi cơ sở dữ liệu hoạt động.");
            return false;
        }
        Service.gI().sendThongBao(actor,"Người chơi đã offline hoặc phiên đăng nhập không còn hợp lệ.");
        return false;
    }
    public static boolean hasPending(Player p) {
        return CommesonChallengeService.hasPendingId(p.id) || nro.models.shop_ky_gui.ConsignTransactions.hasPendingId(p.id) || nro.models.shop_ky_gui.ConsignTransactions.hasPendingAccount(p.session.userId) || hasRenamePending(p) || RubyGiftService.hasPending(p) || RubyGiftService.hasPendingId(p.id)
                || RubyGiftService.hasPendingAccount(p.session.userId) || p.kolRewardPending
                || p.walletPurchasePending != null || p.kolProgressPending
                || KolQuestService.hasProgressPendingId(p.id)
                || KolQuestService.hasProgressPendingAccount(p.session.userId);
    }
    public long grantCurrency(Player actor, Player target, int id, int quantity) {
        if (!AdminCommandService.requireAdmin(actor)) return 0;
        if (id < -3 || id > -1 || quantity <= 0) throw new IllegalArgumentException("Tiền hoặc số lượng không hợp lệ");
        if (!requireOnline(actor,target)) return 0;
        synchronized (target) {
            if (!AdminCommandService.requireAdmin(actor) || !requireOnline(actor,target)) return 0;
            if (hasPending(target)) { Service.gI().sendThongBao(actor,"Người nhận có giao dịch chờ đối soát."); return 0; }
            long before = id == -1 ? target.inventory.gold : id == -2 ? target.inventory.gem : target.inventory.ruby;
            long cap = id == -1 ? Inventory.LIMIT_GOLD : 2_000_000_000L;
            long accepted = Math.max(0, Math.min((long)quantity, cap-before));
            if (accepted == 0) return 0;
            if (id == -1) target.inventory.gold=before+accepted;
            else if (id == -2) target.inventory.gem=(int)(before+accepted);
            else target.inventory.ruby=(int)(before+accepted);
            Service.gI().sendMoney(target);
            return accepted;
        }
    }
    public boolean kick(Player actor, Player target) {
        if (!AdminCommandService.requireAdmin(actor) || !requireOnline(actor,target)) return false;
        final nro.models.network.MySession session;
        final String name;
        synchronized(target) {
            if (!AdminCommandService.requireAdmin(actor) || !requireOnline(actor,target)) return false;
            session=target.session;name=target.name;
        }
        // Logout reconciles gifts by locking both participants in ID order.
        // Never call it while holding one participant's player monitor.
        if (!AdminCommandService.requireAdmin(actor) || target.session!=session
                || session.player!=target || !requireOnline(actor,target)) return false;
        try {
            Client.gI().kickSession(session);
            if(actor!=target)Service.gI().sendThongBao(actor,"Kick người chơi " + name + " thành công");
            return true;
        } catch(Exception failure) {
            Logger.logException(AdminPlayerActions.class,failure);
            Service.gI().sendThongBao(actor,"Không thể ngắt phiên người chơi. Hãy thử lại.");return false;
        }
    }

    public boolean ban(Player actor, Player target) {
        if (!AdminCommandService.requireAdmin(actor) || !requireOnline(actor,target)) return false;
        synchronized (target) {
            if (!AdminCommandService.requireAdmin(actor) || !requireOnline(actor,target)) return false;
            var session = target.session;
            boolean attempted=false,success=false;
            try (Connection c=connections.open(); PreparedStatement q=c.prepareStatement("UPDATE account SET ban=1 WHERE id=? AND username=?")) {
                q.setInt(1,session.userId);q.setString(2,session.uu);
                if (!AdminCommandService.requireAdmin(actor) || !isOnline(target) || target.session!=session) return false;
                attempted=true;success=q.executeUpdate()==1;
            } catch (Exception failure) {
                Logger.logException(AdminPlayerActions.class,failure);
                if(attempted) {
                    try(Connection c=connections.open();PreparedStatement q=c.prepareStatement("SELECT ban FROM account WHERE id=? AND username=?")) {
                        q.setInt(1,session.userId);q.setString(2,session.uu);
                        try(ResultSet r=q.executeQuery()){success=r.next()&&r.getBoolean(1);}
                    } catch(Exception readback){Logger.logException(AdminPlayerActions.class,readback);success=false;}
                }
            }
            if(!success) {
                Service.gI().sendThongBao(actor,"Không thể khóa tài khoản: dữ liệu hoặc phiên đã thay đổi.");return false;
            }
            target.idMark.setLastTimeBan(System.currentTimeMillis());target.idMark.setBan(true);
            session.timeWait=5;
            Service.gI().sendThongBao(target,"Tài khoản đã bị khóa. Game sẽ ngắt kết nối sau 5 giây.");
            Service.gI().sendThongBao(actor,"Ban người chơi " + target.name + " thành công");
            return true;
        }
    }
    public boolean rename(Player actor, Player target, String requested, boolean byItem) {
        if ((!byItem || actor!=target) && !AdminCommandService.requireAdmin(actor)) return false;
        String name=requested==null ? "" : requested.toLowerCase(Locale.ROOT);
        if (!name.matches("[a-z0-9]{5,10}")) {
            Service.gI().sendThongBao(actor,"Tên phải gồm 5-10 ký tự a-z, 0-9.");return false;
        }
        if (!reconcileRename(target) || !requireOnline(actor,target)) return false;
        synchronized (target) {
            if (!requireOnline(actor,target) || ((!byItem || actor!=target) && !AdminCommandService.requireAdmin(actor))) return false;
            if(name.equals(target.name)){Service.gI().sendThongBao(actor,"Tên mới trùng tên hiện tại.");return false;}
            Item card=byItem ? InventoryService.gI().findItem(target.inventory.itemsBag,2006) : null;
            if (byItem && (actor!=target || card==null || card.quantity<1)) {
                Service.gI().sendThongBao(actor,"Không tìm thấy thẻ đổi tên.");return false;
            }
            if (hasPending(target) || nro.models.services_func.TransactionService.isTrading(target)) {
                Service.gI().sendThongBao(actor,"Người chơi có giao dịch đang thực hiện hoặc chờ đối soát.");return false;
            }
            var session=target.session;String old=target.name;
            try {
                boolean renamed=Client.gI().renameOnline(target,name,()-> {
                  synchronized(NAME_WRITE_LOCK) {
                    boolean attempted = false;
                    try(Connection c=connections.open()) {
                        c.setAutoCommit(false);
                        try {
                            try(PreparedStatement q=c.prepareStatement("SELECT id FROM player WHERE name=? AND id<>?")) {
                                q.setString(1,name);q.setLong(2,target.id);
                                try(ResultSet r=q.executeQuery()){if(r.next()){c.rollback();return false;}}
                            }
                            if (target.session!=session || !isOnline(target) || ((!byItem || actor!=target) && !AdminCommandService.requireAdmin(actor))) {c.rollback();return false;}
                            try(PreparedStatement q=c.prepareStatement("UPDATE player SET name=? WHERE id=? AND account_id=? AND name=?")) {
                                q.setString(1,name);q.setLong(2,target.id);q.setInt(3,session.userId);q.setString(4,old);
                                attempted = true;
                                if(q.executeUpdate()!=1){c.rollback();return false;}
                            }
                            c.commit();return true;
                        } catch(Exception e){try {c.rollback();} catch(Exception rollback){e.addSuppressed(rollback);}throw e;}
                    } catch(Exception failure) {
                        if (!attempted) throw failure;
                        Logger.logException(AdminPlayerActions.class,failure);
                        // A commit or connection close can throw after SQL has persisted.
                        // Confirm ownership and the intended value before updating the registry.
                        try(Connection c=connections.open();PreparedStatement q=c.prepareStatement("SELECT name FROM player WHERE id=? AND account_id=?")) {
                            q.setLong(1,target.id);q.setInt(2,session.userId);
                            try(ResultSet r=q.executeQuery()) {
                                if (r.next()) {
                                    String persisted=r.getString(1);
                                    if (name.equals(persisted)) return true;
                                    if (old.equals(persisted)) return false;
                                }
                            }
                        } catch(Exception readback){failure.addSuppressed(readback);Logger.logException(AdminPlayerActions.class,readback);}
                        retainRename(target,session.userId,old,name,card);
                        session.timeWait=1;
                        Service.gI().sendThongBao(actor,"Đổi tên đang chờ xác minh. Hãy đăng nhập lại sau khi cơ sở dữ liệu hoạt động.");
                        return false;
                    }
                  }
                });
                if(!renamed){Service.gI().sendThongBao(actor,"Tên đã tồn tại hoặc dữ liệu người chơi đã thay đổi.");return false;}
                if(byItem){InventoryService.gI().subQuantityItemsBag(target,card,1);InventoryService.gI().sendItemBags(target);}
                Service.gI().sendThongBao(actor,"Đổi tên thành công: "+name);
                if(actor!=target)Service.gI().sendThongBao(target,"Tên mới của bạn: "+name);
                Service.gI().player(target);Service.gI().Send_Caitrang(target);Service.gI().sendFlagBag(target);
                return true;
            } catch(Exception e){Logger.logException(AdminPlayerActions.class,e);Service.gI().sendThongBao(actor,"Không thể đổi tên. Hãy thử lại.");return false;}
        }
    }
}
