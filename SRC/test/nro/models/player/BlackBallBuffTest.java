package nro.models.player;

import java.util.Arrays;
import nro.models.intrinsic.Intrinsic;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.services.PinkFixture;
import nro.models.task.TaskMain;

public final class BlackBallBuffTest {
    private static Player player() throws Exception {
        Player p = new Player() { @Override public void sendMessage(Message m) {} };
        p.idNRNM = -1;
        p.playerIntrinsic.intrinsic = new Intrinsic();
        p.playerTask.taskMain = new TaskMain(); p.playerTask.taskMain.id = -1;
        p.nPoint.hpg = 100_000; p.nPoint.mpg = 100_000; p.nPoint.dameg = 1_000;
        p.nPoint.limitPower = 9; p.nPoint.power = 90_010_000_000L;
        for (int i = 0; i < 12; i++) p.inventory.itemsBody.add(new Item());
        p.zone = PinkFixture.zone();
        return p;
    }
    public static void main(String[] args) throws Exception {
        PinkFixture.install();
        Player p = player();
        p.nPoint.calPoint();
        int hp = p.nPoint.hpMax, ki = p.nPoint.mpMax, attack = p.nPoint.dame;
        for (int star = 1; star <= 7; star++) {
            Arrays.fill(p.rewardBlackBall.timeOutOfDateReward, 0);
            p.rewardBlackBall.timeOutOfDateReward[star - 1] = System.currentTimeMillis() + 60_000;
            for (int i = 0; i < 3; i++) {
                p.nPoint.calPoint();
                PinkFixture.check(p.nPoint.hpMax == (star == 2 ? hp * 135L / 100 : hp), "HP star " + star);
                PinkFixture.check(p.nPoint.mpMax == (star == 3 ? ki * 135L / 100 : ki), "KI star " + star);
                PinkFixture.check(p.nPoint.dame == (star == 1 ? attack * 121L / 100 : attack), "Attack star " + star);
                PinkFixture.check(p.nPoint.tlHutHp == (star == 5 ? 35 : 0), "HP leech star " + star);
                PinkFixture.check(p.nPoint.tlSDCM == 0 && p.nPoint.tlDameCrit.isEmpty(), "No critical damage buff star " + star);
                PinkFixture.check(p.nPoint.tlPST == (star == 4 ? 35 : 0), "Reflect star " + star);
                PinkFixture.check(p.nPoint.tlGiap == (star == 6 ? 20 : 0), "Armor star " + star);
                PinkFixture.check(p.nPoint.tlNeDon == (star == 7 ? 14 : 0), "Dodge star " + star);
            }
        }
        Arrays.fill(p.rewardBlackBall.timeOutOfDateReward, System.currentTimeMillis() + 60_000);
        p.nPoint.calPoint();
        PinkFixture.check(p.nPoint.mpMax == ki * 135L / 100 && p.nPoint.tlHutHp == 35 && p.nPoint.tlGiap == 20,
                "All stars combine without extra KI or repeated leech");
        Arrays.fill(p.rewardBlackBall.timeOutOfDateReward, System.currentTimeMillis() - 1);
        p.nPoint.calPoint();
        PinkFixture.check(p.nPoint.mpMax == ki && p.nPoint.hpMax == hp && p.nPoint.tlGiap == 0 && p.nPoint.tlHutHp == 0,
                "Expired buffs are removed");
        p.rewardBlackBall.timeOutOfDateReward[5] = System.currentTimeMillis() + 60_000;
        p.nPoint.calPoint(); p.nPoint.hp = hp;
        PinkFixture.check(p.injured(null, 10_000, false, true) == 8_000, "Six-star armor reduces normal hit by 20%");
        p.nPoint.hp = hp;
        PinkFixture.check(p.injured(null, 10_000, true, true) == 10_000, "Piercing bypasses existing armor");
        p.nPoint.hp = hp; p.nPoint.tlGiap = 100;
        PinkFixture.check(p.injured(null, 10_000, false, true) == 1_400, "Total armor retains 86% cap");
        System.out.println("BLACK_BALL_BUFF PASS 7 stars, repeated refresh, expiry, combined buffs and armor damage");
    }
}
