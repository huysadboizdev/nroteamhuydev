package nro.models.map;

import java.util.ArrayList;
import java.util.List;
import nro.models.player.Player;
import nro.models.services.PinkFixture;
import nro.models.services.Service;
import nro.models.services_func.UseItem;

/** Regression coverage for rejected capsule destinations. */
public final class CapsuleMapSelectionFailureTest {
    private static final String REJECTION = "Hiện tại không thể vào được khu!";

    private static void rejects(int mapId, boolean full, String scenario) throws Exception {
        Player player = PinkFixture.player(1);
        player.idNRNM = -1;
        player.mapCapsule = new ArrayList<>();
        Zone startingZone = player.zone;
        Zone destination = PinkFixture.zone();
        destination.map.mapId = mapId;
        if (full) {
            for (int i = 0; i < 26; i++) {
                destination.players.add(PinkFixture.player(100 + i));
            }
        }
        player.mapCapsule.add(destination);

        List<String> notices = new ArrayList<>();
        int[] hidden = {0};
        PinkFixture.replace(Service.class, "instance", new Service() {
            @Override public void sendThongBao(Player p, String text) { notices.add(text); }
            @Override public void hideWaitDialog(Player p) { hidden[0]++; }
        });

        UseItem.gI().choseMapCapsule(player, 0);

        PinkFixture.check(notices.equals(List.of(REJECTION)), scenario + " sends the existing rejection notice once");
        PinkFixture.check(hidden[0] == 1, scenario + " hides the wait dialog exactly once");
        PinkFixture.check(player.zone == startingZone, scenario + " does not move the player");
        PinkFixture.check(player.mapBeforeCapsule == null && !player.changeMapVIP,
                scenario + " does not commit successful capsule travel state");
    }

    public static void main(String[] args) throws Exception {
        PinkFixture.install();
        rejects(5, true, "zone over 25 players");
        rejects(53, false, "Doanh Trai zone");
        rejects(114, false, "Ma Bu zone");
        rejects(169, false, "Huy Diet zone");
        System.out.println("PASS CapsuleMapSelectionFailureTest");
        System.exit(0);
    }
}
