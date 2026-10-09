package nro.models.boss;
import java.util.*;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.*;
public class PrisonBillBossTest {
 static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
 public static void main(String[] args){try{
  Class<?> catalog;try{catalog=Class.forName("nro.models.boss.PrisonBill.PrisonBillBossCatalog");}catch(ClassNotFoundException absent){throw new AssertionError("seven Prison/Bill boss templates missing",absent);}
  String[] keys={"HEART","KAMIN","OREN","BEERUS_BATTLE","WHIS_BATTLE","CHAMPA_BATTLE","VADOS_BATTLE"};
  Set<Integer> ids=new HashSet<>(),heads=new HashSet<>();
  for(int i=0;i<keys.length;i++){
   int id=BossID.class.getField(keys[i]).getInt(null);check(ids.add(id),"unique identity");
   BossData d=(BossData)catalog.getMethod("data",int.class).invoke(null,id);check(d!=null,"configured boss "+keys[i]);
   check(Arrays.equals(d.getHp(),new int[]{i==1||i==2?1_000_000_000:2_000_000_000}),"requested HP");
   check(d.getDame()==BossesData.SUPER_BLACK_GOKU.getDame()&&Arrays.deepEquals(d.getSkillTemp(),BossesData.SUPER_BLACK_GOKU.getSkillTemp()),"same skills and damage");
   check(Arrays.equals(d.getMapJoin(),new int[]{154,155}),"only approved planets");check(heads.add((int)d.getOutfit()[0]),"private part triples");
   check(d.getOutfit()[0]==2099+3*i,"manifest matches code");
   Boss b=BossManager.gI().createBoss(id);check(b!=null&&b.id==id&&b.data.length==1,"factory and one phase");
   b.currentLevel=0;check(b.getHead()==d.getOutfit()[0]&&b.getBody()==d.getOutfit()[1]&&b.getLeg()==d.getOutfit()[2],"appearance getters");
   check(d.getTypeAppear()==(i==2?AppearType.APPEAR_WITH_ANOTHER:AppearType.DEFAULT_APPEAR),"paired child never independently spawns");
   if(i==1){Boss[] group=b.getCompanionBosses();check(group.length==1&&group[0].id==BossID.class.getField("OREN").getInt(null)&&group[0].parentBoss==b,"Kamin owns Oren");}
  }
  check(BossID.WHIS==-364&&BossesData.WHIS.getName().equals("Whis"),"training Whis unchanged");
  System.out.println("PRISON_BILL_BOSS PASS seven/factory/hp/skills/maps/parts/group/training-identity");System.exit(0);
 }catch(Throwable failure){failure.printStackTrace();System.exit(1);}}
}
