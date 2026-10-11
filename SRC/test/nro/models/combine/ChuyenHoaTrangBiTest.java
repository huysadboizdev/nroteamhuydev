package nro.models.combine;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.npc.Npc;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

/** Real conversion handlers, actual SQL catalog; only network output is replaced. */
public final class ChuyenHoaTrangBiTest {
    private static final RecordingNpc NPC = new RecordingNpc();
    private static String lastMessage = "";
    public static void main(String[] args) throws Exception {
        for (String line : Files.readAllLines(Path.of(args[0], "templates.tsv")).subList(1,
                Files.readAllLines(Path.of(args[0], "templates.tsv")).size())) {
            String[] v = line.split("\t");
            Template.ItemTemplate t = new Template.ItemTemplate();
            t.id = Short.parseShort(v[0]); t.type = Byte.parseByte(v[1]);
            t.gender = Byte.parseByte(v[2]); t.name = v[3]; t.level = Byte.parseByte(v[4]);
            check(Manager.ITEM_TEMPLATES.size() == t.id, "SQL template index = ID");
            Manager.ITEM_TEMPLATES.add(t);
        }
        List<String> options = Files.readAllLines(Path.of(args[0], "options.tsv"));
        for (String line : options.subList(1, options.size())) {
            String[] v = line.split("\t", -1); int id = Integer.parseInt(v[0]);
            check(Manager.ITEM_OPTION_TEMPLATES.size() == id, "SQL option index = ID");
            Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id, v[1], 0));
        }
        replace(Service.class, "instance", new Service() {
            @Override public void sendMoney(Player p) { }
            @Override public void sendThongBao(Player p, String s) { }
            @Override public void sendThongBaoOK(Player p, String s) { lastMessage = s; }
        });
        replace(InventoryService.class, "I", new InventoryService() {
            @Override public void sendItemBags(Player p) { }
        });
        Field npcField = CombineService.class.getDeclaredField("baHatMit");
        npcField.setAccessible(true); npcField.set(CombineService.gI(), NPC);
        switch (args[1]) {
            case "gold" -> success(false);
            case "gems" -> success(true);
            case "catalog" -> catalog();
            case "guards" -> guards();
            case "stats" -> stats();
            case "sql" -> sqlRoundTrip();
            case "concurrent" -> concurrent();
            case "activation" -> activation();
            default -> throw new AssertionError("Unknown mode");
        }
        System.out.println("PASS conversion " + args[1]);
    }
    private static Player player(int sourceId, int targetId, boolean gems) {
        Player p = new Player() { @Override public void sendMessage(Message m) { } };
        p.inventory.gold = 3_000_000_000L; p.inventory.gem = 6000; p.inventory.ruby = 7000;
        p.combineNew.setTypeCombine(gems ? CombineService.CHUYEN_HOA_TRANG_BI_NGOC
                : CombineService.CHUYEN_HOA_TRANG_BI_VANG);
        Item s = item(sourceId), t = item(targetId);
        s.itemOptions.add(new Item.ItemOption(72, 4));
        s.itemOptions.add(new Item.ItemOption(50, 15));
        s.itemOptions.add(new Item.ItemOption(50, 3));
        s.itemOptions.add(new Item.ItemOption(102, 5));
        s.itemOptions.add(new Item.ItemOption(107, 9));
        s.itemOptions.add(new Item.ItemOption(34, 1));
        t.itemOptions.add(new Item.ItemOption(107, 2));
        t.itemOptions.add(new Item.ItemOption(72, 0));
        t.itemOptions.add(new Item.ItemOption(102, 0));
        p.inventory.itemsBag.add(s); p.inventory.itemsBag.add(t); // full bag
        p.combineNew.itemsCombine.add(s); p.combineNew.itemsCombine.add(t);
        return p;
    }
    private static Item item(int id) {
        Item item = ItemService.gI().createNewItem((short) id);
        item.itemOptions.clear(); // This fixture explicitly supplies the exact conversion input.
        item.itemOptions.add(new Item.ItemOption(21, 80)); // deliberately first, not base stat
        int base = switch (item.template.type) { case 0 -> 47; case 1 -> 22;
            case 2 -> 0; case 3 -> 23; default -> 14; };
        item.itemOptions.add(new Item.ItemOption(base, 100));
        if (item.template.type == 1) item.itemOptions.add(new Item.ItemOption(27, 10));
        if (item.template.type == 3) item.itemOptions.add(new Item.ItemOption(28, 20));
        return item;
    }
    private static void preview(Player p, boolean gems) {
        NPC.menu = -1;
        if (gems) ChuyenHoaTrangBi_Ngoc.chuyenHoaTrangBiNgoc(p);
        else ChuyenHoaTrangBi_Vang.chuyenHoaTrangBiVang(p);
    }
    private static void convert(Player p, boolean gems) {
        if (gems) ChuyenHoaTrangBi_Ngoc.thucHienChuyenHoa(p);
        else ChuyenHoaTrangBi_Vang.thucHienChuyenHoa(p);
    }
    private static void success(boolean gems) throws Exception {
        Player p = player(264, 566, gems);
        preview(p, gems); check(NPC.menu == ConstNpc.MENU_START_COMBINE, "Tier 11 +4 opens preview");
        check(NPC.text.contains(gems ? "5.000 ngọc" : "2 tỷ vàng"), "Correct preview price");
        convert(p, gems);
        check(p.inventory.itemsBag.size() == 2 && !p.inventory.itemsBag.get(0).isNotNullItem(), "Full bag consumes source safely");
        Item out = p.inventory.itemsBag.get(1);
        check(out.template.id == 566 && param(out, 72) == 4, "Divine output +4");
        check(param(out, 0) == 146 && param(out, 21) == 80, "Upgrade own base, retain strength requirement");
        check(out.itemOptions.stream().filter(o -> o.optionTemplate.id == 50).count() == 2
                && out.itemOptions.stream().filter(o -> o.optionTemplate.id == 50).mapToInt(o -> o.param).sum() == 18,
                "Keep separate percentage lines including crystal bonuses");
        check(param(out, 34) == 0, "Do not transfer set marker");
        check(param(out, 102) == (gems ? 5 : 0) && param(out, 107) == (gems ? 9 : 2), "Mode-specific stars/holes");
        check(p.inventory.gold == (gems ? 3_000_000_000L : 1_000_000_000L)
                && p.inventory.gem == (gems ? 1000 : 6000) && p.inventory.ruby == 7000, "Exact currency only");
        check(NPC.text.contains(option(out, 0).getOptionString()), "Preview matches resulting base stat");
        roundTrip(out);
        convert(p, gems);
        check(p.inventory.itemsBag.get(1) == out && p.inventory.gold == (gems ? 3_000_000_000L : 1_000_000_000L)
                && p.inventory.gem == (gems ? 1000 : 6000), "Repeated confirmation is inert");
    }
    private static void catalog() {
        int accepted = 0;
        for (Template.ItemTemplate s : Manager.ITEM_TEMPLATES) {
            if (s.type > 4 || s.level < 10 || s.level > 12) continue;
            Template.ItemTemplate t = Manager.ITEM_TEMPLATES.stream().filter(x -> x.id >= 555 && x.id <= 567
                    && x.type == s.type && x.gender == s.gender).findFirst().orElseThrow();
            for (boolean gems : new boolean[]{false, true}) {
                Player p = player(s.id, t.id, gems); preview(p, gems);
                check((NPC.menu == ConstNpc.MENU_START_COMBINE) == (s.level >= 11), "Actual SQL tier " + s.id);
                if (s.level >= 11) accepted++;
            }
        }
        check(accepted == 52, "26 source templates, both modes, all 13 divine destinations");
    }
    private static void guards() {
        for (boolean gems : new boolean[]{false,true}) {
            for (String kind : new String[]{"low", "money", "removed", "changed", "same", "planet", "type", "source-divine", "target-level", "target-stars", "trade", "unconfirmed"}) {
                Player p = player(264,566,gems); Item s=p.inventory.itemsBag.get(0), t=p.inventory.itemsBag.get(1);
                if (kind.equals("low")) option(s,72).param=3;
                if (kind.equals("same")) p.combineNew.itemsCombine.set(1,s);
                if (kind.equals("planet")) t.template=Manager.ITEM_TEMPLATES.get(564);
                if (kind.equals("type")) t.template=Manager.ITEM_TEMPLATES.get(567);
                if (kind.equals("source-divine")) s.template=Manager.ITEM_TEMPLATES.get(566);
                if (kind.equals("target-level")) option(t,72).param=1;
                if (kind.equals("target-stars")) option(t,102).param=1;
                if (kind.equals("trade")) p.tradeWVP=true;
                if (!kind.equals("unconfirmed")) preview(p,gems);
                if (kind.equals("money")) { if(gems) p.inventory.gem=4999; else p.inventory.gold=1_999_999_999L; }
                if (kind.equals("removed")) p.inventory.itemsBag.set(0,ItemService.gI().createItemNull());
                if (kind.equals("changed")) option(s,50).param++;
                long gold=p.inventory.gold; int gem=p.inventory.gem;
                convert(p,gems);
                check(p.inventory.gold==gold && p.inventory.gem==gem && p.inventory.itemsBag.get(1)==t,
                        "Reject without loss: " + kind + " gems=" + gems);
            }
        }
    }
    private static void stats() {
        for (int[] pair : new int[][]{{244,556},{268,563},{280,561}}) {
            Player p=player(pair[0],pair[1],true); preview(p,true); convert(p,true);
            Item out=p.inventory.itemsBag.get(1);
            int base=out.template.type==1?22:out.template.type==3?23:14;
            check(param(out,base)==146, "Each base uses own parameter");
            if(base==14) check(out.itemOptions.stream().filter(o->o.optionTemplate.id==14).count()==1,"No duplicate critical base");
            if(base==22) check(param(out,27)==14,"HP recovery upgraded separately");
            if(base==23) check(param(out,28)==29,"KI recovery upgraded separately");
        }
    }
    private static void concurrent() throws Exception {
        Player p=player(264,566,true); preview(p,true);
        Thread a=new Thread(()->convert(p,true)), b=new Thread(()->convert(p,true));
        a.start(); b.start(); a.join(); b.join();
        check(p.inventory.gem==1000 && p.inventory.itemsBag.get(1).template.id==566,
                "Concurrent confirmation charges once");
        Player cancel=player(264,566,false); preview(cancel,false);
        ChuyenHoaTrangBiRules.cancel(cancel); convert(cancel,false);
        check(cancel.inventory.gold==3_000_000_000L && cancel.inventory.itemsBag.get(0).quantity==1,
                "Cancel discards confirmation without consuming items");
    }
    private static void activation() {
        int cases = 0;
        for (int id = 0; id < Manager.ITEM_OPTION_TEMPLATES.size(); id++) {
            if (nro.models.item.ActivationSet.primaryId(id) == -1) continue;
            for (boolean gems : new boolean[]{false, true}) {
                for (int sourceId : new int[]{264, 265}) {
                    for (int slot : new int[]{0, 1}) {
                        for (int param : new int[]{0, 1}) {
                            Player p = player(sourceId, 566, gems);
                            Item source = p.inventory.itemsBag.get(0), target = p.inventory.itemsBag.get(1);
                            p.inventory.itemsBag.get(slot).itemOptions.add(new Item.ItemOption(id, param));
                            preview(p, gems);
                            check(lastMessage.contains("kích hoạt"), "Explicit activation rejection message");
                            check(NPC.menu != ConstNpc.MENU_START_COMBINE,
                                    "Block activation marker/effect even at zero: " + id + " tier="
                                    + source.template.level + " slot=" + slot + " gems=" + gems);
                            convert(p, gems);
                            check(p.inventory.gold == 3_000_000_000L && p.inventory.gem == 6000
                                    && p.inventory.itemsBag.get(0) == source && source.quantity == 1
                                    && p.inventory.itemsBag.get(1) == target && target.quantity == 1,
                                    "Rejected activation must not consume money/items");
                            cases++;
                        }
                        Player changed = player(sourceId, 566, gems);
                        preview(changed, gems);
                        check(NPC.menu == ConstNpc.MENU_START_COMBINE, "Normal gear can preview");
                        Item original = changed.inventory.itemsBag.get(slot);
                        original.itemOptions.add(new Item.ItemOption(id, 0));
                        convert(changed, gems);
                        check(lastMessage.contains("kích hoạt"), "Commit rechecks activation before stale quote");
                        check(changed.inventory.itemsBag.get(slot) == original && original.quantity == 1
                                && changed.inventory.gold == 3_000_000_000L && changed.inventory.gem == 6000,
                                "Activation added after preview rejected at commit: " + id);
                        cases++;
                    }
                }
            }
        }
        check(cases > 500, "Every activation option, both modes/tiers/sides");
        System.out.println("Activation guard cases=" + cases);
    }
    private static void sqlRoundTrip() throws Exception {
        java.util.Properties config=new java.util.Properties();
        try(var reader=Files.newBufferedReader(Path.of("SRC/Config.properties"))){config.load(reader);}
        Class.forName(config.getProperty("database.driver"));
        String url="jdbc:mysql://"+config.getProperty("database.host")+":"+config.getProperty("database.port")
                +"/"+config.getProperty("database.name")+"?useSSL=false&useUnicode=true&characterEncoding=UTF-8";
        try(var connection=java.sql.DriverManager.getConnection(url,config.getProperty("database.user"),config.getProperty("database.pass"))){
            try(var statement=connection.createStatement()){
                statement.execute("CREATE TEMPORARY TABLE conversion_roundtrip AS SELECT id,data_inventory,items_bag FROM player WHERE 1=0");
            }
            for(boolean gems:new boolean[]{false,true}){
                Player p=player(264,566,gems); preview(p,gems);convert(p,gems);
                JSONArray bag=new JSONArray();
                for(Item item:p.inventory.itemsBag){
                    JSONArray row=new JSONArray(),options=new JSONArray();
                    for(Item.ItemOption o:item.itemOptions){JSONArray pair=new JSONArray();pair.add(o.optionTemplate.id);pair.add(o.param);options.add(pair.toJSONString());}
                    row.add(item.isNotNullItem()?item.template.id:-1);row.add(item.quantity);row.add(options.toJSONString());row.add(item.createTime);bag.add(row.toJSONString());
                }
                JSONArray money=new JSONArray();money.add(p.inventory.gold);money.add(p.inventory.gem);money.add(p.inventory.ruby);money.add(p.inventory.coupon);money.add(p.inventory.event);
                int id=gems?2:1;
                try(var insert=connection.prepareStatement("INSERT INTO conversion_roundtrip (id,data_inventory,items_bag) VALUES (?,'[]','[]')")){insert.setInt(1,id);insert.executeUpdate();}
                // Same money/bag column bindings used by PlayerDAO.updatePlayer, temporary rows only.
                try(var update=connection.prepareStatement("UPDATE conversion_roundtrip SET data_inventory=?,items_bag=? WHERE id=?")){
                    update.setString(1,money.toJSONString());update.setString(2,bag.toJSONString());update.setInt(3,id);check(update.executeUpdate()==1,"One SQL row saved");
                }
                try(var query=connection.prepareStatement("SELECT data_inventory,items_bag,JSON_VALID(data_inventory),JSON_VALID(items_bag) FROM conversion_roundtrip WHERE id=?")){
                    query.setInt(1,id);try(var rs=query.executeQuery()){check(rs.next(),"SQL reload row");check(rs.getInt(3)==1&&rs.getInt(4)==1,"SQL JSON valid");check(money.toJSONString().equals(rs.getString(1))&&bag.toJSONString().equals(rs.getString(2)),"SQL save/reload lossless");}
                }
                roundTrip(p.inventory.itemsBag.get(1));
            }
        }
    }
    private static void roundTrip(Item item) {
        JSONArray options=new JSONArray();
        for(Item.ItemOption o:item.itemOptions) { JSONArray pair=new JSONArray(); pair.add(o.optionTemplate.id);pair.add(o.param);options.add(pair.toJSONString()); }
        JSONArray row=new JSONArray();row.add(item.template.id);row.add(item.quantity);row.add(options.toJSONString());row.add(item.createTime);
        JSONArray loaded=(JSONArray)JSONValue.parse(row.toJSONString());
        Item restored=ItemService.gI().createNewItem(Short.parseShort(loaded.get(0).toString()),Integer.parseInt(loaded.get(1).toString()));
        restored.itemOptions.clear(); // Saved options replace fresh-item defaults, as in the DAO.
        JSONArray opts=(JSONArray)JSONValue.parse(loaded.get(2).toString().replaceAll("\"", ""));
        for(Object v:opts){JSONArray pair=(JSONArray)JSONValue.parse(v.toString()); restored.itemOptions.add(new Item.ItemOption(Integer.parseInt(pair.get(0).toString()),Integer.parseInt(pair.get(1).toString())));}
        restored.createTime=Long.parseLong(loaded.get(3).toString());
        check(NangSKHVip.state(item).equals(NangSKHVip.state(restored)), "Existing DAO JSON format preserves duplicate options");
    }
    private static Item.ItemOption option(Item item,int id){return item.itemOptions.stream().filter(o->o.optionTemplate.id==id).findFirst().orElseThrow();}
    private static int param(Item item,int id){return item.itemOptions.stream().filter(o->o.optionTemplate.id==id).mapToInt(o->o.param).sum();}
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    private static void replace(Class<?> c,String name,Object value)throws Exception{Field f=c.getDeclaredField(name);f.setAccessible(true);f.set(null,value);}
    private static final class RecordingNpc extends Npc {
        int menu=-1; String text="";
        RecordingNpc(){super(5,1,0,0,ConstNpc.BA_HAT_MIT,0);}
        @Override public void confirmMenu(Player p,int i){}
        @Override public void createOtherMenu(Player p,int i,String t,String... choices){menu=i;text=t; p.idMark.setIndexMenu(i);}
    }
}
