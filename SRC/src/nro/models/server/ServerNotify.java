package nro.models.server;

import nro.models.player.Player;
import nro.models.network.Message;
import nro.models.services.Service;
import nro.models.utils.Util;
import java.util.ArrayList;
import java.util.List;
import nro.models.utils.Functions;

/**
 *
 * @author By Mr Blue
 * 
 */

public class ServerNotify extends Thread {

    private byte[] gk = new byte[]{67, 104, -61, -96, 111, 32, 109, -31, -69, -85,
        110, 103, 32, 98, -31, -70, -95, 110, 32, -60, -111, -61, -93, 32, 116, -31,
        -69, -101, 105, 32, 118, -31, -69, -101, 105, 32, 109, -61, -95, 121, 32,
        99, 104, -31, -69, -89, 32, 71, 105, 114, 108, 107, 117, 110, 55, 53, 46,
        32, 67, 104, -61, -70, 99, 32, 99, -61, -95, 99, 32, 98, -31, -70, -95,
        110, 32, 99, 104, -58, -95, 105, 32, 103, 97, 109, 101, 32, 118, 117,
        105, 32, 118, -31, -70, -69, 46, 46};
    private long lastTimeGK;

    private final List<String> notifies;

    private static ServerNotify i;

    private ServerNotify() {
        this.notifies = java.util.Collections.synchronizedList(new ArrayList<>());
        this.start();
    }

    public static synchronized ServerNotify gI() {
        if (i == null) {
            i = new ServerNotify();
        }
        return i;
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                while (!notifies.isEmpty()) {
                    ThongBao(notifies.remove(0));
                }
                if (Util.canDoWithTime(this.lastTimeGK, 500000)) {
                    ThongBao("Chào mừng bạn đã đến server Ngọc Rồng Online");
                    this.lastTimeGK = System.currentTimeMillis();
                }
            } catch (Exception ignored) {

            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private void ThongBao(String text) {
        Message msg;
        try {
            msg = new Message(93);
            msg.writer().writeUTF(text);
            Service.gI().sendMessAllPlayer(msg);
            msg.cleanup();
        } catch (Exception e) {
        }
    }

    public void notify(String text) {
        this.notifies.add(text);
    }

    public static final int SCREEN_COMMAND = 115;
    public static final int SCREEN_SCHEMA = 1;
    public static final int SCREEN_VERSION = 2;

    public record BossScreenEvent(String eventKey, int semanticBossId, int form, int mapId, int mapType,
            String ticker, boolean hiddenFromPanel) {
    }

    public static nro.models.network.Message buildScreenMessage(String eventKey, int semanticBossId, int form,
            int mapId, int mapType, String ticker, boolean hiddenFromPanel) throws java.io.IOException {
        return buildScreenMessageWithVersion(SCREEN_SCHEMA, SCREEN_VERSION, eventKey, semanticBossId, form, mapId,
                mapType, ticker, hiddenFromPanel);
    }

    public static nro.models.network.Message buildScreenMessageWithVersion(int schema, int version, String eventKey,
            int semanticBossId, int form, int mapId, int mapType, String ticker, boolean hiddenFromPanel)
            throws java.io.IOException {
        nro.models.network.Message message = new nro.models.network.Message(SCREEN_COMMAND);
        message.writer().writeByte(schema);
        message.writer().writeByte(version);
        message.writer().writeUTF(eventKey == null ? "" : eventKey);
        message.writer().writeInt(semanticBossId);
        message.writer().writeInt(form);
        message.writer().writeInt(mapId);
        message.writer().writeInt(mapType);
        message.writer().writeUTF(ticker == null ? "" : ticker);
        message.writer().writeBoolean(hiddenFromPanel);
        return message;
    }

    public static BossScreenEvent decodeScreenMessage(byte[] data) {
        try {
            java.io.DataInputStream reader = new java.io.DataInputStream(new java.io.ByteArrayInputStream(data));
            int schema = reader.readUnsignedByte();
            int version = reader.readUnsignedByte();
            if (schema != SCREEN_SCHEMA || version != SCREEN_VERSION) {
                return null;
            }
            String eventKey = reader.readUTF();
            int semanticBossId = reader.readInt();
            int form = reader.readInt();
            int mapId = reader.readInt();
            int mapType = reader.readInt();
            String ticker = reader.readUTF();
            boolean hiddenFromPanel = reader.readBoolean();
            return new BossScreenEvent(eventKey, semanticBossId, form, mapId, mapType, ticker, hiddenFromPanel);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void notifyBossScreen(String eventKey, int semanticBossId, int form, int mapId, int mapType,
            String ticker, boolean hiddenFromPanel) {
        nro.models.network.Message message = null;
        try {
            message = buildScreenMessage(eventKey, semanticBossId, form, mapId, mapType, ticker, hiddenFromPanel);
            Service.gI().sendMessAllPlayer(message);
        } catch (Exception ignored) {
        } finally {
            if (message != null) {
                message.cleanup();
            }
        }
    }

     public void sendNotifyTab(Player player) {
        Message msg;
        try {
            msg = new Message(50);
            java.util.List<String> snapshot=Manager.NOTIFY;
            int count=Math.min(127,snapshot.size());
            msg.writer().writeByte(count);
            for (int i = 0; i < count; i++) {
                String[] arr = snapshot.get(i).split("<>",2);
                msg.writer().writeShort(i);
                msg.writer().writeUTF(arr[0]);
                msg.writer().writeUTF(arr.length>1?arr[1]:"");
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception ignored) {
        }
    }
}
