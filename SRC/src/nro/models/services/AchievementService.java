package nro.models.services;

import nro.models.consts.ConstAchievement;
import nro.models.mob.Mob;
import nro.models.player_system.Template.AchievementTemplate;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.server.Manager;
import nro.models.skill.Skill;
import nro.models.utils.Util;
import nro.models.database.AchievementRewardDAO;
import nro.models.database.PlayerDAO;
import com.google.gson.JsonArray;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

/**
 *
 * @author By Mr Blue
 *
 */
public class AchievementService {

    public static final int REWARD_RUBY = 1000;
    private static AchievementService instance;
    private final AchievementRewardDAO rewards;
    private final Predicate<Player> logoutSaver;
    private static final Map<Player,Pending> PENDING = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService RECOVERY = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread thread = new Thread(task, "Achievement reward reconciliation"); thread.setDaemon(true); return thread;
    });
    private static final class Pending {
        final AchievementService service; final Player player; final AchievementRewardDAO.Snapshot snapshot;
        final int ruby; boolean applied; AchievementRewardDAO.Result result;
        Pending(AchievementService service, Player player, AchievementRewardDAO.Snapshot snapshot, int ruby) {
            this.service = service; this.player = player; this.snapshot = snapshot; this.ruby = ruby;
        }
    }
    public AchievementService() { this(new AchievementRewardDAO(), PlayerDAO::saveLiveCommandLogout); }
    public AchievementService(AchievementRewardDAO rewards, Predicate<Player> logoutSaver) { this.rewards = rewards; this.logoutSaver = logoutSaver; }
    private void schedule(Pending pending) {
        if (this != instance) return;
        RECOVERY.schedule(() -> { if (PENDING.get(pending.player) == pending && !reconcilePending(pending.player)) schedule(pending); }, 2, TimeUnit.SECONDS);
    }
    public boolean reconcilePending(Player player) {
        Pending pending = PENDING.get(player);
        if (pending == null) return true;
        synchronized (player) {
            try {
                if (!pending.applied) {
                    pending.result = pending.service.rewards.claim(pending.snapshot);
                    player.inventory.ruby = pending.ruby;
                    player.achievement.mergeStored(pending.result.achievements());
                    player.achievement.reward(pending.snapshot.index()); pending.applied = true;
                }
                if (player.beforeDispose && !pending.service.logoutSaver.test(player)) return false;
                PENDING.remove(player, pending); LiveSyncCommandGate.clear(player.id, pending.snapshot.account());
                if (player.beforeDispose) { player.liveLogoutCompleted = true; LiveSyncCommandGate.retired(player.id, pending.snapshot.account()); player.dispose(); }
                else pending.service.rewardAcknowledgement(player, pending.snapshot.index());
                return true;
            } catch (SQLException failure) { return false; }
        }
    }
    public boolean drainPending() {
        for (Pending pending : PENDING.values()) if (!reconcilePending(pending.player)) return false;
        return PENDING.isEmpty();
    }

    public static AchievementService gI() {
        if (instance == null) {
            instance = new AchievementService();
        }
        return instance;
    }

    public void openAchievementUI(Player player) {
        Message msg = null;
        try {
            msg = new Message(-76);
            msg.writer().writeByte(0);
            msg.writer().writeByte(Manager.ACHIEVEMENT_TEMPLATE.size());
            for (int i = 0; i < Manager.ACHIEVEMENT_TEMPLATE.size(); i++) {
                AchievementTemplate at = Manager.ACHIEVEMENT_TEMPLATE.get(i);
                msg.writer().writeUTF(at.info1); // info 1
                msg.writer().writeUTF(regex(player, at.info2) + " (" + numberToString(player.achievement.getCompleted(i)) + "/" + numberToString(at.maxCount) + ")"); // info 2
                msg.writer().writeShort(REWARD_RUBY); // money
                msg.writer().writeBoolean(player.achievement.isFinish(i, at.maxCount));// isFinish
                msg.writer().writeBoolean(player.achievement.isRecieve(i)); // isRecieve
            }
            player.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void confirmAchievement(Player player, byte select) {
        if (player == null || !player.isPl()) return;
        synchronized (player) {
            if (select < 0 || select >= Manager.ACHIEVEMENT_TEMPLATE.size() || player.achievement == null) return;
            if (!player.achievement.canReward(select)) {
                Service.gI().sendThongBao(player, player.achievement.isRecieve(select) ? "Bạn đã nhận phần thưởng này." : "Nhiệm vụ chưa hoàn thành."); return;
            }
            if (player.beforeDispose || player.inventory == null || player.session == null || player.session.userId <= 0) return;
            if (LiveSyncMutationGate.blocked(player) || player.walletPurchasePending != null || WalletPurchaseService.hasPending(player)
                    || RubyGiftService.hasPending(player) || nro.models.shop_ky_gui.ConsignTransactions.hasPending(player)
                    || CommesonChallengeService.hasPending(player) || player.kolProgressPending || player.kolRewardPending
                    || KolQuestService.hasProgressPendingId(player.id) || AdminPlayerActions.hasRenamePendingId(player.id)
                    || player.tradeWVP || nro.models.services_func.TransactionService.isTrading(player)) {
                Service.gI().sendThongBao(player, "Có giao dịch đang chờ xử lý, vui lòng thử lại sau."); return;
            }
            long nextRuby = (long) player.inventory.ruby + REWARD_RUBY;
            if (player.inventory.ruby < 0 || nextRuby + VipRewardCatalog.reservedRuby(player) > Integer.MAX_VALUE) {
                Service.gI().sendThongBao(player, "Hồng ngọc vượt giới hạn, chưa nhận thưởng."); return;
            }
            JsonArray inventory = new JsonArray(); inventory.add(player.inventory.gold); inventory.add(player.inventory.gem);
            inventory.add(nextRuby); inventory.add(player.inventory.coupon); inventory.add(player.inventory.event);
            var snapshot = new AchievementRewardDAO.Snapshot(player.id, player.session.userId, select, inventory.toString(), player.achievement.toJson(select));
            Pending pending = new Pending(this, player, snapshot, (int) nextRuby);
            LiveSyncCommandGate.mark(player.id, player.session.userId);
            try {
                AchievementRewardDAO.Result result = rewards.claim(snapshot);
                player.achievement.mergeStored(result.achievements());
                if (result.credited()) player.inventory.ruby = (int) nextRuby;
                LiveSyncCommandGate.clear(player.id, player.session.userId);
                if (result.credited()) rewardAcknowledgement(player, select);
                else { Service.gI().sendThongBao(player, "Bạn đã nhận phần thưởng này."); openAchievementUI(player); }
            } catch (AchievementRewardDAO.CommitUncertain uncertain) {
                PENDING.put(player, pending); schedule(pending);
                Service.gI().sendThongBao(player, "Đang đối soát phần thưởng, vui lòng chờ.");
            } catch (SQLException failure) {
                LiveSyncCommandGate.clear(player.id, player.session.userId);
                Service.gI().sendThongBao(player, "Chưa lưu được phần thưởng, vui lòng thử lại sau.");
            }
        }
    }
    private void rewardAcknowledgement(Player player, int index) {
        Service.gI().sendMoney(player);
        Service.gI().sendThongBao(player, "Bạn vừa nhận được 1.000 hồng ngọc.");
        Message message = null;
        try { message = new Message(-76); message.writer().writeByte(1); message.writer().writeByte(index); player.sendMessage(message); }
        catch (Exception failure) { /* The durable claim survives a disconnected client. */ }
        finally { if (message != null) message.cleanup(); }
    }

    public String numberToString(long num) {
        return num <= 10000 ? num + "" : Util.numberToMoney(num);
    }

    public String regex(Player player, String text) {
        int gen = player.gender;
        return text.replaceAll("%1", gen == 0 ? "Siêu nhân" : gen == 1 ? "Siêu Namếc" : "Siêu Xayda").replaceAll("%2", gen == 0 ? "Bunma" : gen == 1 ? "Dende" : "Appule");
    }

    public void checkDoneTask(Player player, int aId) {
        if (player != null && player.isPl() && player.achievement != null && !LiveSyncMutationGate.blocked(player)) {
            synchronized (player) {
            if (LiveSyncMutationGate.blocked(player)) return;
            switch (aId) {
                case ConstAchievement.LAN_DAU_NAP_NGOC ->
                    { if (player.getSession() != null) player.achievement.doneNotAdd(aId, player.getSession().tongnap); }
                default ->
                    player.achievement.done(aId, 1);
            }
            }
        }
    }

    public void checkDoneTask(Player player, int aId, long quantity) {
        if (player != null && player.isPl() && player.achievement != null && !LiveSyncMutationGate.blocked(player))
            synchronized (player) { if (!LiveSyncMutationGate.blocked(player)) player.achievement.done(aId, quantity); }
    }
    public void checkDoneTaskPvpWin(Player winner, Player loser) {
        if (winner != null && winner.achievement != null && !LiveSyncMutationGate.blocked(winner))
            synchronized (winner) { if (!LiveSyncMutationGate.blocked(winner)) winner.achievement.recordWin(loser); }
    }

    public void checkDoneTaskKillMob(Player player, Mob mob) {
        try {
            if (mob.lvMob > 0) {
                checkDoneTask(player, ConstAchievement.DANH_BAI_SIEU_QUAI);
            }
            if (mob.type == 4) {
                checkDoneTask(player, ConstAchievement.THO_SAN_THIEN_XA);
            }
            if (mob.tempId == 0) {
                checkDoneTask(player, ConstAchievement.TAP_LUYEN_BAI_BAN);
            }
        } catch (Exception e) {
        }
    }

    public void checkDoneTaskUseSkill(Player player) {
        if (player != null && player.isPl() && player.playerSkill != null && player.playerSkill.skillSelect != null && player.playerSkill.skillSelect.template != null) {
            switch (player.playerSkill.skillSelect.template.id) {
                case Skill.KAMEJOKO, Skill.MASENKO, Skill.ANTOMIC -> {
                    checkDoneTask(player, ConstAchievement.NOI_CONG_CAO_CUONG);
                }
                case Skill.DRAGON, Skill.DEMON, Skill.GALICK, Skill.LIEN_HOAN, Skill.KAIOKEN, Skill.DICH_CHUYEN_TUC_THOI -> {
                }
                default ->
                    checkDoneTask(player, ConstAchievement.KY_NANG_THANH_THAO);
            }
        }
    }

    public void checkDoneTaskFly(Player player, int length) {
        if (player != null && player.isPl() && player.achievement != null && !LiveSyncMutationGate.blocked(player)) {
            synchronized (player) { if (!LiveSyncMutationGate.blocked(player)) player.achievement.recordFlight(length); }
        }
    }

}
