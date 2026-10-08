package nro.models.services;

import com.google.gson.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import nro.models.database.GiftCodeClaimDAO;
import nro.models.database.PlayerDAO;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player.Inventory;
import nro.models.managers.GiftCodeManager;
import nro.models.server.Manager;

/** Account-scoped, durable gifts. Nothing is published before the SQL commit is known. */
public class GiftCodeService {
    private static final Map<Player,Operation> ACTIVE=new ConcurrentHashMap<>();
    private static final ScheduledExecutorService RECOVERY=Executors.newSingleThreadScheduledExecutor(task->{Thread t=new Thread(task,"Giftcode reconciliation");t.setDaemon(true);return t;});
    private static final GiftCodeService INSTANCE=new GiftCodeService(new GiftCodeClaimDAO(),PlayerDAO::savePendingGiftLogout);
    private final GiftCodeClaimDAO dao;
    private final Predicate<Player> logoutSaver;
    private static final class Operation {
        final GiftCodeService owner;final Player player;final long id;final int account;final String code,token;
        Rewards rewards;GiftCodeClaimDAO.Receipt receipt;boolean resolved,applied;
        Operation(GiftCodeService owner,Player p,int account,String code){this.owner=owner;this.player=p;this.id=p.id;this.account=account;this.code=code;this.token=UUID.randomUUID().toString();}
    }
    static record Rewards(long gold,int gem,int ruby,List<Item> bag,List<String> history,GiftCodeClaimDAO.Snapshot snapshot){}
    public GiftCodeService(GiftCodeClaimDAO dao,Predicate<Player> logoutSaver){this.dao=dao;this.logoutSaver=logoutSaver;}
    public static GiftCodeService gI(){return INSTANCE;}
    public static boolean hasPending(Player p){return p!=null&&ACTIVE.containsKey(p);}
    public static boolean hasPendingId(long id){for(Operation op:ACTIVE.values())if(op.id==id)return true;return false;}
    public static boolean hasPendingAccount(int account){for(Operation op:ACTIVE.values())if(op.account==account)return true;return false;}
    public static boolean isFinalizingLogout(Player p){Operation op=ACTIVE.get(p);return op!=null&&op.resolved&&Thread.holdsLock(p)&&p.beforeDispose&&p.liveLogoutCleanupCompleted;}
    private static int bounded(int base,int quantity){if(base<0||quantity<=0)throw new IllegalArgumentException("Invalid currency");return (int)Math.min((long)base+quantity,200_000_000L);}
    private static JsonArray array(String raw){return new JsonParser().parse(raw).getAsJsonArray();}
    static String serializeBag(List<Item> bag){
        JsonArray result=new JsonArray();
        for(Item item:bag){JsonArray row=new JsonArray(),options=new JsonArray();boolean present=item.template!=null;
            row.add(present?item.template.id:-1);row.add(present?item.quantity:0);
            if(present)for(Item.ItemOption op:item.itemOptions){JsonArray v=new JsonArray();v.add(op.optionTemplate.id);v.add(op.param);options.add(v.toString());}
            row.add(options.toString());row.add(item.createTime);result.add(row.toString());}
        return result.toString();
    }
    private static int integer(JsonElement value){if(value==null||!value.isJsonPrimitive()||!value.getAsString().matches("-?[0-9]+"))throw new IllegalArgumentException("Invalid integer");return Integer.parseInt(value.getAsString());}
    static Rewards stage(Player p,int account,GiftCodeClaimDAO.Catalog gift){
        if(p.inventory==null||p.inventory.itemsBag==null||p.giftCode==null||p.inventory.gold<0)throw new IllegalArgumentException("Invalid inventory");
        List<Item> bag=new ArrayList<>();for(Item item:p.inventory.itemsBag){if(item==null)throw new IllegalArgumentException("Null bag slot");bag.add(item.cloneItem());}
        long gold=Math.min(p.inventory.gold,Inventory.LIMIT_GOLD);int gem=p.inventory.gem,ruby=p.inventory.ruby;
        if(gem<0||ruby<0)throw new IllegalArgumentException("Negative currency");
        Set<Integer> seen=new HashSet<>();JsonArray details=array(gift.detail());
        if(details.size()==0)throw new IllegalArgumentException("Empty gift");
        for(JsonElement raw:details){JsonObject entry=raw.getAsJsonObject();int id=integer(entry.get("id")),quantity=integer(entry.get("quantity"));
            if(id < -3||quantity<=0||!seen.add(id))throw new IllegalArgumentException("Invalid or duplicate gift item");
            List<Item.ItemOption> opts=new ArrayList<>();JsonElement options=entry.get("options");Set<Integer> optionIds=new HashSet<>();
            if(options!=null)for(JsonElement option:options.getAsJsonArray()){JsonObject v=option.getAsJsonObject();int key=integer(v.get("id")),param=integer(v.get("param"));
                if(param<0||!optionIds.add(key))throw new IllegalArgumentException("Invalid duplicate option");
                var template=Manager.ITEM_OPTION_TEMPLATES.stream().filter(t->t.id==key).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown item option"));
                var op=new Item.ItemOption();op.optionTemplate=template;op.param=param;opts.add(op);}
            if(id==-1){gold=Math.min(Inventory.LIMIT_GOLD,gold+quantity);continue;}
            if(id==-2){gem=bounded(gem,quantity);continue;}
            if(id==-3){ruby=bounded(ruby,quantity);continue;}
            nro.models.item.DisabledItemRules.requireEnabled(id);
            var template=Manager.ITEM_TEMPLATES.stream().filter(t->t.id==id).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown gift item"));
            if(template.type==9){gold=Math.min(Inventory.LIMIT_GOLD,gold+quantity);continue;}
            if(template.type==10){gem=bounded(gem,quantity);continue;}
            if(template.type==34){ruby=bounded(ruby,quantity);continue;}
            // These grants modify map/task/character containers outside the transactional reward columns.
            if(template.type==13||id==74||id==453||id==517||id==518||id>=353&&id<=360||id==362||id>=372&&id<=378||id>=1765&&id<=1771)
                throw new IllegalArgumentException("Gift item requires unsupported side effects");
            Item item=new Item();item.template=template;item.quantity=quantity;item.createTime=System.currentTimeMillis();item.itemOptions=new ArrayList<>(opts);
            if(id==457&&opts.stream().noneMatch(o->o.optionTemplate.id==30)){
                var optionTemplate=Manager.ITEM_OPTION_TEMPLATES.stream().filter(t->t.id==30).findFirst().orElseThrow();
                Item.ItemOption option=new Item.ItemOption();option.optionTemplate=optionTemplate;item.itemOptions.add(option);}
            nro.models.item.ItemDefaultOptions.complete(item);
            if(!InventoryService.gI().addItemList(bag,item))throw new IllegalArgumentException("Not enough bag space");
        }
        if((long)ruby+VipRewardCatalog.reservedRuby(p)>Integer.MAX_VALUE)throw new IllegalArgumentException("Reserved ruby capacity exceeded");
        List<String> history=new ArrayList<>();for(String raw:p.giftCode.rewards){String code=nro.models.player.GiftCode.canonical(raw);if(!history.contains(code))history.add(code);}
        String code=nro.models.player.GiftCode.canonical(gift.code());if(history.contains(code))throw new IllegalArgumentException("Giftcode already claimed");history.add(code);
        JsonArray inventory=new JsonArray();inventory.add(gold);inventory.add(gem);inventory.add(ruby);inventory.add(p.inventory.coupon);inventory.add(p.inventory.event);
        JsonArray claims=new JsonArray();for(String claim:history)claims.add(claim);
        return new Rewards(gold,gem,ruby,bag,history,new GiftCodeClaimDAO.Snapshot(p.id,account,inventory.toString(),serializeBag(bag),claims.toString()));
    }
    private boolean finish(Operation op){
        Player p=op.player;
        if(op.receipt!=null&&!op.applied){Rewards r=op.rewards;if(r==null)throw new IllegalStateException("Missing committed reward snapshot");
            p.inventory.gold=r.gold();p.inventory.gem=r.gem();p.inventory.ruby=r.ruby();p.inventory.itemsBag=r.bag();p.giftCode.rewards=r.history();
            GiftCodeManager.gI().publishClaim(op.code,op.receipt.catalogId(),op.receipt.remaining());op.applied=true;}
        if(p.beforeDispose&&(!p.liveLogoutCleanupCompleted||!op.owner.logoutSaver.test(p)))return false;
        if(p.beforeDispose)p.giftLogoutSaved=true;
        ACTIVE.remove(p,op);
        if(p.beforeDispose){p.dispose();p.liveLogoutCompleted=true;LiveSyncCommandGate.retired(p.id,op.account);}
        if(op.receipt!=null&&!p.beforeDispose){packet(()->InventoryService.gI().sendItemBags(p));packet(()->Service.gI().sendMoney(p));packet(()->Service.gI().sendThongBao(p,"Nhận giftcode thành công."));}
        return true;
    }
    private static void packet(Runnable send){try{send.run();}catch(Exception ignored){}}
    public boolean claim(Player p,String raw){
        if(p==null)return false;
        synchronized(LiveSyncCommandGate.LIFECYCLE_LOCK){synchronized(p){synchronized(GiftCodeManager.gI()){
            if(p.beforeDispose||p.isOffline||p.getSession()==null||LiveSyncMutationGate.blocked(p)||RubyGiftService.hasPending(p)||p.walletPurchasePending!=null
                ||CommesonChallengeService.hasPendingId(p.id)||nro.models.shop_ky_gui.ConsignTransactions.hasPendingId(p.id)||KolQuestService.hasProgressPendingId(p.id)
                ||AdminPlayerActions.hasRenamePendingId(p.id)||nro.models.services_func.TransactionService.isTrading(p)||p.tradeWVP)return false;
            Operation op;
            try{op=new Operation(this,p,p.getSession().userId,nro.models.player.GiftCode.canonical(raw));}catch(Exception invalid){return false;}
            if(hasPendingId(p.id)||hasPendingAccount(op.account))return false;ACTIVE.put(p,op);
            try{op.receipt=dao.claim(op.id,op.account,op.code,op.token,gift->{op.rewards=stage(p,op.account,gift);return op.rewards.snapshot();});op.resolved=true;
            }catch(GiftCodeClaimDAO.CommitUncertain uncertain){schedule(op);return false;}
            catch(Exception rejected){op.resolved=true;op.receipt=null;}
            try{if(!finish(op)){schedule(op);return false;}return op.receipt!=null;}
            catch(Exception publicationFailure){schedule(op);return false;}
        }}}
    }
    public void giftCode(Player p,String code){if(!claim(p,code))packet(()->Service.gI().sendThongBao(p,"Không thể nhận code: mã đã dùng, hết hạn, thiếu chỗ hoặc giao dịch đang chờ đối chiếu."));}
    public boolean reconcilePending(Player p){Operation op=ACTIVE.get(p);if(op==null)return !hasPendingId(p.id);
        synchronized(p){synchronized(GiftCodeManager.gI()){
            if(ACTIVE.get(p)!=op)return !hasPendingId(p.id);
            try{if(!op.resolved){op.receipt=op.owner.dao.reconcile(op.id,op.account,op.code,op.token);op.resolved=true;}return finish(op);}catch(Exception unavailable){return false;}
        }}
    }
    /** Maintenance must refuse process shutdown while a durable outcome is still unknown. */
    public boolean drainPending(){for(Operation op:new ArrayList<>(ACTIVE.values()))op.owner.reconcilePending(op.player);return ACTIVE.isEmpty();}
    private void schedule(Operation op){if(this!=INSTANCE)return;RECOVERY.schedule(()->{if(ACTIVE.get(op.player)!=op)return;if(!reconcilePending(op.player)){schedule(op);return;}if(op.player.beforeDispose)op.player.dispose();},3,TimeUnit.SECONDS);}
}
