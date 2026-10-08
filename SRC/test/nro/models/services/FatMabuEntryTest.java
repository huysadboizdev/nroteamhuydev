package nro.models.services;
import nro.models.boss.Mabu.*;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.*;
import nro.models.player.*;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.skill.Skill;
import static nro.models.services.PinkFixture.*;
public final class FatMabuEntryTest {
 public static void main(String[] args)throws Exception {try{
  install();HuntingTrioEntryTest.skills();Manager.INTRINSICS.add(new nro.models.intrinsic.Intrinsic());
  replace(nro.models.services_dungeon.NgocRongNamecService.class,"instance",new nro.models.services_dungeon.NgocRongNamecService(){@Override public void dropNamekBall(Player player){}});
  Template.SkillTemplate shield=new Template.SkillTemplate();shield.id=Skill.KHIEN_NANG_LUONG;shield.type=3;shield.name="shield";
  for(int level=1;level<=7;level++){Skill s=new Skill();s.template=shield;s.point=level;s.skillId=(short)(120+level);shield.skillss.add(s);}
  Manager.NCLASS.get(0).skillTemplatess.add(shield);
  var zone=zone();zone.map.mapId=52;FatMabuTournament b=new FatMabuTournament();
  b.prepare(zone);check(b.zone==zone&&zone.getBosses().contains(b)&&zone.getHumanoids().contains(b),"actual map admission");
  check(b.nPoint.hp==300_000_000&&b.playerSkill.skills.size()==5&&b.typePk==ConstPlayer.NON_PK,"initial HP/skills/protection");
  Player owner=player(91);owner.zone=zone;owner.inventory.itemsBag.add(item(568,1));
  check(b.injured(owner,1_000_000,true,false)==0,"conversation protected");b.activate();
  b.playerSkill.skillSelect=b.playerSkill.skills.get(4);b.nPoint.mp=b.nPoint.mpg;
  check(SkillService.gI().useSkill(b,null,null,-1,null)&&b.effectSkill.isShielding&&b.effectSkill.timeShield==45_000,"actual skill casts45s shield");
  check(!SkillService.gI().canUseSkillWithCooldown(b),"shield cannot recast immediately");
  check(b.injured(owner,1_000_000,false,false)==1,"actual shield blocks");
  check(b.injured(owner,300_000_001L,false,false)==1&&!b.effectSkill.isShielding,"oversized attack breaks actual shield");
  b.effectSkill.isShielding=true;b.effectSkill.timeShield=45_000;b.effectSkill.lastTimeShieldUp=System.currentTimeMillis()-45_001;
  b.effectSkill.update();check(!b.effectSkill.isShielding,"actual timed shield expires");
  b.injured(owner,Long.MAX_VALUE,true,false);check(b.isDie()&&b.bossStatus==BossStatus.DIE,"uncapped fatal hit closes combat");
  long gear=zone.items.stream().filter(i->i.itemTemplate.id>=230&&i.itemTemplate.id<=281||i.itemTemplate.id>=555&&i.itemTemplate.id<=567).count();
  check(gear==1,"exactly one equipment on actual death");int drops=zone.items.size();b.die(owner);b.reward(owner);check(zone.items.size()==drops,"duplicate callbacks cannot award twice");
  FatMabuService service=FatMabuService.gI();var field=FatMabuService.class.getDeclaredField("boss");field.setAccessible(true);field.set(service,b);service.remove();
  check(service.current()==null&&b.zone==null&&!zone.getBosses().contains(b)&&!zone.getHumanoids().contains(b)&&!BossManager.gI().getBosses().contains(b),"map/manager cleanup completes");
  System.out.println("PASS actual Mabu entry, shield cast/cooldown/break/expiry, fatal death single equipment, cleanup");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
