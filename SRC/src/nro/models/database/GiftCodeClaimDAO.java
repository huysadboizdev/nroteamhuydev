package nro.models.database;

import com.google.gson.*;
import java.sql.*;
import nro.models.data.LocalManager;

/** Claim, authoritative quota, and only reward-bearing player columns commit together. */
public class GiftCodeClaimDAO {
    @FunctionalInterface public interface Connections {Connection open()throws SQLException;}
    @FunctionalInterface public interface Stage {Snapshot prepare(Catalog gift)throws Exception;}
    public record Catalog(int id,String code,int remaining,String detail){}
    public record Snapshot(long player,int account,String inventory,String bag,String history){}
    public record Receipt(int catalogId,int remaining){}
    public static final class CommitUncertain extends SQLException {
        public CommitUncertain(SQLException cause){super("Giftcode commit requires reconciliation",cause);}
    }
    private final Connections connections;
    public GiftCodeClaimDAO(){this(LocalManager::getConnection);}
    public GiftCodeClaimDAO(Connections connections){this.connections=connections;}
    private String lockPlayer(Connection c,long player,int account)throws SQLException {
        try(PreparedStatement q=c.prepareStatement("SELECT account_id,data_inventory FROM player WHERE id=? FOR UPDATE")) {
            q.setLong(1,player);try(ResultSet r=q.executeQuery()) {
                if(!r.next()||r.getInt(1)!=account)throw new SQLException("Player ownership changed");
                return r.getString(2);
            }
        }
    }
    private Receipt receipt(Connection c,long player,int account,String code,String token)throws SQLException {
        try(PreparedStatement q=c.prepareStatement("SELECT request_id,player_id,catalog_id,remaining_after FROM giftcode_claim WHERE account_id=? AND canonical_code=?")) {
            q.setInt(1,account);q.setString(2,code);try(ResultSet r=q.executeQuery()) {
                if(!r.next())return null;
                if(!token.equals(r.getString(1))||r.getLong(2)!=player)throw new SQLException("Account already claimed this giftcode");
                return new Receipt(r.getInt(3),r.getInt(4));
            }
        }
    }
    /** Locks player first: an absent receipt is conclusive only after original transaction ended. */
    public Receipt reconcile(long player,int account,String code,String token)throws SQLException {
        try(Connection c=connections.open()) {
            c.setAutoCommit(false);
            try {lockPlayer(c,player,account);Receipt found=receipt(c,player,account,code,token);c.rollback();return found;}
            catch(SQLException e){try{c.rollback();}catch(SQLException ignored){}throw e;}
        }
    }
    public Receipt claim(long player,int account,String raw,String token,Stage stage)throws Exception {
        String code=nro.models.player.GiftCode.canonical(raw);
        if(token==null||!java.util.UUID.fromString(token).toString().equals(token))throw new SQLException("Invalid request UUID");
        boolean committing=false;
        try(Connection c=connections.open()) {
            c.setAutoCommit(false);
            try {
                String stored=lockPlayer(c,player,account);
                Receipt prior=receipt(c,player,account,code,token);
                if(prior!=null){c.rollback();return prior;}
                Catalog gift;
                try(PreparedStatement q=c.prepareStatement("SELECT id,code,count_left,detail,(datecreate<=CURRENT_TIMESTAMP AND expired>CURRENT_TIMESTAMP) AS usable FROM giftcode WHERE code=? FOR UPDATE")) {
                    q.setString(1,code);try(ResultSet r=q.executeQuery()) {
                        if(!r.next())throw new SQLException("Giftcode no longer exists");
                        gift=new Catalog(r.getInt(1),r.getString(2),r.getInt(3),r.getString(4));
                        if(!code.equals(gift.code())||!r.getBoolean(5)||gift.remaining()==0||gift.remaining() < -1||r.next())throw new SQLException("Giftcode expired, exhausted or ambiguous");
                    }
                }
                Snapshot snapshot=stage.prepare(gift);
                if(snapshot.player()!=player||snapshot.account()!=account)throw new SQLException("Snapshot ownership mismatch");
                int remaining=gift.remaining()==-1?-1:gift.remaining()-1;
                try(PreparedStatement q=c.prepareStatement("INSERT INTO giftcode_claim(account_id,canonical_code,request_id,player_id,catalog_id,remaining_after) VALUES(?,?,?,?,?,?)")) {
                    q.setInt(1,account);q.setString(2,code);q.setString(3,token);q.setLong(4,player);q.setInt(5,gift.id());q.setInt(6,remaining);
                    if(q.executeUpdate()!=1)throw new SQLException("Claim insert failed");
                }
                try(PreparedStatement q=c.prepareStatement("UPDATE giftcode SET count_left=?,datecreate=datecreate WHERE id=? AND code=? AND count_left=? AND datecreate<=CURRENT_TIMESTAMP AND expired>CURRENT_TIMESTAMP")) {
                    q.setInt(1,remaining);q.setInt(2,gift.id());q.setString(3,code);q.setInt(4,gift.remaining());
                    // Unlimited produces a matched row even though no quota changes (Connector/J defaults matched rows).
                    if(q.executeUpdate()!=1)throw new SQLException("Quota changed or giftcode expired");
                }
                JsonArray old=new JsonParser().parse(stored).getAsJsonArray(),next=new JsonParser().parse(snapshot.inventory()).getAsJsonArray();
                if(old.size()<3||next.size()!=5)throw new SQLException("Invalid inventory shape");
                for(int i=0;i<5;i++){if(i<old.size())old.set(i,next.get(i));else old.add(next.get(i));}
                try(PreparedStatement q=c.prepareStatement("UPDATE player SET data_inventory=?,items_bag=?,giftcode=? WHERE id=? AND account_id=?")) {
                    q.setString(1,old.toString());q.setString(2,snapshot.bag());q.setString(3,snapshot.history());q.setLong(4,player);q.setInt(5,account);
                    if(q.executeUpdate()!=1)throw new SQLException("Reward persistence failed");
                }
                committing=true;c.commit();return new Receipt(gift.id(),remaining);
            }catch(Exception e){try{c.rollback();}catch(SQLException ignored){}throw e;}
        }catch(SQLException failure) {
            if(committing) {
                try{Receipt committed=reconcile(player,account,code,token);if(committed!=null)return committed;}
                catch(SQLException unavailable){failure.addSuppressed(unavailable);throw new CommitUncertain(failure);}
            }
            throw failure;
        }
    }
}
