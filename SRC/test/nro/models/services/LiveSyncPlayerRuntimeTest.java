package nro.models.services;
import java.lang.reflect.*;
import java.util.*;
import nro.models.player.*;
import nro.models.item.Item;
import nro.models.skill.Skill;
import org.json.simple.*;
public final class LiveSyncPlayerRuntimeTest {
 static Map<String,String> snapshot(Player p)throws Exception{return (Map<String,String>)Class.forName("nro.models.services.LiveSyncPlayerRuntime").getMethod("snapshot",Player.class).invoke(null,p);}
 static void publish(Player p,Map<String,String> values)throws Exception{try{Class.forName("nro.models.services.LiveSyncPlayerRuntime").getMethod("publish",Player.class,Map.class).invoke(null,p,values);}catch(InvocationTargetException e){throw (Exception)e.getCause();}}
 static void check(boolean value,String m){if(!value)throw new AssertionError(m);}
 public static void main(String[]args)throws Exception{
  try{Class.forName("nro.models.services.LiveSyncPlayerRuntime");}catch(ClassNotFoundException e){throw new AssertionError("Runtime snapshot and committed publish must exist");}
  PinkFixture.install();Player p=new Player();p.id=999;p.gender=2;p.nPoint.hpg=1000;p.nPoint.mpg=2000;p.nPoint.hp=99;p.nPoint.mp=88;p.nPoint.hpMax=1500;p.nPoint.mpMax=2500;p.nPoint.power=123;p.nPoint.stamina=42;p.inventory.gold=20;p.inventory.coupon=50;
  for(int i=0;i<9;i++)p.inventory.itemsBody.add(new Item());p.inventory.itemsBag.add(PinkFixture.item(663,3));
  p.magicTree=new nro.models.npc.MagicTree(p,(byte)3,(byte)5,77,false,88);p.playerTask.taskMain=new nro.models.task.TaskMain();p.playerTask.taskMain.id=7;p.playerTask.taskMain.subTasks.add(new nro.models.task.SubTaskMain());
  Pet pet=new Pet(p);p.pet=pet;pet.name="$old";pet.typePet=3;pet.gender=2;pet.nPoint.hpg=100;pet.nPoint.mpg=200;pet.nPoint.hp=77;pet.nPoint.mp=66;pet.nPoint.power=60000000000L;pet.nPoint.limitPower=7;pet.nPoint.defg=19;pet.playerSkill.skills.add(new Skill());pet.playerSkill.skills.get(0).skillId=-1;pet.inventory.itemsBody.add(new Item());
  Map<String,String>b=snapshot(p);check(b.get("id").equals("999")&&b.get("gender").equals("2"),"runtime identity");check(b.get("_hp_max").equals("1500"),"derived max available for heal");
  JSONObject payload=new JSONObject();payload.put("pet_tiem_nang",1234);Map<String,String>changed=LiveSyncPlayerPatch.patch(b,"UPDATE_PET",payload,2);Skill skill=pet.playerSkill.skills.get(0);var body=pet.inventory.itemsBody;publish(p,changed);
  check(p.pet==pet&&pet.playerSkill.skills.get(0)==skill&&pet.inventory.itemsBody==body,"existing pet/skills/body references preserved");check(pet.name.equals("$old")&&pet.nPoint.tiemNang==1234&&pet.nPoint.limitPower==7&&pet.nPoint.defg==19,"stats patch preserves saved name/combat stats");
  payload=new JSONObject();payload.put("pet_type",4);publish(p,LiveSyncPlayerPatch.patch(snapshot(p),"UPDATE_PET",payload,2));check(p.pet==pet&&pet.typePet==4&&pet.name.equals("$Kid Jiren")&&pet.playerSkill.skills.get(0)==skill,"type change publishes default name without replacing pet or skills");
  var bag=p.inventory.itemsBag;publish(p,Map.of("data_inventory","[40,2,3,50,0]"));check(p.inventory.gold==40&&p.inventory.coupon==50&&p.inventory.itemsBag==bag,"currency publishes only currency");
  publish(p,Map.of("data_magic_tree","[10,5,0,77,88]"));check(p.magicTree.level==10&&p.magicTree.lastTimeHarvest==77,"tree publish preserves timer");
  long gold=p.inventory.gold;try{publish(p,Map.of("data_inventory","[50,2,3,50,0]","items_bag","BROKEN"));throw new AssertionError("malformed staged data accepted");}catch(IllegalArgumentException expected){}check(p.inventory.gold==gold,"decode all before mutating runtime");
  System.out.println("PASS runtime snapshots, committed field publication, pet reference preservation, atomic decoding");
 }
}
