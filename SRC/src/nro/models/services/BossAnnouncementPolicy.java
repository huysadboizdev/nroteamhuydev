package nro.models.services;
import nro.models.boss.Boss;
import nro.models.boss.BossID;
/** Explicit user exclusions and loot exceptions shared by the board and ticker. */
public final class BossAnnouncementPolicy {
 private BossAnnouncementPolicy(){}
 public static boolean excluded(int id){return id==BossID.AN_TROM||id==BossID.MAT_TROI||id==BossID.O_DO;}
 public static boolean excluded(Boss b){return excluded(b.questBossId())||b instanceof nro.models.boss.Boss_mini.AnTrom
   ||b instanceof nro.models.boss.Boss_mini.MatTroi||b instanceof nro.models.boss.Boss_mini.Odo;}
 public static boolean excludedHistory(nro.models.boss.notice.BossNoticeRow r){
  if(excluded(r.semanticBossId()))return true;
  String name=nro.models.utils.Util.removeAccent(r.bossName()).toLowerCase(java.util.Locale.ROOT).trim();
  return name.matches("(?:an trom|mat troi|o do)(?:\\s.*)?");
 }
 public static boolean equipment(Boss b){
  if(b==null||b.isQuestSupport()||excluded(b))return false;
  if(RequestedBossEquipment.hasEquipmentDrops(b)
    ||b instanceof nro.models.boss.HuntingTrio.HuntingTrioBoss
    ||b instanceof nro.models.boss.Mabu.FatMabuTournament)return true;
  // Native divine reward callbacks outside the recently requested low-rate pools.
  if(java.util.Set.of(BossID.BABY,BossID.BUI_BUI,BossID.BUI_BUI_2,BossID.YA_CON,
       BossID.MABU_12H,BossID.GOKU,BossID.CADIC).contains(b.questBossId()))return true;
  BossAdminConfig.Config c=BossAdminConfig.config(b);
  if(c==null||"default".equals(c.dropMode()))return false;
  return c.drops().stream().anyMatch(r->r.chanceBp()>0&&(r.itemId()>=555&&r.itemId()<=567
    ||r.options().stream().anyMatch(o->o.optionTemplate.id>=34&&o.optionTemplate.id<=36)));
 }
 public static boolean grouped(Boss b){return b instanceof nro.models.boss.HuntingTrio.HuntingTrioBoss||b instanceof nro.models.boss.Mabu.FatMabuTournament;}
}
