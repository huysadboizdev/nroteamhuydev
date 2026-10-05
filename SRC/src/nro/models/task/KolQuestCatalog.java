package nro.models.task;
import nro.models.player.KolQuestState;
public final class KolQuestCatalog  {
    private KolQuestCatalog() {
    }
    public record Reward(int vnd,int points) {
    }
    private static final long[] REQUIRED= {
        0,10000,100000,10,3,200,10,10
    }
    ;
    private static final String[] TEXT= {
        "","Hạ Mộc nhân khi dùng Tự động luyện tập","Hạ Quỷ chim ở Rừng đá Fide/Hang quỷ chim khi dùng Tự động luyện tập","Diệt đủ Fide 1 + Fide 2 + Fide 3 (turn cộng dồn)","Hạ Xên Hoàn Thiện tại Võ đài Xên Bọ Hung","Nhặt Capsule hồng ngọc (ID 722)","Hạ Black Goku","Hạ Super Black Goku"
    }
    ;
    private static void check(int id) {
        if(id<1 || id>7)throw new IllegalArgumentException("quest");
    }
    public static long required(int id) {
        check(id);
        return REQUIRED[id];
    }
    public static String description(int id) {
        check(id);
        return TEXT[id];
    }
    public static Reward reward(int id,boolean vip) {
        check(id);
        return new Reward(id*100000+(vip?50000:0),id==7?(vip?100000:50000):id*1000+(vip?500:0));
    }
    public static long progress(KolQuestState s,int id) {
        check(id);
        return switch(id) {
            case 1->s.woodenDummyKills;
            case 2->s.birdDemonKills;
            case 3->Math.min(s.fide1Kills,Math.min(s.fide2Kills,s.fide3Kills));
            case 4->s.perfectCellKills;
            case 5->s.pinkCapsulesPicked;
            case 6->s.blackGokuKills;
            default->s.superBlackGokuKills;
        }
        ;
    }
}
