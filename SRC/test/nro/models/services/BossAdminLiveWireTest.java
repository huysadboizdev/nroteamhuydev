package nro.models.services;
import java.io.*;
import java.util.*;
import nro.models.boss.*;
import nro.models.network.Message;
import nro.models.player.Player;
import static nro.models.services.PinkFixture.*;

public final class BossAdminLiveWireTest {
 public static void main(String[] args)throws Exception {
  install();List<byte[]> packets=new ArrayList<>();
  replace(Service.class,"instance",new Service(){
   @Override public void sendMessAnotherNotMeInMap(Player player,Message message){
    check(message.command==-30,"Existing client subcommand envelope");packets.add(message.getData());
   }
  });
  Boss b=blank(Boss.class);b.id=BossID.FIDE;b.currentLevel=0;b.isBoss=true;b.bossStatus=nro.models.consts.BossStatus.ACTIVE;
  b.zone=zone();b.nPoint=new nro.models.player.NPoint(b);b.nPoint.hpg=b.nPoint.hpMax=b.nPoint.hp=100;b.nPoint.dameg=b.nPoint.dame=10;
  BossAdminConfig.stage(BossAdminConfigTest.db(1,List.of(BossAdminConfigTest.row(1,"next",100)))).run();
  BossAdminConfig.applyAtSpawn(b);b.nPoint.hp=25;check(packets.isEmpty(),"Spawn has its own publication");
  BossAdminConfig.stage(BossAdminConfigTest.db(2,List.of(BossAdminConfigTest.row(2,"next",200)))).run();
  BossAdminConfig.applyAtTick(b);check(b.nPoint.hpMax==100&&packets.isEmpty(),"Next does not change live boss or send packet");
  BossAdminConfig.stage(BossAdminConfigTest.db(3,List.of(BossAdminConfigTest.row(3,"alive",200)))).run();
  BossAdminConfig.applyAtTick(b);
  check(packets.size()==1,"Alive HP change must reach clients immediately");
  var reader=new DataInputStream(new ByteArrayInputStream(packets.get(0)));
  check(reader.readByte()==9&&reader.readInt()==(int)b.id&&reader.readInt()==50&&reader.readInt()==200&&reader.available()==0,"Exact HP/max HP packet and preserved ratio");
  BossAdminConfig.applyAtTick(b);check(packets.size()==1,"No resend for same revision");
  BossAdminConfig.stage(BossAdminConfigTest.db(4,List.of(BossAdminConfigTest.row(4,"alive",200)))).run();
  BossAdminConfig.applyAtTick(b);check(packets.size()==1,"No HP packet when HP unchanged");
  b.nPoint.hp=0;BossAdminConfig.stage(BossAdminConfigTest.db(5,List.of(BossAdminConfigTest.row(5,"alive",400)))).run();
  BossAdminConfig.applyAtTick(b);check(b.nPoint.hp==0&&packets.size()==1,"Dead boss stays dead and sends no HP update");
  b.nPoint.hp=50;b.zone=null;BossAdminConfig.applyAtTick(b);check(packets.size()==1,"Detached boss sends no HP update");
  System.out.println("BOSS_ADMIN_LIVE_WIRE PASS existing client packet,ratio,once,next,dead,detached");
 }
}
