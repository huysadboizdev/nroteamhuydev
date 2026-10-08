package nro.models.event;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;

/** Pure JSON tests; no templates, database or player fixtures required. */
public final class EventCleanupCodecTest {
    private static String call(String name,String json) throws Exception {
        try { return (String)Class.forName("nro.models.event.EventCleanupCodec")
                .getMethod(name,String.class,Set.class).invoke(null,json,Set.of(1220)); }
        catch(InvocationTargetException failure) {
            if(failure.getCause() instanceof IllegalArgumentException invalid) throw invalid;
            throw failure;
        }
    }
    private static JSONArray array(Object value) {
        return (JSONArray)(value instanceof String ? JSONValue.parse((String)value) : value);
    }
    private static void check(boolean ok,String label) { if(!ok)throw new AssertionError(label); }
    private static void reject(String json) throws Exception {
        try {call("clean",json);throw new AssertionError("Accepted invalid JSON: "+json);}
        catch(IllegalArgumentException expected) {}
    }
    public static void main(String[] args) throws Exception {
        String saved="[\"[1220,999999,\\\"[\\\\\\\"[93,10]\\\\\\\"]\\\",123]\",\"[1150,1,\\\"[]\\\",456]\",\"[-1,0,\\\"[]\\\",789]\"]";
        JSONArray before=array(saved),after=array(call("clean",saved));
        check(after.size()==3,"Legacy slots retained");
        check(array(after.get(0)).equals(array("[-1,0,\"[]\",123]")),"Legacy removed with timestamp");
        check(before.get(1).equals(after.get(1)),"Permanent reward raw row preserved");
        check(before.get(2).equals(after.get(2)),"Empty raw row preserved");
        check(call("clean",call("clean",saved)).equals(call("clean",saved)),"Idempotent cleanup");
        String collection="[[1220,1,[[93,10]],123,456,789],[1150,1,[[50,20]],999,888,777]]";
        after=array(call("clean",collection));
        check(array(after.get(0)).equals(array("[-1,0,[],123,0,0]")),"Collection cleared identity and expiry");
        check(array(after.get(1)).equals(array(array(collection).get(1))),"Collection reward metadata preserved");
        JSONArray pet=new JSONArray();pet.add("[4,0,\"Pet\",0,0,1]");pet.add("[0,999]");pet.add(saved);pet.add("[]");
        JSONArray cleanedPet=array(call("cleanPet",pet.toJSONString()));
        check(cleanedPet.get(0).equals(pet.get(0))&&cleanedPet.get(1).equals(pet.get(1))&&cleanedPet.get(3).equals(pet.get(3)),"Pet name, stats, skills preserved");
        check(cleanedPet.get(2).equals(call("clean",saved)),"Pet equipment cleaned");
        check(call("cleanPet","[]").equals("[]"),"No pet preserved");
        check(call("clean",saved.replace("1220","1221")).equals(saved.replace("1220","1221")),"No match returns exact original bytes");
        reject("null");reject("[");reject("{}");reject("[[1220,1,[],0,9]]");
        reject("[[1150,1,[[93]],123,0,0]]");reject("[[1150,1,\"broken\",123]]");
        reject("[[1150,1.5,[],123]]");reject("[[-1,5,[],123]]");reject("[[1150,1,[],123.5]]");
        reject("[[1150,1,[[93,1.5]],123]]");reject("[[1150,1,[],123],null]");
        reject("[[1150,2,[],123,0,0]]");reject("[[1150,1,[[256,1]],123,0,0]]");
        System.out.println("EventCleanupCodecTest passed");
    }
}
