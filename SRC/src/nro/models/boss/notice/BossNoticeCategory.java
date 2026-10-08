package nro.models.boss.notice;
public final class BossNoticeCategory {
 public static final int UNKNOWN=0, QUEST=1, GENERAL=2;
 private BossNoticeCategory(){}
 public static boolean valid(int category){return category==QUEST||category==GENERAL;}
}
