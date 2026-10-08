package nro.models.services;

import java.util.ArrayList;
import java.util.List;
import nro.models.item.Item;
import nro.models.player.Player;

/** One catalog for VIP previews, capacity checks and the actual rewards. */
public final class VipRewardCatalog {
    private VipRewardCatalog() { }
    private static final int[] RUBY = {200,500,700,1000};
    private static final int[] CARDS = {1204,1791,1792,1793};
    private static final String[] CARD_NAMES = {"Mảnh Rồng thần Namếc","Mảnh khỉ Oozaru","Mảnh khỉ Oozarun 1","Mảnh khỉ Oozarun 2"};
    public static int reservedRuby(Player p) {
        if(p==null||p.walletPurchasePending==null)return 0;
        String kind=p.walletPurchasePending.kind();
        if(kind==null||!kind.matches("vip_[1-4]"))return 0;
        return RUBY[kind.charAt(4)-'1'];
    }

    public static final class Package {
        private final int level;
        private final List<Item> items;
        private boolean granted;
        private boolean storedInChest;
        private Package(int level,List<Item> items) {this.level=level;this.items=items;}
        public int ruby() {return RUBY[level-1];}
        public List<Item> items() {return java.util.Collections.unmodifiableList(items);}
        private List<Item> preparedBag(Player p) {
            List<Item> bag=InventoryService.gI().copyItemsBag(p);
            for(int n=0;n<bag.size();n++) {
                Item original=p.inventory.itemsBag.get(n),copy=bag.get(n);
                copy.id=original.id;copy.expire=original.expire;copy.quantityGD=original.quantityGD;
                copy.text=original.text;copy.options=original.options;copy.info=original.info;copy.content=original.content;
                copy.itemOptions=new ArrayList<>();
                for(Item.ItemOption option:original.itemOptions)copy.itemOptions.add(new Item.ItemOption(option));
            }
            for(Item item:items) {
                if(!InventoryService.gI().addItemList(bag,ItemService.gI().copyItem(item)))return null;
            }
            return bag;
        }
        public boolean fits(Player p) {
            synchronized(p) {
                return p.inventory.ruby>=0 && (long)p.inventory.ruby+ruby()<=Integer.MAX_VALUE
                    && !RubyGiftService.hasPending(p) && !CommesonChallengeService.hasPendingId(p.id)
                    && !nro.models.shop_ky_gui.ConsignTransactions.hasPending(p)
                    && !p.tradeWVP && !nro.models.services_func.TransactionService.isTrading(p)
                    && preparedBag(p)!=null;
            }
        }
        /** The owner lock also protects the delayed wallet reconciliation callback. */
        public void grant(Player p) {
            synchronized(p) {
                if(granted)return;
                int newRuby=Math.toIntExact((long)p.inventory.ruby+ruby());
                List<Item> bag=(p.tradeWVP || nro.models.services_func.TransactionService.isTrading(p))?null:preparedBag(p);
                if(bag!=null)p.inventory.itemsBag=bag;
                else {
                    // Payment can resolve later, after other rewards fill the bag.
                    // This already-persisted reward chest preserves the complete package.
                    List<Item> chest=new ArrayList<>(p.inventory.itemsBoxCrackBall);
                    for(Item item:items)chest.add(ItemService.gI().copyItem(item));
                    p.inventory.itemsBoxCrackBall=chest;
                    storedInChest=true;
                }
                p.inventory.ruby=newRuby;
                p.vip=(byte)Math.max(p.vip,level);
                p.vipPurchaseCount++;
                long now=System.currentTimeMillis();
                p.timevip=Math.max(now,p.timevip)+30L*24*60*60*1000;
                granted=true;
            }
        }
        public boolean storedInChest() {return storedInChest;}
    }

    private static Item item(int id,int count) {return ItemService.gI().createNewItem((short)id,count);}
    private static Item gear(int id,int percent,int days) {
        Item item=item(id,1);
        option(item,50,percent);option(item,77,percent);option(item,103,percent);
        if(days>0)option(item,93,days);
        return item;
    }
    private static void option(Item item,int id,int value) {item.itemOptions.add(new Item.ItemOption(id,value));}

    public static Package prepare(int level) {
        if(level<1||level>4)throw new IllegalArgumentException("Invalid VIP level");
        List<Item> items=new ArrayList<>();
        items.add(item(722,10));items.add(item(1283,level*1000));
        items.add(item(1794,100));items.add(item(1284,100));items.add(item(CARDS[level-1],1000));
        items.add(item(987,new int[]{5,10,30,50}[level-1]));
        if(level<=3) {
            items.add(gear(1252,level==1?10:12,level<=2?30:0));
            items.add(gear(level==1?1256:1254,level==1?10:12,level<=2?30:0));
            if(level>=2) {
                Item costume=item(584,1);option(costume,50,24);option(costume,77,24);option(costume,117,15);
                if(level==2)option(costume,93,30);
                items.add(costume);
            }
            if(level==3)items.add(item(956,10));
        } else {
            Item mount=gear(1554,15,0);option(mount,14,10);items.add(mount);
            Item hammer=gear(1772,15,0);option(hammer,236,15);items.add(hammer);
            Item costume=item(1557,1);option(costume,50,25);option(costume,77,25);option(costume,117,25);option(costume,236,25);items.add(costume);
            items.add(item(1204,20));
        }
        return new Package(level,items);
    }

    public static String description(int level) {
        if(level<1||level>4)throw new IllegalArgumentException("Invalid VIP level");
        StringBuilder text=new StringBuilder("Nâng cấp VIP "+level+" nhận:\n"+RUBY[level-1]+" hồng ngọc\n10 Capsule hồng ngọc\n"+level*1000+" Cuốn sách cũ\n100 Con dấu\n100 Bùa giám định\n1000 "+CARD_NAMES[level-1]+"\n");
        text.append("X3 kinh nghiệm toàn mùa\n");
        for(Item item:prepare(level).items()) {
            if(item.template.id==722||item.template.id==1283||item.template.id==1794||item.template.id==1284||item.template.id==CARDS[level-1])continue;
            text.append(item.quantity).append(' ').append(item.template.name);
            for(Item.ItemOption opt:item.itemOptions)if(opt.optionTemplate.id==93)text.append(" ("+opt.param+" ngày)");
            text.append('\n');
        }
        text.append("Thời hạn VIP: 30 ngày. Hãy chừa đủ chỗ trong hành trang.");
        return text.toString();
    }
}
