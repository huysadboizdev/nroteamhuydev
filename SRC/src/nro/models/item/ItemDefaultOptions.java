package nro.models.item;

import java.sql.*;
import java.util.*;

/** SQL-owned base options. Completing a saved item never replaces a saved timer or upgrade. */
public final class ItemDefaultOptions {
    private ItemDefaultOptions() {}
    private record OptionValue(int id, int param) {}
    private static volatile Map<Integer,List<OptionValue>> profiles=Map.of();
    private static final Set<String> audited=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private static final Map<Integer,Set<Integer>> REQUIRED=Map.of(
        710,Set.of(50,77,103,159,160),711,Set.of(50,77,103,159,160),
        1710,Set.of(50,77,103,159,160),1869,Set.of(77,103,94,47,9,251));

    public static void loadFromDatabase(Connection connection)throws SQLException {
        Map<Integer,List<OptionValue>> loaded=new LinkedHashMap<>();
        try(Statement st=connection.createStatement();ResultSet rs=st.executeQuery(
                "SELECT item_id,option_id,param FROM item_default_option ORDER BY item_id,sort_order,option_id")) {
            while(rs.next()) {
                int item=rs.getInt("item_id"),id=rs.getInt("option_id"),param=rs.getInt("param");
                Set<Integer> required=REQUIRED.get(item);
                if(required==null||!required.contains(id)||param<0)
                    throw new SQLException("Invalid default option for item "+item+": "+id);
                List<OptionValue> list=loaded.computeIfAbsent(item,k->new ArrayList<>());
                if(list.stream().anyMatch(o->o.id()==id))throw new SQLException("Duplicate default option "+item+":"+id);
                list.add(new OptionValue(id,param));
            }
        }
        for(var entry:REQUIRED.entrySet()) {
            List<OptionValue> list=loaded.get(entry.getKey());
            if(list==null||list.size()!=entry.getValue().size())throw new SQLException("Incomplete item default options: "+entry.getKey());
        }
        loaded.replaceAll((id,list)->List.copyOf(list));
        profiles=Collections.unmodifiableMap(loaded);
    }

    public static void complete(Item item) {
        if(item==null||!item.isNotNullItem())return;
        List<OptionValue> defaults=profiles.get((int)item.template.id);
        if(defaults==null)return;
        if(item.itemOptions==null)item.itemOptions=new ArrayList<>();
        // Explicit options appended by a reward override its initial defaults. Only managed IDs deduplicate.
        Set<Integer> managed=new HashSet<>();
        for(OptionValue option:defaults)managed.add(option.id());
        Map<Integer,Item.ItemOption> last=new HashMap<>();
        for(Item.ItemOption option:item.itemOptions) {
            if(option!=null&&option.optionTemplate!=null&&managed.contains(option.optionTemplate.id)) {
                Item.ItemOption previous=last.put(option.optionTemplate.id,option);
                if(previous!=null&&previous.param!=option.param)
                    audit(item,option.optionTemplate.id,"duplicate",previous.param,option.param);
            }
        }
        for(OptionValue option:defaults) {
            Item.ItemOption saved=last.get(option.id());
            // Elapsed timer is expected to differ. Other differences remain intact and are audited.
            if(saved!=null&&option.id()!=9&&saved.param!=option.param())
                audit(item,option.id(),"preserved",saved.param,option.param());
        }
        List<Item.ItemOption> result=new ArrayList<>();
        Set<Integer> kept=new HashSet<>();
        for(Item.ItemOption option:item.itemOptions) {
            if(option==null||option.optionTemplate==null)continue;
            int id=option.optionTemplate.id;
            if(!managed.contains(id))result.add(option);
            else if(kept.add(id))result.add(last.get(id));
        }
        for(OptionValue option:defaults)if(!kept.contains(option.id()))result.add(new Item.ItemOption(option.id(),option.param()));
        item.itemOptions=result;
    }

    private static void audit(Item item,int option,String reason,int actual,int reference) {
        String key=item.template.id+":"+option+":"+reason;
        if(audited.add(key))nro.models.utils.Logger.warningln("ITEM_DEFAULT_OPTION_AUDIT item="+item.template.id
            +" option="+option+" reason="+reason+" existing="+actual+" reference="+reference
            +"; explicit/saved values retained; restore snapshot available before deployment");
    }
}
