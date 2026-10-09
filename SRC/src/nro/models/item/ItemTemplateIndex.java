package nro.models.item;
import java.util.*;
import nro.models.player_system.Template.ItemTemplate;

/** The client protocol uses list positions as IDs; SQL IDs may have gaps. */
public final class ItemTemplateIndex {
    private ItemTemplateIndex() {}
    public static void align(List<ItemTemplate> templates) {
        List<ItemTemplate> rows=new ArrayList<>(templates);
        int max=-1;
        for(ItemTemplate t:rows){if(t.id<0)throw new IllegalArgumentException("Negative item ID");max=Math.max(max,t.id);}
        List<ItemTemplate> indexed=new ArrayList<>(max+1);
        for(int id=0;id<=max;id++){
            ItemTemplate gap=new ItemTemplate();gap.id=(short)id;gap.type=-1;gap.gender=3;
            gap.name="";gap.description="";gap.iconID=-1;gap.part=-1;gap.head=-1;gap.body=-1;gap.leg=-1;
            indexed.add(gap);
        }
        Set<Short> seen=new HashSet<>();
        for(ItemTemplate t:rows){if(!seen.add(t.id))throw new IllegalArgumentException("Duplicate item ID "+t.id);indexed.set(t.id,t);}
        templates.clear();templates.addAll(indexed);
    }
}
