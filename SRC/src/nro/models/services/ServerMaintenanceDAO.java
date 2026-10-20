package nro.models.services;

import java.sql.*;
import org.json.simple.JSONObject;
import static nro.models.services.ServerMaintenanceService.*;

/** One durable state transition and queue receipt per transaction; no SQL lease during drain. */
public final class ServerMaintenanceDAO implements Store {
    @FunctionalInterface public interface Connections { Connection open() throws SQLException; }
    private final Connections connections;
    public ServerMaintenanceDAO(Connections connections) { this.connections = connections; }
    private static State read(Connection c, boolean lock) throws SQLException {
        try (Statement q = c.createStatement(); ResultSet r = q.executeQuery(
                "SELECT phase,revision,command_id,message FROM server_maintenance_state WHERE id=1" + (lock ? " FOR UPDATE" : ""))) {
            if (!r.next()) throw new SQLException("Apply server maintenance migration first");
            String phase = r.getString(1);
            if (!java.util.Set.of("OPEN","DRAINING","MAINTENANCE","FAILED").contains(phase)) throw new SQLException("Invalid maintenance phase");
            return new State(phase,r.getLong(2),r.getLong(3),r.getString(4));
        }
    }
    private static void heartbeat(Connection c, String run) throws SQLException {
        try (PreparedStatement q = c.prepareStatement("UPDATE server_maintenance_state SET server_run=?,heartbeat_at=CURRENT_TIMESTAMP WHERE id=1")) {
            q.setString(1,run); q.executeUpdate();
        }
    }
    @Override public State load(String run) throws SQLException {
        try (Connection c = connections.open()) { heartbeat(c,run); return read(c,false); }
    }
    @Override public void heartbeat(String run) throws SQLException {
        try (Connection c = connections.open()) { heartbeat(c,run); }
    }
    @Override public Intent next() throws SQLException {
        try (Connection c = connections.open(); Statement q = c.createStatement(); ResultSet r = q.executeQuery(
                "SELECT id,command_type FROM server_command_queue WHERE status IN ('pending','processing') AND command_type IN ('MAINTENANCE_START','MAINTENANCE_OPEN') ORDER BY id LIMIT 1")) {
            return r.next() ? new Intent(r.getLong(1),r.getString(2)) : null;
        }
    }
    @Override public State accept(Intent intent, String run) throws SQLException {
        try (Connection c = connections.open()) {
            c.setAutoCommit(false);
            try {
                State current = read(c,true);
                var command = LiveSyncCommandDAO.lock(c,intent.id());
                if (command == null) { c.rollback(); return current; }
                if (!ServerMaintenanceService.supports(command.type())) throw new IllegalArgumentException("Invalid control command");
                if (LiveSyncCommandDAO.receipt(c,intent.id())) {
                    // Never replay a completed operation even if an old queue row is retried.
                    if (current.commandId()!=intent.id() || !"DRAINING".equals(current.phase())) complete(c,intent.id());
                    c.commit(); return current;
                }
                try {
                    LiveSyncCommandDAO.authorize(c,command.actor());
                    if (!command.payload().isEmpty()) throw new IllegalArgumentException("Maintenance payload must be empty");
                    if (intent.id() <= current.commandId()) throw new IllegalArgumentException("Stale maintenance command");
                    String next;
                    if ("MAINTENANCE_START".equals(command.type())) {
                        if (!java.util.Set.of("OPEN","FAILED").contains(current.phase())) throw new IllegalArgumentException("Maintenance already active");
                        next="DRAINING";
                    } else {
                        if (!"MAINTENANCE".equals(current.phase())) throw new IllegalArgumentException("Maintenance has not finished saving");
                        next="OPEN";
                    }
                    String message = "OPEN".equals(next) ? "Đã mở đăng nhập" : "Đang lưu và ngắt người chơi";
                    LiveSyncCommandDAO.record(c,command,0,0,message);
                    write(c,next,current.revision()+1,intent.id(),message,run);
                    LiveSyncCommandDAO.status(c,intent.id(),"OPEN".equals(next)?"completed":"processing",message);
                } catch (IllegalArgumentException denied) {
                    LiveSyncCommandDAO.status(c,intent.id(),"failed",denied.getMessage());
                }
                State result = read(c,false); c.commit(); return result;
            } catch (SQLException | RuntimeException failure) { c.rollback(); throw failure; }
        }
    }
    private static void write(Connection c,String phase,long revision,long command,String message,String run) throws SQLException {
        try (PreparedStatement q=c.prepareStatement("UPDATE server_maintenance_state SET phase=?,revision=?,command_id=?,message=?,server_run=?,heartbeat_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=1")) {
            q.setString(1,phase);q.setLong(2,revision);q.setLong(3,command);q.setString(4,message);q.setString(5,run);q.executeUpdate();
        }
    }
    private static void complete(Connection c,long id) throws SQLException {
        try (PreparedStatement q=c.prepareStatement("UPDATE server_command_queue q JOIN admin_command_receipt r ON r.command_id=q.id SET q.status='completed',q.result_message=r.result_message,q.executed_at=CURRENT_TIMESTAMP WHERE q.id=? AND q.status IN ('pending','processing')")) {
            q.setLong(1,id);q.executeUpdate();
        }
    }
    @Override public State finish(State expected,String phase,String message,String run) throws SQLException {
        try (Connection c=connections.open()) {
            c.setAutoCommit(false);
            try {
                State current=read(c,true);
                if (current.revision()!=expected.revision() || current.commandId()!=expected.commandId() || !"DRAINING".equals(current.phase())) { c.rollback(); return current; }
                write(c,phase,current.revision()+1,current.commandId(),message,run);
                try(PreparedStatement q=c.prepareStatement("UPDATE admin_command_receipt SET result_message=? WHERE command_id=?")) { q.setString(1,message);q.setLong(2,current.commandId());q.executeUpdate(); }
                LiveSyncCommandDAO.status(c,current.commandId(),"FAILED".equals(phase)?"failed":"completed",message);
                State result=read(c,false);c.commit();return result;
            } catch (SQLException | RuntimeException failure) { c.rollback();throw failure; }
        }
    }
}
