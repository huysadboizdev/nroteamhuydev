package nro.models.map;

import static nro.models.services.PinkFixture.*;

public final class ItemMapWireIdTest {
    public static void wrapsWithinSignedShortRange() throws Exception {
        install();
        Zone zone = zone();
        zone.countItemAppeaerd = Short.MAX_VALUE;

        ItemMap lastWireId = new ItemMap(zone, 77, 1, 50, 150, 7);
        ItemMap firstWireId = new ItemMap(zone, 77, 1, 60, 150, 7);

        check(lastWireId.itemMapId == Short.MAX_VALUE, "last positive signed-short ID remains usable");
        check(firstWireId.itemMapId == 0, "allocator wraps to zero instead of publishing 32768");
    }

    public static void skipsIdsHeldByLiveItems() throws Exception {
        install();
        Zone zone = zone();
        ItemMap first = new ItemMap(zone, 77, 1, 50, 150, 7);
        zone.countItemAppeaerd = 0;

        ItemMap second = new ItemMap(zone, 77, 1, 60, 150, 7);

        check(first.itemMapId == 0, "first item receives ID zero");
        check(second.itemMapId == 1, "allocator skips an ID still assigned to a live item");
    }

    public static void removalMakesIdReusable() throws Exception {
        install();
        Zone zone = zone();
        ItemMap removed = new ItemMap(zone, 77, 1, 50, 150, 7);
        zone.countItemAppeaerd = removed.itemMapId;
        zone.removeItemMap(removed);

        ItemMap replacement = new ItemMap(zone, 77, 1, 60, 150, 7);

        check(replacement.itemMapId == removed.itemMapId, "removed item ID can be reused");
    }

    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "wrap" -> wrapsWithinSignedShortRange();
            case "collision" -> skipsIdsHeldByLiveItems();
            case "reuse" -> removalMakesIdReusable();
            default -> throw new AssertionError(args[0]);
        }
        System.out.println("PASS ItemMapWireId " + args[0]);
        System.exit(0);
    }
}
