package nro.models.boss.HuntingTrio;

import nro.models.boss.BossData;
import nro.models.consts.ConstPlayer;
import nro.models.skill.Skill;

public final class HuntingTrioCatalog {
    public static final int[] MAPS={39,0,1,2,3,40,7,8,9,10,41,14,15,16,17};
    private static final String[][] NAMES={{"Basil","Lavender","Bergamo"},{"Pi Láp","Mai","Su"}};
    private static final short[][][] OUTFITS={{{745,746,747,-1,-1,-1},{748,749,750,-1,-1,-1},{751,752,753,-1,-1,-1}},{{612,613,614,-1,-1,-1},{615,616,617,-1,-1,-1},{618,619,620,-1,-1,-1}}};
    private HuntingTrioCatalog(){}
    public static BossData data(int group,int member) {
        if(group<0||group>1||member<0||member>2)throw new IllegalArgumentException("group/member");
        return new BossData(NAMES[group][member],ConstPlayer.XAYDA,OUTFITS[group][member].clone(),1,new int[]{1000},MAPS.clone(),
                new int[][]{{Skill.KAMEJOKO,7,10_000},{Skill.DICH_CHUYEN_TUC_THOI,7,20_000},{Skill.GALICK,7,1000},{Skill.THAI_DUONG_HA_SAN,7,50_000}},
                new String[]{},new String[]{},new String[]{},600);
    }
    public static String chat(int group,int member) {
        return group==0 ? new String[]{"Ba anh em Sói đã có mặt!","Cùng nhau thử sức nào!","Cả ba chúng ta cùng chiến đấu!"}[member]
                : new String[]{"Tiểu đội Pi Láp đã tới!","Mai đã sẵn sàng!","Su vào trận cùng đồng đội!"}[member];
    }
}
