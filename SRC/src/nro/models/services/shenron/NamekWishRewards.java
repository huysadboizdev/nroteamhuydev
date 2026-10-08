package nro.models.services.shenron;

import java.sql.*;
import java.util.*;
import java.util.function.LongFunction;
import java.util.function.Supplier;
import nro.models.data.LocalManager;
import nro.models.item.Item;
import nro.models.player.Inventory;
import nro.models.player.Player;
import nro.models.server.Client;
import nro.models.services.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

/** Stage every recipient before granting; offline bags and receipts commit together. */
public final class NamekWishRewards {
    public enum Result { SUCCESS, BLOCKED, FAILED, PENDING }
    @FunctionalInterface public interface Connections { Connection open() throws SQLException; }
    public static final class Session {
        public final String token = UUID.randomUUID().toString();
        public final int wish;
        public final SortedMap<Long,Item> awards = new TreeMap<>();
        private final Set<Long> gated = new HashSet<>();
        public boolean uncertain, completed;
        public Session(int wish, Collection<Long> ids) {
            if(wish<0||wish>1)throw new IllegalArgumentException("Invalid Namek wish");
            this.wish=wish;
            for(long id:ids)awards.putIfAbsent(id,reward(wish));
            if(awards.isEmpty())throw new IllegalArgumentException("Empty recipients");
        }
    }
    private final Connections connections;
    private final LongFunction<Player> online;
    public NamekWishRewards(){this(LocalManager::getConnection,id->Client.gI().getPlayer(id));}
    public NamekWishRewards(Connections connections,LongFunction<Player> online){this.connections=connections;this.online=online;}

