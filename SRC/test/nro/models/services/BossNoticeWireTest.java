package nro.models.services;
import java.io.*;
import nro.models.network.Message;
import nro.models.boss.notice.BossNoticeRow;
public final class BossNoticeWireTest {
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    static void verify(String run,String status,long next,String eta)throws Exception{
        var row=new BossNoticeRow("spawn","run",1,0,"Fide","Núi",1000,2000,"admin","DEFEATED",3000,"COOLDOWN",3,2000);
        var m=new Message(-58);BossNoticeService.write(m,row,run);
        var r=new DataInputStream(new ByteArrayInputStream(m.getData()));
        check(r.readUTF().equals("spawn")&&r.readUTF().equals("Fide")&&r.readUTF().equals("Núi")&&r.readUTF().equals("admin"),"identity/map/killer");
        check(r.readUTF().equals(status)&&r.readLong()==1000&&r.readLong()==2000&&r.readLong()==next&&r.readUTF().equals(eta)&&r.available()==0,"times/status/ETA exact wire, no trailing zone");
    }
    public static class Capture extends nro.models.player.Player {
        byte[] packet;int sent;
        @Override public void sendMessage(Message message){packet=message.getData();sent++;}
    }
    static void set(Object object,String name,Object value)throws Exception{var f=BossNoticeService.class.getDeclaredField(name);f.setAccessible(true);f.set(object,value);}
    static void classified()throws Exception{
        var service=PinkFixture.blank(BossNoticeService.class);set(service,"run","run");set(service,"stored",java.util.List.of());
        long[] now={System.currentTimeMillis()};var tracker=new nro.models.boss.notice.BossNoticeTracker("run",()->now[0],row->{});set(service,"tracker",tracker);
        Object quest=new Object(),general=new Object(),dead=new Object();
        tracker.appear(quest,-900000,0,"Quest","Map",nro.models.boss.BossID.KUKU,true,7);
        tracker.appear(general,-799,0,"General","Map",nro.models.boss.BossID.MAT_TROI,false,5);
        tracker.appear(dead,-20,0,"Dead","Map",nro.models.boss.BossID.KUKU,false,8);tracker.defeat(dead,"admin");
        Capture player=PinkFixture.blank(Capture.class);player.session=PinkFixture.blank(nro.models.shop_ky_gui.ConsignRegressionTest.Connected.class);player.session.player=player;player.session.bossNoticeBoard=true;player.session.bossNoticeCategories=true;
        service.requestV2(player.session,5,1,0,0);
        var r=new DataInputStream(new ByteArrayInputStream(player.packet));
        check(r.readInt()==-2&&r.readInt()==5&&r.readUnsignedByte()==1&&r.readUnsignedByte()==0&&r.readUnsignedShort()==0&&r.readUnsignedShort()==1,"V2 request/category/view/page echo");r.readLong();r.readUTF();check(r.readUnsignedShort()==1,"category and alive filtered before count");
        for(int i=0;i<5;i++)r.readUTF();for(int i=0;i<3;i++)r.readLong();r.readUTF();
        check(r.readUnsignedByte()==1&&r.readInt()==nro.models.boss.BossID.KUKU&&r.readBoolean()&&r.readShort()==-1&&r.available()==0,"semantic identity support and masked zone exact V2 fields");
        service.requestV2(player.session,6,2,0,0);check(player.sent==1,"V2 shares server request throttle");
        player.session.lastBossNoticeRequest=0;service.requestV2(player.session,6,0,0,0);service.requestV2(player.session,6,1,2,0);service.requestV2(player.session,6,1,0,500);check(player.sent==1,"invalid category view page denied");
        service.requestV2(player.session,7,2,0,499);r=new DataInputStream(new ByteArrayInputStream(player.packet));r.readInt();r.readInt();check(r.readUnsignedByte()==2&&r.readUnsignedByte()==0&&r.readUnsignedShort()==0,"general category clamped final page");r.readShort();r.readLong();r.readUTF();check(r.readUnsignedShort()==1,"general excludes quests");
        player.session.lastBossNoticeRequest=0;player.session.bossNoticeCategories=false;service.requestV2(player.session,8,1,0,0);check(player.sent==2,"BOSS1 cannot receive V2");
        player.session.bossNoticeCategories=true;player.session.player=null;service.requestV2(player.session,8,1,0,0);check(player.sent==2,"logout denies classified data");
        player.session.player=player;player.session.lastBossNoticeRequest=0;service.requestV2(player.session,9,1,1,0);r=new DataInputStream(new ByteArrayInputStream(player.packet));r.readInt();r.readInt();r.readByte();r.readByte();r.readShort();r.readShort();r.readLong();r.readUTF();check(r.readUnsignedShort()==2,"quest history keeps defeated row");
    }
    public static void main(String[] args)throws Exception{
        classified();
        verify("run","DEFEATED",3000,"COOLDOWN");verify("other","DEFEATED",0,"UNKNOWN");
        var m=new Message(-58);BossNoticeService.write(m,new BossNoticeRow("s","old",1,0,"Fide","Núi",1000,0,"","ALIVE",3000,"COOLDOWN",1,1000),"new");
        var r=new DataInputStream(new ByteArrayInputStream(m.getData()));for(int i=0;i<4;i++)r.readUTF();check(r.readUTF().equals("PREVIOUS_RUN"),"restart invalidates stale alive state");
        System.out.println("PASS BossNotice wire: exact fields without zone, durable killer/times, restart invalidates ETA/alive");
    }
}
