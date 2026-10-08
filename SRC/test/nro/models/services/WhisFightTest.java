package nro.models.services;
import nro.models.boss.luyen_tap_tu_dong.Whis;
import nro.models.player.*;
import java.lang.reflect.Field;
public final class WhisFightTest {
 public static void main(String[]args)throws Exception {
  Whis boss=PinkFixture.blank(Whis.class);Player owner=PinkFixture.player(1),outsider=PinkFixture.player(2);
  boss.bossStatus=nro.models.consts.BossStatus.RESPAWN;
  boss.update();PinkFixture.check(boss.bossStatus==nro.models.consts.BossStatus.RESPAWN,"Published boss must not update before constructor is ready");
  boss.playerAtt=owner;boss.nPoint=new NPoint(boss);boss.nPoint.hpMax=10000;boss.nPoint.hp=10000;
  Field level=Whis.class.getDeclaredField("level");level.setAccessible(true);level.setLong(boss,1);
  PinkFixture.check(boss.injured(outsider,1000,true,false)==0&&boss.nPoint.hp==10000,"Outsider cannot damage Whis");
  PinkFixture.check(boss.injured(null,1000,true,false)==0&&boss.nPoint.hp==10000,"Null attacker cannot damage Whis");
  PinkFixture.check(boss.injured(owner,1000,true,true)==0&&boss.nPoint.hp==10000,"Mob attack cannot win Whis");
  PinkFixture.check(boss.injured(owner,1000,true,false)==1000&&boss.nPoint.hp==9000,"Owner damage remains valid");
  boss.die(outsider);PinkFixture.check(owner.thachdauwhis==0&&outsider.thachdauwhis==0,"Outsider die call awards no points");
  System.out.println("WHIS_OWNER_DAMAGE PASS");
 }
}
