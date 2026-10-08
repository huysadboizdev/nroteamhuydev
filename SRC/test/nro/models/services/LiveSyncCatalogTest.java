package nro.models.services;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import nro.models.server.Manager;
import nro.models.player_system.Template.MobTemplate;
import org.json.simple.JSONObject;

/** Fixture JDBC only: never opens a database connection. */
public final class LiveSyncCatalogTest {
    static Class<?> handler;
    static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    static Object call(String name,Class<?>[] signature,Object... args)throws Exception {
        try{return handler.getMethod(name,signature).invoke(null,args);}
        catch(InvocationTargetException e){if(e.getCause() instanceof Exception x)throw x;throw e;}
    }
    static Connection fixture(Map<String,List<Map<String,Object>>> data) {
        data=new HashMap<>(data);
        data.putIfAbsent("boss_live_override",List.of());
        final Map<String,List<Map<String,Object>>> tables=data;
        final String[] exp={null};
        return (Connection)Proxy.newProxyInstance(LiveSyncCatalogTest.class.getClassLoader(),new Class[]{Connection.class},(o,m,a)-> {
            if(m.getName().equals("prepareStatement")) {
                String sql=(String)a[0];
                return Proxy.newProxyInstance(LiveSyncCatalogTest.class.getClassLoader(),new Class[]{PreparedStatement.class},(p,pm,pa)-> {
                    if(pm.getName().equals("executeQuery")) {
                        List<Map<String,Object>> rows=null;
                        for(var e:tables.entrySet())if(sql.matches("(?is).*\\bfrom\\s+"+e.getKey()+"(?:\\s|$).*"))rows=e.getValue();
                        if(sql.contains("FROM server_runtime_setting"))rows=exp[0]==null?List.of():List.of(Map.of("setting_value",exp[0],"version",1L));
                        if(rows==null)throw new SQLException("fixture query failure: "+sql);
                        var result=rows;int[] index={-1};
                        return Proxy.newProxyInstance(LiveSyncCatalogTest.class.getClassLoader(),new Class[]{ResultSet.class},(r,rm,ra)-> {
                            String n=rm.getName();if(n.equals("next"))return ++index[0]<result.size();if(n.equals("close"))return null;
                            Object value=result.get(index[0]).get(String.valueOf(ra[0]));
                            return switch(n){case "getInt" -> ((Number)value).intValue();case "getLong" -> ((Number)value).longValue();case "getByte" -> ((Number)value).byteValue();case "getBoolean" -> value;case "getString" -> value==null?null:value.toString();case "getTimestamp" -> value;default -> null;};
                        });
                    }
                    if(pm.getName().equals("setString")&&sql.startsWith("INSERT INTO server_runtime_setting")){exp[0]=(String)pa[1];return null;}
                    if(pm.getName().equals("executeUpdate"))return 1;
                    return null;
                });
            }
            return null;
        });
    }
    static void execute(String type,Map<String,List<Map<String,Object>>> data)throws Exception {
        call("execute",new Class[]{Connection.class,String.class,JSONObject.class},fixture(data),type,new JSONObject());
    }
    public static void main(String[] args)throws Exception {
        try {handler=Class.forName("nro.models.services.LiveSyncCatalogCommands");}
        catch(ClassNotFoundException missing){throw new AssertionError("Catalog reload handler missing",missing);}
        for(String type:List.of("RELOAD_GIFTCODE","RELOAD_MOB","RELOAD_SHOP","RELOAD_NOTIFY","RELOAD_ALL"))
            require((Boolean)call("supports",new Class[]{String.class},type),"Unsupported "+type);
        require(!(Boolean)call("supports",new Class[]{String.class},"TYPO"),"Unknown accepted");
        var old=Manager.NOTIFY;old.add("old");
        execute("RELOAD_NOTIFY",Map.of("notify",List.of(Map.of("name","new","text","message"))));
        require(old.equals(List.of("old")),"Reload cleared an iterated list");
        require(Manager.NOTIFY.equals(List.of("new<>message")),"Notify not published");
        var current=Manager.NOTIFY;
        try{execute("RELOAD_ALL",Map.of("notify",List.of(Map.of("name","bad","text","bad"))));throw new AssertionError("SQL failure accepted");}catch(SQLException expected){}
        require(Manager.NOTIFY==current,"ALL partially published");
        Map<String,Object> mob=new HashMap<>();mob.put("id",117);mob.put("type",4);mob.put("name","meter");mob.put("hp",999);mob.put("range_move",9);mob.put("speed",8);mob.put("dart_type",0);mob.put("percent_dame",0);mob.put("percent_tiem_nang",0);
        execute("RELOAD_MOB",Map.of("mob_template",List.of(mob)));
        MobTemplate meter=Manager.MOB_TEMPLATES.get(0);
        require(meter.type==0&&meter.speed==0&&meter.rangeMove==0,"117 movement changed");
        var mobs=Manager.MOB_TEMPLATES;mob.put("hp",0);
        try{execute("RELOAD_MOB",Map.of("mob_template",List.of(mob)));throw new AssertionError("Invalid HP accepted");}catch(IllegalArgumentException expected){}
        require(Manager.MOB_TEMPLATES==mobs,"Invalid mob replaced cache");
        var alive=new nro.models.mob.Mob();alive.tempId=117;alive.point.maxHp=80;alive.point.hp=20;
        mob.put("hp",500);execute("RELOAD_MOB",Map.of("mob_template",List.of(mob)));
        require(alive.point.hp==20&&alive.point.maxHp==80,"Reload touched live HP");
        var newlySpawned=new nro.models.mob.Mob(alive);require(newlySpawned.point.maxHp==500&&newlySpawned.point.hp==500,"New spawn used old map HP");
        alive.hoiSinh();require(alive.point.maxHp==500&&alive.point.hp==500,"Respawn did not apply new HP");
        require(alive.type==0,"Respawn enabled meter movement");
        long version=LiveSyncCatalogCommands.shopVersion();
        execute("RELOAD_SHOP",Map.of("shop",List.of()));
        require(LiveSyncCatalogCommands.shopVersion()>version,"Shop snapshot not invalidated");
        require((Boolean)call("supports",new Class[]{String.class},"SET_EXP_RATE"),"EXP unsupported");
        require((Boolean)call("supports",new Class[]{String.class},"SEND_NOTICE"),"Notice unsupported");
        JSONObject rate=new JSONObject();rate.put("rate",100L);byte oldRate=Manager.RATE_EXP_SERVER;
        Object staged=call("stage",new Class[]{Connection.class,String.class,JSONObject.class},fixture(Map.of()),"SET_EXP_RATE",rate);
        require(Manager.RATE_EXP_SERVER==oldRate,"Rate changed before commit");
        staged.getClass().getMethod("publish").invoke(staged);require(Manager.RATE_EXP_SERVER==100,"Rate not published");
        rate.put("rate",101L);
        try{call("stage",new Class[]{Connection.class,String.class,JSONObject.class},fixture(Map.of()),"SET_EXP_RATE",rate);throw new AssertionError("Invalid exp accepted");}catch(IllegalArgumentException expected){}
        JSONObject notice=new JSONObject();notice.put("message","x".repeat(2001));
        try{call("stage",new Class[]{Connection.class,String.class,JSONObject.class},fixture(Map.of()),"SEND_NOTICE",notice);throw new AssertionError("Oversize notice accepted");}catch(IllegalArgumentException expected){}
        Map<String,Object> gift=new HashMap<>();gift.put("id",1);gift.put("code","fixture");gift.put("count_left",3);gift.put("datecreate",new Timestamp(1000));gift.put("expired",new Timestamp(4102444800000L));gift.put("detail","[{\"id\":-1,\"quantity\":2,\"options\":[]}]");
        var giftBefore=nro.models.managers.GiftCodeManager.gI().listGiftCode;
        execute("RELOAD_GIFTCODE",Map.of("giftcode",List.of(gift)));
        require(nro.models.managers.GiftCodeManager.gI().listGiftCode.get(0).countLeft==3,"Gift count refilled");
        require(giftBefore.isEmpty(),"Gift reload cleared an existing reference");
        gift.put("count_left",1);execute("RELOAD_GIFTCODE",Map.of("giftcode",List.of(gift)));
        require(nro.models.managers.GiftCodeManager.gI().listGiftCode.get(0).countLeft==1,"Consumed gift count not loaded");
        gift.put("detail","bad");var safeGifts=nro.models.managers.GiftCodeManager.gI().listGiftCode;
        try{execute("RELOAD_GIFTCODE",Map.of("giftcode",List.of(gift)));throw new AssertionError("Invalid gifts accepted");}catch(IllegalArgumentException expected){}
        require(nro.models.managers.GiftCodeManager.gI().listGiftCode==safeGifts,"Gift failure replaced cache");
        gift.put("detail","[]");gift.put("expired",new Timestamp(1));
        try{execute("RELOAD_GIFTCODE",Map.of("giftcode",List.of(gift)));throw new AssertionError("Reversed gift dates accepted");}catch(IllegalArgumentException expected){}
        var itemTemplate=new nro.models.player_system.Template.ItemTemplate();itemTemplate.id=1;itemTemplate.name="fixture";Manager.ITEM_TEMPLATES.add(itemTemplate);
        Map<String,Object> shop=new HashMap<>(Map.of("id",1,"npc_id",56,"tag_name","THIEN_SU","type_shop",3));
        Map<String,Object> row=new HashMap<>(Map.of("id",1,"temp_id",1,"is_new",false,"cost",10,"icon_spec",7743,"type_sell",3));
        Map<String,List<Map<String,Object>>> catalog=new HashMap<>();catalog.put("shop",List.of(shop));catalog.put("tab_shop",List.of(Map.of("id",1,"name","fixture")));catalog.put("item_shop",List.of(row));catalog.put("item_shop_option",List.of());
        execute("RELOAD_SHOP",catalog);long goodVersion=LiveSyncCatalogCommands.shopVersion();row.put("cost",11);
        try{execute("RELOAD_SHOP",catalog);throw new AssertionError("Fixed ruby10 price altered");}catch(IllegalArgumentException expected){}
        require(LiveSyncCatalogCommands.shopVersion()==goodVersion,"Invalid price changed version");
        var buyer=new nro.models.player.Player();var openShop=new nro.models.shop.Shop();openShop.tagName="fixture";openShop.catalogVersion=goodVersion-1;buyer.idMark.setShopOpen(openShop);buyer.idMark.setTagNameShop("fixture");
        nro.models.shop.ShopService.gI().buyItem(buyer,1);require(buyer.idMark.getShopOpen()==null,"Stale shop remained purchaseable");
        var moneyCheck=nro.models.shop.ShopService.class.getDeclaredMethod("subMoneyByItemShop",nro.models.player.Player.class,nro.models.shop.ItemShop.class);moneyCheck.setAccessible(true);
        var rubyRow=new nro.models.shop.ItemShop();rubyRow.typeSell=3;rubyRow.cost=10;buyer.inventory.gem=20;buyer.inventory.ruby=0;
        require(!(Boolean)moneyCheck.invoke(nro.models.shop.ShopService.gI(),buyer,rubyRow)&&buyer.inventory.ruby==0,"Ruby shop spent nonexistent ruby");
        System.out.println("LiveSyncCatalogTest PASS");
    }
}
