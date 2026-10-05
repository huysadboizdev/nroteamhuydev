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
    public static void main(String[] args)throws Exception{
        verify("run","DEFEATED",3000,"COOLDOWN");verify("other","DEFEATED",0,"UNKNOWN");
        var m=new Message(-58);BossNoticeService.write(m,new BossNoticeRow("s","old",1,0,"Fide","Núi",1000,0,"","ALIVE",3000,"COOLDOWN",1,1000),"new");
        var r=new DataInputStream(new ByteArrayInputStream(m.getData()));for(int i=0;i<4;i++)r.readUTF();check(r.readUTF().equals("PREVIOUS_RUN"),"restart invalidates stale alive state");
        System.out.println("PASS BossNotice wire: exact fields without zone, durable killer/times, restart invalidates ETA/alive");
    }
}
