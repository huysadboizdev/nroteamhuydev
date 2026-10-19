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
        b.zone=zone();check(b.noticePublic(),"world map boss visible");
        b.setQuestSupportIdentity(BossID.KUKU,-900000000);check(b.noticePublic(),"quest support enters public board");
        var notify=Boss.class.getDeclaredMethod("canSendNotify");notify.setAccessible(true);check(!(boolean)notify.invoke(b),"quest support still cannot broadcast spawn spam");
        field(b,Boss.class,"questSupportRetired",true);check(!b.noticePublic(),"retired support excluded");field(b,Boss.class,"questSupportRetired",false);
        b.zone.map.mapId=140;check(!b.noticePublic(),"private training map excluded");b.zone.map.mapId=5;b.isNotifyDisabled=true;check(!b.noticePublic(),"hidden bosses excluded");
        int[] twelveHour={BossID.DRABURA,BossID.BUI_BUI,BossID.YA_CON,BossID.MABU_12H,BossID.DRABURA_2,BossID.BUI_BUI_2,BossID.DRABURA_3,BossID.GOKU,BossID.CADIC};
        for(int id:twelveHour){
            check(BossAnnouncementPolicy.suppressedNoticeId(id,ConstMap.MAP_NORMAL),"12h identity suppressed at any location "+id);
        }
        check(BossAnnouncementPolicy.suppressedNoticeId(BossID.MABU,ConstMap.MAP_MABU_14H),"Mabu 14h identity and map type suppressed");
        check(BossAnnouncementPolicy.suppressedNoticeId(BossID.SUPERBU,ConstMap.MAP_MABU_14H),"SuperBu 14h identity and map type suppressed");
        check(!BossAnnouncementPolicy.suppressedNoticeId(BossID.MABU,ConstMap.MAP_NORMAL),"Mabu outside 14h map remains allowed");
        check(!BossAnnouncementPolicy.suppressedNoticeId(BossID.KUKU,ConstMap.MAP_NORMAL),"ordinary boss remains allowed");
        int[] specials=new int[]{BossID.AN_TROM,BossID.O_DO,BossID.O_DO1,BossID.SOI_HEC_QUYN1,BossID.MAT_TROI};
        String[] names=new String[]{"Ăn trộm","Ở dơ","Ở dơ","Sói hẹc quyn","Mặt Trời 12"};
        for(int i=0;i<specials.length;i++){
            BossNoticeRow row=new BossNoticeRow("spawn","run",specials[i],0,names[i],"Map",1000,0,"","ALIVE",0,"UNKNOWN",1,1000);
            check(BossAnnouncementPolicy.suppressedNoticeId(specials[i],ConstMap.MAP_NORMAL),"special semantic identity suppressed "+specials[i]);
            check(BossScreenNoticePolicy.hide(specials[i],1,ConstMap.MAP_NORMAL),"screen hides special "+specials[i]);
        }
        System.out.println("PASS BossNotice policy: request0 throttle, bad requests/logout/legacy guard, scoped damage attribution, real/conditional timer and public scope");
    }
}
