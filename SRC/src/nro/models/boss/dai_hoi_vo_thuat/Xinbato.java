package nro.models.boss.dai_hoi_vo_thuat;


import nro.models.boss.BossID;
import nro.models.boss.BossesData;
import static nro.models.consts.BossType.PHOBAN;
import nro.models.player.Player;

public class Xinbato extends The23rdMartialArtCongress {

    private boolean countedTitle;
    @Override public void die(Player killer) {
        if(!countedTitle && killer==playerAtt) {
            countedTitle=true;
            nro.models.task.BadgesTaskService.updateCountBagesTask(killer,8,1);
        }
        super.die(killer);
    }
    public Xinbato(Player player) throws Exception {
        super(PHOBAN, BossID.XINBATO, BossesData.XINBATO);
        this.playerAtt = player;
    }
}
