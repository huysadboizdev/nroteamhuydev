package nro.models.npc_list;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import nro.models.consts.ConstNpc;
import nro.models.map.Map;
import nro.models.map.Zone;
import nro.models.network.Message;
import nro.models.network.MySession;
import nro.models.npc.Npc;
import nro.models.npc.NpcFactory;
import nro.models.player.IDMark;
import nro.models.player.Location;
import nro.models.player.Player;
import nro.models.player_system.Template.NpcTemplate;
import nro.models.server.Manager;
import sun.misc.Unsafe;

/** Runs real factory, map initialization, menus and ranking packet routing without a live database. */
public final class DaishinkanNpcTest {
    private static final Unsafe UNSAFE;
    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            UNSAFE = (Unsafe) field.get(null);
        } catch (Exception failure) { throw new ExceptionInInitializerError(failure); }
    }
    public static class CapturePlayer extends Player {
        final List<Integer> commands = new ArrayList<>();
        final List<byte[]> payloads = new ArrayList<>();
        @Override public void sendMessage(Message message) {
            commands.add((int) message.command);
            payloads.add(message.getData());
        }
        void clear() { commands.clear(); payloads.clear(); }
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    private static Map map(int id) throws Exception {
        Map map = (Map) UNSAFE.allocateInstance(Map.class);
        map.mapId = id;
        map.npcs = new ArrayList<>();
        Manager.MAPS.add(map);
        return map;
    }
    private static CapturePlayer player(Map map) throws Exception {
        CapturePlayer player = new CapturePlayer();
        player.session = (MySession) UNSAFE.allocateInstance(MySession.class);
        player.session.version = 220;
        player.zone = (Zone) UNSAFE.allocateInstance(Zone.class);
        player.zone.map = map;
        player.idMark = new IDMark();
        player.location = new Location();
        player.location.x = 100;
        player.location.y = 100;
        return player;
    }
    private static void templates() {
        Manager.NPC_TEMPLATES.clear();
        for (int id : new int[]{ConstNpc.BO_MONG, ConstNpc.DAU_THAN, 111}) {
            NpcTemplate template = new NpcTemplate();
            template.id = id;
            template.avatar = 123;
            Manager.NPC_TEMPLATES.add(template);
        }
    }
    private static void home() throws Exception {
        templates();
        for (int id : new int[]{21, 22, 23, 0, 7, 14, 24}) {
            Map map = map(id);
            map.initNpc(new byte[]{ConstNpc.BO_MONG, ConstNpc.DAU_THAN},
                    new short[]{100, 200}, new short[]{100, 200});
            boolean home = id >= 21 && id <= 23;
            check(map.npcs.size() == (home ? 1 : 2), "Bo Mong suppressed only in homes: " + id);
            check(map.npcs.stream().noneMatch(npc -> npc == null), "map NPC list has no null");
            check(map.npcs.stream().anyMatch(npc -> npc.tempId == ConstNpc.DAU_THAN), "other home NPC retained");
            check(map.npcs.stream().anyMatch(npc -> npc.tempId == ConstNpc.BO_MONG) == !home, "Bo Mong elsewhere retained");
        }
    }
    private static void installEmptyRankings() throws Exception {
        Class<?> service = Class.forName("nro.models.services.VillageRankingService");
        Class<?> source = Class.forName("nro.models.services.VillageRankingService$Source");
        Object rows = Proxy.newProxyInstance(source.getClassLoader(), new Class<?>[]{source},
                (proxy, method, args) -> List.of());
        Object instance = service.getConstructor(source, LongSupplier.class)
                .newInstance(rows, (LongSupplier) () -> 1000L);
        Field singleton = service.getDeclaredField("INSTANCE");
        UNSAFE.putObject(UNSAFE.staticFieldBase(singleton), UNSAFE.staticFieldOffset(singleton), instance);
    }
    private static void menu() throws Exception {
        templates();
        Class<?> type;
        try { type = Class.forName("nro.models.npc_list.Daishinkan"); }
        catch (ClassNotFoundException missing) { throw new AssertionError("Daishinkan NPC feature missing"); }
        check(ConstNpc.class.getField("DAISHINKAN").getInt(null) == 111, "unique template ID 111");
        installEmptyRankings();
        for (int id : new int[]{0, 7, 14}) {
            Map map = map(id);
            Npc npc = NpcFactory.createNPC(id, 1, 100, 100, 111);
            check(type.isInstance(npc), "factory creates actual Daishinkan");
            CapturePlayer player = player(map);
            npc.openBaseMenu(player);
            check(player.commands.equals(List.of(32)), "real menu packet emitted");
            DataInputStream packet = new DataInputStream(new ByteArrayInputStream(player.payloads.get(0)));
            check(packet.readShort() == 111, "packet template id");
            packet.readUTF();
            check(packet.readUnsignedByte() == 3, "exactly three choices");
            check(packet.readUTF().equals("Top sức mạnh"), "power choice");
            check(packet.readUTF().equals("Top nhiệm vụ"), "task choice");
            check(packet.readUTF().equals("Đóng") && packet.available() == 0, "close choice / complete payload");
            check(player.idMark.isBaseMenu(), "base menu state");
            for (int selection : new int[]{0, 1}) {
                player.clear();
                npc.confirmMenu(player, selection);
                check(player.commands.equals(List.of(-96)), "selection routes ranking packet: " + selection);
                packet = new DataInputStream(new ByteArrayInputStream(player.payloads.get(0)));
                packet.readByte();
                check(packet.readUTF().equals(selection == 0 ? "Top sức mạnh" : "Top nhiệm vụ"), "correct category title");
            }
            for (int selection : new int[]{-1, 2, 3, Integer.MAX_VALUE}) {
                player.clear();
                npc.confirmMenu(player, selection);
                check(player.commands.isEmpty(), "invalid / close selection has no ranking");
            }
            player.idMark.setIndexMenu(12345);
            player.clear();
            npc.confirmMenu(player, 0);
            check(player.commands.isEmpty(), "stale menu rejected");
            player.location.x = 10000;
            player.idMark.setIndexMenu(ConstNpc.BASE_MENU);
            npc.openBaseMenu(player);
            check(player.commands.equals(List.of(32)), "village interaction preserves existing distance policy");
            player.zone.map = map(24);
            player.clear();
            npc.openBaseMenu(player);
            npc.confirmMenu(player, 0);
            check(!player.commands.contains(32) && !player.commands.contains(-96), "wrong player map rejected");
        }
        Map other = map(24);
        CapturePlayer player = player(other);
        Npc npc = NpcFactory.createNPC(24, 1, 100, 100, 111);
        npc.openBaseMenu(player);
        player.idMark.setIndexMenu(ConstNpc.BASE_MENU);
        npc.confirmMenu(player, 0);
        check(player.commands.isEmpty(), "Daishinkan disabled outside villages");
    }
    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("home")) home(); else menu();
        System.out.println("PASS DaishinkanNpcTest " + (args.length == 0 ? "menu" : args[0]));
    }
}
