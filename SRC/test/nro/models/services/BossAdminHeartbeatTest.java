package nro.models.services;
import java.util.*;
import nro.models.boss.Boss;
import nro.models.consts.BossStatus;
import static nro.models.services.PinkFixture.*;
public class BossAdminHeartbeatTest {
 public static void main(String[] args)throws Exception {
  install();Boss alive=blank(Boss.class);alive.id=-28;alive.currentLevel=0;alive.bossStatus=BossStatus.ACTIVE;
  alive.nPoint=new nro.models.player.NPoint(alive);alive.nPoint.hp=50;alive.zone=blank(nro.models.map.Zone.class);
  Boss dead=blank(Boss.class);dead.id=-28;dead.nPoint=new nro.models.player.NPoint(dead);dead.nPoint.hp=0;dead.zone=alive.zone;
  var rows=BossAdminHeartbeat.collect(Arrays.asList(alive,alive,dead));
  check(rows.size()==1&&rows.get(0).aliveCount()==1,"Alive count excludes duplicates/dead");
  check(rows.get(0).key().equals("id:-28")&&rows.get(0).phase()==0,"Stable type grouping");
  alive.zone=null;check(BossAdminHeartbeat.collect(List.of(alive)).isEmpty(),"Detached not displayed alive");
  System.out.println("BOSS_ADMIN_HEARTBEAT PASS duplicate,dead,detached,grouping");
 }
}
