package nro.models.event;
import java.util.*;
import nro.models.boss.BossID;
/** Explicit ownership: rewards never appear in the consumable list. */
public final class EventCatalog {
 public record Definition(String key,String name,boolean supported,String reason,int[] consumables,int[] keptRewards,int[] bossIds,int[] npcIds) {}
 private static final List<Definition> ALL=List.of(
  new Definition("hung_vuong","Hùng Vương",true,"",new int[]{1220,1221,1222,1542,1544,1545,1546,1547,1548,1549,1556,1558},new int[]{1776,1777,381,382,383,384,385,1150,1151,1152,1153,1154,1423,1438,1634,1635},new int[]{BossID.THUY_TINH},new int[]{52,66}),
  new Definition("summer","Hè",true,"",new int[]{1609,1612,1613,1614,1615,1616,1798,1799,1800,1801,1802},new int[]{1631,1804},new int[]{},new int[]{84}),
  new Definition("halloween","Halloween",true,"",new int[]{585},new int[]{},new int[]{BossID.BIMA,BossID.MATROI,BossID.DOI},new int[]{}),
  new Definition("christmas","Noel",true,"",new int[]{649},new int[]{648},new int[]{BossID.ONG_GIA_NOEL},new int[]{}),
  new Definition("lunar_new_year","Tết",false,"Cần hoàn thiện phân loại nguyên liệu và NPC trước khi bật",new int[]{},new int[]{569},new int[]{BossID.LAN_CON},new int[]{49}),
  new Definition("trung_thu","Trung thu",false,"Outfit boss có part sai vị trí, chưa đủ điều kiện bật",new int[]{},new int[]{},new int[]{},new int[]{}),
  new Definition("womens_day","8/3",false,"Chưa có luồng hiệu ứng đầy đủ",new int[]{},new int[]{},new int[]{},new int[]{}),
  new Definition("teachers_day","20/11",false,"Chưa có nội dung",new int[]{},new int[]{},new int[]{},new int[]{}),
  new Definition("valentine","Valentine",false,"Chưa có nội dung",new int[]{},new int[]{},new int[]{},new int[]{}),
  new Definition("top_up","TopUp",false,"Event hiện là lớp rỗng; không điều khiển giao dịch nạp",new int[]{},new int[]{},new int[]{},new int[]{}));
 public static List<Definition> all(){return ALL;}
 public static Definition get(String key){return ALL.stream().filter(d->d.key().equals(key)).findFirst().orElse(null);}
}
