package nro.models.services.shenron;

import nro.models.services.shenron.SummonDragon;
import nro.models.network.Message;
import nro.models.consts.ConstNpc;
import java.util.List;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.map.service.NpcService;
import nro.models.services.Service;
import nro.models.utils.Util;

/**
 *
 * @author By Mr Blue
 *
 */
public class SummonDragonNamek {

    public static final byte DRAGON_PORUNGA = 1;
    private static volatile SummonDragonNamek instance;

    public static final byte WISHED = 0;
    public static final byte TIME_UP = 1;
    private volatile boolean isShenronAppear;
    public volatile Player playerSummonShenron;
    private long playerSummonShenronId;
    private Zone mapShenronAppear;
    private int menuShenron;
    private byte select;
    private final Thread update;
    private boolean active;
    public volatile boolean isPlayerDisconnect;
    private long lastTimeShenronWait;
    private static final int timeShenronWait = 300000;
    private NamekWishRewards.Session rewardSession;
    private java.util.Set<Long> recipientsAtSummon;
    private NamekWishRewards rewards = new NamekWishRewards();

    public static synchronized SummonDragonNamek gI() {
        if (instance == null) {
            instance = new SummonDragonNamek();
        }
        return instance;
    }

    private SummonDragonNamek() {
        this.update = new Thread(() -> {
            while (active) {
                try {
                    synchronized (this) { if (isShenronAppear) {
                        if(rewardSession != null && rewardSession.uncertain
                                && rewards.grant(rewardSession) == NamekWishRewards.Result.SUCCESS) {
                            shenronLeave(playerSummonShenron,WISHED);
                            continue;
                        }
                        if (isPlayerDisconnect) {
                            List<Player> players;
                            synchronized(mapShenronAppear){players = new java.util.ArrayList<>(mapShenronAppear.getPlayers());}
                            for (Player plMap : players) {
                                if (plMap.isPl() && plMap.id == playerSummonShenronId) {
                                    playerSummonShenron = plMap;
                                    reSummonShenron();
                                    isPlayerDisconnect = false;
                                    break;
                                }
                            }

                        }
                        if ((rewardSession == null || !rewardSession.uncertain) && Util.canDoWithTime(lastTimeShenronWait, timeShenronWait)) {
                            shenronLeave(playerSummonShenron, TIME_UP);
                        }
                    }
                    } Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        this.active();
    }

    private void active() {
        if (!active) {
            active = true;
            this.update.start();
        }
    }

    public synchronized boolean canSummon(Player pl) {
        return !nro.models.server.Maintenance.isRunning && !isShenronAppear
                && (rewardSession == null || rewardSession.completed) && pl != null && pl.isPl()
                && !pl.beforeDispose && pl.zone != null && pl.zone.map.mapId == 7;
    }

    public synchronized boolean summonNamec(Player pl) {
        if (!canSummon(pl)) return false;
        {
            java.util.Set<Long> recipients = new java.util.TreeSet<>();
            if(pl.clan != null) {
                nro.models.clan.Clan clan=pl.clan;
                synchronized(clan){for(var member:new java.util.ArrayList<>(clan.members))recipients.add((long)member.id);}
            }
            recipients.add(pl.id);this.recipientsAtSummon=java.util.Set.copyOf(recipients);
            playerSummonShenron = pl;
            playerSummonShenronId = pl.id;
            this.menuShenron = -1; this.select = -1; this.rewardSession = null;
            this.isShenronAppear = true;
            mapShenronAppear = pl.zone;
            lastTimeShenronWait = System.currentTimeMillis();
            sendNotifyShenronNamekAppear();
            activeShenron(pl, true, DRAGON_PORUNGA);
            sendBlackGokuhesNamec(pl);
        }
        return true;
    }

    private void reSummonShenron() {
        activeShenron(playerSummonShenron, true, DRAGON_PORUNGA);
        sendBlackGokuhesNamec(playerSummonShenron);
    }

    private void activeShenron(Player pl, boolean appear, byte type) {
        Message msg;
        try {
            msg = new Message(-83);
            msg.writer().writeByte(appear ? 0 : (byte) 1);
            if (appear) {
                msg.writer().writeShort(pl.zone.map.mapId);
                msg.writer().writeShort(pl.zone.map.bgId);
                msg.writer().writeByte(pl.zone.zoneId);
                msg.writer().writeInt((int) pl.id);
                msg.writer().writeUTF("null");
                msg.writer().writeShort(pl.location.x);
                msg.writer().writeShort(pl.location.y);
                msg.writer().writeByte(type);
                isShenronAppear = true;
            }
            Service.gI().sendMessAllPlayer(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendNotifyShenronNamekAppear() {
        Message msg = null;
        try {
            msg = new Message(-25);
            msg.writer().writeUTF(playerSummonShenron.name + " vừa gọi rồng thần namek tại "
                    + playerSummonShenron.zone.map.mapName + " khu vực " + playerSummonShenron.zone.zoneId);
            Service.gI().sendMessAllPlayerIgnoreMe(playerSummonShenron, msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    private boolean owner(Player pl) {
        return isShenronAppear && pl != null && pl.isPl() && pl.id == playerSummonShenronId
                && pl == playerSummonShenron && pl.zone == mapShenronAppear && !pl.beforeDispose
                && (rewardSession != null && rewardSession.uncertain
                    || !Util.canDoWithTime(lastTimeShenronWait,timeShenronWait));
    }

    public synchronized void confirmWish(Player actor) {
        if (!owner(actor) || menuShenron != ConstNpc.SHOW_SHENRON_NAMEK_CONFIRM || select < 0 || select > 1) return;
        if(nro.models.server.Maintenance.isRunning && (rewardSession == null || !rewardSession.uncertain))return;
        if (rewardSession == null) {
            if(recipientsAtSummon == null)return;
            rewardSession = new NamekWishRewards.Session(select, recipientsAtSummon);
        }
        NamekWishRewards.Result result;
        try { result = rewards.grant(rewardSession); }
        catch(Exception failure) {
            nro.models.utils.Logger.logException(SummonDragonNamek.class,failure);
            Service.gI().sendThongBao(actor,"Chưa thể thực hiện điều ước. Vui lòng thử lại.");return;
        }
        if(result == NamekWishRewards.Result.SUCCESS) shenronLeave(actor,WISHED);
        else Service.gI().sendThongBao(actor,result==NamekWishRewards.Result.BLOCKED
                ? "Điều ước chưa thực hiện: thành viên cần chỗ trống hành trang và không có giao dịch đang chờ."
                : "Điều ước đang chờ đối chiếu dữ liệu. Vui lòng thử lại cùng lựa chọn.");
    }

    public synchronized void showConfirmShenron(Player pl,int menu,byte choice) {
        if(!owner(pl) || menu != ConstNpc.SHOW_SHENRON_NAMEK_CONFIRM || choice < 0 || choice > 1
                || rewardSession != null && rewardSession.uncertain && rewardSession.wish != choice) return;
        if(rewardSession != null && rewardSession.wish != choice) rewardSession=null;
        this.menuShenron=menu;this.select=choice;
        NpcService.gI().createMenuRongThieng(pl,ConstNpc.SHENRON_NAMEK_CONFIRM,"Ngươi có chắc muốn ước?",
                choice==0?"1-20 viên ngọc rồng 3 sao":"pet hổ sề béo","Từ chối");
    }

    public synchronized void sendBlackGokuhesNamec(Player pl) {
        if(!owner(pl))return;
        NpcService.gI().createMenuRongThieng(pl,ConstNpc.SHOW_SHENRON_NAMEK_CONFIRM,
                "Ta sẽ ban cho cả bang hội ngươi 1 điều ước, ngươi có 5 phút, hãy suy nghĩ thật kỹ trước khi quyết định",
                "1-20 viên ngọc rồng 3 sao","pet hổ sề béo");
    }

    public static boolean reconcilePendingForMaintenance() {
        SummonDragonNamek dragon=instance;
        if(dragon==null)return true;
        synchronized(dragon){
            if(dragon.rewardSession==null||!dragon.rewardSession.uncertain)return true;
            if(dragon.rewards.grant(dragon.rewardSession)!=NamekWishRewards.Result.SUCCESS)return false;
            dragon.shenronLeave(dragon.playerSummonShenron,WISHED);return true;
        }
    }

    public synchronized void shenronLeave(Player pl, byte type) {
        if(!isShenronAppear || pl == null || pl.id != playerSummonShenronId
                || type == WISHED && (rewardSession == null || !rewardSession.completed)
                || rewardSession != null && rewardSession.uncertain) return;
        try { if (type == WISHED) {
            //Điều ước Bùa mạnh mẽ cho tất cả trong 7 ngày của các con đã được thực hiện...tạm biệt
            NpcService.gI().createTutorial(pl, 0, "Điều ước của ngươi đã được thực hiện...tạm biệt");
        } else {
            NpcService.gI().createMenuRongThieng(pl, ConstNpc.IGNORE_MENU, "Ta buồn ngủ quá rồi\nHẹn gặp ngươi lần sau, ta đi đây, bái bai");
        }
        activeShenron(pl, false, SummonDragon.DRAGON_SHENRON);
        } finally {
        this.isShenronAppear = false;
        this.menuShenron = -1;
        this.select = -1;
        this.playerSummonShenron = null;
        this.playerSummonShenronId = -1;
        this.mapShenronAppear = null;
        this.rewardSession = null;
        this.recipientsAtSummon = null;
        }
    }
}
