package nro.models.services_func;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.network.MySession;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import sun.misc.Unsafe;

/** Replays real client trade requests without a server, database, or background worker. */
public final class TradeProtocolTest {
    private static final class Owner extends Player {
        final List<byte[]> tradePackets = new ArrayList<>();
        @Override public void sendMessage(Message message) {
            if (message.command == -86) tradePackets.add(message.getData());
        }
    }

    private static Unsafe unsafe;
    private static TransactionService controller;

    public static void main(String[] args) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        unsafe = (Unsafe) field.get(null);
        Constructor<TransactionService> constructor = TransactionService.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        controller = constructor.newInstance(); // Avoid the singleton's background worker.
        for (int id = 0; id <= 73; id++)
            Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id, "Option #", 0));
        Owner left = owner(1, (args[0].equals("zero-stack") || args[0].equals("zero-remainder")) ? 2
                : args[0].equals("remove-stack") ? 250 : 1);
        Owner right = owner(2, 0);
        Trade trade = new Trade(left, right);
        switch (args[0]) {
            case "single":
                request(left, 2, 0, 0);
                check(TransactionService.isTrading(left), "Legacy single-item quantity zero must not cancel trade");
                check(offered(trade).size() == 1 && offered(trade).get(0).quantity == 1,
                        "Legacy single item is offered once");
                check(left.inventory.itemsBag.get(0).quantity == 1, "Live bag is unchanged before acceptance");
                break;
            case "zero-stack":
                request(left, 2, 0, 0);
                check(!TransactionService.isTrading(left), "Zero quantity for a multi-item stack stays rejected");
                check(left.inventory.itemsBag.get(0).quantity == 2, "Rejected request preserves bag");
                break;
            case "negative":
                request(left, 2, 0, -1);
                check(!TransactionService.isTrading(left), "Negative quantities cancel trade");
                check(left.inventory.itemsBag.get(0).quantity == 1, "Negative request preserves bag");
                break;
            case "zero-remainder":
                request(left, 2, 0, 1);
                request(left, 2, 0, 0);
                check(!TransactionService.isTrading(left), "Legacy zero cannot select a remainder of an original multi-item stack");
                check(left.inventory.itemsBag.get(0).quantity == 2, "Rejected remainder request preserves live bag");
                break;
            case "invalid-zero":
                request(left, 2, 250, 0);
                check(!TransactionService.isTrading(left), "Invalid legacy zero slot stays rejected");
                break;
            case "remove":
                request(left, 2, 0, 1);
                check(offered(trade).size() == 1, "Item offered before removal");
                request(left, 4, 0, null);
                check(offered(trade).isEmpty(), "Client action four removes the offered item");
                request(left, 2, 0, 1);
                check(offered(trade).size() == 1, "Removed item can be offered again without duplication");
                check(left.inventory.itemsBag.get(0).quantity == 1, "Editing offer preserves live bag");
                break;
            case "remove-stack":
                request(left, 2, 0, 200);
                check(offered(trade).size() == 3, "Stack split into three wire entries");
                request(left, 4, 0, null);
                check(offered(trade).isEmpty(), "Removing a slot removes all split entries");
                request(left, 2, 0, 250);
                check(offered(trade).stream().mapToInt(item -> item.quantity).sum() == 250,
                        "Original stack restored to the staging slot");
                check(left.inventory.itemsBag.get(0).quantity == 250, "Live stack unchanged");
                break;
            case "remove-duplicates":
                Item duplicate = nro.models.services.ItemService.gI().copyItem(left.inventory.itemsBag.get(0));
                duplicate.itemOptions.get(0).param = 1234;
                left.inventory.itemsBag.add(duplicate);
                trade.dispose();
                trade = new Trade(left, right);
                request(left, 2, 0, 1);
                request(left, 2, 1, 1);
                request(left, 4, 0, null);
                check(offered(trade).size() == 1 && offered(trade).get(0).itemOptions.get(0).param == 1234,
                        "Removal uses source slot rather than template ID");
                request(left, 2, 0, 1);
                check(offered(trade).size() == 2 && offered(trade).get(1).itemOptions.get(0).param == 0,
                        "Restored slot retains its original options");
                break;
            case "locked-remove":
                request(left, 2, 0, 1);
                request(left, 5, 0, null);
                request(left, 4, 0, null);
                check(offered(trade).size() == 1, "Locked offer cannot be removed");
                request(left, 2, 0, 1);
                check(offered(trade).size() == 1, "Locked offer cannot be changed or duplicated");
                break;
            default: throw new AssertionError("Unknown scenario");
        }
        if (TransactionService.isTrading(left)) trade.dispose();
        System.out.println("PASS: trade " + args[0]);
    }

    private static Owner owner(long id, int quantity) throws Exception {
        Owner player = new Owner();
        player.id = id;
        player.session = (MySession) unsafe.allocateInstance(MySession.class);
        player.session.actived = true;
        player.session.version = 248;
        player.inventory.itemsBag.add(new Item());
        if (quantity > 0) {
            Item item = new Item();
            Template.ItemTemplate template = new Template.ItemTemplate();
            template.id = 1; template.name = "Trade test item"; template.type = 0;
            item.template = template;
            item.quantity = quantity;
            item.itemOptions.add(new Item.ItemOption(73, 0));
            player.inventory.itemsBag.set(0, item);
        }
        return player;
    }

    private static void request(Player player, int action, int index, Integer quantity) throws Exception {
        Message outbound = new Message(-86);
        outbound.writer().writeByte(action);
        outbound.writer().writeByte(index);
        if (quantity != null) outbound.writer().writeInt(quantity);
        Message inbound = new Message((byte) -86, outbound.getData());
        controller.controller(player, inbound);
        outbound.cleanup(); inbound.cleanup();
    }

    @SuppressWarnings("unchecked")
    private static List<Item> offered(Trade trade) throws Exception {
        Field field = Trade.class.getDeclaredField("itemsTrade1");
        field.setAccessible(true);
        return (List<Item>) field.get(trade);
    }

    private static void check(boolean condition, String reason) {
        if (!condition) throw new AssertionError(reason);
    }
}
