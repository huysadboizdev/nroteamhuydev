package nro.models.player;
/**
 *
 * @author By Mr Blue
 */
public class KOLProgressData  {
    public KolQuestState v2 = new KolQuestState();
    public int kolQuestStage;
    public int kolVIPQuestStage;
    public int destronGas70CompletionCount;
    public int martialArtsTournamentWins;
    public int dailySuperHardQuestCompletionCount;
    public int bossBabyDefeatParticipationCount;
    public long monsterKillCountAutoTrain;
    private static boolean validV2Json(com.google.gson.JsonElement element) {
        if(element==null || !element.isJsonObject())return false;
        com.google.gson.JsonObject object=element.getAsJsonObject();
        try {
            if(exactNumber(object,"version",true)!=2)return false;
            long normal=exactNumber(object,"normalStage",true),vip=exactNumber(object,"vipStage",true);
            if(normal<1 || normal>8 || vip<1 || vip>8)return false;
            for(String field:new String[]{"woodenDummyKills","birdDemonKills","fide1Kills","fide2Kills","fide3Kills","perfectCellKills","pinkCapsulesPicked","blackGokuKills","superBlackGokuKills"}) {
                if(exactNumber(object,field,false)<0)return false;
            }
            return true;
        } catch(Exception malformed) { return false; }
    }
    private static long exactNumber(com.google.gson.JsonObject object,String field,boolean required) {
        if(!object.has(field)) { if(required)throw new IllegalArgumentException("missing "+field);return 0; }
        var value=object.get(field);
        if(!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())throw new IllegalArgumentException("non numeric "+field);
        return value.getAsBigDecimal().longValueExact();
    }
    public static KOLProgressData parse(String json)  {
        try  {
            if(json==null || json.isBlank()) return new KOLProgressData();
            com.google.gson.JsonObject obj=new com.google.gson.JsonParser().parse(json).getAsJsonObject();
            if(obj.has("v2") && !validV2Json(obj.get("v2"))) { obj.remove("v2");KOLProgressData d=new com.google.gson.Gson().fromJson(obj,KOLProgressData.class);d.v2=KolQuestState.invalid();return d; }
            KOLProgressData d=new com.google.gson.Gson().fromJson(obj,KOLProgressData.class);
            if(obj.has("v2") && (!obj.get("v2").isJsonObject() || !obj.getAsJsonObject("v2").has("version") || !obj.getAsJsonObject("v2").has("normalStage") || !obj.getAsJsonObject("v2").has("vipStage") || d.v2==null || !d.v2.valid())) d.v2=KolQuestState.invalid();
            if(!obj.has("v2")) d.v2=new KolQuestState();
            return d;
        }
        catch(Exception e) {
            KOLProgressData d=new KOLProgressData();
            d.v2=KolQuestState.invalid();
            return d;
        }
    }
    public static KOLProgressData snapshot(Player p,KolQuestState state)  {
        KOLProgressData d=new KOLProgressData();
        d.kolQuestStage=p.kolQuestStage;
        d.kolVIPQuestStage=p.kolVIPQuestStage;
        d.destronGas70CompletionCount=(int)p.destronGas70CompletionCount;
        d.martialArtsTournamentWins=(int)p.martialArtsTournamentWins;
        d.dailySuperHardQuestCompletionCount=(int)p.dailySuperHardQuestCompletionCount;
        d.bossBabyDefeatParticipationCount=(int)p.bossBabyDefeatParticipationCount;
        d.monsterKillCountAutoTrain=p.monsterKillCountAutoTrain;
        d.v2=state.copy();
        return d;
    }
    public KOLProgressData()  {
        this.kolQuestStage = 1;
        this.kolVIPQuestStage = 1;
        this.destronGas70CompletionCount = 0;
        this.martialArtsTournamentWins = 0;
        this.dailySuperHardQuestCompletionCount = 0;
        this.bossBabyDefeatParticipationCount = 0;
        this.monsterKillCountAutoTrain = 0;
    }
}
