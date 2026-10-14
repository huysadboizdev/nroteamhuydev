package nro.models.services;

import java.lang.reflect.Method;
import nro.models.player.Player;
import nro.models.player.PetFusionBonusTest;
import nro.models.player.BuuUpgradeBonusTest;
import nro.models.network.Message;

public final class EquipmentAuraPacketTest {
    static byte[] data;
    public static void main(String[] args)throws Exception {
        Method owner=PetFusionBonusTest.class.getDeclaredMethod("owner",int.class);owner.setAccessible(true);
        Player p=(Player)owner.invoke(null,1);
        Method set=BuuUpgradeBonusTest.class.getDeclaredMethod("set",Player.class,int.class);set.setAccessible(true);
        Service s=new Service(){@Override public void sendMessAllPlayerInMap(Player p,Message m){data=m.getData();}};
        for(int grade=5;grade<=8;grade++) {
            set.invoke(null,p,grade);s.Send_Caitrang(p);
            PinkFixture.check(data!=null && data.length==13 && data[12]==grade,"Appearance refresh must include upgrade effect grade="+grade);
        }
        p.inventory.itemsBody.set(0,new nro.models.item.Item());s.Send_Caitrang(p);
        PinkFixture.check(data[12]==-1,"Unequip clears effect");
        System.out.println("PASS equipment aura packet");
    }
}
