package nro.models.shop_ky_gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

import nro.models.consts.ConstNpc;
import nro.models.item.Item;
import nro.models.item.Item.ItemOption;
import nro.models.map.service.NpcService;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.services.InventoryService;
import nro.models.services.ItemService;
import nro.models.services.Service;

/**
 *
 * @author By Mr Blue
 * 
 */

public class ConsignShopService {

    private static ConsignShopService instance;
    protected ConsignTransactions transactions = new ConsignTransactions();
    private static final class View {
        int next=1;
        final Map<Integer,Integer> listingToWire=new HashMap<>();
        final Map<Integer,Viewed> wireToListing=new HashMap<>();
    }
    private record Viewed(int id,String fingerprint) {}
    private final Map<Player,View> views=Collections.synchronizedMap(new WeakHashMap<>());
    private static String fingerprint(ConsignItem it) {
        return it.itemId+":"+it.player_sell+":"+it.goldSell+":"+it.gemSell+":"+it.quantity
                +":"+it.currency+":"+it.createTime+":"+it.options.toString();
    }
    private int wireId(Player p,ConsignItem it) {
        synchronized(views) {
            View view=views.computeIfAbsent(p,key->new View());
            Integer wire=view.listingToWire.get(it.id);
            if(wire==null) {
                if(view.next>32767)throw new IllegalStateException("Reopen session to refresh consignment view IDs");
                wire=view.next++;view.listingToWire.put(it.id,wire);
            }
            view.wireToListing.put(wire,new Viewed(it.id,fingerprint(it)));return wire;
        }
    }
    private int lookupId(Player p,int wire) {
        synchronized(views) {
            View view=views.get(p);if(view==null)return 0;
            Viewed seen=view.wireToListing.get(wire);if(seen==null)return 0;
            for(ConsignItem item:ConsignShopManager.gI().listItem)
                if(item.id==seen.id && fingerprint(item).equals(seen.fingerprint))return item.id;
            return 0;
        }
    }

    public static ConsignShopService gI() {
        if (instance == null) {
            instance = new ConsignShopService();
        }
        return instance;
    }

    private List<ConsignItem> pageItems(Player pl, byte tab, int from, int to) {
        List<ConsignItem> all = getItemKyGui(pl, tab);
        if (from < 0 || to < from || from >= all.size()) return new ArrayList<>();
        return new ArrayList<>(all.subList(from, Math.min(to, all.size())));
    }

    private List<ConsignItem> getItemKyGui2(Player pl, byte tab, byte from, byte to) {
        return pageItems(pl, tab, from, to);
    }

    private List<ConsignItem> getItemKyGui(Player pl, byte tab, byte... max) {
        List<ConsignItem> filtered = ConsignShopManager.gI().listItem.stream()
                .filter(it -> it != null && it.tab == tab && !it.isBuy && it.player_sell != pl.id && "ruby".equals(it.currency))
                .sorted(Comparator.comparingInt((ConsignItem it) -> it.isUpTop).reversed())
                .collect(Collectors.toList());

        List<ConsignItem> result = new ArrayList<>();

        if (max.length == 2) {
            int from = max[0], to = max[1];
            for (int i = from; i < to && i < filtered.size(); i++) {
                result.add(filtered.get(i));
            }
        } else if (max.length == 1) {
            int limit = max[0];
            for (int i = 0; i < limit && i < filtered.size(); i++) {
                result.add(filtered.get(i));
            }
        } else {
            return filtered;
        }

        return result;
    }

    private List<ConsignItem> getItemKyGui() {
        return ConsignShopManager.gI().listItem.stream()
                .filter(it -> it != null && !it.isBuy)
                .sorted(Comparator.comparingInt((ConsignItem it) -> it.isUpTop).reversed())
                .collect(Collectors.toList());
    }

    private void finish(Player pl, ConsignTransactions.Result result) {
        if (pl == null) return;
        try { Service.gI().sendThongBao(pl, result.message()); } catch (Exception ignored) {}
        try { InventoryService.gI().sendItemBags(pl); } catch (Exception ignored) {}
        try { Service.gI().sendMoney(pl); } catch (Exception ignored) {}
        try { openShopKyGui(pl); } catch (Exception ignored) {}
    }

    public void buyItem(Player pl, int id) { finish(pl, transactions.buy(pl,lookupId(pl,id))); }

    public ConsignItem getItemBuy(int id) {
        for (ConsignItem it : getItemKyGui()) {
            if (it != null && it.id == id) {
                return it;
            }
        }
        return null;
    }

    public ConsignItem getItemBuy(Player pl, int id) {
        for (ConsignItem it : ConsignShopManager.gI().listItem) {
            if (it != null && it.id == id && it.player_sell == pl.id) {
                return it;
            }
        }
        return null;
    }

