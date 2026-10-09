package nro.models.services;

import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.function.IntFunction;
import java.util.function.LongSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import nro.models.data.LocalManager;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.player_system.Template.ItemTemplate;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

/** Village leaderboards use read-only projections; never load a full player. */
public final class VillageRankingService {
    public enum Kind { POWER, TASK }
    public record Row(int id, String name, short head, short body, short leg,
            long power, long taskId, long taskIndex, long taskCount) {}
    @FunctionalInterface public interface Source { List<Row> load(Kind kind) throws SQLException; }
    private record Cache(List<Row> rows, long loadedAt) {}
    private static final Logger LOG=Logger.getLogger(VillageRankingService.class.getName());
    private static final long CACHE_MS=60_000L;
    private static final int LIMIT=100;
    private static final VillageRankingService INSTANCE=new VillageRankingService(VillageRankingService::loadDatabase,System::currentTimeMillis);
    private final Source source;
    private final LongSupplier clock;
    private final EnumMap<Kind,Cache> caches=new EnumMap<>(Kind.class);

    public VillageRankingService(Source source, LongSupplier clock) {
        this.source=Objects.requireNonNull(source);this.clock=Objects.requireNonNull(clock);
    }
    public static VillageRankingService gI() { return INSTANCE; }
    public void showPower(Player player) { show(player,Kind.POWER); }
    public void showTask(Player player) { show(player,Kind.TASK); }

    /** Refreshes are serialized so concurrent menu requests share one query. */
    public synchronized List<Row> rows(Kind kind) throws SQLException {
        long now=clock.getAsLong(); Cache old=caches.get(kind);
        if(old!=null && now>=old.loadedAt && now-old.loadedAt<CACHE_MS)return old.rows;
        List<Row> fresh=rank(source.load(kind),kind);
        caches.put(kind,new Cache(fresh,now));return fresh;
    }

    private void show(Player player,Kind kind) {
        Message message=null;
        try {
            List<Row> top=rows(kind);
            message=new Message(-96);
            writePacket(message,top,kind,player.getSession().version);
            player.sendMessage(message);
        } catch(Exception failure) {
            LOG.log(Level.WARNING,"Village ranking unavailable: "+kind,failure);
            Service.gI().sendThongBao(player,"Bảng xếp hạng tạm thời không khả dụng. Vui lòng thử lại sau.");
        } finally { if(message!=null)message.cleanup(); }
    }

    public static String formatPower(long power) {
        if(power<0)throw new IllegalArgumentException("Negative power");
        long divisor;String unit;
        if(power>=1_000_000_000L){divisor=1_000_000_000L;unit=" tỷ";}
        else if(power>=1_000_000L){divisor=1_000_000L;unit=" tr";}
        else if(power>=1_000L){divisor=1_000L;unit=" nghìn";}
        else return Long.toString(power);
        long decimal=(power%divisor)/(divisor/10);
        return Long.toString(power/divisor)+(decimal==0?"":","+decimal)+unit;
    }
    public static String taskInfo(Row row) { return Long.toString(row.taskId); }
    public static List<Row> rank(List<Row> rows,Kind kind) {
        Comparator<Row> order=kind==Kind.POWER?Comparator.comparingLong(Row::power).reversed():
                Comparator.comparingLong(Row::taskId).reversed()
                .thenComparing(Comparator.comparingLong(Row::taskIndex).reversed())
                .thenComparing(Comparator.comparingLong(Row::taskCount).reversed());
        return rows.stream().filter(Objects::nonNull).sorted(order.thenComparingInt(Row::id)).limit(LIMIT).toList();
    }
    public static void writePacket(Message message,List<Row> rows,Kind kind,int version) throws IOException {
        List<Row> top=rank(rows,kind);
        message.writer().writeByte(0);
        message.writer().writeUTF(kind==Kind.POWER?"Top sức mạnh":"Top nhiệm vụ");
        message.writer().writeByte(top.size());
        for(int i=0;i<top.size();i++) {
            Row row=top.get(i);
            message.writer().writeInt(i+1);message.writer().writeInt(row.id);
            message.writer().writeShort(row.head);
            if(version>214)message.writer().writeShort(-1);
            message.writer().writeShort(row.body);message.writer().writeShort(row.leg);
            message.writer().writeUTF(row.name);
            message.writer().writeUTF(kind==Kind.POWER?formatPower(row.power):taskInfo(row));
            message.writer().writeUTF("");
        }
    }