    private static Item reward(int wish) {
        Item item=ItemService.gI().createNewItem((short)(wish==0?16:942));
        if(wish==0)item.quantity=nro.models.utils.Util.nextInt(1,20);
        else {
            int[] options={77,80,81,103,50,94,5}, second={14,16,17,19,27,28,5,47,87};
            int param=nro.models.utils.Util.nextInt(5,10);
            item.itemOptions.clear();
            item.itemOptions.add(new Item.ItemOption(options[nro.models.utils.Util.nextInt(options.length)],param));
            if(nro.models.utils.Util.isTrue(20,100))item.itemOptions.add(new Item.ItemOption(second[nro.models.utils.Util.nextInt(second.length)],param));
            item.itemOptions.add(new Item.ItemOption(30,0));item.quantity=1;
        }
        return item;
    }
    /** Exact clone: staging must not add defaults or normalize existing possessions. */
    public static List<Item> copy(List<Item> items) {
        List<Item> result=new ArrayList<>();
        for(Item source:items){
            if(source==null||source.itemOptions==null)throw new IllegalArgumentException("Invalid bag slot");
            Item item=new Item();item.template=source.template;item.quantity=source.quantity;
            item.createTime=source.createTime;item.expire=source.expire;item.id=source.id;
            item.info=source.info;item.content=source.content;item.quantityGD=source.quantityGD;
            for(Item.ItemOption option:source.itemOptions)item.itemOptions.add(new Item.ItemOption(option));
            result.add(item);
        }
        return result;
    }
    private static boolean pendingId(long id,Session session) {
        return (!session.gated.contains(id)&&LiveSyncCommandGate.hasPendingId(id))
                ||LiveSyncCommandGate.isRetiringId(id)||GiftCodeService.hasPendingId(id)
                ||CommesonChallengeService.hasPendingId(id)||RubyGiftService.hasPendingId(id)
                ||WalletPurchaseService.hasPendingId(id)||nro.models.shop_ky_gui.ConsignTransactions.hasPendingId(id)
                ||KolQuestService.hasProgressPendingId(id)||AdminPlayerActions.hasRenamePendingId(id);
    }
    private static boolean blocked(Player p,Session session) {
        return pendingId(p.id,session)||p.beforeDispose||p.inventory==null||p.walletPurchasePending!=null
                ||nro.models.services_func.TransactionService.isTrading(p)||p.tradeWVP
                ||p.kolProgressPending||p.kolRewardPending
                ||(p.session!=null&&(LiveSyncCommandGate.hasPendingAccount(p.session.userId)||GiftCodeService.hasPendingAccount(p.session.userId)));
    }
    public Result grant(Session session) {
        synchronized(LiveSyncCommandGate.LIFECYCLE_LOCK){
            if(session.completed)return Result.BLOCKED;
            List<Player> players=new ArrayList<>();
            for(long id:session.awards.keySet()){
                if(pendingId(id,session))return Result.BLOCKED;
                Player p=online.apply(id);if(p!=null)players.add(p);
            }
            return locked(players,0,()->grantLocked(session,players));
        }
    }
    private static Result locked(List<Player> players,int index,Supplier<Result> action) {
        if(index==players.size())return action.get();
        synchronized(players.get(index)){return locked(players,index+1,action);}
    }
    private Result grantLocked(Session session,List<Player> players) {
        Map<Long,List<Item>> staged=new LinkedHashMap<>();
        for(Player p:players){
            if(blocked(p,session)||online.apply(p.id)!=p)return Result.BLOCKED;
            List<Item> bag=copy(p.inventory.itemsBag);
            if(!InventoryService.gI().addItemList(bag,copy(List.of(session.awards.get(p.id))).get(0)))return Result.BLOCKED;
            staged.put(p.id,bag);
        }
        List<Long> offline=new ArrayList<>();for(long id:session.awards.keySet())if(!staged.containsKey(id))offline.add(id);
        if(!offline.isEmpty()){
            Result result=offline(session,offline);
            if(result!=Result.SUCCESS)return result;
        }
        // Publication contains no fallible work. Packets are sent after the receipt is consumed.
        for(Player p:players){
            List<Item> live=p.inventory.itemsBag, prepared=staged.get(p.id);
            for(int slot=0;slot<live.size();slot++){
                Item existing=live.get(slot), granted=prepared.get(slot);
                if(existing.isNotNullItem()){
                    // addItemList keeps occupied slots in place; queued gameplay may retain this Item.
                    existing.quantity=granted.quantity;
                    for(int option=0;option<existing.itemOptions.size();option++)
                        existing.itemOptions.get(option).param=granted.itemOptions.get(option).param;
                }else if(granted.isNotNullItem())live.set(slot,granted);
            }
        }
        session.completed=true;session.uncertain=false;
        for(long id:session.gated)LiveSyncCommandGate.clear(id,0);session.gated.clear();
        for(Player p:players){try{InventoryService.gI().sendItemBags(p);}catch(Exception ignored){}}
        return Result.SUCCESS;
    }
    private Result offline(Session session,List<Long> ids) {
        boolean committing=false;
        try(Connection c=connections.open()){
            c.setAutoCommit(false);
            try{
                Map<Long,String> updates=new LinkedHashMap<>();
                for(long id:ids){
                    String saved;
                    // Player row lock also waits for an earlier uncertain transaction to settle.
                    try(PreparedStatement q=c.prepareStatement("SELECT items_bag FROM player WHERE id=? FOR UPDATE")){
                        q.setLong(1,id);try(ResultSet r=q.executeQuery()){if(!r.next())throw new SQLException("Missing Namek recipient");saved=r.getString(1);}
                    }
                    boolean received=false;
                    try(PreparedStatement q=c.prepareStatement("SELECT wish FROM namek_wish_receipt WHERE session_id=? AND player_id=?")){
                        q.setString(1,session.token);q.setLong(2,id);try(ResultSet r=q.executeQuery()){if(r.next()){if(r.getInt(1)!=session.wish)throw new SQLException("Namek receipt mismatch");received=true;}}
                    }
                    if(received)continue;
                    List<Item> bag=decodeBag(saved);
                    if(!InventoryService.gI().addItemList(bag,copy(List.of(session.awards.get(id))).get(0))){c.rollback();return Result.BLOCKED;}
                    updates.put(id,encodeBag(bag));
                }
                for(var entry:updates.entrySet()){
                    try(PreparedStatement q=c.prepareStatement("UPDATE player SET items_bag=? WHERE id=?")){q.setString(1,entry.getValue());q.setLong(2,entry.getKey());if(q.executeUpdate()!=1)throw new SQLException("Namek bag update row count");}
                    try(PreparedStatement q=c.prepareStatement("INSERT INTO namek_wish_receipt(session_id,player_id,wish) VALUES(?,?,?)")){q.setString(1,session.token);q.setLong(2,entry.getKey());q.setInt(3,session.wish);q.executeUpdate();}
                }
                committing=true;c.commit();
            }catch(Exception failure){
                if(committing)throw failure;
                try{c.rollback();}catch(SQLException ignored){}
                throw failure;
            }
            return Result.SUCCESS;
        }catch(Exception failure){
            if(committing||session.uncertain){
                session.uncertain=true;
                for(long id:ids){session.gated.add(id);LiveSyncCommandGate.mark(id,0);}
                return Result.PENDING;
            }
            return Result.FAILED;
        }
    }
    public static List<Item> decodeBag(String json) {
        Object parsed=JSONValue.parse(json);
        if(!(parsed instanceof JSONArray rows)||rows.size()>Inventory.MAX_ITEMS_BAG)throw new IllegalArgumentException("Invalid saved bag");
        List<Item> bag=new ArrayList<>();
        for(Object value:rows){
            Object parsedRow=value instanceof String?JSONValue.parse((String)value):value;
            if(!(parsedRow instanceof JSONArray row)||row.size()!=4)throw new IllegalArgumentException("Invalid saved bag row");
            int id=Integer.parseInt(String.valueOf(row.get(0))),quantity=Integer.parseInt(String.valueOf(row.get(1)));
            Item item=new Item();
            if(id!=-1){
                if(id<0||id>Short.MAX_VALUE||quantity<=0)throw new IllegalArgumentException("Invalid bag item");
                item=ItemService.gI().createItemForRestore((short)id,quantity);if(item.template==null)throw new IllegalArgumentException("Unknown bag item");
                Object optData=row.get(2) instanceof String?JSONValue.parse((String)row.get(2)):row.get(2);
                if(!(optData instanceof JSONArray options))throw new IllegalArgumentException("Invalid bag options");
                for(Object option:options){Object pairData=option instanceof String?JSONValue.parse((String)option):option;
                    if(!(pairData instanceof JSONArray pair)||pair.size()!=2)throw new IllegalArgumentException("Invalid bag option");
                    Item.ItemOption decoded=new Item.ItemOption(Integer.parseInt(String.valueOf(pair.get(0))),Integer.parseInt(String.valueOf(pair.get(1))));
                    if(decoded.optionTemplate==null)throw new IllegalArgumentException("Unknown bag option");item.itemOptions.add(decoded);
                }
            }else if(quantity!=0)throw new IllegalArgumentException("Invalid empty bag item");
            item.createTime=Long.parseLong(String.valueOf(row.get(3)));bag.add(item);
        }
        Inventory.padSlots(bag,Inventory.MAX_ITEMS_BAG);return bag;
    }
    @SuppressWarnings("unchecked") public static String encodeBag(List<Item> bag) {
        JSONArray rows=new JSONArray();
        for(Item item:bag){JSONArray row=new JSONArray(),options=new JSONArray();row.add(item.isNotNullItem()?(int)item.template.id:-1);row.add(item.isNotNullItem()?item.quantity:0);
            if(item.isNotNullItem())for(Item.ItemOption option:item.itemOptions){JSONArray pair=new JSONArray();pair.add(option.optionTemplate.id);pair.add(option.param);options.add(pair.toJSONString());}
            row.add(options.toJSONString());row.add(item.createTime);rows.add(row.toJSONString());
        }
        return rows.toJSONString();
    }
}
