package nro.models.combine;

import java.lang.reflect.*;
import java.util.*;
import java.util.function.IntUnaryOperator;
import nro.models.item.Item;
import static nro.models.services.PinkFixture.*;

public final class ThienSuCraftStatsTest {
    static Class<?> feature(String name) {
        try{return Class.forName("nro.models.combine."+name);}
        catch(ClassNotFoundException absent){throw new AssertionError(name+" behavior is missing");}
    }
    static void apply(Item item,int luck,IntUnaryOperator random) throws Exception {
        feature("ThienSuCraftStats").getMethod("apply",Item.class,int.class,IntUnaryOperator.class).invoke(null,item,luck,random);
    }
    static Item equipment(int id,int base) {
        Item i=item(id,1);i.template.type=(byte)((id-1048)/3);
        i.itemOptions.add(new Item.ItemOption(new int[]{47,22,0,23,14}[i.template.type],base));
        i.itemOptions.add(new Item.ItemOption(21,80));i.itemOptions.add(new Item.ItemOption(30,1));
        return i;
    }
    public static void main(String[] args)throws Exception {
        install();feature("ThienSuCraftStats");
        for(int id=1048;id<=1062;id++)for(int base:new int[]{18,19,20,22,130,2800,12650,Integer.MAX_VALUE/2})for(int roll=1;roll<=30;roll++) {
            Item i=equipment(id,base);int[] calls={0};final int r=roll;
            apply(i,10,bound->{calls[0]++;return bound==30?r-1:50;});
            long inc=Math.min(((long)base*roll+99)/100,(long)base*30/100);
            check(i.itemOptions.get(0).param==base+inc,"Main bonus bounded with correct rounding");
            check(i.itemOptions.size()==3&&i.itemOptions.get(1).param==80&&i.itemOptions.get(2).param==1,"Other options untouched");
            check(calls[0]==2,"Main rolls once then luck once");
        }
        for(int luck=10;luck<=30;luck+=5)for(int draw=0;draw<=50;draw++)for(int param:new int[]{1,7}) {
            Item i=equipment(1048,4000);final int d=draw,v=param;int[] count={0};
            apply(i,luck,bound->{int n=count[0]++;return n==0?0:n==1?d:bound==7?v-1:0;});
            int expected=draw>luck?0:draw>=luck-3?3:draw>=luck-10?2:1;
            check(i.itemOptions.size()==3+expected,"Preserve luck line distribution");
            Set<Integer> seen=new HashSet<>();
            for(int n=3;n<i.itemOptions.size();n++) {
                var o=i.itemOptions.get(n);check(Set.of(50,77,103,94,5).contains(o.optionTemplate.id),"Auxiliary pool without fake option15");
                check(seen.add(o.optionTemplate.id)&&o.param==param,"Distinct auxiliary lines each1..7");
            }
        }
        realFactory();
        System.out.println("PASS stats:15templates/all30rolls/rounding/luck51draws/distinctaux1..7");
    }
    static void realFactory()throws Exception {
        // Only catalog lookup is supplied in memory; call the unchanged production factory.
        replace(nro.models.services.ItemService.class,"i",new nro.models.services.ItemService(){
            @Override public nro.models.player_system.Template.ItemTemplate getTemplate(int id){
                var t=template(id);t.type=(byte)((id-1048)/3);t.gender=(byte)((id-1048)%3);t.isUpToUp=false;return t;
            }
            @Override public nro.models.player_system.Template.ItemOptionTemplate getItemOptionTemplate(int id){var t=new nro.models.player_system.Template.ItemOptionTemplate();t.id=id;t.name="fixture #";return t;}
        });
        int[] min={2800,120,10350,90,18},max={4000,150,11500,130,20};
        for(int id=1048;id<=1062;id++) {
            int type=(id-1048)/3,gender=(id-1048)%3;
            boolean racial=type==0&&gender==2||type==1&&gender==0||type==2&&gender==2||type==3&&gender==1||type==4&&gender==1;
            int lower=racial?(int)(min[type]*1.1):min[type],upper=racial?(int)(max[type]*1.1):max[type];
            Item crafted=nro.models.services.ItemService.gI().DoThienSu(id,gender);
            int base=crafted.itemOptions.get(0).param;
            check(base>=lower&&base<=upper,"Real factory main stat includes racial bonus before crafting:"+id);
            int count=crafted.itemOptions.size();int power=crafted.itemOptions.get(1).param,lock=crafted.itemOptions.get(2).param;
            apply(crafted,10,bound->bound==30?29:50);
            check(crafted.itemOptions.get(0).param==base+(long)base*30/100,"Real factory baseline scaled exactly once:"+id);
            check(crafted.itemOptions.size()==count&&crafted.itemOptions.get(1).param==power&&crafted.itemOptions.get(2).param==lock,"Real factory nonmain options untouched");
            Item plain=nro.models.services.ItemService.gI().DoThienSu(id,gender);
            check(plain.itemOptions.size()==3&&plain.itemOptions.get(0).param>=lower&&plain.itemOptions.get(0).param<=upper,"Other factory grants remain at original baseline");
        }
        System.out.println("PASS real DoThienSu:15templates/racialbaseline/craftrelativebonus/factoryunchanged");
    }
}
