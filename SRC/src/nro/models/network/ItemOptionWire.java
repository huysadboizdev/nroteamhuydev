package nro.models.network;

import java.io.DataOutputStream;
import java.io.IOException;
import nro.models.player.Player;

/** Item option width is negotiated per recipient, not inferred from game version. */
public final class ItemOptionWire {
    public static final String CAPABILITY = "IOP32";
    public static final int ACK_SUBCOMMAND = 100;
    private ItemOptionWire() { }

    public static void write(Message message, Player recipient, int param) throws IOException {
        boolean extended = recipient != null && recipient.getSession() != null
                && recipient.getSession().itemOptionsInt32;
        write(message.writer(), extended, param);
    }

    public static void write(DataOutputStream writer, boolean extended, int param) throws IOException {
        if (extended) {
            writer.writeInt(param);
        } else {
            // Legacy clients cannot represent large values; avoid wrapping display values.
            writer.writeShort(Math.max(0, Math.min(param, 65535)));
        }
    }

    public static void writePickupQuantity(Message message, Player recipient, int quantity) throws IOException {
        boolean extended = recipient != null && recipient.getSession() != null
                && recipient.getSession().itemOptionsInt32;
        writePickupQuantity(message.writer(), extended, quantity);
    }

    public static void writePickupQuantity(DataOutputStream writer, boolean extended, int quantity) throws IOException {
        writer.writeShort(quantity > Short.MAX_VALUE ? 9999 : quantity);
        if (extended) {
            writer.writeInt(quantity);
        }
    }
}
