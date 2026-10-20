package nro.models.server;

import nro.models.data.LocalManager;
import nro.models.database.PlayerDAO;
import lombok.Getter;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.network.SessionManager;
import nro.models.interfaces.ISession;
import nro.models.network.MySession;
import nro.models.services.Service;
import nro.models.map.service.ChangeMapService;
import nro.models.services.shenron.SummonDragon;
import nro.models.services_func.TransactionService;
import nro.models.services_dungeon.NgocRongNamecService;
import nro.models.utils.Functions;
import nro.models.utils.Logger;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import nro.models.services.shenron.SummonDragonNamek;

/**
 *
 * @author By Mr Blue
 * 
 */

public class Client implements Runnable {

    private static Client instance;

    private final Map<Long, Player> players_id = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<Integer, Player> players_userId = new java.util.concurrent.ConcurrentHashMap<>();
    private final Map<String, Player> players_name = new java.util.concurrent.ConcurrentHashMap<>();
    @Getter
    private final List<Player> players = new java.util.concurrent.CopyOnWriteArrayList<>();

    private static final Map<MySession, LogoutRecord> UNSAVED_LOGOUTS = new java.util.concurrent.ConcurrentHashMap<>();
    private final LogoutActions logoutActions;

    interface LogoutActions {
        void cleanup(Player player) throws Exception;
        boolean savePlayer(Player player) throws Exception;
        boolean saveAccount(int account, Timestamp logoutTime) throws Exception;
        void dispose(Player player);
        void disconnect(MySession session);
    }

    private static final LogoutActions DEFAULT_LOGOUT_ACTIONS = new LogoutActions() {
        public void cleanup(Player player) { cleanupDetachedPlayer(player); }
        public boolean savePlayer(Player player) { return PlayerDAO.saveDetachedChecked(player); }
        public boolean saveAccount(int account, Timestamp logoutTime) throws Exception {
            return LocalManager.executeUpdate("update account set last_time_logout = ? where id = ?", logoutTime, account) == 1;
        }
        public void dispose(Player player) { player.dispose(); }
        public void disconnect(MySession session) {
            try { ServerManager.gI().disconnect(session); }
            finally { session.disconnect(); }
        }
    };

    private static final class LogoutRecord {
        final MySession session;
        final Player player;
        final int account;
        final Timestamp logoutTime = new Timestamp(System.currentTimeMillis());
        final LogoutActions actions;
        volatile boolean cleanupComplete;
        boolean playerSaved;
        boolean metadataSaved;
        LogoutRecord(MySession session, LogoutActions actions) {
            this.session = session; this.player = session.player; this.account = session.userId;
            this.actions = actions; this.playerSaved = player == null;
            this.metadataSaved = !session.joinedGame;
        }
    }

    Client(LogoutActions actions) { this.logoutActions = actions; }

    private Client() {
        this(DEFAULT_LOGOUT_ACTIONS);
        Executors.newSingleThreadExecutor().submit(this, "Update Client");
    }

    public static Client gI() {
        if (instance == null) {
            instance = new Client();
        }
        return instance;
    }

    private final Object registryLock = new Object();

    public void put(Player player) {
      synchronized (player) { synchronized (registryLock) {
        if (!players_id.containsKey(player.id)) {
            this.players_id.put(player.id, player);
        }
        if (!players_name.containsValue(player)) {
            this.players_name.put(player.name, player);
        }
        if (!players_userId.containsValue(player)) {
            this.players_userId.put(player.getSession().userId, player);
        }
        if (!players.contains(player)) {
            this.players.add(player);
        }
      }}
    }

    private void detach(Player player, int account) {
        synchronized (player) {
            nro.models.services.LiveSyncCommandGate.retire(player.id, account);
            synchronized (registryLock) {
                this.players_id.remove(player.id, player);
                this.players_name.remove(player.name, player);
                this.players_userId.remove(account, player);
                this.players.remove(player);
                player.beforeDispose = true;
            }
        }
    }

    private static void cleanupDetachedPlayer(Player player) {
        {
            if (player.zone != null) player.mapIdBeforeLogout = player.zone.map.mapId;
            if (player.idNRNM != -1) {
                ItemMap itemMap = new ItemMap(player.zone, player.idNRNM, 1, player.location.x, player.location.y, -1);
                Service.gI().dropItemMap(player.zone, itemMap);
                NgocRongNamecService.gI().pNrNamec[player.idNRNM - 353] = "";
                NgocRongNamecService.gI().idpNrNamec[player.idNRNM - 353] = -1;
                player.idNRNM = -1;
            }
            ChangeMapService.gI().exitMap(player);
            TransactionService.gI().cancelTrade(player);
            if (player.clan != null) {
                player.clan.removeMemberOnline(null, player);
            }
            if (SummonDragon.gI().playerSummonShenron != null
                    && SummonDragon.gI().playerSummonShenron.id == player.id) {
                SummonDragon.gI().isPlayerDisconnect = true;
            }
            if (SummonDragonNamek.gI().playerSummonShenron != null
                    && SummonDragonNamek.gI().playerSummonShenron.id == player.id) {
                SummonDragonNamek.gI().isPlayerDisconnect = true;
            }
            if (player.shenronEvent != null) {
                player.shenronEvent.isPlayerDisconnect = true;
            }
            if (player.mobMe != null) {
                player.mobMe.mobMeDie();
            }
            if (player.pet != null) {
                if (player.pet.mobMe != null) {
                    player.pet.mobMe.mobMeDie();
                }
                ChangeMapService.gI().exitMap(player.pet);
            }
        }
        nro.models.event.EventCleanupLogoutService.retainIfPending(player);
    }

