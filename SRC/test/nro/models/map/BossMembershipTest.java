package nro.models.map;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import nro.models.boss.Boss;
import nro.models.boss.Cell.SieuBoHung;
import nro.models.boss.luyen_tap_tu_dong.TrainingBoss;
import nro.models.map.service.ChangeMapService;
import nro.models.network.Message;
import nro.models.player.Player;
import nro.models.services.PinkFixture;

public final class BossMembershipTest {
    public static class Viewer extends Player {
        final List<Integer> appearances = new ArrayList<>();
        @Override public void sendMessage(Message m) {
            if (m.command == -5) {
                try { appearances.add(new java.io.DataInputStream(new java.io.ByteArrayInputStream(m.getData())).readInt()); }
                catch (Exception e) { throw new AssertionError(e); }
            }
        }
    }
    static void check(boolean value, String text) { PinkFixture.check(value,text); }
    static Player fake(Zone zone, long id) {
        Player p=new Player();p.id=id;p.isBoss=true;p.isPlayer=false;p.idNRNM=-1;p.zone=zone;return p;
    }
    static <T extends Boss> T real(Class<T> type, Zone zone, long id) throws Exception {
        T b=PinkFixture.blank(type);Player base=fake(zone,id);
        for(Field f:Player.class.getDeclaredFields())if(!Modifier.isStatic(f.getModifiers())){f.setAccessible(true);f.set(b,f.get(base));}
        b.isNotifyDisabled=true;b.currentLevel=0;b.name="fixture boss";
        Field data=Boss.class.getDeclaredField("data");data.setAccessible(true);
        data.set(b,new nro.models.boss.BossData[]{nro.models.boss.BossesData.SIEU_BO_HUNG_1});
        b.nPoint.hp=100;b.nPoint.hpMax=100;return b;
    }
    static int petCount(Zone z,Player p)throws Exception{Field f=Zone.class.getDeclaredField("pets");f.setAccessible(true);return Collections.frequency((List<?>)f.get(z),p);}
    static int count(List<Player> list,Player p){return Collections.frequency(list,p);}
    static void once(Zone z,Player p){check(count(z.getHumanoids(),p)==1&&count(z.getBosses(),p)==1,"exactly one registration");check(z.getNumOfPlayers()==0,"boss consumes no player slots");}
    static void run(String mode)throws Exception {
        PinkFixture.install();Zone z=PinkFixture.zone();Player p=fake(z,-901);
        if(mode.equals("preassigned")){
            check(ChangeMapService.gI().goToMap(p,z),"accepted");once(z,p);
        } else if(mode.equals("repeat")){
            for(int i=0;i<20;i++)z.addPlayer(p);once(z,p);
        } else if(mode.equals("partial")){
            z.getHumanoids().add(p);check(ChangeMapService.gI().goToMap(p,z),"partial admitted");once(z,p);
            z.getHumanoids().clear();check(ChangeMapService.gI().goToMap(p,z),"opposite partial admitted");once(z,p);
        } else if(mode.equals("remove")){
            z.addPlayer(p);z.addPlayer(p);z.removePlayer(p);
            check(!z.getBosses().contains(p)&&!z.getHumanoids().contains(p),"departure removes all memberships");
        } else if(mode.equals("race")){
            for(int round=0;round<30;round++){
                Zone target=PinkFixture.zone();Player b=fake(target,-902);CountDownLatch start=new CountDownLatch(1);
                AtomicReference<Throwable> error=new AtomicReference<>();List<Thread> workers=new ArrayList<>();
                for(int i=0;i<8;i++){Thread t=new Thread(()->{try{start.await();for(int j=0;j<10;j++)target.addPlayer(b);}catch(Throwable e){error.set(e);}});workers.add(t);t.start();}
                start.countDown();for(Thread t:workers){t.join(3000);check(!t.isAlive(),"registration finishes");}
                if(error.get()!=null)throw new AssertionError(error.get());once(target,b);
            }
        } else if(mode.equals("pet")){
            p.isBoss=false;p.isPet=true;for(int i=0;i<10;i++)z.addPlayer(p);
            check(petCount(z,p)==1&&count(z.getHumanoids(),p)==1&&z.getNumOfPlayers()==0,"pet unique and not player");
        } else if(mode.equals("cellwire")){
            Boss b=real(SieuBoHung.class,z,-101);check(ChangeMapService.gI().goToMap(b,z),"real Cell committed");
            Viewer late=new Viewer();late.id=33;late.isPlayer=true;late.zone=z;z.addPlayer(late);
            z.load_Another_To_Me(late);check(late.appearances.contains(-101),"late entrant receives Cell appearance");
            late.appearances.clear();z.removePlayer(late);z.addPlayer(late);z.load_Another_To_Me(late);
            check(Collections.frequency(late.appearances,-101)==1,"reconnect receives one Cell appearance");
        } else if(mode.equals("offline")){
            z.map.mapId=21;z.map.type=nro.models.consts.ConstMap.MAP_OFFLINE;
            nro.models.server.Manager.MAPS.clear();nro.models.server.Manager.MAPS.add(z.map);
            TrainingBoss b=real(nro.models.boss.luyen_tap_tu_dong.Whis.class,z,-990);Viewer owner=new Viewer(),stranger=new Viewer();
            owner.id=1;owner.isPlayer=true;owner.zone=z;stranger.id=2;stranger.isPlayer=true;stranger.zone=z;b.playerAtt=owner;
            z.addPlayer(owner);z.addPlayer(stranger);check(ChangeMapService.gI().goToMap(b,z),"training boss registered");
            z.load_Another_To_Me(owner);z.load_Another_To_Me(stranger);
            check(owner.appearances.contains(-990)&&!stranger.appearances.contains(-990),"offline owner isolation");
        } else if(mode.equals("closing")){
            z.dungeonClosing=true;check(!ChangeMapService.gI().goToMap(p,z)&&z.getHumanoids().isEmpty(),"closed admission does not register ghost");
        } else if(mode.equals("defaultcapacity")){
            z.map.zones=new ArrayList<>();z.map.zones.add(z);Zone next=new Zone(z.map,1,20);z.map.zones.add(next);
            nro.models.server.Manager.MAPS.clear();nro.models.server.Manager.MAPS.add(z.map);
            Player user=new Player();user.isPlayer=true;user.zone=z;
            for(int i=0;i<7;i++){Player q=new Player();q.id=100+i;q.isPlayer=true;z.addPlayer(q);}
            check(ChangeMapService.gI().getMapCanJoin(user,5)==z,"seven people do not make twenty-slot zone full");
        } else if(mode.equals("bossmove")){
            z.map.zones=new ArrayList<>();z.map.zones.add(z);Zone next=new Zone(z.map,1,20);z.map.zones.add(next);
            nro.models.server.Manager.MAPS.clear();nro.models.server.Manager.MAPS.add(z.map);
            Boss b=real(SieuBoHung.class,z,-101);z.addPlayer(b);b.joinMapByZone(next);
            check(b.zone==next&&next.getBosses().contains(b)&&!z.getBosses().contains(b)&&!z.getHumanoids().contains(b),"boss moves without leaving old membership");
        } else if(mode.equals("forcedzero")){
            Zone origin=PinkFixture.zone();origin.map.mapId=126;
            z.map.mapName="fixture";
            z.map.zones=new ArrayList<>();z.map.zones.add(z);z.map.zones.add(new Zone(z.map,1,20));
            nro.models.server.Manager.MAPS.clear();nro.models.server.Manager.MAPS.add(z.map);nro.models.server.Manager.MAPS.add(origin.map);
            Player resident=new Player();resident.id=42;resident.isPlayer=true;z.addPlayer(resident);
            Viewer user=new Viewer();user.id=43;user.isPlayer=true;user.idNRNM=-1;user.zone=origin;origin.addPlayer(user);
            check(nro.models.services.DungeonExitService.exit(user,origin,5,100,100)&&user.zone==z,"forced exit uses zone zero while it has space");
        } else throw new AssertionError("unknown mode "+mode);
    }
    public static void main(String[] args){try{run(args[0]);System.out.println("BOSS_MEMBERSHIP PASS "+args[0]);System.exit(0);}catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
