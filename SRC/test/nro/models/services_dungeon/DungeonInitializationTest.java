package nro.models.services_dungeon;

import nro.models.clan.Clan;
import nro.models.map.Zone;
import nro.models.map.phoban.*;
import nro.models.map.service.ChangeMapService;
import nro.models.services.PinkFixture;

public final class DungeonInitializationTest {
    public static void main(String[] args) {
        try {
            PinkFixture.install();
            var player = PinkFixture.player(9200);
            player.clan = new Clan();
            player.zone = PinkFixture.zone();
            Zone treasure = PinkFixture.zone(), snake = PinkFixture.zone(), gas = PinkFixture.zone(), ribbon = PinkFixture.zone();
            treasure.map.mapId = 135; snake.map.mapId = 143; gas.map.mapId = 147; ribbon.map.mapId = 53;
            var a = new BanDoKhoBau(0); var b = new SnakeWay(0); var c = new DestronGas(0); var d = new RedRibbonHQ(0);
            a.clan = b.clan = c.clan = player.clan; d.setClan(player.clan);
            player.clan.BanDoKhoBau = a; player.clan.ConDuongRanDoc = b; player.clan.KhiGasHuyDiet = c; player.clan.doanhTrai = d;
            a.zones.add(treasure); b.zones.add(snake); c.zones.add(gas); d.zones.add(ribbon);
            a.isOpened = b.isOpened = c.isOpened = d.isOpened = true;
            a.isInitializing = b.isInitializing = c.isInitializing = d.isInitializing = true;
            for (Zone destination : new Zone[]{treasure, snake, gas, ribbon})
                PinkFixture.check(!ChangeMapService.gI().canCommitDungeonEntry(player, destination), "partially initialized dungeon rejects admission " + destination.map.mapId);
            a.update(); b.update(); c.update(); d.update();
            PinkFixture.check(a.isOpened && b.isOpened && c.isOpened && d.isOpened, "initialization tick cannot close reserved instances");
            a.isInitializing = b.isInitializing = c.isInitializing = d.isInitializing = false;
            for (Zone destination : new Zone[]{treasure, snake, gas, ribbon})
                PinkFixture.check(ChangeMapService.gI().canCommitDungeonEntry(player, destination), "complete dungeon accepts owned admission " + destination.map.mapId);
            // Actual production open methods fail on a malformed fixture map and release reservations.
            a.zones.clear(); b.zones.clear(); c.zones.clear(); d.zones.clear();
            a.zones.add(null); b.zones.add(null); c.zones.add(null); d.zones.add(null);
            a.isInitializing = b.isInitializing = c.isInitializing = d.isInitializing = true;
            Clan originalOwner = player.clan;
            player.clan = new Clan();
            player.clan.lastTimeOpenBanDoKhoBau = player.clan.lastTimeOpenConDuongRanDoc = player.clan.lastTimeOpenKhiGasHuyDiet = player.clan.lastTimeOpenDoanhTrai = 4242;
            a.openBanDoKhoBau(player, originalOwner, (byte) 1);
            b.openConDuongRanDoc(player, originalOwner, (byte) 1);
            c.openKhiGasHuyDiet(player, originalOwner, (byte) 1);
            d.openDoanhTrai(player);
            PinkFixture.check(!a.isOpened && !b.isOpened && !c.isOpened && !d.isOpened, "failed initialization returns instance pool capacity");
            PinkFixture.check(!a.isInitializing && !b.isInitializing && !c.isInitializing && !d.isInitializing, "failed initialization clears reservation marker");
            PinkFixture.check(originalOwner.BanDoKhoBau == null && originalOwner.ConDuongRanDoc == null && originalOwner.KhiGasHuyDiet == null && originalOwner.doanhTrai == null, "failed initialization clears captured owner clan pointers");
            PinkFixture.check(player.clan.lastTimeOpenBanDoKhoBau == 4242 && player.clan.lastTimeOpenConDuongRanDoc == 4242 && player.clan.lastTimeOpenKhiGasHuyDiet == 4242 && player.clan.lastTimeOpenDoanhTrai == 4242, "failed reservation must never reset replacement clan timestamps");
            System.out.println("DUNGEON_INITIALIZATION PASS admission, update isolation, captured clan rollback for 4 dungeon types");
            System.exit(0);
        } catch (Throwable e) { e.printStackTrace(); System.exit(1); }
    }
}
