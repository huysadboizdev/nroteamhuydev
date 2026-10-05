package nro.models.services_dungeon;

import nro.models.clan.Clan;
import nro.models.map.Zone;
import nro.models.map.phoban.BanDoKhoBau;
import nro.models.map.service.ChangeMapService;
import nro.models.services.PinkFixture;

public final class DungeonAdmissionTest {
    public static void main(String[] args) throws Exception {
        var player=PinkFixture.player(73);
        Zone old=PinkFixture.zone(), target=PinkFixture.zone();
        target.map.mapId=135;
        player.zone=old;player.clan=new Clan();
        BanDoKhoBau dungeon=new BanDoKhoBau(0);
        dungeon.zones.add(target);dungeon.isOpened=true;
        player.clan.BanDoKhoBau=dungeon;
        var movement=ChangeMapService.gI();
        PinkFixture.check(movement.canCommitDungeonEntry(player,target),"open owned instance accepts admission");
        target.dungeonClosing=true;
        PinkFixture.check(!movement.goToMap(player,target) && player.zone==old,
                "late admission after closing preserves old zone");
        target.dungeonClosing=false;dungeon.isOpened=false;
        PinkFixture.check(!movement.canCommitDungeonEntry(player,target),"disposed instance refuses captured destination");
        player.clan.BanDoKhoBau=null;
        PinkFixture.check(!movement.goToMap(player,target) && player.zone==old,
                "cleared clan pointer cannot admit into retired zone");
        var replacement=new BanDoKhoBau(1);replacement.isOpened=true;
        replacement.zones.add(PinkFixture.zone());player.clan.BanDoKhoBau=replacement;
        PinkFixture.check(!movement.canCommitDungeonEntry(player,target),"new clan instance cannot admit into old captured zone");
        PinkFixture.check(movement.canCommitDungeonEntry(player,old),"ordinary return map remains available");
        System.out.println("DUNGEON_ADMISSION PASS 6 cases");
    }
}
