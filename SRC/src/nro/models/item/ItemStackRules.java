package nro.models.item;
import java.util.*;
import nro.models.player_system.Template.ItemTemplate;
import nro.models.server.Manager;

/** Explicit consumable/material corrections; never infer stacking from a pet/equipment type. */
public final class ItemStackRules {
 private ItemStackRules(){}
 public static final Set<Integer> CONSUMABLE_IDS=Set.of(1071,1072,1073,1084,1085,1086,1143,1509,1521,1525,1528,1529,1532,1569,1608,1609,1628,1652,1653,1775);
 public static boolean stackable(ItemTemplate t){return t!=null&&(t.isUpToUp||CONSUMABLE_IDS.contains((int)t.id));}
 public static void normalizeCatalog(){for(ItemTemplate t:Manager.ITEM_TEMPLATES)if(CONSUMABLE_IDS.contains((int)t.id))t.isUpToUp=true;}
 public static boolean sameExpiry(Item a,Item b){
  boolean timed=a.itemOptions.stream().anyMatch(o->o.optionTemplate.id==93)||b.itemOptions.stream().anyMatch(o->o.optionTemplate.id==93);
  return !timed||a.createTime==b.createTime;
 }
 public static boolean sameOptions(List<Item.ItemOption> a,List<Item.ItemOption> b){return values(a,-1).equals(values(b,-1));}
 public static boolean sameExcept(List<Item.ItemOption> a,List<Item.ItemOption> b,int except){return values(a,except).equals(values(b,except));}
 private static Map<Long,Integer> values(List<Item.ItemOption> options,int except){
  Map<Long,Integer> values=new HashMap<>();
  for(Item.ItemOption o:options){int id=o.optionTemplate.id;if(id==except||(id==73&&o.param==0))continue;long key=((long)id<<32)|(o.param&0xffffffffL);values.merge(key,1,Integer::sum);}
  return values;
 }
}