    public void openShopKyGui(Player pl, byte index, int page) {
        if (pl == null || index < 0 || index > 3 || page < 0 || page >= 127
                || page >= Math.max(1, (getItemKyGui(pl,index).size() + 19) / 20)) {
            return;
        }
        Message msg = null;
        try {
            msg = new Message(-100);
            msg.writer().writeByte(index);
            List<ConsignItem> items = getItemKyGui(pl, index);
            List<ConsignItem> itemsSend = pageItems(pl, index, page * 20, page * 20 + 20);
            byte tab = (byte) Math.min(127, Math.max(1, (items.size() + 19) / 20));
            msg.writer().writeByte(tab); // max page
            msg.writer().writeByte(page);
            msg.writer().writeByte(itemsSend.size());
            for (int j = 0; j < itemsSend.size(); j++) {
                ConsignItem itk = itemsSend.get(j);
                Item it = ItemService.gI().createNewItem(itk.itemId);
                it.itemOptions.clear();
                if (itk.options.isEmpty()) {
                    it.itemOptions.add(new ItemOption(73, 0));
                } else {
                    for (ItemOption option : itk.options) { if (option.optionTemplate.id != 86) it.itemOptions.add(new ItemOption(option)); }
                }
                msg.writer().writeShort(it.template.id);
                msg.writer().writeShort(itk.isUpTop == -1 ? itk.id : wireId(pl,itk));
                msg.writer().writeInt(-1);
                msg.writer().writeInt("ruby".equals(itk.currency) ? itk.gemSell : -1);
                msg.writer().writeByte(0); // buy type
                if (pl.getSession().version >= 222) {
                    msg.writer().writeInt(itk.quantity);
                } else {
                    msg.writer().writeByte(itk.quantity);
                }
                msg.writer().writeByte(itk.player_sell == pl.id ? 1 : 0); // isMe
                msg.writer().writeByte(it.itemOptions.size());
                for (int a = 0; a < it.itemOptions.size(); a++) {
                    msg.writer().writeByte(it.itemOptions.get(a).optionTemplate.id);
                    nro.models.network.ItemOptionWire.write(msg, pl, it.itemOptions.get(a).param);
                }
                msg.writer().writeByte(0);
            }
            pl.sendMessage(msg);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
                msg = null;
            }
        }
    }

    public void upItemToTop(Player pl, int id) {
        id=lookupId(pl,id);
        ConsignItem it = getItemBuy(pl,id);
        if (it == null || it.isBuy || !"ruby".equals(it.currency)) {
            Service.gI().sendThongBao(pl,"Tin đăng không hợp lệ."); return;
        }
        pl.idMark.setIdItemUpTop(id);
        NpcService.gI().createMenuConMeo(pl, ConstNpc.UP_TOP_ITEM, -1,
                "Đưa tin lên đầu: 5 hồng ngọc. Chỉ thu phí khi thực hiện thành công.", "Đồng ý", "Từ chối");
    }

    public void StartupItemToTop(Player pl) {
        int id = pl.idMark.getIdItemUpTop();
        pl.idMark.setIdItemUpTop(0);
        finish(pl, transactions.top(pl,id));
    }

    public void claimOrDel(Player pl, byte action, int id) {
        finish(pl, transactions.claimOrCancel(pl,action,lookupId(pl,id)));
    }

    public List<ConsignItem> getItemCanKiGui(Player pl) {
        List<ConsignItem> its = new ArrayList<>();
        ConsignShopManager.gI().listItem.stream().filter((it) -> (it != null && it.player_sell == pl.id)).forEachOrdered((it) -> {
            its.add(it);
        });
        pl.inventory.itemsBag.stream().filter((it) -> (itemCanConsign(it))).forEachOrdered((it) -> {
            List<ItemOption> options = new ArrayList<>(); for (ItemOption option : it.itemOptions) { if (option.optionTemplate.id != 86 && option.optionTemplate.id != 87) options.add(new ItemOption(option)); } options.add(new ItemOption(87, 1)); its.add(new ConsignItem(InventoryService.gI().getIndexBag(pl, it), it.template.id, (int) pl.id, (byte) 4, -1, -1, it.quantity, (byte) -1, options, false));
        });
        return its;
    }

    public boolean itemCanConsign(Item it) { return ConsignTransactions.eligible(it); }

    public int getMaxId() {
        try {
            List<Integer> id = new ArrayList<>();
            ConsignShopManager.gI().listItem.stream().filter((it) -> (it != null)).forEachOrdered((it) -> {
                id.add(it.id);
            });

            if (id.isEmpty()) {
                return 0;
            }

            return Collections.max(id);
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public byte getTabKiGui(Item it) {
        if (it.template.type >= 0 && it.template.type <= 2) {
            return 0;
        } else if ((it.template.type >= 3 && it.template.type <= 4)) {
            return 1;
        } else if (it.template.type == 29) {
            return 2;
        } else {
            return 3;
        }
    }

    public void KiGui(Player pl, int id, int money, byte moneyType, int quantity) {
        finish(pl, transactions.post(pl,id,money,moneyType,quantity));
    }

    public void openShopKyGui(Player pl) {
        if (pl == null || pl.session == null) return;
        Message msg = null;
        try {
            msg = new Message(-44);
            msg.writer().writeByte(2);
            msg.writer().writeByte(5);
            for (byte i = 0; i < 5; i++) {
                if (i == 4) {
                    List<ConsignItem> personalItems = getItemCanKiGui(pl);
                    boolean extendedCount = pl.getSession().inventory200;
                    if (personalItems.size() > (extendedCount ? 65535 : 255)) {
                        throw new IOException("Personal consignment item count exceeds protocol capacity");
                    }
                    msg.writer().writeUTF(ConsignShopManager.gI().tabName[i]);
                    msg.writer().writeByte(0);
                    if (extendedCount) msg.writer().writeShort(personalItems.size());
                    else msg.writer().writeByte(personalItems.size());
                    for (int j = 0; j < personalItems.size(); j++) {
                        ConsignItem itk = personalItems.get(j);
                        if (itk == null) {
                            continue;
                        }
                        Item it = ItemService.gI().createNewItem(itk.itemId);
                        it.itemOptions.clear();
                        if (itk.options.isEmpty()) {
                            it.itemOptions.add(new ItemOption(73, 0));
                        } else {
                            for (ItemOption option : itk.options) { if (option.optionTemplate.id != 86) it.itemOptions.add(new ItemOption(option)); }
                        }
                        msg.writer().writeShort(it.template.id);
                        msg.writer().writeShort(itk.isUpTop == -1 ? itk.id : wireId(pl,itk));
                        msg.writer().writeInt(-1);
                        msg.writer().writeInt("ruby".equals(itk.currency) ? itk.gemSell : -1);
                        if (itk.isUpTop == -1) {
                            msg.writer().writeByte(0); // buy type
                        } else if (itk.isBuy) {
                            msg.writer().writeByte(2);
                        } else {
                            msg.writer().writeByte(1);
                        }
                        msg.writer().writeInt(itk.quantity);
                        msg.writer().writeByte(1); // isMe
                        msg.writer().writeByte(it.itemOptions.size());
                        for (int a = 0; a < it.itemOptions.size(); a++) {
                            msg.writer().writeByte(it.itemOptions.get(a).optionTemplate.id);
                            nro.models.network.ItemOptionWire.write(msg, pl, it.itemOptions.get(a).param);
                        }
                        msg.writer().writeByte(0);
                        msg.writer().writeByte(0);
                    }
                } else {
                    List<ConsignItem> items = getItemKyGui(pl, i);
                    List<ConsignItem> itemsSend = pageItems(pl, i, 0, 20);
                    msg.writer().writeUTF(ConsignShopManager.gI().tabName[i]);
                    byte tab = (byte) Math.min(127, Math.max(1, (items.size() + 19) / 20));
                    msg.writer().writeByte(tab); // max page
                    msg.writer().writeByte(itemsSend.size());
                    for (int j = 0; j < itemsSend.size(); j++) {
                        ConsignItem itk = itemsSend.get(j);
                        Item it = ItemService.gI().createNewItem(itk.itemId);
                        it.itemOptions.clear();
                        if (itk.options.isEmpty()) {
                            it.itemOptions.add(new ItemOption(73, 0));
                        } else {
                            for (ItemOption option : itk.options) { if (option.optionTemplate.id != 86) it.itemOptions.add(new ItemOption(option)); }
                        }
                        msg.writer().writeShort(it.template.id);
                        msg.writer().writeShort(itk.isUpTop == -1 ? itk.id : wireId(pl,itk));
                        msg.writer().writeInt(-1);
                        msg.writer().writeInt("ruby".equals(itk.currency) ? itk.gemSell : -1);
                        msg.writer().writeByte(0); // buy type
                        msg.writer().writeInt(itk.quantity);
                        msg.writer().writeByte(itk.player_sell == pl.id ? 1 : 0); // isMe     
                        msg.writer().writeByte(it.itemOptions.size());
                        for (int a = 0; a < it.itemOptions.size(); a++) {
                            msg.writer().writeByte(it.itemOptions.get(a).optionTemplate.id);
                            nro.models.network.ItemOptionWire.write(msg, pl, it.itemOptions.get(a).param);
                        }
                        msg.writer().writeByte(0);
                        msg.writer().writeByte(0);
                    }
                }
            }
            pl.sendMessage(msg);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
                msg = null;
            }
        }
    }
}
