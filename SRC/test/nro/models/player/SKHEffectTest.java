package nro.models.player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;
import nro.models.item.Item;
import nro.models.item.ActivationSet;
import nro.models.intrinsic.Intrinsic;
import nro.models.map.ItemMap;
import nro.models.map.Map;
import nro.models.map.Zone;
import nro.models.mob.Mob;
import nro.models.mob.MobMe;
import nro.models.network.Message;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.EffectSkillService;
import nro.models.services.Service;
import nro.models.services.SkillService;
import nro.models.services_dungeon.NgocRongNamecService;
import nro.models.skill.Skill;
import nro.models.skill.NClass;
import nro.models.utils.SkillUtil;
import nro.models.utils.Util;

/** Offline tests exercise real combat/drop paths; map IO and network are omitted. */
public final class SKHEffectTest {
    public static void main(String[] args) throws Exception {
        Field service = Service.class.getDeclaredField("instance");
        service.setAccessible(true);
        service.set(null, new Service() {
            @Override public void sendMessAllPlayerInMap(Player p, Message m) { }
            @Override public void sendMessAllPlayerInMap(Zone z, Message m) { }
            @Override public void sendThongBao(Player p, String text) { }
        });
        for (int id = 0; id <= 250; id++) {
            Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id, "Option #", 0));
        }
        for (int id = 0; id <= 1802; id++) {
            Template.ItemTemplate t = new Template.ItemTemplate();
            t.id = (short) id;
            t.type = 12;
            t.name = "Test item";
            Manager.ITEM_TEMPLATES.add(t);
        }
        for (int i = 0; i < 3; i++) Manager.NCLASS.add(new NClass());
        Field namek = NgocRongNamecService.class.getDeclaredField("instance");
        namek.setAccessible(true);
        namek.set(null, new NgocRongNamecService());
        switch (args[0]) {
            case "damage": damage(); break;
            case "critical": critical(); break;
            case "kaioken": kaioken(); break;
            case "explosion": explosion(); break;
            case "summon": summon(); break;
            case "loot": loot(); break;
            case "existing": existing(); break;
            case "skh-drop": skhDrop(); break;
            default: throw new AssertionError("Unknown mode");
        }
        System.out.println("PASS: SKH effects " + args[0]);
    }

    private static Player player(int skillId) throws Exception {
        Player p = new Player();
        p.playerIntrinsic.intrinsic = new Intrinsic();
        // Construct the game zone normally, omit Map's file loading and scheduler.
        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field singleton = unsafeClass.getDeclaredField("theUnsafe");
        singleton.setAccessible(true);
        Map map = (Map) unsafeClass.getMethod("allocateInstance", Class.class)
                .invoke(singleton.get(null), Map.class);
        map.mapId = 6;
        p.zone = new Zone(map, 0, 20);
        p.nPoint.hpMax = p.nPoint.hp = 10000;
        p.nPoint.mpMax = p.nPoint.mp = 10000;
        p.nPoint.dame = 10000;
        p.nPoint.stamina = 100;
        Skill s = new Skill();
        s.template = new Template.SkillTemplate();
        s.template.id = (byte) skillId;
        s.damage = 100;
        s.point = 1;
        s.coolDown = 10000;
        p.playerSkill.skills.add(s);
        p.playerSkill.skillSelect = s;
        for (int i = 0; i < 6; i++) p.inventory.itemsBody.add(new Item());
        return p;
    }

    private static Object call(Object target, String name, Class<?>[] types, Object... values) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method.invoke(target, values);
    }

    private static void seed(long value) throws Exception {
        Field random = Util.class.getDeclaredField("rand");
        random.setAccessible(true);
        ((Random) random.get(null)).setSeed(value);
    }

    private static void skhDrop() throws Exception {
        Player p = player(Skill.DRAGON);
        Mob mob = new Mob(); mob.tempId = 1; mob.zone = p.zone;
        for (int mapId : new int[]{1,2,3,8,9,11,15,16,17}) {
            p.zone.map.mapId = mapId;
            check(nro.models.map.service.MapService.gI().isMapUpSKH(mapId), "Other drop features must keep their map classification");
            for (int value = 0; value < 10000; value++) {
                seed(value);
                for (ItemMap drop : mob.getItemMobReward(p,0,0)) {
                    check(drop.options.stream().noneMatch(o -> ActivationSet.primaryId(o.optionTemplate.id) >= 0),
                            "Direct SKH drop must be disabled on map " + mapId + ", seed " + value);
                }
                p.zone.items.clear();
            }
        }
        p.zone.map.mapId = 164;
        check(nro.models.map.service.MapService.gI().isMapRiengTu(164), "Private map classification must remain available");
        for (int value = 0; value < 10000; value++) {
            seed(value);
            for (ItemMap drop : mob.getItemMobReward(p,0,0)) {
                check(drop.options.stream().noneMatch(o -> ActivationSet.primaryId(o.optionTemplate.id) >= 0),
                        "Private map direct SKH drop must be disabled, seed " + value);
            }
            p.zone.items.clear();
        }
    }

    private static void damage() throws Exception {
        Player p = player(Skill.MAKANKOSAPPO);
        for (int pieces = 0; pieces <= 5; pieces++) {
            p.setClothes.picolo = (byte) pieces;
            check(p.nPoint.getDameAttack(false) == (pieces == 5 ? 20000 : 10000),
                    "Picolo must double damage only at 5 pieces");
        }
        p.nPoint.mpMax = Integer.MAX_VALUE;
        check(p.nPoint.getDameAttack(false) == Integer.MAX_VALUE, "Picolo damage must saturate");
        p = player(Skill.DE_TRUNG);
        p.setClothes.pikkoroDaimao = 5;
        check(p.nPoint.getDameAttack(false) == 20000, "Pikkoro must give +100%, not +300%");
        p.setClothes.pikkoroDaimao = 4;
        check(p.nPoint.getDameAttack(false) == 10000, "Pikkoro bonus must disappear");
        p = player(Skill.MASENKO);
        p.setClothes.nail = 5;
        seed(50);
        int result = p.nPoint.getDameAttack(false);
        check(result >= 17100 && result <= 18901, "Nail +80% must apply once, got " + result);
        p.playerIntrinsic.intrinsic.id = 9;
        p.playerIntrinsic.intrinsic.param1 = 10;
        seed(50);
        result = p.nPoint.getDameAttack(false);
        check(result >= 18810 && result <= 20791, "Nail must coexist with Masenko intrinsic");
    }

    private static void critical() throws Exception {
        Player p = player(Skill.DRAGON);
        p.nPoint.critg = 3;
        for (int pieces = 0; pieces <= 5; pieces++) {
            p.setClothes.thanVuTruKaio = (byte) pieces;
            call(p.nPoint, "setCrit", new Class<?>[0]);
            check(p.nPoint.crit == (pieces >= 2 ? 13 : 3), "Kaio critical threshold is 2 pieces");
        }
        p.setClothes.thanVuTruKaio = 0;
        for (int pieces = 0; pieces <= 5; pieces++) {
            p.setClothes.nail = (byte) pieces;
            p.nPoint.isCrit100 = true;
            seed(100);
            int result = p.nPoint.getDameAttack(false);
            int expected = pieces >= 2 ? 22000 : 20000;
            check(result >= expected * 95 / 100 && result <= expected * 105 / 100 + 1,
                    "Nail must increase actual critical damage at 2+ pieces, got " + result);
        }
        p.nPoint.tlSDCM = 50;
        for (int pieces = 1; pieces <= 2; pieces++) {
            p.setClothes.nail = (byte) pieces;
            for (int i = 0; i < 25; i++) {
                p.nPoint.isCrit100 = true;
                seed(i);
                int result = p.nPoint.getDameAttack(false);
                int expected = pieces == 2 ? 32000 : 30000;
                check(result >= expected * 95 / 100 && result <= expected * 105 / 100 + 1,
                        "Nail must add 10 critical damage points to existing modifiers once");
            }
        }
    }

    private static void kaioken() throws Exception {
        Player p = player(Skill.KAIOKEN);
        SkillService service = SkillService.gI();
        p.playerSkill.skillSelect.manaUse = 1000;
        for (int pieces = 0; pieces <= 5; pieces++) {
            p.setClothes.thanVuTruKaio = (byte) pieces;
            int hpCost = pieces == 5 ? 300 : pieces == 4 ? 500 : 1000;
            int mpCost = pieces == 5 ? 300 : pieces == 4 ? 500 : 1000;
            p.nPoint.hp = hpCost + 1;
            p.nPoint.mp = mpCost;
            check(service.canUseSkillWithMana(p), "Kaioken eligibility must use reduced HP/KI cost at " + pieces);
            service.useSkillAttack(p, null, null);
            check(p.nPoint.hp == 1 && p.nPoint.mp == 0, "Kaioken must consume exactly reduced costs");
            check(!service.canUseSkillWithMana(p), "Kaioken must reject insufficient resources");
            p.nPoint.hp = hpCost;
            p.nPoint.mp = 10000;
            check(!service.canUseSkillWithMana(p), "Kaioken must not kill its user");
        }
        p.setClothes.thanVuTruKaio = 5;
        p.nPoint.hp = 10000;
        p.nPoint.mpMax = p.nPoint.mp = 10000;
        p.playerSkill.skillSelect.template.manaUseType = 1;
        p.playerSkill.skillSelect.manaUse = 10;
        call(service, "setMpAffterUseSkill", new Class<?>[]{Player.class}, p);
        check(p.nPoint.mp == 9700, "Percentage mana must use the same reduction");
        p.playerSkill.skillSelect.template.manaUseType = 2;
        p.nPoint.mp = 1000;
        call(service, "setMpAffterUseSkill", new Class<?>[]{Player.class}, p);
        check(p.nPoint.mp == 700, "All-current-mana mode must honor Kaio reduction");
        p.playerSkill.skillSelect.template.id = Skill.DRAGON;
        p.nPoint.mp = 1000;
        call(service, "setMpAffterUseSkill", new Class<?>[]{Player.class}, p);
        check(p.nPoint.mp == 0, "Kaio must not discount another skill");
        p = player(Skill.KAIOKEN);
        for (int pieces = 4; pieces <= 5; pieces++) {
            p.setClothes.thanVuTruKaio = (byte) pieces;
            int result = p.nPoint.getDameAttack(false);
            int expected = pieces == 5 ? 13000 : 10000;
            check(result >= expected * 95 / 100 && result <= expected * 105 / 100 + 1,
                    "Kaio damage bonus must activate only at 5 pieces");
        }
    }

    private static void explosion() throws Exception {
        for (int hp : new int[]{10000, Integer.MAX_VALUE}) {
            for (int pieces = 0; pieces <= 5; pieces++) {
                Player p = player(Skill.TU_SAT);
                p.nPoint.hpMax = p.nPoint.hp = hp;
                p.setClothes.cadicM = (byte) pieces;
                p.playerSkill.prepareTuSat = true;
                p.playerSkill.lastTimePrepareTuSat = System.currentTimeMillis() - 2000;
                final long[] hit = {0};
                Mob target = new Mob() {
                    @Override public synchronized void injured(Player attacker, long damage, boolean fullHp) {
                        hit[0] = damage;
                    }
                };
                target.location.x = SkillUtil.getRangeBom(1) + 100;
                target.zone = p.zone;
                p.zone.mobs.add(target);
                SkillService.gI().useSkillAlone(p);
                long expected = hp + (pieces == 5 ? (long) hp * 50 / 100 : pieces == 4 ? (long) hp * 20 / 100 : 0);
                check(hit[0] == (pieces >= 2 ? expected : 0), "Explosion radius must persist at 2-5 pieces");
            }
        }
    }

    private static void summon() throws Exception {
        Player owner = player(Skill.DE_TRUNG);
        owner.setClothes.pikkoroDaimao = 5;
        MobMe pet = new MobMe(owner);
        owner.mobMe = pet;
        Player attacker = player(Skill.DRAGON);
        int hp = pet.point.hp;
        for (int i = 0; i < 25; i++) pet.injured(attacker, Integer.MAX_VALUE, false);
        check(owner.mobMe == pet && pet.point.hp == hp, "Pikkoro creature must be immune to damage");
        Field born = MobMe.class.getDeclaredField("lastTimeSpawn");
        born.setAccessible(true);
        born.setLong(pet, 0);
        pet.update();
        check(owner.mobMe == pet, "Pikkoro creature must survive past its lifetime");
        Zone next = new Zone(owner.zone.map, 1, 20);
        pet.goToMap(next);
        check(pet.zone == next, "Creature must retain map movement");
        owner.setClothes.pikkoroDaimao = 4;
        pet.update();
        check(owner.mobMe == null, "Removing a piece must expire the old creature");
        pet.update(); // Disposed lifecycle must be safe.
        owner = player(Skill.DE_TRUNG);
        owner.setClothes.pikkoroDaimao = 5;
        pet = new MobMe(owner);
        owner.mobMe = pet;
        owner.setClothes.pikkoroDaimao = 4;
        pet.update();
        check(pet.point.dame == 3000, "Creature damage must lose the set bonus without resummoning");
        hp = pet.point.hp;
        pet.injured(attacker, 100, false);
        check(pet.point.hp == hp - 100, "Removing a piece must restore incoming damage");
        owner.setClothes.pikkoroDaimao = 5;
        pet.update();
        check(pet.point.dame == 6000, "Re-equipping Pikkoro must restore damage without stacking");
        owner.setDie();
        check(owner.mobMe == null, "Owner death must remove even protected creature");
    }

    private static void loot() throws Exception {
        Player p = player(Skill.DRAGON);
        Mob mob = new Mob();
        mob.tempId = 1;
        mob.zone = p.zone;
        for (int pieces = 0; pieces <= 5; pieces++) {
            long seed = -1;
            int baseGold = 0;
            for (int i = 0; i < 100; i++) {
                Random expectedRandom = new Random(i);
                long denominator = pieces == 5 ? 200000 : 20;
                long numerator = pieces == 5 ? 25000 : 1;
                if (expectedRandom.nextLong(denominator) < numerator) {
                    seed = i;
                    baseGold = 500 + expectedRandom.nextInt(2501);
                    break;
                }
            }
            check(seed >= 0, "Gold seed fixture");
            p.setClothes.gohan = (byte) pieces;
            seed(seed);
            List<ItemMap> items = mob.getItemMobReward(p, 0, 0);
            ItemMap gold = items.stream().filter(it -> it.itemTemplate.id == 76
                    || it.itemTemplate.id == 188 || it.itemTemplate.id == 189 || it.itemTemplate.id == 190)
                    .findFirst().orElseThrow();
            check(gold.quantity == (pieces == 5 ? baseGold * 130 / 100 : baseGold),
                    "Gohan must add exactly 30% to dropped gold only at 5 pieces");
            p.zone.items.clear();
        }
        // Actual dragon-ball drop rolls, seeded identically for both sets.
        int[] count = new int[2];
        for (int mode = 0; mode < 2; mode++) {
            p.setClothes.gohan = (byte) (mode == 0 ? 4 : 5);
            seed(42);
            for (int i = 0; i < 20000; i++) {
                for (ItemMap it : mob.getItemMobReward(p, 0, 0)) {
                    if (it.itemTemplate.id >= 17 && it.itemTemplate.id <= 20) count[mode]++;
                }
                p.zone.items.clear();
            }
        }
        check(count[1] > count[0] * 2 && count[1] < count[0] * 3,
                "Gohan must multiply loot chance by 2.5: " + count[0] + " -> " + count[1]);
        p.setClothes.gohan = 4;
        check((int) call(mob, "goldDropAmount", new Class<?>[]{Player.class, int.class}, p, 1000) == 1000,
                "Gohan gold bonus must disappear after removing a piece");
        p.setClothes.gohan = 5;
        check((int) call(mob, "goldDropAmount", new Class<?>[]{Player.class, int.class}, p, Integer.MAX_VALUE)
                == Integer.MAX_VALUE, "Gohan gold amount must saturate without overflow");
        Class<?>[] signature = {Player.class, double.class, long.class};
        for (int i = 0; i < 1000; i++) {
            // Existing option 236 gives 20%, clover gives 50%; Gohan multiplies the resulting 3.6 by 2.5.
            seed(i);
            boolean actual = (boolean) call(mob, "isLuckyDrop", signature, p, 2 * 1.2 * 1.5, 9999L);
            check(actual == (new Random(i).nextLong(9999L * 10000) < 90000),
                    "Luck must retain fractions and stack with existing bonuses");
        }
        check(!(boolean) call(mob, "isLuckyDrop", signature, p, 0.0, 100L), "Zero chance must remain zero");
        check((boolean) call(mob, "isLuckyDrop", signature, p, 80.0, 100L), "Boosted chance must cap at 100%");
    }

    private static void existing() throws Exception {
        for (int skill : new int[]{Skill.KAMEJOKO, Skill.GALICK, Skill.LIEN_HOAN}) {
            Player p = player(skill);
            if (skill == Skill.KAMEJOKO) p.setClothes.songoku = 5;
            if (skill == Skill.GALICK) p.setClothes.kakarot = 5;
            if (skill == Skill.LIEN_HOAN) p.setClothes.ocTieu = 5;
            int damage = p.nPoint.getDameAttack(false);
            check(damage >= 19000 && damage <= 21001, "Existing +100% set must keep its effect");
        }
        Player p = player(Skill.DRAGON);
        p.nPoint.hpg = 10000;
        p.setClothes.nappa = 5;
        call(p.nPoint, "setHpMax", new Class<?>[0]);
        check(p.nPoint.hpMax == 18000, "Nappa must retain +80% HP");
        p.setClothes.nappa = 4;
        call(p.nPoint, "setHpMax", new Class<?>[0]);
        check(p.nPoint.hpMax == 10000, "Removing Nappa must restore HP");
        for (int pieces = 0; pieces <= 5; pieces++) {
            p.setClothes.cadicM = (byte) pieces;
            call(p.nPoint, "setHpMax", new Class<?>[0]);
            check(p.nPoint.hpMax == (pieces >= 2 ? 12000 : 10000), "Cadic M HP threshold must remain 2 pieces");
        }
        p = player(Skill.QUA_CAU_KENH_KHI);
        for (int pieces = 4; pieces <= 5; pieces++) {
            p.setClothes.kirin = (byte) pieces;
            int result = p.nPoint.getDameAttack(false);
            int expected = pieces == 5 ? 200000 : 100000;
            check(result >= expected * 95 / 100 && result <= expected * 105 / 100,
                    "Kirin must double QCKK damage only at 5 pieces");
        }
        p = player(Skill.THAI_DUONG_HA_SAN);
        Mob stunned = new Mob();
        stunned.zone = p.zone;
        p.zone.mobs.add(stunned);
        for (int pieces = 4; pieces <= 5; pieces++) {
            p.setClothes.thienXinHang = (byte) pieces;
            SkillService.gI().useSkillAlone(p);
            check(stunned.effectSkill.timeStun == SkillUtil.getTimeStun(1) * (pieces == 5 ? 2 : 1),
                    "Thien Xin Hang must double stun only at 5 pieces");
        }
        p = player(Skill.BIEN_KHI);
        for (int pieces = 4; pieces <= 5; pieces++) {
            p.setClothes.cadic = (byte) pieces;
            EffectSkillService.gI().setIsMonkey(p);
            check(p.effectSkill.timeMonkey == SkillUtil.getTimeMonkey(1) * (pieces == 5 ? 5 : 1),
                    "Cadic must multiply monkey duration by 5 only at 5 pieces");
        }
        p = player(Skill.MASENKO);
        for (int pieces = 0; pieces <= 5; pieces++) {
            p.setClothes.nail = (byte) pieces;
            p.effectSkill.isIntrinsic = false;
            long before = System.currentTimeMillis();
            call(SkillService.gI(), "setLastTimeUseSkill", new Class<?>[]{Player.class, int.class}, p, (int) Skill.MASENKO);
            if (pieces >= 4) {
                long offset = before - p.effectSkill.lastTimeUseSkill;
                check(Math.abs(offset - (pieces == 5 ? 5000 : 2000)) < 100,
                        "Nail must retain 20/50% cooldown reductions");
            } else check(!p.effectSkill.isIntrinsic, "Nail cooldown needs 4 pieces");
        }
    }

    private static void check(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
