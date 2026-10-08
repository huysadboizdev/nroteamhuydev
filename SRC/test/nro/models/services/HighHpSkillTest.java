package nro.models.services;

import nro.models.player.*;
import nro.models.skill.*;
import static nro.models.services.PinkFixture.*;

public final class HighHpSkillTest {
    public static void main(String[] args)throws Exception {
        for(int i=0;i<3;i++)nro.models.server.Manager.NCLASS.add(blank(NClass.class));
        replace(Service.class,"instance",new Service(){
            @Override public void Send_Caitrang(Player p){}
            @Override public void Send_Info_NV(Player p){}
            @Override public void sendInfoPlayerEatPea(Player p){}
            @Override public void point(Player p){}
            @Override public void sendMessAllPlayerInMap(Player p,nro.models.network.Message m){}
        });
        replace(PlayerService.class,"i",new PlayerService(){@Override public void sendInfoHpMp(Player p){}});
        EffectSkillService effects=blank(EffectSkillService.class);
        replace(EffectSkillService.class,"instance",effects);
        Player p=player(91);p.nPoint=blank(NPoint.class);p.effectSkill=blank(EffectSkill.class);
        p.setClothes=blank(SetClothes.class);p.playerSkill=blank(PlayerSkill.class);
        p.playerSkill.skillSelect=new Skill();p.playerSkill.skillSelect.point=7;
        p.playerSkill.skillSelect.template=new nro.models.player_system.Template.SkillTemplate();
        p.playerSkill.skillSelect.template.id=Skill.BIEN_KHI;
        p.playerSkill.skills=java.util.List.of(p.playerSkill.skillSelect);
        for(int hp:new int[]{100,1_073_741_824,1_500_000_000,2_000_000_000}) {
            p.nPoint.hp=hp;p.nPoint.hpMax=Integer.MAX_VALUE;effects.setIsMonkey(p);
            check(p.nPoint.hp==Math.min(2L*hp,Integer.MAX_VALUE),"monkey must not lose HP: input="+hp+" output="+p.nPoint.hp);
        }
        System.out.println("PASS real monkey skill: low HP, signed overflow boundary, 1.5B and 2B HP");
    }
}
