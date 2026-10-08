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

    private Client() {
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

    private void remove(MySession session) {
        if (session.player != null) {
            Player retiring=session.player;
            this.remove(retiring);
            retiring.dispose();
            if(!nro.models.event.EventCleanupLogoutService.pending(retiring.id)&&!nro.models.services.LiveSyncCommandGate.hasPendingId(retiring.id)&&!nro.models.services.GiftCodeService.hasPendingId(retiring.id))retiring.liveLogoutCompleted=true;
        }
        if (session.joinedGame) {
            session.joinedGame = false;
            try {
                LocalManager.executeUpdate("update account set last_time_logout = ? where id = ?", new Timestamp(System.currentTimeMillis()), session.userId);
            } catch (Exception e) {
                Logger.logException(Client.class, e);
            }
        }
        ServerManager.gI().disconnect(session);
    }

    private void remove(Player player) {
        boolean cleanup;
        synchronized (player) {
            nro.models.services.LiveSyncCommandGate.retire(player.id,player.getSession().userId);
            synchronized (registryLock) {
            this.players_id.remove(player.id, player);
            this.players_name.remove(player.name, player);
            this.players_userId.remove(player.getSession().userId, player);
            this.players.remove(player);
            cleanup = !player.beforeDispose;
            player.beforeDispose = true;
            }
        }
        if (cleanup) {
            player.mapIdBeforeLogout = player.zone.map.mapId;
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
        PlayerDAO.updatePlayer(player);
    }

    public void kickSession(MySession session) {
        if (session != null && session.claimLogout()) {
            Player retiring=session.player;
            try { this.remove(session); }
            finally {
                try { session.disconnect(); }
                finally {
                    if(retiring!=null) {
                        retiring.liveLogoutCleanupCompleted=true;
                        if(!nro.models.event.EventCleanupLogoutService.pending(retiring.id)&&!nro.models.services.LiveSyncCommandGate.hasPendingId(retiring.id)&&!nro.models.services.GiftCodeService.hasPendingId(retiring.id))
                            nro.models.services.LiveSyncCommandGate.retired(retiring.id,session.userId);
                    }
                }
            }
        }
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
        synchronized(nro.models.services.LiveSyncCommandGate.LIFECYCLE_LOCK) {
        for(Player pl:getPlayers()) {
            synchronized(pl) {
            if(pl.getSession()==null)throw new IllegalStateException("Registered player has no session; maintenance cannot verify save");
            TransactionService.gI().cancelTrade(pl);
            if(!PlayerDAO.saveChecked(pl))throw new IllegalStateException("Player full save failed; event cleanup must not start");
            this.kickSession(pl.getSession());
            }
        }
        }
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
