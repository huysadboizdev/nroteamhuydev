package nro.models.services;

import java.lang.reflect.*;
import java.sql.*;
import java.util.*;
import nro.models.commeson.CommesonAttempt;
import nro.models.commeson.CommesonAttempt.State;
import nro.models.database.CommesonDAO;
import nro.models.map.Zone;
import nro.models.map.ItemMap;
import nro.models.player.Player;
import nro.models.player.Inventory;

/** Exercises the actual service against the isolated SQL database owned by CommesonDbTest. */
public final class CommesonServiceTest {
    static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    static Object bare(Class<?> type)throws Exception {
        Class<?> c=Class.forName("sun.misc.Unsafe");Field f=c.getDeclaredField("theUnsafe");f.setAccessible(true);
        return c.getMethod("allocateInstance",Class.class).invoke(f.get(null),type);
    }
    static Player player(long id,int map)throws Exception {
        Player p=new Player("spec");p.id=id;p.inventory=new Inventory();p.effectSkill=new nro.models.player.EffectSkill(p);p.location=new nro.models.player.Location();
        nro.models.map.Map m=(nro.models.map.Map)bare(nro.models.map.Map.class);m.mapId=map;
        p.zone=new Zone(m,0,20);return p;
    }
    @SuppressWarnings("unchecked") static java.util.Map<Long,Object> matches(CommesonChallengeService service)throws Exception {
        Field f=CommesonChallengeService.class.getDeclaredField("matches");f.setAccessible(true);return (java.util.Map<Long,Object>)f.get(service);
    }
    static CommesonAttempt state(CommesonChallengeService service,long id)throws Exception {
        Object m=matches(service).get(id);if(m==null)return null;
        Field f=m.getClass().getDeclaredField("attempt");f.setAccessible(true);return (CommesonAttempt)f.get(m);
    }
    static void run(CommesonDAO dao)throws Exception {
        try(Connection c=CommesonDbTest.open();Statement s=c.createStatement()){s.execute("INSERT INTO player(id,items_bag) VALUES(6,'before'),(7,'before'),(8,'before'),(9,'before'),(10,'before'),(11,'before'),(12,'before'),(13,'before')");}
        long now=System.currentTimeMillis();
        CommesonAttempt a=CommesonDbTest.attempt(6,now);check(dao.claim(a,0),"restart fight claim");
        CommesonChallengeService service=new CommesonChallengeService(dao);Player p=player(6,140);
        service.onLogin(p);check(state(service,6).state==State.RETURN,"restart fight becomes return without reward replay");
        check(dao.pending(6).state==State.RETURN,"restart cancellation durable");
        Player reconnect=player(6,24);service.onLogin(reconnect);
        check(dao.pending(6)==null && state(service,6)==null,"disposed old owner cleanup and outside-zone reconnect completes");
        CommesonAttempt picked=CommesonDbTest.attempt(7,now);check(dao.claim(picked,0),"picked restart claim");picked.state=State.PICKED;picked.deadline=now+60_000;dao.transition(picked,State.FIGHT);
        Player p7=player(7,140);service.onLogin(p7);
        check(state(service,7).state==State.PICKED && state(service,7).deadline==picked.deadline,"reconnect preserves original pickup deadline");
        p7.beforeDispose=true;service.onMapExit(p7,p7.zone);
        check(state(service,7)==null && dao.pending(7).state==State.PICKED && dao.pending(7).deadline==picked.deadline,"disconnect does not reset picked timer");
        service.onLogin(player(7,140));check(state(service,7).deadline==picked.deadline,"second reconnect cannot extend picked wait");
        CommesonAttempt fight=CommesonDbTest.attempt(8,now);check(dao.claim(fight,0),"capsule claim");
        Player p8=player(8,140);Class<?> mt=Class.forName("nro.models.services.CommesonChallengeService$Match");Constructor<?> ctor=mt.getDeclaredConstructor(CommesonAttempt.class,Player.class);ctor.setAccessible(true);
        matches(service).put(8L,ctor.newInstance(fight,p8));Zone old=p8.zone;
        service.onMapExit(p8,old);check(p8.zone==old,"capsule cancellation must not start a second transport");
        p8.zone=player(800,24).zone;service.update(p8);check(dao.pending(8)==null && state(service,8)==null,"successful capsule movement completes attempt");
        CommesonAttempt won=CommesonDbTest.attempt(9,now);check(dao.claim(won,0),"bag full claim");won.state=State.WON;won.deadline=now+60_000;dao.transition(won,State.FIGHT);
        Player p9=player(9,140);Object match=ctor.newInstance(won,p9);matches(service).put(9L,match);
        ItemMap bottle=(ItemMap)bare(ItemMap.class);bottle.commesonMatchId=won.matchId;bottle.zone=p9.zone;
        Field bf=mt.getDeclaredField("bottle");bf.setAccessible(true);bf.set(match,bottle);
        long deadline=won.deadline;check(service.beforePickup(p9,bottle),"actual service reserves pickup");
        check(!service.afterPickup(p9,bottle,false),"full bag rejected");
        check(won.state==State.WON && dao.pending(9).state==State.WON && won.deadline==deadline,"full bag keeps original reward and countdown");
        check(!service.beforePickup(player(90,140),bottle),"another player cannot claim bottle");
        won.deadline=System.currentTimeMillis()-1;check(!service.beforePickup(p9,bottle),"expired bottle cannot be picked");
        System.out.println("PASS service restart/reconnect, preserved pickup deadline, capsule cancellation, bag-full retry, owner restriction, expired pickup");
        for(long id:new long[]{10,11}) {
            CommesonDAO rewardDao=id==10?dao:new CommesonDAO(()->CommesonDbTest.fault(false));
            CommesonChallengeService rewards=new CommesonChallengeService(rewardDao);
            CommesonAttempt reward=CommesonDbTest.attempt(id,now);check(dao.claim(reward,0),"actual reward claim");reward.state=State.WON;reward.deadline=now+60_000;dao.transition(reward,State.FIGHT);
            Player owner=player(id,140);Object rm=ctor.newInstance(reward,owner);matches(rewards).put(id,rm);
            ItemMap drop=(ItemMap)bare(ItemMap.class);drop.commesonMatchId=reward.matchId;drop.zone=owner.zone;bf.set(rm,drop);
            check(rewards.beforePickup(owner,drop),"actual reward reserve");
            nro.models.item.Item item=new nro.models.item.Item();item.template=(nro.models.player_system.Template.ItemTemplate)bare(nro.models.player_system.Template.ItemTemplate.class);item.template.id=638;item.quantity=1;owner.inventory.itemsBag.add(item);
            long before=System.currentTimeMillis();boolean accepted=rewards.afterPickup(owner,drop,true);long after=System.currentTimeMillis();
            if(id==10){
                check(accepted && reward.state==State.PICKED,"actual successful service pickup");
                check(reward.deadline>=before+60_000 && reward.deadline<=after+60_000,"successful pickup starts a fresh sixty seconds");
                check(dao.pending(id).deadline==reward.deadline && CommesonDbTest.bag(id).contains("638"),"actual bottle and countdown durable together");
                check(!rewards.beforePickup(owner,drop),"successful pickup cannot duplicate");
            }else{
                check(!accepted && reward.state==State.WON && owner.inventory.itemsBag.isEmpty(),"failed pickup transaction restores full bag and original reward");
                check(dao.pending(id).state==State.WON && "before".equals(CommesonDbTest.bag(id)),"failed pickup neither consumes reward nor writes inventory");
            }
        }
        System.out.println("PASS actual successful pickup durable fresh timer and SQL-failed pickup restores reward + bag");
        while(nro.models.server.Manager.ITEM_TEMPLATES.size()<=638)nro.models.server.Manager.ITEM_TEMPLATES.add(null);
        nro.models.player_system.Template.ItemTemplate bottleTemplate=(nro.models.player_system.Template.ItemTemplate)bare(nro.models.player_system.Template.ItemTemplate.class);bottleTemplate.id=638;
        nro.models.server.Manager.ITEM_TEMPLATES.set(638,bottleTemplate);
        while(nro.models.server.Manager.ITEM_OPTION_TEMPLATES.size()<=93)nro.models.server.Manager.ITEM_OPTION_TEMPLATES.add(null);
        for(int id:new int[]{30,93}){nro.models.player_system.Template.ItemOptionTemplate option=(nro.models.player_system.Template.ItemOptionTemplate)bare(nro.models.player_system.Template.ItemOptionTemplate.class);option.id=id;nro.models.server.Manager.ITEM_OPTION_TEMPLATES.set(id,option);}
        for(long id:new long[]{12,13}) {
            CommesonAttempt unpicked=CommesonDbTest.attempt(id,now);check(dao.claim(unpicked,0),"unpicked reconnect claim");unpicked.state=id==12?State.WON:State.PICKING;unpicked.deadline=System.currentTimeMillis()+15_000;dao.transition(unpicked,State.FIGHT);
            Player owner=player(id,140);service.onLogin(owner);Object recovered=matches(service).get(id);ItemMap recoveredBottle=(ItemMap)bf.get(recovered);
            check(state(service,id).state==State.WON && state(service,id).deadline==unpicked.deadline,"ungranted reconnect preserves original remaining win countdown");
            check(recoveredBottle!=null && recoveredBottle.quantity==1 && recoveredBottle.itemTemplate.id==638 && recoveredBottle.playerId==id && recoveredBottle.keepOwnerUntilDespawn && recoveredBottle.commesonMatchId.equals(unpicked.matchId),"ungranted reconnect restores exactly one owner bottle");
            check(recoveredBottle.createTime+60_000==unpicked.deadline && owner.zone.items.size()==1,"restored bottle autonomous expiry uses original deadline");
            service.update(owner);check(owner.zone.items.size()==1,"repeated tick cannot redrop a bottle");
            check(dao.pending(id).state==State.WON,"PICKING without committed grant safely restores WON");
            owner.beforeDispose=true;service.onMapExit(owner,owner.zone);
            check(dao.pending(id).state==State.WON && dao.pending(id).deadline==unpicked.deadline,"clean disconnect preserves unpicked phase and original deadline");
            Player second=player(id,140);service.onLogin(second);
            check(state(service,id).state==State.WON && state(service,id).deadline==unpicked.deadline && second.zone.items.size()==1,"normal reconnect restores one remaining bottle");
        }
        check(bf.get(matches(service).get(7L))==null,"committed PICKED reconnect never redrops bottle");
        System.out.println("PASS WON/PICKING reconnect restores one owner bottle with remaining time; PICKED never redrops");
        try(Connection c=CommesonDbTest.open();Statement s=c.createStatement()){s.execute("INSERT INTO player(id,items_bag) VALUES(14,'before'),(15,'before')");}
        CommesonAttempt reservation=CommesonDbTest.attempt(14,now);reservation.state=State.RESERVED;check(dao.claim(reservation,0),"durable prestart reservation");
        CommesonDAO deleteFails=new CommesonDAO(()->{
            Connection c=CommesonDbTest.open();
            return (Connection)Proxy.newProxyInstance(Connection.class.getClassLoader(),new Class<?>[]{Connection.class},(proxy,method,args)->{
                if(method.getName().equals("prepareStatement") && ((String)args[0]).startsWith("DELETE FROM commeson_attempt"))throw new SQLException("constructor failed then cleanup SQL unavailable");
                try{return method.invoke(c,args);}catch(InvocationTargetException e){throw e.getCause();}
            });
        });
        try{deleteFails.releasePreStart(reservation);throw new AssertionError("cleanup failure injected");}catch(SQLException expected){}
        check(dao.pending(14).state==State.RESERVED,"failed cleanup remains RESERVED, never FIGHT");
        new CommesonChallengeService(dao).onLogin(player(14,140));
        check(dao.pending(14)==null && dao.claim(CommesonDbTest.attempt(14,now),0),"restart releases prestart reservation and same day can retry");
        CommesonAttempt started=CommesonDbTest.attempt(15,now);check(dao.claim(started,0),"started daily fight");dao.releasePreStart(started);
        check(dao.pending(15).state==State.FIGHT && !dao.claim(CommesonDbTest.attempt(15,now),0),"prestart cleanup never deletes an actual fight");
        System.out.println("PASS RESERVED constructor/cleanup failure restart retry; actual FIGHT cannot be released");
    }
}
