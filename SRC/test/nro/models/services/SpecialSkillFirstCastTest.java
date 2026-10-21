package nro.models.services;

import nro.models.network.Message;
import nro.models.player.*;
import nro.models.skill.*;
import nro.models.player_system.Template;
import static nro.models.services.PinkFixture.*;

/** Reproduces the first special cast after login without SQL, sockets or timer callbacks. */
public final class SpecialSkillFirstCastTest {
    private static final class ManualSpecial extends NewSkill {
        ManualSpecial(Player player) { super(player); }
        @Override public void start(int interval) { }
    }
    private static final class Skills extends SkillService {
        int starts, completed;
        @Override public boolean canUseSkillWithMana(Player p) { return true; }
        @Override public void newSkillNotFocus(Player p, int status) { starts++; }
        @Override public void affterUseSkill(Player p, int id) { completed++; }
    }
    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 3; i++) nro.models.server.Manager.NCLASS.add(new NClass());
        replace(AchievementService.class, "instance", new AchievementService() {
            @Override public void checkDoneTask(Player p, int id) { }
        });
        for (byte id : new byte[]{Skill.SUPER_KAME, Skill.LIEN_HOAN_CHUONG, Skill.MA_PHONG_BA}) {
            Player p = player(9000 + id);
            p.playerSkill = new PlayerSkill(p);
            Skill skill = new Skill();
            skill.skillId = id; skill.point = 1;
            skill.template = new Template.SkillTemplate();
            skill.template.id = id; skill.template.type = 4;
            p.playerSkill.skills.add(skill);
            p.playerSkill.skillSelect = skill;
            p.newSkill = new ManualSpecial(p);
            Skills service = new Skills();
            Message outgoing = new Message(-45);
            outgoing.writer().writeByte(id);
            outgoing.writer().writeShort(100); outgoing.writer().writeShort(100);
            outgoing.writer().writeByte(1);
            outgoing.writer().writeShort(300); outgoing.writer().writeShort(100);
            Message incoming = new Message((byte)-45, outgoing.getData());
            try {
                check(p.newSkill.skillSelect == null, "fresh login has no previous special cast");
                check(service.useSkill(p, null, null, 20, incoming), "first special cast accepted: " + id);
                check(p.newSkill.skillSelect == skill && p.newSkill.isStartSkillSpecial,
                        "cast initializes selected special skill: " + id);
                check(service.starts == 1 && service.completed == 1, "cast starts and completes once: " + id);
                p.newSkill.closeSkillSpecial();
            } finally { outgoing.cleanup(); incoming.cleanup(); }
        }
        System.out.println("PASS first casts of Super Kame, Lien Hoan Chuong and Ma Phong Ba");
    }
}
