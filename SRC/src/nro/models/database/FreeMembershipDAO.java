package nro.models.database;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import java.sql.*;
import nro.models.data.LocalManager;

/** Free, idempotent account activation; task proof and activation commit together. */
public class FreeMembershipDAO {
    @FunctionalInterface public interface Connections {Connection open()throws SQLException;}
    private final Connections connections;
    public FreeMembershipDAO(){this(LocalManager::getConnection);}
    public FreeMembershipDAO(Connections connections){this.connections=connections;}
    private boolean lock(Connection c,long player,int account)throws SQLException {
        boolean active;
        try(PreparedStatement q=c.prepareStatement("SELECT active,ban FROM account WHERE id=? FOR UPDATE")){
            q.setInt(1,account);try(ResultSet r=q.executeQuery()){
                if(!r.next()||r.getBoolean(2))throw new SQLException("account unavailable");active=r.getBoolean(1);
            }
        }
        try(PreparedStatement q=c.prepareStatement("SELECT account_id FROM player WHERE id=? FOR UPDATE")){
            q.setLong(1,player);try(ResultSet r=q.executeQuery()){
                if(!r.next()||r.getInt(1)!=account)throw new SQLException("player ownership changed");
            }
        }
        return active;
    }
    private boolean read(long player,int account)throws SQLException {
        try(Connection c=connections.open()){
            c.setAutoCommit(false);try{boolean active=lock(c,player,account);c.rollback();return active;}
            catch(SQLException failure){try{c.rollback();}catch(SQLException ignored){}throw failure;}
        }
    }
    public boolean activate(long player,int account,String task)throws SQLException {
        if(player<=0||account<=0)throw new SQLException("invalid identity");
        try{
            JsonArray a=new Gson().fromJson(task,JsonArray.class);
            if(a==null||a.size()!=4||a.get(0).getAsInt()<2||a.get(1).getAsInt()<0||a.get(2).getAsInt()<0)throw new IllegalArgumentException();
        }catch(RuntimeException malformed){throw new SQLException("Moc Nhan task not completed",malformed);}
        boolean committing=false;
        try(Connection c=connections.open()){
            c.setAutoCommit(false);
            try{
                if(lock(c,player,account)){c.rollback();return true;}
                try(PreparedStatement q=c.prepareStatement("UPDATE player SET data_task=? WHERE id=? AND account_id=?")){
                    q.setString(1,task);q.setLong(2,player);q.setInt(3,account);q.executeUpdate();
                }
                try(PreparedStatement q=c.prepareStatement("UPDATE account SET active=1 WHERE id=? AND active=0 AND ban=0")){
                    q.setInt(1,account);if(q.executeUpdate()!=1)throw new SQLException("activation failed");
                }
                committing=true;c.commit();return true;
            }catch(SQLException failure){try{c.rollback();}catch(SQLException ignored){}throw failure;}
        }catch(SQLException failure){
            if(committing){try{return read(player,account);}catch(SQLException unavailable){failure.addSuppressed(unavailable);}}
            throw failure;
        }
    }
}
