package nro.models.services;
import com.google.gson.Gson;
import nro.models.player.*;
import nro.models.mob.Mob;
import nro.models.boss.Boss;
import nro.models.database.KolRewardDAO;
import nro.models.task.KolQuestCatalog;
public final class KolQuestService  {
    private static final KolQuestService INSTANCE=new KolQuestService(new KolRewardDAO());
    private final KolRewardDAO dao;
    private final java.util.function.Predicate<Player> logoutSaver;
    private static final java.util.Map<Player,PendingProgress> PROGRESS_PENDING=new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.ScheduledExecutorService PROGRESS_RECOVERY=java.util.concurrent.Executors.newSingleThreadScheduledExecutor(task->{Thread t=new Thread(task,"KOL progress reconciliation");t.setDaemon(true);return t;});
    private static final class PendingProgress {
        final KolQuestService owner; final Player player; final int account;
        boolean finalizing;
        PendingProgress(KolQuestService owner,Player player){this.owner=owner;this.player=player;this.account=player.session.userId;}
    }
    public static boolean hasProgressPending(Player p){return p!=null&&PROGRESS_PENDING.containsKey(p);}
    public static boolean hasProgressPendingId(long id){return PROGRESS_PENDING.keySet().stream().anyMatch(p->p.id==id);}
    public static boolean hasProgressPendingAccount(int account){return PROGRESS_PENDING.values().stream().anyMatch(op->op.account==account);}
    public static boolean isFinalizingProgressLogout(Player p){PendingProgress op=PROGRESS_PENDING.get(p);return op!=null&&op.finalizing&&Thread.holdsLock(p);}
    private void retainProgress(Player p){
        PendingProgress op=new PendingProgress(this,p);PROGRESS_PENDING.put(p,op);
        if(this==INSTANCE)scheduleProgress(op);
    }
    private void scheduleProgress(PendingProgress op){
        PROGRESS_RECOVERY.schedule(()->{
            if(PROGRESS_PENDING.get(op.player)!=op)return;
            if(!op.owner.reconcileProgressPending(op.player)){scheduleProgress(op);return;}
            if(op.player.beforeDispose)op.player.dispose();
        },3,java.util.concurrent.TimeUnit.SECONDS);
    }
    public KolQuestService(KolRewardDAO dao) {
        this(dao,nro.models.database.PlayerDAO::savePendingKolLogout);
    }
    public KolQuestService(KolRewardDAO dao,java.util.function.Predicate<Player> logoutSaver) {
        this.dao=dao;this.logoutSaver=logoutSaver;
    }
    public static KolQuestService gI() {
        return INSTANCE;
    }
    public static boolean primary(Player p) {
        return p!=null && p.isPl();
    }
    public boolean reconcileProgressPending(Player p) {
        PendingProgress op=PROGRESS_PENDING.get(p);
        if(op!=null&&op.owner!=this)return op.owner.reconcileProgressPending(p);
        synchronized (p) {
            if (!p.kolProgressPending && op==null) return true;
            try {
              if(p.kolProgressPending) {
                KolQuestState stored = KOLProgressData.parse(dao.readProgress(p.id,p.getSession().userId)).v2;
                if (!stored.valid() || p.kolQuestState == null || !p.kolQuestState.valid()
                        || stored.normalStage != p.kolQuestState.normalStage || stored.vipStage != p.kolQuestState.vipStage) return false;
                KolQuestState live = p.kolQuestState;
                live.woodenDummyKills=Math.max(live.woodenDummyKills,stored.woodenDummyKills);
                live.birdDemonKills=Math.max(live.birdDemonKills,stored.birdDemonKills);
                live.fide1Kills=Math.max(live.fide1Kills,stored.fide1Kills);
                live.fide2Kills=Math.max(live.fide2Kills,stored.fide2Kills);
                live.fide3Kills=Math.max(live.fide3Kills,stored.fide3Kills);
                live.perfectCellKills=Math.max(live.perfectCellKills,stored.perfectCellKills);
                live.pinkCapsulesPicked=Math.max(live.pinkCapsulesPicked,stored.pinkCapsulesPicked);
                live.blackGokuKills=Math.max(live.blackGokuKills,stored.blackGokuKills);
                live.superBlackGokuKills=Math.max(live.superBlackGokuKills,stored.superBlackGokuKills);
                p.kolProgressPending=false;
              }
              if(op!=null&&p.beforeDispose){
                op.finalizing=true;
                try{if(!logoutSaver.test(p))return false;}finally{op.finalizing=false;}
              }
              if(op!=null)PROGRESS_PENDING.remove(p,op);
              return true;
            } catch(Exception failure) { return false; }
        }
    }

