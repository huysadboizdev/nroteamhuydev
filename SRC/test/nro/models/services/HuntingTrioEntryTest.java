package nro.models.services;

import java.util.HashSet;
import java.util.Set;
import nro.models.boss.HuntingTrio.*;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstPlayer;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.skill.NClass;
import nro.models.skill.Skill;
import static nro.models.services.PinkFixture.*;

/** Real boss construction, initBase, prepare and ChangeMapService admission; only skill data/packets are fixtures. */
public final class HuntingTrioEntryTest {
    static void skills() {
        Manager.NCLASS.clear();for(int i=0;i<3;i++)Manager.NCLASS.add(new NClass());
        for(byte id:new byte[]{Skill.KAMEJOKO,Skill.DICH_CHUYEN_TUC_THOI,Skill.GALICK,Skill.THAI_DUONG_HA_SAN}) {
            Template.SkillTemplate template=new Template.SkillTemplate();template.id=id;template.name="fixture";
            for(int level=1;level<=7;level++){Skill skill=new Skill();skill.template=template;skill.point=level;skill.skillId=(short)(id*10+level);template.skillss.add(skill);}
            Manager.NCLASS.get(0).skillTemplatess.add(template);
        }
    }
    static HuntingTrioLifecycle lifecycle() {
        HuntingTrioLifecycle state=new HuntingTrioLifecycle(new HuntingTrioLifecycle.Backend(){
            public boolean spawn(){return true;}public void chat(int member){}public void activate(){}
            public void removeMember(int member){}public void removeAll(){}public boolean playersPresent(){return true;}public boolean intact(){return true;}
        },()->0L);state.tick();return state;
    }
    public static void main(String[] args)throws Exception {
        try {
            install();skills();Set<Long> ids=new HashSet<>();
            for(int group=0;group<2;group++) {
                Zone zone=zone();zone.map.mapId=group==0?0:14;
                HuntingTrioLifecycle state=lifecycle();Player attacker=player(91);attacker.zone=zone;
                int[] inputs={-50,120,500};int[] expected={0,120,239};
                for(int member=0;member<3;member++) {
                    HuntingTrioBoss boss=new HuntingTrioBoss(group,member,state);
                    check(boss.zone==null,"brand-new boss starts without a zone");
                    try {boss.prepare(zone,inputs[member]);}
                    catch(RuntimeException failure){throw new AssertionError("real first-entry prepare must register new hunting boss in its reserved zone",failure);}
                    check(boss.zone==zone&&zone.getBosses().contains(boss)&&zone.getHumanoids().contains(boss),"real ChangeMapService registers boss in both zone lists");
                    check(zone.getBosses().size()==member+1,"each of three members publishes exactly once");
                    check(boss.location.x==expected[member]&&boss.location.y==100+expected[member],"entry uses clamped terrain position");
                    check(boss.typePk==ConstPlayer.NON_PK&&boss.bossStatus==BossStatus.CHAT_S,"new boss protected during group conversation");
                    check(boss.nPoint.hp==1000&&boss.nPoint.hpMax==1000&&boss.injured(attacker,999999,true,false)==0&&boss.nPoint.hp==1000,"protected boss cannot lose HP during conversation");
                    check(boss.playerSkill.skills.size()==4,"real initBase creates four configured skills");
                    check(ids.add(boss.id),"all six real boss network identities distinct");
                }
                check(zone.getBosses().size()==3,"all three group members share reserved zone");
            }
            System.out.println("PASS HuntingTrioEntry real construction/prepare/map admission: six members, shared zones, protection, clamped terrain, distinct IDs");System.exit(0);
        } catch(Throwable failure){failure.printStackTrace();System.exit(1);}
    }
}
