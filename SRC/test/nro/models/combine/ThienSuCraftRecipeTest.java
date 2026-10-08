package nro.models.combine;

import java.lang.reflect.*;
import nro.models.player.Player;
import nro.models.item.Item;
import static nro.models.services.PinkFixture.*;

public final class ThienSuCraftRecipeTest {
    static Player ready(int fragment,int formula)throws Exception {
        Player p=player(234567);p.gender=2;p.combineNew=new Combine();p.combineNew.typeCombine=515;
        p.inventory.ruby=50;
        for(int id:new int[]{fragment,formula,1074,1079})p.inventory.itemsBag.add(item(id,id==fragment?999:1));
        p.inventory.itemsBag.get(1).template.gender=(byte)(formula>=1084?formula-1084:formula-1071);
        p.inventory.itemsBag.add(new Item());
        int[] body={650,651,657,658,656};
        for(int n=0;n<5;n++){Item i=item(body[n],1);i.template.type=(byte)n;p.inventory.itemsBody.add(i);}
        p.combineNew.itemsCombine.addAll(p.inventory.itemsBag.subList(0,4));return p;
    }
    static Object select(Player p)throws Exception {
        try{return ThienSuCraftStatsTest.feature("ThienSuCraftRecipe").getMethod("select",Player.class).invoke(null,p);}
        catch(InvocationTargetException e){throw (RuntimeException)e.getCause();}
    }
    static void rejected(Player p,String why)throws Exception {
        try{select(p);throw new AssertionError("Accepted "+why);}catch(IllegalArgumentException expected){}
    }
    public static void main(String[] args)throws Exception {
        install();ThienSuCraftStatsTest.feature("ThienSuCraftRecipe");
        int[] fragments={1066,1067,1070,1068,1069};
        for(int slot=0;slot<5;slot++)for(int formula:new int[]{1071,1072,1073,1084,1085,1086}) {
            Object s=select(ready(fragments[slot],formula));
            check((int)s.getClass().getMethod("bodySlot").invoke(s)==slot,"Fragment slot mapping");
            int gender=formula>=1084?formula-1084:formula-1071;
            check((int)s.getClass().getMethod("outputId").invoke(s)==1048+slot*3+gender,"Formula output gender");
        }
        Player p=ready(1066,1071);p.inventory.ruby=49;rejected(p,"ruby49");
        p=ready(1066,1071);p.inventory.itemsBag.get(0).quantity=998;rejected(p,"998fragments");
        p=ready(1066,1071);p.inventory.itemsBag.get(2).quantity=0;rejected(p,"emptyupgrade");
        p=ready(1066,1071);p.combineNew.itemsCombine.set(1,p.combineNew.itemsCombine.get(0));rejected(p,"duplicategroup");
        p=ready(1066,1071);p.inventory.itemsBag.set(0,new Item());rejected(p,"unownedfragment");
        p=ready(1066,1071);p.inventory.itemsBody.set(4,new Item());rejected(p,"missingbody");
        p=ready(1066,1071);p.inventory.itemsBag.set(4,item(6,1));rejected(p,"fullbag");
        p=ready(1066,1071);p.inventory.itemsBag.add(p.inventory.itemsBag.get(0));rejected(p,"duplicatebagidentity");
        p=ready(1066,1071);p.inventory.itemsBody.get(0).quantityGD=1;rejected(p,"reservedbody");
        System.out.println("PASS recipe:5slots/6formulas/quantity/ruby/ownership/body/capacity");
    }
}
