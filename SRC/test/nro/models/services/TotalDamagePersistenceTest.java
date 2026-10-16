package nro.models.services;

import nro.models.player.Player;

public final class TotalDamagePersistenceTest {
    public static void main(String[] args) {
        Player player = new Player();
        Player other = new Player();
        check(Service.shouldPersistTotalDamage(player, 1000L), "first damage update is persisted");
        check(!Service.shouldPersistTotalDamage(player, 1001L), "same burst skips a second SQL write");
        check(!Service.shouldPersistTotalDamage(player, 1999L), "write interval is enforced");
        check(Service.shouldPersistTotalDamage(player, 2000L), "latest aggregate is persisted after one second");
        check(Service.shouldPersistTotalDamage(other, 2000L), "players have independent write intervals");
        System.out.println("Total damage persistence throttle PASS");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
