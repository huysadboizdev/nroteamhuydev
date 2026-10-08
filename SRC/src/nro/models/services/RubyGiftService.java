package nro.models.services;

import com.google.gson.JsonArray;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import nro.models.consts.ConstItem;
import nro.models.database.RubyGiftDAO;
import nro.models.database.PlayerDAO;
import nro.models.item.Item;
import nro.models.network.MySession;
import nro.models.player.Player;
import nro.models.server.Client;
import nro.models.services_func.TransactionService;

/** Persists both players before applying ruby and ticket changes in memory. */
public final class RubyGiftService {
    private static final RubyGiftService INSTANCE=new RubyGiftService(new RubyGiftDAO());
    private static final Map<Player,Operation> ACTIVE=new ConcurrentHashMap<>();
    private static final ScheduledExecutorService RECOVERY=Executors.newSingleThreadScheduledExecutor(task-> {
        Thread thread=new Thread(task,"Ruby gift reconciliation");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<Player,Pending> proposals=new ConcurrentHashMap<>();
    private final RubyGiftDAO dao;
    private final Predicate<Player> online;
    private final Predicate<Player> logoutSaver;
    private static final class Finalization {
        boolean resolved;
        boolean committed;
        boolean applied;
        boolean senderSaved;
        boolean targetSaved;
    }
    public record Proposal(boolean success,String message,int amount,int fee,int net) {
    }
    public record Result(boolean success,String message) {
    }
    private record Pending(Player sender,Player target,MySession senderSession,MySession targetSession,int amount,String token,long createdAt) {
    }
    private record Operation(RubyGiftService owner,Pending pending,Item ticket,
            RubyGiftDAO.Snapshot sender,RubyGiftDAO.Snapshot target,
            RubyGiftDAO.Snapshot senderBase,RubyGiftDAO.Snapshot targetBase,
            Finalization finalization) {
    }
    public RubyGiftService(RubyGiftDAO dao) {
        this(dao,p->Client.gI().getPlayer(p.id)==p&&p.getSession()!=null&&p.getSession().player==p&&p.getSession().isConnected());
    }
    public RubyGiftService(RubyGiftDAO dao,Predicate<Player> online) {
        this(dao, online, PlayerDAO::savePendingRubyLogout);
    }
    public RubyGiftService(RubyGiftDAO dao,Predicate<Player> online,Predicate<Player> logoutSaver) {
        this.dao=dao;
        this.online=online;
        this.logoutSaver=logoutSaver;
    }
    public static RubyGiftService gI() {
        return INSTANCE;
    }
    public static int fee(int amount) {
        return amount<=0?0:(int)((amount+9L)/10);
    }
    public static int net(int amount) {
        return amount<=0?0:amount-fee(amount);
    }
    public static boolean hasPending(Player p) {
        return p!=null&&ACTIVE.containsKey(p);
    }
    public static boolean isPending(Player p) {
        return hasPending(p);
    }
    public static boolean hasPendingId(long id) {
        for (Operation op:ACTIVE.values())if (op.sender().id()==id||op.target().id()==id)return true;
        return false;
    }
    public static boolean hasPendingAccount(int account) {
        for (Operation op:ACTIVE.values())if (op.sender().account()==account||op.target().account()==account)return true;
        return false;
    }
    /** The full-save bypass is restricted to this operation while both players are locked. */
    public static boolean isFinalizingLogout(Player player) {
        Operation operation = ACTIVE.get(player);
        return operation != null && operation.finalization().resolved && player.beforeDispose
                && Thread.holdsLock(first(operation.pending()))
                && Thread.holdsLock(second(operation.pending()));
    }
    public void cancel(Player p) {
        if (p!=null)proposals.remove(p);
    }
    private Item ticket(Player p) {
        if (p.inventory==null||p.inventory.itemsBag==null)return null;
        for (Item i:p.inventory.itemsBag)if (i!=null&&i.template!=null&&i.template.id==ConstItem.VE_TANG_HONG_NGOC&&i.template.type==27&&i.quantity>0)return i;
        return null;
    }
    private String validate(Pending p) {
        Player a=p.sender(),b=p.target();
        if(LiveSyncMutationGate.blocked(a)||LiveSyncMutationGate.blocked(b))return "Web command is awaiting commit reconciliation.";
        if (a!=null && b!=null && (a.walletPurchasePending!=null || b.walletPurchasePending!=null)) return "Thanh toán đang chờ đối soát, vui lòng thử lại sau.";
        if (System.currentTimeMillis()-p.createdAt()>60_000)return "Yêu cầu đã hết hạn. Hãy mở lại vé.";
        if (a==null||b==null||a==b||a.id==b.id||!a.isPl()||!b.isPl())return "Nhân vật nhận không hợp lệ.";
        if (a.getSession()!=p.senderSession()||b.getSession()!=p.targetSession()||a.beforeDispose||b.beforeDispose||!online.test(a)||!online.test(b))return "Nhân vật đã thoát hoặc thay phiên đăng nhập.";
        if (CommesonChallengeService.hasPendingId(a.id)||CommesonChallengeService.hasPendingId(b.id)||AdminPlayerActions.hasRenamePendingId(a.id)||AdminPlayerActions.hasRenamePendingId(b.id)||(a.session!=null&&AdminPlayerActions.hasRenamePendingAccount(a.session.userId))||(b.session!=null&&AdminPlayerActions.hasRenamePendingAccount(b.session.userId))||hasPending(a)||hasPending(b)||a.kolProgressPending||b.kolProgressPending||KolQuestService.hasProgressPendingId(a.id)||KolQuestService.hasProgressPendingId(b.id))return "Giao dịch trước đang chờ đối chiếu.";
        if (nro.models.shop_ky_gui.ConsignTransactions.hasPendingId(a.id)||nro.models.shop_ky_gui.ConsignTransactions.hasPendingId(b.id))return "Ký gửi đang chờ đối soát.";
        if (a.tradeWVP||b.tradeWVP||TransactionService.isTrading(a)||TransactionService.isTrading(b))return "Không thể tặng khi đang giao dịch.";
        if (p.amount()<=0||net(p.amount())<=0)return "Số hồng ngọc không hợp lệ.";
        if (a.inventory==null||b.inventory==null||a.inventory.ruby<0||b.inventory.ruby<0)return "Dữ liệu hồng ngọc không hợp lệ.";
        if (a.inventory.ruby<p.amount())return "Không đủ hồng ngọc.";
        if ((long)b.inventory.ruby+net(p.amount())+VipRewardCatalog.reservedRuby(b)>Integer.MAX_VALUE)return "Hồng ngọc người nhận vượt giới hạn.";
        if (ticket(a)==null)return "Cần một Vé tặng hồng ngọc trong hành trang.";
        return null;
    }
    private static Player first(Pending p) {
        return p.sender().id<p.target().id?p.sender():p.target();
    }
    private static Player second(Pending p) {
        return first(p)==p.sender()?p.target():p.sender();
    }
    public Proposal prepare(Player sender,Player target,int amount) {
        cancel(sender);
        if (sender==null||target==null)return new Proposal(false,"Không tìm thấy nhân vật đang online.",amount,fee(amount),net(amount));
        Pending p=new Pending(sender,target,sender.getSession(),target.getSession(),amount,UUID.randomUUID().toString(),System.currentTimeMillis());
        synchronized (first(p)) {
            synchronized (second(p)) {
                String e=validate(p);
                if (e!=null)return new Proposal(false,e,amount,fee(amount),net(amount));
                proposals.put(sender,p);
                return new Proposal(true,"",amount,fee(amount),net(amount));
            }
        }
    }
    private RubyGiftDAO.Snapshot snapshot(Player p,int ruby,Item consumed) {
        JsonArray inventory=new JsonArray();
        inventory.add(p.inventory.gold);
        inventory.add(p.inventory.gem);
        inventory.add(ruby);
        inventory.add(p.inventory.coupon);
        inventory.add(p.inventory.event);
        return new RubyGiftDAO.Snapshot(p.id,p.getSession().userId,inventory.toString(),
                serializedItems(p.inventory.itemsBag,consumed),
                serializedItems(p.inventory.itemsBody,null),
                serializedItems(p.inventory.itemsBox,null),
                serializedItems(p.inventory.itemsBoxCrackBall,null),
                serializedItems(p.inventory.itemsDaBan,null));
    }

    /** Match PlayerDAO's nested string-array shape for every inventory container. */
    private String serializedItems(List<Item> items,Item consumed) {
        if (items == null) throw new IllegalArgumentException("missing inventory container");
        JsonArray serialized=new JsonArray();
        for (Item item:items) {
            if (item==null)throw new IllegalArgumentException("null bag slot");
            JsonArray row=new JsonArray(),options=new JsonArray();
            int quantity=item.quantity-(item==consumed?1:0);
            boolean present=item.template!=null&&(item!=consumed||quantity>0);
            row.add(present?item.template.id:-1);
            row.add(present?quantity:0);
            if (present)for (Item.ItemOption op:item.itemOptions) {
                JsonArray option=new JsonArray();
                option.add(op.optionTemplate.id);
                option.add(op.param);
                options.add(option.toString());
            }
            row.add(options.toString());
            row.add(item.createTime);
            serialized.add(row.toString());
        }
        return serialized.toString();
    }
    private boolean finish(Operation op,boolean committed) {
        Pending p=op.pending();
        Finalization state = op.finalization();
        if (!state.resolved) {
            state.committed = committed;
            state.resolved = true;
        }
        if (state.committed && !state.applied) {
            p.sender().inventory.ruby-=p.amount();
            p.target().inventory.ruby+=net(p.amount());
            op.ticket().quantity--;
            if (op.ticket().quantity==0) {
                op.ticket().template=null;
                op.ticket().itemOptions.clear();
            }
            state.applied = true;
        }
        // Keep both player/account gates until every detached player has a full save.
        try {
            if (p.sender().beforeDispose && !state.senderSaved) {
                if (!op.owner().logoutSaver.test(p.sender())) return false;
                state.senderSaved = true;
            }
            if (p.target().beforeDispose && !state.targetSaved) {
                if (!op.owner().logoutSaver.test(p.target())) return false;
                state.targetSaved = true;
            }
        } catch (Exception unavailable) {
            return false;
        }
        ACTIVE.remove(p.sender(),op);
        ACTIVE.remove(p.target(),op);
        if (state.committed) {
            packet(()->InventoryService.gI().sendItemBags(p.sender()));
            packet(()->Service.gI().sendMoney(p.sender()));
            packet(()->Service.gI().sendMoney(p.target()));
            packet(()->Service.gI().sendThongBao(p.target(),"Bạn nhận được "+net(p.amount())+" hồng ngọc từ "+p.sender().name+" (phí "+fee(p.amount())+" hồng ngọc do người gửi chịu)."));
        }
        return true;
    }
    private static void packet(Runnable send) {
        try {
            send.run();
        }
        catch (Exception disconnected) {
            /* A packet failure never replays a committed transfer. */
        }
    }
    public Result confirm(Player sender) {
        if (sender==null)return new Result(false,"Nhân vật không hợp lệ.");
        Pending p=proposals.remove(sender);
        if (p==null)return new Result(false,"Yêu cầu đã hết hạn. Hãy mở lại vé.");
        synchronized (first(p)) {
            synchronized (second(p)) {
                String error=validate(p);
                if (error!=null)return new Result(false,error);
                Item t=ticket(sender);
                Operation op;
                try {
                    op=new Operation(this,p,t,
                            snapshot(sender,sender.inventory.ruby-p.amount(),t),
                            snapshot(p.target(),p.target().inventory.ruby+net(p.amount()),null),
                            snapshot(sender,sender.inventory.ruby,null),
                            snapshot(p.target(),p.target().inventory.ruby,null),new Finalization());
                }
                catch (Exception invalid) {
                    return new Result(false,"Dữ liệu hành trang không hợp lệ.");
                }
                ACTIVE.put(sender,op);
                ACTIVE.put(p.target(),op);
                try {
                    dao.transfer(p.token(),op.sender(),op.target(),p.amount());
                    if (!finish(op,true)) {
                        if (this==INSTANCE)scheduleRecovery(op);
                        return new Result(false,"Giao dịch đang chờ lưu dữ liệu đăng xuất. Vui lòng thử lại sau.");
                    }
                    return new Result(true,"Đã tặng "+net(p.amount())+" hồng ngọc cho "+p.target().name+" (phí "+fee(p.amount())+").");
                }
                catch (RubyGiftDAO.CommitUncertain uncertain) {
                    if (this==INSTANCE)scheduleRecovery(op);
                    return new Result(false,"Giao dịch đang chờ đối chiếu. Vui lòng thử lại sau.");
                }
                catch (Exception rejected) {
                    finish(op,false);
                    return new Result(false,"Không thể lưu giao dịch. Hồng ngọc và vé chưa bị trừ.");
                }
            }
        }
    }
    private void scheduleRecovery(Operation op) {
        RECOVERY.schedule(()-> {
            Pending p=op.pending();
            if (ACTIVE.get(p.sender())!=op)return;
            if (!reconcilePending(p.sender())) {
                scheduleRecovery(op);
                return;
            }
            // Both account gates were retained until the deferred full saves succeeded.
            if (p.sender().beforeDispose)p.sender().dispose();
            if (p.target().beforeDispose)p.target().dispose();
        },3,TimeUnit.SECONDS);
    }
    /** Call before acquiring an individual player monitor; always acquires both in ID order. */
    public boolean reconcilePending(Player player) {
        Operation op=ACTIVE.get(player);
        if (op==null)return true;
        Pending p=op.pending();
        synchronized (first(p)) {
            synchronized (second(p)) {
                if (ACTIVE.get(player)!=op)return !hasPending(player);
                try {
                    Finalization state = op.finalization();
                    if (!state.resolved) {
                        var receipt=op.owner().dao.reconcile(p.token(),op.sender(),op.target(),p.amount());
                        if (receipt == null) {
                            receipt = op.owner().dao.persistUncommitted(p.token(),
                                    op.senderBase(), op.targetBase(), p.amount());
                        }
                        state.committed = receipt != null;
                        state.resolved = true;
                    }
                    return finish(op,state.committed);
                }
                catch (Exception unavailable) {
                    return false;
                }
            }
        }
    }
}
