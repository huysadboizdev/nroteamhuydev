package nro.models.boss.luyen_tap_tu_dong;

import nro.models.boss.BossesData;
import nro.models.consts.ConstTask;
import nro.models.player.Player;
import nro.models.services.TaskService;
import nro.models.utils.Util;
import nro.models.boss.BossID;
import nro.models.consts.BossStatus;
import static nro.models.consts.BossType.PHOBAN;
import nro.models.map.service.ChangeMapService;

public class TauPayPay extends TrainingBoss {

    public TauPayPay(Player player) throws Exception {
        super(PHOBAN, BossID.TAUPAYPAY, BossesData.TAUPAYPAY);
        this.playerAtt = player;
    }

    @Override
    public void joinMap() {
        if (playerAtt.zone != null) {
            this.zone = playerAtt.zone;
            ChangeMapService.gI().changeMapBySpaceShip(this, this.zone, 775);
            this.changeStatus(BossStatus.CHAT_S);
        }
    }

//    @Override
//    public void checkPlayerDie(Player pl) {
//        Service.gI().sendPVB(playerAtt, this, ConstPlayer.PK_PVP);
//        TaskService.gI().doneTask(pl, ConstTask.TASK_31_1);
//    }
//
//    @Override
//    public void die(Player plKill) {
//        this.changeStatus(BossStatus.LEAVE_MAP);
//        this.chatE();
//        this.lastTimeAFK = 0;
//        Service.gI().sendPlayerVS(playerAtt, null, (byte) 0);
//        boolean doneTask = TaskService.gI().doneTask(plKill, ConstTask.TASK_31_1);
//    }

    @Override
    public boolean chatS() {
        if (Util.canDoWithTime(lastTimeChatS, timeChatS)) {
            if (this.doneChatS) {
                return true;
            }
            String textChat = this.data[this.currentLevel].getTextS()[playerAtt.isThachDau ? 1 : 0];
            int prefix = Integer.parseInt(textChat.substring(1, textChat.lastIndexOf("|")));
            textChat = textChat.substring(textChat.lastIndexOf("|") + 1);
            if (!this.chat(prefix, textChat)) {
                return false;
            }
            this.moveToPlayer(playerAtt);
            this.lastTimeChatS = System.currentTimeMillis();
            this.timeChatS = 2000;
            doneChatS = true;
        }
        return false;
    }

    @Override
    public synchronized void leaveMap() {
        if (this.zone == null && this.bossStatus == BossStatus.REST) {
            return;
        }
        try {
            ChangeMapService.gI().spaceShipArrive(this, (byte) 2, ChangeMapService.DEFAULT_SPACE_SHIP);
        } finally {
            super.leaveMap();
        }
    }

    @Override
    public void buffPea() {
    }

  
    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(400, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }

            if (TaskService.gI().getIdTask(plAtt) != ConstTask.TASK_10_1) {
                return 100;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage);
            this.nPoint.subHP(damage);

            if (this.nPoint.hp > 0 && this.nPoint.hp < this.nPoint.hpMax / 5) {
                if (Util.canDoWithTime(lastTimeChat, 2000)) {
                    String[] text = {"AAAAAAAAA", "ai da"};
                    this.chat(text[Util.nextInt(text.length)]);
                }
            }

            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }

            return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, damage));
        } else {
            return 0;
        }
    }

    @Override
    public void afk() {
        if (Util.canDoWithTime(lastTimeAFK, 5000)) {
            this.changeStatus(BossStatus.LEAVE_MAP);
        }
    }
}
