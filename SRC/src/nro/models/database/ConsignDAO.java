package nro.models.database;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.sql.*;
import java.util.Objects;
import java.util.UUID;
import nro.models.data.LocalManager;
import nro.models.database.RubyGiftDAO.Snapshot;
import nro.models.shop_ky_gui.ConsignItem;

/** Inventory, one listing and the permanent request receipt commit together. */
public class ConsignDAO {
    @FunctionalInterface public interface Connections { Connection open() throws SQLException; }
    public static final class CommitUncertain extends SQLException {
        public CommitUncertain(SQLException cause){super("Consignment commit requires reconciliation",cause);}
    }
    private final Connections connections;
    public ConsignDAO(){this(LocalManager::getConnection);}
    public ConsignDAO(Connections connections){this.connections=connections;}
    private void validate(String token,String action)throws SQLException {
        try { if(token==null||!UUID.fromString(token).toString().equals(token))throw new IllegalArgumentException(); }
        catch(IllegalArgumentException invalid){throw new SQLException("request UUID invalid",invalid);}
        if(!"post".equals(action)&&!"buy".equals(action)&&!"top".equals(action)&&!"cancel".equals(action)&&!"claim".equals(action)
                &&!"box1775".equals(action)&&!"shop1775".equals(action)&&!"ruby_material".equals(action)&&!"intrinsic_max".equals(action))throw new SQLException("invalid inventory action");
    }
    private String lockPlayer(Connection c,Snapshot p)throws SQLException {
        try(PreparedStatement q=c.prepareStatement("SELECT account_id,data_inventory FROM player WHERE id=? FOR UPDATE")) {
            q.setLong(1,p.id());try(ResultSet r=q.executeQuery()){
                if(!r.next()||r.getInt(1)!=p.account())throw new SQLException("player ownership changed");return r.getString(2);
            }
        }
    }
    private Integer existing(Connection c,String token,String action,Snapshot p)throws SQLException {
        try(PreparedStatement q=c.prepareStatement("SELECT action,player_id,account_id,listing_id FROM consign_receipt WHERE request_id=?")) {
            q.setString(1,token);try(ResultSet r=q.executeQuery()) {
                if(!r.next())return null;
                if(!action.equals(r.getString(1))||p.id()!=r.getLong(2)||p.account()!=r.getInt(3))throw new SQLException("receipt parameters mismatch");return r.getInt(4);
            }
        }
    }
    public Integer reconcile(String token,String action,Snapshot player)throws SQLException {
        validate(token,action);
        try(Connection c=connections.open()) {
            c.setAutoCommit(false);try {lockPlayer(c,player);Integer id=existing(c,token,action,player);c.rollback();return id;}
            catch(SQLException failure){rollback(c);throw failure;}
        }
    }
    private static void rollback(Connection c){try{c.rollback();}catch(SQLException ignored){}}
    private String options(ConsignItem i) {
        JsonArray out=new JsonArray();for(var option:i.options){var entry=new com.google.gson.JsonObject();entry.addProperty("id",option.optionTemplate.id);entry.addProperty("param",option.param);out.add(entry);}return out.toString();
    }
    private boolean sameOptions(String stored,ConsignItem i)throws SQLException {
        try {JsonArray array=new Gson().fromJson(stored,JsonArray.class);if(array.size()!=i.options.size())return false;
            for(int n=0;n<array.size();n++){var entry=array.get(n).getAsJsonObject();var live=i.options.get(n);if(entry.get("id").getAsInt()!=live.optionTemplate.id||entry.get("param").getAsInt()!=live.param)return false;}return true;
        }catch(RuntimeException invalid){throw new SQLException("invalid listing options",invalid);}
    }
    private void lockListing(Connection c,ConsignItem i)throws SQLException {
        try(PreparedStatement q=c.prepareStatement("SELECT player_id,tab,item_id,gold,gem,quantity,itemOption,isUpTop,isBuy,currency,create_time FROM shop_ky_gui WHERE id=? FOR UPDATE")) {
            q.setInt(1,i.id);try(ResultSet r=q.executeQuery()){
                if(!r.next()||r.getInt(1)!=i.player_sell||r.getInt(2)!=i.tab||r.getInt(3)!=i.itemId||r.getInt(4)!=i.goldSell||r.getInt(5)!=i.gemSell||r.getInt(6)!=i.quantity||!sameOptions(r.getString(7),i)||r.getInt(8)!=i.isUpTop||r.getBoolean(9)!=i.isBuy||!Objects.equals(r.getString(10),i.currency)||r.getLong(11)!=i.createTime)throw new SQLException("listing changed");
            }
        }
    }
    private void bindListing(PreparedStatement q,ConsignItem i)throws SQLException {
        q.setInt(1,i.player_sell);q.setInt(2,i.tab);q.setInt(3,i.itemId);q.setInt(4,i.goldSell);q.setInt(5,i.gemSell);q.setInt(6,i.quantity);q.setString(7,options(i));q.setInt(8,i.isUpTop);q.setBoolean(9,i.isBuy);q.setString(10,i.currency);q.setLong(11,i.createTime);
    }
    private int changeListing(Connection c,ConsignItem before,ConsignItem after)throws SQLException {
        if(before==null){try(PreparedStatement q=c.prepareStatement("INSERT INTO shop_ky_gui(player_id,tab,item_id,gold,gem,quantity,itemOption,isUpTop,isBuy,currency,create_time) VALUES(?,?,?,?,?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)){
            bindListing(q,after);if(q.executeUpdate()!=1)throw new SQLException("listing insert failed");try(ResultSet key=q.getGeneratedKeys()){if(!key.next())throw new SQLException("listing id missing");long generated=key.getLong(1);if(generated<=0||generated>Integer.MAX_VALUE)throw new SQLException("listing id exceeds database capacity");after.id=(int)generated;return after.id;}
        }}
        lockListing(c,before);
        if(after==null){try(PreparedStatement q=c.prepareStatement("DELETE FROM shop_ky_gui WHERE id=?")){q.setInt(1,before.id);if(q.executeUpdate()!=1)throw new SQLException("listing delete missing");}}
        else {try(PreparedStatement q=c.prepareStatement("UPDATE shop_ky_gui SET player_id=?,tab=?,item_id=?,gold=?,gem=?,quantity=?,itemOption=?,isUpTop=?,isBuy=?,currency=?,create_time=? WHERE id=?")){bindListing(q,after);q.setInt(12,before.id);if(q.executeUpdate()!=1)throw new SQLException("listing update missing");}}
        return before.id;
    }
    private void updatePlayer(Connection c,Snapshot p,String stored,String intrinsic)throws SQLException {
        String inventory;
        try {JsonArray old=new Gson().fromJson(stored,JsonArray.class),next=new Gson().fromJson(p.inventory(),JsonArray.class);if(old.size()<3||next.size()!=5)throw new SQLException("inventory shape");for(int n=0;n<5;n++){JsonElement value=next.get(n);if(n<old.size())old.set(n,value);else old.add(value);}inventory=old.toString();}
        catch(RuntimeException invalid){throw new SQLException("invalid inventory JSON",invalid);}
        StringBuilder sql=new StringBuilder("UPDATE player SET data_inventory=?,items_bag=?");
        if(p.body()!=null)sql.append(",items_body=?");if(p.box()!=null)sql.append(",items_box=?");if(p.luckyBox()!=null)sql.append(",items_box_lucky_round=?");if(p.soldItems()!=null)sql.append(",items_daban=?");if(intrinsic!=null)sql.append(",data_intrinsic=?");sql.append(" WHERE id=? AND account_id=?");
        try(PreparedStatement q=c.prepareStatement(sql.toString())){q.setString(1,inventory);q.setString(2,p.bag());int n=3;if(p.body()!=null)q.setString(n++,p.body());if(p.box()!=null)q.setString(n++,p.box());if(p.luckyBox()!=null)q.setString(n++,p.luckyBox());if(p.soldItems()!=null)q.setString(n++,p.soldItems());if(intrinsic!=null)q.setString(n++,intrinsic);q.setLong(n++,p.id());q.setInt(n,p.account());if(q.executeUpdate()!=1)throw new SQLException("player update missing");}
    }
    public int transact(String token,String action,ConsignItem before,ConsignItem after,Snapshot player)throws SQLException {
        return transact(token,action,before,after,player,null);
    }
    public int transactIntrinsic(String token,Snapshot player,String intrinsic)throws SQLException {
        return transact(token,"intrinsic_max",null,null,player,intrinsic);
    }
    private int transact(String token,String action,ConsignItem before,ConsignItem after,Snapshot player,String intrinsic)throws SQLException {
        if("intrinsic_max".equals(action)) {
            try {JsonArray data=new Gson().fromJson(intrinsic,JsonArray.class);if(data==null||data.size()!=8||data.get(0).getAsInt()<=0||data.get(1).getAsInt()<0||data.get(2).getAsInt()<0)throw new IllegalArgumentException();}
            catch(RuntimeException invalid){throw new SQLException("invalid intrinsic payload",invalid);}
        }else if(intrinsic!=null)throw new SQLException("unexpected intrinsic payload");
        validate(token,action);boolean committing=false;
        try(Connection c=connections.open()) {
            c.setAutoCommit(false);try {
                String inventory=lockPlayer(c,player);Integer prior=existing(c,token,action,player);if(prior!=null){c.rollback();if(before==null&&after!=null)after.id=prior;return prior;}
                boolean inventoryOnly="box1775".equals(action)||"shop1775".equals(action)||"ruby_material".equals(action)||"intrinsic_max".equals(action);
                if(inventoryOnly) {if(before!=null||after!=null)throw new SQLException("inventory receipt cannot change listing");}
                else if("post".equals(action)) {if(before!=null||after==null||after.id!=0||after.player_sell!=player.id()||!"ruby".equals(after.currency)||after.isBuy||after.goldSell!=-1||after.gemSell<=0||after.gemSell>1000000||after.quantity<=0)throw new SQLException("invalid ruby listing");}
                else {
                    if(before==null||before.id<=0)throw new SQLException("listing required");
                    if("buy".equals(action)) {if(before.isBuy||before.player_sell==player.id()||after==null||!after.isBuy)throw new SQLException("invalid purchase");}
                    else if(before.player_sell!=player.id())throw new SQLException("listing ownership changed");
                    if("cancel".equals(action)&&before.isBuy||"claim".equals(action)&&(!before.isBuy||!"ruby".equals(before.currency))||"top".equals(action)&&before.isBuy)throw new SQLException("invalid listing state");
                    if("buy".equals(action)||"top".equals(action)){
                        if(after==null||after.id!=before.id||!"ruby".equals(before.currency)||!"ruby".equals(after.currency)||after.player_sell!=before.player_sell||after.tab!=before.tab||after.itemId!=before.itemId||after.goldSell!=before.goldSell||after.gemSell!=before.gemSell||after.quantity!=before.quantity||after.createTime!=before.createTime||!sameOptions(options(before),after))throw new SQLException("invalid ruby update");
                    }else if(after!=null)throw new SQLException("delete expected");
                }
                int id=inventoryOnly?0:changeListing(c,before,after);updatePlayer(c,player,inventory,intrinsic);
                try(PreparedStatement q=c.prepareStatement("INSERT INTO consign_receipt(request_id,action,player_id,account_id,listing_id) VALUES(?,?,?,?,?)")){q.setString(1,token);q.setString(2,action);q.setLong(3,player.id());q.setInt(4,player.account());q.setInt(5,id);if(q.executeUpdate()!=1)throw new SQLException("receipt insert failed");}
                committing=true;c.commit();return id;
            }catch(SQLException failure){rollback(c);throw failure;}
        }catch(SQLException failure){if(committing){try{Integer id=reconcile(token,action,player);if(id!=null){if(before==null&&after!=null)after.id=id;return id;}}catch(SQLException unavailable){failure.addSuppressed(unavailable);throw new CommitUncertain(failure);}}throw failure;}
    }
}
