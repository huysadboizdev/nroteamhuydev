package nro.models.event;
import java.sql.*;
import java.util.*;
import org.json.simple.JSONObject;
import nro.models.data.LocalManager;
/** Durable desired revisions; runtime completion is recorded separately. */
public final class EventStateDAO {
 public record State(String key,boolean enabled,long revision,long run) {}
 public static List<State> read(Connection c)throws SQLException {
  List<State> states=new ArrayList<>();try(Statement p=c.createStatement();ResultSet r=p.executeQuery("SELECT event_key,desired_enabled,revision,run_id FROM game_event_state")){while(r.next())states.add(new State(r.getString(1),r.getBoolean(2),r.getLong(3),r.getLong(4)));}return states;
 }
 public static void load()throws Exception {
  List<State> states;try(Connection c=LocalManager.getConnection()){states=read(c);}
  Map<String,Boolean> desired=new HashMap<>(),closed=new HashMap<>();
  for(State state:states){desired.put(state.key,state.enabled);closed.put(state.key,state.enabled&&EventItemPolicy.isEnabled(state.key));}
  // Close admission before retiring sources. New admission opens only after
  // every requested runtime has been prepared successfully.
  EventItemPolicy.publish(closed);
  EventRuntimeService.gI().reconcile(desired);
  EventItemPolicy.publish(desired);
  EventRuntimeService.gI().refreshVisibility();
 }
 public static void publish(List<State> states){Map<String,Boolean> m=new HashMap<>();for(State s:states)m.put(s.key,s.enabled);EventItemPolicy.publish(m);}
 public static String stage(Connection c,JSONObject payload)throws SQLException {
  if(!payload.keySet().equals(Set.of("event_key","enabled","revision"))||!(payload.get("enabled") instanceof Boolean))throw new IllegalArgumentException("Invalid event payload");
  String key=String.valueOf(payload.get("event_key"));var definition=EventCatalog.get(key);if(definition==null)throw new IllegalArgumentException("Unknown event");
  boolean enabled=(Boolean)payload.get("enabled");long revision;try{revision=Long.parseLong(String.valueOf(payload.get("revision")));}catch(Exception e){throw new IllegalArgumentException("Invalid revision");}
  if(enabled&&(!definition.supported()||!EventRuntimeService.gI().ready(key)))throw new IllegalArgumentException("Sự kiện chưa đủ nội dung/tài nguyên để bật: "+definition.reason());
  long current,run;boolean before;
  try(PreparedStatement p=c.prepareStatement("SELECT revision,run_id,desired_enabled FROM game_event_state WHERE event_key=? FOR UPDATE")){p.setString(1,key);try(ResultSet r=p.executeQuery()){if(!r.next())throw new SQLException("Missing event state");current=r.getLong(1);run=r.getLong(2);before=r.getBoolean(3);}}
  if(revision!=current)throw new IllegalArgumentException("Cấu hình đã thay đổi, hãy tải lại trang");
  if(enabled){try(PreparedStatement p=c.prepareStatement("SELECT id FROM game_event_cleanup_job WHERE event_key=? AND status<>'completed' LIMIT 1")){p.setString(1,key);try(ResultSet r=p.executeQuery()){if(r.next())throw new IllegalArgumentException("Chờ bảo trì dọn rương trước khi mở phiên mới");}}}
  if(before!=enabled&&enabled)run++;
  if(!enabled){try(PreparedStatement p=c.prepareStatement("INSERT IGNORE INTO game_event_cleanup_job(event_key,run_id,consumed_ids,error_message) VALUES(?,?,?,'')")){p.setString(1,key);p.setLong(2,run);p.setString(3,Arrays.toString(definition.consumables()));p.executeUpdate();}}
  try(PreparedStatement p=c.prepareStatement("UPDATE game_event_state SET desired_enabled=?,revision=revision+1,run_id=?,status='APPLYING',last_error='' WHERE event_key=?")){p.setBoolean(1,enabled);p.setLong(2,run);p.setString(3,key);p.executeUpdate();}
  return "Đã lưu yêu cầu "+(enabled?"bật ":"tắt ")+definition.name()+"; giữ quà đã nhận";
 }
 public static void applied()throws SQLException {
  try(Connection c=LocalManager.getConnection();Statement p=c.createStatement()) {
   p.executeUpdate("UPDATE game_event_state s SET applied_enabled=desired_enabled,status=CASE WHEN desired_enabled=1 THEN 'ON' WHEN EXISTS(SELECT 1 FROM game_event_cleanup_job j WHERE j.event_key=s.event_key AND j.status<>'completed') THEN 'WAITING_MAINTENANCE' ELSE 'OFF' END,last_error='',heartbeat_at=CURRENT_TIMESTAMP");
  }
 }
 public static void heartbeat()throws SQLException {
  try(Connection c=LocalManager.getConnection();Statement p=c.createStatement()){p.executeUpdate("UPDATE game_event_state SET heartbeat_at=CURRENT_TIMESTAMP");}
 }
 public static void error(String message){try(Connection c=LocalManager.getConnection();PreparedStatement p=c.prepareStatement("UPDATE game_event_state SET status='ERROR',last_error=?,heartbeat_at=CURRENT_TIMESTAMP WHERE status='APPLYING'")){p.setString(1,message);p.executeUpdate();}catch(SQLException ignored){}}
}
