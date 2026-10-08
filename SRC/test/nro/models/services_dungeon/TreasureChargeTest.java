package nro.models.services_dungeon;

import java.util.List;
import nro.models.clan.Clan;
import nro.models.map.phoban.BanDoKhoBau;
import nro.models.player.Player;
import nro.models.services.*;

public final class TreasureChargeTest {
    static final class Instance extends BanDoKhoBau {
        int openings;
        Instance(){super(0);}
        @Override public boolean openBanDoKhoBau(Player p,Clan owner,byte level,java.util.function.BooleanSupplier charge){
            openings++;PinkFixture.check(charge.getAsBoolean(),"payment commits before admission");isOpened=true;isInitializing=false;return true;
        }
    }
    public static void main(String[] args){
        try {
            PinkFixture.install();
            PinkFixture.replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}});
            var p=PinkFixture.player(9700);p.clan=new Clan();
            var map=PinkFixture.item(611,1);p.inventory.itemsBag.add(map);
            var broken=new BanDoKhoBau(0);broken.zones.add(null);
            var service=TreasureUnderSeaService.gI();service.banDoKhoBaus=List.of(broken);
            service.openBanDoKhoBau(p,(byte)1);
            PinkFixture.check(map.quantity==1 && p.inventory.itemsBag.contains(map),"failed initialization must retain treasure map611");
            var normal=new Instance();service.banDoKhoBaus=List.of(normal);
            LiveSyncCommandGate.mark(p.id,0);
            try {service.openBanDoKhoBau(p,(byte)1);}
            finally {LiveSyncCommandGate.clear(p.id,0);}
            PinkFixture.check(normal.openings==0 && p.clan.BanDoKhoBau==null && map.quantity==1,"pending mutation must refuse reservation and free entry");
            service.openBanDoKhoBau(p,(byte)1);
            PinkFixture.check(normal.openings==1 && map.quantity==0,"successful initialized attempt consumes exactly one map");
            service.openBanDoKhoBau(p,(byte)1);
            PinkFixture.check(normal.openings==1 && map.quantity==0,"duplicate request cannot charge or initialize twice");
            p.clan=new Clan();var unpaid=PinkFixture.item(611,1);p.inventory.itemsBag.add(unpaid);
            var noPayment=new BanDoKhoBau(0);service.banDoKhoBaus=List.of(noPayment);
            PinkFixture.replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){} @Override public void subQuantityItemsBag(Player p,nro.models.item.Item item,int count){}});
            service.openBanDoKhoBau(p,(byte)1);
            PinkFixture.check(!noPayment.isOpened && !noPayment.isInitializing && p.clan.BanDoKhoBau==null && unpaid.quantity==1,"silent failed charge cannot publish free admission and releases reservation");
            System.out.println("TREASURE_CHARGE PASS failed init preserves map, pending refuses entry, successful charge exactly once, silent no-op rolls back");System.exit(0);
        }catch(Throwable e){e.printStackTrace();System.exit(1);}
    }
}
