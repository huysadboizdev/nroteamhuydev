package nro.models.services;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import nro.models.boss.Broly.*;
import nro.models.boss.Boss_Manager.BrolyManager;
import nro.models.consts.BossStatus;
import nro.models.consts.ConstPlayer;
import nro.models.map.Zone;
import nro.models.player.*;

public final class BrolyLifecycleTest {
    static final List<String> events=new CopyOnWriteArrayList<>();
    static class Normal extends Broly {
        int creations;
        boolean failPreparation, failEntry;
        Super next;
        Normal() throws Exception { super(); }
        @Override public void attack() { }
        @Override protected void enterZone(Zone z,int x,int y) {
            if(failEntry)throw new IllegalStateException("injected rollback entry failure");
            zone=z;location.x=x;location.y=y;z.addPlayer(this);events.add("normal-add");
        }
        @Override protected void exitZone() {if(zone!=null)zone.removePlayer(this);zone=null;events.add("normal-remove");}
        @Override protected SuperBroly createSuperBroly(Zone z,int x,int y) throws Exception {
            creations++;
            if(failPreparation)throw new Exception("injected preparation failure");
            Super s=next!=null?next:superFixture(z);s.zone=z;s.location.x=x;s.location.y=y;return s;
        }
    }
    static class Super extends SuperBroly {
        boolean failEntry,failCompanion;
        int companions, cleared;
        Super() throws Exception {super(null,0,0);}
        @Override protected void enterZone(Zone z,int x,int y) {
            zone=z;location.x=x;location.y=y;z.addPlayer(this);events.add("super-add");
            if(failEntry)throw new IllegalStateException("injected partial Super admission failure");
        }
        @Override protected void exitZone(){if(zone!=null)zone.removePlayer(this);zone=null;events.add("super-remove");}
        @Override protected void createCompanion(){companions++;if(failCompanion)throw new IllegalStateException("injected pet preparation failure");}
        @Override protected void clearCompanion(){cleared++;}
        @Override protected void notifyJoinMap() { }
    }
    static class ConstructedSuper extends SuperBroly {
        ConstructedSuper(Zone zone)throws Exception {super(zone,110,75);}
        @Override public void initBase() {
            check(currentLevel==0,"Super preparation must select its first data level before initializing");
            check(!BrolyManager.gI().getBosses().contains(this),"constructor must not publish a partially prepared Super");
        }
    }
    static class RealEntrySuper extends ConstructedSuper {
        RealEntrySuper(Zone zone)throws Exception {super(zone);}
        @Override protected void createCompanion() { }
        @Override protected void notifyJoinMap() { }
        void restoreTo(Zone target) {zone=null;enterZone(target,110,75);}
    }
    static Normal normal(int hpMax) throws Exception {
        Normal n=PinkFixture.blank(Normal.class);n.isBoss=true;n.nPoint=new NPoint(n);n.nPoint.hp=100;n.nPoint.hpMax=hpMax;
        n.location=new Location();n.location.x=110;n.location.y=75;n.zone=PinkFixture.zone();n.zone.map.zones=List.of(n.zone,n.zone,n.zone);n.zone.addPlayer(n);n.bossStatus=BossStatus.ACTIVE;n.typePk=ConstPlayer.PK_ALL;
        return n;
    }
    static Super superFixture(Zone zone) throws Exception {
        Super s=PinkFixture.blank(Super.class);s.isBoss=true;s.zone=zone;s.nPoint=new NPoint(s);s.nPoint.hp=100;s.nPoint.hpMax=1_500_000;s.location=new Location();return s;
    }
    static void check(boolean result,String message){PinkFixture.check(result,message);}
    static void reset()throws Exception{PinkFixture.replace(BrolyManager.class,"instance",new BrolyManager());events.clear();}
    static void thresholdAndFailure()throws Exception {
        reset();Normal n=normal(1_500_001);n.failPreparation=true;n.active();
        check(n.creations==1,"living Broly above threshold must attempt transformation");
        check(n.zone!=null && n.zone.getBosses().contains(n),"preparation failure must keep original Broly");
        n.active();check(n.creations==1,"failed preparation must back off rather than retry every update");n.leaveMap();
        reset();n=normal(1_499_999);n.active();check(n.creations==0,"below threshold must not transform");n.leaveMap();
        reset();n=normal(1_500_000);Zone z=n.zone;n.active();
        check(n.creations==1 && n.zone==null && n.bossStatus==BossStatus.REST,"exact threshold ends normal lifetime");
        Super s=(Super)z.getBosses().get(0);
        check(s.location.x==110&&s.location.y==75,"transformation preserves coordinates");
        check(events.indexOf("normal-remove")<events.indexOf("super-add"),"remove normal must precede Super add");
        check(BrolyManager.gI().getBosses().contains(s),"completed Super published to manager");
        n.active();n.leaveMap();check(n.creations==1,"late normal callbacks must not recreate Super");
        check(BrolyManager.gI().zones().isReserved(z),"old normal leave must not release Super slot");
        n.zone=z;n.joinMap2();check(n.zone==null,"respawn must not reenter occupied Super zone");
        s.leaveMap();check(!BrolyManager.gI().zones().isReserved(z),"Super leave releases its slot");
        check(!BrolyManager.gI().getBosses().contains(s),"Super retirement removes manager entry");s.leaveMap();
        reset();n=normal(1_500_001);n.nPoint.hp=0;n.active();check(n.creations==0,"dead normal cannot transform");n.leaveMap();
        reset();n=normal(1_500_001);n.leaveMap();check(n.creations==0,"ordinary leave creates no Super");
    }
    static void admissionRollback()throws Exception {
        reset();Normal n=normal(1_500_000);Zone z=n.zone;Super s=superFixture(z);s.failEntry=true;n.next=s;
        n.active();check(n.zone==z && z.getBosses().size()==1 && z.getBosses().contains(n),"partial Super join restores one normal");
        check(s.zone==null&&!BrolyManager.gI().getBosses().contains(s),"failed Super cannot leak into zone or manager");
        check(s.cleared>0,"failed Super cleans companion");n.leaveMap();
        reset();n=normal(1_500_000);z=n.zone;s=superFixture(z);s.failCompanion=true;n.next=s;n.failEntry=true;
        n.active();check(n.zone==z&&z.getBosses().contains(n),"rollback packet failure still preserves server-side boss");
        n.failEntry=false;n.leaveMap();
        reset();z=PinkFixture.zone();s=superFixture(z);s.failEntry=true;
        try{s.joinMap();throw new AssertionError("admission should fail");}catch(IllegalStateException expected){}
        check(z.getBosses().isEmpty()&&!BrolyManager.gI().zones().isReserved(z),"independent failed spawn releases slot");
    }
    static void reservationsAndConcurrency()throws Exception {
        reset();BrolyZoneCoordinator c=BrolyManager.gI().zones();Zone z=PinkFixture.zone();Object hunt=new Object();
        check(c.tryReserveExternal(z,hunt),"Hunt reservation succeeds");Normal n=normal(500);
        check(c.reserve(z,n)==null,"pending Hunt reservation blocks Broly");c.releaseExternal(z,new Object());check(c.isReserved(z),"foreign Hunt cleanup cannot release ownership");c.releaseExternal(z,hunt);
        var first=c.reserve(z,n);Super s=superFixture(z);var second=c.transfer(first,s);c.release(first);
        check(c.owns(second),"stale normal lease cannot release Super generation");c.release(second);
        CountDownLatch start=new CountDownLatch(1);AtomicInteger wins=new AtomicInteger();List<Thread> workers=new ArrayList<>();
        for(int i=0;i<24;i++){final Object owner=new Object();Thread t=new Thread(()->{try{start.await();if(c.tryReserveExternal(z,owner))wins.incrementAndGet();}catch(Exception e){throw new RuntimeException(e);}});workers.add(t);t.start();}
        start.countDown();for(Thread t:workers)t.join();check(wins.get()==1,"concurrent zone admission must elect exactly one owner");
        reset();Normal race=normal(1_500_001);Zone rz=race.zone;CountDownLatch go=new CountDownLatch(1);workers.clear();
        for(int i=0;i<12;i++){Thread t=new Thread(()->{try{go.await();race.active();}catch(Exception e){throw new RuntimeException(e);}});workers.add(t);t.start();}
        go.countDown();for(Thread t:workers)t.join();check(race.creations==1&&rz.getBosses().size()==1,"racing transformation callbacks create exactly one Super");((Super)rz.getBosses().get(0)).leaveMap();
    }
    static void legacyAndRewardTimer()throws Exception {
        reset();Normal n=normal(500);Zone z=n.zone;Super s=superFixture(z);z.addPlayer(s);
        BrolyManager.gI().publish(n);BrolyManager.gI().publish(s);
        try {BrolyManager.class.getMethod("reconcileExistingZones").invoke(BrolyManager.gI());}
        catch(NoSuchMethodException missing){throw new AssertionError("legacy duplicate reconciliation is required");}
        check(z.getBosses().size()==1&&z.getBosses().contains(s)&&n.creations==0,"legacy duplicate cleanup prefers valid Super without another transformation");
        s.leaveMap();
        reset();s=superFixture(PinkFixture.zone());s.joinMap();
        AtomicInteger rewards=new AtomicInteger();PinkFixture.replace(PetService.class,"instance",new PetService(){
            @Override public void createNormalPet(Player player,byte... limits){rewards.incrementAndGet();}
        });
        Player winner=PinkFixture.player(42);s.reward(winner);s.reward(winner);check(rewards.get()==1,"Super grants the pet reward only once");
        var timer=SuperBroly.class.getDeclaredField("st");timer.setAccessible(true);timer.setLong(s,System.currentTimeMillis()-300_001);
        winner.zone=s.zone;s.zone.addPlayer(winner);s.autoLeaveMap();check(s.zone!=null,"players present refresh Super empty timer before expiry check");
        s.zone.removePlayer(winner);timer.setLong(s,System.currentTimeMillis()-300_001);s.autoLeaveMap();check(s.zone==null,"Super leaves after five empty minutes");
        reset();n=normal(500);n.zone.map.zones=List.of(n.zone,n.zone);n.zone.removePlayer(n);n.joinMap2();check(n.zone==null&&n.bossStatus==BossStatus.REST&&n.creations==0,"maps with fewer than three zones safely wait without Super creation");
        Broly constructed=new Broly();var rest=nro.models.boss.Boss.class.getDeclaredField("secondsRest");rest.setAccessible(true);check(rest.getInt(constructed)==600,"normal respawn remains 600 seconds");BrolyManager.gI().removeBoss(constructed);
    }
    static void realMapAdmission()throws Exception {
        PinkFixture.replace(nro.models.services_func.TransactionService.class,"i",PinkFixture.blank(nro.models.services_func.TransactionService.class));
        reset();Broly normal=new Broly();normal.currentLevel=0;normal.nPoint.hp=500;normal.nPoint.hpMax=500;
        Zone seed=PinkFixture.zone();Zone target=new Zone(seed.map,2,20);
        seed.map.zones=List.of(seed,new Zone(seed.map,1,20),target);normal.zone=seed;
        normal.joinMap2();
        check(normal.zone==target&&target.getBosses().contains(normal)&&normal.bossStatus==BossStatus.CHAT_S,
                "real ChangeMapService must admit normal Broly from detached spawn state");
        normal.leaveMap();check(target.getBosses().isEmpty(),"real map leave removes normal membership");BrolyManager.gI().removeBoss(normal);
        RealEntrySuper superBoss=new RealEntrySuper(target);superBoss.joinMap();
        check(superBoss.zone==target&&target.getBosses().contains(superBoss),"real ChangeMapService admits fully prepared Super");
        target.removePlayer(superBoss);superBoss.restoreTo(target);
        check(superBoss.zone==target&&target.getBosses().contains(superBoss),"real Super admission restores detached map membership");
        superBoss.leaveMap();
    }
    static void missingAttackerSkill()throws Exception {
        PinkFixture.replace(SkillService.class,"instance",new SkillService(){
            @Override public boolean useSkill(Player p,Player target,nro.models.mob.Mob mob,int status,nro.models.network.Message message){return false;}
        });
        reset();Broly normal=new Broly();normal.currentLevel=0;normal.zone=PinkFixture.zone();normal.zone.addPlayer(normal);
        normal.nPoint.hpMax=500;normal.nPoint.hp=100;normal.bossStatus=BossStatus.ACTIVE;
        for(int i=0;i<7;i++)normal.playerSkill.skills.add(new nro.models.skill.Skill());
        int taken=normal.injured(null,20,false,true);
        check(taken>0&&taken<=normal.nPoint.hpMax/100,"null attacker uses normal damage cap safely");
        Player attacker=PinkFixture.player(89);taken=normal.injured(attacker,20,false,false);
        check(taken>0&&taken<=normal.nPoint.hpMax/100,"missing attacker skill uses normal damage cap safely");normal.leaveMap();BrolyManager.gI().removeBoss(normal);
        reset();Super s=superFixture(PinkFixture.zone());s.nPoint.hp=100_000;s.playerSkill=new nro.models.skill.PlayerSkill(s);
        for(int i=0;i<7;i++)s.playerSkill.skills.add(new nro.models.skill.Skill());s.joinMap();
        taken=s.injured(null,30_000,false,true);check(taken==0,"Super now accepts only qualified melee/reflection/bomb; environmental source ignored safely");s.leaveMap();
    }
    public static void main(String[] args)throws Exception {
        PinkFixture.install();realMapAdmission();missingAttackerSkill();reset();new ConstructedSuper(PinkFixture.zone());thresholdAndFailure();admissionRollback();reservationsAndConcurrency();legacyAndRewardTimer();
        System.out.println("BrolyLifecycleTest PASS: threshold, death/leave, rollback, cleanup, ownership, concurrent transform/admission");
    }
}
