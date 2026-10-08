package nro.models.boss;
import java.lang.reflect.Method;
import java.util.*;
import nro.models.boss.Boss_mini.*;
import nro.models.map.*;
import nro.models.player.*;
import static nro.models.services.PinkFixture.*;
public final class MiniBossRewardTest {
 static final Class<?>[] TYPES={AnTrom.class,SoiHecQuyn.class,MatTroi.class,Odo.class};
 static Boss boss(Class<?> type)throws Exception {Boss b=(Boss)blank(type);b.zone=zone();b.location=blank(Location.class);b.location.x=239;b.location.y=200;return b;}
 static long capsules(Boss b){return b.zone.items.stream().filter(i->i.itemTemplate.id==722).count();}
 public static void main(String[] args)throws Exception {install();String mode=args[0];
  for(Class<?> type:TYPES){Boss b=boss(type);Player p=player(777);
   if(mode.equals("reward")){b.reward(p);long n=capsules(b);check(n>=1&&n<=5,"every mini rewards capsules even thief without stolen gold "+type);for(ItemMap i:b.zone.items)if(i.itemTemplate.id==722)check(i.quantity==1&&i.playerId==777&&kept(i)&&i.x>=0&&i.x<240,"exclusive capsule ownership and valid terrain");}
   else if(mode.equals("repeat")){b.reward(p);int n=b.zone.items.size(),points=p.event.getEventPoint();b.reward(p);check(b.zone.items.size()==n&&p.event.getEventPoint()==points,"no duplicate items or points "+type);Method reset=type.getDeclaredMethod("resetBase");reset.setAccessible(true);reset.invoke(b);b.reward(p);check(b.zone.items.size()>n,"new spawn may reward "+type);}
   else if(mode.equals("invalid")){b.reward(null);check(b.zone.items.isEmpty(),"invalid killer grants nothing");Zone valid=b.zone;b.zone=null;b.reward(p);b.zone=valid;b.reward(p);check(capsules(b)>=1,"invalid context does not consume claim");}
   else if(mode.equals("race")){Thread a=new Thread(()->b.reward(p)),c=new Thread(()->b.reward(p));a.start();c.start();a.join();c.join();check(capsules(b)>=1&&capsules(b)<=5,"concurrent rewards once");check(p.event.getEventPoint()<=5,"points only once");}
   else throw new AssertionError(mode);
  }
  System.out.println("MINI_REWARD PASS "+mode);System.exit(0);
 }
}
