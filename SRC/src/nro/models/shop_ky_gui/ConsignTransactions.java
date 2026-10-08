package nro.models.shop_ky_gui;

import com.google.gson.JsonArray;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import nro.models.database.ConsignDAO;
import nro.models.database.RubyGiftDAO.Snapshot;
import nro.models.item.Item;
import nro.models.item.SealedEquipment;
import nro.models.item.ItemStackRules;
import nro.models.player.Player;
import nro.models.services.*;
import nro.models.services_func.TransactionService;

/** Stage copies, commit player and listing together, then publish once to RAM. */
public final class ConsignTransactions {
    public static final int POST_FEE = 1, TOP_FEE = 5, MAX_PRICE = 1_000_000;
    private static final ConcurrentMap<Player, Pending> PENDING = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService RECOVERY = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r,"Consignment reconciliation"); t.setDaemon(true); return t;
    });
    private final ConsignDAO dao;
    private final Predicate<Player> online;
    private final Predicate<Player> logoutSaver;
    private static final ThreadLocal<Player> FINALIZING_LOGOUT = new ThreadLocal<>();
    public record Result(boolean success,String message) {}
    private static final class Pending {
        final ConsignTransactions owner;final Player player;final String token,action;
        final ConsignItem before,after;final List<Item> bag;final int ruby;final Snapshot snapshot;
        boolean resolved,committed,published,logoutSaved;int id;
        nro.models.intrinsic.Intrinsic intrinsic;
        Pending(ConsignTransactions owner,Player player,String token,String action,ConsignItem before,
                ConsignItem after,List<Item> bag,int ruby,Snapshot snapshot) {
            this.owner=owner;this.player=player;this.token=token;this.action=action;this.before=before;
            this.after=after;this.bag=bag;this.ruby=ruby;this.snapshot=snapshot;
        }
    }
    public ConsignTransactions() {
        this(new ConsignDAO(),p -> p.session != null && p.session.player == p && p.session.isConnected());
    }
    public ConsignTransactions(ConsignDAO dao,Predicate<Player> online) {
        this(dao,online,nro.models.database.PlayerDAO::savePendingConsignLogout);
    }
    public ConsignTransactions(ConsignDAO dao,Predicate<Player> online,Predicate<Player> logoutSaver) {
        this.dao=dao;this.online=online;this.logoutSaver=logoutSaver;
    }
    public static boolean isFinalizingLogout(Player p) {
        Pending op=PENDING.get(p);
        return op!=null && op.resolved && p.beforeDispose && FINALIZING_LOGOUT.get()==p && Thread.holdsLock(p);
    }
    public static boolean hasPending(Player p) {return p != null && PENDING.containsKey(p);}
    public static boolean hasPendingId(long id) {return PENDING.keySet().stream().anyMatch(p -> p.id==id);}
    public static boolean hasPendingAccount(int account) {
        return PENDING.values().stream().anyMatch(p -> p.snapshot.account()==account);
    }
    private static boolean reserved(int id) {
        return PENDING.values().stream().anyMatch(p -> (p.before!=null && p.before.id==id)
                || (p.after!=null && p.after.id==id));
    }
    public static boolean isListingPending(int id){return reserved(id);}
    private String validate(Player p) {
        return validate(p,true);
    }
    private String validate(Player p,boolean requireMember) {
        if(LiveSyncMutationGate.blocked(p))return "Web command is awaiting commit reconciliation.";
        if(p==null || p.inventory==null || p.session==null || !online.test(p)
                || p.beforeDispose || p.isOffline || p.id<=0 || p.id>Integer.MAX_VALUE)
            return "Nhân vật không sẵn sàng ký gửi.";
        if(requireMember&&!p.session.actived)return "Tài khoản chưa kích hoạt, chưa thể mua/bán ký gửi.";
        if(hasPendingId(p.id) || hasPendingAccount(p.session.userId))return "Ký gửi đang chờ đối soát. Vui lòng thử lại sau.";
        if(TransactionService.isTrading(p) || p.tradeWVP || RubyGiftService.hasPendingId(p.id)
                || RubyGiftService.hasPendingAccount(p.session.userId) || p.walletPurchasePending!=null
                || p.kolProgressPending || p.kolRewardPending || KolQuestService.hasProgressPendingId(p.id)
                || AdminPlayerActions.hasRenamePendingId(p.id)
                || AdminPlayerActions.hasRenamePendingAccount(p.session.userId))return "Đang có giao dịch khác, chưa thể ký gửi.";
        if(p.inventory.ruby<0)return "Dữ liệu hồng ngọc không hợp lệ.";
        return null;
    }
    public static boolean eligible(Item item) {
        if(nro.models.event.EventItemPolicy.isBlocked(item))return false;
        if(item==null || !item.isNotNullItem() || item.quantity<=0 || item.itemOptions==null)return false;
        if(nro.models.item.ItemDefaultOptions.isLockedHighTier(item.template.id))return false;
        int type=item.template.type;
        if(type==9 || type==10 || type==34 || item.template.id==517 || item.template.id==518)return false;
        boolean seal=SealedEquipment.hasSeal(item),permission=seal || type==14 || type==15 || type==6
                || (item.template.id>=14 && item.template.id<=20);
        Set<Integer> seals=new HashSet<>();
        for(Item.ItemOption o:item.itemOptions) {
            if(o==null || o.optionTemplate==null || o.param<0)return false;
            if(o.optionTemplate.id>=34 && o.optionTemplate.id<=36)seals.add(o.optionTemplate.id);
            if(o.optionTemplate.id==30 && !seal)return false;
            if(o.optionTemplate.id==86 || o.optionTemplate.id==87)permission=true;
        }
        return permission && seals.size()<=1;
    }
    private static Item copy(Item item) {
        Item out=new Item();out.template=item.template;out.quantity=item.quantity;
        out.createTime=item.createTime;out.expire=item.expire;
        for(Item.ItemOption o:item.itemOptions)out.itemOptions.add(new Item.ItemOption(o));
        return out;
    }
    private static List<Item> copyBag(List<Item> bag) {
        List<Item> out=new ArrayList<>();for(Item item:bag) {
            if(item==null)throw new IllegalArgumentException("Null inventory slot");out.add(copy(item));
        }return out;
    }
    private static ConsignItem copyListing(ConsignItem it) {
        ConsignItem out=new ConsignItem(it.id,it.itemId,it.player_sell,it.tab,it.goldSell,it.gemSell,
                it.quantity,(byte)it.isUpTop,new ArrayList<>(),it.isBuy);
        for(Item.ItemOption o:it.options)out.options.add(new Item.ItemOption(o));
        out.currency=it.currency;out.createTime=it.createTime;return out;
    }
    private static ConsignItem find(int id) {
        if(id<=0 || reserved(id))return null;
        for(ConsignItem it:ConsignShopManager.gI().listItem)if(it.id==id)return it;return null;
    }
    private static Item restore(ConsignItem it) {
        Item item=new Item();item.template=ItemService.gI().getTemplate(it.itemId);item.quantity=it.quantity;
        if(it.createTime>0)item.createTime=it.createTime;
        for(Item.ItemOption o:it.options)item.itemOptions.add(new Item.ItemOption(o));
        SealedEquipment.normalize(item);return item;
    }
    public Result purchaseIntrinsicMax(Player p,nro.models.intrinsic.Intrinsic reward) {
        if(p==null)return fail("Không tìm thấy nhân vật.");
        synchronized(p) {synchronized(ConsignShopManager.gI()) {
            String invalid=validate(p,false);if(invalid!=null)return fail(invalid);
            if(p.nPoint==null||p.nPoint.power<10_000_000_000L)return fail("Yêu cầu sức mạnh tối thiểu 10 tỷ.");
            if(p.inventory.ruby<100)return fail("Cần đủ 100 hồng ngọc để mở nội tại max.");
            if(p.playerIntrinsic==null||p.effectSkill==null)return fail("Dữ liệu nội tại chưa sẵn sàng.");
            boolean valid=false;
            for(var template:IntrinsicService.gI().maxPool(p.gender))if(reward!=null&&template.id==reward.id
                    &&template.gender==reward.gender&&reward.param1==template.paramTo1&&reward.param2==template.paramTo2)valid=true;
            if(!valid)return fail("Nội tại không hợp lệ; chưa trừ hồng ngọc.");
            return commit(p,"intrinsic_max",null,null,copyBag(p.inventory.itemsBag),p.inventory.ruby-100,
                    "Bạn nhận được Nội tại max:\n"+reward.getName(),reward);
        }}
    }
    public static int rubyMaterialPrice(int id) {
        return switch(id){case 933,934,1820 -> 100;case 935 -> 10;default -> 0;};
    }
    public Result purchaseRubyMaterial(Player p,int id) {
        int price=rubyMaterialPrice(id);
        if(p==null||price==0)return fail("Vật phẩm không có trong cửa hàng hồng ngọc.");
        synchronized(p) {synchronized(ConsignShopManager.gI()) {
            String invalid=validate(p);if(invalid!=null)return fail(invalid);
            if(p.inventory.ruby<price)return fail("Không đủ hồng ngọc.");
            List<Item> bag=copyBag(p.inventory.itemsBag);
            Item material=ItemService.gI().createNewItem((short)id);
            material.quantity=1;
            material.itemOptions.clear();
            if(id!=935)material.itemOptions.add(new Item.ItemOption(31,1000));
            if(!InventoryService.gI().addItemList(bag,material))return fail("Hành trang không đủ chỗ; chưa trừ hồng ngọc.");
            return commit(p,"ruby_material",null,null,bag,p.inventory.ruby-price,"Mua vật phẩm bằng hồng ngọc thành công.");
        }}
    }
    public Result purchaseDivineBox(Player p) {
        if(p==null)return fail("Không tìm thấy nhân vật.");
        synchronized(p) {synchronized(ConsignShopManager.gI()) {
            String invalid=validate(p);if(invalid!=null)return fail(invalid);
            if(p.inventory.ruby<50_000)return fail("Cần 50.000 hồng ngọc để mua hộp Thần Linh.");
            List<Item> bag=copyBag(p.inventory.itemsBag);
            Item box=ItemService.gI().createNewItem((short)1775);
            nro.models.item.RubyItemRules.normalize(box);
            if(!InventoryService.gI().addItemList(bag,box))return fail("Hành trang không đủ chỗ, chưa trừ hồng ngọc.");
            return commit(p,"shop1775",null,null,bag,p.inventory.ruby-50_000,"Đã mua hộp Thần Linh bằng 50.000 hồng ngọc.");
        }}
    }
    public Result redeemDivineBox(Player p,Item selected,int planet) {
        if(p==null)return fail("Không tìm thấy nhân vật.");
        synchronized(p) {synchronized(ConsignShopManager.gI()) {
            String invalid=validate(p);if(invalid!=null)return fail(invalid);
            int slot=selected==null?-1:p.inventory.itemsBag.indexOf(selected);
            if(planet<0||planet>2||slot<0||selected.template==null||selected.template.id!=1775||selected.quantity<1)
                return fail("Hộp hoặc lựa chọn không còn hợp lệ. Hãy mở lại hộp.");
            List<Item> bag=copyBag(p.inventory.itemsBag);
            if(bag.get(slot).quantity==1)bag.set(slot,new Item());else bag.get(slot).quantity--;
            for(Item reward:nro.models.services.DivineEquipmentBoxService.createSet(planet)) {
                if(!InventoryService.gI().addItemList(bag,reward))return fail("Cần đủ chỗ nhận 5 món; hộp chưa bị sử dụng.");
            }
            return commit(p,"box1775",null,null,bag,p.inventory.ruby,"Đã nhận đủ 5 món Thần Linh, chỉ số gốc 0%, không giao dịch/ký gửi.");
        }}
    }
    public Result post(Player p,int slot,int price,byte currency,int quantity) {
        if(p==null)return fail("Không tìm thấy nhân vật.");
        synchronized(p) {
        try(var eventPermit=nro.models.event.EventItemPolicy.admit()) {synchronized(ConsignShopManager.gI()) {
            String invalid=validate(p);if(invalid!=null)return fail(invalid);
            if(currency!=1 || price<=0 || price>MAX_PRICE || quantity<=0 || quantity>99
                    || slot<0 || slot>=p.inventory.itemsBag.size())return fail("Chỉ ký gửi bằng hồng ngọc, giá 1–1.000.000, số lượng 1–99.");
            Item source=p.inventory.itemsBag.get(slot);
            if(!eligible(source) || quantity>source.quantity || (!ItemStackRules.stackable(source.template)&&quantity!=1))
                return fail("Vật phẩm không thể ký gửi hoặc số lượng không hợp lệ.");
            if(p.inventory.ruby<POST_FEE)return fail("Cần 1 hồng ngọc làm phí đăng bán.");
            List<Item> bag=copyBag(p.inventory.itemsBag);Item offered=copy(source);offered.quantity=quantity;
            offered.itemOptions.removeIf(o->o.optionTemplate.id==86 || o.optionTemplate.id==87);
            offered.itemOptions.add(new Item.ItemOption(87,1));SealedEquipment.normalize(offered);
            if(quantity==bag.get(slot).quantity)bag.set(slot,new Item());else bag.get(slot).quantity-=quantity;
            byte tab=(byte)(source.template.type<=1?0:source.template.type<=4?1:source.template.type==29?2:3);
            ConsignItem after=new ConsignItem(0,source.template.id,(int)p.id,tab,-1,price,quantity,(byte)0,offered.itemOptions,false);
            after.currency="ruby";after.createTime=source.createTime;
            return commit(p,"post",null,after,bag,p.inventory.ruby-POST_FEE,"Đăng bán bằng hồng ngọc thành công.");
        }
        }
}
    }
    public Result buy(Player p,int id) {
        if(p==null)return fail("Không tìm thấy nhân vật.");
        synchronized(p) {
        try(var eventPermit=nro.models.event.EventItemPolicy.admit()) {synchronized(ConsignShopManager.gI()) {
            String invalid=validate(p);if(invalid!=null)return fail(invalid);
            ConsignItem before=find(id);
            if(before==null || before.isBuy || before.player_sell==p.id)return fail("Vật phẩm đã bán, không tồn tại hoặc do bạn đăng.");
            if(!"ruby".equals(before.currency) || before.goldSell>0 || before.gemSell<=0 || before.gemSell>MAX_PRICE)
                return fail("Tin đăng cũ chỉ được chủ lấy lại. Siêu thị chỉ mua bán bằng hồng ngọc.");
            if(p.nPoint==null || p.nPoint.power<17_000_000_000L)return fail("Yêu cầu sức mạnh từ 17 tỷ.");
            if(p.inventory.ruby<before.gemSell)return fail("Không đủ hồng ngọc.");
            Item incoming=restore(before);if(!eligible(incoming))return fail("Dữ liệu vật phẩm ký gửi không hợp lệ.");
            List<Item> bag=copyBag(p.inventory.itemsBag);
            if(!InventoryService.gI().addItemList(bag,incoming))return fail("Hành trang không đủ chỗ; chưa trừ hồng ngọc.");
            ConsignItem after=copyListing(before);after.isBuy=true;
            return commit(p,"buy",before,after,bag,p.inventory.ruby-before.gemSell,"Mua vật phẩm bằng hồng ngọc thành công.");
        }
        }
}
    }
    public Result claimOrCancel(Player p,byte action,int id) {
        if(p==null)return fail("Không tìm thấy nhân vật.");
        synchronized(p) {
        try(var eventPermit=nro.models.event.EventItemPolicy.admit()) {synchronized(ConsignShopManager.gI()) {
            String invalid=validate(p);if(invalid!=null)return fail(invalid);
            ConsignItem before=find(id);
            if(before==null || before.player_sell!=p.id)return fail("Không tìm thấy tin đăng của bạn.");
            if(action==1 && !before.isBuy) {
                List<Item> bag=copyBag(p.inventory.itemsBag);
                if(!InventoryService.gI().addItemList(bag,restore(before)))return fail("Hành trang không đủ chỗ để lấy lại đồ.");
                return commit(p,"cancel",before,null,bag,p.inventory.ruby,"Đã lấy lại vật phẩm ký gửi.");
            }
            if(action!=2 || !before.isBuy)return fail("Vật phẩm chưa bán hoặc thao tác không hợp lệ.");
            if(!"ruby".equals(before.currency) || before.gemSell<=0 || before.goldSell>0)return fail("Tin bán cũ cần quản trị đối soát tiền; không đổi sang hồng ngọc.");
            int net=before.gemSell-(int)((long)before.gemSell*10/100);
            long ruby=(long)p.inventory.ruby+net;
            if(ruby>Integer.MAX_VALUE)return fail("Hồng ngọc vượt giới hạn, chưa nhận tiền.");
            return commit(p,"claim",before,null,copyBag(p.inventory.itemsBag),(int)ruby,"Đã nhận hồng ngọc, phí bán 10%.");
        }
        }
}
    }
    public Result top(Player p,int id) {
        if(p==null)return fail("Không tìm thấy nhân vật.");
        synchronized(p) {synchronized(ConsignShopManager.gI()) {
            String invalid=validate(p);if(invalid!=null)return fail(invalid);
            ConsignItem before=find(id);
            if(before==null || before.player_sell!=p.id || before.isBuy || !"ruby".equals(before.currency))return fail("Tin đăng không hợp lệ hoặc không thuộc bạn.");
            if(before.isUpTop!=0)return fail("Tin đăng đã ở đầu trang; không thu thêm phí.");
            if(p.inventory.ruby<TOP_FEE)return fail("Cần 5 hồng ngọc để đưa tin lên đầu.");
            ConsignItem after=copyListing(before);after.isUpTop=1;
            return commit(p,"top",before,after,copyBag(p.inventory.itemsBag),p.inventory.ruby-TOP_FEE,"Đưa tin lên đầu thành công.");
        }}
    }
    private static Result fail(String message) {return new Result(false,message);}
    private Result commit(Player p,String action,ConsignItem before,ConsignItem after,List<Item> bag,int ruby,String success) {
        return commit(p,action,before,after,bag,ruby,success,null);
    }
    private Result commit(Player p,String action,ConsignItem before,ConsignItem after,List<Item> bag,int ruby,String success,nro.models.intrinsic.Intrinsic intrinsic) {
        String token=UUID.randomUUID().toString();Snapshot snapshot=snapshot(p,bag,ruby);
        Pending op=new Pending(this,p,token,action,before,after,bag,ruby,snapshot);
        op.intrinsic=intrinsic;
        PENDING.put(p,op);
        try {
            int id=intrinsic==null?dao.transact(token,action,before,after,snapshot):dao.transactIntrinsic(token,snapshot,intrinsicData(p,intrinsic));
            op.id=id;op.resolved=true;op.committed=true;
            if(!finishPending(op)){schedule(op);return fail(intrinsic==null?"Ký gửi đã lưu, đang chờ lưu dữ liệu đăng xuất.":"Nội tại đã lưu, đang chờ lưu dữ liệu đăng xuất.");}
            return new Result(true,success);
        } catch(ConsignDAO.CommitUncertain uncertain) {
            schedule(op);return fail(intrinsic==null?"Ký gửi đang chờ đối soát; chưa thể thao tác tiếp. Hệ thống sẽ tự kiểm tra lại.":"Mở nội tại đang chờ đối soát. Hệ thống sẽ tự kiểm tra; vui lòng chờ.");
        } catch(SQLException | RuntimeException failure) {
            PENDING.remove(p,op);nro.models.utils.Logger.logException(ConsignTransactions.class,failure,"Consignment transaction rolled back");
            return fail(intrinsic==null?"Không lưu được ký gửi; tiền và đồ chưa bị thay đổi. Vui lòng thử lại.":"Không lưu được nội tại; hồng ngọc và nội tại cũ chưa thay đổi. Vui lòng thử lại.");
        }
    }
    private static void publish(Pending op,int id) {
        List<ConsignItem> items=ConsignShopManager.gI().listItem;
        if(op.before!=null)items.removeIf(it->it.id==op.before.id);
        if(op.after!=null){op.after.id=id;items.add(op.after);}
        op.player.inventory.itemsBag=op.bag;op.player.inventory.ruby=op.ruby;
        if(op.intrinsic!=null)op.player.playerIntrinsic.intrinsic=op.intrinsic;
    }
    private static boolean finishPending(Pending op) {
        if(op.committed && !op.published){publish(op,op.id);op.published=true;}
        if(op.player.beforeDispose && !op.logoutSaved) {
            FINALIZING_LOGOUT.set(op.player);
            try {op.logoutSaved=op.owner.logoutSaver.test(op.player);}
            catch(RuntimeException failure){return false;}
            finally {FINALIZING_LOGOUT.remove();}
            if(!op.logoutSaved)return false;
            op.player.consignLogoutSaved=true;
        }
        PENDING.remove(op.player,op);return true;
    }
    private static void schedule(Pending op) {
        RECOVERY.schedule(()->{
            if(PENDING.get(op.player)!=op)return;
            if(!reconcilePending(op.player)){schedule(op);return;}
            try {
                InventoryService.gI().sendItemBags(op.player);Service.gI().sendMoney(op.player);
                if(op.intrinsic!=null&&!op.player.beforeDispose){IntrinsicService.gI().sendInfoIntrinsic(op.player);Service.gI().point(op.player);}
                if(op.player.beforeDispose)op.player.dispose();
            }catch(Exception failure){nro.models.utils.Logger.logException(ConsignTransactions.class,failure,"Consignment recovery refresh");}
        },3,TimeUnit.SECONDS);
    }
    public static boolean reconcilePending(Player p) {
        Pending op=PENDING.get(p);if(op==null)return true;
        synchronized(p) {synchronized(ConsignShopManager.gI()) {
            if(PENDING.get(p)!=op)return !hasPending(p);
            try {
                if(!op.resolved) {
                    Integer id=op.owner.dao.reconcile(op.token,op.action,op.snapshot);
                    op.committed=id!=null;op.id=id==null?0:id;op.resolved=true;
                }
                return finishPending(op);
            }catch(SQLException failure){return false;}
        }}
    }
    private static String intrinsicData(Player p,nro.models.intrinsic.Intrinsic intrinsic) {
        JsonArray data=new JsonArray();data.add(intrinsic.id);data.add(intrinsic.param1);data.add(intrinsic.param2);
        data.add(p.playerIntrinsic.countOpen);data.add(p.effectSkill.isIntrinsic);data.add(p.effectSkill.skillID);
        data.add(p.effectSkill.cooldown);data.add(p.effectSkill.lastTimeUseSkill);return data.toString();
    }
    private static String serialized(List<Item> items) {
        JsonArray result=new JsonArray();
        for(Item item:items) {
            JsonArray row=new JsonArray(),options=new JsonArray();
            row.add(item.template==null?-1:item.template.id);row.add(item.template==null?0:item.quantity);
            if(item.template!=null)for(Item.ItemOption o:item.itemOptions) {
                JsonArray option=new JsonArray();option.add(o.optionTemplate.id);option.add(o.param);options.add(option.toString());
            }
            row.add(options.toString());row.add(item.createTime);result.add(row.toString());
        }return result.toString();
    }
    private static Snapshot snapshot(Player p,List<Item> bag,int ruby) {
        JsonArray inventory=new JsonArray();inventory.add(p.inventory.gold);inventory.add(p.inventory.gem);
        inventory.add(ruby);inventory.add(p.inventory.coupon);inventory.add(p.inventory.event);
        return new Snapshot(p.id,p.session.userId,inventory.toString(),serialized(bag),
                serialized(p.inventory.itemsBody),serialized(p.inventory.itemsBox),
                serialized(p.inventory.itemsBoxCrackBall),serialized(p.inventory.itemsDaBan));
    }
}