    public Claim completeCurrentForAdmin(Player p) {
        if (!AdminCommandService.requireAdmin(p)) return new Claim(false,"Chỉ dành cho admin.");
        synchronized (p) {
            synchronized (p.session) {
                if (!AdminCommandService.requireAdmin(p)) return new Claim(false,"Phiên admin không hợp lệ.");
                if (AdminPlayerActions.hasRenamePendingId(p.id) || AdminPlayerActions.hasRenamePendingAccount(p.session.userId) || RubyGiftService.hasPendingId(p.id) || RubyGiftService.hasPendingAccount(p.session.userId)
                        || p.kolRewardPending || p.walletPurchasePending != null || p.kolProgressPending || hasProgressPendingId(p.id) || hasProgressPendingAccount(p.session.userId))
                    return progressNotice(p,false,"Giao dịch đang chờ đối chiếu. Hãy thử lại sau.");
                KolQuestState state=p.kolQuestState;
                if (state==null || !state.valid()) return progressNotice(p,false,"Dữ liệu KOL không hợp lệ.");
                int stage=p.kolMenuVip?state.vipStage:state.normalStage;
                if (stage==8) return progressNotice(p,false,"Đã hoàn thành tất cả nhiệm vụ KOL.");
                KolQuestState next=state.copy(); long required=KolQuestCatalog.required(stage);
                switch(stage) {
                    case 1 -> next.woodenDummyKills=Math.max(next.woodenDummyKills,required);
                    case 2 -> next.birdDemonKills=Math.max(next.birdDemonKills,required);
                    case 3 -> { next.fide1Kills=Math.max(next.fide1Kills,required); next.fide2Kills=Math.max(next.fide2Kills,required); next.fide3Kills=Math.max(next.fide3Kills,required); }
                    case 4 -> next.perfectCellKills=Math.max(next.perfectCellKills,required);
                    case 5 -> next.pinkCapsulesPicked=Math.max(next.pinkCapsulesPicked,required);
                    case 6 -> next.blackGokuKills=Math.max(next.blackGokuKills,required);
                    case 7 -> next.superBlackGokuKills=Math.max(next.superBlackGokuKills,required);
                }
                try {
                    dao.saveProgress(p.id,p.session.userId,new Gson().toJson(KOLProgressData.snapshot(p,next)));
                } catch(Exception failure) {
                    if (failure instanceof KolRewardDAO.CommitUncertain) {p.kolProgressPending=true;retainProgress(p);}
                    return progressNotice(p,false,"Không thể xác nhận lưu tiến độ KOL. Hãy thử lại sau.");
                }
                p.kolQuestState=next; p.kolMenuQuest=0;
                return progressNotice(p,true,"Đã đủ tiến độ "+(p.kolMenuVip?"KOL VIP":"KOL thường")+" nhiệm vụ "+stage+". Mở lại menu để nhận thưởng.");
            }
        }
    }

    private Claim progressNotice(Player p,boolean success,String message) {
        Service.gI().sendThongBao(p,message); return new Claim(success,message);
    }

