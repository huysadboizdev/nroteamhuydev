package nro.models.data;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import nro.models.network.Message;
import nro.models.network.MySession;
import nro.models.player_system.Template.NpcTemplate;
import nro.models.server.Manager;
import sun.misc.Unsafe;

/** Existing clients must refresh the cached NPC catalog and receive template 111. */
public final class DaishinkanCatalogTest {
    static class Session extends MySession {
        byte[] payload;
        private Session() throws Exception { super(null); }
        @Override public void sendMessage(Message message) { payload = message.getData(); }
    }
    public static void main(String[] args) throws Exception {
        if (DataGame.vsMap <= 5) throw new AssertionError("NPC catalog version must invalidate version 5 cache");
        var field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);
        Session session = (Session) unsafe.allocateInstance(Session.class);
        Manager.NPC_TEMPLATES.clear(); Manager.MOB_TEMPLATES.clear();
        Manager.MAP_TEMPLATES = new nro.models.player_system.Template.MapTemplate[0];
        NpcTemplate npc = new NpcTemplate(); npc.id = 111; npc.name = "Daishinkan";
        npc.head = 1; npc.body = 2; npc.leg = 3;
        Manager.NPC_TEMPLATES.add(npc);
        DataGame.updateMap(session);
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(session.payload));
        if (in.readByte() != 6 || in.readByte() != DataGame.vsMap) throw new AssertionError("Wire catalog version");
        int maps = in.readUnsignedByte(); for (int i = 0; i < maps; i++) in.readUTF();
        if (in.readUnsignedByte() != 112) throw new AssertionError("Sparse catalog must include slot111");
        for (int i = 0; i < 112; i++) {
            String name = in.readUTF(); short head = in.readShort(); in.readShort(); in.readShort();
            if (in.readUnsignedByte() != 0) throw new AssertionError("Unexpected template menu");
            if (i == 111 && (!name.equals("Daishinkan") || head != 1)) throw new AssertionError("Wrong new NPC slot");
        }
        if (in.readUnsignedByte() != 0 || in.available() != 0) throw new AssertionError("Malformed catalog packet");
        System.out.println("DAISHINKAN_CATALOG PASS cache-version,sparse-template111,packet");
    }
}
