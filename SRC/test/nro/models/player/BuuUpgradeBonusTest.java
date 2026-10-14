package nro.models.player;

import java.lang.reflect.Method;
import nro.models.item.Item;
import nro.models.player_system.Template;

public final class BuuUpgradeBonusTest {
    static void check(boolean v,String s){if(!v)throw new AssertionError(s);}
    static Player owner(int type)throws Exception {
        Method m=PetFusionBonusTest.class.getDeclaredMethod("owner",int.class);m.setAccessible(true);
        return (Player)m.invoke(null,type);
    }
    static Item gear(int slot,int level) {
        Item i=new Item();i.template=new Template.ItemTemplate();i.template.id=(short)(100+slot);i.template.type=(byte)slot;i.quantity=1;
        i.itemOptions.add(new Item.ItemOption(new Template.ItemOptionTemplate(72,"Upgrade #",0),level));return i;
    }
    static void set(Player p,int level){for(int s=0;s<5;s++)p.inventory.itemsBody.set(s,gear(s,level));}
    static void stats(Player p,int hp,int ki,int attack){check(p.nPoint.hpMax==hp && p.nPoint.mpMax==ki && p.nPoint.dame==attack,"Stats expected "+hp+"/"+ki+"/"+attack+" actual "+p.nPoint.hpMax+"/"+p.nPoint.mpMax+"/"+p.nPoint.dame);}
    static void buu()throws Exception {
        for(byte mode:new byte[]{4,6,8,9,10}) {
            Player p=owner(1);p.fusion.typeFusion=mode;p.pet.status=Pet.FUSION;
            for(int n=0;n<20;n++){p.nPoint.calPoint();stats(p.pet,440000,440000,22000);stats(p,540000,540000,23000);}
            p.fusion.typeFusion=0;p.pet.status=Pet.FOLLOW;p.nPoint.calPoint();stats(p.pet,400000,400000,20000);stats(p,100000,100000,1000);
        }
    }
    static void equipment()throws Exception {
        for(int level=4;level<=9;level++) {
            Player p=owner(0);set(p,level);int pct=Math.max(0,Math.min(8,level)-4);
            for(int n=0;n<20;n++){p.nPoint.calPoint();stats(p,100000+1000*pct,100000+1000*pct,1000+10*pct);}
            check(p.getEffFront()==Math.min(8,level),"Full-set aura grade");
        }
        Player p=owner(1);set(p,8);p.inventory.itemsBody.set(2,gear(2,5));p.nPoint.calPoint();stats(p,101000,101000,1010);check(p.getEffFront()==5,"Weakest equipped item determines grade");
        p.inventory.itemsBody.set(1,new Item());p.nPoint.calPoint();stats(p,100000,100000,1000);check(p.getEffFront()==-1,"Missing gear removes aura");
        set(p.pet,8);p.nPoint.calPoint();stats(p.pet,416000,416000,20800);check(p.pet.getEffFront()==8,"Nine-slot disciple aura");
        p.fusion.typeFusion=6;p.pet.status=Pet.FUSION;p.nPoint.calPoint();stats(p.pet,457600,457600,22880);stats(p,557600,557600,23880);
    }
    public static void main(String[] args)throws Exception {if(args[0].equals("buu"))buu();else equipment();System.out.println("PASS "+args[0]);}
}