    private static JSONArray array(Object value) {
        Object parsed=value instanceof String?JSONValue.parse((String)value):value;
        if(!(parsed instanceof JSONArray))throw new IllegalArgumentException("Invalid array");
        return (JSONArray)parsed;
    }
    private static long nonNegative(Object value) {
        if(value==null || !value.toString().matches("[0-9]+"))throw new IllegalArgumentException("Invalid integer");
        return Long.parseLong(value.toString());
    }
    public static Row parseRow(int id,String name,short head,byte gender,String points,String task,String equipment,
            IntFunction<ItemTemplate> templates) {
        try {
            if(id<=0 || name==null || name.isEmpty())return null;
            JSONArray p=array(points),t=array(task);
            long power=nonNegative(p.get(1)),taskId=nonNegative(t.get(0)),index=nonNegative(t.get(1)),count=nonNegative(t.get(2));
            short body=(short)(gender==1?59:57),leg=(short)(gender==1?60:58);
            // An invalid equipment slot falls back independently, preserving the row.
            JSONArray items;
            try { items=array(equipment); } catch(RuntimeException malformed) { items=new JSONArray(); }
            ItemTemplate shirt=equipped(items,0,templates),pants=equipped(items,1,templates),costume=equipped(items,5,templates);
            if(shirt!=null)body=shirt.part;if(pants!=null)leg=pants.part;
            if(costume!=null) {
                if(costume.head>=0)head=(short)costume.head;
                if(costume.body>=0)body=(short)costume.body;
                if(costume.leg>=0)leg=(short)costume.leg;
            }
            return new Row(id,name,head,body,leg,power,taskId,index,count);
        } catch(RuntimeException malformed) { return null; }
    }
    private static ItemTemplate equipped(JSONArray items,int slot,IntFunction<ItemTemplate> templates) {
        try {
            if(items.size()<=slot)return null;
            JSONArray item=array(items.get(slot));int id=Integer.parseInt(item.get(0).toString());
            if(id<0 || nonNegative(item.get(1))==0)return null;
            // Match equipped item day expiry without modifying inventory/options.
            if(item.size()>3) {
                long created=nonNegative(item.get(3));
                long days=Math.max(0,java.time.temporal.ChronoUnit.DAYS.between(
                        java.time.Instant.ofEpochMilli(created).atZone(nro.models.utils.TimeUtil.VIETNAM_ZONE).toLocalDate(),
                        java.time.Instant.ofEpochMilli(System.currentTimeMillis()).atZone(nro.models.utils.TimeUtil.VIETNAM_ZONE).toLocalDate()));
                for(Object option:array(item.get(2))) {
                    JSONArray values=array(option);
                    if(nonNegative(values.get(0))==93 && nonNegative(values.get(1))<days)return null;
                }
            }
            return templates.apply(id);
        } catch(RuntimeException malformed) { return null; }
    }

    // CASE keeps JSON_EXTRACT safe even on historical malformed saves.
    private static String number(String column,int index) {
        return "JSON_UNQUOTE(JSON_EXTRACT(CASE WHEN JSON_VALID("+column+") THEN "+column+" ELSE '[]' END,'$["+index+"]'))";
    }
    static String query(Kind kind) {
        String[] values={number("data_point",1),number("data_task",0),number("data_task",1),number("data_task",2)};
        StringBuilder sql=new StringBuilder("SELECT id,name,head,gender,data_point,data_task,items_body FROM player WHERE id>0 AND name<>''");
        for(String value:values)sql.append(" AND ").append(value).append(" REGEXP '^[0-9]+$' AND CAST(").append(value).append(" AS DECIMAL(20,0))<=9223372036854775807");
        sql.append(" ORDER BY ");
        if(kind==Kind.POWER)sql.append("CAST(").append(values[0]).append(" AS SIGNED) DESC");
        else for(int i=1;i<4;i++){if(i>1)sql.append(',');sql.append("CAST(").append(values[i]).append(" AS SIGNED) DESC");}
        return sql.append(",id ASC LIMIT 100").toString();
    }
    private static List<Row> loadDatabase(Kind kind) throws SQLException {
        List<Row> top=new ArrayList<>(LIMIT);
        try(Connection connection=LocalManager.getConnection();PreparedStatement statement=connection.prepareStatement(query(kind))) {
            statement.setQueryTimeout(10);
            try(ResultSet result=statement.executeQuery()) {
                while(result.next()) {
                    Row row=parseRow(result.getInt("id"),result.getString("name"),result.getShort("head"),result.getByte("gender"),
                            result.getString("data_point"),result.getString("data_task"),result.getString("items_body"),
                            id->ItemService.gI().getTemplate(id));
                    if(row!=null)top.add(row);
                }
            }
        }
        return top;
    }
}
