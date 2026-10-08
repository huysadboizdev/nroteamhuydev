package nro.models.services;
import java.lang.reflect.*;
import nro.models.boss.Broly.SuperBroly;
import nro.models.player.*;
import nro.models.skill.*;
import nro.models.player_system.Template;
import static nro.models.services.PinkFixture.*;
public final class SuperBrolyDamagePolicyTest {
 static void select(Player p,int id){Skill s=new Skill();s.template=new Template.SkillTemplate();s.template.id=(byte)id;p.playerSkill.skillSelect=s;}
 static SuperBroly boss()throws Exception {
  SuperBroly b=blank(SuperBroly.class);b.isBoss=true;b.zone=zone();b.nPoint=blank(NPoint.class);b.nPoint.hp=b.nPoint.hpMax=1_500_000;b.playerSkill=new PlayerSkill(b);
  for(int i=0;i<7;i++){select(b,Skill.TAI_TAO_NANG_LUONG);b.playerSkill.skills.add(b.playerSkill.skillSelect);}
  Field f=SuperBroly.class.getDeclaredField("joined");f.setAccessible(true);f.setBoolean(b,true);return b;
 }
 public static void main(String[] args)throws Exception {try{
  install();HuntingTrioEntryTest.skills();nro.models.server.Manager.INTRINSICS.add(new nro.models.intrinsic.Intrinsic());
  replace(SkillService.class,"instance",new SkillService(){@Override public boolean useSkill(Player p,Player target,nro.models.mob.Mob mob,int id,nro.models.network.Message msg){return false;}});
  Player a=new Player();SuperBroly b=boss();a.zone=b.zone;
  select(a,Skill.TU_SAT);check(b.injured(a,900_000,true,false)==10_000,"bomb must cause fixed10k not original/%HP damage");
  b.nPoint.def=9000;check(b.injured(a,1,true,false)==10_000,"bomb10k independent of defense and original positive amount");b.nPoint.def=0;
  for(int id:new int[]{Skill.ANTOMIC,Skill.KAMEJOKO,Skill.MASENKO,Skill.LIEN_HOAN_CHUONG,Skill.SUPER_KAME,Skill.MAKANKOSAPPO,Skill.QUA_CAU_KENH_KHI}){
   select(a,id);int hp=b.nPoint.hp;check(b.injured(a,500_000,true,false)==0&&b.nPoint.hp==hp,"beam/special skill cannot damage Super:"+id);
  }
  select(a,Skill.LIEN_HOAN_CHUONG);a.newSkill.skillSelect=a.playerSkill.skillSelect;a.newSkill.isStartSkillSpecial=true;select(a,Skill.GALICK);
  int unchanged=b.nPoint.hp;check(b.injured(a,500_000,false,false)==0&&b.nPoint.hp==unchanged,"switching selected skill cannot disguise a running new-skill cast as melee");
  var attack=SkillService.class.getDeclaredMethod("playerAttackPlayer",Player.class,Player.class,boolean.class,int.class);attack.setAccessible(true);a.nPoint.dame=500_000;
  attack.invoke(SkillService.gI(),a,b,false,Skill.LIEN_HOAN_CHUONG);check(b.nPoint.hp==unchanged,"real special attack callback after melee switch remains harmless");
  select(a,Skill.TU_SAT);attack.invoke(SkillService.gI(),a,b,false,Skill.LIEN_HOAN_CHUONG);check(b.nPoint.hp==unchanged,"real special callback after bomb switch cannot mint10k ticks");
  select(a,Skill.GALICK);var directAttack=SkillService.class.getDeclaredMethod("playerAttackPlayer",Player.class,Player.class,boolean.class);directAttack.setAccessible(true);
  directAttack.invoke(SkillService.gI(),a,b,false);check(b.nPoint.hp<unchanged,"legitimate melee still works during special animation");
  select(a,Skill.TU_SAT);int bombHp=b.nPoint.hp;check(BossNoticeService.injure(b,a,500_000,true,false,(int)Skill.TU_SAT)==10_000&&b.nPoint.hp==bombHp-10_000,"actual explosion retains10k during special animation");
  a.newSkill.isStartSkillSpecial=false;
  for(int id:new int[]{Skill.GALICK,Skill.DRAGON,Skill.DEMON,Skill.LIEN_HOAN,Skill.KAIOKEN}){
   select(a,id);int hp=b.nPoint.hp;int applied=b.injured(a,500_000,false,false);check(applied>0&&applied<=b.nPoint.hpMax/100&&b.nPoint.hp==hp-applied,"melee retained with existing cap");
  }
  select(b,Skill.ANTOMIC);int hp=b.nPoint.hp;check(b.injured(b,50_000,true,false)==50_000&&b.nPoint.hp==hp-50_000,"reflection allowed independent of boss selected skill");
  select(a,Skill.TU_SAT);check(b.injured(a,0,true,false)==0,"zero/miss cannot cause10k");
  check(b.injured(null,100,true,false)==0,"null source safe");a.zone=zone();check(b.injured(a,100,true,false)==0,"cannot hit after leaving zone");
  System.out.println("PASS Super damage: fixed10k bomb, melee/reflection only, forbidden beams, miss/null/zone guards");System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
