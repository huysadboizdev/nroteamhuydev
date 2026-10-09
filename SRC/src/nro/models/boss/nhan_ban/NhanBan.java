package nro.models.boss.nhan_ban;


import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstPlayer;
import nro.models.consts.ConstRatio;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.matches.PKCommeson;
import nro.models.player.Player;
import nro.models.server.ServerNotify;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.services.SkillService;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.SkillUtil;
import nro.models.utils.Util;

public class NhanBan extends Boss {

    private Player playerAtt;
    private final nro.models.map.Zone challengeZone;
    private boolean removed;

    public NhanBan(Player player, BossData bossData) throws Exception {
        super(Util.createIdBossClone((int) player.id), bossData);
        this.playerAtt = player;
        this.challengeZone = nro.models.services.CommesonChallengeService.gI().origin(player);
        this.isCopy = true;
    }

    @Override
    public void rewardDefault(Player killer) {
        if(killer==playerAtt)nro.models.services.CommesonChallengeService.gI().won(playerAtt,this);
    }
    @Override
    public int injured(Player attacker,long damage,boolean piercing,boolean mob) {
        Player owner=playerAtt;if(owner==null)return 0;
        synchronized(owner){
            if(attacker!=owner || !nro.models.services.CommesonChallengeService.gI().owns(owner,this))return 0;
            return super.injured(attacker,damage,piercing,mob);
        }
    }

    @Override
    public void active() {
        if(playerAtt==null)return;
        if(nro.models.services.CommesonChallengeService.gI().preparing(playerAtt))return;
        if(playerAtt.zone!=challengeZone || !nro.models.services.CommesonChallengeService.gI().owns(playerAtt,this)){leaveMap();return;}
        if(playerAtt.pvp==null)new PKCommeson(playerAtt,this);
        this.attack();
    }

    @Override
    public void joinMap() {
        if(playerAtt==null)return;
        if(playerAtt.zone!=challengeZone || challengeZone==null){leaveMap();return;}
        this.zone = challengeZone;
        ChangeMapService.gI().changeMap(this, this.zone,
                this.playerAtt.location.x + Util.nextInt(-200, 200), this.playerAtt.location.y);
        this.changeStatus(BossStatus.CHAT_S);
    }

    private void goToXY(int x, int y, boolean isTeleport) {
        if (!isTeleport) {
            byte dir = (byte) (this.location.x - x < 0 ? 1 : -1);
            byte move = (byte) Util.nextInt(50, 100);
            PlayerService.gI().playerMove(this, this.location.x + (dir == 1 ? move : -move), y);
        } else {
            ChangeMapService.gI().changeMapYardrat(this, this.zone, x, y);
        }
    }

    private void goToPlayer(Player pl, boolean isTeleport) {
        goToXY(pl.location.x, pl.location.y, isTeleport);
    }

    @Override
    public void attack() {
        try {
            if(playerAtt==null || !nro.models.services.CommesonChallengeService.gI().owns(playerAtt,this))return;
            if (playerAtt.typePk == ConstPlayer.NON_PK) {
                PlayerService.gI().changeAndSendTypePK(playerAtt, ConstPlayer.PK_PVP);
            }
            if (this.typePk == ConstPlayer.NON_PK) {
                PlayerService.gI().changeAndSendTypePK(this, ConstPlayer.PK_PVP);
            }
            if (playerAtt.location != null && playerAtt != null && playerAtt.zone != null && this.zone != null && this.zone.equals(playerAtt.zone)) {
                if (this.isDie()) {
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                if (Util.getDistance(this, playerAtt) <= this.getRangeCanAttackWithSkillSelect()) {
                    if (Util.isTrue(15, ConstRatio.PER100) && SkillUtil.isUseSkillChuong(this)) {
                        goToXY(playerAtt.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 80)), Util.nextInt(10) % 2 == 0 ? playerAtt.location.y : playerAtt.location.y - Util.nextInt(0, 50), false);
                    }
                    SkillService.gI().useSkill(this, playerAtt, null, -1, null);
                    checkPlayerDie(playerAtt);
                } else {
                    goToPlayer(playerAtt, false);
                }
            }
        } catch (Exception ex) {
//            ex.printStackTrace();
        }
    }

    @Override
    public void die(Player killer) {
        if(killer==playerAtt){reward(killer);ServerNotify.gI().notify(killer.name+" đã đánh bại bản sao Commeson");}
        this.changeStatus(BossStatus.DIE);
    }
    @Override
    public void autoLeaveMap() {
        if(playerAtt==null || playerAtt.zone!=challengeZone){leaveMap();return;}
        nro.models.services.CommesonChallengeService.gI().update(playerAtt);
    }

    @Override
    public synchronized void leaveMap() {
        if(removed)return;
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
        BossManager.gI().removeBoss(this);
        removed=true;
        try{this.dispose();}catch(Exception e){nro.models.utils.Logger.logException(getClass(),e);}
    }
    public synchronized boolean isRemoved(){return removed;}
}
