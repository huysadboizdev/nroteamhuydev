package nro.models.services;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import nro.models.boss.BossData;
import nro.models.boss.nhan_ban.NhanBan;
import nro.models.commeson.CommesonAttempt;
import nro.models.commeson.CommesonAttempt.State;
import nro.models.database.CommesonDAO;
import nro.models.database.PlayerDAO;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.map.service.ItemMapService;
import nro.models.matches.PKCommeson;
import nro.models.player.Player;
import nro.models.utils.Logger;

/** A claim is durable before a clone exists. Database failure denies new fights/rewards. */
public final class CommesonChallengeService {
    private static final CommesonChallengeService INSTANCE=new CommesonChallengeService();
    public static CommesonChallengeService gI(){return INSTANCE;}
    private final CommesonDAO dao;
    public CommesonChallengeService(){this(new CommesonDAO());}
    public CommesonChallengeService(CommesonDAO dao){this.dao=dao;}
    static {
        java.util.concurrent.ScheduledExecutorService retry=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"Commeson pickup recovery");t.setDaemon(true);return t;});
        retry.scheduleWithFixedDelay(()->{
            for(Match m:INSTANCE.matches.values())try{
                if(m.quarantine || m.owner.beforeDispose)synchronized(m.owner){
                    if(m.quarantine)INSTANCE.reconcilePickup(m);
                    if(!m.quarantine && m.owner.beforeDispose){INSTANCE.clearCombat(m);INSTANCE.removeBottle(m);INSTANCE.removeBoss(m);if(m.boss==null && m.bottle==null){INSTANCE.matches.remove(m.owner.id,m);if(m.owner.inventory!=null)m.owner.dispose();}}
                }
            }catch(Exception e){Logger.logException(CommesonChallengeService.class,e);}
        },2,2,java.util.concurrent.TimeUnit.SECONDS);
    }
    private final ConcurrentHashMap<Long,Match> matches=new ConcurrentHashMap<>();
    private static final class Match {
        final CommesonAttempt attempt;final Zone origin;final Player owner;
        NhanBan boss;ItemMap bottle;boolean returning;volatile boolean quarantine;
        List<Item> beforeBag;
        Match(CommesonAttempt a,Player p){attempt=a;origin=p.zone;owner=p;}
    }
    public void start(Player p,BossData data) {
        synchronized(p) {
            if(p.zone==null || p.zone.map.mapId!=140 || p.pvp!=null || matches.containsKey(p.id))return;
            CommesonAttempt a=new CommesonAttempt(UUID.randomUUID().toString(),p.id,System.currentTimeMillis());
            a.state=State.RESERVED;
            Match m=new Match(a,p);
            if(matches.putIfAbsent(p.id,m)!=null)return;
            try {
                CommesonAttempt pending=dao.pending(p.id);
                // No in-memory match existed before our atomic reservation: this is stale pre-start work.
                if(pending!=null && pending.state==State.RESERVED){dao.releasePreStart(pending);pending=dao.pending(p.id);}
                if(pending!=null || !dao.claim(a,p.lastPkCommesonTime)){
                    matches.remove(p.id,m);Service.gI().sendThongBao(p,"Hãy chờ đến ngày mai");return;
                }
                try {m.boss=new NhanBan(p,data);}
                catch(Exception e){matches.remove(p.id,m);dao.releasePreStart(a);throw e;}
                a.state=State.FIGHT;a.deadline=System.currentTimeMillis()+300_000;
                try{if(!dao.transition(a,State.RESERVED))throw new java.sql.SQLException("Commeson start reservation changed");}
                catch(Exception e){removeBoss(m);matches.remove(p.id,m);try{dao.releasePreStart(a);}catch(Exception ignored){}throw e;}
                EffectSkillService.gI().setPKCommeson(p,300_000);
                p.lastPkCommesonTime=System.currentTimeMillis();
            } catch(Exception e){
                if(m.boss==null){matches.remove(p.id,m);try{dao.releasePreStart(a);}catch(Exception cleanup){Logger.logException(getClass(),cleanup);}}
                Logger.logException(getClass(),e);Service.gI().sendThongBao(p,"Không thể mở trận đấu lúc này, hãy thử lại sau");
            }
        }
    }
    public boolean owns(Player p,NhanBan boss) {
        Match m=matches.get(p.id);return m!=null && m.owner==p && m.boss==boss && m.attempt.state==State.FIGHT && !m.attempt.due(System.currentTimeMillis()) && p.zone==m.origin && boss.zone==m.origin;
    }
    public Zone origin(Player p){Match m=matches.get(p.id);return m==null?null:m.origin;}
    public boolean preparing(Player p){Match m=matches.get(p.id);return m!=null && m.owner==p && m.attempt.state==State.RESERVED;}
    public void won(Player p,NhanBan boss) {
        synchronized(p) {
            Match m=matches.get(p.id);
            if(m==null || m.boss!=boss || m.owner!=p || p.zone!=m.origin)return;
            long now=System.currentTimeMillis();
            if(!m.attempt.win(now)){if(m.attempt.due(now))lose(p);return;}
            try {
                if(!dao.transition(m.attempt,State.FIGHT)){m.attempt.state=State.RETURN;return;}
                clearCombat(m);
                ItemMap it=new ItemMap(m.origin,638,1,boss.location.x,boss.location.y,p.id,true);
                it.commesonMatchId=m.attempt.matchId;
                it.options.clear();it.options.add(new Item.ItemOption(93,30));it.options.add(new Item.ItemOption(30,0));
                m.bottle=it;Service.gI().dropItemMap(m.origin,it);
                Service.gI().sendThongBao(p,"Bạn đã thắng! Hãy nhặt bình Commeson trong 60 giây");
            } catch(Exception e){Logger.logException(getClass(),e);m.attempt.state=State.RETURN;m.attempt.deadline=now;}
        }
    }
    private void clearCombat(Match m) {
        Player p=m.owner;
        try{if(p.effectSkill!=null)EffectSkillService.gI().removePKCommeson(p);}catch(Exception e){Logger.logException(getClass(),e);}
        try{if(p.pvp instanceof PKCommeson)p.pvp.dispose();}catch(Exception e){Logger.logException(getClass(),e);}
        try{if(p.nPoint!=null)PlayerService.gI().changeAndSendTypePK(p,nro.models.consts.ConstPlayer.NON_PK);}catch(Exception e){Logger.logException(getClass(),e);}
    }
    public void lose(Player p) {
        synchronized(p) {
            Match m=matches.get(p.id);if(m==null || m.returning || m.attempt.state!=State.FIGHT)return;
            transitionReturn(m);clearCombat(m);removeBoss(m);
            Service.gI().sendThongBao(p,"Bạn đã thất bại, ngày mai hãy thử sức tiếp");
            returnHome(m);
        }
    }
    private void transitionReturn(Match m) {
        State old=m.attempt.state;m.attempt.state=State.RETURN;m.attempt.deadline=System.currentTimeMillis();
        try{dao.transition(m.attempt,old);}catch(Exception e){Logger.logException(getClass(),e);}
    }
    private void removeBoss(Match m){NhanBan b=m.boss;try{if(b!=null){b.leaveMap();if(b.isRemoved())m.boss=null;}}catch(Exception e){Logger.logException(getClass(),e);}}
    private void removeBottle(Match m){ItemMap it=m.bottle;try{if(it!=null && !it.isPickedUp && it.zone!=null)ItemMapService.gI().removeItemMapAndSendClient(it);m.bottle=null;}catch(Exception e){Logger.logException(getClass(),e);}}
    private void complete(Match m) {
        State old=m.attempt.state;m.attempt.state=State.DONE;
        try{if(dao.finish(m.attempt.matchId))matches.remove(m.owner.id,m);else m.attempt.state=old;}
        catch(Exception e){m.attempt.state=old;Logger.logException(getClass(),e);}
    }
    private void returnHome(Match m) {
        if(m.returning)return;m.returning=true;
        try {
            clearCombat(m);removeBottle(m);removeBoss(m);
            if(m.boss!=null || m.bottle!=null)return;
            if(m.owner.zone!=m.origin){complete(m);return;}
            if(DungeonExitService.exit(m.owner,m.origin,24+m.owner.gender,-1,-1))complete(m);
        }finally{m.returning=false;}
    }
    /** Called on the old-zone departure, only when map movement really starts. */
    public void onMapExit(Player p,Zone origin) {
        Match m=matches.get(p.id);if(m==null || m.owner!=p || m.origin!=origin || m.returning)return;
        synchronized(p){
            if(m.quarantine)return;
            if(p.beforeDispose){
                if(m.attempt.state==State.FIGHT)transitionReturn(m);
                clearCombat(m);removeBottle(m);removeBoss(m);if(m.boss==null && m.bottle==null)matches.remove(p.id,m);return;
            }
            // Movement itself is the requested exit. Never send a second ship here.
            transitionReturn(m);clearCombat(m);removeBottle(m);removeBoss(m);
        }
    }
    /** Restore a pending return, never recreate a fight or a reward after reconnect. */
    public void onLogin(Player p) {
        synchronized(p){
            Match existing=matches.remove(p.id);
            if(existing!=null){synchronized(existing.owner){clearCombat(existing);removeBottle(existing);removeBoss(existing);}}
            try {
                CommesonAttempt a=dao.pending(p.id);if(a==null)return;
                if(a.state==State.RESERVED){
                    if(existing!=null && (existing.boss!=null || existing.bottle!=null)){
                        Match recovering=new Match(a,p);recovering.boss=existing.boss;recovering.bottle=existing.bottle;matches.put(p.id,recovering);return;
                    }
                    dao.releasePreStart(a);return;
                }
                Match m=new Match(a,p);
                if(existing!=null){m.boss=existing.boss;m.bottle=existing.bottle;}
                matches.put(p.id,m);
                if(p.zone==null || p.zone.map.mapId!=140){complete(m);return;}
                if(a.state==State.FIGHT)transitionReturn(m);
                else if(a.state==State.WON || a.state==State.PICKING)restoreUnpicked(m);
                EffectSkillService.gI().removePKCommeson(p);
            }catch(Exception e){Logger.logException(getClass(),e);}
        }
    }
    public void update(Player p) {
        try{updateInternal(p);}catch(Exception e){Logger.logException(getClass(),e);}
    }
    private void updateInternal(Player p)throws java.sql.SQLException {
        Match m=matches.get(p.id);if(m==null || m.owner!=p)return;
        synchronized(p){
            if(m.quarantine){reconcilePickup(m);return;}
            if(m.attempt.state==State.RESERVED){
                clearCombat(m);removeBottle(m);removeBoss(m);
                if(m.boss==null && m.bottle==null){dao.releasePreStart(m.attempt);matches.remove(p.id,m);}
                return;
            }
            if(m.returning || p.zone==null)return;
            if(p.zone!=m.origin){clearCombat(m);removeBottle(m);removeBoss(m);if(m.boss==null && m.bottle==null)complete(m);return;}
            if((m.attempt.state==State.WON || m.attempt.state==State.PICKING) && m.bottle==null)restoreUnpicked(m);
            if(m.attempt.state==State.FIGHT && (p.isDie() || m.attempt.due(System.currentTimeMillis())))lose(p);
            else if(m.attempt.state==State.RETURN || m.attempt.due(System.currentTimeMillis())){
                if(m.attempt.state!=State.RETURN)transitionReturn(m);returnHome(m);
            }
        }
    }
    /** SQL WON/PICKING proves no committed bag grant; restore only the original remaining time. */
    private void restoreUnpicked(Match m)throws java.sql.SQLException {
        if(m.attempt.due(System.currentTimeMillis())){transitionReturn(m);return;}
        if(m.attempt.state==State.PICKING){
            m.attempt.state=State.WON;
            try{if(!dao.transition(m.attempt,State.PICKING)){m.attempt.state=State.PICKING;return;}}
            catch(java.sql.SQLException e){m.attempt.state=State.PICKING;throw e;}
        }
        if(m.attempt.state==State.WON && m.bottle==null){
            Player p=m.owner;
            ItemMap it=new ItemMap(m.origin,638,1,p.location.x,p.location.y,p.id,true);
            it.commesonMatchId=m.attempt.matchId;
            // ItemMap's autonomous expiry must share the original absolute win deadline.
            it.createTime=m.attempt.deadline-60_000;
            it.options.clear();it.options.add(new Item.ItemOption(93,30));it.options.add(new Item.ItemOption(30,0));
            m.bottle=it;Service.gI().dropItemMap(m.origin,it);
        }
    }
    /** Reserve durably before adding to inventory; a crash cannot replay a bottle. */
    public boolean beforePickup(Player p,ItemMap it) {
        if(it.commesonMatchId==null)return true;
        if(p.walletPurchasePending!=null)return false;
        Match m=matches.get(p.id);
        if(m==null || m.owner!=p || m.bottle!=it || !m.attempt.matchId.equals(it.commesonMatchId) || p.zone!=m.origin || m.attempt.state!=State.WON || m.attempt.due(System.currentTimeMillis()))return false;
        try{m.beforeBag=new ArrayList<>();for(Item item:p.inventory.itemsBag)m.beforeBag.add(ItemService.gI().copyItem(item));m.attempt.state=State.PICKING;if(dao.transition(m.attempt,State.WON))return true;m.attempt.state=State.WON;}
        catch(Exception e){m.attempt.state=State.WON;m.quarantine=true;reconcilePickup(m);Logger.logException(getClass(),e);}return false;
    }
    public boolean afterPickup(Player p,ItemMap it,boolean added) {
        if(it.commesonMatchId==null)return added;
        Match m=matches.get(p.id);if(m==null || m.bottle!=it)return false;
        try {
            if(!added){m.attempt.state=State.WON;dao.transition(m.attempt,State.PICKING);m.beforeBag=null;return false;}
            m.attempt.state=State.PICKED;m.attempt.deadline=System.currentTimeMillis()+60_000;
            dao.persistPickup(m.attempt,serializeBag(p));m.bottle=null;m.beforeBag=null;
            Service.gI().sendThongBao(p,"Đã nhận bình Commeson. Tàu sẽ đưa bạn về sau 60 giây");
            return true;
        }catch(Exception e){Logger.logException(getClass(),e);m.quarantine=true;reconcilePickup(m);return !m.quarantine && m.attempt.state==State.PICKED;}
    }
    public static boolean hasPending(Player p){Match m=INSTANCE.matches.get(p.id);return m!=null && m.quarantine;}
    public static boolean hasPendingId(long id){Match m=INSTANCE.matches.get(id);return m!=null && m.quarantine;}
    public void reconcile(Player p){Match m=matches.get(p.id);if(m!=null && m.quarantine)synchronized(p){reconcilePickup(m);}}
    private void reconcilePickup(Match m){
        try {
            CommesonAttempt durable=dao.pending(m.owner.id);
            if(durable==null || !durable.matchId.equals(m.attempt.matchId))return;
            if(durable.state==State.PICKED){
                m.attempt.state=State.PICKED;m.attempt.deadline=durable.deadline;
                if(m.bottle!=null){m.bottle.isPickedUp=true;ItemMapService.gI().sendItemMapDisappear(m.bottle);if(m.bottle.zone!=null)m.bottle.zone.removeItemMap(m.bottle);m.bottle=null;}
                m.beforeBag=null;m.quarantine=false;InventoryService.gI().sendItemBags(m.owner);return;
            }
            if(durable.state==State.PICKING || durable.state==State.WON){
                if(m.beforeBag!=null){m.owner.inventory.itemsBag.clear();m.owner.inventory.itemsBag.addAll(m.beforeBag);}
                m.attempt.state=State.WON;m.attempt.deadline=durable.deadline;
                if(durable.state==State.WON || dao.transition(m.attempt,State.PICKING)){m.beforeBag=null;m.quarantine=false;InventoryService.gI().sendItemBags(m.owner);}
            }
        }catch(Exception e){Logger.logException(getClass(),e);}
    }
    private static String serializeBag(Player p){
        org.json.simple.JSONArray bag=new org.json.simple.JSONArray();
        for(Item item:p.inventory.itemsBag){
            org.json.simple.JSONArray row=new org.json.simple.JSONArray(),options=new org.json.simple.JSONArray();
            if(item.isNotNullItem()){
                row.add(item.template.id);row.add(item.quantity);
                for(Item.ItemOption io:item.itemOptions){org.json.simple.JSONArray opt=new org.json.simple.JSONArray();opt.add(io.optionTemplate.id);opt.add(io.param);options.add(opt.toJSONString());}
            }else{row.add(-1);row.add(0);}
            row.add(options.toJSONString());row.add(item.createTime);bag.add(row.toJSONString());
        }return bag.toJSONString();
    }
}
