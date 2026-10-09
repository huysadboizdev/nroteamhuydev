package nro.models.services;
import nro.models.map.ItemMap;
import nro.models.mob.Mob;
import nro.models.matches.*;
import nro.models.player_system.Template.ItemTemplate;
public class AchievementHookTest {
    public static void main(String[] args) { try {
        AchievementAuditTest.fixture(); var p=AchievementAuditTest.owner();var other=AchievementAuditTest.owner();other.id++;
        ThachDau duel=new ThachDau(p,other,0){@Override public void start(){}};
        duel.sendResult(p,TYPE_LOSE_PVP.DEAD);AchievementAuditTest.check(other.achievement.getCompleted(3)==1,"second challenger winner counts");
        duel.sendResult(other,TYPE_LOSE_PVP.DEAD);duel.sendResult(other,TYPE_LOSE_PVP.DEAD);AchievementAuditTest.check(p.achievement.getCompleted(3)==1,"first challenger and duplicate opponent counted correctly");
        Mob mob=PinkFixture.blank(Mob.class);mob.lvMob=1;mob.type=4;mob.tempId=0;AchievementService.gI().checkDoneTaskKillMob(p,mob);
        for(int i:new int[]{6,7,12})AchievementAuditTest.check(p.achievement.getCompleted(i)==1,"mob hook "+i);
        ItemMap drop=PinkFixture.blank(ItemMap.class);drop.itemTemplate=new ItemTemplate();drop.itemTemplate.id=77;drop.quantity=5;
        TaskService.gI().checkDoneTaskPickItem(p,drop);AchievementAuditTest.check(p.achievement.getCompleted(15)==5,"five picked gems count five");
        System.out.println("ACHIEVEMENT_HOOK PASS both-pvp-winners/distinct/mob/gem-quantity");System.exit(0);
    } catch(Throwable error){error.printStackTrace();System.exit(1);}}
}
