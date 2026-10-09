package nro.models.boss.luyen_tap_tu_dong;

import nro.models.boss.BossID;
import nro.models.consts.BossStatus;
import nro.models.boss.BossesData;
import static nro.models.consts.BossType.PHOBAN;
import nro.models.services.WhisWeeklyService;
import nro.models.services.WhisFightGuard;
import nro.models.player.Player;
import nro.models.services.Service;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.Logger;
import nro.models.utils.Util;

public class Whis extends TrainingBoss {

    private final Object fightToken;
    private boolean victoryHandled;
    private volatile boolean ready;

    public Whis(Player player) throws Exception {
        this(player, reserveOwner(player));
    }

    private static Object reserveOwner(Player player) {
        if (player == null) return null;
        Object token = WhisFightGuard.reserve(player.id);
        if (token == null) throw new IllegalStateException("Whis fight already active");
        return token;
    }

    private Whis(Player player, Object token) throws Exception {
        super(PHOBAN, BossID.WHIS, BossesData.WHIS);
        fightToken = token;
        playerAtt = player;
        try {
            if (player != null) Service.gI().setPos0(player, 488, 360);
            ready = true;
        } catch (RuntimeException failure) {
            nro.models.boss.Boss_Manager.OtherBossManager.gI().removeBoss(this);
            if (player != null) WhisFightGuard.release(player.id, token);
            throw failure;
        }
    }

    @Override
    public void update() { if (ready) super.update(); }

    public static Whis start(Player player) throws Exception {
        if (player == null || player.zone == null || player.zone.map.mapId != 154 || player.isDie()) return null;
        Object token = WhisFightGuard.reserve(player.id);
        if (token == null) {
            Service.gI().sendThongBao(player, "Bạn đang có một trận Whis, hãy kết thúc trận đó trước.");
            return null;
        }
        try { return new Whis(player, token); }
        catch (Exception failure) { WhisFightGuard.release(player.id, token); throw failure; }
    }

    private long lastJoinMapTime;
    private boolean isTele;
    private boolean isChatS;
    private long level;

    @Override
    public void joinMap() {
        if (playerAtt != null && playerAtt.zone != null && playerAtt.zone.map.mapId == 154 && !playerAtt.beforeDispose) {
            this.zone = playerAtt.zone;
            this.level = Math.max(1L, (long) playerAtt.traning.getTop() + 1L);
            this.nPoint.hpMax = (int) Math.min(Integer.MAX_VALUE, (long) this.nPoint.hpMax * level);
            this.nPoint.hp = this.nPoint.hpMax;
            this.nPoint.dame = (int) Math.min(Integer.MAX_VALUE, (long) this.nPoint.dame * level);
            nro.models.services.BossAdminConfig.applyAfterNativeSpawn(this);
            this.name = "Whis [LV:" + level + "]";
            ChangeMapService.gI().changeMap(this, this.zone, 725, 312);
            this.chat("Ta sẽ dạy ngươi vài chiêu");
            this.changeStatus(BossStatus.AFK);
            lastTimeAFK = System.currentTimeMillis();
            lastJoinMapTime = System.currentTimeMillis();
        } else { leaveMap(); }
    }

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
            this.lastTimeChatS = System.currentTimeMillis();
            this.timeChatS = 2000;
            doneChatS = true;
        }
        return false;
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (plAtt == null || plAtt != playerAtt || isMobAttack || victoryHandled || damage <= 0) return 0;
        if(nPoint==null||nPoint.hp<=0)return 0;
        if (!piercing && Util.isTrue(400, 1000)) {
            this.chat("Xí hụt");
            return 0;
        }
        damage /= Math.max(1,level);
        this.nPoint.subHP(damage);
        if (this.nPoint.hp > 0 && this.nPoint.hp < this.nPoint.hpMax / 3) {
            if (Util.canDoWithTime(lastTimeChat, 2000)) {
                String[] text = {"AAAAAAAAA", "ai da"};
                this.chat(text[Util.nextInt(text.length)]);
            }
        }
        if (this.nPoint.hp <= 0) {
            this.setDie(plAtt);
            die(plAtt);
        }
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0L, damage));
    }

    @Override
    public synchronized void die(Player plKill) {
        if ((plKill != null && plKill != playerAtt) || victoryHandled) return;
        victoryHandled = true;
        nro.models.services.BossAdminDropService.rewardWithoutDefault(this, plKill);
        this.enterDefeatedWait();
        this.lastTimeAFK = System.currentTimeMillis();

        
        this.chatE();

        if (playerAtt != null && plKill != null) {
            Service.gI().sendPlayerVS(playerAtt, plKill, (byte) 0);
        } else if (playerAtt != null) {
            Service.gI().sendPlayerVS(playerAtt, playerAtt, (byte) 0);
        }

        if (plKill == null || playerAtt == null || playerAtt.beforeDispose) return;
        try {
            synchronized (playerAtt) {
                WhisWeeklyService.gI().recordWin(playerAtt, (int)Math.min(Integer.MAX_VALUE,level),
                    (int)Math.min(Integer.MAX_VALUE, Math.max(0L,System.currentTimeMillis()-lastJoinMapTime)));
            }
            Service.gI().sendThongBao(playerAtt, "Bạn thắng Whis: +1 điểm bảng xếp hạng tuần.");
        } catch (Exception e) {
            Logger.logException(Whis.class,e);
            Service.gI().sendThongBao(playerAtt, "Chưa lưu được điểm Whis do lỗi dữ liệu. Vui lòng báo quản trị viên.");
        }
    }

    @Override
    public synchronized void leaveMap() {
        Player owner = playerAtt;
        try { super.leaveMap(); }
        finally { if (owner != null) WhisFightGuard.release(owner.id, fightToken); }
    }

    @Override
    public boolean chatE() {
        if (Util.canDoWithTime(lastTimeChatE, timeChatE)) {
            if (this.indexChatE == 1) {
                return true;
            }
            String[] text = new String[]{"OK ta chịu thua", "Ta rất tự hào về con", "Tại hôm nay ta...ta hơi bị đau bụng", "Thua thì thua"};
            String t = text[Util.nextInt(text.length)];
            this.chat(t);
            this.lastTimeChatE = System.currentTimeMillis();
            this.timeChatE = t.length() * 100;
            if (this.timeChatE > 2000) {
                this.timeChatE = 2000;
            }
            this.indexChatE = 1;
        }
        return false;
    }

    @Override
    public void afk() {
        if (Util.canDoWithTime(lastJoinMapTime, 50) && !isTele) {
            isTele = true;
            Service.gI().setPos(this, 341, 320);
        } else if (Util.canDoWithTime(lastJoinMapTime, 500) && !isChatS) {
            isChatS = true;
            this.changeStatus(BossStatus.CHAT_S);
        } else if (Util.canDoWithTime(lastTimeAFK, 1000) && isTele && isChatS) {
            this.changeStatus(BossStatus.LEAVE_MAP);
        }
    }
}