    public void kickSession(MySession session) {
        if (session == null || !session.claimLogout()) return;
        LogoutRecord record = new LogoutRecord(session, logoutActions);
        // Publish the reservation before cleanup or pending-operation recovery can run.
        UNSAVED_LOGOUTS.put(session, record);
        try {
            if (record.player != null) {
                detach(record.player, record.account);
                // Trade cancellation may acquire two players; never hold either here.
                record.actions.cleanup(record.player);
                record.player.liveLogoutCleanupCompleted = true;
            }
            record.cleanupComplete = true;
            completeLogout(record);
        } catch (Exception failure) {
            Logger.logException(Client.class, failure);
        } finally {
            try { record.actions.disconnect(session); }
            finally {
                if (record.player != null && UNSAVED_LOGOUTS.containsKey(session))
                    nro.models.services.LiveSyncCommandGate.retire(record.player.id, record.account);
            }
        }
    }

    private void completeLogout(LogoutRecord record) throws Exception {
        synchronized (record) {
            if (!record.cleanupComplete || UNSAVED_LOGOUTS.get(record.session) != record) return;
            if (!record.playerSaved) {
                synchronized (record.player) {
                    record.playerSaved = record.actions.savePlayer(record.player);
                }
            }
            if (!record.playerSaved) return;
            if (!record.metadataSaved) record.metadataSaved = record.actions.saveAccount(record.account, record.logoutTime);
            if (!record.metadataSaved) return;
            if (record.player != null) {
                synchronized (record.player) {
                    record.actions.dispose(record.player);
                    record.player.liveLogoutCompleted = true;
                }
            }
            record.session.joinedGame = false;
            UNSAVED_LOGOUTS.remove(record.session, record);
            if (record.player != null) nro.models.services.LiveSyncCommandGate.retired(record.player.id, record.account);
        }
    }

    public void retryUnsavedLogouts() {
        for (LogoutRecord record : new ArrayList<>(UNSAVED_LOGOUTS.values())) {
            try { completeLogout(record); }
            catch (Exception failure) { Logger.logException(Client.class, failure); }
        }
    }

    public boolean hasUnsavedLogouts() { return !UNSAVED_LOGOUTS.isEmpty(); }
    public int getUnsavedLogoutCount() { return UNSAVED_LOGOUTS.size(); }
    public static boolean hasUnsavedLogoutForAccount(int account) {
        if (account <= 0) return false;
        for (LogoutRecord record : UNSAVED_LOGOUTS.values()) if (record.account == account) return true;
        return false;
    }
    public static boolean hasUnsavedPlayerSnapshot(Player player) {
        for (LogoutRecord record : UNSAVED_LOGOUTS.values()) if (record.player == player) return true;
        return false;
    }

    /** Admission/mutation must already be closed by the maintenance coordinator. */
    public void drainForWebMaintenance() {
        for (Player player : new ArrayList<>(players)) {
            MySession session = player.getSession();
            if (session == null) throw new IllegalStateException("Registered player has no session");
            kickSession(session);
        }
        for (ISession session : new ArrayList<>(SessionManager.gI().getSessions()))
            if (session instanceof MySession) kickSession((MySession) session);
        retryUnsavedLogouts();
        if (!players.isEmpty() || hasUnsavedLogouts())
            throw new IllegalStateException("Logout saves/metadata remain incomplete: " + getUnsavedLogoutCount());
    }

    @FunctionalInterface
    public interface RenameWrite { boolean write() throws Exception; }
    /** Caller holds player; registry order matches logout. DB must succeed before memory. */
    public boolean renameOnline(Player player, String name, RenameWrite write) throws Exception {
        synchronized (player) { synchronized (registryLock) {
            if (player.beforeDispose || player.session == null || player.session.player != player
                    || players_id.get(player.id) != player || players_name.get(player.name) != player
                    || players_userId.get(player.session.userId) != player) return false;
            Player occupied = players_name.get(name);
            if (occupied != null && occupied != player) return false;
            if (!write.write()) return false;
            players_name.remove(player.name, player);
            player.name = name;
            players_name.put(name, player);
            return true;
        }}
    }

    public Player getPlayer(long playerId) {
        return this.players_id.get(playerId);
    }

    public Player getPlayerByUser(int userId) {
        return this.players_userId.get(userId);
    }

    public Player getPlayer(String name) {
        return this.players_name.get(name);
    }

    public void close() {
        Logger.log(Logger.YELLOW, "BEGIN KICK OUT SESSION " + players.size() + "\n");
        drainForWebMaintenance();
        Logger.success("SUCCESSFUL\n");
    }

    private void update() {
        for (int i = SessionManager.gI().getSessions().size() - 1; i >= 0; i--) {
            ISession s = SessionManager.gI().getSessions().get(i);
            MySession session = (MySession) s;
            if (session == null) {
                SessionManager.gI().getSessions().remove(i);
                continue;
            }
            if (session.timeWait > 0) {
                session.timeWait--;
                if (session.timeWait == 0) {
                    kickSession(session);
                }
            }
        }
    }

    public Player getPlayerByID(int playerId) {
        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (player != null && player.id == playerId) {
                return player;
            }
        }
        return null;
    }

    @Override
    public void run() {
        while (ServerManager.isRunning) {
            long st = System.currentTimeMillis();
            try {
                update();
            } catch (Exception e) {
                e.printStackTrace();
            }
            Functions.sleep(Math.max(1000 - (System.currentTimeMillis() - st), 10));
        }
    }

}
