package nro.models.services;
import java.util.*;
import nro.models.boss.*;
import nro.models.player.*;
import static nro.models.services.PinkFixture.*;
public final class BossAdminAdapterTest {
 public static void main(String[] args)throws Exception {
  install();LiveSyncBossArithmeticTest.fixtures();
  var owner=player(102);owner.zone=zone();owner.zone.map.mapId=154;owner.traning=new Traning();owner.traning.setTop(9);
  var boss=blank(LiveSyncBossArithmeticTest.WhisProbe.class);boss.id=BossID.WHIS;boss.currentLevel=0;boss.playerAtt=owner;boss.nPoint=new NPoint(boss);boss.nPoint.hpMax=boss.nPoint.hpg=550000;boss.nPoint.dame=boss.nPoint.dameg=100;boss.location=new Location();boss.lastTimePickNRNM=System.currentTimeMillis();
  var row=BossAdminConfigTest.row(1,"next",2000);row.put("boss_key","id:"+BossID.WHIS);row.put("damage",77);BossAdminConfig.stage(BossAdminConfigTest.db(1,List.of(row))).run();
  BossAdminConfig.applyAtSpawn(boss);boss.joinMap();check(boss.nPoint.hpMax==2000&&boss.nPoint.hp==2000&&boss.nPoint.dame==77,"native Whis level scaling must not overwrite selected absolute stats");
  row.put("revision",2L);row.put("damage",88);BossAdminConfig.stage(BossAdminConfigTest.db(2,List.of(row))).run();check(BossAdminConfig.nativeDamage(boss,999)==77,"next revision native formula keeps per-life configuration");
  BossAdminConfig.applyAtSpawn(boss);boss.joinMap();check(boss.nPoint.dame==88,"next life receives changed config after native scaling");
  System.out.println("Boss native reset adapter and per-life next damage PASS");
 }
}
