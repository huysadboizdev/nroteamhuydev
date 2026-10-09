package nro.models.services;
import nro.models.boss.*;
import java.sql.*;
import java.lang.reflect.*;
import java.util.*;
import static nro.models.services.PinkFixture.*;
public final class BossAdminConfigTest {
 static Connection db(long revision,List<Map<String,Object>> rows) {
  return (Connection)Proxy.newProxyInstance(BossAdminConfigTest.class.getClassLoader(),new Class[]{Connection.class},(o,m,a)->{
   if(!m.getName().equals("prepareStatement"))return null;
   boolean state=((String)a[0]).contains("boss_admin_state");
   List<Map<String,Object>> data=state?List.of(Map.of("revision",revision)):rows;
   return Proxy.newProxyInstance(BossAdminConfigTest.class.getClassLoader(),new Class[]{PreparedStatement.class},(p,pm,pa)->{
    if(!pm.getName().equals("executeQuery"))return null;int[] i={-1};boolean[] nil={false};
    return Proxy.newProxyInstance(BossAdminConfigTest.class.getClassLoader(),new Class[]{ResultSet.class},(r,rm,ra)->{
     if(rm.getName().equals("next"))return ++i[0]<data.size();if(rm.getName().equals("wasNull"))return nil[0];
     if(rm.getName().startsWith("get")){Object v=data.get(i[0]).get(ra[0]);nil[0]=v==null;return switch(rm.getName()){case "getInt"->v==null?0:((Number)v).intValue();case "getLong"->v==null?0L:((Number)v).longValue();case "getString"->v==null?null:v.toString();case "getBigDecimal"->v==null?null:new java.math.BigDecimal(v.toString());default->null;};}return null;
    });
   });
  });
 }
  static Map<String,Object> row(long rev,String mode,int hp){return new HashMap<>(Map.of("boss_key","id:"+BossID.FIDE,"phase",0,"hp",hp,"damage",30,"cap_damage",25,"cap_percent","10.000","apply_mode",mode,"drop_mode","default","drops","[]","revision",rev));}
  static Map<String,Object> defRow(long rev,int defense,int critical){Map<String,Object> m=row(rev,"next",200);m.put("defense",defense);m.put("critical",critical);return m;}
  static void defenseCrit()throws Exception {
   install();var api=Class.forName("nro.models.services.BossAdminConfig");
   var stage=api.getMethod("stage",Connection.class);var spawn=api.getMethod("applyAtSpawn",Boss.class);
   Boss b=blank(Boss.class);b.id=BossID.FIDE;b.currentLevel=0;b.isBoss=true;b.bossStatus=nro.models.consts.BossStatus.ACTIVE;b.nPoint=new nro.models.player.NPoint(b);b.nPoint.hpg=b.nPoint.hpMax=100;b.nPoint.hp=100;b.nPoint.dameg=b.nPoint.dame=10;
   ((Runnable)stage.invoke(null,db(7,List.of(defRow(7,1000,15))))).run();spawn.invoke(null,b);
   check(b.nPoint.defAdd==1000&&b.nPoint.def==1000,"armor lands in defAdd and effective def");
   check(b.nPoint.critAdd==15&&b.nPoint.crit==15,"crit lands in critAdd and effective crit");
   // defAdd/critAdd survive NPoint.calPoint by construction (def=defg*4+defAdd, crit=critg+critAdd); blank fixtures cannot run full calPoint.
   ((Runnable)stage.invoke(null,db(8,List.of(row(8,"next",200))))).run();spawn.invoke(null,b);
   check(b.nPoint.defAdd==0&&b.nPoint.critAdd==0,"nullable reset restores native armor/crit");
   var bad=defRow(8,-1,0);
   try{stage.invoke(null,db(8,List.of(bad)));throw new AssertionError("negative defense accepted");}catch(InvocationTargetException e){check(e.getCause() instanceof IllegalArgumentException,"defense bound");}
   bad=defRow(8,0,101);
   try{stage.invoke(null,db(8,List.of(bad)));throw new AssertionError("crit>100 accepted");}catch(InvocationTargetException e){check(e.getCause() instanceof IllegalArgumentException,"crit bound");}
   System.out.println("Boss admin defense/crit PASS");
  }
 public static void main(String[] args)throws Exception {
  Class<?> api=Class.forName("nro.models.services.BossAdminConfig");
  var stage=api.getMethod("stage",Connection.class);var spawn=api.getMethod("applyAtSpawn",Boss.class);var tick=api.getMethod("applyAtTick",Boss.class);
  install();Boss b=blank(Boss.class);b.id=BossID.FIDE;b.currentLevel=0;b.isBoss=true;b.bossStatus=nro.models.consts.BossStatus.ACTIVE;b.nPoint=new nro.models.player.NPoint(b);b.nPoint.hpg=b.nPoint.hpMax=100;b.nPoint.hp=40;b.nPoint.dameg=b.nPoint.dame=10;
  Runnable first=(Runnable)stage.invoke(null,db(1,List.of(row(1,"next",200))));check(b.nPoint.hp==40,"stage must not touch boss");first.run();tick.invoke(null,b);check(b.nPoint.hp==40,"next does not update alive");spawn.invoke(null,b);check(b.nPoint.hp==200&&b.nPoint.hpMax==200,"spawn stats");
  b.nPoint.hp=50;Runnable newer=(Runnable)stage.invoke(null,db(3,List.of(row(3,"alive",400))));Runnable older=(Runnable)stage.invoke(null,db(2,List.of(row(2,"alive",300))));newer.run();older.run();tick.invoke(null,b);check(b.nPoint.hp==100&&b.nPoint.hpMax==400,"ratio and monotonic revision");b.nPoint.hp=75;tick.invoke(null,b);check(b.nPoint.hp==75,"once per revision");
  var cap=api.getMethod("limitDamage",nro.models.player.Player.class,nro.models.player.Player.class,long.class);var target=player(77);target.nPoint=new nro.models.player.NPoint(target);target.nPoint.hpMax=100;check((long)cap.invoke(null,b,target,500L)==10,"outgoing percentage cap");check((long)cap.invoke(null,target,b,500L)==500,"incoming untouched");
  b.nPoint.hp=0;((Runnable)stage.invoke(null,db(4,List.of(row(4,"alive",800))))).run();tick.invoke(null,b);check(b.nPoint.hp==0,"dead never revived");
  try{stage.invoke(null,db(5,List.of(row(5,"alive",0))));throw new AssertionError("invalid accepted");}catch(InvocationTargetException e){check(e.getCause() instanceof IllegalArgumentException,"validation failure");}
  // All failed stages retain the last valid snapshot, including SQL transport failures.
  var bad=row(5,"alive",900);bad.put("boss_key","class:missing.Boss");
  try{stage.invoke(null,db(5,List.of(bad)));throw new AssertionError("unknown key accepted");}catch(InvocationTargetException e){check(e.getCause() instanceof IllegalArgumentException,"unknown catalog rejected");}
  Connection failure=(Connection)Proxy.newProxyInstance(api.getClassLoader(),new Class[]{Connection.class},(o,m,a)->{throw new SQLException("isolated failure");});
  try{stage.invoke(null,failure);throw new AssertionError("SQL failure ignored");}catch(InvocationTargetException e){check(e.getCause() instanceof SQLException,"SQL failure propagated");}
  check((long)api.getMethod("revision").invoke(null)==4,"failed staging retains revision");
  b.nPoint.hp=400;var reset=row(5,"alive",800);reset.remove("hp");reset.remove("damage");reset.remove("cap_damage");reset.remove("cap_percent");((Runnable)stage.invoke(null,db(5,List.of(reset)))).run();tick.invoke(null,b);check(b.nPoint.hpMax==100&&b.nPoint.hp==100&&b.nPoint.dame==10,"nullable reset restores native stats and ratio");
  Boss clone=blank(Boss.class);clone.id=BossID.FIDE;clone.currentLevel=0;clone.isBoss=true;clone.bossStatus=nro.models.consts.BossStatus.ACTIVE;clone.nPoint=new nro.models.player.NPoint(clone);clone.nPoint.hpMax=clone.nPoint.hpg=100;clone.nPoint.hp=100;clone.nPoint.dame=clone.nPoint.dameg=10;spawn.invoke(null,clone);
  ((Runnable)stage.invoke(null,db(6,List.of(row(6,"alive",Integer.MAX_VALUE))))).run();tick.invoke(null,b);tick.invoke(null,clone);check(b.nPoint.hp==Integer.MAX_VALUE&&clone.nPoint.hp==Integer.MAX_VALUE,"all independent clones and INT max boundary");
  clone.currentLevel=1;clone.nPoint.hp=23;tick.invoke(null,clone);check(clone.nPoint.hp==23,"phase isolation");
  b.currentLevel=0;b.zone=zone();b.playerSkill=new nro.models.skill.PlayerSkill(b);var skill=new nro.models.skill.Skill();skill.template=new nro.models.player_system.Template.SkillTemplate();b.playerSkill.skillSelect=skill;
  var victim=new nro.models.player.Player();victim.isPlayer=true;victim.zone=b.zone;victim.nPoint.hp=victim.nPoint.hpMax=1000;victim.nPoint.def=300;victim.nPoint.tlGiap=0;victim.lastTimeRevived=0;
  check(victim.injured(b,500,false,false)==25&&victim.nPoint.hp==975,"cap applied to actual post-defense HP loss");
  victim.effectSkill.isShielding=true;check(victim.injured(b,100,false,false)==1&&victim.nPoint.hp==974,"shield preserved before cap");
  long life=b.adminLife;b.nPoint.hp=50;spawn.invoke(null,b);check(b.adminLife==life+1&&b.nPoint.hp==Integer.MAX_VALUE,"spawn advances life and reapplies current configuration");
  var variant=blank(nro.models.boss.HuntingTrio.HuntingTrioBoss.class);var field=Boss.class.getDeclaredField("data");field.setAccessible(true);field.set(variant,new BossData[]{nro.models.boss.HuntingTrio.HuntingTrioCatalog.data(0,0)});check(api.getMethod("keyFor",Boss.class).invoke(null,variant).toString().endsWith("#Basil"),"same class variants have independent key");
  System.out.println("Boss admin snapshot, next/alive ratio, revision, dead and outgoing cap PASS");
  defenseCrit();
 }
}
