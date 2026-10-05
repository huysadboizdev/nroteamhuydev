package nro.models.database;

import java.sql.*;
import nro.models.data.LocalManager;
import nro.models.commeson.CommesonAttempt;

public final class CommesonDAO {
    @FunctionalInterface public interface Connections {Connection open() throws SQLException;}
    private final Connections connections;
    public CommesonDAO(){this(LocalManager::getConnection);}
    public CommesonDAO(Connections c){connections=c;}
    public boolean claim(CommesonAttempt a,long legacyTime)throws SQLException {
        try(Connection c=connections.open()) {
            c.setAutoCommit(false);
            try {
                // Lock the character row so separate sessions also serialize legacy migration.
                try(PreparedStatement q=c.prepareStatement("SELECT lasttimepkcommeson FROM player WHERE id=? FOR UPDATE")) {
                    q.setLong(1,a.playerId);try(ResultSet r=q.executeQuery()){
                        if(!r.next()){c.rollback();return false;}
                        long old=Math.max(legacyTime,r.getLong(1));
                        if(old>0 && CommesonAttempt.day(old).equals(a.attemptDay)){c.rollback();return false;}
                    }
                }
                // An expired pre-start reservation never becomes an active fight.
                try(PreparedStatement q=c.prepareStatement("DELETE FROM commeson_attempt WHERE player_id=? AND state='RESERVED' AND deadline<=?")){
                    q.setLong(1,a.playerId);q.setLong(2,a.deadline-300_000);q.executeUpdate();
                }
                try(PreparedStatement q=c.prepareStatement("INSERT INTO commeson_attempt(player_id,attempt_day,match_id,state,deadline) VALUES(?,?,?,?,?)")) {
                    q.setLong(1,a.playerId);q.setString(2,a.attemptDay);q.setString(3,a.matchId);q.setString(4,a.state.name());q.setLong(5,a.deadline);q.executeUpdate();
                }
                c.commit();return true;
            } catch(SQLException e) {c.rollback();if(e.getErrorCode()==1062 || "23505".equals(e.getSQLState()))return false;throw e;}
        }
    }
    public void releasePreStart(CommesonAttempt a)throws SQLException {
        try(Connection c=connections.open();PreparedStatement q=c.prepareStatement("DELETE FROM commeson_attempt WHERE match_id=? AND state='RESERVED'")){q.setString(1,a.matchId);q.executeUpdate();}
    }
    public boolean transition(CommesonAttempt a,CommesonAttempt.State expected)throws SQLException {
        try(Connection c=connections.open();PreparedStatement q=c.prepareStatement("UPDATE commeson_attempt SET state=?,deadline=? WHERE match_id=? AND state=?")) {
            q.setString(1,a.state.name());q.setLong(2,a.deadline);q.setString(3,a.matchId);q.setString(4,expected.name());return q.executeUpdate()==1;
        }
    }
    public CommesonAttempt pending(long player)throws SQLException {
        try(Connection c=connections.open();PreparedStatement q=c.prepareStatement("SELECT * FROM commeson_attempt WHERE player_id=? AND state<>'DONE' ORDER BY attempt_day DESC LIMIT 1")) {
            q.setLong(1,player);try(ResultSet r=q.executeQuery()){return r.next()?new CommesonAttempt(r.getString("match_id"),player,r.getString("attempt_day"),CommesonAttempt.State.valueOf(r.getString("state")),r.getLong("deadline")):null;}
        }
    }
    /** The durable reward receipt and inventory snapshot commit together. */
    public void persistPickup(CommesonAttempt a,String bag)throws SQLException {
        try(Connection c=connections.open()) {
            c.setAutoCommit(false);
            try {
                try(PreparedStatement q=c.prepareStatement("UPDATE commeson_attempt SET state='PICKED',deadline=? WHERE match_id=? AND state='PICKING'")) {
                    q.setLong(1,a.deadline);q.setString(2,a.matchId);if(q.executeUpdate()!=1)throw new SQLException("Pickup reservation changed");
                }
                try(PreparedStatement q=c.prepareStatement("UPDATE player SET items_bag=? WHERE id=?")) {
                    q.setString(1,bag);q.setLong(2,a.playerId);if(q.executeUpdate()!=1)throw new SQLException("Character missing");
                }
                c.commit();
            } catch(SQLException e){try{c.rollback();}catch(SQLException ignored){}throw e;}
        }
    }
    public boolean finish(String match)throws SQLException {
        try(Connection c=connections.open();PreparedStatement q=c.prepareStatement("UPDATE commeson_attempt SET state='DONE' WHERE match_id=?")){q.setString(1,match);return q.executeUpdate()==1;}
    }
    public boolean finish(CommesonAttempt a)throws SQLException{return finish(a.matchId);}
}
