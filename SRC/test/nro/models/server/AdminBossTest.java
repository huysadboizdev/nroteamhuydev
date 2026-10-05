package nro.models.server;

import java.util.*;
import java.lang.reflect.*;
import java.io.*;
import nro.models.boss.*;
import nro.models.boss.Boss_Manager.*;
import nro.models.player.*;
import nro.models.network.*;
import nro.models.map.*;
import nro.models.map.service.*;
import nro.models.consts.BossStatus;
import nro.models.services.*;
import static nro.models.services.PinkFixture.*;

public class AdminBossTest {
    public static class Viewer extends Owner { byte[] packet; @Override public void sendMessage(Message m){packet=m.getData();} }
    static final List<String> notices=new ArrayList<>(); static Zone destination; static int x,y;
    static Viewer viewer(int version)throws Exception {
        Viewer p=blank(Viewer.class);p.idMark=new IDMark();p.session=blank(MySession.class);
        p.session.isAdmin=true;p.session.player=p;p.session.version=version;return p;
    }
    static Boss boss(int id,int zoneId)throws Exception {
        Boss b=blank(Boss.class);b.id=id;b.isBoss=true;b.nPoint=blank(NPoint.class);b.nPoint.hp=100;
        b.location=new Location();b.location.x=70;b.location.y=90;
        b.bossStatus=BossStatus.ACTIVE;
        Field f=Boss.class.getDeclaredField("data");f.setAccessible(true);f.set(b,new BossData[]{new BossData("test",(byte)0,new short[]{1,2,3},10,new int[]{100},new int[]{5},new int[0][],new String[0],new String[0],new String[0],10)});
        b.zone=zone();b.zone.zoneId=zoneId;b.zone.map.mapHeight=240;b.zone.addPlayer(b);return b;
    }
    static void resolveSelection(Player p,int id)throws Exception {
        try{Class<?> c=Class.forName("nro.models.services.AdminCommandService"); destination=null;Boss selected=(Boss)c.getMethod("resolveBossSelection",Player.class,int.class).invoke(null,p,id);if(selected!=null){destination=selected.zone;x=selected.location.x;y=selected.location.y;}}
        catch(ClassNotFoundException|NoSuchMethodException e){throw new AssertionError("stable boss resolver missing",e);}
    }
    static int firstId(Viewer p)throws Exception {
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(p.packet));in.readByte();in.readUTF();
        check(in.readByte()>0,"empty boss packet"); return in.readInt();
    }
    static void verifyPacket(Viewer p,int expected)throws Exception {
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(p.packet));
        in.readByte();in.readUTF();int count=in.readByte();check(count==expected,"row count mismatch");
        for(int i=0;i<count;i++){
            in.readInt();in.readInt();in.readShort();if(p.session.version>=214)check(in.readShort()==-1,"new client marker");
            in.readShort();in.readShort();in.readUTF();in.readUTF();in.readUTF();
        }
        check(in.available()==0,"extra/missing packet rows");
    }
    public static void main(String[] args)throws Exception {
        install();replace(Service.class,"instance",new Service(){@Override public void sendThongBao(Player p,String s){notices.add(s);}});

        BossManager m=new BossManager();Viewer p=viewer(Integer.parseInt(args[0]));Boss a=boss(-1,1),b=boss(-1,2);m.addBoss(a);m.addBoss(b);
        m.showListBoss(p);verifyPacket(p,2);int id=firstId(p);m.getBosses().remove(a);m.getBosses().add(0,b);
        resolveSelection(p,id);check(destination==null,"removed instance teleported");check(!notices.isEmpty(),"missing stale notice");
        m.getBosses().clear();m.addBoss(a);m.addBoss(b);m.showListBoss(p);id=firstId(p);
        a.location.x=85;a.location.y=95;resolveSelection(p,id);check(destination==a.zone&&x==85&&y==95,"wrong instance/zone/coordinates");
        int oldId=id;m.showListBoss(p);resolveSelection(p,oldId);check(destination==null,"previous snapshot token accepted");id=firstId(p);
        Zone moved=zone();moved.zoneId=4;moved.map.mapHeight=240;a.zone.removePlayer(a);a.zone=moved;moved.addPlayer(a);
        resolveSelection(p,id);check(destination==moved,"boss moved zone not resolved");
        destination=null;a.nPoint.hp=0;resolveSelection(p,id);check(destination==null,"dead boss teleport");a.nPoint.hp=100;
        p.session.isAdmin=false;resolveSelection(p,id);check(destination==null,"normal user teleport");p.session.isAdmin=true;
        p.idMark.setMenuType(999);resolveSelection(p,id);check(destination==null,"unrelated menu teleport");
        MySession oldSession=p.session;p.session=blank(MySession.class);p.session.player=p;p.session.isAdmin=true;
        p.idMark.setMenuType(3);resolveSelection(p,id);check(destination==null,"snapshot survived session replacement");p.session=oldSession;
        Field snapshots=Class.forName("nro.models.services.AdminCommandService").getDeclaredField("BOSS_LISTS");snapshots.setAccessible(true);
        java.util.Map<?,?> cache=(java.util.Map<?,?>)snapshots.get(null);Object snapshot=cache.get(p);
        var ctor=snapshot.getClass().getDeclaredConstructors()[0];ctor.setAccessible(true);
        Object expired=ctor.newInstance(new java.lang.ref.WeakReference<>(p.session),m,0L,java.util.Map.of(id,a));
        @SuppressWarnings("unchecked") java.util.Map<Object,Object> mutable=(java.util.Map<Object,Object>)cache;mutable.put(p,expired);
        resolveSelection(p,id);check(destination==null,"expired snapshot accepted");
        m.getBosses().clear();for(int i=0;i<140;i++)m.addBoss(boss(-i,0));m.showListBoss(p);
        DataInputStream in=new DataInputStream(new ByteArrayInputStream(p.packet));in.readByte();in.readUTF();check(in.readByte()==127,"boss byte count overflow");
        verifyPacket(p,127);
        System.out.println("PASS AdminBossTest version="+args[0]);System.exit(0);
    }
}
