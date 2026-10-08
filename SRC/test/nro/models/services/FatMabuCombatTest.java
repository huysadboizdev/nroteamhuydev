package nro.models.services;

import nro.models.boss.Boss;
import nro.models.boss.BossData;
import nro.models.consts.BossStatus;
import nro.models.player.*;
import nro.models.skill.Skill;
import static nro.models.services.PinkFixture.*;

public final class FatMabuCombatTest {
    public static void main(String[] args)throws Exception {
        install();
        Class<?> type;
        try {type=Class.forName("nro.models.boss.Mabu.FatMabuTournament");}
        catch(ClassNotFoundException absent){throw new AssertionError("missing approved 300M tournament Mabu",absent);}
        Boss b=(Boss)blank(type);b.id=-927;b.isBoss=true;b.bossStatus=BossStatus.ACTIVE;b.zone=zone();
        b.nPoint=blank(NPoint.class);b.nPoint.hpMax=300_000_000;b.nPoint.hp=300_000_000;
        b.effectSkill=new EffectSkill(b);
        b.playerSkill=new nro.models.skill.PlayerSkill(b);
        Player a=player(91);a.zone=b.zone;
        check(b.injured(a,80_000_000,false,false)==80_000_000&&b.nPoint.hp==220_000_000,"ordinary damage is not capped");
        b.effectSkill.isShielding=true;
        check(b.injured(a,80_000_000,false,false)==1&&b.nPoint.hp==219_999_999,"shield actually reduces incoming damage");
        check(b.injured(a,10_000_000,true,false)==10_000_000,"piercing passes shield");
        b.effectSkill.isShielding=false;
        check(b.injured(a,10_000_000,false,false)==10_000_000,"after shield ends full damage returns");
        check(b.injured(null,50,false,false)==0,"null attacker is safe");
        b.bossStatus=BossStatus.CHAT_S;check(b.injured(a,50,true,false)==0,"opening conversation protected");
        b.bossStatus=BossStatus.ACTIVE;
        Player reflector=new Player();reflector.zone=b.zone;reflector.nPoint.tlPST=100;b.nPoint.hp=300_000_000;
        var reflect=SkillService.class.getDeclaredMethod("phanSatThuong",Player.class,Player.class,long.class);reflect.setAccessible(true);
        reflect.invoke(SkillService.gI(),b,reflector,50_000_000L);
        check(b.nPoint.hp==250_000_000,"reflection must not retain generic boss1percent cap");
        Player p=new Player();p.zone=b.zone;p.nPoint.hpMax=1_000_000;p.nPoint.hp=1_000_000;
        p.nPoint.def=999_999;p.nPoint.tlGiap=86;p.effectSkill.isShielding=true;p.itemTime.isUseGiapXen2=true;
        check(BossNoticeService.injure(p,b,123,false,false)==100_000&&p.nPoint.hp==900_000,"10% pierces actual armor/shield/GiapXen");
        p.nPoint.hpMax=2_000_000_000;p.nPoint.hp=2_000_000_000;
        check(BossNoticeService.injure(p,b,123,false,false)==200_000_000,"large target long math uses its own MAX HP");
        check(BossNoticeService.injure(p,b,0,false,false)==0,"miss remains zero");
        p.lastTimeRevived=System.currentTimeMillis();check(BossNoticeService.injure(p,b,123,false,false)==0,"revive immunity remains");
        p.lastTimeRevived=0;p.zone=zone();check(BossNoticeService.injure(p,b,123,false,false)==0,"no damage after leaving zone");
        BossData data=(BossData)type.getMethod("configuration").invoke(null);
        check(data.getHp()[0]==300_000_000&&data.getMapJoin()[0]==52,"approved HP/map");
        check(data.getSkillTemp().length==5,"four Cell skills plus shield");
        boolean shield=false;for(int[] s:data.getSkillTemp()){check(s[0]!=Skill.TAI_TAO_NANG_LUONG,"no regeneration");if(s[0]==Skill.KHIEN_NANG_LUONG){shield=true;check(s[1]==7&&s[2]==100_000,"shield level/cooldown");}}
        check(shield,"shield configured");
        System.out.println("PASS Mabu combat: 300M, uncapped damage, temporary shield, 10% per target piercing, skill/map guards");
    }
}
