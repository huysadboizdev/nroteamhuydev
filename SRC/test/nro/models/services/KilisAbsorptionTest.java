package nro.models.services;

import java.util.function.IntUnaryOperator;
import nro.models.item.Item;
import nro.models.item.ItemTime;
import nro.models.mob.Mob;
import nro.models.player.Pet;
import nro.models.player.Player;
import static nro.models.services.PinkFixture.*;

public class KilisAbsorptionTest {
    public static void main(String[] args) throws Exception {
        try { run(args); } catch (Throwable failure) { failure.printStackTrace(); System.exit(1); }
        System.exit(0);
    }
    private static void run(String[] args) throws Exception {
        install();
        if (args.length > 0 && args[0].equals("map")) {
            nro.models.map.Map map = new nro.models.map.Map(165, "fixture", (byte)0,
                    (byte)0, (byte)0, (byte)0, (byte)9, new int[][]{{0}}, new int[]{0},
                    10, 15, java.util.List.of());
            check(map.type == 0, "Kilis map must stay normal even with legacy event type9");
            check(map.zones.size() == 10, "Kilis map must keep its own zones");
            var event = blank(nro.models.services_dungeon.MajinBuu14HService.class);
            event.maBu2Hs = new java.util.ArrayList<>();
            event.addMapMaBu2H(0, map.zones.get(0));
            check(event.maBu2Hs.isEmpty(), "Absorption zone must not register with event controller");
            System.out.println("Kilis map isolation PASS"); return;
        }
        if (args.length > 0 && args[0].equals("duration")) {
            Player p = player(1); p.itemTime = new ItemTime(p);
            p.itemTime.isUseKilis = true;
            p.itemTime.lastTimeUseKilis = System.currentTimeMillis() - 660000;
            p.itemTime.timeLengthKilis = 600000;
            p.itemTime.update();
            check(!p.itemTime.isUseKilis, "Ten-minute Kilis buff must expire after ten minutes");
            p.itemTime.isUseKilis = true;
            p.itemTime.lastTimeUseKilis = System.currentTimeMillis() - 660000;
            p.itemTime.timeLengthKilis = 1200000;
            p.itemTime.update();
            check(p.itemTime.isUseKilis, "Purchased extension must remain active");
            System.out.println("Kilis duration PASS"); return;
        }
        var absorb = Class.forName("nro.models.services.KilisAbsorptionService").getMethod(
                "absorb", Mob.class, Player.class, long.class, IntUnaryOperator.class);
        Player p = player(1); p.zone = zone(); p.zone.map.mapId = 165;
        Item jar = item(1795, 1); p.inventory.itemsBag.add(jar);
        Mob mob = new Mob(); mob.zone = p.zone;
        IntUnaryOperator win = bound -> 0, lose = bound -> bound - 1;
        absorb.invoke(null, mob, p, 1L, win);
        check(jar.getOptionById(250).param == 1, "Direct hit must fill jar and initialize missing option");
        Pet pet = blank(Pet.class); pet.master = p; pet.isPet = true; pet.zone = p.zone;
        absorb.invoke(null, mob, pet, 1L, win);
        check(jar.getOptionById(250).param == 2, "Pet hit must fill owner's jar");
        absorb.invoke(null, mob, p, 0L, win);
        absorb.invoke(null, mob, p, -1L, win);
        absorb.invoke(null, mob, p, 1L, lose);
        p.zone.map.mapId = 166;
        absorb.invoke(null, mob, p, 1L, win);
        check(jar.getOptionById(250).param == 2, "No credit for zero damage, failed roll or unrelated maps");
        p.zone.map.mapId = 165; p.zone = zone();
        absorb.invoke(null, mob, pet, 1L, win);
        check(jar.getOptionById(250).param == 2, "Owner outside zone cannot receive pet credit");
        p.zone = mob.zone;
        p.itemTime = new ItemTime(p); p.itemTime.isUseKilis = true;
        p.itemTime.lastTimeUseKilis = System.currentTimeMillis(); p.itemTime.timeLengthKilis = 600000;
        absorb.invoke(null, mob, p, 1L, (IntUnaryOperator)(bound -> 45));
        check(jar.getOptionById(250).param == 3, "Active buff uses boosted rate");
        p.itemTime.lastTimeUseKilis -= 660000;
        absorb.invoke(null, mob, p, 1L, (IntUnaryOperator)(bound -> 45));
        check(jar.getOptionById(250).param == 3, "Expired buff must not boost a hit before timer tick");
        Mob dead = new Mob(); dead.zone = mob.zone; dead.point.hp = 0;
        for (int n = 0; n < 5000; n++) dead.injured(p, 1, true);
        check(jar.getOptionById(250).param == 3, "Dead target packets cannot grant Killis");
        jar.getOptionById(250).param = Integer.MAX_VALUE;
        absorb.invoke(null, mob, p, 1L, win);
        check(jar.getOptionById(250).param == Integer.MAX_VALUE, "Jar cannot overflow");
        check(!nro.models.map.service.MapService.gI().isMapCadic(166), "Only map165 is Cadic absorption map");
        System.out.println("Kilis absorption PASS");
    }
}
