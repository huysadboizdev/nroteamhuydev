package nro.models.services_dungeon;

import java.util.List;
import java.util.ArrayList;
import nro.models.clan.Clan;
import nro.models.clan.ClanMember;
import nro.models.map.phoban.SnakeWay;
import nro.models.player.Player;
import nro.models.services.PinkFixture;
import nro.models.services.Service;

public final class SnakeWayWeeklyAdmissionTest {
    static class Instance extends SnakeWay {
        int openings;
        Instance() { super(0); }
        @Override public void openConDuongRanDoc(Player p, Clan owner, byte level) {
            openings++; clan=owner;owner.ConDuongRanDoc=this;isOpened=true;isInitializing=false;isClosing=true;
        }
    }
    public static void main(String[] args) {
        try {
            PinkFixture.install();
            List<String> notices = new ArrayList<>();
            PinkFixture.replace(Service.class,"instance",new Service(){ @Override public void sendThongBao(Player p,String text){ notices.add(text); } });
            var p=PinkFixture.player(9400);p.clan=new Clan();p.clanMember=new ClanMember();p.clanMember.id=(int)p.id;p.clan.members.add(p.clanMember);
            p.zone=PinkFixture.zone();p.idNRNM=-1;p.location=new nro.models.player.Location();
            var service=SnakeWayService.gI();var instance=new Instance();service.conDuongRanDocs=List.of(instance);
            p.joinCDRD=true;p.lastTimeJoinCDRD=System.currentTimeMillis();
            service.openConDuongRanDoc(p,(byte)1);
            PinkFixture.check(instance.openings==0 && p.clan.ConDuongRanDoc==null,"completed online attempt cannot bypass seven-day cooldown via stale joinCDRD");
            String cooldownNotice=notices.get(0);
            notices.clear();p.clan.ConDuongRanDoc=instance;instance.clan=p.clan;instance.isOpened=true;instance.isClosing=false;p.clan.lastTimeOpenConDuongRanDoc=p.lastTimeJoinCDRD;
            p.talkToThanMeo=true;p.talkToThuongDe=true;
            service.openConDuongRanDoc(p,(byte)1);
            PinkFixture.check(!notices.contains(cooldownNotice) && p.talkToThanMeo && p.talkToThuongDe,"current attempt can rejoin and keeps NPC progress");
            p.clan.ConDuongRanDoc=null;instance.clan=null;instance.isOpened=false;
            p.lastTimeJoinCDRD=System.currentTimeMillis()-8L*86400000;
            service.openConDuongRanDoc(p,(byte)1);
            PinkFixture.check(instance.openings==1 && !p.talkToThanMeo && !p.talkToThuongDe,"new attempt after cooldown starts with fresh NPC progress");
            System.out.println("SNAKE_WEEKLY_ADMISSION PASS stale online flag, current rejoin, fresh eligible attempt");System.exit(0);
        } catch(Throwable e){e.printStackTrace();System.exit(1);}
    }
}
