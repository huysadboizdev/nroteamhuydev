package nro.models.services;

import java.sql.*;
import java.util.*;
import org.json.simple.*;

/** Queue and receipt use the very same transaction as the resource update. */
public final class LiveSyncCommandDAO {
    public record Command(long id,String type,JSONObject payload,int actor) {}
    private LiveSyncCommandDAO() {}
    public static List<Long> pending(Connection c)throws SQLException {
        List<Long> ids=new ArrayList<>();
        try(PreparedStatement q=c.prepareStatement("SELECT id FROM server_command_queue WHERE status IN ('pending','processing') ORDER BY id LIMIT 10");ResultSet r=q.executeQuery()){
            while(r.next())ids.add(r.getLong(1));
        }return ids;
    }
    public static Command lock(Connection c,long id)throws SQLException {
        try(PreparedStatement q=c.prepareStatement("SELECT command_type,payload,actor_account_id,status FROM server_command_queue WHERE id=? FOR UPDATE")){
            q.setLong(1,id);try(ResultSet r=q.executeQuery()){
                if(!r.next()||!Set.of("pending","processing").contains(r.getString("status")))return null;
                Object value=JSONValue.parse(r.getString("payload"));
                if(!(value instanceof JSONObject))throw new IllegalArgumentException("Invalid command JSON");
                return new Command(id,r.getString("command_type"),(JSONObject)value,r.getInt("actor_account_id"));
            }
        }
    }
    public static boolean receipt(Connection c,long id)throws SQLException {
        try(PreparedStatement q=c.prepareStatement("SELECT command_id FROM admin_command_receipt WHERE command_id=?")){
            q.setLong(1,id);try(ResultSet r=q.executeQuery()){return r.next();}
        }
    }
    public static void authorize(Connection c,int actor)throws SQLException {
        if(actor<=0)throw new IllegalArgumentException("Command lacks authenticated admin identity");
        try(PreparedStatement q=c.prepareStatement("SELECT is_admin,admin,ban FROM account WHERE id=?")){
            q.setInt(1,actor);try(ResultSet r=q.executeQuery()){
                if(!r.next()||(!r.getBoolean(1)&&!r.getBoolean(2))||r.getBoolean(3))throw new IllegalArgumentException("Admin access has been revoked");
            }
        }
    }
    public static Map<String,String> player(Connection c,long id)throws SQLException {
        try(PreparedStatement q=c.prepareStatement("SELECT * FROM player WHERE id=? FOR UPDATE")){
            q.setLong(1,id);try(ResultSet r=q.executeQuery()){
                if(!r.next())throw new IllegalArgumentException("Player does not exist");
                Map<String,String> out=new LinkedHashMap<>();ResultSetMetaData meta=r.getMetaData();
                for(int i=1;i<=meta.getColumnCount();i++)out.put(meta.getColumnLabel(i),r.getString(i));return out;
            }
        }
    }
    public static void updatePlayer(Connection c,long id,int account,Map<String,String> changes)throws SQLException {
        if(changes.isEmpty())return;
        Set<String> allowed=Set.of("data_inventory","data_point","items_bag","items_box","items_body","pet","data_location","data_task","data_magic_tree");
        if(!allowed.containsAll(changes.keySet()))throw new IllegalArgumentException("Unsupported SQL player field");
        StringJoiner columns=new StringJoiner(",");for(String key:changes.keySet())columns.add(key+"=?");
        try(PreparedStatement q=c.prepareStatement("UPDATE player SET "+columns+" WHERE id=? AND account_id=?")){
            int index=1;for(String value:changes.values())q.setString(index++,value);q.setLong(index++,id);q.setInt(index,account);
            if(q.executeUpdate()!=1)throw new SQLException("Player identity changed");
        }
    }
    public static void record(Connection c,Command cmd,long player,int account,String result)throws SQLException {
        try(PreparedStatement q=c.prepareStatement("INSERT INTO admin_command_receipt(command_id,command_type,player_id,account_id,result_message) VALUES(?,?,?,?,?)")){
            q.setLong(1,cmd.id());q.setString(2,cmd.type());q.setLong(3,player);q.setInt(4,account);q.setString(5,result);q.executeUpdate();
        }
    }
    public static void status(Connection c,long id,String status,String result)throws SQLException {
        try(PreparedStatement q=c.prepareStatement("UPDATE server_command_queue SET status=?,result_message=?,executed_at=CURRENT_TIMESTAMP WHERE id=? AND status IN ('pending','processing')")){
            q.setString(1,status);q.setString(2,result==null?status:result.substring(0,Math.min(250,result.length())));q.setLong(3,id);q.executeUpdate();
        }
    }
}
