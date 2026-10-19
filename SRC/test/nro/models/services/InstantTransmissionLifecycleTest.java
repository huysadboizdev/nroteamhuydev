package nro.models.services;

import java.io.*;
import java.util.*;
import nro.models.map.Zone;
import nro.models.mob.*;
import nro.models.network.Message;
import nro.models.player.*;
import nro.models.player_system.Template;
import nro.models.skill.*;
import static nro.models.services.PinkFixture.*;

/** Real skill 20 execution with captured wire packets and isolated damage callbacks. */
public final class InstantTransmissionLifecycleTest {
    static final List<Integer> commands = new ArrayList<>();
    static int positions, completions, timers;
    static int damage = 10;
    static Zone moveTargetTo, moveCasterTo;
    static boolean killCaster;
    static class Points extends NPoint {
        Points(Player p) { super(p); }
        @Override public int getDameAttack(boolean mob) { return damage; }
        @Override public int getDameAttack(boolean mob, Player target, boolean legal) { return damage; }
    }
    static class Target extends Player {
        @Override public int injured(Player attacker, long value, boolean piercing, boolean mob) {
            nPoint.hp = Math.max(0, nPoint.hp - (int) value);
            if (moveTargetTo != null) zone = moveTargetTo;
            if (moveCasterTo != null) attacker.zone = moveCasterTo;
            if (killCaster) attacker.nPoint.hp = 0;
            return (int) value;
        }
    }
    static class TargetMob extends Mob {
        @Override public void injured(Player attacker, long value, boolean dieWhenFull) {
            point.hp = Math.max(0, point.hp - (int) value);
            if (moveTargetTo != null) zone = moveTargetTo;
            if (moveCasterTo != null) attacker.zone = moveCasterTo;
            if (killCaster) attacker.nPoint.hp = 0;
        }
    }
    static class Skills extends SkillService {
        @Override public void affterUseSkill(Player p, int id) {
            completions++;
            p.playerSkill.skillSelect.lastTimeUseThisSkill = System.currentTimeMillis();
        }
    }
    static void record(Message message) {
        commands.add((int) message.command);
        if (message.command == 123) {
            positions++;
            try {
                var reader = new DataInputStream(new ByteArrayInputStream(message.getData()));
                check(reader.readInt() == 1, "position caster ID");
                check(reader.readShort() == 300 && reader.readShort() == 240, "position destination");
                check(reader.readByte() == 1 && reader.available() == 0, "unchanged exact command 123 layout");
            } catch (IOException e) { throw new AssertionError(e); }
        }
    }
    static Player caster(Zone zone) {
        Player p = new Player(); p.id = 1; p.isBoss = true; p.isPlayer = false; p.zone = zone;
        p.location.x = p.location.y = 100;
        p.nPoint = new Points(p); p.nPoint.hp = p.nPoint.hpMax = 100000;
        p.nPoint.mp = p.nPoint.mpMax = 100000; p.nPoint.stamina = 100;
        Skill s = new Skill(); s.skillId = 134; s.point = 7; s.coolDown = 500;
        s.template = new Template.SkillTemplate(); s.template.id = Skill.DICH_CHUYEN_TUC_THOI; s.template.type = 1;
        p.playerSkill.skillSelect = s; p.playerSkill.skills.add(s);
        return p;
    }
    static Target target(Zone zone, boolean boss) {
        Target p = new Target(); p.id = 2; p.isBoss = boss; p.isPlayer = false;
        p.zone = zone; p.location.x = 300; p.location.y = 240;
        p.nPoint.hp = p.nPoint.hpMax = 100000;
        p.typePk = 5; return p;
    }
    static TargetMob mob(Zone zone) throws Exception {
        TargetMob m = blank(TargetMob.class); m.id = 3; m.zone = zone;
        m.location = blank(Location.class); m.location.x = 300; m.location.y = 240;
        m.point = new MobPoint(m); m.point.hp = m.point.maxHp = 100000;
        m.effectSkill = new MobEffectSkill(m); return m;
    }
    public static void main(String[] args) {
        try {
            for (int i = 0; i < 3; i++) nro.models.server.Manager.NCLASS.add(blank(NClass.class));
            install();
            replace(Service.class, "instance", new Service() {
                @Override public void sendMessAllPlayerInMap(Player p, Message m) { record(m); }
                @Override public void sendMessAllPlayerInMap(Zone z, Message m) { record(m); }
                @Override public void reload_HP_NV(Player p) {}
                @Override public void chat(Player p, String text) {}
            });
            replace(PlayerService.class, "i", new PlayerService() {
                @Override public void sendInfoHpMpMoney(Player p) {}
            });
            replace(ItemTimeService.class, "i", new ItemTimeService() {
                @Override public void sendItemTime(Player p, int id, int seconds) { timers++; }
            });
            Skills skills = new Skills(); Zone origin = zone(); Player p = caster(origin);
            String mode = args[0]; boolean vsMob = mode.startsWith("mob-");
            Target t = vsMob ? null : target(origin, mode.startsWith("boss-"));
            TargetMob m = vsMob ? mob(origin) : null;
            if (mode.endsWith("cross-zone")) { if (m != null) m.zone = zone(); else t.zone = zone(); }
            if (mode.endsWith("dead")) { if (m != null) m.point.hp = 0; else t.nPoint.hp = 0; }
            if (mode.endsWith("disposed")) { if (m != null) m.location = null; else t.location = null; }
            if (mode.endsWith("lethal")) damage = 100000;
            if (mode.endsWith("target-moves")) moveTargetTo = zone();
            if (mode.endsWith("caster-moves")) moveCasterTo = zone();
            if (mode.endsWith("caster-dies")) killCaster = true;
            boolean invalid = mode.endsWith("cross-zone") || mode.endsWith("dead") || mode.endsWith("disposed")
                    || mode.equals("missing") || mode.equals("both") || mode.equals("caster-dead");
            if (mode.equals("missing")) t = null;
            if (mode.equals("both")) m = mob(origin);
            if (mode.equals("caster-dead")) p.nPoint.hp = 0;
            if (mode.endsWith("repeat")) {
                for (int i = 0; i < 1000; i++) {
                    p.playerSkill.skillSelect.lastTimeUseThisSkill = 0;
                    check(skills.useSkill(p, t, m, -1, null), "stress valid cast " + i);
                }
                check(positions == 1000 && completions == 1000, "one position and completion per accepted cast");
                check(!skills.useSkill(p, t, m, -1, null), "immediate repeat is rejected by existing cooldown");
                check(positions == 1000 && completions == 1000, "rejected cooldown has no position side effect");
            } else {
                boolean accepted = skills.useSkill(p, t, m, -1, null);
                if (invalid) {
                    check(!accepted, "invalid target must be rejected before execution: " + mode);
                    check(positions == 0 && completions == 0 && commands.isEmpty(), "invalid cast has no packet/cooldown side effects");
                    check(p.location.x == 100 && p.location.y == 100, "invalid cast leaves caster position unchanged");
                    skills.useSkillAttack(p, t, m);
                    check(positions == 0 && completions == 0, "direct boss caller must reject invalid target too");
                } else {
                    check(accepted && positions == 1 && completions == 1, "valid cast has one position and completion");
                    check(commands.get(0) == 123, "position precedes damage animation");
                    boolean terminal = mode.endsWith("lethal") || mode.endsWith("target-moves")
                            || mode.endsWith("caster-moves") || mode.endsWith("caster-dies");
                    boolean blind = m != null ? m.effectSkill.isBlindDCTT : t.effectSkill.isBlindDCTT;
                    check(blind != terminal, "no blind effect on dead/transferred participants: " + mode);
                    check(commands.contains(-124) != terminal, "effect packet follows only a live same-zone hit");
                    if (terminal) check(timers == 0, "no new timer after terminal hit");
                }
            }
            System.out.println("PASS InstantTransmissionLifecycle " + mode);
            System.exit(0);
        } catch (Throwable error) { error.printStackTrace(); System.exit(1); }
    }
}
