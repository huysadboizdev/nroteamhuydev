package nro.models.services;
import nro.models.boss.*;
import nro.models.boss.MajinBuu_12h.Drabura;
import nro.models.player.*;
import static nro.models.services.PinkFixture.*;
public class DraburaLootRevivalTest {
 public static void main(String[] args){try{
  HuntingTrioLootTest.setup();
  replace(Service.class,"instance",new Service(){@Override public void hsChar(Player p,int hp,int mp){p.nPoint.hp=hp;}});
  Drabura b=blank(Drabura.class);Boss base=HuntingTrioLootTest.boss();b.zone=base.zone;b.location=base.location;b.id=BossID.DRABURA;
  b.nPoint=new NPoint(b);b.nPoint.hpMax=100;b.nPoint.mpMax=100;b.nPoint.hp=0;
  Player p=player(123);BossAdminDropService.reward(b,p,()->{},null,n->0);check(b.zone.items.size()==3,"first defeat loot");
  b.afk();check(b.nPoint.hp>0,"real revival");
  BossAdminDropService.reward(b,p,()->{},null,n->0);check(b.zone.items.size()==6,"revived Drabura can drop on second defeat");
  System.out.println("DRABURA_LOOT_REVIVAL PASS");System.exit(0);
 }catch(Throwable failure){failure.printStackTrace();System.exit(1);}}
}
