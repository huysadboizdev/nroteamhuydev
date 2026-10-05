package nro.models.player;

import java.lang.reflect.*;
import java.util.Random;
import nro.models.consts.ConstPlayer;
import nro.models.intrinsic.Intrinsic;
import nro.models.item.Item;
import nro.models.map.Map;
import nro.models.map.Zone;
import nro.models.npc.NonInteractiveNPC;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.Service;
import nro.models.services.ItemService;
import nro.models.services.SkillService;
import nro.models.skill.Skill;
import nro.models.utils.Util;

/** Offline checks run real stat, damage and power/potential award code. */
public final class CostumeCombatTest {
    static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
    static void equal(long actual, long expected, String message) {
        check(actual == expected, message + ": expected " + expected + ", got " + actual);
    }
    static void seed() throws Exception {
        Field f = Util.class.getDeclaredField("rand"); f.setAccessible(true);
        ((Random) f.get(null)).setSeed(50);
    }
    static Map map() throws Exception {
        Class<?> u = Class.forName("sun.misc.Unsafe");
        Field f = u.getDeclaredField("theUnsafe"); f.setAccessible(true);
        Map map = (Map) u.getMethod("allocateInstance", Class.class).invoke(f.get(null), Map.class);
        map.mapId = 6;
        return map;
    }
    static Player setup(Player p) throws Exception {
        p.isPlayer = true;
        p.playerIntrinsic.intrinsic = new Intrinsic();
        p.zone = new Zone(map(), 0, 20);
        p.nPoint.hp = p.nPoint.hpMax = 100000;
        p.nPoint.mp = p.nPoint.mpMax = 100000;
        p.nPoint.dame = 1000; p.nPoint.stamina = 100;
        Skill skill = new Skill(); skill.template = new Template.SkillTemplate();
        skill.template.id = Skill.KAMEJOKO; skill.damage = 100; skill.point = 1;
        p.playerSkill.skillSelect = skill; p.playerSkill.skills.add(skill);
        for (int i = 0; i < 7; i++) p.inventory.itemsBody.add(new Item());
        return p;
    }
    static Player player() throws Exception { return setup(new Player()); }
    static Item costume(Player p, int id, int multiplier) {
        Item item = new Item((short) id); item.quantity = 1;
        item.itemOptions.add(new Item.ItemOption(159, multiplier));
        p.inventory.itemsBody.set(5, item); p.nPoint.xChuong = (short) multiplier;
        return item;
    }
    static int damage(Player p, boolean mob, Player target, boolean legal) throws Exception {
        seed();
        try {
            return (int) NPoint.class.getMethod("getDameAttack", boolean.class, Player.class, boolean.class)
                    .invoke(p.nPoint, mob, target, legal);
        } catch (NoSuchMethodException beforeImplementation) {
            return p.nPoint.getDameAttack(mob);
        }
    }
    static void sync(Player p, long now) throws Exception {
        try {
            Method m = EffectSkin.class.getDeclaredMethod("updateXChuong", long.class);
            m.setAccessible(true); m.invoke(p.effectSkin, now);
        } catch (NoSuchMethodException beforeImplementation) {
            Method m = EffectSkin.class.getDeclaredMethod("updateXChuong");
            m.setAccessible(true); m.invoke(p.effectSkin);
        }
    }
    static void ready(Player p) throws Exception {
        sync(p, System.currentTimeMillis());
        p.effectSkin.lastTimeXChuong = System.currentTimeMillis() - 60001;
        sync(p, System.currentTimeMillis());
    }
    static void pvp() throws Exception {
        for (int[] c : new int[][]{{710,3},{711,4},{1710,5}}) {
            Player p = player(), target = player(); costume(p,c[0],c[1]); ready(p);
            p.effectSkin.isXChuong = false;
            int base = damage(p,false,target,false);
            ready(p);
            equal(damage(p,true,null,false),base,"Mob damage must stay normal for " + c[0]);
            check(p.effectSkin.isXChuong,"Mob must preserve charge");
            for (int kind = 0; kind < 5; kind++) {
                Player excluded = player();
                if (kind == 0) excluded.isBoss = true;
                if (kind == 1) excluded.isBot = true;
                if (kind == 2) excluded.isPet = true;
                if (kind == 3) excluded.isNewPet = true;
                if (kind == 4) excluded.isPlayer = false;
                equal(damage(p,false,excluded,true),base,"Excluded target must stay normal " + kind);
                check(p.effectSkin.isXChuong,"Excluded target preserves charge " + kind);
            }
            Player npc=setup(new NonInteractiveNPC());
            equal(damage(p,false,npc,true),base,"Actual NPC subclass has no multiplier");
            check(p.effectSkin.isXChuong,"NPC preserves charge");
            equal(damage(p,false,null,true),base,"Null target has no PvP permission");
            equal(damage(p,false,target,false),base,"Miss or prohibited PK preserves normal damage");
            check(p.effectSkin.isXChuong,"Invalid PvP preserves charge");
            p.playerSkill.skillSelect.template.id = Skill.DRAGON;
            damage(p,false,target,true); check(p.effectSkin.isXChuong,"Punch preserves charge");
            p.playerSkill.skillSelect.template.id = Skill.KAMEJOKO;
            equal(damage(p,false,target,true),(long)base*c[1],"Valid PvP multiplies exactly " + c[1]);
            check(!p.effectSkin.isXChuong,"Valid PvP consumes charge");
            equal(damage(p,false,target,true),base,"Next attack waits for cooldown");
            for(int beam:new int[]{Skill.ANTOMIC,Skill.MASENKO}) {
                p.playerSkill.skillSelect.template.id=(byte)beam;
                int beamBase=damage(p,false,target,false);ready(p);
                equal(damage(p,false,target,true),(long)beamBase*c[1],"Other basic beam uses exact multiplier " + beam);
            }
            p.playerSkill.skillSelect.template.id=Skill.KAMEJOKO;
            for (int kind = 0; kind < 3; kind++) {
                Player attacker = player(); costume(attacker,c[0],c[1]); ready(attacker);
                if (kind == 0) attacker.isBoss = true;
                if (kind == 1) attacker.isBot = true;
                if (kind == 2) attacker.isPet = true;
                equal(damage(attacker,false,target,true),base,"Excluded attacker has no multiplier");
            }
        }
    }
    static void cooldown() throws Exception {
        Player p = player(); costume(p,710,3); long now = 1000000;
        sync(p,now); check(!p.effectSkin.isXChuong,"Fresh equip waits sixty seconds");
        sync(p,now+59999); check(!p.effectSkin.isXChuong,"59999ms is too early");
        sync(p,now+60000); check(p.effectSkin.isXChuong,"60000ms is ready");
        costume(p,711,4); sync(p,now+60001);
        check(!p.effectSkin.isXChuong,"Changing costume drops previous charge");
        sync(p,now+120001); check(p.effectSkin.isXChuong,"New costume gets independent cooldown");
        p.inventory.itemsBody.set(5,new Item()); p.nPoint.xChuong=0; sync(p,now+120002);
        check(!p.effectSkin.isXChuong,"Removal drops charge");
        costume(p,711,4); sync(p,now+120003);
        check(!p.effectSkin.isXChuong,"Reequip cannot get instant charge");
        costume(p,711,4); sync(p,now+180003);
        check(!p.effectSkin.isXChuong,"Another item with same ID resets cooldown");
    }
    static void pet() throws Exception {
        for (int bonus : new int[]{35,50,60}) {
            Player master=player(); Pet pet=(Pet)setup(new Pet(master));
            Item equipped=costume(master,bonus==35?710:bonus==50?711:1710,3);
            equipped.itemOptions.add(new Item.ItemOption(160,bonus)); master.nPoint.calPoint();
            Manager.RATE_EXP_SERVER=1;
            equal(pet.nPoint.calSucManhTiemNang(1000),1000+10*bonus,"Pet bonus percentage " + bonus);
            equal(pet.nPoint.calSucManhTiemNang(99),99+99*bonus/100,"Small reward preserves fractional calculation");
            long large=Long.MAX_VALUE/2;
            equal(pet.nPoint.calSucManhTiemNang(large),large+(large/100*bonus+large%100*bonus/100),"Large reward avoids product overflow");
            equal(pet.nPoint.calSucManhTiemNang(Long.MAX_VALUE),Long.MAX_VALUE,"Unrepresentable positive pet reward saturates safely");
            equal(master.nPoint.calSucManhTiemNang(1000),1000,"Master has no pet costume bonus");
            long sm=pet.nPoint.power,tn=pet.nPoint.tiemNang;
            Service.gI().addSMTN(pet,(byte)2,pet.nPoint.calSucManhTiemNang(1000),false);
            equal(pet.nPoint.power-sm,1000+10*bonus,"Real award updates pet power");
            equal(pet.nPoint.tiemNang-tn,1000+10*bonus,"Real award updates pet potential");
            master.inventory.itemsBody.set(5,new Item()); master.nPoint.calPoint();
            equal(pet.nPoint.calSucManhTiemNang(1000),1000,"Removing costume ends bonus");
        }
    }
    static void armor() throws Exception {
        Player p=player(); p.nPoint.dameg=1000; p.nPoint.hpg=p.nPoint.mpg=1000;
        Item armor=new Item((short)1869); armor.quantity=1;
        for(int[] o:new int[][]{{77,15},{103,15},{94,5},{47,50000},{9,1000},{251,0}})
            armor.itemOptions.add(new Item.ItemOption(o[0],o[1]));
        p.inventory.itemsBody.set(6,armor); p.nPoint.calPoint();
        equal(p.nPoint.dame,600,"Level5 wearing reduces damage forty percent");
        equal(p.nPoint.hpMax,1150,"Level5 HP +15%"); equal(p.nPoint.mpMax,1150,"Level5 KI +15%");
        equal(p.nPoint.def,50000,"Level5 armor +50000"); equal(p.nPoint.tlGiap,5,"Level5 mitigation 5%");
        p.nPoint.hp=p.nPoint.hpMax=1000000;
        equal(p.injured(null,100000,false,true),45000,"Real injury applies five percent mitigation and 50000 defense");
        Method timer=EffectSkin.class.getDeclaredMethod("updateTrainArmor");timer.setAccessible(true);
        p.effectSkin.lastTimeAttack=System.currentTimeMillis();
        p.effectSkin.lastTimeSubTimeTrainArmor=System.currentTimeMillis();
        armor.itemOptions.get(4).param=999; timer.invoke(p.effectSkin);
        equal(armor.itemOptions.get(4).param,1000,"Worn active training recharges one minute");
        p.effectSkin.lastTimeAddTimeTrainArmor=0; timer.invoke(p.effectSkin);
        equal(armor.itemOptions.get(4).param,1001,"Level5 farming recharges beyond initial 1000 minutes");
        p.effectSkin.lastTimeAddTimeTrainArmor=0;armor.itemOptions.get(4).param=9999;timer.invoke(p.effectSkin);
        equal(armor.itemOptions.get(4).param,10000,"Level5 farming reaches 10000 minute cap");
        p.effectSkin.lastTimeAddTimeTrainArmor=0;timer.invoke(p.effectSkin);
        equal(armor.itemOptions.get(4).param,10000,"Level5 never recharges beyond 10000");
        armor.itemOptions.get(4).param=1200;p.effectSkin.lastTimeAddTimeTrainArmor=0;
        p.effectSkin.lastTimeAttack=System.currentTimeMillis()-31000;timer.invoke(p.effectSkin);
        equal(armor.itemOptions.get(4).param,1200,"Idle equipped armor does not recharge");
        p.effectSkin.lastTimeAttack=System.currentTimeMillis();p.effectSkin.lastTimeAddTimeTrainArmor=System.currentTimeMillis();
        armor.itemOptions.get(4).param=1000;
        p.inventory.itemsBody.set(6,new Item()); p.inventory.itemsBag.add(armor); p.nPoint.calPoint();
        equal(p.nPoint.dame,1500,"Removed level5 active timer increases fifty percent");
        equal(p.nPoint.hpMax,1000,"Removed armor drops HP bonus"); equal(p.nPoint.mpMax,1000,"Removed armor drops KI bonus");
        equal(p.nPoint.def,0,"Removed armor drops defense"); equal(p.nPoint.tlGiap,0,"Removed armor drops mitigation");
        p.effectSkin.lastTimeSubTimeTrainArmor=0; timer.invoke(p.effectSkin);
        equal(armor.itemOptions.get(4).param,999,"Removed armor decrements training timer");
        armor.itemOptions.get(4).param=0; p.nPoint.calPoint();
        equal(p.nPoint.dame,1000,"Expired training armor drops damage bonus");
        for(int[] legacy:new int[][]{{529,10},{534,10},{530,20},{535,20},{531,30},{536,30},{1716,40}}) {
            Player q=player(); q.nPoint.dameg=1000;
            Item old=new Item((short)legacy[0]);old.quantity=1;old.itemOptions.add(new Item.ItemOption(9,1));
            q.inventory.itemsBody.set(6,old);q.nPoint.calPoint();
            equal(q.nPoint.dame,1000-10*legacy[1],"Legacy worn armor damage " + legacy[0]);
            old.itemOptions.get(0).param=1000;q.effectSkin.lastTimeAttack=System.currentTimeMillis();
            q.effectSkin.lastTimeSubTimeTrainArmor=System.currentTimeMillis();timer.invoke(q.effectSkin);
            equal(old.itemOptions.get(0).param,1000,"Other armor levels keep their original 1000 minute cap");
            q.inventory.itemsBody.set(6,new Item());q.inventory.itemsBag.add(old);q.nPoint.calPoint();
            equal(q.nPoint.dame,1000+10*legacy[1],"Legacy removed armor damage " + legacy[0]);
            check(ItemService.gI().isTrainArmor(old),"Legacy armor recognition " + legacy[0]);
        }
    }
    static void legacy() throws Exception {
        Player p=player(); costume(p,999,3); p.effectSkin.isXChuong=true;
        int boosted=damage(p,true,null,false); equal(boosted,3039,"Unrelated option159 keeps legacy mob multiplier");
        check(p.effectSkin.isXDame,"Legacy effect marker survives");
    }
    static void timerGaps(boolean box) throws Exception {
        Player p=player();
        java.util.List<Item> slots=box?p.inventory.itemsBox:p.inventory.itemsBag;
        slots.add(new Item());
        Item armor=new Item((short)1869);
        armor.itemOptions.add(new Item.ItemOption(9,2)); slots.add(armor);
        Method timer=EffectSkin.class.getDeclaredMethod("updateTrainArmor");timer.setAccessible(true);
        timer.invoke(p.effectSkin);
        equal(armor.itemOptions.get(0).param,1,"Level5 timer counts down after an empty " + (box?"box":"bag") + " slot");
        // All slots and legacy armor remain supported, including malformed optional values.
        slots.add(null);
        Item malformed=new Item((short)529);malformed.itemOptions=null;slots.add(malformed);
        for(int id:new int[]{529,530,531,1716}) {
            Item old=new Item((short)id);
            old.itemOptions.add(null);
            old.itemOptions.add(new Item.ItemOption());
            old.itemOptions.add(new Item.ItemOption(9,2));slots.add(old);
        }
        p.effectSkin.lastTimeSubTimeTrainArmor=0;timer.invoke(p.effectSkin);
        equal(armor.itemOptions.get(0).param,0,"Level5 timer expires after second minute");
        for(Item old:slots)if(old!=null&&old.itemOptions!=null&&old.itemOptions.size()==3)
            equal(old.itemOptions.get(2).param,1,"Legacy timer counts down after sparse slots " + old.template.id);
    }
    static void armorOwnership(String mode) throws Exception {
        Player p=player(); p.nPoint.dameg=1000;
        Item armor=new Item((short)1869);armor.quantity=1;
        armor.itemOptions.add(new Item.ItemOption(9,2));
        p.inventory.itemsBody.set(6,armor);p.nPoint.calPoint();
        p.inventory.itemsBody.set(6,new Item());p.inventory.itemsBag.add(armor);p.nPoint.calPoint();
        equal(p.nPoint.dame,1500,"Owned removed armor enables bonus");
        if(mode.equals("sold")) {
            p.inventory.itemsBag.clear();p.nPoint.calPoint();
            equal(p.nPoint.dame,1000,"Sold or traded armor cannot keep damage bonus");
            check(p.inventory.trainArmor==null,"Detached armor cache is cleared");
        } else if(mode.equals("zero")) {
            armor.quantity=0;p.nPoint.calPoint();
            equal(p.nPoint.dame,1000,"Zero quantity armor cannot keep damage bonus");
        } else if(mode.equals("box")) {
            p.inventory.itemsBag.clear();p.inventory.itemsBox.add(armor);
            p.inventory.trainArmor=null;p.nPoint.calPoint();
            equal(p.nPoint.dame,1500,"Owned armor can be selected from box after cache reset");
            p.inventory.itemsBox.clear();p.nPoint.calPoint();
            equal(p.nPoint.dame,1000,"Transferred box armor cannot keep damage bonus");
        } else if(mode.equals("replacement")) {
            p.inventory.itemsBag.clear();
            Item replacement=new Item((short)1869);replacement.quantity=1;
            replacement.itemOptions.add(new Item.ItemOption(9,2));
            p.inventory.itemsBag.add(replacement);p.nPoint.calPoint();
            check(p.inventory.trainArmor==replacement,"Cache reassesses current owned armor by identity");
            replacement.itemOptions.get(0).param=0;p.nPoint.calPoint();
            equal(p.nPoint.dame,1000,"New armor timer governs bonus instead of detached cache");
        } else if(mode.equals("unrelated")) {
            p.inventory.itemsBag.clear();p.inventory.trainArmor=null;
            Item unrelated=new Item((short)999);unrelated.quantity=1;
            p.inventory.itemsBody.set(6,unrelated);p.nPoint.calPoint();
            check(!p.nPoint.wearingTrainArmor,"Unrelated equipment is not training armor");
        } else throw new AssertionError(mode);
    }
    static final class Target extends Player {
        long received;
        boolean dodge;
        @Override public int injured(Player attacker,long damage,boolean piercing,boolean mob) {
            received=damage; return dodge ? 0 : (int)damage;
        }
    }
    static void service() throws Exception {
        Method attack=SkillService.class.getDeclaredMethod("playerAttackPlayer",Player.class,Player.class,boolean.class);
        attack.setAccessible(true);
        for(int[] c:new int[][]{{710,3},{711,4},{1710,5}}) {
            Player p=player(); Target target=(Target)setup(new Target());
            costume(p,c[0],c[1]); ready(p); p.typePk=ConstPlayer.PK_ALL;
            int base=damage(p,false,target,false);
            attack.invoke(SkillService.gI(),p,target,true);
            equal(target.received,0,"SkillService explicit miss deals zero");
            check(p.effectSkin.isXChuong,"SkillService miss preserves charge");
            p.typePk=0; target.typePk=0;
            seed(); attack.invoke(SkillService.gI(),p,target,false);
            equal(target.received,base,"SkillService blocked PK has no multiplier");
            check(p.effectSkin.isXChuong,"SkillService blocked PK preserves charge");
            p.typePk=ConstPlayer.PK_ALL; target.dodge=true;
            long cooldown=p.effectSkin.lastTimeXChuong;
            seed(); attack.invoke(SkillService.gI(),p,target,false);
            check(p.effectSkin.isXChuong,"Zero actual damage preserves charge");
            equal(p.effectSkin.lastTimeXChuong,cooldown,"Dodge preserves cooldown timestamp");
            target.dodge=false; seed(); attack.invoke(SkillService.gI(),p,target,false);
            equal(target.received,(long)base*c[1],"SkillService qualifies valid PvP target");
            check(!p.effectSkin.isXChuong,"SkillService consumes successful PvP charge");
            ready(p); costume(p,c[0],c[1]); target.dodge=true;
            seed(); attack.invoke(SkillService.gI(),p,target,false);
            check(!p.effectSkin.isXChuong,"Changed costume cannot recover stale charge on a dodge");
        }
    }
    public static void main(String[] args) throws Exception {
        for(int i=0;i<2000;i++){Template.ItemTemplate t=new Template.ItemTemplate();t.id=(short)i;t.type=5;t.name="fixture";Manager.ITEM_TEMPLATES.add(t);}
        Manager.ITEM_TEMPLATES.get(1869).type=32;
        for(int i=0;i<=251;i++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(i,"option",0));
        Field f=Service.class.getDeclaredField("instance");f.setAccessible(true);
        f.set(null,new Service(){@Override public void point(Player p){} });
        if(args[0].startsWith("armor-owned-"))armorOwnership(args[0].substring("armor-owned-".length()));
        else switch(args[0]){case "pvp":pvp();break;case "cooldown":cooldown();break;case "pet":pet();break;case "armor":armor();break;case "legacy":legacy();break;case "service":service();break;case "timer-gaps-bag":timerGaps(false);break;case "timer-gaps-box":timerGaps(true);break;default:throw new AssertionError(args[0]);}
        System.out.println("PASS CostumeCombatTest " + args[0]);
    }
}

