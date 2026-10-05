package nro.models.database;
import java.sql.*;
import nro.models.data.LocalManager;
public class KolRewardDAO  {
    @FunctionalInterface public interface Connections  {
        Connection open() throws SQLException;
    }
    public static class CommitUncertain extends SQLException {
        public CommitUncertain(SQLException cause) { super("KOL commit outcome pending reconciliation",cause); }
    }
    public void saveProgress(long player, int account, String json) throws SQLException {
        boolean committing = false;
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                try (PreparedStatement q = c.prepareStatement("UPDATE player SET nhiem_vu_kol=? WHERE id=? AND account_id=?")) {
                    q.setString(1,json); q.setLong(2,player); q.setInt(3,account);
                    if (q.executeUpdate()!=1) throw new SQLException("KOL progress ownership/rowcount");
                }
                committing = true;
                c.commit();
            } catch (SQLException e) {
                if (committing) throw new CommitUncertain(e);
                try { c.rollback(); } catch (SQLException ignored) {}
                throw e;
            }
        } catch (SQLException e) {
            // A connection-close failure after commit also needs reconciliation.
            if (committing && !(e instanceof CommitUncertain)) throw new CommitUncertain(e);
            throw e;
        }
    }

    public String readProgress(long player, int account) throws SQLException {
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try (PreparedStatement q = c.prepareStatement("SELECT nhiem_vu_kol FROM player WHERE id=? AND account_id=? FOR UPDATE")) {
                q.setLong(1,player); q.setInt(2,account);
                try (ResultSet r = q.executeQuery()) {
                    if (!r.next()) throw new SQLException("KOL progress ownership");
                    String json = r.getString(1); c.rollback(); return json;
                }
            }
        }
    }

    public Balance reconcile(long player,int account,boolean vip,int quest) throws SQLException {
        try(Connection c=connections.open()) {
            c.setAutoCommit(false);
            try {
                // Wait for any original account transaction to settle before checking absence.
                try(PreparedStatement q=c.prepareStatement("SELECT id FROM account WHERE id=? FOR UPDATE")) {
                    q.setInt(1,account);try(ResultSet r=q.executeQuery()){if(!r.next())throw new SQLException("account missing");}
                }
                Balance result=existing(c,player,account,vip,quest);
                c.rollback();return result;
            } catch(SQLException e) { try { c.rollback(); } catch(SQLException ignored) {} throw e; }
        }
    }
    private final Connections connections;
    public KolRewardDAO() {
        this(LocalManager::getConnection);
    }
    public KolRewardDAO(Connections connections) {
        this.connections=connections;
    }
    public record Balance(int vnd,int points,String json,boolean newlyClaimed) {
    }
    private Balance existing(Connection c,long player,int account,boolean vip,int quest) throws SQLException  {
        try(PreparedStatement q=c.prepareStatement("SELECT a.vnd,p.event_point,p.nhiem_vu_kol FROM kol_reward_claim k JOIN player p ON p.id=k.player_id JOIN account a ON a.id=p.account_id WHERE k.player_id=? AND k.quest_version=2 AND k.branch=? AND k.quest_id=? AND k.account_id=? AND p.account_id=?")) {
            q.setLong(1,player);
            q.setInt(2,vip?1:0);
            q.setInt(3,quest);
            q.setInt(4,account);
            q.setInt(5,account);
            try(ResultSet r=q.executeQuery()) {
                return r.next()?new Balance(r.getInt(1),r.getInt(2),r.getString(3),false):null;
            }
        }
    }
    public Balance claim(long player,int account,boolean vip,int quest,int vndReward,int pointReward,int livePoints,String json) throws SQLException  {
        boolean committing=false;
        try(Connection c=connections.open()) {
            c.setAutoCommit(false);
            try  {
                int wallet;
                try(PreparedStatement q=c.prepareStatement("SELECT vnd FROM account WHERE id=? FOR UPDATE")) {
                    q.setInt(1,account);
                    try(ResultSet r=q.executeQuery()) {
                        if(!r.next())throw new SQLException("account missing");
                        wallet=r.getInt(1);
                    }
                }
                int storedPoints;
                String storedJson;
                try(PreparedStatement q=c.prepareStatement("SELECT account_id,event_point,nhiem_vu_kol FROM player WHERE id=? FOR UPDATE")) {
                    q.setLong(1,player);
                    try(ResultSet r=q.executeQuery()) {
                        if(!r.next()||r.getInt(1)!=account)throw new SQLException("player ownership");
                        storedPoints=r.getInt(2);
                        storedJson=r.getString(3);
                    }
                }
                Balance old=existing(c,player,account,vip,quest);
                if(old!=null) {
                    c.rollback();
                    return old;
                }
                var storedState=nro.models.player.KOLProgressData.parse(storedJson).v2;
                if(!storedState.valid() || (vip?storedState.vipStage:storedState.normalStage)!=quest)throw new SQLException("persisted quest stage");
                long newWallet=(long)wallet+vndReward,newPoints=(long)livePoints+pointReward;
                if(wallet<0||storedPoints<0||livePoints<0||vndReward<0||pointReward<0||newWallet>Integer.MAX_VALUE||newPoints>Integer.MAX_VALUE)throw new SQLException("reward overflow");
                try(PreparedStatement q=c.prepareStatement("INSERT INTO kol_reward_claim(player_id,quest_version,branch,quest_id,account_id,reward_vnd,reward_points) VALUES(?,2,?,?,?,?,?)")) {
                    q.setLong(1,player);
                    q.setInt(2,vip?1:0);
                    q.setInt(3,quest);
                    q.setInt(4,account);
                    q.setInt(5,vndReward);
                    q.setInt(6,pointReward);
                    if(q.executeUpdate()!=1)throw new SQLException("ledger row");
                }
                try(PreparedStatement q=c.prepareStatement("UPDATE account SET vnd=vnd+? WHERE id=?")) {
                    q.setInt(1,vndReward);
                    q.setInt(2,account);
                    if(q.executeUpdate()!=1)throw new SQLException("account row");
                }
                try(PreparedStatement q=c.prepareStatement("UPDATE player SET event_point=?,nhiem_vu_kol=? WHERE id=? AND account_id=?")) {
                    q.setInt(1,(int)newPoints);
                    q.setString(2,json);
                    q.setLong(3,player);
                    q.setInt(4,account);
                    if(q.executeUpdate()!=1)throw new SQLException("player row");
                }
                committing=true;
                c.commit();
                return new Balance((int)newWallet,(int)newPoints,json,true);
            }
            catch(SQLException e) {
                try {
                    c.rollback();
                }
                catch(SQLException ignored) {
                }
                throw e;
            }
        }
        catch(SQLException e) {
            if(committing) {
                try {
                    Balance found=reconcile(player,account,vip,quest);
                    if(found!=null)return found;
                }
                catch(SQLException check) {
                    e.addSuppressed(check);
                    throw new CommitUncertain(e);
                }
            }
            throw e;
        }
    }
    public record WalletDebit(boolean committed,int vnd,boolean insufficient) {
        public WalletDebit(boolean committed,int vnd) { this(committed,vnd,false); }
    }

    private WalletDebit walletReceipt(Connection connection,int account,int cost,String kind,String receiptId,int wallet) throws SQLException {
        try(PreparedStatement query=connection.prepareStatement("SELECT account_id,cost,debit_kind FROM wallet_debit_receipt WHERE receipt_id=?")) {
            query.setString(1,receiptId);
            try(ResultSet result=query.executeQuery()) {
                if(!result.next())return new WalletDebit(false,wallet);
                if(result.getInt(1)!=account || result.getInt(2)!=cost || !kind.equals(result.getString(3))) {
                    throw new SQLException("wallet receipt ownership or purchase mismatch");
                }
                return new WalletDebit(true,wallet);
            }
        }
    }

    private int lockWallet(Connection connection,int account) throws SQLException {
        try(PreparedStatement query=connection.prepareStatement("SELECT vnd FROM account WHERE id=? FOR UPDATE")) {
            query.setInt(1,account);
            try(ResultSet result=query.executeQuery()) {
                if(!result.next())throw new SQLException("account missing");
                int wallet=result.getInt(1);
                if(wallet<0)throw new SQLException("invalid wallet");
                return wallet;
            }
        }
    }

    public WalletDebit reconcileDebit(int account,int cost,String kind,String receiptId) throws SQLException {
        try(Connection connection=connections.open()) {
            connection.setAutoCommit(false);
            try {
                int wallet=lockWallet(connection,account);
                WalletDebit result=walletReceipt(connection,account,cost,kind,receiptId,wallet);
                connection.rollback();
                return result;
            } catch(SQLException failure) {
                try { connection.rollback(); } catch(SQLException ignored) {}
                throw failure;
            }
        }
    }

    public WalletDebit debit(int account,int cost,String kind,String receiptId,Integer active) throws SQLException {
        if(cost<=0 || kind==null || kind.length()>32 || kind.isBlank())throw new SQLException("invalid debit");
        try { if(receiptId==null || receiptId.length()!=36 || !java.util.UUID.fromString(receiptId).toString().equalsIgnoreCase(receiptId))throw new IllegalArgumentException("UUID format"); } catch(Exception malformed) { throw new SQLException("invalid receipt UUID",malformed); }
        boolean committing=false;
        try(Connection connection=connections.open()) {
            connection.setAutoCommit(false);
            try {
                int wallet=lockWallet(connection,account);
                WalletDebit previous=walletReceipt(connection,account,cost,kind,receiptId,wallet);
                if(previous.committed()) { connection.rollback();return previous; }
                if(wallet<cost) { connection.rollback();return new WalletDebit(false,wallet,true); }
                try(PreparedStatement query=connection.prepareStatement("INSERT INTO wallet_debit_receipt(receipt_id,account_id,debit_kind,cost) VALUES(?,?,?,?)")) {
                    query.setString(1,receiptId);
                    query.setInt(2,account);
                    query.setString(3,kind);
                    query.setInt(4,cost);
                    if(query.executeUpdate()!=1)throw new SQLException("wallet receipt row");
                }
                String sql=active==null?"UPDATE account SET vnd=vnd-? WHERE id=? AND vnd>=?":"UPDATE account SET vnd=vnd-?,active="+active+" WHERE id=? AND vnd>=?";
                try(PreparedStatement query=connection.prepareStatement(sql)) {
                    query.setInt(1,cost);
                    query.setInt(2,account);
                    query.setInt(3,cost);
                    if(query.executeUpdate()!=1)throw new SQLException("wallet debit row");
                }
                committing=true;
                connection.commit();
                return new WalletDebit(true,wallet-cost);
            } catch(SQLException failure) {
                try { connection.rollback(); } catch(SQLException ignored) {}
                throw failure;
            }
        } catch(SQLException failure) {
            if(committing) {
                try { return reconcileDebit(account,cost,kind,receiptId); }
                catch(SQLException recoveryFailure) {
                    failure.addSuppressed(recoveryFailure);
                    throw new CommitUncertain(failure);
                }
            }
            throw failure;
        }
    }

    public int debit(int account,int cost) throws SQLException {
        WalletDebit result=debit(account,cost,"legacy_debit",java.util.UUID.randomUUID().toString(),null);
        return result.committed()?result.vnd():-1;
    }

    public int activate(int account,int cost,boolean active) throws SQLException {
        WalletDebit result=debit(account,cost,"membership",java.util.UUID.randomUUID().toString(),active?1:0);
        return result.committed()?result.vnd():-1;
    }
}
