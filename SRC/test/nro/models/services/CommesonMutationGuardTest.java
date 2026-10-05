package nro.models.services;

import java.lang.reflect.*;
import java.sql.SQLException;
import java.util.Map;
import nro.models.commeson.CommesonAttempt;
import nro.models.database.RubyGiftDAO;
import nro.models.item.Item;
import nro.models.network.MySession;
import nro.models.player.*;
import nro.models.services_func.Trade;
import static nro.models.services.PinkFixture.*;

/** Cross-player operations must respect an uncertain Commeson inventory receipt. */
public final class CommesonMutationGuardTest {
    @SuppressWarnings("unchecked")
    static Map<Long,Object> matches() throws Exception {
        Field f=CommesonChallengeService.class.getDeclaredField("matches");
        f.setAccessible(true);return (Map<Long,Object>)f.get(CommesonChallengeService.gI());
    }
    static void quarantine(Player p) throws Exception {
        Class<?> type=Class.forName("nro.models.services.CommesonChallengeService$Match");
        Constructor<?> ctor=type.getDeclaredConstructor(CommesonAttempt.class,Player.class);
        ctor.setAccessible(true);
        Object match=ctor.newInstance(new CommesonAttempt("guard-"+p.id,p.id,System.currentTimeMillis()),p);
        Field f=type.getDeclaredField("quarantine");f.setAccessible(true);f.setBoolean(match,true);
        matches().put(p.id,match);
    }
    static Player connected(long id) throws Exception {
        Player p=player(id);p.idMark=new IDMark();p.session=blank(MySession.class);
        p.session.userId=(int)id;p.session.player=p;return p;
    }
    static void trade() throws Exception {
        Player a=connected(99001),b=connected(99002);
        a.inventory.itemsBag.add(item(638,1));b.inventory.itemsBag.add(item(17,1));
        Trade t=new Trade(a,b);
        t.acceptTrade(); // Owner accepts before SQL acknowledgement becomes uncertain.
        Object ownerBag=a.inventory.itemsBag,otherBag=b.inventory.itemsBag;
        quarantine(a);
        try {
            Throwable failure=null;
            try {t.acceptTrade();} catch(Throwable e){failure=e;}
            // The other participant confirms through its own live session.
            check(a.inventory.itemsBag==ownerBag && b.inventory.itemsBag==otherBag,
                    "counterparty confirmation must not replace either inventory while owner is quarantined");
            if(failure!=null)throw new AssertionError("quarantine denial must finish before trade side effects",failure);
        } finally {matches().remove(a.id);t.dispose();}
        quarantine(a);
        try {
            try {new Trade(b,a);throw new AssertionError("quarantined target must reject a new trade");}
            catch(IllegalStateException expected){}
        } finally {matches().remove(a.id);}
    }
    static void gift() throws Exception {
        Player a=connected(99003),b=connected(99004);
        a.inventory.ruby=100;b.inventory.ruby=10;
        Item ticket=item(2000,2);a.inventory.itemsBag.add(ticket);
        RubyGiftService service=new RubyGiftService(new RubyGiftDAO(()->{
            throw new SQLException("gift SQL must never be reached for quarantined participant");
        }),p->true);
        check(service.prepare(a,b,10).success(),"proposal valid before quarantine");
        quarantine(b);
        try {
            check(!service.confirm(a).success(),"confirmation must reject newly quarantined target");
            check(!service.prepare(a,b,10).success(),"proposal must reject quarantined target");
            check(a.inventory.ruby==100 && b.inventory.ruby==10 && ticket.quantity==2,
                    "rejected target leaves both balances and sender ticket unchanged");
        } finally {matches().remove(b.id);}
    }
    static void inventory() throws Exception {
        Player p=connected(99005);Item existing=item(17,3);p.inventory.itemsBag.add(existing);
        InventoryService actual=new InventoryService();quarantine(p);
        try {
            check(!actual.addItemBag(p,item(638,1)),"indirect bag grant must reject quarantine");
            actual.subQuantityItemsBag(p,existing,1);
            check(existing.quantity==3 && p.inventory.itemsBag.size()==1,
                    "indirect consumption must preserve the recovery snapshot");
        } finally {matches().remove(p.id);}
    }
    public static void main(String[] args) {
        try {
            install();trade();gift();inventory();
            System.out.println("PASS CommesonMutationGuard accepted trade, quarantined gift target, indirect bag grant/consume");
            System.exit(0);
        } catch(Throwable e){e.printStackTrace();System.exit(1);}
    }
}
