package nro.models.network;
import java.io.DataInputStream;
import java.io.IOException;
public final class InventoryWire {
    public static final String CAPABILITY = "INV200";
    private InventoryWire() {}
    /** Decode and validate the whole combine selection before mutating selected items. */
    public static int[] readSlots(DataInputStream in, int capacity) throws IOException {
        int count = in.readUnsignedByte();
        if (count > capacity || in.available() != count) throw new IOException("Invalid slot selection payload");
        int[] result = new int[count]; boolean[] seen = new boolean[capacity];
        for (int i = 0; i < count; i++) {
            int slot = in.readUnsignedByte();
            if (slot >= capacity || seen[slot]) throw new IOException("Invalid or duplicate inventory slot");
            seen[slot] = true; result[i] = slot;
        }
        return result;
    }
}
