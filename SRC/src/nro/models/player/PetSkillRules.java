package nro.models.player;

import nro.models.skill.Skill;
import nro.models.utils.SkillUtil;

/** Shared presentation and execution policy; persisted surplus slots are retained. */
public final class PetSkillRules {
    public static final long FIFTH_SKILL_POWER = 60_000_000_000L;

    private PetSkillRules() { }

    public static int visibleCount(Pet pet) {
        return pet.typePet >= 2 && pet.typePet <= 4 ? 5 : 4;
    }

    public static boolean canOpenFifth(Pet pet) {
        return pet != null && visibleCount(pet) == 5 && pet.nPoint != null
                && pet.nPoint.power >= FIFTH_SKILL_POWER;
    }

    public static void ensureSlots(Pet pet) {
        if (pet == null || pet.playerSkill == null || pet.playerSkill.skills == null) {
            return;
        }
        while (pet.playerSkill.skills.size() < visibleCount(pet)) {
            pet.playerSkill.skills.add(SkillUtil.createEmptySkill());
        }
    }

    private static boolean isFifth(Skill skill) {
        return skill != null && skill.template != null
                && (skill.template.id == Skill.SUPER_KAME
                || skill.template.id == Skill.LIEN_HOAN_CHUONG
                || skill.template.id == Skill.MA_PHONG_BA);
    }

    public static Skill skillAt(Pet pet, int index) {
        if (pet == null || pet.playerSkill == null || pet.playerSkill.skills == null
                || index < 0 || index >= visibleCount(pet) || index >= pet.playerSkill.skills.size()) {
            return null;
        }
        Skill skill = pet.playerSkill.skills.get(index);
        if (index == 4 && (!canOpenFifth(pet) || !isFifth(skill))
                || index != 4 && isFifth(skill)) {
            return null;
        }
        return skill;
    }

    public static boolean canExecute(Player player) {
        if (player == null || player.playerSkill == null || player.playerSkill.skillSelect == null
                || player.playerSkill.skillSelect.skillId == -1 || player.playerSkill.skillSelect.template == null) {
            return false;
        }
        if (!(player instanceof Pet pet)) {
            return true;
        }
        Skill selected = player.playerSkill.skillSelect;
        if (player.playerSkill.skills == null) {
            return false;
        }
        int found = -1;
        for (int i = 0; i < player.playerSkill.skills.size(); i++) {
            if (player.playerSkill.skills.get(i) == selected) {
                if (found != -1) {
                    return false;
                }
                found = i;
            }
        }
        return found >= 0 && skillAt(pet, found) == selected;
    }
}
