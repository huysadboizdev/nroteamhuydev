package nro.models.player;
import com.google.gson.Gson;
public final class KolQuestState  {
    public int version=2, normalStage=1, vipStage=1;
    public long woodenDummyKills,birdDemonKills,fide1Kills,fide2Kills,fide3Kills,perfectCellKills,pinkCapsulesPicked,blackGokuKills,superBlackGokuKills;
    public boolean valid() {
        return version==2 && normalStage>=1 && normalStage<=8 && vipStage>=1 && vipStage<=8 && woodenDummyKills>=0 && birdDemonKills>=0 && fide1Kills>=0 && fide2Kills>=0 && fide3Kills>=0 && perfectCellKills>=0 && pinkCapsulesPicked>=0 && blackGokuKills>=0 && superBlackGokuKills>=0;
    }
    public static long add(long value,long count) {
        if(value<0 || count<0)throw new IllegalArgumentException("negative counter");
        return value>Long.MAX_VALUE-count?Long.MAX_VALUE:value+count;
    }
    public KolQuestState copy() {
        return new Gson().fromJson(new Gson().toJson(this),KolQuestState.class);
    }
    public static KolQuestState invalid() {
        KolQuestState s=new KolQuestState();
        s.version=-1;
        return s;
    }
}
