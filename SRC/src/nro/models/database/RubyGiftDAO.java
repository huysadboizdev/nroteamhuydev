package nro.models.database;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import nro.models.data.LocalManager;

/** Both inventory snapshots and the unique receipt commit in one InnoDB transaction. */
public class RubyGiftDAO {
    @FunctionalInterface public interface Connections {
        Connection open()throws SQLException;
    }
    public static final String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS ruby_gift_receipt ("
            + "request_id CHAR(36) CHARACTER SET ascii COLLATE ascii_bin PRIMARY KEY,"
            + "sender_id BIGINT NOT NULL,receiver_id BIGINT NOT NULL,"
            + "sender_account INT NOT NULL,receiver_account INT NOT NULL,"
            + "amount INT NOT NULL,fee INT NOT NULL,net INT NOT NULL,"
            + "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
            + "KEY sender_created(sender_id,created_at),"
            + "KEY receiver_created(receiver_id,created_at)) ENGINE=InnoDB";
    public static final class CommitUncertain extends SQLException {
        public CommitUncertain(SQLException cause) {
            super("Ruby gift commit requires reconciliation",cause);
        }
    }
    public record Snapshot(long id,int account,String inventory,String bag,
            String body,String box,String luckyBox,String soldItems) {
        /** Legacy callers can omit containers, which the UPDATE then leaves unchanged. */
        public Snapshot(long id,int account,String inventory,String bag) {
            this(id,account,inventory,bag,null,null,null,null);
        }
    }
    public record Receipt(long sender,long receiver,int senderAccount,int receiverAccount,int amount,int fee,int net) {
    }
    private final Connections connections;
    public RubyGiftDAO() {
        this(LocalManager::getConnection);
    }
    public RubyGiftDAO(Connections connections) {
        this.connections=connections;
    }
    private String lock(Connection c,Snapshot p)throws SQLException {
        try(PreparedStatement q=c.prepareStatement("SELECT account_id,data_inventory FROM player WHERE id=? FOR UPDATE")) {
            q.setLong(1,p.id());
            try(ResultSet r=q.executeQuery()) {
                if (!r.next()||r.getInt(1)!=p.account())throw new SQLException("player ownership changed");
                return r.getString(2);
            }
        }
    }
    private Receipt existing(Connection c,String token,Snapshot sender,Snapshot receiver,int amount)throws SQLException {
        try(PreparedStatement q=c.prepareStatement("SELECT sender_id,receiver_id,sender_account,receiver_account,amount,fee,net FROM ruby_gift_receipt WHERE request_id=?")) {
            q.setString(1,token);
            try(ResultSet r=q.executeQuery()) {
                if (!r.next())return null;
                Receipt old=new Receipt(r.getLong(1),r.getLong(2),r.getInt(3),r.getInt(4),r.getInt(5),r.getInt(6),r.getInt(7));
                int fee=(int)((amount+9L)/10);
                if (old.sender()!=sender.id()||old.receiver()!=receiver.id()||old.senderAccount()!=sender.account()||old.receiverAccount()!=receiver.account()||old.amount()!=amount||old.fee()!=fee||old.net()!=amount-fee)throw new SQLException("receipt parameters mismatch");
                return old;
            }
        }
    }
    public Receipt reconcile(String token,Snapshot sender,Snapshot receiver,int amount)throws SQLException {
        try(Connection c=connections.open()) {
            c.setAutoCommit(false);
            try {
                if (sender.id()<receiver.id()) {
                    lock(c,sender);
                    lock(c,receiver);
                }
                else {
                    lock(c,receiver);
                    lock(c,sender);
                }
                Receipt r=existing(c,token,sender,receiver,amount);
                c.rollback();
                return r;
            }
            catch (SQLException e) {
                try {
                    c.rollback();
                }
                catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }
    private String mergedInventory(String stored,String live)throws SQLException {
        try {
            JsonArray old=new Gson().fromJson(stored,JsonArray.class),next=new Gson().fromJson(live,JsonArray.class);
            if (old.size()<3||next.size()!=5)throw new SQLException("inventory shape");
            for (int i=0;i<5;i++) {
                if (i<old.size())old.set(i,next.get(i));
                else old.add(next.get(i));
            }
            return old.toString();
        }
        catch (RuntimeException invalid) {
            throw new SQLException("invalid inventory JSON",invalid);
        }
    }
    private void update(Connection c,Snapshot p,String stored)throws SQLException {
        StringBuilder sql = new StringBuilder("UPDATE player SET data_inventory=?,items_bag=?");
        if (p.body() != null) sql.append(",items_body=?");
        if (p.box() != null) sql.append(",items_box=?");
        if (p.luckyBox() != null) sql.append(",items_box_lucky_round=?");
        if (p.soldItems() != null) sql.append(",items_daban=?");
        sql.append(" WHERE id=? AND account_id=?");
        try(PreparedStatement q=c.prepareStatement(sql.toString())) {
            q.setString(1,mergedInventory(stored,p.inventory()));
            q.setString(2,p.bag());
            int index = 3;
            if (p.body() != null) q.setString(index++,p.body());
            if (p.box() != null) q.setString(index++,p.box());
            if (p.luckyBox() != null) q.setString(index++,p.luckyBox());
            if (p.soldItems() != null) q.setString(index++,p.soldItems());
            q.setLong(index++,p.id());
            q.setInt(index,p.account());
            if (q.executeUpdate()!=1)throw new SQLException("player update missing");
        }
    }

    /** Idempotent restoration after a locked receipt lookup confirmed no gift committed. */
    public Receipt persistUncommitted(String token, Snapshot sender, Snapshot receiver, int amount)
            throws SQLException {
        try (Connection connection = connections.open()) {
            connection.setAutoCommit(false);
            try {
                String senderStored, receiverStored;
                if (sender.id() < receiver.id()) {
                    senderStored = lock(connection, sender);
                    receiverStored = lock(connection, receiver);
                } else {
                    receiverStored = lock(connection, receiver);
                    senderStored = lock(connection, sender);
                }
                Receipt receipt = existing(connection, token, sender, receiver, amount);
                if (receipt != null) {
                    connection.rollback();
                    return receipt;
                }
                update(connection, sender, senderStored);
                update(connection, receiver, receiverStored);
                connection.commit();
                return null;
            } catch (SQLException failure) {
                try { connection.rollback(); } catch (SQLException ignored) {}
                // The service keeps both accounts gated and retries these same snapshots.
                throw failure;
            }
        }
    }

    public Receipt transfer(String token,Snapshot sender,Snapshot receiver,int amount)throws SQLException {
        try {
            if (token==null||!UUID.fromString(token).toString().equals(token))throw new IllegalArgumentException();
        }
        catch (IllegalArgumentException e) {
            throw new SQLException("request UUID invalid",e);
        }
        int fee=(int)((amount+9L)/10),net=amount-fee;
        if (amount<=0||net<=0||sender.id()==receiver.id())throw new SQLException("invalid gift");
        boolean committing=false;
        try(Connection c=connections.open()) {
            c.setAutoCommit(false);
            try {
                String a,b;
                if (sender.id()<receiver.id()) {
                    a=lock(c,sender);
                    b=lock(c,receiver);
                }
                else {
                    b=lock(c,receiver);
                    a=lock(c,sender);
                }
                Receipt prior=existing(c,token,sender,receiver,amount);
                if (prior!=null) {
                    c.rollback();
                    return prior;
                }
                try(PreparedStatement q=c.prepareStatement("INSERT INTO ruby_gift_receipt(request_id,sender_id,receiver_id,sender_account,receiver_account,amount,fee,net) VALUES(?,?,?,?,?,?,?,?)")) {
                    q.setString(1,token);
                    q.setLong(2,sender.id());
                    q.setLong(3,receiver.id());
                    q.setInt(4,sender.account());
                    q.setInt(5,receiver.account());
                    q.setInt(6,amount);
                    q.setInt(7,fee);
                    q.setInt(8,net);
                    if (q.executeUpdate()!=1)throw new SQLException("receipt insert failed");
                }
                update(c,sender,a);
                update(c,receiver,b);
                committing=true;
                c.commit();
                return new Receipt(sender.id(),receiver.id(),sender.account(),receiver.account(),amount,fee,net);
            }
            catch (SQLException e) {
                try {
                    c.rollback();
                }
                catch (SQLException ignored) {
                }
                throw e;
            }
        }
        catch (SQLException failure) {
            if (committing) {
                try {
                    Receipt r=reconcile(token,sender,receiver,amount);
                    if (r!=null)return r;
                }
                catch (SQLException unavailable) {
                    failure.addSuppressed(unavailable);
                    throw new CommitUncertain(failure);
                }
            }
            throw failure;
        }
    }
}
