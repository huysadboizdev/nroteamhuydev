package nro.models.services;

import com.google.gson.*;
import java.sql.*;
import java.util.*;
import nro.models.item.*;
import nro.models.player.Player;
import nro.models.shop_ky_gui.*;
import org.json.simple.JSONObject;

/** Caller holds seller lifecycle/player then ConsignShopManager through commit/publication. */
public final class LiveSyncConsignCommands {
    private LiveSyncConsignCommands() {}
    public static boolean supports(String type){return "CONSIGN_CANCEL".equals(type);}
    private static int listingId(JSONObject payload) {
        Object raw=payload.get("listing_id");if(raw==null)raw=payload.get("id");
        try {int id=Integer.parseInt(String.valueOf(raw));if(id<=0)throw new IllegalArgumentException();return id;}
        catch(RuntimeException invalid){throw new IllegalArgumentException("Invalid listing_id",invalid);}
    }
    public static long sellerId(Connection con,JSONObject payload)throws SQLException {
        try(PreparedStatement q=con.prepareStatement("SELECT player_id FROM shop_ky_gui WHERE id=?")) {
            q.setInt(1,listingId(payload));try(ResultSet r=q.executeQuery()) {
                if(!r.next())throw new IllegalArgumentException("Listing does not exist");return r.getLong("player_id");
            }
        }
    }
    public static final class Operation {
        private final int listing;private final Player online;private final List<Item> bag;
        private final String serialized;private boolean published;
        private Operation(int listing,Player online,List<Item> bag,String serialized) {
            this.listing=listing;this.online=online;this.bag=bag;this.serialized=serialized;
        }
        public String result(){return "Unsold listing #"+listing+" cancelled; item returned to seller";}
        public String bag(){return serialized;}
        public void publish() {
            synchronized(ConsignShopManager.gI()) {
                if(published)return;
                ConsignShopManager.gI().listItem.removeIf(i->i.id==listing);
                if(online!=null)online.inventory.itemsBag=bag;
                published=true;
                if(online!=null&&online.getSession()!=null) {
                    try{InventoryService.gI().sendItemBags(online);}
                    catch(RuntimeException packetFailure){nro.models.utils.Logger.logException(LiveSyncConsignCommands.class,packetFailure,"Admin cancellation bag refresh failed");}
                }
            }
        }
    }
    public static Operation stage(Connection con,JSONObject payload,Map<String,String> before,Player online)throws SQLException {
        int listing=listingId(payload);
        if(!Thread.holdsLock(ConsignShopManager.gI()))throw new IllegalArgumentException("Cancellation requires consignment transaction lock through commit");
        if(ConsignTransactions.isListingPending(listing))throw new IllegalArgumentException("Listing has pending transaction reconciliation");
        long expected=Long.parseLong(before.get("id"));int account=Integer.parseInt(before.get("account_id"));
        if(expected<=0||account<=0)throw new IllegalArgumentException("Invalid seller identity");
        if(online!=null&&(!Thread.holdsLock(online)||online.id!=expected||online.beforeDispose||online.inventory==null))
            throw new IllegalArgumentException("Seller session changed");
        if(ConsignTransactions.hasPendingId(expected)||ConsignTransactions.hasPendingAccount(account)
                ||RubyGiftService.hasPendingId(expected)||RubyGiftService.hasPendingAccount(account))
            throw new IllegalArgumentException("Seller has pending transaction reconciliation");
        if(online!=null&&(online.tradeWVP||nro.models.services_func.TransactionService.isTrading(online)
                ||online.walletPurchasePending!=null||online.kolProgressPending||online.kolRewardPending))
            throw new IllegalArgumentException("Seller has another active transaction");
        Item returned=new Item();
        try(PreparedStatement q=con.prepareStatement("SELECT player_id,item_id,quantity,itemOption,isBuy,create_time FROM shop_ky_gui WHERE id=? FOR UPDATE")) {
            q.setInt(1,listing);try(ResultSet r=q.executeQuery()) {
                if(!r.next()||r.getLong("player_id")!=expected)throw new IllegalArgumentException("Listing seller changed");
                if(r.getBoolean("isBuy"))throw new IllegalArgumentException("Listing already sold; seller revenue claim is preserved");
                int id=r.getInt("item_id");returned.template=LiveSyncCatalogCommands.template(id);returned.quantity=r.getInt("quantity");
                returned.createTime=r.getLong("create_time");
                if(returned.template==null||returned.quantity<=0)throw new IllegalArgumentException("Invalid listing item");
                JsonArray options=parseArray(r.getString("itemOption"));
                for(JsonElement option:options) {
                    JsonObject o=option.getAsJsonObject();returned.itemOptions.add(LiveSyncCatalogCommands.option(o.get("id").getAsInt(),o.get("param").getAsInt()));
                }
            }
        }
        SealedEquipment.normalize(returned);
        List<Item> bag=online==null?decodeBag(before.get("items_bag")):copyBag(online.inventory.itemsBag);
        if(!InventoryService.gI().addItemList(bag,returned))throw new IllegalArgumentException("Seller bag has insufficient space; listing preserved");
        String serialized=encodeBag(bag);
        try(PreparedStatement q=con.prepareStatement("UPDATE player SET items_bag=? WHERE id=? AND account_id=?")) {
            q.setString(1,serialized);q.setLong(2,expected);q.setInt(3,account);
            if(q.executeUpdate()!=1)throw new SQLException("Seller update failed");
        }
        try(PreparedStatement q=con.prepareStatement("DELETE FROM shop_ky_gui WHERE id=? AND player_id=? AND isBuy=0")) {
            q.setInt(1,listing);q.setLong(2,expected);if(q.executeUpdate()!=1)throw new SQLException("Listing changed during cancellation");
        }
        return new Operation(listing,online,bag,serialized);
    }
    static List<Item> copyBag(List<Item> original) {
        List<Item> out=new ArrayList<>();for(Item item:original) {
            if(item==null||item.itemOptions==null)throw new IllegalArgumentException("Invalid live bag slot");
            Item copy=new Item();copy.template=item.template;copy.info=item.info;copy.content=item.content;
            copy.quantity=item.quantity;copy.quantityGD=item.quantityGD;copy.createTime=item.createTime;
            copy.id=item.id;copy.text=item.text;copy.expire=item.expire;copy.options=item.options;
            for(Item.ItemOption option:item.itemOptions)copy.itemOptions.add(new Item.ItemOption(option));
            out.add(copy);
        }return out;
    }
    static JsonArray parseArray(String raw) {
        try {JsonArray out=new Gson().fromJson(raw,JsonArray.class);if(out==null)throw new IllegalArgumentException();return out;}
        catch(RuntimeException invalid){throw new IllegalArgumentException("Invalid saved item JSON",invalid);}
    }
    static JsonArray nested(JsonElement value){return value.isJsonArray()?value.getAsJsonArray():parseArray(value.getAsString());}
    static List<Item> decodeBag(String raw) {
        List<Item> bag=new ArrayList<>();JsonArray rows=parseArray(raw);
        if(rows.size()>nro.models.player.Inventory.MAX_ITEMS_BAG)throw new IllegalArgumentException("Invalid seller bag size");
        for(JsonElement value:rows) {
            JsonArray row=nested(value);if(row.size()!=4)throw new IllegalArgumentException("Unsupported seller item save shape");
            Item item=new Item();int id=row.get(0).getAsInt();
            if(id!=-1) {
                item.template=LiveSyncCatalogCommands.template(id);item.quantity=row.get(1).getAsInt();
                if(item.template==null||item.quantity<=0)throw new IllegalArgumentException("Invalid saved seller item");
                for(JsonElement option:nested(row.get(2))) {
                    JsonArray pair=nested(option);if(pair.size()!=2)throw new IllegalArgumentException("Invalid saved seller option");
                    item.itemOptions.add(LiveSyncCatalogCommands.option(pair.get(0).getAsInt(),pair.get(1).getAsInt()));
                }
            }
            item.createTime=row.get(3).getAsLong();bag.add(item);
        }return bag;
    }
    static String encodeBag(List<Item> bag) {
        JsonArray out=new JsonArray();for(Item item:bag) {
            JsonArray row=new JsonArray(),options=new JsonArray();row.add(item.template==null?-1:item.template.id);row.add(item.template==null?0:item.quantity);
            if(item.template!=null)for(Item.ItemOption option:item.itemOptions) {
                if(option.optionTemplate==null)throw new IllegalArgumentException("Invalid staged item option");
                JsonArray pair=new JsonArray();pair.add(option.optionTemplate.id);pair.add(option.param);options.add(pair.toString());
            }
            row.add(options.toString());row.add(item.createTime);out.add(row.toString());
        }return out.toString();
    }
}
