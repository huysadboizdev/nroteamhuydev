package nro.models.services;
import java.util.*;
import org.json.simple.*;
public final class LiveSyncPlayerPatchTest {
 static void check(boolean v,String m){if(!v)throw new AssertionError(m);}
 static Map<String,String> patch(Map<String,String>b,String t,JSONObject p)throws Exception{
  try{return (Map<String,String>)Class.forName("nro.models.services.LiveSyncPlayerPatch").getMethod("patch",Map.class,String.class,JSONObject.class,int.class).invoke(null,b,t,p,0);}
  catch(java.lang.reflect.InvocationTargetException e){throw (Exception)e.getCause();}
 }
 static JSONObject payload(String k,Object v){JSONObject p=new JSONObject();p.put(k,v);return p;}
 public static void main(String[]args)throws Exception{
  try{Class.forName("nro.models.services.LiveSyncPlayerPatch");}catch(ClassNotFoundException e){throw new AssertionError("Player patch must exist to stage changes without touching live data");}
  Map<String,String>b=new HashMap<>();b.put("data_inventory","[10,20,30,40,50]");b.put("data_point","[2,300,400,88,100,1000,2000,3000,40,5,6,0,333,444]");
  JSONArray pet=new JSONArray();pet.add("[3,1,\"$Beer\",2,987,1]");pet.add("[4,60000000000,123,98,999,400000,400000,20000,99,7,321,654]");pet.add("[\"[-1,0,\\\"[]\\\",1]\"]");pet.add("[\"[0,1,765,4]\",\"[-1,0,0,0]\"]");b.put("pet",pet.toJSONString());String original=b.toString();
  try{patch(b,"UPDATE_PET",payload("pet_name","New"));throw new AssertionError("admin pet name must be rejected");}catch(IllegalArgumentException expected){}
  Map<String,String>r=patch(b,"UPDATE_PET",payload("pet_tiem_nang",124));JSONArray after=(JSONArray)JSONValue.parse(r.get("pet"));check(after.get(0).equals(pet.get(0))&&after.get(2).equals(pet.get(2))&&after.get(3).equals(pet.get(3)),"stats-only update preserves name/items/skills");JSONArray info=(JSONArray)JSONValue.parse((String)after.get(0));check(info.get(3).toString().equals("2")&&info.get(4).toString().equals("987"),"fusion preserved");
  String[] names={"$Đệ tử","$Mabư","$Uub","$Kid Beer","$Kid Jiren"};
  for(int type=0;type<names.length;type++){r=patch(b,"UPDATE_PET",payload("pet_type",type));after=(JSONArray)JSONValue.parse(r.get("pet"));info=(JSONArray)JSONValue.parse((String)after.get(0));check(info.get(2).equals(names[type]),"type change uses default name");check(after.get(1).equals(pet.get(1))&&after.get(3).equals(pet.get(3)),"type change preserves points/skills");check(info.get(3).toString().equals("2")&&info.get(4).toString().equals("987"),"type change preserves fusion");}
  r=patch(b,"UPDATE_STATS",payload("limit_power",8));check(((JSONArray)JSONValue.parse(r.get("data_point"))).get(0).toString().equals("8"),"limit_power persisted");check(original.equals(b.toString()),"input immutable");
  JSONObject p=payload("add_gem",Integer.MAX_VALUE);try{patch(b,"TOPUP_PLAYER",p);throw new AssertionError("overflow rejected");}catch(IllegalArgumentException expected){}check(original.equals(b.toString()),"overflow leaves input intact");
  PinkFixture.install();PinkFixture.replace(InventoryService.class,"I",new InventoryService());
  b.put("items_bag","[\"[663,999999,\\\"[\\\\\\\"[73,0]\\\\\\\"]\\\",1]\",\"[-1,0,\\\"[]\\\",1]\"]");
  p=payload("item_id",663);p.put("quantity",3);r=patch(b,"GIFT_ITEM",p);JSONArray bag=(JSONArray)JSONValue.parse(r.get("items_bag"));check(((JSONArray)JSONValue.parse((String)bag.get(0))).get(1).toString().equals("1000000"),"offline stacks to million");check(((JSONArray)JSONValue.parse((String)bag.get(1))).get(1).toString().equals("2"),"spill into existing slot");
  p=payload("slot",0);p.put("target","bag");p.put("expected_item","[664,999999,\"[]\",1]");try{patch(b,"DELETE_ITEM",p);throw new AssertionError("stale slot rejected");}catch(IllegalArgumentException expected){}
  p=payload("pet_hpg",0);try{patch(b,"UPDATE_PET",p);throw new AssertionError("zero pet HP rejected");}catch(IllegalArgumentException expected){}
  JSONArray different=(JSONArray)JSONValue.parse(b.get("pet"));different.set(3,"[\"[2,1,765,4]\",\"[-1,0,0,0]\"]");b.put("pet",different.toJSONString());
  p=payload("pet_skill_1",0);nro.models.server.Manager.NCLASS.clear();
  for(int i=0;i<3;i++){var n=new nro.models.skill.NClass();n.classId=i;for(int id:new int[]{0,2,4}){var t=new nro.models.player_system.Template.SkillTemplate();t.id=(byte)id;var s=new nro.models.skill.Skill();s.skillId=(short)id;s.template=t;s.point=1;t.skillss.add(s);n.skillTemplatess.add(t);}nro.models.server.Manager.NCLASS.add(n);}
  r=patch(b,"UPDATE_PET",p);after=(JSONArray)JSONValue.parse(r.get("pet"));check(((JSONArray)JSONValue.parse((String)((JSONArray)JSONValue.parse((String)after.get(3))).get(0))).get(0).toString().equals("0"),"skill ID zero accepted");
  Map<String,String>empty=new HashMap<>(b);empty.put("pet","[]");
  for(int kind=2;kind<=4;kind++){r=patch(empty,"UPDATE_PET",payload("pet_type",kind));after=(JSONArray)JSONValue.parse(r.get("pet"));check(((JSONArray)JSONValue.parse((String)after.get(0))).get(0).toString().equals(""+kind),"canonical advanced type mapping");check(((JSONArray)JSONValue.parse((String)after.get(1))).get(0).toString().equals("4"),"canonical starter limit4");check(((JSONArray)JSONValue.parse((String)after.get(2))).size()==9,"canonical advanced body slots");}
  r=patch(b,"HEAL_FULL",new JSONObject());JSONArray hp=(JSONArray)JSONValue.parse(r.get("data_point"));
  var loaded=new nro.models.player.Player();loaded.nPoint.hp=Integer.parseInt(String.valueOf(hp.get(12)));loaded.nPoint.mp=Integer.parseInt(String.valueOf(hp.get(13)));loaded.nPoint.hpg=1000;loaded.nPoint.mpg=2000;loaded.nPoint.hpMax=9999;loaded.nPoint.mpMax=8888;
  for(String method:new String[]{"setHp","setMp"}){var clamp=nro.models.player.NPoint.class.getDeclaredMethod(method);clamp.setAccessible(true);clamp.invoke(loaded.nPoint);}
  check(loaded.nPoint.hp==9999&&loaded.nPoint.mp==8888,"offline heal fills equipment-derived maxima when login clamps saved health");
  b.put("_hp_max","9999");b.put("_mp_max","8888");r=patch(b,"HEAL_FULL",new JSONObject());hp=(JSONArray)JSONValue.parse(r.get("data_point"));check(hp.get(12).toString().equals("9999")&&hp.get(13).toString().equals("8888"),"trusted derived max heal");
  b.put("data_magic_tree","[3,8,1,123,456]");r=patch(b,"UPGRADE_MAGIC_TREE",new JSONObject());check(r.get("data_magic_tree").equals("[10,8,0,123,456]"),"tree upgrade preserves harvest/timer");
  Map<String,String>full=new HashMap<>(b);full.put("items_bag","[\"[663,1000000,\\\"[\\\\\\\"[73,0]\\\\\\\"]\\\",1]\"]");String snapshot=full.toString();p=payload("add_gold",10);p.put("add_thoi_vang",1);try{patch(full,"TOPUP_PLAYER",p);throw new AssertionError("partial topup rejected");}catch(IllegalArgumentException expected){}check(full.toString().equals(snapshot),"gold stays unchanged when item does not fit");
  p=payload("slot",0);p.put("expected_item",((JSONArray)JSONValue.parse(full.get("items_bag"))).get(0));p.put("quantity",2);r=patch(full,"UPDATE_ITEM_QTY",p);check(((JSONArray)JSONValue.parse((String)((JSONArray)JSONValue.parse(r.get("items_bag"))).get(0))).get(1).toString().equals("2"),"matching snapshot updates exact slot");
  p=payload("pet_skill_4",10);try{patch(b,"UPDATE_PET",p);throw new AssertionError("invalid pet slot skill accepted");}catch(IllegalArgumentException expected){}
  PinkFixture.replace(ItemService.class,"i",new ItemService(){@Override public nro.models.player_system.Template.ItemTemplate getTemplate(int id){var t=PinkFixture.template(id);if(id==500){t.type=0;t.isUpToUp=false;}return t;}@Override public nro.models.player_system.Template.ItemOptionTemplate getItemOptionTemplate(int id){var t=new nro.models.player_system.Template.ItemOptionTemplate();t.id=id;t.name="test";return t;}});
  full.put("items_bag","[\"[-1,0,\\\"[]\\\",1]\"]");p=payload("item_id",500);p.put("quantity",2);try{patch(full,"GIFT_ITEM",p);throw new AssertionError("equipment grants require separate slots");}catch(IllegalArgumentException expected){}
  full.put("items_bag","[\"[500,1,\\\"[]\\\",1]\"]");p=payload("slot_index",0);p.put("expected_item",((JSONArray)JSONValue.parse(full.get("items_bag"))).get(0));p.put("quantity",2);try{patch(full,"UPDATE_ITEM_QTY",p);throw new AssertionError("equipment cannot stack");}catch(IllegalArgumentException expected){}p.put("quantity",1);check(patch(full,"UPDATE_ITEM_QTY",p).isEmpty(),"web slot_index supported without rewriting unchanged slot");
  JSONArray low=(JSONArray)JSONValue.parse(b.get("pet"));low.set(1,"[0,60000000000,123,98,999,400000,400000,20000,99,7,321,654]");full.put("pet",low.toJSONString());
  r=patch(full,"UPDATE_PET",payload("pet_type",2));after=(JSONArray)JSONValue.parse(r.get("pet"));check(((JSONArray)JSONValue.parse((String)after.get(1))).get(0).toString().equals("4"),"explicit advanced type change applies canonical minimum tier");
  var invalidTask=new nro.models.task.TaskMain();invalidTask.id=999;nro.models.server.Manager.TASKS.add(invalidTask);try{patch(b,"CHANGE_TASK",payload("task_id",999));throw new AssertionError("empty subtasks accepted");}catch(IllegalArgumentException expected){}
  System.out.println("PASS pet preservation, immutable staging, limit_power, currency overflow, million stacks, stale slots");
 }
}
