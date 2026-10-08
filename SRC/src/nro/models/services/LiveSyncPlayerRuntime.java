package nro.models.services;

import java.util.*;
import nro.models.item.Item;
import nro.models.player.*;
import nro.models.skill.Skill;
import nro.models.task.TaskMain;
import nro.models.server.Manager;
import nro.models.utils.SkillUtil;
import nro.models.map.service.ChangeMapService;
import org.json.simple.JSONArray;

/** SQL-format snapshots and publication of already committed changes. Never writes SQL. */
public final class LiveSyncPlayerRuntime {
    private LiveSyncPlayerRuntime() {}
    private static JSONArray values(Object... values){JSONArray a=new JSONArray();Collections.addAll(a,values);return a;}
    private static long value(JSONArray a,int i){return Long.parseLong(String.valueOf(a.get(i)));}
    private static JSONArray points(NPoint n,boolean pet){
        JSONArray a=values(n.limitPower,n.power,n.tiemNang,n.stamina,n.maxStamina,n.hpg,n.mpg,n.dameg,n.defg,n.critg);
        if(!pet){a.add(n.critdragon);a.add(0);}a.add(n.hp);a.add(n.mp);return a;
    }
    private static String skills(List<Skill> skills){JSONArray list=new JSONArray();for(Skill s:skills)list.add(values(s.skillId==-1?-1:s.template.id,s.skillId==-1?0:s.point,s.skillId==-1?0:s.lastTimeUseThisSkill,s.skillId==-1?0:s.currLevel).toJSONString());return list.toJSONString();}
    private static String pet(Player p){
        if(p.pet==null)return "[]";Pet pet=p.pet;
        int remaining=(int)Math.max(0,Fusion.TIME_FUSION-(System.currentTimeMillis()-p.fusion.lastTimeFusion));
        return values(values(pet.typePet,pet.gender,pet.name,p.fusion.typeFusion,remaining,pet.status).toJSONString(),points(pet.nPoint,true).toJSONString(),LiveSyncPlayerPatch.encodeItemList(pet.inventory.itemsBody),skills(pet.playerSkill.skills)).toJSONString();
    }
    public static Map<String,String> snapshot(Player p){synchronized(p){
        Map<String,String> out=new LinkedHashMap<>();out.put("id",String.valueOf(p.id));out.put("account_id",String.valueOf(p.getSession()==null?0:p.getSession().userId));out.put("gender",String.valueOf(p.gender));
        out.put("data_inventory",values(p.inventory.gold,p.inventory.gem,p.inventory.ruby,p.inventory.coupon,p.inventory.event).toJSONString());
        out.put("data_point",points(p.nPoint,false).toJSONString());out.put("_hp_max",String.valueOf(p.nPoint.hpMax));out.put("_mp_max",String.valueOf(p.nPoint.mpMax));
        out.put("data_location",values(p.zone==null?p.mapIdBeforeLogout:p.zone.map.mapId,p.location.x,p.location.y).toJSONString());
        out.put("items_bag",LiveSyncPlayerPatch.encodeItemList(p.inventory.itemsBag));out.put("items_box",LiveSyncPlayerPatch.encodeItemList(p.inventory.itemsBox));out.put("items_body",LiveSyncPlayerPatch.encodeItemList(p.inventory.itemsBody));
        if(p.magicTree!=null)out.put("data_magic_tree",values(p.magicTree.level,p.magicTree.currPeas,p.magicTree.isUpgrade?1:0,p.magicTree.lastTimeHarvest,p.magicTree.lastTimeUpgrade).toJSONString());
        TaskMain task=p.playerTask.taskMain;if(task!=null)out.put("data_task",values(task.id,task.index,task.subTasks.isEmpty()?0:task.subTasks.get(task.index).count,task.lastTime).toJSONString());
        out.put("skills",skills(p.playerSkill.skills));out.put("pet",pet(p));return out;
    }}
    private static void assignPoints(NPoint n,JSONArray a,boolean pet){
        n.limitPower=(byte)value(a,0);n.power=value(a,1);n.tiemNang=value(a,2);n.stamina=(short)value(a,3);n.maxStamina=(short)value(a,4);
        n.hpg=(int)value(a,5);n.mpg=(int)value(a,6);n.dameg=(int)value(a,7);n.defg=(int)value(a,8);n.critg=(int)value(a,9);
        if(!pet)n.critdragon=(int)value(a,10);n.hp=(int)value(a,pet?10:12);n.mp=(int)value(a,pet?11:13);
    }
    private static List<Skill> decodeSkills(String raw,List<Skill> previous){
        JSONArray list=LiveSyncPlayerPatch.array(raw);List<Skill> result=new ArrayList<>();
        for(int i=0;i<list.size();i++){JSONArray a=LiveSyncPlayerPatch.array(list.get(i));int id=(int)value(a,0);int level=(int)value(a,1);Skill old=i<previous.size()?previous.get(i):null;
            if(old!=null&&(old.skillId==-1?-1:old.template.id)==id&&(old.skillId==-1?0:old.point)==level
                    &&(old.skillId==-1?0:old.lastTimeUseThisSkill)==value(a,2)&&(old.skillId==-1?0:old.currLevel)==value(a,3)){result.add(old);continue;}
            Skill skill=id==-1?SkillUtil.createEmptySkill():level==0?SkillUtil.createSkillLevel0(id):SkillUtil.createSkill(id,level);
            if(skill==null)throw new IllegalArgumentException("Invalid staged skill");skill.lastTimeUseThisSkill=value(a,2);skill.currLevel=(short)value(a,3);if(id==Skill.KAMEJOKO||id==Skill.MASENKO||id==Skill.ANTOMIC)skill.coolDown=1000;result.add(skill);
        }return result;
    }
    private static void numericArray(JSONArray a,int length){if(a.size()<length)throw new IllegalArgumentException("Invalid committed player field");for(int i=0;i<length;i++)value(a,i);}
    private static void safe(Runnable notify){try{notify.run();}catch(RuntimeException e){System.err.println("[LiveSync] Packet refresh failed: "+e.getClass().getSimpleName());}}
    public static void publish(Player p,Map<String,String> changed){synchronized(p){
        // Decode all fallible input before modifying any resource. Preserve untouched object references.
        Map<String,JSONArray> arrays=new HashMap<>();Map<String,List<Item>> items=new HashMap<>();
        for(String key:List.of("data_inventory","data_point","data_location","data_task","data_magic_tree"))if(changed.containsKey(key))arrays.put(key,LiveSyncPlayerPatch.array(changed.get(key)));
        for(String key:List.of("items_bag","items_box","items_body"))if(changed.containsKey(key))items.put(key,LiveSyncPlayerPatch.decodeItemList(changed.get(key)));
        if(arrays.containsKey("data_inventory"))numericArray(arrays.get("data_inventory"),5);
        if(arrays.containsKey("data_point"))numericArray(arrays.get("data_point"),14);
        if(arrays.containsKey("data_location"))numericArray(arrays.get("data_location"),3);
        if(arrays.containsKey("data_magic_tree"))numericArray(arrays.get("data_magic_tree"),5);
        TaskMain nextTask=null;if(arrays.containsKey("data_task")){JSONArray a=arrays.get("data_task");numericArray(a,4);int id=(int)value(a,0);
            if(Manager.TASKS.stream().noneMatch(t->t.id==id))throw new IllegalArgumentException("Invalid committed task");
            nextTask=TaskService.gI().getTaskMainById(p,id);int index=(int)value(a,1);if(index<0||index>=nextTask.subTasks.size())throw new IllegalArgumentException("Invalid committed task progress");
            nextTask.index=index;nextTask.subTasks.get(index).count=(short)value(a,2);nextTask.lastTime=value(a,3);
        }
        JSONArray petData=null,petInfo=null,petPoints=null;List<Item> petItems=null;List<Skill> petSkills=null;Pet nextPet=p.pet;
        if(changed.containsKey("pet")){petData=LiveSyncPlayerPatch.array(changed.get("pet"));if(petData.size()<4)throw new IllegalArgumentException("Invalid committed pet");petInfo=LiveSyncPlayerPatch.array(petData.get(0));petPoints=LiveSyncPlayerPatch.array(petData.get(1));numericArray(petPoints,12);
            for(int i:new int[]{0,1,3,4,5})value(petInfo,i);
            if(nextPet==null){nextPet=new Pet(p);nextPet.id=-p.id;}
            if(!petData.get(2).equals(LiveSyncPlayerPatch.encodeItemList(nextPet.inventory.itemsBody)))petItems=LiveSyncPlayerPatch.decodeItemList(String.valueOf(petData.get(2)));
            petSkills=decodeSkills(String.valueOf(petData.get(3)),nextPet.playerSkill.skills);
        }
        boolean pointsChanged=arrays.containsKey("data_point")||items.containsKey("items_body")||petData!=null;
        if(arrays.containsKey("data_inventory")){JSONArray a=arrays.get("data_inventory");p.inventory.gold=value(a,0);p.inventory.gem=(int)value(a,1);p.inventory.ruby=(int)value(a,2);p.inventory.coupon=(int)value(a,3);p.inventory.event=(int)value(a,4);}
        if(arrays.containsKey("data_point"))assignPoints(p.nPoint,arrays.get("data_point"),false);
        if(items.containsKey("items_bag"))p.inventory.itemsBag=items.get("items_bag");if(items.containsKey("items_box"))p.inventory.itemsBox=items.get("items_box");if(items.containsKey("items_body"))p.inventory.itemsBody=items.get("items_body");
        if(arrays.containsKey("data_magic_tree")){JSONArray a=arrays.get("data_magic_tree");if(p.magicTree==null)p.magicTree=new nro.models.npc.MagicTree(p,(byte)value(a,0),(byte)value(a,1),value(a,3),value(a,2)!=0,value(a,4));else{p.magicTree.level=(byte)value(a,0);p.magicTree.currPeas=(int)value(a,1);p.magicTree.isUpgrade=value(a,2)!=0;p.magicTree.lastTimeHarvest=value(a,3);p.magicTree.lastTimeUpgrade=value(a,4);}}
        if(nextTask!=null)p.playerTask.taskMain=nextTask;
        if(petData!=null){Pet pet=nextPet;pet.typePet=(byte)value(petInfo,0);pet.type=pet.typePet;pet.gender=(byte)value(petInfo,1);pet.name=String.valueOf(petInfo.get(2));pet.status=(byte)value(petInfo,5);assignPoints(pet.nPoint,petPoints,true);
            if(petItems!=null)pet.inventory.itemsBody=petItems;
            if(!pet.playerSkill.skills.equals(petSkills)){Skill selected=pet.playerSkill.skillSelect;int selectedIndex=pet.playerSkill.skills.indexOf(selected);pet.playerSkill.skills=petSkills;if(selectedIndex>=0&&selectedIndex<petSkills.size())pet.playerSkill.skillSelect=petSkills.get(selectedIndex);}
            if(p.pet==null){p.fusion.typeFusion=(byte)value(petInfo,3);p.fusion.lastTimeFusion=System.currentTimeMillis()-(Fusion.TIME_FUSION-value(petInfo,4));p.pet=pet;}
            pet.nPoint.calPoint();
        }
        if(pointsChanged)p.nPoint.calPoint();
        if(arrays.containsKey("data_location")){JSONArray a=arrays.get("data_location");p.mapIdBeforeLogout=(int)value(a,0);if(p.zone!=null)ChangeMapService.gI().changeMap(p,(int)value(a,0),-1,(int)value(a,1),(int)value(a,2));else{p.location.x=(int)value(a,1);p.location.y=(int)value(a,2);}}
        if(p.getSession()!=null){
            if(arrays.containsKey("data_inventory"))safe(()->Service.gI().sendMoney(p));
            if(items.containsKey("items_bag"))safe(()->InventoryService.gI().sendItemBags(p));if(items.containsKey("items_box"))safe(()->InventoryService.gI().sendItemBox(p));
            if(items.containsKey("items_body")){safe(()->InventoryService.gI().sendItemBody(p));safe(()->Service.gI().Send_Caitrang(p));}
            if(pointsChanged){safe(()->Service.gI().point(p));safe(()->PlayerService.gI().sendInfoHpMp(p));safe(()->Service.gI().Send_Info_NV(p));}
            if(petData!=null){safe(()->Service.gI().sendHavePet(p));safe(()->Service.gI().showInfoPet(p));safe(()->Service.gI().Send_Caitrang(p.pet));}
            if(nextTask!=null)safe(()->TaskService.gI().sendTaskMain(p));if(arrays.containsKey("data_magic_tree"))safe(()->p.magicTree.loadMagicTree());
        }
    }}
}
