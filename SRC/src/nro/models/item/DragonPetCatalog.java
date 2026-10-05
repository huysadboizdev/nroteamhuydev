package nro.models.item;

import com.google.gson.Gson;
import java.sql.*;
import java.util.*;
import nro.models.item.Item.ItemOption;

/** Golden-egg dragon profiles; SQL overrides are loaded atomically at startup. */
public final class DragonPetCatalog {
    private DragonPetCatalog() {}
    private record Profile(int stars, int permanentPercent, int minDays, int maxDays, int[][] options) {}
    private static volatile Map<Integer, Profile> profiles = defaults();

    private static Map<Integer, Profile> defaults() {
        int[][][] options = {
            {{77,16},{50,16},{103,16},{236,20},{30,0}},
            {{50,18},{5,7},{14,5},{30,0}},
            {{50,18},{94,5},{108,7},{30,0}},
            {{77,14},{5,7},{14,5},{30,0}},
            {{77,18},{94,5},{108,7},{30,0}},
            {{77,22},{50,22},{94,8},{5,11},{14,8},{30,0}},
            {{77,22},{50,22},{94,8},{5,11},{14,8},{106,0},{30,0}}
        };
        Map<Integer, Profile> result = new LinkedHashMap<>();
        for (int i=0;i<7;i++) result.put(1765+i,new Profile(7-i,10,1,30,options[i]));
        return Collections.unmodifiableMap(result);
    }

    public static int stars(int itemId) {
        return profile(itemId).stars();
    }

    private static Profile profile(int itemId) {
        Profile p=profiles.get(itemId);
        if(p==null)throw new IllegalArgumentException("Not a dragon pet: "+itemId);
        return p;
    }

    /** Applied to a freshly created reward, never retroactively to player inventory. */
    public static void applyRewardOptions(Item item, Random rng) {
        Profile p=profile(item.template.id);
        item.itemOptions.clear();
        for(int[] pair:p.options())item.itemOptions.add(new ItemOption(pair[0],pair[1]));
        if(rng.nextInt(100)>=p.permanentPercent()) {
            int days=p.minDays()+rng.nextInt(p.maxDays()-p.minDays()+1);
            item.itemOptions.add(new ItemOption(93,days));
        }
    }

    public static void loadFromDatabase(Connection connection) throws SQLException {
        Map<Integer, Profile> loaded=new LinkedHashMap<>();
        try(Statement st=connection.createStatement();ResultSet rs=st.executeQuery(
                "SELECT item_id,stars,permanent_percent,min_days,max_days,options_json FROM dragon_pet_config ORDER BY item_id")) {
            while(rs.next()) {
                int id=rs.getInt("item_id"),stars=rs.getInt("stars"),chance=rs.getInt("permanent_percent");
                int min=rs.getInt("min_days"),max=rs.getInt("max_days");
                int[][] opts;
                try{opts=new Gson().fromJson(rs.getString("options_json"),int[][].class);}
                catch(RuntimeException e){throw new SQLException("Invalid dragon options for "+id,e);}
                if(id<1765||id>1771||stars!=1772-id||chance<0||chance>100||min<1||max>30||min>max||opts==null)
                    throw new SQLException("Invalid dragon profile "+id);
                Set<Integer> seen=new HashSet<>();
                for(int[] pair:opts) {
                    if(pair==null||pair.length!=2||pair[1]<0||pair[0]==93||
                            !Set.of(5,14,30,50,77,94,103,106,108,236).contains(pair[0])||!seen.add(pair[0]))
                        throw new SQLException("Invalid/duplicate dragon option for "+id);
                }
                if(!seen.contains(30))throw new SQLException("Dragon must be non-tradeable: "+id);
                if(loaded.put(id,new Profile(stars,chance,min,max,opts))!=null)throw new SQLException("Duplicate dragon "+id);
            }
        } catch(SQLException e) {
            // Fresh databases using an older dump retain the same built-in profiles.
            if(e.getErrorCode()==1146) {
                profiles=defaults();
                System.out.println("Dragon pet config table missing; using built-in profiles");
                return;
            }
            throw e;
        }
        if(loaded.size()!=7)throw new SQLException("Expected seven dragon profiles");
        profiles=Collections.unmodifiableMap(loaded);
        System.out.println("Successfully loaded dragon pet profiles (7); permanent chance 10%, duration 1-30 days");
    }
}
