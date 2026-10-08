package nro.models.services;

import java.lang.reflect.*;
import java.util.function.IntUnaryOperator;
import nro.models.boss.Boss;
import nro.models.player.Player;
import static nro.models.services.PinkFixture.*;

public final class SealedBossEquipmentLootTest {
    public static void main(String[] args)throws Exception {try{
        HuntingTrioLootTest.setup();Class<?> type;
        try{type=Class.forName("nro.models.services.SealedBossEquipmentLoot");}catch(ClassNotFoundException e){throw new AssertionError("shared equipment-only loot missing",e);}
        Method m=type.getDeclaredMethod("drop",Boss.class,Player.class,IntUnaryOperator.class);m.setAccessible(true);
        for(int gender=0;gender<3;gender++)for(int category:new int[]{0,59,60,89,90,99}) {
            Boss b=HuntingTrioLootTest.boss();Player p=player(7);p.gender=(byte)gender;Object h=type.getConstructor().newInstance();
            IntUnaryOperator rolls=category<60||category>=90?HuntingTrioLootTest.rolls(category,0,2):HuntingTrioLootTest.rolls(category,0);
            check((boolean)m.invoke(h,b,p,rolls),"valid equipment drop");check(b.zone.items.size()==1,"no hunting extras for Mabu");
            var i=HuntingTrioLootTest.equipment(b);check((i.itemTemplate.id<555)==(category<60),"same pools");
            check(HuntingTrioLootTest.seals(i)==(category>=60&&category<90?0:1),"same 60/30/10 seals");
            check(kept(i)&&i.playerId==7,"private equipment");check(!(boolean)m.invoke(h,b,p,HuntingTrioLootTest.rolls()),"one equipment per life");
        }
        System.out.println("PASS SealedBossEquipmentLoot");System.exit(0);
    }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
