package nro.models.services;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import nro.models.boss.*;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.BossStatus;
import nro.models.map.Zone;
import nro.models.map.service.ChangeMapService;
import nro.models.player.Player;
import java.util.concurrent.atomic.AtomicReference;
import static nro.models.services.PinkFixture.*;

public final class QuestSupportLifecycleTest {
    static void joinRetirementRace(boolean fullJoin) throws Exception {
        install();
        replace(nro.models.services_func.TransactionService.class,"i",blank(nro.models.services_func.TransactionService.class));
        var notify = blank(nro.models.server.ServerNotify.class);
        var messages = nro.models.server.ServerNotify.class.getDeclaredField("notifies"); messages.setAccessible(true); messages.set(notify,new ArrayList<String>());
        replace(nro.models.server.ServerNotify.class,"i",notify);
        var map = blank(Terrain.class); map.mapId = 80; map.mapWidth = 240;
        Zone target = new Zone(map,0,20);
        Boss boss = QuestSupportProgressTest.support(BossID.FIDE); boss.currentLevel = 0; boss.nPoint.hp = 10; boss.zoneFinal = target;
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch started = new CountDownLatch(1);
        Thread joining = new Thread(() -> { started.countDown(); try { if(fullJoin) boss.joinMap(); else boss.joinMapByZone(target); } catch(Throwable e) { failure.set(e); } });
        synchronized(boss) {
            joining.start(); check(started.await(2,TimeUnit.SECONDS), "join thread started");
            long deadline = System.nanoTime()+2_000_000_000L;
            while(joining.isAlive() && joining.getState()!=Thread.State.BLOCKED && System.nanoTime()<deadline) Thread.sleep(1);
            check(joining.getState()==Thread.State.BLOCKED, "join is waiting for retirement monitor");
            // Before the fix this occurs after zone selection, at real goToMap's commit lock.
            boss.retireQuestSupport();
        }
        joining.join(2000);
        check(!joining.isAlive() && failure.get()==null, "race thread finishes: "+failure.get());
        check(boss.isQuestSupportRetired() && boss.zone==null && !target.getBosses().contains(boss), "retired join cannot create untracked zombie: "+fullJoin);
        boss.joinMapByZone(target); boss.joinMap();
        check(boss.zone==null && !target.getBosses().contains(boss), "late direct joins denied");
    }
    static final class Time extends Clock {
        long now = Instant.parse("2026-10-07T06:59:59Z").toEpochMilli();
        public ZoneId getZone() { return ZoneOffset.UTC; }
        public Clock withZone(ZoneId zone) { return this; }
        public Instant instant() { return Instant.ofEpochMilli(now); }
        public long millis() { return now; }
        void at(String value) { now = Instant.parse(value).toEpochMilli(); }
    }
    static final class Backend implements QuestSupportService.Backend {
        final List<Zone> zones = new ArrayList<>();
        final List<String> notices = new ArrayList<>();
        final Set<Boss> removed = new HashSet<>();
        boolean missing, partial, failRemove;
        int created;
        Backend() throws Exception { for (int i = 0; i < 3; i++) zones.add(zone()); }
        public Boss create(int id) {
            created++;
            Boss root = BossManager.gI().createBoss(id);
            if (partial) throw new IllegalStateException("injected partial construction");
            return root;
        }
        public Zone chooseZone(Boss root, List<Boss> members, Set<Zone> reserved) {
            return missing ? null : zones.stream().filter(z -> !reserved.contains(z)).findFirst().orElse(null);
        }
        public void remove(Boss boss) {
            if (failRemove) { failRemove = false; throw new IllegalStateException("injected remove failure"); }
            removed.add(boss); BossManager.gI().removeBoss(boss);
        }
        public void announce(String message) { notices.add(message); }
    }
    static List<Boss> roots(QuestSupportService service) { return service.snapshot().stream().filter(b -> b.questParent() == null).toList(); }
    static void finish(QuestSupportService service) { for (Boss b : service.snapshot()) b.bossStatus = BossStatus.REST; }
    static void start(QuestSupportService service) {
        for (Boss root : roots(service)) { check(service.canRespawn(root), "root eligible"); root.currentLevel = 0; root.bossStatus = BossStatus.ACTIVE; root.nPoint.hp = 1; }
        service.tick();
    }
    public static void main(String[] args) throws Exception {
        Time clock = new Time(); Backend backend = new Backend();
        QuestSupportService service = new QuestSupportService(clock, QuestSupportConfig.DEFAULT, backend);
        service.tick(); check(service.chainCount() == 0 && backend.notices.isEmpty(), "before14 empty");
        clock.at("2026-10-07T07:00:00Z"); service.tick();
        check(service.chainCount() == 12 && service.snapshot().size() == 30 && backend.notices.size() == 1, "two extra chains each, one opening");
        Set<Long> ids = new HashSet<>();
        for (Boss b : service.snapshot()) {
            check(ids.add(b.id), "all network IDs unique");
            check(b.questParent() == null || service.snapshot().contains(b.questParent()), "parents belong to pool");
            for (Boss[] group : b.bossAppearTogether) if (group != null) for (Boss child : group) check(child.questParent() == b, "independent companion links");
        }
        for (int id : QuestSupportService.ROOT_IDS) {
            List<Boss> group = roots(service).stream().filter(b -> b.questBossId() == id).toList();
            check(group.size() == 2 && group.get(0).zoneFinal != group.get(1).zoneFinal, "extra chains spread");
        }
        Boss original = BossManager.gI().createBoss(BossID.ANDROID_14); original.currentLevel = 0;
        var restField = Boss.class.getDeclaredField("lastTimeRest"); restField.setAccessible(true);
        restField.setLong(original, 1000);
        for (Boss child : original.getCompanionBosses()) restField.setLong(child, 2000);
        Boss lateChild = original.getCompanionBosses()[0]; lateChild.bossStatus = BossStatus.ACTIVE;
        check(service.originalRespawnAt(original) == -1, "original and ETA wait for final child");
        lateChild.bossStatus = BossStatus.REST; restField.setLong(lateChild, 5000);
        check(service.originalRespawnAt(original) == 125_000, "120s begins after final child; same deadline for ETA");
        int originalSeconds = original.questBaseRestSeconds();
        clock.at("2026-10-07T10:00:00Z");
        check(service.originalRespawnAt(original) == 1000 + originalSeconds*1000L && original.questBaseRestSeconds() == originalSeconds, "17 restores original timer without mutation before tick");
        clock.at("2026-10-07T07:00:00Z");
        int created = backend.created;
        ExecutorService threads = Executors.newFixedThreadPool(4);
        List<Future<?>> jobs = new ArrayList<>();
        for (int i = 0; i < 100; i++) jobs.add(threads.submit(service::tick));
        for (Future<?> f : jobs) f.get(); threads.shutdown();
        check(backend.created == created && service.chainCount() == 12, "concurrent ticks bounded");
        start(service);
        Boss android = roots(service).stream().filter(b -> b.questBossId() == BossID.ANDROID_14).findFirst().orElseThrow();
        android.bossStatus = BossStatus.REST;
        Boss child = android.bossAppearTogether[0][0]; child.bossStatus = BossStatus.ACTIVE;
        check(!service.canRespawn(android), "dead parent cannot respawn over fighting child");
        finish(service); service.tick();
        clock.now += 119_999; service.tick(); check(!service.canRespawn(android), "119999ms rest blocked");
        clock.now++; service.tick(); start(service);
        // Exercise an entire support window at the server's 1.5s tick cadence and repeat full chains.
        long end = Instant.parse("2026-10-07T09:59:59Z").toEpochMilli();
        long completedAt = clock.now; boolean running = true;
        while (clock.now < end) {
            clock.now = Math.min(end, clock.now + 1500);
            if (running && clock.now-completedAt >= 30_000) { finish(service); running = false; completedAt = clock.now; }
            service.tick();
            if (!running && clock.now-completedAt >= 120_000) { start(service); running = true; completedAt = clock.now; }
            check(service.chainCount() == 12 && backend.created == created && service.snapshot().size() == 30, "3h pool never accumulates");
        }
        finish(service); service.tick(); clock.now += 120_000;
        // Still inside17 for the final combat test.
        clock.at("2026-10-07T09:59:59Z");
        service.shutdown();
        Backend ending = new Backend(); service = new QuestSupportService(clock, QuestSupportConfig.DEFAULT, ending); service.tick(); start(service);
        Boss fighter = roots(service).get(0); fighter.markQuestSupportCombat(QuestSupportProgressTest.player(20,1), 1);
        Boss fighterChild = fighter.bossAppearTogether[0][0]; fighterChild.currentLevel = 0; fighterChild.bossStatus = BossStatus.ACTIVE;
        fighter.bossStatus = BossStatus.REST;
        clock.at("2026-10-07T10:00:00Z");
        check(!service.canRespawn(fighter), "17 boundary blocks before next tick");
        service.tick(); check(service.chainCount() == 1, "only combat chain kept; idle extras recalled");
        check(!fighter.isQuestSupportRetired() && !fighterChild.isQuestSupportRetired(), "whole chain retains child battle");
        clock.at("2026-10-07T10:04:59Z"); service.tick(); check(service.chainCount() == 1, "combat grace below5min");
        clock.at("2026-10-07T10:05:00Z"); service.tick(); check(service.chainCount() == 0 && fighter.isQuestSupportRetired(), "5min hard recall");
        check(ending.notices.size() == 3, "open close recall notifications once");
        check(!fighter.claimQuestSupportCredit(), "late credit after retirement denied");
        clock.at("2026-10-08T08:00:00Z"); service.tick(); check(service.chainCount() == 12, "next day restart mid-window");
        service.shutdown(); service.tick(); check(service.snapshot().isEmpty(), "shutdown pool released and cannot reopen");
        Backend absent = new Backend(); absent.missing = true; QuestSupportService missing = new QuestSupportService(clock, QuestSupportConfig.DEFAULT, absent);
        missing.tick(); check(missing.chainCount() == 0, "missing zones not spawned");
        Backend partial = new Backend(); partial.partial = true; QuestSupportService broken = new QuestSupportService(clock, QuestSupportConfig.DEFAULT, partial);
        int before = BossManager.gI().getBosses().size(); broken.tick();
        check(broken.snapshot().isEmpty() && BossManager.gI().getBosses().size() == before, "partial constructors fully rolled back");
        Backend failing = new Backend(); QuestSupportService cleanup = new QuestSupportService(clock, QuestSupportConfig.DEFAULT, failing); cleanup.tick();
        List<Boss> all = cleanup.snapshot(); failing.failRemove = true; cleanup.shutdown(); cleanup.shutdown();
        check(all.stream().allMatch(b -> b.isQuestSupportRetired() && failing.removed.contains(b)), "all removals retried after injected failure");
        // Exercise the production zone chooser against loaded map fixtures and actual task steps.
        var type = Class.forName("nro.models.services.QuestSupportService$LiveBackend");
        var constructor = type.getDeclaredConstructor(); constructor.setAccessible(true);
        QuestSupportService.Backend live = (QuestSupportService.Backend)constructor.newInstance();
        var map = blank(Terrain.class); map.mapId = 80; map.mapWidth = 240; map.zones = new ArrayList<>();
        for (int i = 0; i < 4; i++) map.zones.add(new Zone(map,i,20));
        map.zones.get(0).addPlayer(QuestSupportProgressTest.player(21,0));
        map.zones.get(1).addPlayer(QuestSupportProgressTest.player(21,1));
        map.zones.get(1).addPlayer(QuestSupportProgressTest.player(21,3));
        map.zones.get(2).addPlayer(QuestSupportProgressTest.player(21,1));
        var maps = nro.models.server.Manager.MAPS; List<nro.models.map.Map> prior = List.copyOf(maps);
        maps.clear(); maps.add(map);
        Boss baseFide = BossManager.gI().createBoss(BossID.FIDE); baseFide.zone = map.zones.get(2);
        QuestSupportIdentity.begin(); Boss extraFide; List<Boss> members;
        try { extraFide = live.create(BossID.FIDE); members = QuestSupportIdentity.constructingMembers(); }
        finally { QuestSupportIdentity.end(); }
        check(live.chooseZone(extraFide,members,Set.of()) == map.zones.get(1), "production chooser prioritizes matching boss steps");
        check(live.chooseZone(extraFide,members,Set.of(map.zones.get(1))) == map.zones.get(0), "reserved and original occupied zones excluded");
        map.mapWidth = 0; check(live.chooseZone(extraFide,members,Set.of()) == null, "missing tile data excluded");
        maps.clear(); maps.addAll(prior);
        joinRetirementRace(false); joinRetirementRace(true);
        for (Boss b : List.copyOf(BossManager.gI().getBosses())) BossManager.gI().removeBoss(b);
        System.out.println("PASS QuestSupport lifecycle: 3h simulation, concurrent ticks, groups, rests, grace, rollback and shutdown");
    }
}
