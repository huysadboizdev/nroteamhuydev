package nro.models.services;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import nro.models.item.Item;
import nro.models.player_system.Template.ItemTemplate;
import nro.models.server.Manager;
import nro.models.shop_ky_gui.*;
import org.json.simple.JSONObject;

public final class LiveSyncConsignAdminTest {
    static boolean sold;static int writes;static String bag;
    static Object zero(Class<?> c){if(c==boolean.class)return false;if(c==int.class)return 0;if(c==long.class)return 0L;return null;}
    static Connection connection() {
        return (Connection)Proxy.newProxyInstance(LiveSyncConsignAdminTest.class.getClassLoader(),new Class[]{Connection.class},(o,m,a)-> {
            if(!m.getName().equals("prepareStatement"))return zero(m.getReturnType());String sql=(String)a[0];Map<Integer,Object> bindings=new HashMap<>();
            return Proxy.newProxyInstance(LiveSyncConsignAdminTest.class.getClassLoader(),new Class[]{PreparedStatement.class},(p,pm,pa)-> {
                if(pm.getName().startsWith("set")){bindings.put((Integer)pa[0],pa[1]);return null;}
                if(pm.getName().equals("executeUpdate")){writes++;if(sql.startsWith("UPDATE player"))bag=(String)bindings.get(1);return 1;}
                if(pm.getName().equals("executeQuery")) {boolean[] next={true};
                    return Proxy.newProxyInstance(LiveSyncConsignAdminTest.class.getClassLoader(),new Class[]{ResultSet.class},(r,rm,ra)-> {
                        if(rm.getName().equals("next")){boolean v=next[0];next[0]=false;return v;}
                        if(rm.getName().equals("close"))return null;
                        Object v=switch(String.valueOf(ra[0])) {case "id"->7;case "player_id"->42;case "item_id"->933;case "quantity"->3;case "create_time"->1000L;case "itemOption"->"[]";case "isBuy"->sold;case "currency"->"ruby";default->0;};
                        return switch(rm.getName()){case "getInt"->((Number)v).intValue();case "getLong"->((Number)v).longValue();case "getBoolean"->v;case "getString"->v.toString();default->zero(rm.getReturnType());};
                    });
                }return zero(pm.getReturnType());
            });
        });
    }
    static Object stage(Class<?> c,Map<String,String> before)throws Exception {
        return stage(c,before,null);
    }
    static Object stage(Class<?> c,Map<String,String> before,nro.models.player.Player online)throws Exception {
        JSONObject payload=new JSONObject();payload.put("listing_id",7L);
        try{return c.getMethod("stage",Connection.class,JSONObject.class,Map.class,nro.models.player.Player.class).invoke(null,connection(),payload,before,online);}
        catch(InvocationTargetException x){if(x.getCause() instanceof Exception e)throw e;throw x;}
    }
    public static void main(String[] args)throws Exception {
        Class<?> c;try{c=Class.forName("nro.models.services.LiveSyncConsignCommands");}catch(ClassNotFoundException e){throw new AssertionError("Admin consign stage missing");}
        ItemTemplate template=new ItemTemplate();template.id=933;template.type=14;template.name="fixture";template.isUpToUp=true;Manager.ITEM_TEMPLATES.add(template);
        for(int id=0;id<=73;id++){var option=new nro.models.player_system.Template.ItemOptionTemplate();option.id=id;option.name="fixture";Manager.ITEM_OPTION_TEMPLATES.add(option);}
        Item empty=new Item();String emptyBag="[\"[-1,0,\\\"[]\\\",0]\"]";
        Map<String,String> before=Map.of("id","42","account_id","2","items_bag",emptyBag);
        ConsignShopManager.gI().listItem.add(new ConsignItem(7,(short)933,42,(byte)0,-1,10,3,(byte)0,new ArrayList<>(),false));
        Object op;synchronized(ConsignShopManager.gI()){op=stage(c,before);}
        if(writes!=2||bag==null||!bag.contains("933"))throw new AssertionError("Atomic bag/listing stage missing");
        if(ConsignShopManager.gI().listItem.isEmpty())throw new AssertionError("Cache changed before commit");
        op.getClass().getMethod("publish").invoke(op);
        if(!ConsignShopManager.gI().listItem.isEmpty())throw new AssertionError("Listing not published");
        sold=true;writes=0;
        synchronized(ConsignShopManager.gI()) {
            try{stage(c,before);throw new AssertionError("Sold claim erased");}catch(IllegalArgumentException expected){}
        }
        if(writes!=0)throw new AssertionError("Sold listing mutated");
        sold=false;writes=0;
        String fullBag=new com.google.gson.Gson().toJson(List.of("[933,1000000,\"[\\\"[73,0]\\\"]\",1000]"));
        Map<String,String> full=Map.of("id","42","account_id","2","items_bag",fullBag);
        synchronized(ConsignShopManager.gI()) {
            try{stage(c,full);throw new AssertionError("Full bag cancellation accepted");}catch(IllegalArgumentException expected){if(!expected.getMessage().contains("insufficient space"))throw expected;}
        }
        if(writes!=0)throw new AssertionError("Full bag cancellation wrote listing");
        try{stage(c,before);throw new AssertionError("Missing catalog lock accepted");}catch(IllegalArgumentException expected){}
        var online=new nro.models.player.Player();online.id=42;Item owned=new Item();owned.template=template;owned.quantity=2;owned.expire=Long.MAX_VALUE;owned.id=123;owned.quantityGD=7;online.inventory.itemsBag.add(owned);online.inventory.itemsBag.add(new Item());
        synchronized(online){synchronized(ConsignShopManager.gI()){
            Object staged=stage(c,before,online);staged.getClass().getMethod("publish").invoke(staged);
        }}
        if(online.inventory.itemsBag.get(0).expire!=Long.MAX_VALUE||online.inventory.itemsBag.get(0).id!=123||online.inventory.itemsBag.get(0).quantityGD!=7)throw new AssertionError("Cancellation lost existing item metadata");
        System.out.println("LiveSyncConsignAdminTest PASS");
    }
}
