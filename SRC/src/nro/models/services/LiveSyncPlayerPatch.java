package nro.models.services;

import java.util.*;
import nro.models.item.Item;
import nro.models.item.ItemDefaultOptions;
import nro.models.item.RubyItemRules;
import nro.models.item.ItemStackRules;
import nro.models.player.Inventory;
import nro.models.player.Player;
import nro.models.player.Pet;
import nro.models.player.PetSkillRules;
import nro.models.server.Manager;
import nro.models.skill.Skill;
import org.json.simple.*;

/** Detached SQL-format staging. No database, sessions, packets or mutation of the input. */
public final class LiveSyncPlayerPatch {
    private LiveSyncPlayerPatch() {}
    public static JSONArray array(Object value) {
        Object decoded=value instanceof String ? JSONValue.parse((String)value) : value;
        if (!(decoded instanceof JSONArray)) throw new IllegalArgumentException("Invalid saved JSON array");
        return (JSONArray)JSONValue.parse(((JSONArray)decoded).toJSONString());
    }
    private static long number(Object value,long min,long max) {
        final long n;
        try { n=Long.parseLong(String.valueOf(value)); }
        catch(RuntimeException e) { throw new IllegalArgumentException("Expected integer",e); }
        if(n<min||n>max)throw new IllegalArgumentException("Value outside allowed range");
        return n;
    }
    private static void set(JSONArray a,int index,JSONObject p,String key,long min,long max) {
        if(p.containsKey(key))a.set(index,number(p.get(key),min,max));
    }
    private static JSONArray values(Object... values){JSONArray a=new JSONArray();Collections.addAll(a,values);return a;}
    public static List<Item> decodeItemList(String json) {
        List<Item> list=new ArrayList<>();
        for(Object raw:array(json)) {
            JSONArray slot=array(raw);if(slot.size()<4)throw new IllegalArgumentException("Invalid saved item slot");
            int id=(int)number(slot.get(0),-1,Short.MAX_VALUE);Item item=id==-1?ItemService.gI().createItemNull():ItemService.gI().createItemForRestore((short)id,(int)number(slot.get(1),1,Integer.MAX_VALUE));
            if(id!=-1&&!item.isNotNullItem())throw new IllegalArgumentException("Unknown saved item template");
            item.itemOptions.clear();
            for(Object opt:array(slot.get(2))) {
                JSONArray pair=array(opt);if(pair.size()!=2)throw new IllegalArgumentException("Invalid item option");
                Item.ItemOption io=new Item.ItemOption((int)number(pair.get(0),0,Integer.MAX_VALUE),(int)number(pair.get(1),0,Integer.MAX_VALUE));
                if(io.optionTemplate==null)throw new IllegalArgumentException("Unknown item option");item.itemOptions.add(io);
            }
            item.createTime=number(slot.get(3),0,Long.MAX_VALUE);list.add(item);
        }
        return list;
    }
    public static String encodeItemList(List<Item> items) {
        JSONArray list=new JSONArray();for(Item item:items){JSONArray options=new JSONArray();
            if(item.isNotNullItem())for(Item.ItemOption option:item.itemOptions)options.add(values(option.optionTemplate.id,option.param).toJSONString());
            list.add(values(item.isNotNullItem()?item.template.id:-1,item.isNotNullItem()?item.quantity:0,options.toJSONString(),item.createTime).toJSONString());
        }return list.toJSONString();
    }
    private static String target(JSONObject p) {
        String t=String.valueOf(p.getOrDefault("target",p.getOrDefault("container","bag")));
        if(!Set.of("bag","box","body").contains(t))throw new IllegalArgumentException("Invalid item container");return "items_"+t;
    }
    private static void gift(Map<String,String>b,Map<String,String>out,JSONObject p) {
        String column=target(p);if(column.equals("items_body"))throw new IllegalArgumentException("Cannot gift into equipment slots");
        List<Item> slots=decodeItemList(out.getOrDefault(column,b.get(column)));
        int id=(int)number(p.get("item_id"),0,Short.MAX_VALUE),qty=(int)number(p.getOrDefault("quantity",1),1,Integer.MAX_VALUE);
        Item item=ItemService.gI().createNewItem((short)id,qty);if(!item.isNotNullItem())throw new IllegalArgumentException("Unknown item template");
        if(p.containsKey("options")){item.itemOptions.clear();for(Object raw:array(p.get("options"))){JSONArray pair=array(raw);if(pair.size()!=2)throw new IllegalArgumentException("Invalid option pair");Item.ItemOption io=new Item.ItemOption((int)number(pair.get(0),0,Integer.MAX_VALUE),(int)number(pair.get(1),0,Integer.MAX_VALUE));if(io.optionTemplate==null)throw new IllegalArgumentException("Unknown item option");item.itemOptions.add(io);}}
        ItemDefaultOptions.complete(item);RubyItemRules.normalize(item);if(item.itemOptions.isEmpty())item.itemOptions.add(new Item.ItemOption(73,0));
        if(!ItemStackRules.stackable(item.template)&&qty!=1)throw new IllegalArgumentException("Nonstackable items require quantity one");
        if(!InventoryService.gI().canAddItemList(slots,item)||!InventoryService.gI().addItemList(slots,item))throw new IllegalArgumentException("Inventory capacity insufficient");
        out.put(column,encodeItemList(slots));
    }
    public static Map<String,String> patch(Map<String,String>b,String type,JSONObject p,int gender)throws Exception {
        Objects.requireNonNull(p,"payload");Map<String,String>out=new LinkedHashMap<>();
        switch(type){
        case "UPDATE_STATS": {
            JSONArray point=array(b.get("data_point"));if(point.size()<14)throw new IllegalArgumentException("Invalid player points");
            set(point,0,p,"limit_power",0,9);set(point,1,p,"power",0,90_010_000_000L);set(point,2,p,"tiem_nang",0,Long.MAX_VALUE);
            for(int i=0;i<4;i++)set(point,5+i,p,new String[]{"hpg","mpg","dameg","defg"}[i],i<2?1:0,Integer.MAX_VALUE);
            set(point,9,p,"critg",0,100);out.put("data_point",point.toJSONString());
            JSONArray money=array(b.get("data_inventory"));set(money,0,p,"gold",0,Inventory.LIMIT_GOLD);set(money,1,p,"gem",0,Integer.MAX_VALUE);set(money,2,p,"ruby",0,Integer.MAX_VALUE);out.put("data_inventory",money.toJSONString());break;
        }
        case "TOPUP_PLAYER": {
            JSONArray money=array(b.get("data_inventory"));String[] keys={"add_gold","add_gem","add_ruby"};
            for(int i=0;i<keys.length;i++)if(p.containsKey(keys[i])){long cap=i==0?Inventory.LIMIT_GOLD:Integer.MAX_VALUE;long delta=number(p.get(keys[i]),Long.MIN_VALUE,Long.MAX_VALUE);long next;try{next=Math.addExact(number(money.get(i),0,cap),delta);}catch(ArithmeticException e){throw new IllegalArgumentException("Currency overflow",e);}money.set(i,number(next,0,cap));}
            out.put("data_inventory",money.toJSONString());if(p.containsKey("add_thoi_vang")){long qty=number(p.get("add_thoi_vang"),0,Integer.MAX_VALUE);if(qty>0){JSONObject gift=new JSONObject();gift.put("item_id",457);gift.put("quantity",qty);gift(b,out,gift);}}break;
        }
        case "GIFT_ITEM":gift(b,out,p);break;
        case "DELETE_ITEM":case "UPDATE_ITEM_QTY": {
            String column=target(p);JSONArray slots=array(b.get(column));int index=(int)number(p.getOrDefault("slot_index",p.getOrDefault("slot",p.get("index"))),0,slots.size()-1);JSONArray actual=array(slots.get(index));
            if(!p.containsKey("expected_item")||!actual.equals(array(p.get("expected_item"))))throw new IllegalArgumentException("Item slot conflict; refresh before retry");
            if(number(actual.get(0),-1,Short.MAX_VALUE)==-1)throw new IllegalArgumentException("Empty item slot");
            if(type.equals("DELETE_ITEM"))slots.set(index,values(-1,0,"[]",actual.get(3)).toJSONString());
            else {int qty=(int)number(p.getOrDefault("quantity",p.get("new_quantity")),1,InventoryService.MAX_STACK_QUANTITY);var template=ItemService.gI().getTemplate(Integer.parseInt(String.valueOf(actual.get(0))));if(template==null||!ItemStackRules.stackable(template)&&qty!=1)throw new IllegalArgumentException("Invalid quantity for item template");actual.set(1,qty);slots.set(index,actual.toJSONString());}
            out.put(column,slots.toJSONString());break;
        }
        case "UPDATE_PET":out.put("pet",pet(b.get("pet"),p,gender));break;
        case "WARP_HOME":number(gender,0,2);out.put("data_location",values(21+gender,300,336).toJSONString());break;
        case "HEAL_FULL": {
            JSONArray point=array(b.get("data_point"));
            // Login computes equipment/buff maxima and clamps these values via setHp/setMp.
            long hp=number(b.getOrDefault("_hp_max",String.valueOf(Integer.MAX_VALUE)),1,Integer.MAX_VALUE);
            long mp=number(b.getOrDefault("_mp_max",String.valueOf(Integer.MAX_VALUE)),1,Integer.MAX_VALUE);
            point.set(12,hp);point.set(13,mp);out.put("data_point",point.toJSONString());break;
        }
        case "CHANGE_TASK": {
            int id=(int)number(p.get("task_id"),0,Short.MAX_VALUE);
            if(Manager.TASKS.stream().noneMatch(t->t.id==id))throw new IllegalArgumentException("Unknown task");
            if(Manager.TASKS.stream().filter(t->t.id==id).anyMatch(t->t.subTasks==null||t.subTasks.isEmpty()))throw new IllegalArgumentException("Task has no valid subtasks");
            out.put("data_task",values(id,0,0,0).toJSONString());break;
        }
        case "UPGRADE_MAGIC_TREE": {
            JSONArray tree=array(b.get("data_magic_tree"));if(!p.containsKey("level")&&!p.containsKey("tree_level"))tree.set(0,10);set(tree,0,p,"level",1,10);set(tree,0,p,"tree_level",1,10);tree.set(2,0);out.put("data_magic_tree",tree.toJSONString());break;
        }
        default:throw new IllegalArgumentException("Unsupported staged player command: "+type);
        }
        out.entrySet().removeIf(e->Objects.equals(e.getValue(),b.get(e.getKey())));return out;
    }
    private static String pet(String saved,JSONObject p,int gender) {
        if(p.containsKey("pet_name"))throw new IllegalArgumentException("Admin cannot edit pet name");
        JSONArray pet=array(saved);if(pet.isEmpty()){
            int type=(int)number(p.getOrDefault("pet_type",0),0,4);Player master=new Player();master.gender=(byte)number(gender,0,2);
            Pet created=PetService.gI().createNewPet(master,type==1,type==2,type==3,type==4,(byte)number(p.getOrDefault("pet_gender",gender),0,2));
            JSONArray skills=new JSONArray();for(Skill skill:created.playerSkill.skills)skills.add(values(skill.skillId==-1?-1:skill.template.id,skill.skillId==-1?0:skill.point,0,0).toJSONString());
            pet=values(values(created.typePet,created.gender,created.name,0,0,created.status).toJSONString(),values(created.nPoint.limitPower,created.nPoint.power,created.nPoint.tiemNang,created.nPoint.stamina,created.nPoint.maxStamina,created.nPoint.hpg,created.nPoint.mpg,created.nPoint.dameg,created.nPoint.defg,created.nPoint.critg,created.nPoint.hp,created.nPoint.mp).toJSONString(),encodeItemList(created.inventory.itemsBody),skills.toJSONString());
        }
        if(pet.size()<4)throw new IllegalArgumentException("Invalid pet data");JSONArray info=array(pet.get(0)),point=array(pet.get(1));
        set(info,0,p,"pet_type",0,4);set(info,1,p,"pet_gender",0,2);set(info,5,p,"pet_status",0,4);
        if(p.containsKey("pet_type"))info.set(2,PetService.defaultPetName((int)number(info.get(0),0,4)));
        set(point,0,p,"pet_limit_power",0,9);set(point,1,p,"pet_power",0,90_010_000_000L);set(point,2,p,"pet_tiem_nang",0,Long.MAX_VALUE);
        for(int i=0;i<4;i++)set(point,5+i,p,new String[]{"pet_hpg","pet_mpg","pet_dameg","pet_defg"}[i],i<2?1:0,Integer.MAX_VALUE);set(point,9,p,"pet_critg",0,100);
        JSONArray skills=array(pet.get(3));boolean skillChanged=false;int kind=(int)number(info.get(0),0,4),planet=(int)number(info.get(1),0,2);int visible=kind>=2?5:4;
        if((p.containsKey("pet_type")||p.containsKey("pet_power")||p.containsKey("pet_limit_power"))&&kind>=2&&number(point.get(1),0,Long.MAX_VALUE)>=40_000_000_000L&&number(point.get(0),0,9)<4)point.set(0,4);
        if(p.containsKey("pet_type")&&kind>=2){JSONArray body=array(pet.get(2));while(body.size()<9)body.add(values(-1,0,"[]",System.currentTimeMillis()).toJSONString());pet.set(2,body.toJSONString());}
        for(int i=0;i<7;i++){String key="pet_skill_"+(i+1);if(!p.containsKey(key))continue;if(i>=visible)throw new IllegalArgumentException("Hidden pet skill slot");int id=(int)number(p.get(key),-1,127);JSONArray skill=i<skills.size()?array(skills.get(i)):values(-1,0,0,0);
            if(id!=-1){boolean fifth=id==Skill.SUPER_KAME||id==Skill.LIEN_HOAN_CHUONG||id==Skill.MA_PHONG_BA;if(fifth!=(i==4)||i==4&&number(point.get(1),0,Long.MAX_VALUE)<PetSkillRules.FIFTH_SKILL_POWER)throw new IllegalArgumentException("Skill not valid for pet slot/power");
                boolean found=false;for(var nclass:Manager.NCLASS){var template=nclass.getSkillTemplate(id);if(template!=null&&!template.skillss.isEmpty()){found=true;break;}}if(!found)throw new IllegalArgumentException("Unknown pet skill template");
                if(i==0&&!Set.of(0,2,4).contains(id)||i==1&&!Set.of(1,3,5).contains(id)||i==2&&!Set.of(6,8,9).contains(id)||i==3&&!Set.of(12,13,19).contains(id)||i==4&&id!=(planet==0?Skill.SUPER_KAME:planet==1?Skill.MA_PHONG_BA:Skill.LIEN_HOAN_CHUONG))throw new IllegalArgumentException("Skill not valid for pet slot/planet");
            }
            if(number(skill.get(0),-1,127)!=id)skill=values(id,id==-1?0:1,0,0);
            while(skills.size()<=i)skills.add(values(-1,0,0,0).toJSONString());skills.set(i,skill.toJSONString());skillChanged=true;
        }
        pet.set(0,info.toJSONString());pet.set(1,point.toJSONString());if(skillChanged)pet.set(3,skills.toJSONString());return pet.toJSONString();
    }
}
