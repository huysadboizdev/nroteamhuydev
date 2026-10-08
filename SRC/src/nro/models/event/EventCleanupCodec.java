package nro.models.event;

import java.util.Objects;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/** Detached cleanup of saved inventories. Every slot is validated before output is returned. */
public final class EventCleanupCodec {
    private EventCleanupCodec() {}

    private static JSONArray array(Object value) {
        Object parsed=value;
        if(value instanceof String text) {
            try {parsed=new JSONParser().parse(text);}
            catch(ParseException failure) {throw new IllegalArgumentException("Invalid inventory JSON",failure);}
        }
        if(!(parsed instanceof JSONArray result))throw new IllegalArgumentException("Expected saved JSON array");
        return result;
    }

    private static long integer(Object value,long min,long max) {
        if(!(value instanceof Long number)||number<min||number>max)
            throw new IllegalArgumentException("Invalid saved inventory integer");
        return number;
    }

    @SuppressWarnings("unchecked")
    public static String clean(String json,Set<Integer> removed) {
        Objects.requireNonNull(removed,"removed");
        JSONArray slots=array(json);
        boolean changed=false;
        for(int index=0;index<slots.size();index++) {
            Object raw=slots.get(index);
            JSONArray row=array(raw);
            if(row.size()!=4&&row.size()!=6)throw new IllegalArgumentException("Invalid inventory slot size");
            boolean collection=row.size()==6;
            if(collection&&(raw instanceof String||!(row.get(2) instanceof JSONArray)))
                throw new IllegalArgumentException("Invalid collection slot encoding");
            int template=(int)integer(row.get(0),-1,Short.MAX_VALUE);
            long quantity=integer(row.get(1),0,Integer.MAX_VALUE);
            if(template==-1&&quantity!=0||template!=-1&&quantity==0)
                throw new IllegalArgumentException("Invalid saved slot quantity");
            if(collection&&template!=-1&&quantity!=1)
                throw new IllegalArgumentException("Invalid collection quantity");
            JSONArray options=array(row.get(2));
            for(Object option:options) {
                JSONArray pair=array(option);
                if(pair.size()!=2||collection&&!(option instanceof JSONArray))
                    throw new IllegalArgumentException("Invalid saved item option");
                integer(pair.get(0),0,collection?255:Integer.MAX_VALUE);
                integer(pair.get(1),Integer.MIN_VALUE,Integer.MAX_VALUE);
            }
            if(template==-1&&!options.isEmpty())throw new IllegalArgumentException("Invalid empty slot options");
            integer(row.get(3),Long.MIN_VALUE,Long.MAX_VALUE);
            if(collection) {
                integer(row.get(4),Long.MIN_VALUE,Long.MAX_VALUE);
                integer(row.get(5),Integer.MIN_VALUE,Integer.MAX_VALUE);
            }
            if(template!=-1&&removed.contains(template)) {
                row.set(0,-1L);row.set(1,0L);
                row.set(2,collection||row.get(2) instanceof JSONArray?new JSONArray():"[]");
                if(collection){row.set(4,0L);row.set(5,0L);}
                slots.set(index,raw instanceof String?row.toJSONString():row);
                changed=true;
            }
        }
        return changed?slots.toJSONString():json;
    }

    @SuppressWarnings("unchecked")
    public static String cleanPet(String json,Set<Integer> removed) {
        Objects.requireNonNull(removed,"removed");
        JSONArray pet=array(json);
        if(pet.isEmpty())return json;
        if(pet.size()!=4)throw new IllegalArgumentException("Invalid pet save");
        array(pet.get(0));array(pet.get(1));array(pet.get(3));
        Object savedBody=pet.get(2);
        String body=savedBody instanceof String?(String)savedBody:array(savedBody).toJSONString();
        String cleaned=clean(body,removed);
        if(body.equals(cleaned))return json;
        pet.set(2,savedBody instanceof String?cleaned:array(cleaned));
        return pet.toJSONString();
    }
}
