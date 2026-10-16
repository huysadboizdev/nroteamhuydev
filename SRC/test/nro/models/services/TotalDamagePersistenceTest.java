package nro.models.services;

import nro.models.player.Player;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;

public final class TotalDamagePersistenceTest {
    public static void main(String[] args) {
        Player player = new Player();
        Player other = new Player();
        check(Service.shouldPersistTotalDamage(player, 1000L), "first damage update is persisted");
        check(!Service.shouldPersistTotalDamage(player, 1001L), "same burst skips a second SQL write");
        check(!Service.shouldPersistTotalDamage(player, 1999L), "write interval is enforced");
        check(Service.shouldPersistTotalDamage(player, 2000L), "latest aggregate is persisted after one second");
        check(Service.shouldPersistTotalDamage(other, 2000L), "players have independent write intervals");
        Executor previous = Service.totalDamagePersistenceExecutor;
        BiConsumer<Long, Long> previousWriter = Service.totalDamagePersistenceWriter;
        List<Runnable> queued = new ArrayList<>();
        Map<Long, Long> persisted = new HashMap<>();
        Service.totalDamagePersistenceExecutor = queued::add;
        Service.totalDamagePersistenceWriter = persisted::put;
        try {
            Player combatPlayer = new Player();
            combatPlayer.id = 91L;
            combatPlayer.total_damage_maydam = 1234L;
            Service.gI().updatePlayerTotalDamage(combatPlayer);
            check(queued.size() == 1, "SQL persistence must be queued away from the combat caller");
            check(combatPlayer.lastTotalDamagePersistAt > 0L, "combat path still applies the per-player persistence throttle");
            Service.gI().updatePlayerTotalDamage(combatPlayer);
            check(queued.size() == 1, "a burst must not queue duplicate SQL writes");
            combatPlayer.total_damage_maydam = 5678L;
            combatPlayer.lastTotalDamagePersistAt = System.currentTimeMillis() - 1001L;
            Service.gI().updatePlayerTotalDamage(combatPlayer);
            check(queued.size() == 1,
                    "a newer snapshot for the same player must replace a queued stale write");
            queued.remove(0).run();
            check(persisted.size() == 1 && Long.valueOf(5678L).equals(persisted.get(91L)),
                    "the drain must persist only the latest snapshot for the player");
        } finally {
            Service.totalDamagePersistenceExecutor = previous;
            Service.totalDamagePersistenceWriter = previousWriter;
        }
        System.out.println("Total damage persistence throttle PASS");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
