package nro.models.services;

import java.sql.*;
import java.util.*;
import nro.models.server.Manager;
import nro.models.managers.GiftCodeManager;
import nro.models.player_system.GiftCode;
import nro.models.player_system.Template.MobTemplate;
import nro.models.shop.*;
import nro.models.item.Item;
import org.json.simple.*;

/** Validate complete snapshots first; readers keep their old list until publication. */
public final class LiveSyncCatalogCommands {
    public static final Object LOCK = new Object();
    private LiveSyncCatalogCommands() {}
    public static boolean supports(String type) {
        return type!=null && Set.of("RELOAD_ALL","RELOAD_GIFTCODE","RELOAD_MOB","RELOAD_BOSS","RELOAD_SHOP","RELOAD_NOTIFY","SET_EXP_RATE","SEND_NOTICE").contains(type);
    }
    public static String execute(Connection con,String type,JSONObject payload)throws Exception {
        // Same monitor as consumption: a consume cannot write an old count after reload read.
        synchronized(GiftCodeManager.gI()) {synchronized(LOCK) {
            Operation op=stage(con,type,payload);op.publish();return op.result();
        }}
    }
    public static final class Operation {
        private final String result;private final Runnable action;private boolean published;
        private Operation(String result,Runnable action){this.result=result;this.action=action;}
        public String result(){return result;}
        public synchronized void publish(){if(!published){action.run();published=true;}}
    }
    /** Queue caller retains GiftCodeManager then LOCK monitors until commit and publish. */
    public static Operation stage(Connection con,String type,JSONObject payload)throws Exception {
        if(!supports(type))throw new IllegalArgumentException("Unsupported catalog command");
        if(con==null||payload==null)throw new IllegalArgumentException("Missing catalog connection/payload");
        if("SET_EXP_RATE".equals(type)) {
            Object raw=payload.get("rate");valid(raw instanceof Number||raw instanceof String,"Missing rate");
            String value=String.valueOf(raw);valid(value.matches("[0-9]{1,3}"),"Invalid EXP rate");int rate=Integer.parseInt(value);
            valid(rate>=1&&rate<=100,"EXP rate must be 1..100");
            LiveSettingsDAO.ExpSetting setting=LiveSettingsDAO.storeExp(con,rate);
            return expOperation(setting);
        }
        if("SEND_NOTICE".equals(type)) {
            Object raw=payload.get("message");valid(raw instanceof String,"Missing notice message");String message=(String)raw;
            ChatGlobalService.validateSystemNotice(message);
            return new Operation("World notice broadcast applied",()->ChatGlobalService.sendSystemNotice(message));
        }
            boolean all="RELOAD_ALL".equals(type);
            List<GiftCode> gifts=all||"RELOAD_GIFTCODE".equals(type)?gifts(con):null;
            List<MobTemplate> mobs=all||"RELOAD_MOB".equals(type)?mobs(con):null;
            List<Shop> shops=all||"RELOAD_SHOP".equals(type)?shops(con):null;
            List<String> notices=all||"RELOAD_NOTIFY".equals(type)?notices(con):null;
            Runnable bosses=all||"RELOAD_BOSS".equals(type)?BossLiveReload.stage(con):null;
            return new Operation("Applied "+type+(mobs==null?"":"; mob HP applies at next respawn; reconnect to refresh mob data"),()-> {
                if(gifts!=null)GiftCodeManager.gI().listGiftCode=new ArrayList<>(gifts);
                if(mobs!=null)publishMobs(mobs);
                if(shops!=null){long next=shopVersion+1;for(Shop s:shops)s.catalogVersion=next;Manager.SHOPS=List.copyOf(shops);shopVersion=next;}
                if(notices!=null)Manager.NOTIFY=List.copyOf(notices);
                if(bosses!=null)bosses.run();
            });
    }
    /** Receipt recovery reads latest SQL, never replays a historical setting write. */
    public static Operation restore(Connection con,String type,JSONObject payload)throws Exception {
        if("SET_EXP_RATE".equals(type)) {
            LiveSettingsDAO.ExpSetting current=LiveSettingsDAO.readExp(con);
            if(current==null)throw new SQLException("Committed EXP setting missing");
            return expOperation(current);
        }
        return stage(con,type,payload);
    }
    private static long expVersion;
    private static Operation expOperation(LiveSettingsDAO.ExpSetting setting) {
        return new Operation("EXP rate set to "+setting.rate(),()-> {
            synchronized(LOCK) {
                if(setting.version()>=expVersion) {Manager.RATE_EXP_SERVER=(byte)setting.rate();expVersion=setting.version();}
            }
        });
    }
    private static volatile long shopVersion;
    private record MobChange(long generation,int hp) {}
    private static volatile Map<Integer,MobChange> mobChanges=Map.of();
    private static long mobGeneration;
    private static void publishMobs(List<MobTemplate> mobs) {
        Map<Integer,MobChange> next=new HashMap<>(mobChanges);long generation=++mobGeneration;
        for(MobTemplate t:mobs) {
            MobTemplate before=Manager.getMobTemplateByTemp(t.id);
            if(before==null||before.hp!=t.hp)next.put(t.id,new MobChange(generation,t.hp));
        }
        Manager.MOB_TEMPLATES=List.copyOf(mobs);mobChanges=Map.copyOf(next);
        // Mob name/type/movement are sent in the map catalog at login.
        nro.models.data.DataGame.vsMap=(byte)(nro.models.data.DataGame.vsMap==127?1:nro.models.data.DataGame.vsMap+1);
    }
    public static void applyMobRespawn(nro.models.mob.Mob mob) {
        synchronized(LOCK) {
        MobChange change=mobChanges.get(mob.tempId);
        if(change!=null&&change.generation>mob.catalogGeneration) {
            mob.point.maxHp=change.hp;mob.catalogGeneration=change.generation;
        }
        if(mob.tempId==117)mob.type=0;
        MobTemplate template=Manager.getMobTemplateByTemp(mob.tempId);
        if(template!=null){mob.pDame=template.percentDame;mob.pTiemNang=template.percentTiemNang;}
        }
    }
    public static long shopVersion(){return shopVersion;}
    static void valid(boolean condition,String message){if(!condition)throw new IllegalArgumentException(message);}
    static String text(ResultSet r,String column)throws SQLException {
        String value=r.getString(column);valid(value!=null,"Missing "+column);return value;
    }
    static Item.ItemOption option(int id,int param) {
        valid(param>=0,"Negative item option");
        var template=Manager.ITEM_OPTION_TEMPLATES.stream().filter(t->t!=null&&t.id==id).findFirst().orElse(null);
        valid(template!=null,"Unknown item option "+id);
        Item.ItemOption option=new Item.ItemOption();option.optionTemplate=template;option.param=param;return option;
    }
    static nro.models.player_system.Template.ItemTemplate template(int id) {
        return Manager.ITEM_TEMPLATES.stream().filter(t->t!=null&&t.id==id).findFirst().orElse(null);
    }
    static void validateCustomRow(Shop shop,ItemShop row) {
        if("THIEN_SU".equals(shop.tagName))valid(shop.npcId==56&&shop.typeShop==3&&row.typeSell==3&&row.cost==10&&row.iconSpec==7743,"Thiên Sứ requires ruby10 pricing");
        if("BILL".equals(shop.tagName))valid(shop.npcId==55&&shop.typeShop==3&&row.typeSell==3&&row.cost==100&&row.iconSpec==7743&&row.temp.id>=650&&row.temp.id<=662,"Beerus requires the existing ruby100 exchange rows");
        if("SANTA_HONG_NGOC".equals(shop.tagName)) {
            valid(shop.npcId==39&&shop.typeShop==3&&row.typeSell==3&&row.iconSpec==7743,"Santa requires ruby shop configuration");
            int material=nro.models.shop_ky_gui.ConsignTransactions.rubyMaterialPrice(row.temp.id);
            if(material>0)valid(row.cost==material,"Santa material price is fixed");
            if(row.temp.id==1775)valid(row.cost==50_000,"Santa divine box price is fixed");
        }
    }
    static List<String> notices(Connection c)throws SQLException {
        List<String> out=new ArrayList<>();
        try(PreparedStatement q=c.prepareStatement("select name,text from notify order by id desc");ResultSet r=q.executeQuery()) {
            while(r.next())out.add(text(r,"name")+"<>"+text(r,"text"));
        }return out;
    }
    static List<MobTemplate> mobs(Connection c)throws SQLException {
        List<MobTemplate> out=new ArrayList<>();Set<Integer> ids=new HashSet<>();
        try(PreparedStatement q=c.prepareStatement("select * from mob_template order by id");ResultSet r=q.executeQuery()) {
            while(r.next()) {
                MobTemplate t=new MobTemplate();t.id=r.getInt("id");t.type=r.getByte("type");t.name=text(r,"name");t.hp=r.getInt("hp");
                t.rangeMove=r.getByte("range_move");t.speed=r.getByte("speed");t.dartType=r.getByte("dart_type");
                t.percentDame=r.getByte("percent_dame");t.percentTiemNang=r.getByte("percent_tiem_nang");
                valid(t.id>=0&&t.id<=127&&ids.add(t.id)&&t.hp>0,"Invalid/duplicate mob template");
                if(t.id==117){t.type=0;t.rangeMove=0;t.speed=0;}
                out.add(t);
            }
        }return out;
    }
    static List<GiftCode> gifts(Connection c)throws SQLException {
        List<GiftCode> out=new ArrayList<>();Set<String> codes=new HashSet<>();Set<Integer> ids=new HashSet<>();
        try(PreparedStatement q=c.prepareStatement("select * from giftcode FOR UPDATE");ResultSet r=q.executeQuery()) {
            while(r.next()) {
                GiftCode g=new GiftCode();g.id=r.getInt("id");g.code=nro.models.player.GiftCode.canonical(text(r,"code"));g.countLeft=r.getInt("count_left");
                valid(g.id>0&&ids.add(g.id)&&!g.code.isBlank()&&codes.add(g.code)&&g.countLeft>=-1,"Invalid/duplicate giftcode");
                // -1 is the existing unlimited convention; preserve SQL count, never refill.

                g.datecreate=r.getTimestamp("datecreate");g.dateexpired=r.getTimestamp("expired");
                valid(g.datecreate!=null&&g.dateexpired!=null&&!g.datecreate.after(g.dateexpired),"Invalid giftcode dates");
                Object parsed=JSONValue.parse(r.getString("detail"));valid(parsed instanceof JSONArray,"Invalid giftcode detail JSON");
                for(Object entry:(JSONArray)parsed) {
                    valid(entry instanceof JSONObject,"Invalid giftcode item");JSONObject item=(JSONObject)entry;
                    int id=Integer.parseInt(String.valueOf(item.get("id"))),qty=Integer.parseInt(String.valueOf(item.get("quantity")));
                    valid(qty>0&&id>=-3&&!g.detail.containsKey(id),"Invalid/duplicate giftcode item");
                    if(id>=0)valid(template(id)!=null,"Unknown giftcode item "+id);
                    ArrayList<Item.ItemOption> options=new ArrayList<>();Object opts=item.get("options");
                    valid(opts==null||opts instanceof JSONArray,"Invalid giftcode options");
                    if(opts!=null)for(Object raw:(JSONArray)opts) {
                        valid(raw instanceof JSONObject,"Invalid giftcode option");JSONObject o=(JSONObject)raw;
                        options.add(option(Integer.parseInt(String.valueOf(o.get("id"))),Integer.parseInt(String.valueOf(o.get("param")))));
                    }
                    g.detail.put(id,qty);g.option.put(id,options);
                }out.add(g);
            }
        }return out;
    }
    static List<Shop> shops(Connection c)throws SQLException {
        List<Shop> out=new ArrayList<>();Set<String> tags=new HashSet<>();
        try(PreparedStatement q=c.prepareStatement("select * from shop order by npc_id asc");ResultSet r=q.executeQuery()) {
            while(r.next()) {
                Shop s=new Shop();s.id=r.getInt("id");s.npcId=r.getByte("npc_id");s.tagName=text(r,"tag_name");s.typeShop=r.getByte("type_shop");
                valid(s.id>0&&!s.tagName.isBlank()&&tags.add(s.tagName),"Invalid/duplicate shop");
                try(PreparedStatement tabs=c.prepareStatement("select * from tab_shop where shop_id = ? order by id")) {
                    tabs.setInt(1,s.id);try(ResultSet tr=tabs.executeQuery()) {while(tr.next()) {
                        TabShop t=new TabShop();t.shop=s;t.id=tr.getInt("id");t.name=text(tr,"name").replace("<>","\n");
                        try(PreparedStatement items=c.prepareStatement("select * from item_shop where is_sell = 1 and tab_id = ? order by create_time desc")) {
                            items.setInt(1,t.id>=41&&t.id<=43?t.id-31:t.id);
                            try(ResultSet ir=items.executeQuery()) {while(ir.next()) {
                                ItemShop i=new ItemShop();i.tabShop=t;i.id=ir.getInt("id");i.temp=template(ir.getInt("temp_id"));
                                i.isNew=ir.getBoolean("is_new");i.cost=ir.getInt("cost");i.iconSpec=ir.getInt("icon_spec");i.typeSell=ir.getByte("type_sell");
                                valid(i.id>0&&i.temp!=null&&i.cost>=0,"Invalid shop item/price");
                                validateCustomRow(s,i);
                                try(PreparedStatement options=c.prepareStatement("select * from item_shop_option where item_shop_id = ?")) {
                                    options.setInt(1,i.id);try(ResultSet or=options.executeQuery()){while(or.next())i.options.add(option(or.getInt("option_id"),or.getInt("param")));}
                                }t.itemShops.add(i);
                            }}
                        }s.tabShops.add(t);
                    }}
                }out.add(s);
            }
        }return out;
    }
}
