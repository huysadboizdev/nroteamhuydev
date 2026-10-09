package nro.models.services;
import nro.models.boss.*;
import nro.models.boss.PrisonBill.*;
import nro.models.consts.BossStatus;
import static nro.models.services.PinkFixture.*;
public class PrisonBillPairTest {
 static class Leader extends PrisonBillBoss {
  Leader()throws Exception{super(BossID.KAMIN);}
  @Override protected void initSkill(){} // SQL skill templates are outside this lifecycle fixture.
 }
 static class Child extends PrisonBillBoss {
  int exits;
  Child()throws Exception{super(BossID.OREN);}
  void leader(Boss leader){parentBoss=leader;}
  @Override public void leaveMap(){exits++;zone=null;changeStatus(BossStatus.REST);}
 }
 public static void main(String[] args){try{
  install();PrisonBillBoss k=new Leader();Child o=new Child();k.bossAppearTogether[0]=new Boss[]{o};o.leader(k);k.currentLevel=0;
  o.zone=zone();o.nPoint.hp=100;o.bossStatus=BossStatus.ACTIVE;k.bossStatus=BossStatus.REST;
  k.rest();check(k.bossStatus==BossStatus.REST,"leader waits while child remains on map");
  o.autoLeaveMap();check(o.zone!=null&&o.bossStatus==BossStatus.ACTIVE,"child does not independently leave or respawn");
  k.nPoint.hp=0;k.wakeupAnotherBossWhenDisappear();check(o.exits==0&&o.zone!=null,"dead leader lets Oren finish fighting");
  k.nPoint.hp=100;k.wakeupAnotherBossWhenDisappear();check(o.exits==1&&o.zone==null,"healthy idle departure removes pair");
  o.bossStatus=BossStatus.JOIN_MAP;k.zone=null;o.joinMap();check(o.bossStatus==BossStatus.REST&&o.zone==null,"orphan cannot spawn without leader zone");
  k.zone=zone();k.nPoint.hp=100;o.bossStatus=BossStatus.REST;
  k.wakeupAnotherBossWhenAppear();check(o.bossStatus==BossStatus.RESPAWN,"leader wakes its companion for coordinated spawn");
  o.zone=null;o.bossStatus=BossStatus.REST;
  var restTime=nro.models.boss.Boss.class.getDeclaredField("lastTimeRest");restTime.setAccessible(true);
  long past=System.currentTimeMillis()-301000;restTime.setLong(k,past);restTime.setLong(o,past);
  k.bossStatus=BossStatus.REST;k.rest();check(k.bossStatus==BossStatus.RESPAWN,"pair begins next turn after cooldown");
  k.respawn();check(k.nPoint.hp==1000000000,"next turn restores leader full HP");
  k.zone=zone();k.wakeupAnotherBossWhenAppear();check(o.bossStatus==BossStatus.RESPAWN,"next turn wakes Oren again");
  System.out.println("PRISON_BILL_PAIR PASS coordinated/leader-wait/child-no-idle-exit/no-orphan");System.exit(0);
 }catch(Throwable failure){failure.printStackTrace();System.exit(1);}}
}
