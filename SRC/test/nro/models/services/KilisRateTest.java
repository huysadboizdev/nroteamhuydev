package nro.models.services;

import java.util.function.IntUnaryOperator;
import nro.models.item.ItemTime;
import nro.models.mob.Mob;
import nro.models.player.Pet;
import static nro.models.services.PinkFixture.*;

public final class KilisRateTest {
    public static void main(String[] args) {
        try { run(); System.out.println("Kilis rates 30/60 PASS"); System.exit(0); }
        catch (Throwable failure) { failure.printStackTrace(); System.exit(1); }
    }
    private static void run() throws Exception {
        install();
        var absorb = Class.forName("nro.models.services.KilisAbsorptionService").getMethod(
                "absorb", Mob.class, nro.models.player.Player.class, long.class, IntUnaryOperator.class);
        var owner = player(1); owner.zone = zone(); owner.zone.map.mapId = 165;
        var jar = item(1795, 1); jar.itemOptions.add(new nro.models.item.Item.ItemOption(250, 0));
        owner.inventory.itemsBag.add(jar);
        Mob mob = new Mob(); mob.zone = owner.zone;
        owner.itemTime = new ItemTime(owner);
        Pet pet = blank(Pet.class); pet.isPet = true; pet.master = owner; pet.zone = owner.zone;
        for (String mode : new String[]{"normal", "buff", "expired", "pet"}) {
            jar.getOptionById(250).param = 0;
            owner.itemTime.isUseKilis = mode.equals("buff") || mode.equals("expired");
            owner.itemTime.timeLengthKilis = 600000;
            owner.itemTime.lastTimeUseKilis = System.currentTimeMillis() - (mode.equals("expired") ? 660000 : 0);
            for (int value = 0; value < 100; value++) {
                final int rollValue = value;
                IntUnaryOperator roll = bound -> {
                    check(bound == 100, "Rates must use 100 equally likely outcomes");
                    return rollValue;
                };
                absorb.invoke(null, mob, mode.equals("pet") ? pet : owner, 1L, roll);
            }
            check(jar.getOptionById(250).param == (mode.equals("buff") ? 60 : 30),
                    mode + " must match approved rate exactly");
        }
    }
}
