package nro.models.boss;

import nro.models.boss.Boss_Manager.BrolyManager;
import nro.models.boss.Boss_Manager.LunarNewYearEventManager;
import nro.models.boss.Boss_Manager.GasDestroyManager;
import nro.models.boss.Boss_Manager.RedRibbonHQManager;
import nro.models.boss.Boss_Manager.SkillSummonedManager;
import nro.models.boss.Boss_Manager.TreasureUnderSeaManager;
import nro.models.boss.Boss_Manager.FinalBossManager;
import nro.models.boss.Boss_Manager.HalloweenEventManager;
import nro.models.boss.Boss_Manager.ChristmasEventManager;
import nro.models.boss.Boss_Manager.TrungThuEventManager;
import nro.models.boss.Boss_Manager.YardartManager;
import nro.models.boss.Boss_Manager.SnakeWayManager;
import nro.models.boss.Boss_Manager.HungVuongEventManager;
import nro.models.boss.Boss_Manager.OtherBossManager;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.consts.AppearType;
import nro.models.consts.BossStatus;
import nro.models.consts.BossType;
import nro.models.consts.ConstPlayer;
import static nro.models.consts.BossStatus.ACTIVE;
import static nro.models.consts.BossStatus.AFK;
import static nro.models.consts.BossStatus.CHAT_E;
import static nro.models.consts.BossStatus.CHAT_S;
import static nro.models.consts.BossStatus.DIE;
import static nro.models.consts.BossStatus.JOIN_MAP;
import static nro.models.consts.BossStatus.LEAVE_MAP;
import static nro.models.consts.BossStatus.RESPAWN;
import static nro.models.consts.BossStatus.REST;
import static nro.models.consts.BossType.BROLY;
import static nro.models.consts.BossType.CHRISTMAS_EVENT;
import static nro.models.consts.BossType.FINAL;
import static nro.models.consts.BossType.HALLOWEEN_EVENT;
import static nro.models.consts.BossType.HUNGVUONG_EVENT;
import static nro.models.consts.BossType.PHOBAN;
import static nro.models.consts.BossType.PHOBANBDKB;
import static nro.models.consts.BossType.PHOBANCDRD;
import static nro.models.consts.BossType.PHOBANDT;
import static nro.models.consts.BossType.PHOBANKGHD;
import static nro.models.consts.BossType.SKILLSUMMONED;
import static nro.models.consts.BossType.TET_EVENT;
import static nro.models.consts.BossType.TRUNGTHU_EVENT;
import static nro.models.consts.BossType.YARDART;
import nro.models.network.Message;
import java.util.List;
import nro.models.map.Zone;
import nro.models.mob.Mob;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.skill.Skill;
import nro.models.server.ServerNotify;
import nro.models.map.service.MapService;
import nro.models.services.BossNoticeService;
import nro.models.services.PlayerService;
import nro.models.services.Service;
import nro.models.services.SkillService;
import nro.models.services.TaskService;
import nro.models.map.service.ChangeMapService;
import nro.models.utils.Logger;
import nro.models.utils.SkillUtil;
import nro.models.utils.Util;
import nro.models.interfaces.IBoss;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Boss extends Player implements IBoss {
    public long adminLife, adminAppliedRevision = -1;
    public int adminDefaultHp, adminDefaultDamage, adminDefaultDef, adminDefaultCrit;
    public boolean adminAliveMode;
    public volatile nro.models.services.BossAdminConfig.Config adminStats;
    private boolean questSupport, questSupportCredit;
    private volatile boolean questSupportRetired;
    private int questSupportSemanticId;
    private volatile boolean questSupportCombat;
    public final void setQuestSupportIdentity(int semanticId, int networkId) {
        if (questSupport) throw new IllegalStateException("Identity already assigned");
        questSupport = true; questSupportSemanticId = semanticId; this.id = networkId;
    }
    public final boolean isQuestSupport() { return questSupport; }
    public final int questBossId() { return questSupport ? questSupportSemanticId : (int) id; }
    public final boolean isQuestSupportRetired() { return questSupportRetired; }
    public final synchronized boolean claimQuestSupportCredit() {
        if (!questSupport || questSupportRetired || questSupportCredit || currentLevel < 0
                || nPoint == null || nPoint.hp > 0) return false;
        questSupportCredit = true; return true;
    }
    public final void markQuestSupportCombat(Player attacker, long damage) {
        if (questSupport && attacker != null && !attacker.isBoss && damage > 0) questSupportCombat = true;
    }
    public final boolean hadQuestSupportCombat() { return questSupportCombat; }
    public final void clearQuestSupportCombat() { questSupportCombat = false; }
    public final Boss questParent() { return parentBoss; }
    public final long questLastRest() { return lastTimeRest; }
    public final int questBaseRestSeconds() { return secondsRest; }
    public final synchronized void retireQuestSupport() {
        if (!questSupport) return;
        questSupportRetired = true; bossStatus = BossStatus.REST;
        try {
            if (zone != null) ChangeMapService.gI().exitMap(this);
        } finally {
            zoneFinal = null; lastZone = null; parentBoss = null; playerTarger = null; playerReward = null;
            bossAppearTogether = new Boss[0][];
            nro.models.services.BossNoticeService.gI().departed(this);
        }
    }
    /** Direct reward callbacks use the same task-only guard as the normal death path. */
    protected final boolean questSupportReward(Player killer) {
        if (!questSupport) return false;
        TaskService.gI().checkDoneTaskKillBoss(killer, this); return true;
    }

    private volatile String seasonalEvent;
    private volatile boolean seasonalRetired;
    public final void bindSeasonalEvent(String key) {
        if (seasonalEvent != null && !seasonalEvent.equals(key)) throw new IllegalStateException("Seasonal ownership changed");
        seasonalEvent = key;
    }
    public final boolean seasonalAllowed() {
        return !seasonalRetired && (seasonalEvent == null || nro.models.event.EventItemPolicy.isEnabled(seasonalEvent));
    }
    public final synchronized void retireSeasonalEvent() {
        seasonalRetired = true;
        bossStatus = BossStatus.REST;
        prepareBom = false;
        if (zone != null) ChangeMapService.gI().exitMap(this);
        zoneFinal = null; lastZone = null; playerTarger = null; playerReward = null;
        nro.models.services.BossNoticeService.gI().departed(this);
    }

    public int currentLevel = -1;
    public final BossData[] data;

    public volatile BossStatus bossStatus;
    private boolean defeatedWait;
    private boolean defeatedSinceSpawn,defeatConsumed;
    private long deathPhaseStarted,lastLifecycleError;

    /** Explicit post-defeat timer used by training and scheduled revival bosses. */
    public final void enterDefeatedWait(){synchronized(this){defeatedWait=true;bossStatus=AFK;}}
    /** Never retries rewards; only recovers the state machine after interrupted death work. */
    public final void ensureDeathTransition(){
        boolean recovered=false;
        synchronized(this){
            if(nPoint==null||nPoint.hp>0){defeatedWait=false;return;}
            defeatedSinceSpawn=true;
            if(!defeatedWait&&(bossStatus==ACTIVE||bossStatus==AFK||bossStatus==CHAT_S)){bossStatus=DIE;deathPhaseStarted=System.currentTimeMillis();recovered=true;}
        }
        if(recovered)nro.models.services.BossNoticeService.gI().defeated(this,nro.models.services.BossNoticeService.deathAttacker(this));
    }
    /** Match outcome survives disposal; each spawned opponent can award one win. */
    public final synchronized boolean consumeDefeat(){
        if(defeatConsumed||(!defeatedSinceSpawn&&(nPoint==null||nPoint.hp>0)))return false;
        defeatConsumed=true;return true;
    }
    public final void reportLifecycleError(Exception failure){
        long now=System.currentTimeMillis();if(now-lastLifecycleError>30_000){lastLifecycleError=now;Logger.logException(getClass(),failure,"Boss lifecycle: "+id+" "+name+" "+bossStatus);}
    }
    public final boolean hasCompanionGroup(){Boss[][] groups=bossAppearTogether;int level=currentLevel;return groups!=null&&level>=0&&level<groups.length&&groups[level]!=null;}
    public final Boss[] getCompanionBosses(){
        Boss[][] groups=bossAppearTogether;int level=currentLevel;
        if(groups==null||level<0||level>=groups.length||groups[level]==null)return new Boss[0];
        return groups[level].clone();
    }
    /** Death cleanup is independent of charging/skill effects or unavailable chat speakers. */
    protected final boolean updateDeathLifecycle(){
        ensureDeathTransition();
        BossStatus phase;synchronized(this){phase=bossStatus;}
        if(phase==DIE){changeStatus(CHAT_E);return true;}
        if(phase==CHAT_E){
            boolean finished=false;
            try{finished=chatE()||System.currentTimeMillis()-deathPhaseStarted>=10_000;}
            catch(Exception failure){reportLifecycleError(failure);finished=true;}
            if(finished){try{doneChatE();}finally{changeStatus(LEAVE_MAP);}}return true;
        }
        if(phase==LEAVE_MAP){leaveMap();return true;}return false;
    }

    protected Zone lastZone;

    protected long lastTimeRest;
    protected int secondsRest;

    protected long lastTimeChatS;
    protected int timeChatS;
    protected byte indexChatS;

    protected long lastTimeChatE;
    protected int timeChatE;
    protected byte indexChatE;

    protected long lastTimeChatM;
    protected int timeChatM;

    protected long lastTimeTargetPlayer;
    protected int timeTargetPlayer;
    protected Player playerTarger;

    protected Boss parentBoss;
    public Boss[][] bossAppearTogether;

    public Zone zoneFinal = null;

    public Player playerReward;

    public int lv;

    public int error;

    public boolean prepareBom;

    public boolean isNotifyDisabled;
    public boolean isZone01SpawnDisabled;
    private BossType noticeBossType;

    public boolean noticePublic() {
        if (questSupport && questSupportRetired) return false;
        if(nro.models.services.BossAnnouncementPolicy.excluded(this)||zone==null||zone.map==null)return false;
        if(noticeBossType==BossType.PLAYERAO||noticeBossType==BossType.CADIC||noticeBossType==BossType.MANAGER_DEBUG
                ||this instanceof nro.models.boss.luyen_tap_tu_dong.TrainingBoss||this instanceof nro.models.boss.sieu_hang.SuperRank
                ||this instanceof nro.models.boss.dai_hoi_vo_thuat.The23rdMartialArtCongress
                ||noticeBossType==YARDART||noticeBossType==SKILLSUMMONED)return false;
        int map=zone.map.mapId;
        if(map==140||map==111)return false;
        if(nro.models.services.BossAnnouncementPolicy.equipment(this))return true;
        if(isNotifyDisabled||noticeBossType==PHOBAN||noticeBossType==PHOBANDT||noticeBossType==PHOBANBDKB
                ||noticeBossType==PHOBANCDRD||noticeBossType==PHOBANKGHD)return false;
        return map!=140&&map!=111&&!MapService.gI().isMapPhoBan(map)&&!MapService.gI().isMapMaBu(map)&&!MapService.gI().isMapBlackBallWar(map);
    }
    public long noticeNextSpawnAt() {
        try {
            int next=(currentLevel+1)%data.length;
            if(bossStatus!=REST||parentBoss!=null||getClass().getMethod("rest").getDeclaringClass()!=Boss.class
                    ||data[next].getTypeAppear()!=AppearType.DEFAULT_APPEAR||lastTimeRest<=0)return 0;
            return Math.max(0L, nro.models.services.QuestSupportService.gI().originalRespawnAt(this));
        }catch(Exception ignored){return 0;}
    }
    @Override protected void setDie(Player killer) {
        nro.models.services.BossNoticeService.gI().defeated(this,killer);
        try{super.setDie(killer);}finally{ensureDeathTransition();}
    }

    public Boss(int id, boolean isNotifyDisabled, boolean isZone01SpawnDisabled, BossData... data) throws Exception {
        this(id, data);
        this.isNotifyDisabled = isNotifyDisabled;
        this.isZone01SpawnDisabled = isZone01SpawnDisabled;
    }

    public Boss(BossType bossType, int id, boolean isNotifyDisabled, boolean isZone01SpawnDisabled, BossData... data) throws Exception {
        this(bossType, id, data);
        this.isNotifyDisabled = isNotifyDisabled;
        this.isZone01SpawnDisabled = isZone01SpawnDisabled;
    }

    public Boss(int id, BossData... data) throws Exception {
        this.id = id;
        QuestSupportIdentity.initialize(this, id);
        this.isBoss = true;
        if (data == null || data.length == 0) {
            throw new Exception("Dữ liệu boss không hợp lệ");
        }
        this.data = data;
        this.secondsRest = this.data[0].getSecondsRest();
        this.bossStatus = BossStatus.REST;
        BossManager.gI().addBoss(this);

        this.bossAppearTogether = new Boss[this.data.length][];
        for (int i = 0; i < this.bossAppearTogether.length; i++) {
            if (this.data[i].getBossesAppearTogether() != null) {
                this.bossAppearTogether[i] = new Boss[this.data[i].getBossesAppearTogether().length];
                for (int j = 0; j < this.data[i].getBossesAppearTogether().length; j++) {
                    Boss boss = BossManager.gI().createBoss(this.data[i].getBossesAppearTogether()[j]);
                    if (boss != null) {
                        boss.parentBoss = this;
                        boss.lv = j;
                        this.bossAppearTogether[i][j] = boss;
                    }
                }
            }
        }
    }

    public Boss(BossType bossType, int id, BossData... data) throws Exception {
        this.noticeBossType=bossType;
        this.seasonalEvent = switch (bossType) {
            case HUNGVUONG_EVENT -> "hung_vuong";
            case HALLOWEEN_EVENT -> "halloween";
            case CHRISTMAS_EVENT -> "christmas";
            case TET_EVENT -> "lunar_new_year";
            case TRUNGTHU_EVENT -> "trung_thu";
            default -> null;
        };

        this.id = id;
        this.isBoss = true;
        if (data == null || data.length == 0) {
            throw new Exception("Dữ liệu boss không hợp lệ");
        }
        this.data = data;
        this.secondsRest = this.data[0].getSecondsRest();
        this.bossStatus = BossStatus.REST;
        switch (bossType) {
            case YARDART ->
                YardartManager.gI().addBoss(this);
            case FINAL ->
                FinalBossManager.gI().addBoss(this);
            case SKILLSUMMONED ->
                SkillSummonedManager.gI().addBoss(this);
            case BROLY ->
                BrolyManager.gI().addBoss(this);
            case PHOBAN ->
                OtherBossManager.gI().addBoss(this);
            case PHOBANDT ->
                RedRibbonHQManager.gI().addBoss(this);
            case PHOBANBDKB ->
                TreasureUnderSeaManager.gI().addBoss(this);
            case PHOBANCDRD ->
                SnakeWayManager.gI().addBoss(this);
            case PHOBANKGHD ->
                GasDestroyManager.gI().addBoss(this);
            // Runtime owns seasonal instances after the entire constructor tree returns.
            case TRUNGTHU_EVENT, HALLOWEEN_EVENT, CHRISTMAS_EVENT, HUNGVUONG_EVENT, TET_EVENT -> { }

        }

        this.bossAppearTogether = new Boss[this.data.length][];
        for (int i = 0; i < this.bossAppearTogether.length; i++) {
            if (this.data[i].getBossesAppearTogether() != null) {
                this.bossAppearTogether[i] = new Boss[this.data[i].getBossesAppearTogether().length];
                for (int j = 0; j < this.data[i].getBossesAppearTogether().length; j++) {
                    Boss boss = BossManager.gI().createBoss(this.data[i].getBossesAppearTogether()[j]);
                    if (boss != null) {
                        boss.parentBoss = this;
                        this.bossAppearTogether[i][j] = boss;
                    }
                }
            }
        }
    }

    @Override
    public void initBase() {
        BossData data = this.data[this.currentLevel];
        this.name = String.format(data.getName(), Util.nextInt(0, 100));
        this.gender = data.getGender();
        this.nPoint.mpg = 31_07_2002;
        this.nPoint.dameg = data.getDame();
        this.nPoint.hpg = data.getHp()[Util.nextInt(0, data.getHp().length - 1)];
        this.nPoint.hp = nPoint.hpg;
        this.nPoint.calPoint();
        this.initSkill();
        this.resetBase();
        nro.models.services.BossAdminConfig.applyAtSpawn(this);
    }

    protected void initSkill() {
        for (Skill skill : this.playerSkill.skills) {
            skill.dispose();
        }
        this.playerSkill.skills.clear();
        this.playerSkill.skillSelect = null;
        int[][] skillTemps = data[this.currentLevel].getSkillTemp();
        for (int[] skillTemp : skillTemps) {
            Skill skill = SkillUtil.createSkill(skillTemp[0], skillTemp[1]);
            if (skillTemp.length == 3) {
                skill.coolDown = skillTemp[2];
            }
            this.playerSkill.skills.add(skill);
        }
    }

    private boolean kolDeathCounted;
    private synchronized boolean claimKolDeath() { if(kolDeathCounted)return false;kolDeathCounted=true;return true; }
    protected void resetBase() {
        nro.models.services.BossAdminDropService.reset(this);
        synchronized (this) { questSupportCredit = false; questSupportCombat = false; }
        synchronized(this) { kolDeathCounted=false;defeatedWait=false;deathPhaseStarted=0;defeatedSinceSpawn=false;defeatConsumed=false; }
        this.lastTimeChatS = 0;
        this.lastTimeChatE = 0;
        this.timeChatS = 0;
        this.timeChatE = 0;
        this.indexChatS = 0;
        this.indexChatE = 0;
    }

    //.outfit.
    @Override
    public short getHead() {
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][0];
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return (short) ConstPlayer.HEADMONKEY[effectSkill.levelMonkey - 1];
        }
        return this.data[this.currentLevel].getOutfit()[0];
    }

    @Override
    public short getBody() {
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][1];
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return 193;
        }
        return this.data[this.currentLevel].getOutfit()[1];
    }

    @Override
    public short getLeg() {
        if (effectSkill != null && effectSkill.isBinh) {
            return idOutfitMafuba[effectSkill.typeBinh][2];
        }
        if (effectSkill != null && effectSkill.isMonkey) {
            return 194;
        }
        return this.data[this.currentLevel].getOutfit()[2];

    }

    @Override
    public short getFlagBag() {
        return this.data[this.currentLevel].getOutfit()[3];
    }

    @Override
    public byte getAura() {
        return (byte) this.data[this.currentLevel].getOutfit()[4];
    }

    @Override
    public byte getEffFront() {
        return (byte) this.data[this.currentLevel].getOutfit()[5];
    }

    public Zone getMapJoin() {
        int mapId = this.data[this.currentLevel].getMapJoin()[Util.nextInt(0, this.data[this.currentLevel].getMapJoin().length - 1)];
        Zone map = MapService.gI().getMapWithRandZone(mapId);
        return map;
    }

    @Override
    public void changeStatus(BossStatus status) {
        if (questSupportRetired) return;
        // Late callbacks from companion bosses cannot wake a defeated boss.
        // Keep its death/leave/rest phase; RESPAWN still restores HP normally.
        if(status==BossStatus.RESPAWN)nro.models.services.BossNoticeService.gI().departed(this);
        synchronized(this){
            if(nPoint!=null&&nPoint.hp<=0
                    &&(status==BossStatus.ACTIVE||status==BossStatus.AFK||status==BossStatus.CHAT_S))return;
            this.bossStatus = status;
            if(status==DIE)deathPhaseStarted=System.currentTimeMillis();
            if(status==CHAT_E&&deathPhaseStarted==0)deathPhaseStarted=System.currentTimeMillis();
        }
        if(status==BossStatus.CHAT_S||status==BossStatus.ACTIVE)nro.models.services.BossNoticeService.gI().appeared(this);
        if(status==BossStatus.REST)nro.models.services.BossNoticeService.gI().resting(this);
        if(status==BossStatus.DIE&&nPoint!=null&&isDie())nro.models.services.BossNoticeService.gI().defeated(this,nro.models.services.BossNoticeService.deathAttacker(this));
    }

    @Override
    public Player getPlayerAttack() {
        if (this.zone == null) {
            return null;
        }
        if (this.playerTarger != null && (this.playerTarger.isDie() || !this.zone.equals(this.playerTarger.zone))) {
            this.playerTarger = null;
        }
        if (this.playerTarger == null || Util.canDoWithTime(this.lastTimeTargetPlayer, this.timeTargetPlayer)) {
            this.playerTarger = this.zone.getRandomPlayerInMap();
            this.lastTimeTargetPlayer = System.currentTimeMillis();
            this.timeTargetPlayer = Util.nextInt(5000, 7000);
        }
        if (this.playerTarger != null && this.playerTarger.isPet && ((Pet) this.playerTarger).master != null && ((Pet) this.playerTarger).master.equals(this)) {
            this.playerTarger = null;
        }
        return this.playerTarger;
    }

    @Override
    public void changeToTypePK() {
        PlayerService.gI().changeAndSendTypePK(this, ConstPlayer.PK_ALL);
    }

    @Override
    public void changeToTypeNonPK() {
        PlayerService.gI().changeAndSendTypePK(this, ConstPlayer.NON_PK);
    }

    @Override
    public void updateInfo() {
        nro.models.services.BossAdminConfig.applyAtTick(this);
        super.update();
    }

    @Override
    public void update() {
        nro.models.services.BossAdminConfig.applyAtTick(this);
        if (!seasonalAllowed()) return;
        if (questSupportRetired) return;
        super.update();
        if(updateDeathLifecycle())return;
        boolean combatPhase=bossStatus==ACTIVE||bossStatus==CHAT_S||bossStatus==AFK&&!(nPoint!=null&&nPoint.hp<=0);
        if(combatPhase&&prepareBom)return;
        this.nPoint.mp = this.nPoint.mpg;
        if (combatPhase&&(this.effectSkill == null || this.effectSkill.isHaveEffectSkill() || (this.newSkill != null && this.newSkill.isStartSkillSpecial))) {
            return;
        }
        switch (this.bossStatus) {
            case CHAT_S, AFK, ACTIVE ->
                this.autoLeaveMap();
        }
        switch (this.bossStatus) {
            case REST -> {
                this.rest();
                if (bossStatus == REST) nro.models.services.BossNoticeService.gI().resting(this);
            }
            case RESPAWN -> {
                this.respawn();
                this.changeStatus(BossStatus.JOIN_MAP);
            }
            case JOIN_MAP ->
                this.joinMap();
            case CHAT_S -> {
                if (chatS()) {
                    this.doneChatS();
                    this.lastTimeChatM = System.currentTimeMillis();
                    this.timeChatM = 5000;
                    if (this.bossStatus != BossStatus.AFK) {
                        this.changeStatus(BossStatus.ACTIVE);
                    }
                }
            }
            case AFK ->
                this.afk();
            case ACTIVE -> {
                this.chatM();
                if (this.effectSkill.isCharging && !Util.isTrue(1, 20) || this.effectSkill.useTroi) {
                    return;
                }
                this.active();
            }
            default -> {}
        }
    }

    @Override
    public void rest() {
        int nextLevel = this.currentLevel + 1;
        if (nextLevel >= this.data.length) {
            nextLevel = 0;
        }
        if (questSupport) {
            if (parentBoss == null && nro.models.services.QuestSupportService.gI().canRespawn(this))
                this.changeStatus(BossStatus.RESPAWN);
            return;
        }
        long readyAt = nro.models.services.QuestSupportService.gI().originalRespawnAt(this);
        if (this.data[nextLevel].getTypeAppear() == AppearType.DEFAULT_APPEAR
                && readyAt >= 0 && System.currentTimeMillis() >= readyAt) {
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    @Override
    public void afk() {

    }

    @Override
    public void respawn() {
        if (!seasonalAllowed()) return;
        this.currentLevel++;
        if (this.currentLevel >= this.data.length) {
            this.currentLevel = 0;
        }
        this.initBase();
        this.changeToTypeNonPK();
    }

    @Override
    public void joinMap() {
        if (!seasonalAllowed()) return;
        if (questSupport) {
            synchronized (this) {
                if (questSupportRetired) return;
                joinMapInternal();
            }
        } else {
            joinMapInternal();
        }
    }

    private void joinMapInternal() {
        if (zoneFinal != null) {
            joinMapByZone(zoneFinal);
            this.notifyJoinMap();
            this.changeStatus(BossStatus.CHAT_S);
            this.wakeupAnotherBossWhenAppear();
            return;
        }
        if (this.zone == null) {
            if (this.parentBoss != null) {
                this.zone = parentBoss.zone;
            } else if (this.lastZone == null) {
                this.zone = getMapJoin();
            } else {
                this.zone = this.lastZone;
            }
        }
        if (this.zone == null) {
            this.zone = getMapJoin();
        }
        if (this.zone != null) {
            try {
                if (this.currentLevel == 0) {
                    if (this.parentBoss == null) {
                        int zoneid = 0;
                        //this.zone.map.mapId == 80 || this.zone.map.mapId == 103 || this.zone.map.mapId == 97 || this.zone.map.mapId == 102
                        // Chỉ cho boss xuất hiện từ khu 2 trở lên ở map thường
                        if (this.isZone01SpawnDisabled && this.zone.map.zones.size() > 2) {
                            zoneid = Util.nextInt(2, this.zone.map.zones.size() - 1);
                            while (zoneid < this.zone.map.zones.size() && !this.zone.map.zones.get(zoneid).getBosses().isEmpty()) {
                                zoneid++;
                            }

                            if (zoneid < this.zone.map.zones.size()) {
                                this.zone = this.zone.map.zones.get(zoneid);
                            } else {
                                this.changeStatus(BossStatus.REST);
                                this.zone = null;
                                this.lastZone = null;
                                return;
                            }
                        } else {
                            // Check trong khu lớn hơn 10 người chuyển sang khu n + 1
                            while (zoneid < this.zone.map.zones.size() && this.zone.map.zones.get(zoneid).getNumOfPlayers() > 10) {
                                zoneid++;
                            }
                            // Check trong khu có boss sẽ chuyển sang khu n + 1
                            while (zoneid < this.zone.map.zones.size() && !this.zone.map.zones.get(zoneid).getBosses().isEmpty()) {
                                zoneid++;
                            }
                            if (zoneid < this.zone.map.zones.size()) {
                                this.zone = this.zone.map.zones.get(zoneid);
                            } else {
                                this.zone = this.zone.map.zones.get(0);
                            }
                        }
                        int x = this.zone.map.mapWidth > 100 ? Util.nextInt(100, this.zone.map.mapWidth - 100) : Util.nextInt(100);
                        int y = this.zone.map.yPhysicInTop(x, 100);
                        ChangeMapService.gI().changeMap(this, this.zone, x, y);
                    } else {
                        int x = this.parentBoss.location.x - (this.lv + 1) * 30;
                        int y = this.zone.map.yPhysicInTop(x, 100);
                        ChangeMapService.gI().changeMap(this, this.zone, x, y);
                    }
                    this.wakeupAnotherBossWhenAppear();
                } else {
                    ChangeMapService.gI().changeMap(this, this.zone, this.location.x, this.location.y);
                }
                Service.gI().sendFlagBag(this);
                this.notifyJoinMap();
                this.changeStatus(BossStatus.CHAT_S);
            } catch (Exception e) {
                this.changeStatus(BossStatus.REST);
                if (error < 5) {
                    Logger.error("Lỗi : " + e + "\n");
                    error++;
                }
            }
        } else {
            this.changeStatus(BossStatus.RESPAWN);
        }
    }

    public void joinMapByZone(Zone zone) {
        if (!seasonalAllowed()) return;
        if (questSupport) {
            synchronized (this) {
                if (questSupportRetired) return;
                joinMapByZoneInternal(zone);
            }
        } else {
            joinMapByZoneInternal(zone);
        }
    }

    private void joinMapByZoneInternal(Zone zone) {
        if (zone != null) {
            // Initial spawn needs a target; a live move must retain its origin until commit.
            if (this.zone == null) this.zone = zone;
            int x = zone.map.mapWidth > 100 ? Util.nextInt(100, zone.map.mapWidth - 100) : Util.nextInt(100);
            int y = zone.map.yPhysicInTop(x, 100);
            ChangeMapService.gI().changeMap(this, zone, x, y);
        }
    }

    protected void notifyJoinMap() {
        nro.models.services.BossNoticeService.gI().appeared(this);
    }
    private boolean canSendNotify() { return !questSupport && noticePublic(); }

    @Override
    public boolean chatS() {
        if (Util.canDoWithTime(lastTimeChatS, timeChatS)) {
            if (this.indexChatS == this.data[this.currentLevel].getTextS().length) {
                return true;
            }
            String textChat = this.data[this.currentLevel].getTextS()[this.indexChatS];
            int prefix = Integer.parseInt(textChat.substring(1, textChat.lastIndexOf("|")));
            textChat = textChat.substring(textChat.lastIndexOf("|") + 1);
            if (!this.chat(prefix, textChat)) {
                return false;
            }
            this.lastTimeChatS = System.currentTimeMillis();
            this.timeChatS = textChat.length() * 100;
            if (this.timeChatS > 2000) {
                this.timeChatS = 2000;
            }
            this.indexChatS++;
        }
        return false;
    }

    @Override
    public void doneChatS() {

    }

    @Override
    public void chatM() {
        if (this.typePk == ConstPlayer.NON_PK) {
            return;
        }
        if (this.data[this.currentLevel].getTextM().length == 0) {
            return;
        }
        if (!Util.canDoWithTime(this.lastTimeChatM, this.timeChatM)) {
            return;
        }
        String textChat = this.data[this.currentLevel].getTextM()[Util.nextInt(0, this.data[this.currentLevel].getTextM().length - 1)];
        int prefix = Integer.parseInt(textChat.substring(1, textChat.lastIndexOf("|")));
        textChat = textChat.substring(textChat.lastIndexOf("|") + 1);
        this.chat(prefix, textChat);
        this.lastTimeChatM = System.currentTimeMillis();
        this.timeChatM = Util.nextInt(3000, 20000);
    }

    @Override
    public void active() {
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
        }
        this.attack();
    }

    protected long lastTimeAttack;

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 100) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                if (Util.getDistance(this, pl) <= this.getRangeCanAttackWithSkillSelect()) {
                    if (Util.isTrue(5, 20)) {
                        if (SkillUtil.isUseSkillChuong(this)) {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 200)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 70));
                        } else {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(10, 40)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 50));
                        }
                    }
                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                } else {
                    if (Util.isTrue(1, 2)) {
                        this.moveToPlayer(pl);
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public void checkPlayerDie(Player player) {
        if (player.isDie()) {

        }
    }

    protected int getRangeCanAttackWithSkillSelect() {
        int skillId = this.playerSkill.skillSelect.template.id;
        if (skillId == Skill.KAMEJOKO || skillId == Skill.MASENKO || skillId == Skill.ANTOMIC) {
            return Skill.RANGE_ATTACK_CHIEU_CHUONG;
        } else if (skillId == Skill.DRAGON || skillId == Skill.DEMON || skillId == Skill.GALICK || skillId == Skill.LIEN_HOAN || skillId == Skill.KAIOKEN) {
            return Skill.RANGE_ATTACK_CHIEU_DAM;
        }
        return 500;
    }

    @Override
    public void die(Player plKill) {
        if (!seasonalAllowed()) return;
        if (questSupport) {
            if (!questSupportRetired && plKill != null && !plKill.isBot) reward(plKill);
            this.changeStatus(BossStatus.DIE);
            return;
        }
        nro.models.services.BossNoticeService.gI().defeated(this,plKill);
        if(claimKolDeath()) nro.models.services.KolQuestService.gI().onBossKilled(plKill,this);

        if (plKill != null
                && (this.zone.map.mapId != 140 || !MapService.gI().isMapMaBu(this.zone.map.mapId)
                || !MapService.gI().isMapDoanhTrai(this.zone.map.mapId)
                || !MapService.gI().isMapBanDoKhoBau(this.zone.map.mapId))) {
            if (!plKill.isBot) {
                reward(plKill);
            }
            this.changeStatus(BossStatus.DIE);
        } else {
            if (plKill != null && !plKill.isBot) {
                reward(plKill);
            }
            this.changeStatus(BossStatus.DIE);
        }
    }

    @Override
    public final void reward(Player plKill) {
        if (!rewardAllowed(plKill)) return;
        nro.models.services.BossAdminDropService.reward(this, plKill,
                () -> { if (isQuestSupport()) questSupportReward(plKill); else rewardDefault(plKill); });
    }

    protected boolean rewardAllowed(Player killer) { return seasonalAllowed(); }

    public void rewardDefault(Player plKill) {
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
    }

    @Override
    public boolean chatE() {
        if(data==null||currentLevel<0||currentLevel>=data.length||data[currentLevel]==null||data[currentLevel].getTextE()==null)return true;
        if (Util.canDoWithTime(lastTimeChatE, timeChatE)) {
            if (this.indexChatE >= this.data[this.currentLevel].getTextE().length) {
                return true;
            }
            String textChat = this.data[this.currentLevel].getTextE()[this.indexChatE];
            int prefix = Integer.parseInt(textChat.substring(1, textChat.lastIndexOf("|")));
            textChat = textChat.substring(textChat.lastIndexOf("|") + 1);
            if (!this.chat(prefix, textChat)) {
                return false;
            }
            this.lastTimeChatE = System.currentTimeMillis();
            this.timeChatE = textChat.length() * 100;
            if (this.timeChatE > 2000) {
                this.timeChatE = 2000;
            }
            this.indexChatE++;
        }
        return false;
    }

    @Override
    public void doneChatE() {

    }

    @Override
    public void leaveMap() {
        if (this.currentLevel < this.data.length - 1) {
            this.lastZone = this.zone;
            this.changeStatus(BossStatus.RESPAWN);
        } else {
            ChangeMapService.gI().exitMap(this);
            this.lastZone = null;
            this.lastTimeRest = System.currentTimeMillis();
            this.changeStatus(BossStatus.REST);
        }
        this.wakeupAnotherBossWhenDisappear();
    }

    @Override
    public synchronized int injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (questSupportRetired) return 0;
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }

            if (plAtt != null && plAtt.idNRNM != -1) {
                return 1;
            }
            markQuestSupportCombat(plAtt, damage);
            this.nPoint.subHP(damage);

            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }

            return (int) damage;
        } else {
            return 0;
        }
    }

    @Override
    public void moveToPlayer(Player pl) {
        if (pl.location != null) {
            moveTo(pl.location.x, pl.location.y);
        }
    }

    @Override
    public void moveTo(int x, int y) {
        byte dir = (byte) (this.location.x - x < 0 ? 1 : -1);
        byte move = (byte) Util.nextInt(40, 60);
        PlayerService.gI().playerMove(this, this.location.x + (dir == 1 ? move : -move), y + (Util.isTrue(3, 10) ? -50 : 0));
    }

    public void chat(String text) {
        Service.gI().chat(this, text);
    }

    protected boolean chat(int prefix, String textChat) {
        if (prefix == -1) {
            this.chat(textChat);
        } else if (prefix == -2) {
            if (this.zone != null) {
                Player plMap = this.zone.getRandomPlayerInMap();
                if (plMap != null && !plMap.isDie() && Util.getDistance(this, plMap) <= 600) {
                    Service.gI().chat(plMap, textChat);
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else if (prefix == -3) {
            if (this.parentBoss != null && !this.parentBoss.isDie()) {
                this.parentBoss.chat(textChat);
            }
        } else if (prefix >= 0) {
            Boss[] group=hasCompanionGroup()?getCompanionBosses():parentBoss!=null?parentBoss.getCompanionBosses():new Boss[0];
            if(prefix<group.length&&group[prefix]!=null&&!group[prefix].isDie())group[prefix].chat(textChat);
        }
        return true;
    }

    @Override
    public void wakeupAnotherBossWhenAppear() {
        if (!hasCompanionGroup()) {
            return;
        }
        for (Boss boss : getCompanionBosses()) {
            if(boss==null)continue;
            int nextLevelBoss = boss.currentLevel + 1;
            if (nextLevelBoss >= boss.data.length) {
                nextLevelBoss = 0;
            }
            if (boss.data[nextLevelBoss].getTypeAppear() == AppearType.CALL_BY_ANOTHER) {
                if (boss.zone != null) {
                    boss.leaveMap();
                }
            }
            if (boss.data[nextLevelBoss].getTypeAppear() == AppearType.APPEAR_WITH_ANOTHER) {
                if (boss.zone != null) {
                    boss.leaveMap();
                }
                boss.changeStatus(BossStatus.RESPAWN);
            }
        }
    }

    @Override
    public void wakeupAnotherBossWhenDisappear() {
    }

    @Override
    public void autoLeaveMap() {

    }

    public void leaveMapNew() {
        if (this.data != null) {
            this.currentLevel = this.data.length;
        }
        this.changeStatus(BossStatus.LEAVE_MAP);
    }

    private static final ScheduledExecutorService BOM_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Boss self-destruct timer");
        t.setDaemon(true);
        return t;
    });

    public void setBom(Player plAtt) {
        try {
            if (!prepareBom) {
                prepareBom = true;
                this.nPoint.hp = 1;
                //gồng tự sát
                Service.gI().chat(Boss.this, "Rồi, rồi, mày xong rồi!");
                Message msg;
                try {
                    msg = new Message(-45);
                    msg.writer().writeByte(7);
                    msg.writer().writeInt((int) Boss.this.id);
                    msg.writer().writeShort(104);
                    msg.writer().writeShort(2000);
                    Service.gI().sendMessAllPlayerInMap(Boss.this, msg);
                    msg.cleanup();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // Async explosion after 2.5s — no busy-wait, no thread blocking
                final Player bomber = plAtt;
                BOM_SCHEDULER.schedule(() -> {
                    if (!prepareBom) return;
                    prepareBom = false;
                    try {
                        setDie(this);
                        die(bomber == null ? BossNoticeService.deathAttacker(this) : bomber);
                        long dame = Boss.this.nPoint.hpMax;
                        for (Mob mob : Boss.this.zone.mobs) {
                            mob.injured(Boss.this, dame, true);
                        }
                        List<Player> playersMap = Boss.this.zone.getNotBosses();
                        if (!MapService.gI().isMapOffline(Boss.this.zone.map.mapId)) {
                            for (int i = playersMap.size() - 1; i >= 0; i--) {
                                Player pl = playersMap.get(i);
                                if (!Boss.this.equals(pl)) {
                                    pl.injured(Boss.this, dame, false, false);
                                    PlayerService.gI().sendInfoHpMpMoney(pl);
                                    Service.gI().Send_Info_NV(pl);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        prepareBom = false;
                        setDie(this);
                        die(bomber);
                    }
                }, 2500, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            if (prepareBom) {
                prepareBom = false;
            }
            setDie(this);
            die(plAtt);
        }
    }

}