    public void onMobKilled(Player p,Mob m) {
        if(!primary(p)||m==null||m.zone==null||m.zone.map==null)return;
        synchronized(p) {
            KolQuestState s=p.kolQuestState;
            if(s==null||!s.valid()||p.itemTime==null||!p.itemTime.isUseTDLT||p.itemTime.timeTDLT<=0||System.currentTimeMillis()-p.itemTime.lastTimeUseTDLT>=p.itemTime.timeTDLT)return;
            if(m.tempId==0)s.woodenDummyKills=KolQuestState.add(s.woodenDummyKills,1);
            else if(m.tempId==50&&(m.zone.map.mapId==77||m.zone.map.mapId==81))s.birdDemonKills=KolQuestState.add(s.birdDemonKills,1);
        }
    }
    public void onBossKilled(Player p,Boss boss) {
        if(!primary(p)||boss==null)return;
        synchronized(p) {
            KolQuestState s=p.kolQuestState;
            if(s==null||!s.valid())return;
            if(boss.id==-28) {
                switch(boss.currentLevel) {
                    case 0->s.fide1Kills=KolQuestState.add(s.fide1Kills,1);
                    case 1->s.fide2Kills=KolQuestState.add(s.fide2Kills,1);
                    case 2->s.fide3Kills=KolQuestState.add(s.fide3Kills,1);
                }
            }
            else if(boss.id==-101&&boss.currentLevel==0&&boss.zone!=null&&boss.zone.map.mapId==103)s.perfectCellKills=KolQuestState.add(s.perfectCellKills,1);
            else if(boss.id==-203) {
                if(boss.currentLevel==0)s.blackGokuKills=KolQuestState.add(s.blackGokuKills,1);
                else if(boss.currentLevel==1)s.superBlackGokuKills=KolQuestState.add(s.superBlackGokuKills,1);
            }
        }
    }
    public void onItemPicked(Player p,int item,int quantity) {
        if(!primary(p)||item!=722||quantity<=0)return;
        synchronized(p) {
            if(p.kolQuestState!=null&&p.kolQuestState.valid())p.kolQuestState.pinkCapsulesPicked=KolQuestState.add(p.kolQuestState.pinkCapsulesPicked,quantity);
        }
    }
    public boolean hasTicket(Player p) {
        if(p.inventory==null||p.inventory.itemsBag==null)return false;
        return p.inventory.itemsBag.stream().anyMatch(i->i!=null&&i.template!=null&&i.template.id==1825&&i.quantity>0);
    }
    public String eligibility(Player p,boolean vip,int expected) {
        if(!primary(p)||p.getSession()==null)return "Nhân vật không hợp lệ.";
        KolQuestState s=p.kolQuestState;
        if(s==null||!s.valid())return "Dữ liệu KOL không hợp lệ. Vui lòng liên hệ quản trị viên.";
        int stage=vip?s.vipStage:s.normalStage;
        if(stage==8)return "Đã hoàn thành tất cả nhiệm vụ KOL.";
        if(expected!=stage)return "Hãy mở lại nhiệm vụ KOL hiện tại.";
        if(vip&&!hasTicket(p))return "Cần Vé Nhiệm Vụ VIP ID 1825 trong hành trang.";
        if(KolQuestCatalog.progress(s,stage)<KolQuestCatalog.required(stage))return "Chưa đủ tiến độ nhiệm vụ.";
        return null;
    }
    public record Claim(boolean success,String message) {
    }
    public boolean reconcilePending(Player p) {
        synchronized(p) {
            if(!p.kolRewardPending)return true;
            try {
                var resolved=dao.reconcile(p.id,p.getSession().userId,p.kolPendingVip,p.kolPendingQuest);
                if(resolved!=null) {
                    var stored=KOLProgressData.parse(resolved.json()).v2;
                    if(!stored.valid())return false;
                    int points=Math.addExact(resolved.points(),p.event.getEventPoint()-p.kolPendingPoints);
                    p.kolQuestState.normalStage=stored.normalStage;p.kolQuestState.vipStage=stored.vipStage;
                    p.getSession().vnd=resolved.vnd();p.event.setEventPoint(points);
                }
                p.kolRewardPending=false;return true;
            } catch(Exception e) { return false; }
        }
    }
    public Claim claim(Player p,boolean vip,int expected) {
        if(p==null)return new Claim(false,"Nhân vật không hợp lệ.");
        synchronized(p) {
            if(AdminPlayerActions.hasRenamePendingId(p.id) || (p.session!=null && AdminPlayerActions.hasRenamePendingAccount(p.session.userId)))return new Claim(false,"Đổi tên đang chờ đối chiếu dữ liệu.");
            if(p.kolProgressPending || hasProgressPendingId(p.id)) return new Claim(false,"Tiến độ KOL đang chờ đối chiếu. Hãy mở lại menu sau.");
            if(p.walletPurchasePending!=null) {
                WalletPurchaseService.gI().reconcilePending(p);
                return new Claim(false,"Hãy đối chiếu yêu cầu mua trước và mở lại menu KOL.");
            }
            if(p.kolRewardPending) {
                boolean resolved=reconcilePending(p);
                return new Claim(false,resolved?"Giao d\u1ecbch KOL \u0111\u00e3 \u0111\u01b0\u1ee3c \u0111\u1ed1i chi\u1ebfu. H\u00e3y m\u1edf l\u1ea1i menu.":"Giao d\u1ecbch KOL \u0111ang ch\u1edd \u0111\u1ed1i chi\u1ebfu. Vui l\u00f2ng th\u1eed l\u1ea1i sau.");
            }
            String error=eligibility(p,vip,expected);
            if(error!=null)return new Claim(false,error);
            KolQuestState next=p.kolQuestState.copy();
            if(vip)next.vipStage++;
            else next.normalStage++;
            var reward=KolQuestCatalog.reward(expected,vip);
            p.kolRewardPending=true;p.kolPendingQuest=expected;p.kolPendingVip=vip;p.kolPendingPoints=p.event.getEventPoint();
            try {
                var balance=dao.claim(p.id,p.getSession().userId,vip,expected,reward.vnd(),reward.points(),p.event.getEventPoint(),new Gson().toJson(KOLProgressData.snapshot(p,next)));
                p.kolRewardPending=false;
                p.getSession().vnd=balance.vnd();
                p.event.setEventPoint(balance.points());
                p.kolQuestState=KOLProgressData.parse(balance.json()).v2;
                if(balance.newlyClaimed()) nro.models.task.BadgesTaskService.updateCountBagesTask(p,15,reward.points());
                return new Claim(true,balance.newlyClaimed()?"Đã nhận "+(vip?"KOL VIP":"KOL thường")+": "+reward.vnd()+" VND + "+reward.points()+" điểm.":"Phần thưởng đã được ghi nhận.");
            }
            catch(Exception e) {
                if(!(e instanceof KolRewardDAO.CommitUncertain))p.kolRewardPending=false;
                return new Claim(false,"Không thể xác nhận giao dịch KOL. Vui lòng thử lại sau.");
            }
        }
    }
    public String menuText(Player p,boolean vip) {
        synchronized(p) {
            KolQuestState s=p.kolQuestState;
            if(s==null||!s.valid())return "Dữ liệu KOL không hợp lệ.";
            int id=vip?s.vipStage:s.normalStage;
            if(id==8)return "Đã hoàn thành tất cả 7 nhiệm vụ "+(vip?"KOL VIP":"KOL thường")+".";
            var r=KolQuestCatalog.reward(id,vip);
            String text="Nhiệm vụ "+id+" "+(vip?"KOL VIP":"KOL thường")+"\n"+KolQuestCatalog.description(id)+"\nTiến độ: "+KolQuestCatalog.progress(s,id)+"/"+KolQuestCatalog.required(id)+"\nThưởng: "+r.vnd()+" VND + "+r.points()+" điểm";
            if(id==3)text+="\nFide 1/2/3: "+s.fide1Kills+"/"+s.fide2Kills+"/"+s.fide3Kills;
            if(vip&&!hasTicket(p))text+="\nC\u1ea7n v\u00e9 VIP ID 1825 trong h\u00e0nh trang \u0111\u1ec3 nh\u1eadn th\u01b0\u1edfng.";
            return text;
        }
    }
}
