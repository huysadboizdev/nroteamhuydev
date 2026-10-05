package nro.models.network;

import nro.models.network.Message;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import nro.models.interfaces.IMessageSendCollect;
import nro.models.interfaces.ISession;

public class MessageSendCollect implements IMessageSendCollect {

    private int curR = 0;
    private int curW = 0;

    @Override
    public Message readMessage(ISession session, DataInputStream dis) throws Exception {
        int size;
        byte cmd = dis.readByte();
        if (session.sentKey()) {
            cmd = this.readKey(session, cmd);
        }
        if (session.sentKey()) {
            byte b1 = dis.readByte();
            byte b2 = dis.readByte();
            size = (this.readKey(session, b1) & 0xFF) << 8 | this.readKey(session, b2) & 0xFF;
        } else {
            size = dis.readUnsignedShort();
        }
        byte[] data = new byte[size];
        int len = 0;
        for (int byteRead = 0; len != -1 && byteRead < size; byteRead += len) {
            len = dis.read(data, byteRead, size - byteRead);
        }
        if (session.sentKey()) {
            for (int i = 0; i < data.length; ++i) {
                data[i] = this.readKey(session, data[i]);
            }
        }
        return new Message(cmd, data);
    }

    @Override
    public byte readKey(ISession session, byte b) {
        byte i = (byte) (session.getKey()[this.curR++] & 0xFF ^ b & 0xFF);
        if (this.curR >= session.getKey().length) {
            this.curR %= session.getKey().length;
        }
        return i;
    }

    @Override
    public void doSendMessage(ISession session, DataOutputStream dos, Message msg) throws Exception {
        byte[] original = msg.getData();
        int size = original == null ? 0 : original.length;
        boolean existingLarge = msg.command == -32 || msg.command == -66 || msg.command == -74
                || msg.command == 11 || msg.command == -67 || msg.command == -87 || msg.command == 66;
        boolean inventoryLarge = session.supportsInventory200()
                && (msg.command == -30 || msg.command == -35 || msg.command == -36 || msg.command == -86 || msg.command == -44);
        boolean large = existingLarge || inventoryLarge;
        // Validate before writing anything so rejection cannot desynchronize the stream.
        if (size > (large ? 0xFFFFFF : 0xFFFF)) throw new IllegalArgumentException("Packet exceeds frame capacity");
        dos.writeByte(session.sentKey() ? writeKey(session, msg.command) : msg.command);
        if (large) {
            for (int shift = 0; shift < 24; shift += 8) {
                byte value = (byte) (size >> shift);
                // Preserve historical resource frames; negotiated inventory also supports plaintext.
                if (session.sentKey() || existingLarge) value = writeKey(session, value);
                dos.writeByte(value - 128);
            }
        } else if (session.sentKey()) {
            dos.writeByte(writeKey(session, (byte) (size >> 8)));
            dos.writeByte(writeKey(session, (byte) size));
        } else {
            dos.writeShort(size);
        }
        if (original != null) {
            byte[] data = original.clone();
            if (session.sentKey()) for (int i = 0; i < data.length; i++) data[i] = writeKey(session, data[i]);
            dos.write(data);
        }
        dos.flush();
        msg.cleanup();
    }

    @Override
    public byte writeKey(ISession session, byte b) {
        byte i = (byte) (session.getKey()[this.curW++] & 0xFF ^ b & 0xFF);
        if (this.curW >= session.getKey().length) {
            this.curW %= session.getKey().length;
        }
        return i;
    }
}
