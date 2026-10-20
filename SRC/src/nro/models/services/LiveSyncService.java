package nro.models.services;

import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import nro.models.data.LocalManager;
import nro.models.database.PlayerDAO;
import nro.models.managers.GiftCodeManager;
import nro.models.network.MySession;
import nro.models.player.Player;
import nro.models.server.Client;
import nro.models.shop_ky_gui.ConsignShopManager;
import org.json.simple.JSONObject;

/** Queued player/account changes have one server writer and a transactional receipt. */
public final class LiveSyncService {
    @FunctionalInterface public interface Connections {Connection open()throws SQLException;}
    private static final LiveSyncService INSTANCE=new LiveSyncService(LocalManager::getConnection);
    private static final Set<String> PLAYER=Set.of("GIFT_ITEM","TOPUP_PLAYER","UPDATE_STATS","UPDATE_PET","DELETE_ITEM","UPDATE_ITEM_QTY","WARP_HOME","HEAL_FULL","CHANGE_TASK","UPGRADE_MAGIC_TREE");
    private final Connections connections;
    private final Map<Long,Pending> unresolved=new LinkedHashMap<>();
    private ScheduledExecutorService scheduler;
    private volatile boolean isRunning;
    private static final class Pending {
        final long command,playerId;final int account;final Player player;final Runnable publish;final MySession kick;
        boolean applied,finalized;
        Pending(long command,long playerId,int account,Player player,Runnable publish,MySession kick){this.command=command;this.playerId=playerId;this.account=account;this.player=player;this.publish=publish;this.kick=kick;}
    }
    public LiveSyncService(Connections connections){this.connections=Objects.requireNonNull(connections);}
    public static LiveSyncService gI(){return INSTANCE;}
    public static boolean supports(String type){return type!=null&&(PLAYER.contains(type)||Set.of("EVENT_SET","KICK_PLAYER","UPDATE_ACCOUNT","ACCOUNT_ACTION").contains(type)||LiveSyncCatalogCommands.supports(type)||LiveSyncConsignCommands.supports(type));}
    public synchronized void start(){if(isRunning)return;isRunning=true;scheduler=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"Web command queue");t.setDaemon(true);return t;});scheduler.scheduleWithFixedDelay(()->{processQueue();BossAdminHeartbeat.tick();},1000,1500,TimeUnit.MILLISECONDS);}
    public void stop(){ScheduledExecutorService executor;synchronized(this){isRunning=false;executor=scheduler;if(executor!=null)executor.shutdown();}if(executor!=null)try{if(!executor.awaitTermination(30,TimeUnit.SECONDS))throw new IllegalStateException("Web worker did not drain; do not replace JAR");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}synchronized(this){for(Pending p:new ArrayList<>(unresolved.values()))try{recover(p);}catch(Exception ignored){}if(!unresolved.isEmpty())throw new IllegalStateException("Unresolved web commands; maintenance must retain the running JVM");}}
    public synchronized void processQueue(){
        try{
            for(Pending p:new ArrayList<>(unresolved.values()))recover(p);
            if(ServerMaintenanceGate.paused())return;
            List<Long> ids;try(Connection c=connections.open()){ids=LiveSyncCommandDAO.pending(c);}
            for(long id:ids){if(scheduler!=null&&!isRunning)break;if(!unresolved.containsKey(id))process(id);}
        }catch(Exception e){System.err.println("[LiveSync] Queue unavailable: "+e.getClass().getSimpleName());}
    }
    /** Recover existing commits without admitting new admin mutations or stopping this worker. */
    public synchronized void drainForMaintenance() throws Exception {
        for(Pending p:new ArrayList<>(unresolved.values()))recover(p);
        if(!unresolved.isEmpty())throw new IllegalStateException("Web commands still awaiting reconciliation");
    }
    private static long positive(JSONObject p,String key){try{long value=Long.parseLong(String.valueOf(p.get(key)));if(value<=0)throw new IllegalArgumentException();return value;}catch(RuntimeException e){throw new IllegalArgumentException("Invalid "+key,e);}}
    private static boolean accountCommand(String type){return "UPDATE_ACCOUNT".equals(type)||"ACCOUNT_ACTION".equals(type);}
    private void process(long command){synchronized(LiveSyncCommandGate.LIFECYCLE_LOCK){
        if(ServerMaintenanceGate.paused())return;
        try{
            long playerId=0;int account=0;boolean hasReceipt;LiveSyncCommandDAO.Command descriptor;
            try(Connection probe=connections.open()){
            probe.setAutoCommit(false);LiveSyncCommandDAO.Command cmd=LiveSyncCommandDAO.lock(probe,command);if(cmd==null){probe.rollback();return;}
            descriptor=cmd;
            if(!supports(cmd.type()))throw new IllegalArgumentException("Unsupported command: "+cmd.type());
            hasReceipt=LiveSyncCommandDAO.receipt(probe,command);
            if(hasReceipt){
                // After restart, player/account login already reads the committed SQL.
                // Never replay additive grants or kick a replacement session.
                probe.commit();
            }else{
            if(PLAYER.contains(cmd.type())||"KICK_PLAYER".equals(cmd.type()))playerId=positive(cmd.payload(),"player_id");
            else if(accountCommand(cmd.type()))account=Math.toIntExact(positive(cmd.payload(),"account_id"));
            else if(LiveSyncConsignCommands.supports(cmd.type()))playerId=LiveSyncConsignCommands.sellerId(probe,cmd.payload());
            probe.rollback();
            }
            }
            // The configured production pool can have one connection. Release the
            // identity probe before opening execution or completion transactions.
            if("EVENT_SET".equals(descriptor.type())) {
                if(!hasReceipt)try(Connection c=connections.open()) {
                    c.setAutoCommit(false);
                    try {
                        var cmd=LiveSyncCommandDAO.lock(c,command);if(cmd==null){c.rollback();return;}
                        LiveSyncCommandDAO.authorize(c,cmd.actor());
                        if(!LiveSyncCommandDAO.receipt(c,command)) {
                            String result=nro.models.event.EventStateDAO.stage(c,cmd.payload());
                            LiveSyncCommandDAO.record(c,cmd,0,0,result);
                            LiveSyncCommandDAO.status(c,command,"processing","Đã lưu; đang áp dụng sự kiện");
                        }
                        c.commit();
                    }catch(Exception failure){c.rollback();throw failure;}
                }
                applyEvents();complete(command);return;
            }
            if(hasReceipt){
                if(LiveSyncCatalogCommands.supports(descriptor.type())&&!"SEND_NOTICE".equals(descriptor.type())){
                    // Gift consumption takes its monitor before borrowing SQL too.
                    // Never retain the identity lease while waiting for that monitor.
                    synchronized(GiftCodeManager.gI()){synchronized(LiveSyncCatalogCommands.LOCK){
                        LiveSyncCatalogCommands.Operation op;
                        try(Connection c=connections.open()){
                            c.setAutoCommit(false);op=LiveSyncCatalogCommands.restore(c,descriptor.type(),descriptor.payload());c.commit();
                        }
                        op.publish();
                    }}
                }
                complete(command);return;
            }
            if(GiftCodeService.hasPendingId(playerId)||GiftCodeService.hasPendingAccount(account)||LiveSyncCommandGate.isRetiringId(playerId)||LiveSyncCommandGate.isRetiringAccount(account)||LiveSyncCommandGate.hasPendingId(playerId)||LiveSyncCommandGate.hasPendingAccount(account))return;
            Player online=playerId>0?Client.gI().getPlayer(playerId):account>0?Client.gI().getPlayerByUser(account):null;
            if(online!=null){synchronized(online){if(!AdminPlayerActions.isOnline(online)||AdminPlayerActions.hasPending(online)||online.tradeWVP||nro.models.services_func.TransactionService.isTrading(online))return;executeLocked(command,online);}}
            else executeLocked(command,null);
            Pending p=unresolved.get(command);if(p!=null&&p.applied)finish(p);
        }catch(IllegalArgumentException invalid){fail(command,invalid.getMessage());}
        catch(Exception transientFailure){System.err.println("[LiveSync] Retrying #"+command+": "+transientFailure.getClass().getSimpleName());}
    }}
    private void applyEvents()throws Exception {
        try {
            nro.models.event.EventStateDAO.load();
            nro.models.event.EventCleanupService.drainOnline();
            nro.models.event.EventStateDAO.applied();
        }catch(Exception failure){nro.models.event.EventStateDAO.error(failure.getClass().getSimpleName()+": "+failure.getMessage());throw failure;}
    }
    private void executeLocked(long id,Player online)throws Exception {
        synchronized(GiftCodeManager.gI()){synchronized(LiveSyncCatalogCommands.LOCK){synchronized(ConsignShopManager.gI()){
            Pending ready=null;
            try(Connection c=connections.open()){
                c.setAutoCommit(false);Pending pending=null;boolean commitAttempted=false;
                try{
                    var cmd=LiveSyncCommandDAO.lock(c,id);if(cmd==null){c.rollback();return;}if(LiveSyncCommandDAO.receipt(c,id)){c.rollback();return;}
                    LiveSyncCommandDAO.authorize(c,cmd.actor());
                    long playerId=online==null?0:online.id;int account=online==null?0:online.session.userId;
                    Runnable publication=()->{};MySession kick=null;String result="Applied "+cmd.type();
                    if(PLAYER.contains(cmd.type())||LiveSyncConsignCommands.supports(cmd.type())){
                        if(playerId==0)playerId=LiveSyncConsignCommands.supports(cmd.type())?LiveSyncConsignCommands.sellerId(c,cmd.payload()):positive(cmd.payload(),"player_id");
                        var before=LiveSyncCommandDAO.player(c,playerId);account=Integer.parseInt(before.get("account_id"));
                        if(GiftCodeService.hasPendingId(playerId)||GiftCodeService.hasPendingAccount(account)||LiveSyncCommandGate.hasPendingAccount(account)||LiveSyncCommandGate.isRetiringAccount(account)||nro.models.shop_ky_gui.ConsignTransactions.hasPendingId(playerId)||RubyGiftService.hasPendingId(playerId)||WalletPurchaseService.hasPendingAccount(account)||AdminPlayerActions.hasRenamePendingId(playerId)||KolQuestService.hasProgressPendingId(playerId))throw new SQLException("Player is logging out or reconciling");
                        if(online!=null)before.putAll(LiveSyncPlayerRuntime.snapshot(online));
                        if(LiveSyncConsignCommands.supports(cmd.type())){
                            var op=LiveSyncConsignCommands.stage(c,cmd.payload(),before,online);publication=op::publish;result=op.result();
                        }else{
                            var changes=LiveSyncPlayerPatch.patch(before,cmd.type(),cmd.payload(),Integer.parseInt(before.get("gender")));
                            LiveSyncCommandDAO.updatePlayer(c,playerId,account,changes);if(online!=null)publication=()->LiveSyncPlayerRuntime.publish(online,changes);
                        }
                        if(Boolean.TRUE.equals(cmd.payload().get("kick_player"))&&online!=null)kick=online.session;
                    }else if(accountCommand(cmd.type())){
                        account=Math.toIntExact(positive(cmd.payload(),"account_id"));
                        if(GiftCodeService.hasPendingAccount(account)||WalletPurchaseService.hasPendingAccount(account)||RubyGiftService.hasPendingAccount(account)||nro.models.shop_ky_gui.ConsignTransactions.hasPendingAccount(account)||KolQuestService.hasProgressPendingAccount(account))throw new SQLException("Account transaction is reconciling");
                        publication=account(c,cmd,account,online);
                        if(online!=null&&"ACCOUNT_ACTION".equals(cmd.type())&&Set.of("ban","password").contains(String.valueOf(cmd.payload().get("action"))))kick=online.session;
                    }else if("KICK_PLAYER".equals(cmd.type())){
                        playerId=positive(cmd.payload(),"player_id");var before=LiveSyncCommandDAO.player(c,playerId);account=Integer.parseInt(before.get("account_id"));if(online!=null)kick=online.session;result=online==null?"Player already offline":"Player session selected for kick";
                    }else{var op=LiveSyncCatalogCommands.stage(c,cmd.type(),cmd.payload());publication=op::publish;result=op.result();}
                    pending=new Pending(id,playerId,account,online,publication,kick);LiveSyncCommandGate.mark(playerId,account);unresolved.put(id,pending);
                    LiveSyncCommandDAO.record(c,cmd,playerId,account,result);LiveSyncCommandDAO.status(c,id,"processing","Committed resources; applying runtime state");
                    commitAttempted=true;c.commit();ready=pending;
                }catch(Exception failure){
                    if(!commitAttempted){try{c.rollback();}catch(SQLException rollback){failure.addSuppressed(rollback);}if(pending!=null){unresolved.remove(id);LiveSyncCommandGate.clear(pending.playerId,pending.account);}throw failure;}
                    // An exception cannot prove COMMIT rolled back. Keep save/login gated.
                    System.err.println("[LiveSync] Resolving uncertain command #"+id);
                }
            }
            // Runtime refresh/warp may use SQL too. Commit and return this lease
            // before publishing, while retaining the player/cache monitors.
            if(ready!=null)publish(ready);
        }}}
    }
    private Runnable account(Connection c,LiveSyncCommandDAO.Command cmd,int account,Player online)throws Exception {
        Map<String,String> fields=new LinkedHashMap<>();
        try(PreparedStatement q=c.prepareStatement("SELECT vnd,thoi_vang,tongnap,active,is_admin,admin,ban FROM account WHERE id=? FOR UPDATE")){
            q.setInt(1,account);try(ResultSet r=q.executeQuery()){if(!r.next())throw new IllegalArgumentException("Account does not exist");for(String name:List.of("vnd","thoi_vang","tongnap","active","is_admin","admin","ban"))fields.put(name,r.getString(name));}
        }
        Map<String,String> changes=new LinkedHashMap<>();JSONObject p=cmd.payload();
        if("UPDATE_ACCOUNT".equals(cmd.type())){
            if(!Boolean.TRUE.equals(p.get("refresh_only")))for(String field:List.of("vnd","thoi_vang","tongnap")){
                String key="add_"+field;if(!p.containsKey(key))continue;long next;
                try{next=Math.addExact(Long.parseLong(fields.get(field)),Long.parseLong(String.valueOf(p.get(key))));}catch(RuntimeException e){throw new IllegalArgumentException("Account currency overflow",e);}
                if(next<0||next>Integer.MAX_VALUE)throw new IllegalArgumentException("Account currency outside allowed range");changes.put(field,String.valueOf(next));
            }
        }else{
            String field=switch(String.valueOf(p.get("action"))){case "ban"->"ban";case "active"->"active";case "admin"->"is_admin";case "password"->"password";default->throw new IllegalArgumentException("Unsupported account action");};
            String value=String.valueOf(p.get("value"));if("password".equals(field)){if(value.length()<4||value.length()>100)throw new IllegalArgumentException("Invalid password length");}else if(!Set.of("0","1").contains(value))throw new IllegalArgumentException("Expected boolean flag");changes.put(field,value);if("is_admin".equals(field))changes.put("admin",value);
        }
        if(!changes.isEmpty()){
            StringJoiner columns=new StringJoiner(",");for(String name:changes.keySet())columns.add(name+"=?");
            try(PreparedStatement q=c.prepareStatement("UPDATE account SET "+columns+" WHERE id=?")){int index=1;for(String value:changes.values())q.setString(index++,value);q.setInt(index,account);if(q.executeUpdate()!=1)throw new SQLException("Account update failed");}
        }
        fields.putAll(changes);MySession session=online==null?null:online.session;
        return ()->{if(session!=null){session.vnd=Integer.parseInt(fields.get("vnd"));session.goldBar=Integer.parseInt(fields.get("thoi_vang"));session.tongnap=Integer.parseInt(fields.get("tongnap"));session.actived="1".equals(fields.get("active"));session.isAdmin="1".equals(fields.get("is_admin"))||"1".equals(fields.get("admin"));}};
    }
    private void publish(Pending p){if(p.applied)return;if(p.player!=null){synchronized(p.player){p.publish.run();p.applied=true;}}else{p.publish.run();p.applied=true;}}
    private void finish(Pending p)throws SQLException {
        if(!p.finalized){
            if(p.player!=null){synchronized(p.player){
                if(p.player.beforeDispose){
                    if(!p.player.liveLogoutCleanupCompleted)throw new SQLException("Logout cleanup still running");
                    if(!PlayerDAO.saveLiveCommandLogout(p.player))throw new SQLException("Pending logout save failed");
                    LiveSyncCommandGate.clear(p.playerId,p.account);p.player.dispose();p.player.liveLogoutCompleted=true;LiveSyncCommandGate.retired(p.playerId,p.account);
                }else LiveSyncCommandGate.clear(p.playerId,p.account);
                p.finalized=true;
            }}else{LiveSyncCommandGate.clear(p.playerId,p.account);p.finalized=true;}
            // Never kick while holding one player's monitor: logout can lock both trade parties.
            if(p.kick!=null&&p.kick.player==p.player&&p.player!=null&&!p.player.beforeDispose)Client.gI().kickSession(p.kick);
        }
        complete(p.command);unresolved.remove(p.command);
    }
    private void recover(Pending p)throws Exception {synchronized(LiveSyncCommandGate.LIFECYCLE_LOCK){
        if(!p.finalized&&p.player!=null&&p.player.beforeDispose&&!p.player.liveLogoutCleanupCompleted)throw new SQLException("Logout cleanup still running");
        boolean committed;
        try(Connection c=connections.open()){
            c.setAutoCommit(false);try(PreparedStatement q=c.prepareStatement("SELECT id FROM server_command_queue WHERE id=? FOR UPDATE")){q.setLong(1,p.command);try(ResultSet r=q.executeQuery()){if(!r.next())throw new SQLException("Command disappeared");}}
            committed=LiveSyncCommandDAO.receipt(c,p.command);c.rollback();
        }
        if(committed){if(p.player!=null){synchronized(p.player){
            if(!p.finalized&&p.player.beforeDispose&&!p.player.liveLogoutCleanupCompleted)throw new SQLException("Logout cleanup still running");
            publishRecovered(p);
        }}else publishRecovered(p);finish(p);}
        else{
            if(p.player!=null){synchronized(p.player){
                if(p.player.beforeDispose){
                    if(!p.player.liveLogoutCleanupCompleted)throw new SQLException("Logout cleanup still running");
                    if(!PlayerDAO.saveLiveCommandLogout(p.player))throw new SQLException("Rolled-back logout save failed");
                    LiveSyncCommandGate.clear(p.playerId,p.account);p.player.dispose();p.player.liveLogoutCompleted=true;LiveSyncCommandGate.retired(p.playerId,p.account);
                }else LiveSyncCommandGate.clear(p.playerId,p.account);
            }}else LiveSyncCommandGate.clear(p.playerId,p.account);
            unresolved.remove(p.command);
        }
    }}
    private void publishRecovered(Pending p){synchronized(GiftCodeManager.gI()){synchronized(LiveSyncCatalogCommands.LOCK){synchronized(ConsignShopManager.gI()){publish(p);}}}}
    private void complete(long id)throws SQLException {try(Connection c=connections.open();PreparedStatement q=c.prepareStatement("UPDATE server_command_queue q JOIN admin_command_receipt r ON r.command_id=q.id SET q.status='completed',q.result_message=LEFT(r.result_message,250),q.executed_at=CURRENT_TIMESTAMP WHERE q.id=?")){q.setLong(1,id);q.executeUpdate();}}
    private void fail(long id,String reason){try(Connection c=connections.open()){c.setAutoCommit(false);try(PreparedStatement q=c.prepareStatement("SELECT id FROM server_command_queue WHERE id=? FOR UPDATE")){q.setLong(1,id);try(ResultSet r=q.executeQuery()){if(r.next()&&!LiveSyncCommandDAO.receipt(c,id))LiveSyncCommandDAO.status(c,id,"failed",reason);}}c.commit();}catch(Exception e){System.err.println("[LiveSync] Could not record failure #"+id);}}
}
