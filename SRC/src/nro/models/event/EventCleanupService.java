package nro.models.event;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import nro.models.data.LocalManager;
import nro.models.player.Player;
import nro.models.server.Client;
import nro.models.services.*;
import nro.models.shop_ky_gui.ConsignShopManager;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;

/** Per-player transactions, immutable per-job backups; boxes require maintenance. */
public final class EventCleanupService {
    @FunctionalInterface interface Connections {Connection open()throws SQLException;}
    private record Job(long id,String key,Set<Integer> consumed) {}
    private record Pending(String operation,int account,Map<String,String> changes,Player player) {}
    private static final Map<Long,Pending> PENDING=new ConcurrentHashMap<>();
    private static final List<String> IMMEDIATE=List.of("items_bag","items_body","items_daban","pet");
    private static final List<String> BOXES=List.of("items_box","items_collection","items_box_lucky_round");
    public static boolean pending(long id){return PENDING.containsKey(id);}
    public static List<Player> pendingPlayers(){return PENDING.values().stream().map(Pending::player).toList();}
    private static List<Job> jobs(Connection c)throws SQLException {
        List<Job> out=new ArrayList<>();
        try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT j.id,j.event_key,j.consumed_ids FROM game_event_cleanup_job j JOIN game_event_state s ON s.event_key=j.event_key WHERE j.status<>'completed' AND s.desired_enabled=0 ORDER BY j.id")) {
            while(r.next()) {
                Object value=JSONValue.parse(r.getString(3));
                if(!(value instanceof JSONArray list))throw new SQLException("Invalid cleanup item snapshot");
                Set<Integer> consumed=new HashSet<>();
                for(Object item:list){if(!(item instanceof Long id)||id<0||id>Short.MAX_VALUE)throw new SQLException("Invalid cleanup item ID");consumed.add(id.intValue());}
                out.add(new Job(r.getLong(1),r.getString(2),Set.copyOf(consumed)));
            }
        }
        return out;
    }
    public static void online()throws Exception {
        nro.models.services_func.TransactionService.cancelDisabledEventTrades();
        for(Player p:Client.gI().getPlayers())synchronized(p) {
            if(pending(p.id))recover(p);
            if(p.beforeDispose||p.inventory==null||p.session==null)continue;
            if(LiveSyncMutationGate.blocked(p)||AdminPlayerActions.hasPending(p)||nro.models.services_func.TransactionService.isTrading(p)||p.tradeWVP)continue;
            Map<String,String> runtime=LiveSyncPlayerRuntime.snapshot(p);
            runtime.put("items_daban",LiveSyncPlayerPatch.encodeItemList(p.inventory.itemsDaBan));
            try(Connection c=LocalManager.getConnection()){clean(c,p.id,false,runtime,p);}
            if(pending(p.id))recover(p);
        }
    }
    public static void beforeLogin(long id)throws Exception {
        if(pending(id))throw new SQLException("Event deletion awaiting reconciliation");
        try(Connection c=LocalManager.getConnection()){clean(c,id,false,null,null);}
    }
    private static String cleaned(String field,String value,Set<Integer> removed) {
        return field.equals("pet")?EventCleanupCodec.cleanPet(value,removed):EventCleanupCodec.clean(value,removed);
    }
    private static void clean(Connection c,long id,boolean maintenance,Map<String,String> runtime,Player online)throws Exception {
        c.setAutoCommit(false);boolean attempted=false;Pending pending=null;
        String operation=UUID.randomUUID().toString(),phase=maintenance?"maintenance":"live";
        try {
            List<Job> jobs=jobs(c);if(jobs.isEmpty()){c.rollback();return;}
            Map<String,String> original=new LinkedHashMap<>();
            try(PreparedStatement q=c.prepareStatement("SELECT items_bag,items_body,items_daban,pet,items_box,items_collection,items_box_lucky_round FROM player WHERE id=? FOR UPDATE")) {
                q.setLong(1,id);try(ResultSet r=q.executeQuery()){
                    if(!r.next())throw new SQLException("Cleanup player disappeared");
                    for(String field:IMMEDIATE)original.put(field,r.getString(field));
                    if(maintenance)for(String field:BOXES)original.put(field,r.getString(field));
                }
            }
            if(runtime!=null)for(String field:IMMEDIATE)if(runtime.containsKey(field))original.put(field,runtime.get(field));
            Map<String,String> current=new LinkedHashMap<>(original);
            for(Job job:jobs) {
                for(String field:current.keySet()) {
                    String before=current.get(field),after=cleaned(field,before,job.consumed);
                    if(!after.equals(before)) {
                        try(PreparedStatement q=c.prepareStatement("INSERT INTO game_event_cleanup_backup(operation_id,job_id,player_id,field_name,before_json,after_json,phase) VALUES(?,?,?,?,?,?,?)")) {
                            q.setString(1,operation);q.setLong(2,job.id);q.setLong(3,id);q.setString(4,field);q.setString(5,before);q.setString(6,after);q.setString(7,phase);q.executeUpdate();
                        }
                        current.put(field,after);
                    }
                }
                if(maintenance)try(PreparedStatement q=c.prepareStatement("INSERT IGNORE INTO game_event_cleanup_receipt(job_id,player_id,phase) VALUES(?,?,'maintenance')")) {
                    q.setLong(1,job.id);q.setLong(2,id);
                    if(q.executeUpdate()==1)try(PreparedStatement progress=c.prepareStatement("UPDATE game_event_cleanup_job SET processed_players=processed_players+1 WHERE id=?")){progress.setLong(1,job.id);progress.executeUpdate();}
                }
            }
            Map<String,String> changes=new LinkedHashMap<>();
            for(String field:current.keySet())if(!current.get(field).equals(original.get(field)))changes.put(field,current.get(field));
            if(!changes.isEmpty()) {
                StringJoiner columns=new StringJoiner(",");for(String field:changes.keySet())columns.add(field+"=?");
                try(PreparedStatement q=c.prepareStatement("UPDATE player SET "+columns+" WHERE id=?")) {
                    int n=1;for(String raw:changes.values())q.setString(n++,raw);q.setLong(n,id);
                    if(q.executeUpdate()!=1)throw new SQLException("Cleanup write failed");
                }
                try(PreparedStatement q=c.prepareStatement("INSERT INTO game_event_cleanup_operation(operation_id,player_id,phase) VALUES(?,?,?)")){q.setString(1,operation);q.setLong(2,id);q.setString(3,phase);q.executeUpdate();}
                if(online!=null) {
                    pending=new Pending(operation,online.session.userId,Map.copyOf(changes),online);
                    PENDING.put(id,pending);LiveSyncCommandGate.mark(id,pending.account);
                }
            }
            attempted=true;c.commit();
        }catch(Exception failure) {
            if(!attempted) {
                c.rollback();
                if(pending!=null){PENDING.remove(id);LiveSyncCommandGate.clear(id,pending.account);}
            }
            throw failure;
        }
    }
    /** Wait for the original player lock before treating receipt absence as confirmed rollback. */
    private static boolean committed(Connection c,long id,String operation)throws SQLException {
        c.setAutoCommit(false);
        try {
            try(PreparedStatement q=c.prepareStatement("SELECT id FROM player WHERE id=? FOR UPDATE")){q.setLong(1,id);try(ResultSet r=q.executeQuery()){if(!r.next())throw new SQLException("Cleanup player disappeared during reconciliation");}}
            boolean committed;
            try(PreparedStatement q=c.prepareStatement("SELECT operation_id FROM game_event_cleanup_operation WHERE operation_id=? AND player_id=?")){q.setString(1,operation);q.setLong(2,id);try(ResultSet r=q.executeQuery()){committed=r.next();}}
            c.rollback();return committed;
        }catch(SQLException failure){c.rollback();throw failure;}
    }
    private static void recover(Player p)throws Exception {
        recover(p,LocalManager::getConnection);
    }
    private static void recover(Player p,Connections connections)throws Exception {
        Pending pending=PENDING.get(p.id);if(pending==null)return;
        boolean committed;
        try(Connection c=connections.open()){committed=committed(c,p.id,pending.operation);}
        if(committed) {
            Map<String,String> core=new LinkedHashMap<>(pending.changes);core.remove("items_daban");
            List<nro.models.item.Item> sold=pending.changes.containsKey("items_daban")?LiveSyncPlayerPatch.decodeItemList(pending.changes.get("items_daban")):null;
            LiveSyncPlayerRuntime.publish(p,core);if(sold!=null)p.inventory.itemsDaBan=sold;
        }
        PENDING.remove(p.id);LiveSyncCommandGate.clear(p.id,pending.account);
    }
    public static boolean reconcilePending(Player p)throws Exception {synchronized(p){recover(p);return !pending(p.id);}}
    public static void drainOnline()throws Exception {
        online();for(Player p:Client.gI().getPlayers())synchronized(p){if(pending(p.id))recover(p);}
        if(!PENDING.isEmpty())throw new SQLException("Pending event cleanup must drain before maintenance");
    }
    @SuppressWarnings("unchecked")
    private static void cleanConsign(Connection c)throws Exception {
        c.setAutoCommit(false);boolean attempted=false;
        try {
            for(Job job:jobs(c))for(int item:job.consumed) {
                List<Long> listings=new ArrayList<>();
                try(PreparedStatement q=c.prepareStatement("SELECT * FROM shop_ky_gui WHERE item_id=? AND isBuy=0 ORDER BY id FOR UPDATE")) {
                    q.setInt(1,item);try(ResultSet r=q.executeQuery()) {
                        ResultSetMetaData metadata=r.getMetaData();
                        while(r.next()) {
                            JSONObject before=new JSONObject();for(int n=1;n<=metadata.getColumnCount();n++)before.put(metadata.getColumnLabel(n),r.getObject(n));
                            long listing=r.getLong("id");
                            try(PreparedStatement backup=c.prepareStatement("INSERT INTO game_event_cleanup_consign_backup(job_id,listing_id,before_json) VALUES(?,?,?)")){backup.setLong(1,job.id);backup.setLong(2,listing);backup.setString(3,before.toJSONString());backup.executeUpdate();}
                            listings.add(listing);
                        }
                    }
                }
                for(long listing:listings)try(PreparedStatement q=c.prepareStatement("DELETE FROM shop_ky_gui WHERE id=? AND isBuy=0 AND item_id=?")){q.setLong(1,listing);q.setInt(2,item);if(q.executeUpdate()!=1)throw new SQLException("Unsold event listing changed");}
            }
            attempted=true;c.commit();
        }catch(Exception failure){if(!attempted)c.rollback();throw failure;}
    }
    private static void refreshConsign(Connections connections)throws SQLException {
        Set<Integer> persisted=new HashSet<>();
        try(Connection c=connections.open();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT id FROM shop_ky_gui")){while(r.next())persisted.add(r.getInt(1));}
        ConsignShopManager.gI().listItem.removeIf(item->!persisted.contains(item.id));
    }
    private static void maintenanceError(Connections connections,Exception failure)throws SQLException {
        try(Connection c=connections.open();PreparedStatement q=c.prepareStatement("UPDATE game_event_cleanup_job SET status='error',error_message=? WHERE status='maintenance_running'")) {
            q.setString(1,failure.getClass().getSimpleName()+": "+failure.getMessage());q.executeUpdate();
        }
    }
    public static void resumeMaintenanceIfNeeded()throws Exception {
        boolean resume;
        try(Connection c=LocalManager.getConnection();Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT id FROM game_event_cleanup_job WHERE status IN ('maintenance_running','error') LIMIT 1")){resume=r.next();}
        if(resume)maintenance();
    }
    public static void maintenance()throws Exception {
        if(!Client.gI().getPlayers().isEmpty()||!PENDING.isEmpty())throw new SQLException("Event cleanup requires no online writers");
        maintenance(LocalManager::getConnection);
    }
    private static void maintenance(Connections connections)throws Exception {
        try(Connection c=connections.open();Statement q=c.createStatement()) {
            q.executeUpdate("UPDATE game_event_cleanup_job j JOIN game_event_state s ON s.event_key=j.event_key SET j.status='maintenance_running',j.error_message='' WHERE j.status<>'completed' AND s.desired_enabled=0");
        }
        try {
            long cursor=0;
            while(true) {
                List<Long> batch=new ArrayList<>();
                try(Connection c=connections.open();PreparedStatement q=c.prepareStatement("SELECT id FROM player WHERE id>? ORDER BY id LIMIT 100")){q.setLong(1,cursor);try(ResultSet r=q.executeQuery()){while(r.next())batch.add(r.getLong(1));}}
                if(batch.isEmpty())break;
                for(long id:batch)try(Connection c=connections.open()){clean(c,id,true,null,null);}
                cursor=batch.get(batch.size()-1);
            }
            synchronized(ConsignShopManager.gI()) {
                try {try(Connection c=connections.open()){cleanConsign(c);}}
                finally {refreshConsign(connections);}
            }
            try(Connection c=connections.open()) {
                List<Job> jobs=jobs(c);
                for(Job job:jobs) {
                    try(PreparedStatement q=c.prepareStatement("SELECT p.id FROM player p LEFT JOIN game_event_cleanup_receipt r ON r.player_id=p.id AND r.job_id=? AND r.phase='maintenance' WHERE r.player_id IS NULL LIMIT 1")){q.setLong(1,job.id);try(ResultSet r=q.executeQuery()){if(r.next())throw new SQLException("Maintenance receipt coverage incomplete");}}
                    try(Statement q=c.createStatement();ResultSet r=q.executeQuery("SELECT items_bag,items_body,items_daban,pet,items_box,items_collection,items_box_lucky_round FROM player")) {
                        while(r.next())for(String field:List.of("items_bag","items_body","items_daban","pet","items_box","items_collection","items_box_lucky_round")) {
                            String raw=r.getString(field);if(!raw.equals(cleaned(field,raw,job.consumed)))throw new SQLException("Event consumables remain after maintenance");
                        }
                    }
                    for(int item:job.consumed)try(PreparedStatement q=c.prepareStatement("SELECT id FROM shop_ky_gui WHERE item_id=? AND isBuy=0 LIMIT 1")){q.setInt(1,item);try(ResultSet r=q.executeQuery()){if(r.next())throw new SQLException("Unsold event listing remains");}}
                }
                c.setAutoCommit(false);
                for(Job job:jobs)try(PreparedStatement q=c.prepareStatement("UPDATE game_event_cleanup_job SET status='completed',error_message='' WHERE id=?")){q.setLong(1,job.id);q.executeUpdate();}
                c.commit();
            }
        }catch(Exception failure) {
            try{maintenanceError(connections,failure);}catch(SQLException statusFailure){failure.addSuppressed(statusFailure);}
            throw failure;
        }
    }
}
