package nro.models.player;

import java.lang.reflect.*;
import java.util.*;
import nro.models.item.Item;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.*;
import nro.models.services_func.UseItem;
import nro.models.task.TaskMain;
import nro.models.intrinsic.Intrinsic;
import nro.models.mob.Mob;
import nro.models.utils.Util;

public class DragonPetStatsTest {
    private static Class<?> catalog;
    private static final int[][][] EXPECTED = {
        {{77,16},{50,16},{103,16},{236,20},{30,0}},
        {{50,18},{5,7},{14,5},{30,0}},
        {{50,18},{94,5},{108,7},{30,0}},
        {{77,14},{5,7},{14,5},{30,0}},
        {{77,18},{94,5},{108,7},{30,0}},
        {{77,22},{50,22},{94,8},{5,11},{14,8},{30,0}},
        {{77,22},{50,22},{94,8},{5,11},{14,8},{106,0},{30,0}}
    };
    private static void check(boolean ok,String msg){if(!ok)throw new AssertionError(msg);}
    private static void replace(Class<?> c,String n,Object v)throws Exception {
        Field f=c.getDeclaredField(n);f.setAccessible(true);f.set(null,v);
    }
    private static Item reward(int id,int permanentRoll,int dayRoll)throws Exception {
        Item it=ItemService.gI().createNewItem((short)id);
        Random rng=new Random(){int calls;
            @Override public int nextInt(int bound){
                check(bound==(calls==0?100:30),"Correct duration/permanent bounds");
                return calls++==0?permanentRoll:dayRoll;
            }
        };
        catalog.getMethod("applyRewardOptions",Item.class,Random.class).invoke(null,it,rng);
        return it;
    }
    private static Map<Integer,Integer> options(Item it){
        Map<Integer,Integer> result=new LinkedHashMap<>();
        for(Item.ItemOption op:it.itemOptions) {
            check(!result.containsKey(op.optionTemplate.id),"No duplicate options");
            result.put(op.optionTemplate.id,op.param);
        }
        return result;
    }
    private static Map<Integer,Integer> expected(int id){
        Map<Integer,Integer> result=new LinkedHashMap<>();
        for(int[] pair:EXPECTED[id-1765])result.put(pair[0],pair[1]);
        return result;
    }
    public static void main(String[] args)throws Exception {
        try{catalog=Class.forName("nro.models.item.DragonPetCatalog");}
        catch(ClassNotFoundException e){throw new AssertionError("Fixed dragon profiles have not been implemented",e);}
        for(int id=0;id<=250;id++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"Option #",0));
        for(int id=0;id<=1900;id++) {
            Template.ItemTemplate t=new Template.ItemTemplate();t.id=(short)id;t.type=27;t.gender=3;t.name="Fixture";
            Manager.ITEM_TEMPLATES.add(t);
        }
        replace(Service.class,"instance",new Service(){@Override public void sendThongBao(Player p,String s){} @Override public void point(Player p){}});
        switch(args[0]) {
            case "catalog" -> catalog();
            case "effects" -> effects();
            case "combat" -> combat();
            case "loot" -> loot();
            case "egg" -> egg();
            default -> throw new AssertionError(args[0]);
        }
        System.out.println("PASS dragon stats "+args[0]);
    }
    private static void catalog()throws Exception {
        for(int id=1765;id<=1771;id++) {
            check(options(reward(id,0,0)).equals(expected(id)),"Exact permanent stats pet "+id);
            check((int)catalog.getMethod("stars",int.class).invoke(null,id)==1772-id,"Star-to-icon mapping");
            for(int day=0;day<30;day++) {
                Map<Integer,Integer> want=expected(id);want.put(93,day+1);
                check(options(reward(id,99,day)).equals(want),"Every duration 1-30 for pet "+id);
            }
            int permanent=0;
            for(int roll=0;roll<100;roll++)if(!options(reward(id,roll,0)).containsKey(93))permanent++;
            check(permanent==10,"Exactly 10/100 permanent outcomes");
        }
    }
    private static Player owner(){
        Player p=new Player();p.isPlayer=true;p.playerIntrinsic.intrinsic=new Intrinsic();
        p.playerTask.taskMain=new TaskMain();p.playerTask.taskMain.id=-1;
        p.nPoint.hpg=10000;p.nPoint.mpg=10000;p.nPoint.dameg=1000;p.nPoint.limitPower=9;
        for(int i=0;i<12;i++)p.inventory.itemsBody.add(new Item());
        return p;
    }
    private static void effects()throws Exception {
        Field luck=NPoint.class.getField("tlLuck");
        for(int id=1765;id<=1771;id++) {
            Player p=owner();p.nPoint.calPoint();int hp=p.nPoint.hpMax,ki=p.nPoint.mpMax,sd=p.nPoint.dame;
            p.inventory.itemsBody.set(7,reward(id,0,0));Map<Integer,Integer> spec=expected(id);
            for(int repeat=0;repeat<3;repeat++) {
                p.nPoint.calPoint();
                check(p.nPoint.hpMax==hp*(100+spec.getOrDefault(77,0))/100,"HP pet "+id);
                check(p.nPoint.mpMax==ki*(100+spec.getOrDefault(103,0))/100,"KI pet "+id);
                check(p.nPoint.dame==sd*(100+spec.getOrDefault(50,0))/100,"Damage pet "+id);
                check(p.nPoint.crit==spec.getOrDefault(14,0),"Critical chance");
                check(p.nPoint.tlSDCM==spec.getOrDefault(5,0),"Critical damage");
                check(p.nPoint.tlGiap==spec.getOrDefault(94,0),"Damage reduction");
                check(p.nPoint.tlNeDon==spec.getOrDefault(108,0),"Dodge");
                check(p.nPoint.isKhongLanh==spec.containsKey(106),"Cold protection only 1-star");
                check(luck.getInt(p.nPoint)==spec.getOrDefault(236,0),"Luck aggregates without stacking");
            }
            p.inventory.itemsBody.set(7,new Item());p.nPoint.calPoint();
            check(p.nPoint.hpMax==hp&&p.nPoint.mpMax==ki&&p.nPoint.dame==sd,"Removing pet restores stats");
            check(luck.getInt(p.nPoint)==0&&p.nPoint.tlGiap==0&&p.nPoint.tlNeDon==0&&!p.nPoint.isKhongLanh,"Removing pet clears effects");
        }
    }
    private static void loot()throws Exception {
        Player p=owner();p.inventory.itemsBody.set(7,reward(1765,0,0));p.nPoint.calPoint();
        Method drop=Mob.class.getDeclaredMethod("isLuckyDrop",Player.class,double.class,long.class);drop.setAccessible(true);
        Field f=Util.class.getDeclaredField("rand");f.setAccessible(true);Random rng=(Random)f.get(null);
        Mob m=new Mob();boolean luckChanged=false;
        for(int gohan:new int[]{0,5})for(int seed=0;seed<500;seed++) {
            p.setClothes.gohan=(byte)gohan;
            rng.setSeed(seed);boolean actual=(boolean)drop.invoke(m,p,10.0,100L);
            rng.setSeed(seed);boolean want=Util.isTrue(gohan==5?300000:120000,1000000);
            check(actual==want,"Luck multiplier with Gohan, seed "+seed);
            rng.setSeed(seed);boolean baseline=Util.isTrue(10,100);if(actual!=baseline)luckChanged=true;
        }
        check(luckChanged,"Luck affects actual rolls");
        p.inventory.itemsBody.set(7,new Item());p.nPoint.calPoint();p.setClothes.gohan=0;
        for(int seed=0;seed<100;seed++) {
            rng.setSeed(seed);boolean actual=(boolean)drop.invoke(m,p,10.0,100L);
            rng.setSeed(seed);check(actual==Util.isTrue(10,100),"No-luck rolls preserved");
        }
    }
    private static void combat()throws Exception {
        Class<?> unsafeClass=Class.forName("sun.misc.Unsafe");
        Field sf=unsafeClass.getDeclaredField("theUnsafe");sf.setAccessible(true);
        nro.models.map.Map map=(nro.models.map.Map)unsafeClass.getMethod("allocateInstance",Class.class)
            .invoke(sf.get(null),nro.models.map.Map.class);map.mapId=6;
        Field rf=Util.class.getDeclaredField("rand");rf.setAccessible(true);Random rng=(Random)rf.get(null);
        for(int id:new int[]{1769,1770,1771}) {
            Player p=owner();p.zone=new nro.models.map.Zone(map,0,20);
            p.inventory.itemsBody.set(7,reward(id,0,0));p.nPoint.calPoint();
            for(int seed=0;seed<100;seed++) {
                p.nPoint.hp=p.nPoint.hpMax;
                rng.setSeed(seed);boolean dodge=Util.isTrue(id==1769?7:0,100);
                rng.setSeed(seed);int actual=p.injured(null,1000,false,true);
                check(actual==(dodge?0:id==1769?950:920),"Actual damage reduction/dodge pet "+id+" seed "+seed);
            }
        }
        map.mapId=105;
        Player cold=owner();cold.zone=new nro.models.map.Zone(map,0,20);
        cold.inventory.itemsBody.set(7,reward(1770,0,0));cold.nPoint.calPoint();
        check(cold.nPoint.hpMax==6100&&cold.nPoint.dame==610,"2-star suffers cold penalty");
        cold.inventory.itemsBody.set(7,reward(1771,0,0));cold.nPoint.calPoint();
        check(cold.nPoint.hpMax==12200&&cold.nPoint.dame==1220,"1-star cold protection works");
    }
    private static void egg()throws Exception {
        List<Item> grants=new ArrayList<>();int[] consumed={0};
        replace(InventoryService.class,"I",new InventoryService(){
            @Override public int getCountEmptyBag(Player p){return 10;}
            @Override public Item findItemBag(Player p,int id){Item it=ItemService.gI().createNewItem((short)id);it.quantity=1;return it;}
            @Override public boolean addItemBag(Player p,Item it){grants.add(it);return true;}
            @Override public void subQuantityItemsBag(Player p,Item it,int qty){consumed[0]+=qty;}
            @Override public void sendItemBags(Player p){}
        });
        Set<Integer> variants=new HashSet<>();
        Field random=UseItem.class.getDeclaredField("rand");random.setAccessible(true);
        ((Random)random.get(null)).setSeed(20261004L);
        for(int i=0;i<200;i++) {
            UseItem.gI().OpenTrungRongNhi(owner(),1821);
            Item it=grants.get(i);int id=it.template.id;variants.add(id);
            Map<Integer,Integer> actual=options(it),want=expected(id);Integer days=actual.remove(93);
            check(actual.equals(want),"Egg uses exact fixed profile");
            check(days==null||(days>=1&&days<=30),"Egg duration is permanent or 1-30 days");
        }
        check(variants.size()==7&&consumed[0]==200,"All 7 variants and one consumed egg per reward");
    }
}
