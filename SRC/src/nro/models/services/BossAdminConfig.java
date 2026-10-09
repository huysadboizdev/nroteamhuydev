package nro.models.services;

import java.nio.file.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import nro.models.boss.Boss;
import nro.models.consts.BossStatus;
import nro.models.player.Player;
import nro.models.item.Item;
import org.json.simple.*;

/** Immutable staged administrative policy. SQL and catalog validation never run in combat. */
public final class BossAdminConfig {
 private BossAdminConfig() {}
 public record Rule(String group,int itemId,int quantityMin,int quantityMax,int chanceBp,int weight,List<Item.ItemOption> options) { public Rule { options=copyOptions(options); } @Override public List<Item.ItemOption> options(){return copyOptions(options);} }
  public record Config(Integer hp,Integer damage,Integer capDamage,BigDecimal capPercent,Integer defense,Integer critical,String applyMode,String dropMode,List<Rule> drops,long revision) { public Config { drops=List.copyOf(drops); } }
 private static List<Item.ItemOption> copyOptions(List<Item.ItemOption> options){List<Item.ItemOption> copy=new ArrayList<>();for(Item.ItemOption option:options)copy.add(new Item.ItemOption(option.optionTemplate,option.param));return List.copyOf(copy);}
 private record Key(String boss,int phase) {}
 private record Snapshot(long revision,Map<Key,Config> rows,Map<String,Set<Integer>> catalog) {}
 private static final AtomicReference<Snapshot> CURRENT=new AtomicReference<>(new Snapshot(-1,Map.of(),Map.of()));
 public static long revision(){return CURRENT.get().revision;}
 private static String keyFor(Snapshot s,Boss boss){String c="class:"+boss.getClass().getName();
  if(boss.data!=null&&boss.data.length>0&&boss.data[0]!=null){String variant=c+"#"+boss.data[0].getName();if(s.catalog.containsKey(variant))return variant;}
  return s.catalog.containsKey(c)?c:"id:"+boss.questBossId();}
 public static String keyFor(Boss boss){return keyFor(CURRENT.get(),boss);}
 public static Config config(Boss boss){Snapshot s=CURRENT.get();return s.rows.get(new Key(keyFor(s,boss),boss.currentLevel));}
 private static void valid(boolean ok,String message){if(!ok)throw new IllegalArgumentException(message);}
 private static Map<String,Set<Integer>> catalog()throws SQLException {
  Path p=Path.of("data/boss_admin_catalog.json");if(!Files.exists(p))p=Path.of("SRC/data/boss_admin_catalog.json");
  try {Object raw=JSONValue.parse(Files.readString(p));valid(raw instanceof JSONArray,"Invalid boss catalog");Map<String,Set<Integer>> out=new HashMap<>();
   for(Object o:(JSONArray)raw){JSONObject e=(JSONObject)o;String k=(String)e.get("key");Set<Integer> phases=new HashSet<>();for(Object ph:(JSONArray)e.get("phases"))phases.add(((Number)((JSONObject)ph).get("phase")).intValue());valid(k!=null&&!phases.isEmpty()&&out.put(k,Set.copyOf(phases))==null,"Invalid duplicate catalog key");}return Map.copyOf(out);
  }catch(java.io.IOException|ClassCastException e){throw new SQLException("Cannot load boss admin catalog",e);}
 }
 public static Runnable stage(Connection c)throws SQLException {
  Map<String,Set<Integer>> catalog=catalog();long revision;
  try(PreparedStatement q=c.prepareStatement("SELECT revision FROM boss_admin_state WHERE id=1");ResultSet r=q.executeQuery()){valid(r.next(),"Missing boss admin state");revision=r.getLong("revision");valid(revision>=0&&!r.next(),"Invalid boss admin state");}
  Map<Key,Config> rows=new HashMap<>();
   try(PreparedStatement q=c.prepareStatement("SELECT boss_key,phase,hp,damage,cap_damage,cap_percent,defense,critical,apply_mode,drop_mode,drops,revision FROM boss_admin_config ORDER BY boss_key,phase");ResultSet r=q.executeQuery()) {
    while(r.next()) {String k=r.getString("boss_key");int phase=r.getInt("phase");Integer hp=nullable(r,"hp"),damage=nullable(r,"damage"),cap=nullable(r,"cap_damage"),def=nullable(r,"defense"),crit=nullable(r,"critical");BigDecimal percent=r.getBigDecimal("cap_percent");String mode=r.getString("apply_mode"),drop=r.getString("drop_mode");long rev=r.getLong("revision");
     valid(catalog.containsKey(k)&&catalog.get(k).contains(phase),"Unknown boss key/phase: "+k+"/"+phase);
     valid((hp==null||hp>0)&&(damage==null||damage>=0)&&(cap==null||cap>=0)&&(percent==null||percent.signum()>=0&&percent.compareTo(new BigDecimal("100"))<=0&&percent.scale()<=3),"Invalid boss numeric settings");
     valid((def==null||def>=0)&&(crit==null||crit>=0&&crit<=100),"Invalid boss defense/crit");
     valid(Set.of("next","alive").contains(mode)&&Set.of("default","replace").contains(drop)&&rev>=0&&rev<=revision,"Invalid boss policy/revision");
     Config config=new Config(hp,damage,cap,percent,def,crit,mode,drop,rules(r.getString("drops")),rev);valid(rows.put(new Key(k,phase),config)==null,"Duplicate boss policy");
   }
  }
  try(PreparedStatement q=c.prepareStatement("SELECT revision FROM boss_admin_state WHERE id=1");ResultSet r=q.executeQuery()) {if(!r.next()||r.getLong("revision")!=revision)throw new SQLException("Boss admin revision changed while staging; retry");}
  Snapshot staged=new Snapshot(revision,Map.copyOf(rows),catalog);
  return ()->CURRENT.updateAndGet(old->staged.revision>old.revision?staged:old);
 }
 private static Integer nullable(ResultSet r,String key)throws SQLException{int v=r.getInt(key);return r.wasNull()?null:v;}
 private static int number(JSONObject o,String key){Object v=o.get(key);valid(v instanceof Number,"Missing drop "+key);long n=((Number)v).longValue();valid(n>=0&&n<=Integer.MAX_VALUE&&((Number)v).doubleValue()==n,"Invalid drop "+key);return (int)n;}
 private static List<Rule> rules(String json) {
  Object raw=JSONValue.parse(json);valid(raw instanceof JSONArray,"Invalid drops JSON");List<Rule> out=new ArrayList<>();Map<String,Integer> chances=new HashMap<>();Map<String,Long> weights=new HashMap<>();valid(((JSONArray)raw).size()<=200,"Too many drops");
  for(Object obj:(JSONArray)raw){valid(obj instanceof JSONObject,"Invalid drop row");JSONObject o=(JSONObject)obj;Object g=o.get("group");valid(g instanceof String&&((String)g).length()<=80,"Invalid drop group");String group=(String)g;int id=number(o,"item_id"),min=number(o,"quantity_min"),max=number(o,"quantity_max"),chance=number(o,"chance_bp"),weight=number(o,"weight");
   valid(min>0&&max>=min&&chance<=10000&&(group.isEmpty()||weight>0)&&LiveSyncCatalogCommands.template(id)!=null,"Invalid drop item/quantity/chance/weight");
   if(!group.isEmpty()){Integer previous=chances.putIfAbsent(group,chance);valid(previous==null||previous==chance,"Conflicting group chance");long total=weights.merge(group,(long)weight,Long::sum);valid(total<=Integer.MAX_VALUE,"Group weight overflow");}
   valid(o.get("options") instanceof JSONArray,"Invalid drop options");List<Item.ItemOption> options=new ArrayList<>();for(Object op:(JSONArray)o.get("options")){valid(op instanceof JSONObject,"Invalid option");JSONObject opt=(JSONObject)op;options.add(LiveSyncCatalogCommands.option(number(opt,"id"),number(opt,"param")));}
   out.add(new Rule(group,id,min,max,chance,weight,options));
  }return List.copyOf(out);
 }
 /** Native spawn resets can be followed by a class adapter without changing the life counter. */
 private static boolean hasNativeSpawnReset(Boss b){for(Class<?> type=b.getClass();type!=null;type=type.getSuperclass())if(Set.of("nro.models.boss.luyen_tap_tu_dong.Whis","nro.models.boss.vo_dai_hat_mit.DeathOrAliveArena","nro.models.boss.sieu_hang.SuperRank","nro.models.boss.Broly.Broly","nro.models.boss.Broly.SuperBroly","nro.models.boss.Boss_mini.AnTrom","nro.models.boss.Boss_mini.Virut","nro.models.boss.Boss_mini.MatTroi","nro.models.boss.HuntingTrio.HuntingTrioBoss","nro.models.boss.Mabu.FatMabuTournament").contains(type.getName()))return true;return false;}
  public static void applyAtSpawn(Boss b){synchronized(b){if(b.nPoint==null)return;b.adminLife++;b.adminAppliedRevision=-1;b.adminStats=null;resetAdminOwnedAdds(b);captureDefaults(b);if(!hasNativeSpawnReset(b))apply(b,config(b),true);}}
   private static void captureDefaults(Boss b){b.adminDefaultHp=b.nPoint.hpMax>0?b.nPoint.hpMax:b.nPoint.hpg;b.adminDefaultDamage=b.nPoint.dame;b.adminDefaultDef=b.nPoint.defAdd;b.adminDefaultCrit=b.nPoint.critAdd;}
  /** Native boss code never sets defAdd/critAdd (only XenBoHung critg); admin owns them, so clear leftovers before capturing per-life defaults. */
  private static void resetAdminOwnedAdds(Boss b){b.nPoint.defAdd=0;b.nPoint.critAdd=0;}
  public static void applyAfterNativeSpawn(Boss b){synchronized(b){if(b.nPoint==null)return;resetAdminOwnedAdds(b);captureDefaults(b);b.adminAppliedRevision=-1;apply(b,config(b),true);}}
 private static void apply(Boss b,Config c,boolean spawn){
  b.adminStats=c;
  if(c==null){if(spawn)BossLiveReload.applyLegacyAtSpawn(b);return;}
  if(b instanceof nro.models.boss.sieu_hang.SuperRank)b.nPoint=detachedPoints(b,b.nPoint);
  int oldMax=Math.max(1,b.nPoint.hpMax),hp=b.nPoint.hp;
  int max=c.hp()==null?b.adminDefaultHp:c.hp();
  if(max>0){b.nPoint.hpg=b.nPoint.hpMax=max;b.nPoint.hp=spawn?max:(int)Math.max(1,Math.min(max,(long)hp*max/oldMax));}
   int damage=c.damage()==null?b.adminDefaultDamage:c.damage();b.nPoint.dameg=b.nPoint.dame=damage;
   int defense=c.defense()==null?b.adminDefaultDef:c.defense();b.nPoint.defAdd=defense;b.nPoint.def=b.nPoint.defg*4+defense;
   int critical=c.critical()==null?b.adminDefaultCrit:c.critical();b.nPoint.critAdd=critical;b.nPoint.crit=b.nPoint.critg+critical;
   b.adminAppliedRevision=c.revision();b.adminAliveMode="alive".equals(c.applyMode());
   // Clients already in the map need both HP values; do not wait for the next hit.
   if(!spawn&&b.zone!=null&&(hp!=b.nPoint.hp||oldMax!=b.nPoint.hpMax))Service.gI().reload_HP_NV(b);
 }
 public static void applyAtTick(Boss b){Snapshot s=CURRENT.get();synchronized(b){if(b.nPoint==null||b.nPoint.hp<=0||b.bossStatus==null||!(b.bossStatus==BossStatus.ACTIVE||b.bossStatus==BossStatus.AFK||b.bossStatus==BossStatus.CHAT_S))return;Config c=s.rows.get(new Key(keyFor(s,b),b.currentLevel));if(c!=null&&"alive".equals(c.applyMode())&&b.adminAppliedRevision<c.revision())apply(b,c,false);}}
 /** Native dynamic combat formulas retain their behavior when no damage override is selected. */
 public static int nativeHp(Boss b,int calculated){Config c=b.adminStats;return c!=null&&c.hp()!=null?c.hp():calculated;}
 /** Copy combat points for a rank mirror before any administrative mutation. */
 public static nro.models.player.NPoint detachedPoints(Boss owner,nro.models.player.NPoint source){
  nro.models.player.NPoint copy=new nro.models.player.NPoint(owner);
  try {for(java.lang.reflect.Field field:nro.models.player.NPoint.class.getDeclaredFields())if(!java.lang.reflect.Modifier.isStatic(field.getModifiers())&&!java.lang.reflect.Modifier.isFinal(field.getModifiers())){field.setAccessible(true);Object value=field.get(source);if(value instanceof List<?>)value=new ArrayList<>((List<?>)value);field.set(copy,value);}copy.setPlayer(owner);return copy;}
  catch(ReflectiveOperationException error){throw new IllegalStateException("Cannot detach rank mirror stats",error);}
 }
 public static int nativeDamage(Boss b,int calculated){Config c=b.adminStats;return c!=null&&c.damage()!=null?c.damage():calculated;}
 public static long limitDamage(Player attacker,Player target,long damage){if(!(attacker instanceof Boss b)||target==null||!target.isPl()||target.isBoss||target.isPet||target.isBot||target.isNewPet||target.isNewPet1||damage<=0)return damage;Config c=config(b);if(c==null)return damage;if(c.capDamage()!=null)damage=Math.min(damage,c.capDamage());if(c.capPercent()!=null&&target.nPoint!=null)damage=Math.min(damage,c.capPercent().multiply(BigDecimal.valueOf(Math.max(0,target.nPoint.hpMax))).divide(BigDecimal.valueOf(100)).longValue());return damage;}
}
