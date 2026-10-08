package nro.models.services;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.IntUnaryOperator;
import nro.models.map.ItemMap;
import nro.models.mob.*;
import nro.models.player.Player;
import static nro.models.services.PinkFixture.*;
public class FarmDeathGenerationTest {
 public static void main(String[] args)throws Exception {
  install();replace(InventoryService.class,"I",new InventoryService());
  Player p=EquipmentFarmDropTest.owner(0,true);Mob m=EquipmentFarmDropTest.mob(p,155);m.point=new MobPoint(m);m.point.maxHp=100;
  Method generation=Mob.class.getMethod("equipmentFarmGeneration"),reserve=Mob.class.getMethod("claimEquipmentFarmDeath",long.class);
  Class<?> claimType=Class.forName("nro.models.services.EquipmentFarmDropService$DeathClaim");
  Method rewards=Class.forName("nro.models.services.EquipmentFarmDropService").getMethod("rewards",Mob.class,Player.class,int.class,int.class,IntUnaryOperator.class,long.class,claimType);
  long oldGeneration=(long)generation.invoke(m);Object oldClaim=reserve.invoke(m,oldGeneration);check(oldClaim!=null,"reserve old death");
  var executor=java.util.concurrent.Executors.newSingleThreadExecutor();java.util.concurrent.Future<?> delayed;
  Object fresh;
  try {
   synchronized(p) {
    delayed=executor.submit(()->{try{return rewards.invoke(null,m,p,50,100,(IntUnaryOperator)(bound->0),System.currentTimeMillis(),oldClaim);}catch(Exception e){throw new RuntimeException(e);}});
    m.hoiSinh();fresh=reserve.invoke(m,(long)generation.invoke(m));check(fresh!=null,"new life gets independent reservation");
    check(reserve.invoke(m,oldGeneration)==null,"stale fatal transition cannot reserve new life");
   }
   check(((List<?>)delayed.get()).size()==1,"old delayed death still awards guaranteed shard");
   check(((List<?>)rewards.invoke(null,m,p,50,100,(IntUnaryOperator)(bound->0),System.currentTimeMillis(),oldClaim)).isEmpty(),"old claim cannot replay after revival");
   check(((List<?>)rewards.invoke(null,m,p,50,100,(IntUnaryOperator)(bound->0),System.currentTimeMillis(),fresh)).size()==1,"old callback cannot consume fresh life reward");
  }finally{executor.shutdownNow();}
  System.out.println("FarmDeathGenerationTest PASS delayed old death, revival, fresh reward and no replay");
 }
}
