package nro.models.Bot;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Random;
import java.util.List;
import java.util.Set;
import nro.models.boss.Boss;
import nro.models.boss.Boss_Manager.BossManager;
import nro.models.boss.Boss_Manager.BrolyManager;
import nro.models.boss.Boss_Manager.FinalBossManager;
import nro.models.boss.Boss_Manager.OtherBossManager;
import nro.models.boss.Boss_Manager.SkillSummonedManager;
import nro.models.boss.Boss_Manager.YardartManager;
import nro.models.map.service.ChangeMapService;
import nro.models.skill.Skill;
import nro.models.map.service.MapService;
import nro.models.player.Player;
import nro.models.services.PlayerService;
import nro.models.services.SkillService;

public class BotSanBoss {

    public Bot bot;

    public Boss bossAttack;

    public long lastTimeSkill1;
    private int nextSkillIndex;
    private boolean finished;

    private boolean isBossAttackSkill(Skill skill) {
        if (skill == null || skill.template == null
                || skill.template.id == Skill.QUA_CAU_KENH_KHI
                || skill.template.id == Skill.MAKANKOSAPPO
                || skill.template.id == Skill.TU_SAT) {
            return false;
        }
        if (skill.template.type == 1) {
            return true;
        }
        return skill.template.type == 4 && switch (skill.template.id) {
            case Skill.SUPER_KAME, Skill.LIEN_HOAN_CHUONG, Skill.MA_PHONG_BA -> true;
            default -> false;
        };
    }

    public BotSanBoss(Bot b) {
        this.bot = b;
    }

    public void update() {
        if (this.finished) {
            return;
        }
        this.SanBot();
    }

    private void finishAfterTargetDefeated() {
        if (this.finished) {
            return;
        }
        this.finished = true;
        this.bossAttack = null;
        BotManager.gI().bot.remove(this.bot);
        ChangeMapService.gI().exitMap(this.bot);
    }

    public boolean isMap(int mapId) {
        MapService maps = MapService.gI();
        return maps.isMapOffline(mapId)
                || maps.isMapPhoBan(mapId)
                || maps.isMapBlackBallWar(mapId)
                || maps.isMapMaBu(mapId)
                || maps.isMapMabu2H(mapId)
                || maps.isMapSieuThanhThuy(mapId)
                || maps.isMapRiengTu(mapId)
                || maps.isMapNgucTu(mapId)
                || maps.isMapHuyDiet(mapId)
                || maps.isMapNguHanhSon(mapId);
    }

    private List<Boss> getPublicBossCandidates() {
        Set<Boss> registered = Collections.newSetFromMap(new IdentityHashMap<>());
        registered.addAll(BossManager.gI().getBosses());
        registered.addAll(YardartManager.gI().getBosses());
        registered.addAll(FinalBossManager.gI().getBosses());
        registered.addAll(SkillSummonedManager.gI().getBosses());
        registered.addAll(BrolyManager.gI().getBosses());
        registered.addAll(OtherBossManager.gI().getBosses());

        return registered.stream()
                .filter(boss -> boss != null && !boss.isDie() && boss.zone != null
                        && boss.zone.map != null && !isMap(boss.zone.map.mapId)
                        && !boss.zone.isFullPlayer())
                .toList();
    }

    private Boss getLiveBossInCurrentZone() {
        if (this.bot.zone == null || this.bot.zone.map == null || isMap(this.bot.zone.map.mapId)) {
            return null;
        }
        if (this.bossAttack != null && !this.bossAttack.isDie() && this.bossAttack.zone == this.bot.zone) {
            return this.bossAttack;
        }
        for (Player occupant : this.bot.zone.getBosses()) {
            if (occupant instanceof Boss boss && !boss.isDie()) {
                return boss;
            }
        }
        return null;
    }

    public void GetBoss() {
        Boss localBoss = getLiveBossInCurrentZone();
        if (localBoss != null) {
            this.bossAttack = localBoss;
        } else {
            List<Boss> candidates = getPublicBossCandidates();
            if (candidates.isEmpty()) {
                this.bossAttack = null;
                return;
            }

            if (this.bossAttack == null || !candidates.contains(this.bossAttack)) {
                this.bossAttack = candidates.get(new Random().nextInt(candidates.size()));
            }
        }

        if (this.bot.zone != this.bossAttack.zone) {
            ChangeMapService.gI().goToMap(this.bot, this.bossAttack.zone);
            if (this.bot.zone != null) {
                this.bot.zone.load_Me_To_Another(this.bot);
            }
        }
    }

    private Skill getNextUsableSkill() {
        List<Skill> skills = this.bot.playerSkill.skills;
        if (skills == null || skills.isEmpty()) {
            return null;
        }
        long now = System.currentTimeMillis();

        for (int checked = 0; checked < skills.size(); checked++) {
            int index = Math.floorMod(this.nextSkillIndex + checked, skills.size());
            Skill skill = skills.get(index);
            if (!isBossAttackSkill(skill)
                    || skill.lastTimeUseThisSkillbot >= now - skill.coolDown
                    || skill.lastTimeUseThisSkill >= now - skill.coolDown) {
                continue;
            }
            this.nextSkillIndex = (index + 1) % skills.size();
            return skill;
        }
        return null;
    }

    public void SanBot() {
        if (this.finished) {
            return;
        }
        if (this.bot.playerSkill != null) {
            this.bot.playerSkill.prepareQCKK = false;
            this.bot.playerSkill.prepareLaze = false;
            this.bot.playerSkill.prepareTuSat = false;
        }
        // Check the assigned boss before GetBoss() can replace it with another live boss.
        if (this.bossAttack != null && this.bossAttack.isDie()) {
            this.finishAfterTargetDefeated();
            return;
        }
        if (this.bot.newSkill != null && this.bot.newSkill.isStartSkillSpecial) {
            return;
        }
        this.GetBoss();
        if (this.bossAttack == null || this.bossAttack.isDie() || this.bot.zone != this.bossAttack.zone) {
            return;
        }

        Skill skill = this.getNextUsableSkill();
        if (skill == null) {
            return;
        }
        this.bot.playerSkill.skillSelect = skill;
        PlayerService.gI().playerMove(this.bot, this.bossAttack.location.x, this.bossAttack.location.y);
        if (SkillService.gI().useSkill(this.bot, this.bossAttack, null, 0, null)) {
            skill.lastTimeUseThisSkillbot = System.currentTimeMillis();
        }
    }
}
