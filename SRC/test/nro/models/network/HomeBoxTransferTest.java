package nro.models.network;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import nro.models.item.Item;
import nro.models.player.NPoint;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.InventoryService;
import sun.misc.Unsafe;

/** Replays inventory packets into the same slot model used by both client tabs. */
public final class HomeBoxTransferTest {
    static final class Owner extends Player {
        int[] visibleBox = new int[200];
        int[] visibleBag = new int[200];
        int boxUpdates;
        List<Integer> commands = new ArrayList<>();

        Owner() {
            // Stat recalculation is unrelated to inventory synchronization.
            nPoint = new NPoint(this) { @Override public void calPoint() { } };
            for (int i = 0; i < 200; i++) {
                inventory.itemsBox.add(new Item());
                inventory.itemsBag.add(new Item());
            }
            for (int i = 0; i < 13; i++) inventory.itemsBody.add(new Item());
        }

        @Override public void sendMessage(Message message) {
            commands.add((int) message.command);
            if (message.command != -35 && message.command != -36) return;
            try {
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(message.getData()));
                check(in.readByte() == 0, "Full inventory update action");
                int[] slots = new int[in.readUnsignedByte()];
                for (int i = 0; i < slots.length; i++) {
                    int template = in.readShort();
                    if (template == -1) continue;
                    slots[i] = in.readInt();
                    in.readUTF();
                    in.readUTF();
                    int options = in.readUnsignedByte();
                    for (int j = 0; j < options; j++) {
                        in.readUnsignedByte();
                        if (session.itemOptionsInt32) in.readInt(); else in.readUnsignedShort();
                    }
                }
                check(in.available() == 0, "Packet alignment");
                if (message.command == -35) { visibleBox = slots; boxUpdates++; }
                else visibleBag = slots;
            } catch (Exception e) { throw new AssertionError("Cannot decode inventory packet", e); }
        }
    }

    static void check(boolean value, String reason) {
        if (!value) throw new AssertionError(reason);
    }

    static Item item(int id, int quantity) {
        Item item = new Item((short) id);
        item.quantity = quantity;
        item.itemOptions.add(new Item.ItemOption(73, 0));
        return item;
    }

    public static void main(String[] args) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);
        for (int id = 0; id <= 935; id++) {
            Template.ItemTemplate template = new Template.ItemTemplate();
            template.id = (short) id;
            template.name = "Item " + id;
            template.type = (byte) (id == 900 ? 0 : 27);
            template.gender = 3;
            template.isUpToUp = true;
            Manager.ITEM_TEMPLATES.add(template);
        }
        for (int id = 0; id < 256; id++)
            Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id, "Count #", 0));
        for (boolean extended : new boolean[] {false, true}) {
            Owner player = new Owner();
            player.session = (MySession) unsafe.allocateInstance(MySession.class);
            player.session.itemOptionsInt32 = extended;
            player.session.version = 248;
            int slot = 199;
            String mode = args[0];
            Item source = item(mode.equals("equipment") ? 900 : 933, 7);
            if (!mode.equals("empty")) {
                player.inventory.itemsBox.set(slot, source);
                player.visibleBox[slot] = source.quantity;
            }
            if (mode.equals("stack")) player.inventory.itemsBag.set(0, item(933, 11));
            if (mode.equals("blocked")) {
                for (int i = 0; i < 200; i++) player.inventory.itemsBag.set(i, item(934, 1));
            }
            InventoryService.gI().itemBoxToBodyOrBag(player, slot);
            if (mode.equals("blocked") || mode.equals("empty")) {
                check(player.boxUpdates == 0, "No box update for unsuccessful transfer");
                check(player.visibleBox[slot] == (mode.equals("empty") ? 0 : 7), "Source retained");
                check(mode.equals("empty") || source.quantity == 7, "No quantity lost on rejection");
            } else {
                check(!player.inventory.itemsBox.get(slot).isNotNullItem(), "Server box slot cleared");
                if (mode.equals("equipment")) {
                    check(player.inventory.itemsBody.get(0) == source, "Equipment moved to body");
                    check(player.commands.contains(-37), "Body update sent");
                } else {
                    int expected = mode.equals("stack") ? 18 : 7;
                    check(player.inventory.itemsBag.get(0).quantity == expected, "Correct bag quantity");
                    check(player.visibleBag[0] == expected, "Client bag synchronized");
                }
                check(player.boxUpdates == 1, "Transfer must send one box refresh; stale client icon remains");
                check(player.visibleBox.length == 200 && player.visibleBox[slot] == 0, "Client box icon removed");
            }
        }
        System.out.println("PASS: home box transfer " + args[0] + " (legacy + IOP32, slot 199)");
    }
}
