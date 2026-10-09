package nro.models.services;

import com.google.gson.*;
import java.lang.reflect.*;

/** Pure reward entitlement fixtures; never connects to a real player/database. */
public final class GiftCodeRewardDeltaTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static Object calculate(String current,String delivered)throws Exception {
        try{return Class.forName("nro.models.services.GiftCodeRewardDelta").getMethod("calculate",String.class,String.class).invoke(null,current,delivered);}
        catch(InvocationTargetException e){if(e.getCause() instanceof Exception x)throw x;throw e;}
    }
    static JsonArray value(Object plan,String name)throws Exception {
        return new JsonParser().parse((String)plan.getClass().getMethod(name).invoke(plan)).getAsJsonArray();
    }
    static String item(int id,int quantity,String options){return "{\"id\":"+id+",\"quantity\":"+quantity+",\"options\":"+options+"}";}
    static void invalid(String current,String delivered)throws Exception {
        try{calculate(current,delivered);throw new AssertionError("Invalid entitlement accepted");}catch(IllegalArgumentException expected){}
    }
    public static void main(String[] args)throws Exception {
        String opts="[{\"id\":50,\"param\":20},{\"id\":30,\"param\":0}]";
        String a=item(457,10,opts),b=item(933,3,"[]");
        Object first=calculate("["+a+"]","[]");check(value(first,"deltaDetail").size()==1,"first grant missing");
        Object update=calculate("["+item(457,15,opts)+","+b+"]","["+a+"]");
        JsonArray delta=value(update,"deltaDetail");check(delta.size()==2&&delta.get(0).getAsJsonObject().get("quantity").getAsInt()==5,"increase must deliver 5 plus new item");
        Object repeat=calculate("["+item(457,15,opts)+","+b+"]",value(update,"deliveredDetail").toString());check(value(repeat,"deltaDetail").size()==0,"repeat regranted");
        Object lower=calculate("["+item(457,2,opts)+"]",value(update,"deliveredDetail").toString());
        check(value(lower,"deltaDetail").size()==0&&value(lower,"deliveredDetail").size()==2,"decrease/removal lost watermark");
        Object readd=calculate("["+item(457,15,opts)+","+b+"]",value(lower,"deliveredDetail").toString());check(value(readd,"deltaDetail").size()==0,"readding old rewards regranted");
        String reversed="[{\"id\":30,\"param\":0},{\"id\":50,\"param\":20}]";
        check(value(calculate("["+item(457,11,reversed)+"]","["+a+"]"),"deltaDetail").get(0).getAsJsonObject().get("quantity").getAsInt()==1,"option order changed identity");
        invalid("["+item(457,15,"[]")+"]","["+a+"]");
        invalid("["+a+","+a+"]","[]");invalid("["+item(457,0,opts)+"]","[]");
        invalid("["+item(457,1,"[{\"id\":0,\"param\":0},{\"id\":0,\"param\":1}]")+"]","[]");
        invalid("["+item(457,1,"[{\"id\":0,\"param\":-1}]")+"]","[]");
        invalid("["+a+"]",null);invalid("[]","[]");
        invalid("[{\"id\":457,\"quantity\":1.0}]","[]");
        check(value(calculate("["+item(0,2147483647,"[{\"id\":0,\"param\":0}]")+"]","[]"),"deltaDetail").size()==1,"zero ID/param rejected");
        System.out.println("GiftCodeRewardDeltaTest PASS");
    }
}
