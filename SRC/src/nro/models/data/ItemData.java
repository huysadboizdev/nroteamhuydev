package nro.models.data;

import nro.models.player_system.Template.ArrHead2Frames;
import nro.models.player_system.Template.ItemOptionTemplate;
import nro.models.server.Manager;
import nro.models.network.Message;
import nro.models.network.MySession;
import nro.models.player_system.Template;

public class ItemData {

    public static void updateItem(MySession session) {
        try {
            // Validate complete catalog before sending any partial cache update.
            Message[] catalog = catalogMessages();
            updateItemOptionItemplate(session);
            updateItemArrHead2FTemplate(session);
            for (Message message : catalog) {
                session.doSendMessage(message);
                message.cleanup();
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Unable to encode item catalog", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to send item catalog", e);
        }
    }

    private static Message[] catalogMessages() throws java.io.IOException {
        int count = Manager.ITEM_TEMPLATES.size();
        if (count > Short.MAX_VALUE) throw new IllegalArgumentException("Item catalog exceeds ID range");
        byte[][] rows = new byte[count][];
        long[] prefix = new long[count + 1];
        for (int i = 0; i < count; i++) {
            var bytes = new java.io.ByteArrayOutputStream();
            var out = new java.io.DataOutputStream(bytes);
            writeTemplate(out, Manager.ITEM_TEMPLATES.get(i));
            rows[i] = bytes.toByteArray();
            prefix[i + 1] = prefix[i] + rows[i].length;
        }
        int split = -1;
        long best = Long.MAX_VALUE;
        for (int i = 0; i <= count; i++) {
            long first = 5 + prefix[i], second = 7 + prefix[count] - prefix[i];
            if (first <= 65535 && second <= 65535 && Math.max(first, second) < best) {
                split = i;
                best = Math.max(first, second);
            }
        }
        if (split < 0) throw new IllegalArgumentException("Item catalog cannot fit two 65535-byte cache frames");
        Message first = new Message(-28), second = new Message(-28);
        first.writer().writeByte(8); first.writer().writeByte(DataGame.vsItem);
        first.writer().writeByte(1); first.writer().writeShort(split);
        for (int i = 0; i < split; i++) first.writer().write(rows[i]);
        second.writer().writeByte(8); second.writer().writeByte(DataGame.vsItem);
        second.writer().writeByte(2); second.writer().writeShort(split); second.writer().writeShort(count);
        for (int i = split; i < count; i++) second.writer().write(rows[i]);
        return new Message[]{first, second};
    }

    private static void writeTemplate(java.io.DataOutputStream out, Template.ItemTemplate t) throws java.io.IOException {
        out.writeByte(t.type); out.writeByte(t.gender);
        out.writeUTF(t.name); out.writeUTF(t.description);
        out.writeByte(t.level); out.writeInt(t.strRequire);
        out.writeShort(t.iconID); out.writeShort(t.part); out.writeBoolean(t.isUpToUp);
    }

    private static void updateItemOptionItemplate(MySession session) {
        Message msg;
        try {
            msg = new Message(-28);
            msg.writer().writeByte(8);
            msg.writer().writeByte(DataGame.vsItem); //vcitem
            msg.writer().writeByte(0); //update option
            msg.writer().writeByte(Manager.ITEM_OPTION_TEMPLATES.size());
            for (ItemOptionTemplate io : Manager.ITEM_OPTION_TEMPLATES) {
                msg.writer().writeUTF(io.name);
                msg.writer().writeByte(io.type);
            }
            session.doSendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateItemArrHead2FTemplate(MySession session) {
        Message msg;
        try {
            msg = new Message(-28);
            msg.writer().writeByte(8);
            msg.writer().writeByte(DataGame.vsItem); //vcitem
            msg.writer().writeByte(100);
            msg.writer().writeShort(Manager.ARR_HEAD_2_FRAMES.size());
            for (ArrHead2Frames io : Manager.ARR_HEAD_2_FRAMES) {
                msg.writer().writeByte(io.frames.size());
                for (int i : io.frames) {
                    msg.writer().writeShort(i);
                }
            }
            session.doSendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
