package nro.models.services;

import com.google.gson.*;
import java.util.*;

/** Account entitlement watermarks, independent of the contents of a player's bag. */
public final class GiftCodeRewardDelta {
    private GiftCodeRewardDelta() {}
    public record Plan(String deltaDetail,String deliveredDetail) {}
    private record Reward(int id,int quantity,SortedMap<Integer,Integer> options) {
        JsonObject json(int count) {
            JsonObject row=new JsonObject();row.addProperty("id",id);row.addProperty("quantity",count);
            JsonArray opts=new JsonArray();
            for(var e:options.entrySet()){JsonObject op=new JsonObject();op.addProperty("id",e.getKey());op.addProperty("param",e.getValue());opts.add(op);}
            row.add("options",opts);return row;
        }
    }
    private static int integer(JsonElement value,int min,int max) {
        if(value==null||!value.isJsonPrimitive()||!value.getAsJsonPrimitive().isNumber()
                ||!value.getAsString().matches("-?[0-9]+"))throw new IllegalArgumentException("Invalid gift reward integer");
        try{int n=Integer.parseInt(value.getAsString());if(n<min||n>max)throw new IllegalArgumentException("Gift reward integer out of range");return n;}
        catch(NumberFormatException bad){throw new IllegalArgumentException("Gift reward integer overflow",bad);}
    }
    private static LinkedHashMap<Integer,Reward> parse(String raw,boolean current) {
        if(raw==null)throw new IllegalArgumentException("Giftcode has no historical reward baseline");
        LinkedHashMap<Integer,Reward> result=new LinkedHashMap<>();
        try{
            JsonElement root=new JsonParser().parse(raw);
            if(!root.isJsonArray()||current&&(root.getAsJsonArray().size()==0||root.getAsJsonArray().size()>200))throw new IllegalArgumentException("Invalid gift reward list");
            for(JsonElement element:root.getAsJsonArray()) {
                if(!element.isJsonObject())throw new IllegalArgumentException("Invalid gift reward");
                JsonObject row=element.getAsJsonObject();int id=integer(row.get("id"),-3,32767),quantity=integer(row.get("quantity"),1,Integer.MAX_VALUE);
                SortedMap<Integer,Integer> opts=new TreeMap<>();JsonElement options=row.get("options");
                if(options!=null){
                    if(!options.isJsonArray()||options.getAsJsonArray().size()>100)throw new IllegalArgumentException("Invalid gift option list");
                    for(JsonElement v:options.getAsJsonArray()){
                        if(!v.isJsonObject())throw new IllegalArgumentException("Invalid gift option");
                        int key=integer(v.getAsJsonObject().get("id"),0,32767),param=integer(v.getAsJsonObject().get("param"),0,Integer.MAX_VALUE);
                        if(opts.putIfAbsent(key,param)!=null)throw new IllegalArgumentException("Duplicate gift option");
                    }
                }
                if(result.putIfAbsent(id,new Reward(id,quantity,opts))!=null)throw new IllegalArgumentException("Duplicate gift reward");
            }
            return result;
        }catch(JsonParseException|IllegalStateException bad){throw new IllegalArgumentException("Invalid gift reward JSON",bad);}
    }
    public static Plan calculate(String current,String delivered) {
        var desired=parse(current,true);var watermark=parse(delivered,false);JsonArray delta=new JsonArray();
        for(Reward item:desired.values()){
            Reward prior=watermark.get(item.id());
            if(prior!=null&&!prior.options().equals(item.options()))throw new IllegalArgumentException("Cannot change options of a previously delivered reward: "+item.id());
            int count=prior==null?item.quantity():Math.max(0,item.quantity()-prior.quantity());
            if(count>0){delta.add(item.json(count));watermark.put(item.id(),item);}
        }
        JsonArray next=new JsonArray();for(Reward item:watermark.values())next.add(item.json(item.quantity()));
        return new Plan(delta.toString(),next.toString());
    }
}
