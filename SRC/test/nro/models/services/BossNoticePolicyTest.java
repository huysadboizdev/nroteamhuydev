package nro.models.services;
import java.lang.reflect.*;
import java.util.*;
import nro.models.boss.*;
import nro.models.consts.*;
import nro.models.player.*;
import nro.models.network.*;
import nro.models.boss.notice.*;
import static nro.models.services.PinkFixture.*;
public final class BossNoticePolicyTest {
    static void field(Object object,Class<?> type,String name,Object value)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
    public static class Viewer extends Player {int messages;@Override public void sendMessage(Message m){messages++;}}
    public static class Probe extends Boss {
        public Probe()throws Exception{super(0,(BossData)null);}
        public Player seen;public boolean fail;
        @Override public int injured(Player p,long damage,boolean piercing,boolean mob){seen=BossNoticeService.deathAttacker(this);if(fail)throw new IllegalStateException("test");return 1;}
    }
    public static class Conditional extends Probe {public Conditional()throws Exception{}@Override public void rest(){}}
    public static void main(String[] args)throws Exception{
        var service=blank(BossNoticeService.class);
        field(service,BossNoticeService.class,"run","run");field(service,BossNoticeService.class,"stored",List.of());
        field(service,BossNoticeService.class,"tracker",new BossNoticeTracker("run",System::currentTimeMillis,r->{}));
        Viewer p=blank(Viewer.class);p.session=blank(nro.models.shop_ky_gui.ConsignRegressionTest.Connected.class);p.session.player=p;p.session.bossNoticeBoard=true;
        service.request(p.session,0,0,0);service.request(p.session,0,0,0);service.request(p.session,1,0,0);check(p.messages==1,"zero and positive requests share throttle");
        p.session.lastBossNoticeRequest=0;service.request(p.session,2,0,500);service.request(p.session,2,2,0);check(p.messages==1,"invalid pages/kinds denied");
        p.session.bossNoticeBoard=false;service.request(p.session,2,0,0);check(p.messages==1,"legacy client cannot receive new packet");
        p.session.bossNoticeBoard=true;p.session.player=null;service.request(p.session,2,0,0);check(p.messages==1,"logged out denied");
        Probe b=blank(Probe.class);Player attacker=player(1);BossNoticeService.injure(b,attacker,1,false,false);check(b.seen==attacker&&BossNoticeService.deathAttacker(b)==null,"kill context is scoped to damage call");
        b.fail=true;try{BossNoticeService.injure(b,attacker,1,false,false);}catch(IllegalStateException expected){}check(BossNoticeService.deathAttacker(b)==null,"context removed after throwing attack");
        BossData data=blank(BossData.class);data.setTypeAppear(AppearType.DEFAULT_APPEAR);field(b,Boss.class,"data",new BossData[]{data});b.currentLevel=0;b.bossStatus=BossStatus.REST;
        field(b,Boss.class,"lastTimeRest",1000L);field(b,Boss.class,"secondsRest",300);check(b.noticeNextSpawnAt()==301000,"real rest timer");
        field(b,Boss.class,"parentBoss",b);check(b.noticeNextSpawnAt()==0,"parent spawn cannot advertise timer");field(b,Boss.class,"parentBoss",null);
        Conditional c=blank(Conditional.class);field(c,Boss.class,"data",new BossData[]{data});c.currentLevel=0;c.bossStatus=BossStatus.REST;field(c,Boss.class,"lastTimeRest",1000L);check(c.noticeNextSpawnAt()==0,"custom rest never invents ETA");
        b.zone=zone();check(b.noticePublic(),"world map boss visible");b.zone.map.mapId=140;check(!b.noticePublic(),"private training map excluded");b.zone.map.mapId=5;b.isNotifyDisabled=true;check(!b.noticePublic(),"hidden bosses excluded");
        System.out.println("PASS BossNotice policy: request0 throttle, bad requests/logout/legacy guard, scoped damage attribution, real/conditional timer and public scope");
    }
}
