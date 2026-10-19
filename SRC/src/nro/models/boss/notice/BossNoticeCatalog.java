package nro.models.boss.notice;
import nro.models.boss.BossID;
import java.util.*;
/** Mirrors TaskService kill-credit branches, using semantic identity rather than display names. */
public final class BossNoticeCatalog {
 private BossNoticeCatalog(){}
 private static final Set<Integer> KNOWN=new HashSet<>();
 static {for(var field:BossID.class.getFields())try{if(field.getType()==int.class)KNOWN.add(field.getInt(null));}catch(ReflectiveOperationException ignored){}}
 public static int category(int semanticId,int form){
  return switch(semanticId){
   // Fide belongs in the general Boss tab for every transformation. Xen's
   // quest-credit forms remain in the quest tab.
   case BossID.FIDE -> 2;
   case BossID.XEN_BO_HUNG -> form>=0&&form<=2?1:2;
   case BossID.SIEU_BO_HUNG -> form==1?1:2;
   case BossID.KUKU,BossID.MAP_DAU_DINH,BossID.RAMBO,BossID.SO_4,BossID.SO_3,BossID.SO_2,BossID.SO_1,BossID.TIEU_DOI_TRUONG,
    BossID.ANDROID_19,BossID.DR_KORE,BossID.ANDROID_13,BossID.ANDROID_14,BossID.ANDROID_15,BossID.POC,BossID.PIC,BossID.KING_KONG,
    BossID.XEN_CON_1,BossID.XEN_CON_2,BossID.XEN_CON_3,BossID.XEN_CON_4,BossID.XEN_CON_5,BossID.XEN_CON_6,BossID.XEN_CON_7,
    BossID.DRABURA,BossID.DRABURA_2,BossID.DRABURA_3,BossID.BUI_BUI,BossID.BUI_BUI_2,BossID.YA_CON,BossID.MABU_12H -> 1;
   default -> 2;
  };
 }
 public static int legacyCategory(long networkId,int form){return networkId==(int)networkId&&KNOWN.contains((int)networkId)?category((int)networkId,form):0;}
}
