package nro.models.services;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import nro.models.mob.Mob;
import nro.models.player.*;
import nro.models.skill.*;
import nro.models.player_system.Template;
import nro.models.network.Message;
import nro.models.server.Manager;
import nro.models.skill.NClass;
import static nro.models.services.PinkFixture.*;

/** Exercises the real direct-projectile and charged-area mob-skill paths without SQL. */
public final class MobSkillDeliveryTest {
    private static int hitPackets;
    private static long lastDamage;
    private static int damage = 100;

    public static final class Points extends NPoint {
        public Points(Player p) { super(p); }
        @Override public int getDameAttack(boolean mob) { return damage; }
        @Override public short getTileHutHp(boolean mob) { return 0; }
        @Override public short getTiLeHutMp() { return 0; }
    }

    public static final class Target extends Mob {
        @Override public long getTiemNangForPlayer(Player p, long dealt) { return dealt; }
    }

    private static final class Skills extends SkillService {
        @Override public void sendPlayerPrepareSkill(Player p, int duration) {}
        @Override public void affterUseSkill(Player p, int id) {
            p.playerSkill.skillSelect.lastTimeUseThisSkill = System.currentTimeMillis();
        }
    }

    private static Skill skill(int id, int type, int cooldown, int point) {
        Skill s = new Skill();
        s.template = new Template.SkillTemplate();
        s.template.id = (byte) id;
        s.template.type = (byte) type;
        s.template.manaUseType = 0;
        s.skillId = (short) id;
        s.point = point;
        s.coolDown = cooldown;
        s.manaUse = 1;
        return s;
    }

    private static Target mob(Player p, int id, int x, int hp) {
        Target mob = new Target();
        mob.id = id;
        mob.tempId = 0;
        mob.zone = p.zone;
        mob.point.hp = mob.point.maxHp = hp;
        mob.location.x = x;
        mob.location.y = 100;
        p.zone.mobs.add(mob);
        return mob;
    }

    public static void main(String[] args) throws Exception {
        Manager.NCLASS.clear();
        for (int i = 0; i < 3; i++) Manager.NCLASS.add(new NClass());
        replace(Service.class, "instance", new Service() {
            @Override public void sendMessAllPlayerInMap(Player p, Message m) {}
            @Override public void sendMessAllPlayerInMap(nro.models.map.Zone z, Message m) {
                if (m.command == -9) {
                    hitPackets++;
                    lastDamage = ByteBuffer.wrap(m.getData()).getInt(5);
                }
            }
            @Override public void addSMTN(Player p, byte type, long amount, boolean ori) {}
            @Override public void updatePlayerTotalDamage(Player p) {}
        });
        replace(PlayerService.class, "i", new PlayerService() {
            @Override public void sendInfoHpMp(Player p) {}
            @Override public void sendCurrentStamina(Player p) {}
        });

        Player p = player(456);
        p.name = "fixture";
        p.nPoint = new Points(p);
        p.nPoint.hp = p.nPoint.hpMax = 1000;
        p.nPoint.mp = p.nPoint.mpMax = 1000;
        p.nPoint.stamina = 100;
        p.playerSkill = blank(PlayerSkill.class);
        p.playerSkill.skills = new ArrayList<>();
        p.effectSkill = blank(EffectSkill.class);
        p.effectSkin = blank(EffectSkin.class);
        p.charms = blank(Charms.class);
        p.location = new Location();
        p.location.x = 100;
        p.location.y = 100;
        p.zone = zone();
        Target primary = mob(p, 1, 130, 100_000);
        Target nearby = mob(p, 2, 140, 100_000);
        Skills skills = new Skills();

        Skill blast = skill(Skill.KAMEJOKO, 1, 1000, 1);
        blast.lastTimeUseThisSkill = 0;
        p.playerSkill.skills.add(blast);
        p.playerSkill.skillSelect = blast;
        int hpBefore = primary.point.hp;
        int packetsBefore = hitPackets;
        check(skills.useSkillForMob(p, primary, Skill.KAMEJOKO) == SkillService.MobCastResult.ACCEPTED,
                "valid blast must be accepted by the canonical projectile executor");
        check(primary.point.hp < hpBefore && hitPackets == packetsBefore + 1 && lastDamage > 0,
                "accepted blast must reduce mob HP and emit exactly one positive-damage packet");

        Skill ball = skill(Skill.QUA_CAU_KENH_KHI, 1, 1000, 1);
        ball.lastTimeUseThisSkill = 0;
        p.playerSkill.skills.add(ball);
        p.playerSkill.skillSelect = ball;
        int primaryBeforeCharge = primary.point.hp;
        int nearbyBeforeCharge = nearby.point.hp;
        packetsBefore = hitPackets;
        check(skills.useSkillForMob(p, primary, Skill.QUA_CAU_KENH_KHI) == SkillService.MobCastResult.PREPARING,
                "first charged-ball request must report preparation");
        check(primary.point.hp == primaryBeforeCharge && nearby.point.hp == nearbyBeforeCharge
                        && hitPackets == packetsBefore,
                "preparation must not fabricate or prematurely apply damage");
        p.playerSkill.lastTimePrepareQCKK = System.currentTimeMillis() - 4001;
        check(skills.useSkillForMob(p, primary, Skill.QUA_CAU_KENH_KHI) == SkillService.MobCastResult.ACCEPTED,
                "charged ball must be admitted after its server preparation window");
        check(primary.point.hp < primaryBeforeCharge && nearby.point.hp < nearbyBeforeCharge,
                "charged area cast must damage the selected mob and nearby mob through server execution");
        check(hitPackets >= packetsBefore + 2 && lastDamage > 0,
                "charged area cast must emit positive-damage server packets");
        int resolvedPrimaryHp = primary.point.hp;
        int resolvedNearbyHp = nearby.point.hp;
        check(skills.useSkillForMob(p, primary, Skill.QUA_CAU_KENH_KHI) == SkillService.MobCastResult.COOLDOWN,
                "immediate duplicate charged cast must be rejected by cooldown");
        check(primary.point.hp == resolvedPrimaryHp && nearby.point.hp == resolvedNearbyHp,
                "duplicate charged cast must not produce a second hit");

        Skill laser = skill(Skill.MAKANKOSAPPO, 1, 1000, 1);
        laser.lastTimeUseThisSkill = 0;
        p.playerSkill.skills.add(laser);
        p.playerSkill.skillSelect = laser;
        int hpBeforeLaser = primary.point.hp;
        packetsBefore = hitPackets;
        check(skills.useSkillForMob(p, primary, Skill.MAKANKOSAPPO) == SkillService.MobCastResult.PREPARING,
                "first Makankosappo request must report preparation");
        check(primary.point.hp == hpBeforeLaser && hitPackets == packetsBefore,
                "Makankosappo preparation must not claim a hit");
        p.playerSkill.lastTimePrepareLaze = System.currentTimeMillis() - 3001;
        check(skills.useSkillForMob(p, primary, Skill.MAKANKOSAPPO) == SkillService.MobCastResult.ACCEPTED,
                "Makankosappo must execute after its server preparation window");
        check(primary.point.hp < hpBeforeLaser && hitPackets == packetsBefore + 1 && lastDamage > 0,
                "accepted Makankosappo must emit one positive-damage server packet");

        System.out.println("PASS MobSkillDeliveryTest: projectile packet; charged ball and laser; area hits; duplicate rejection");
    }
}
