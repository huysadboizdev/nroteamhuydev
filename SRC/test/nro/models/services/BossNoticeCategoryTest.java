package nro.models.services;
import nro.models.boss.notice.*;
import nro.models.boss.BossID;
public class BossNoticeCategoryTest {
 public static void main(String[] args)throws Exception {
  Class<?> catalog;
  try { catalog=Class.forName("nro.models.boss.notice.BossNoticeCatalog"); }
  catch(ClassNotFoundException missing){throw new AssertionError("semantic quest classification missing");}
  var method=catalog.getMethod("category",int.class,int.class);
  int[] quest={BossID.KUKU,BossID.MAP_DAU_DINH,BossID.RAMBO,BossID.SO_4,BossID.SO_3,BossID.SO_2,BossID.SO_1,BossID.TIEU_DOI_TRUONG,BossID.ANDROID_19,BossID.DR_KORE,BossID.ANDROID_13,BossID.ANDROID_14,BossID.ANDROID_15,BossID.POC,BossID.PIC,BossID.KING_KONG,BossID.XEN_BO_HUNG,BossID.XEN_CON_1,BossID.XEN_CON_7,BossID.DRABURA,BossID.DRABURA_2,BossID.DRABURA_3,BossID.BUI_BUI,BossID.BUI_BUI_2,BossID.YA_CON,BossID.MABU_12H};
  for(int id:quest)if((int)method.invoke(null,id,0)!=1)throw new AssertionError("quest id "+id);
  for(int form=0;form<3;form++)if((int)method.invoke(null,BossID.FIDE,form)!=2)throw new AssertionError("Fide form "+form+" must appear in general Boss tab");
  if((int)method.invoke(null,BossID.XEN_BO_HUNG,0)!=1 || (int)method.invoke(null,BossID.XEN_BO_HUNG,3)!=2 || (int)method.invoke(null,BossID.SIEU_BO_HUNG,1)!=1 || (int)method.invoke(null,BossID.SIEU_BO_HUNG,0)!=2)throw new AssertionError("form kill credit");
  for(int id:new int[]{BossID.BROLY,BossID.SUPER_BROLY,BossID.MAT_TROI,-900001})if((int)method.invoke(null,id,0)!=2)throw new AssertionError("general id "+id);
  var names=java.util.Arrays.stream(BossNoticeRow.class.getRecordComponents()).map(c->c.getName()).toList();
  if(!names.contains("category")||!names.contains("semanticBossId")||!names.contains("zoneId")||!names.contains("questSupport"))throw new AssertionError("classified snapshot fields missing");
  System.out.println("PASS semantic quest catalog and exact form credit");
 }
}
