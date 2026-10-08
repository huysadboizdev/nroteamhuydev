package nro.models.services;
import java.lang.reflect.*;
import java.util.function.IntUnaryOperator;
import nro.models.boss.Boss;
import nro.models.boss.Mabu.FatMabuTournament;
import nro.models.player.Player;
import static nro.models.services.PinkFixture.*;
public final class MabuLootBalanceTest {
 public static void main(String[] args)throws Exception {try{
  HuntingTrioLootTest.setup();HuntingTrioEntryTest.skills();
  var mabu=new FatMabuTournament();Field field=FatMabuTournament.class.getDeclaredField("equipment");field.setAccessible(true);
  Object actual=field.get(mabu);Method drop=SealedBossEquipmentLoot.class.getDeclaredMethod("drop",Boss.class,Player.class,IntUnaryOperator.class);drop.setAccessible(true);
  int shop=0,divine=0,sealedDivine=0;Player owner=player(7);owner.gender=0;
  for(int roll=0;roll<100;roll++){
   ((SealedBossEquipmentLoot)actual).reset();Boss fixture=HuntingTrioLootTest.boss();
   check((boolean)drop.invoke(actual,fixture,owner,HuntingTrioLootTest.standard(roll,0,0)),"valid Mabu equipment drop");
   var item=HuntingTrioLootTest.equipment(fixture);if(item.itemTemplate.id<555)shop++;else if(HuntingTrioLootTest.seals(item)==0)divine++;else sealedDivine++;
  }
  check(shop==90&&divine==8&&sealedDivine==2,"actual Mabu helper must use90/8/2, got "+shop+"/"+divine+"/"+sealedDivine);
  check(FatMabuTournament.class.getField("EGG_RATE").getInt(null)==5&&FatMabuTournament.class.getField("BOTTLE_RATE").getInt(null)==3,"Mabu rare rates5/3");
  System.out.println("PASS actual Mabu equipment90/8/2 and rare5/3");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
