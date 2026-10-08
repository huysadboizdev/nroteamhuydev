package nro.models.services;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import nro.models.boss.Boss;
import nro.models.boss.BossID;
import nro.models.boss.QuestSupportIdentity;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.BossStatus;
import nro.models.map.Zone;
import nro.models.map.service.MapService;
import nro.models.player.Player;
import nro.models.server.ServerNotify;
import nro.models.utils.Logger;

/** Tick-owned, bounded pool: constructor-created companion groups retain their original behavior. */
public final class QuestSupportService {
    public static final List<Integer> ROOT_IDS = List.of(BossID.TIEU_DOI_TRUONG, BossID.FIDE,
            BossID.DR_KORE, BossID.ANDROID_14, BossID.KING_KONG, BossID.XEN_BO_HUNG);
    public interface Backend {
        Boss create(int rootId);
        Zone chooseZone(Boss root, List<Boss> members, Set<Zone> reserved);
        void remove(Boss boss);
        void announce(String message);
    }
    private static final class LiveBackend implements Backend {
        public Boss create(int rootId) { return BossManager.gI().createBoss(rootId); }
        public void remove(Boss boss) { BossManager.gI().removeBoss(boss); }
        public void announce(String message) { ServerNotify.gI().notify(message); }
        public Zone chooseZone(Boss root, List<Boss> members, Set<Zone> reserved) {
            Zone best = null;
            int bestDemand = -1;
            int task = switch (root.questBossId()) {
                case BossID.TIEU_DOI_TRUONG -> 20;
                case BossID.FIDE -> 21;
                case BossID.DR_KORE -> 23;
                case BossID.ANDROID_14 -> 24;
                case BossID.KING_KONG -> 25;
                default -> 26;
            };
            for (int mapId : root.data[0].getMapJoin()) {
                var map = MapService.gI().getMapById(mapId);
                if (map == null || map.mapWidth <= 0) continue;
                for (Zone zone : List.copyOf(map.zones)) {
                    if (zone == null || reserved.contains(zone)) continue;
                    if (root.isZone01SpawnDisabled && map.zones.size() > 2 && zone.zoneId < 2) continue;
                    boolean occupied = BossManager.gI().getBosses().stream().anyMatch(b ->
                            !members.contains(b) && b.questBossId() == root.questBossId()
                            && (b.zone == zone || b.zoneFinal == zone));
                    if (occupied) continue;
                    int demand = 0;
                    for (Player player : zone.getPlayersSnapshot()) {
                        if (player != null && player.playerTask != null && player.playerTask.taskMain != null
                                && player.playerTask.taskMain.id == task && player.playerTask.taskMain.index >= 1
                                && player.playerTask.taskMain.index <= (task == 20 ? 5 : task == 23 ? 2 : 3)) demand++;
                    }
                    if (demand > bestDemand) { best = zone; bestDemand = demand; }
                }
            }
            return best;
        }
    }
    private static final class Holder {
        static final QuestSupportService INSTANCE = new QuestSupportService(
                Clock.systemUTC(), QuestSupportConfig.DEFAULT, new LiveBackend());
    }
    public static QuestSupportService gI() { return Holder.INSTANCE; }
    private static final class Chain {
        final Boss root;
        final List<Boss> members;
        long readyAt;
        boolean spawned, waiting, fighting;
        Chain(Boss root, List<Boss> members) { this.root = root; this.members = members; }
        boolean complete() { return members.stream().allMatch(b -> b.bossStatus == BossStatus.REST); }
    }
    private final Clock clock;
    private final QuestSupportConfig config;
    private final QuestSupportSchedule schedule;
    private final Backend backend;
    private final List<Chain> chains = new ArrayList<>();
    private final List<Boss> pendingCleanup = new ArrayList<>();
    private volatile boolean open;
    private boolean stopped;
    private long closesAt;
    private long lastErrorAt;
    public QuestSupportService(Clock clock, QuestSupportConfig config, Backend backend) {
        this.clock = clock; this.config = config; this.backend = backend;
        this.schedule = new QuestSupportSchedule(clock);
    }
    public boolean supportsOriginal(Boss boss) {
        return open && schedule.isOpen() && !boss.isQuestSupport() && ROOT_IDS.contains(boss.questBossId());
    }
    /** The boss state machine and notice ETA share the same effective original-group deadline. */
    public long originalRespawnAt(Boss boss) {
        long from = boss.questLastRest();
        int seconds = boss.questBaseRestSeconds();
        if (boss.questParent() == null && supportsOriginal(boss)) {
            for (Boss child : boss.getCompanionBosses()) {
                if (child == null) continue;
                if (child.bossStatus != BossStatus.REST) return -1;
                from = Math.max(from, child.questLastRest());
            }
            seconds = config.restSeconds();
        }
        return from + Math.max(0L, seconds) * 1000;
    }
    public synchronized List<Boss> snapshot() {
        return chains.stream().flatMap(c -> c.members.stream()).toList();
    }
    public synchronized int chainCount() { return chains.size(); }
    public synchronized void tick() {
        cleanup(List.copyOf(pendingCleanup));
        if (stopped) return;
        long now = clock.millis();
        boolean next = config.enabled() && schedule.isOpen();
        if (next != open) {
            open = next;
            if (open) closesAt = schedule.closingMillis();
            announce(open ? "Hỗ trợ boss nhiệm vụ đã mở (14:00–17:00): " + config.totalChains()
                    + " chuỗi, hồi " + config.restSeconds() + " giây."
                    : "Hỗ trợ boss nhiệm vụ đã đóng. Trận đang đánh có tối đa 5 phút để hoàn tất.");
        }
        boolean recalled = false;
        for (Chain chain : List.copyOf(chains)) {
            if (!chain.spawned && chain.root.bossStatus != BossStatus.REST) chain.spawned = true;
            if (chain.members.stream().anyMatch(Boss::hadQuestSupportCombat)) chain.fighting = true;
            boolean complete = chain.complete();
            if (!open) {
                if (!chain.fighting || complete || now >= closesAt + config.graceSeconds() * 1000L) {
                    if (chain.fighting && !complete && now >= closesAt + config.graceSeconds() * 1000L) recalled = true;
                    retire(chain);
                }
                continue;
            }
            if (chain.spawned && complete && !chain.waiting) {
                chain.waiting = true;
                chain.readyAt = now + config.restSeconds() * 1000L;
            }
        }
        if (recalled) announce("Đã thu hồi boss hỗ trợ nhiệm vụ còn lại sau thời gian kết thúc trận.");
        if (!open || !pendingCleanup.isEmpty()) return;
        for (int rootId : ROOT_IDS) {
            long present = chains.stream().filter(c -> c.root.questBossId() == rootId).count();
            while (present < config.totalChains() - 1) {
                if (!create(rootId, now)) break;
                present++;
            }
        }
    }
    private boolean create(int rootId, long now) {
        QuestSupportIdentity.begin();
        List<Boss> members = List.of();
        try {
            Boss root = backend.create(rootId);
            members = QuestSupportIdentity.constructingMembers();
            if (root == null || members.isEmpty() || members.size() > 5 || !members.contains(root))
                throw new IllegalStateException("Incomplete support group " + rootId);
            for (Boss member : members) {
                if (member.bossAppearTogether == null) throw new IllegalStateException("Missing companions");
                for (Boss[] group : member.bossAppearTogether) {
                    if (group == null) continue;
                    for (Boss child : group) {
                        if (child == null || !members.contains(child) || child.questParent() != member)
                            throw new IllegalStateException("Incomplete support companions");
                    }
                }
            }
            Zone zone = backend.chooseZone(root, members, reserved(rootId));
            if (zone == null) { cleanup(members); return false; }
            root.zoneFinal = zone;
            chains.add(new Chain(root, members));
            return true;
        } catch (Exception failure) {
            cleanup(QuestSupportIdentity.constructingMembers());
            if (now - lastErrorAt >= 30_000) {
                lastErrorAt = now;
                Logger.logException(QuestSupportService.class, failure, "Quest support construction");
            }
            return false;
        } finally { QuestSupportIdentity.end(); }
    }
    private Set<Zone> reserved(int rootId) {
        Set<Zone> zones = new HashSet<>();
        for (Chain chain : chains) {
            if (chain.root.questBossId() == rootId) {
                if (chain.root.zoneFinal != null) zones.add(chain.root.zoneFinal);
                if (chain.root.zone != null) zones.add(chain.root.zone);
            }
        }
        return zones;
    }
    /** Parent rest waits for every child, but form/member transitions remain unchanged. */
    public synchronized boolean canRespawn(Boss root) {
        if (!open || !schedule.isOpen() || root.isQuestSupportRetired()) return false;
        for (Chain chain : chains) {
            if (chain.root != root) continue;
            if (!chain.complete() || chain.waiting && clock.millis() < chain.readyAt) return false;
            if (chain.spawned && !chain.waiting) return false;
            if (chain.waiting) {
                Set<Zone> reserved = reserved(root.questBossId());
                reserved.remove(root.zoneFinal);
                Zone zone = backend.chooseZone(root, chain.members, reserved);
                if (zone == null) return false;
                root.zoneFinal = zone;
            }
            chain.waiting = false; chain.spawned = false; chain.fighting = false;
            chain.members.forEach(Boss::clearQuestSupportCombat);
            return true;
        }
        return false;
    }
    private void cleanup(List<Boss> members) {
        for (Boss boss : members) {
            boolean complete = true;
            try { boss.retireQuestSupport(); }
            catch (Exception failure) { complete = false; Logger.logException(QuestSupportService.class, failure, "Quest support retirement"); }
            try { backend.remove(boss); }
            catch (Exception failure) { complete = false; Logger.logException(QuestSupportService.class, failure, "Quest support removal"); }
            if (complete) pendingCleanup.remove(boss);
            else if (!pendingCleanup.contains(boss)) pendingCleanup.add(boss);
        }
    }
    private void announce(String message) {
        try { backend.announce(message); }
        catch (Exception failure) { Logger.logException(QuestSupportService.class, failure, "Quest support announcement"); }
    }
    private void retire(Chain chain) {
        cleanup(chain.members); chains.remove(chain);
    }
    public synchronized void shutdown() {
        stopped = true; open = false;
        for (Chain chain : List.copyOf(chains)) retire(chain);
        cleanup(List.copyOf(pendingCleanup));
    }
}
