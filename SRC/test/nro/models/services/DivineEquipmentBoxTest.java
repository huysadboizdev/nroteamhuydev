package nro.models.services;

import java.util.*;
import nro.models.item.Item;
import nro.models.shop_ky_gui.ConsignTransactions;
import static nro.models.services.PinkFixture.*;

public class DivineEquipmentBoxTest {
    public static void main(String[] args)throws Exception {
        install();
        int[][] expected={{555,556,562,563,561},{557,558,564,565,561},{559,560,566,567,561}};
        replace(ItemService.class,"i",new ItemService(){
            @Override public nro.models.player_system.Template.ItemTemplate getTemplate(int id){
                var t=template(id);
                for(int[] ids:expected)for(int n=0;n<ids.length;n++)if(ids[n]==id){t.type=(byte)n;t.isUpToUp=false;}
                return t;
            }
            @Override public nro.models.player_system.Template.ItemOptionTemplate getItemOptionTemplate(int id){var t=new nro.models.player_system.Template.ItemOptionTemplate();t.id=id;t.name="fixture #";return t;}
        });
        for(int planet=0;planet<3;planet++) {
            var set=DivineEquipmentBoxService.createSet(planet);
            check(set.size()==5,"full set required");
            for(int n=0;n<5;n++) {
                Item item=set.get(n);check(item.template.id==expected[planet][n],"wrong planet item");
                check(item.itemOptions.stream().filter(o->o.optionTemplate.id==30).count()==1,"trade lock");
                check(item.itemOptions.stream().noneMatch(o->Set.of(34,35,36,50,77,86,87,103).contains(o.optionTemplate.id)),"unexpected seal/percent/consign");
                check(!ConsignTransactions.eligible(item),"set must not be consigned");
            }
            var again=DivineEquipmentBoxService.createSet(planet);
            for(int n=0;n<5;n++)check(signature(set.get(n)).equals(signature(again.get(n))),"stats must be deterministic 0%");
            check(param(set.get(0),47)==new int[]{700,600,800}[planet],"exact baseline armor");
            check(param(set.get(1),22)==new int[]{65,55,60}[planet],"exact baseline HP");
            check(param(set.get(2),0)==new int[]{3200,3000,3500}[planet],"exact baseline attack");
            check(param(set.get(4),14)==16,"exact baseline crit");
        }
        Item box=item(1775,1);box.itemOptions.add(new Item.ItemOption(87,1));
        nro.models.item.RubyItemRules.normalize(box);
        check(!ConsignTransactions.eligible(box),"box must be locked");
        check(box.itemOptions.stream().noneMatch(o->o.optionTemplate.id==86||o.optionTemplate.id==87),"no consign flags");
        System.out.println("PASS DivineEquipmentBox sets: three planets, deterministic base, no seals/percent, trade and consign locks");
        System.exit(0);
    }
    static int param(Item i,int id){return i.itemOptions.stream().filter(o->o.optionTemplate.id==id).findFirst().orElseThrow().param;}
    static List<String> signature(Item i){return i.itemOptions.stream().map(o->o.optionTemplate.id+":"+o.param).toList();}
}
