package nro.models.shop_ky_gui;

import java.lang.reflect.*;
import java.util.*;
import java.sql.SQLException;
import java.util.concurrent.*;
import nro.models.database.ConsignDAO;
import nro.models.database.RubyGiftDAO;
import nro.models.item.Item;
import nro.models.network.MySession;
import nro.models.player.*;
import nro.models.services.*;
import static nro.models.services.PinkFixture.*;

public final class ConsignRegressionTest {
    static class MemoryDAO extends ConsignDAO {
        int writes,next=1000;boolean fail,uncertain,outage;
        Map<String,Integer> receipts=new HashMap<>();
        @Override public int transact(String token,String action,ConsignItem before,ConsignItem after,RubyGiftDAO.Snapshot player)throws SQLException {
            if(fail)throw new SQLException("test rollback");
            int id=before==null?next++:before.id;receipts.put(token,id);writes++;
            if(uncertain){outage=true;throw new CommitUncertain(new SQLException("lost acknowledgement"));}
            return id;
        }
        @Override public Integer reconcile(String token,String action,RubyGiftDAO.Snapshot p)throws SQLException {
            if(outage)throw new SQLException("test outage");return receipts.get(token);
        }
    }
    public static class Connected extends MySession {
        public Connected() throws Exception { super(null); }
        @Override public boolean isConnected(){return true;}
    }
    static void set(Class<?> type,String name,Object value)throws Exception {
        Field f=type.getDeclaredField(name);f.setAccessible(true);f.set(null,value);
    }
    static Player owner(long id)throws Exception {
        Player p=player(id);p.nPoint=new NPoint(p);p.nPoint.power=20_000_000_000L;
        p.session=blank(Connected.class);p.session.userId=(int)id;p.session.player=p;p.session.actived=true;
        p.inventory.gold=1000;p.inventory.gem=100;p.inventory.ruby=100;
        for(int i=0;i<10;i++)p.inventory.itemsBag.add(new Item());return p;
    }
    static ConsignItem listing(int id,int seller,int price)throws Exception {
        ConsignItem it=new ConsignItem(id,(short)555,seller,(byte)0,-1,price,1,(byte)0,new ArrayList<>(),false);
        it.options.add(new Item.ItemOption(87,1));
        try{ConsignItem.class.getField("currency").set(it,"ruby");}catch(NoSuchFieldException old){}
        return it;
    }
    static int requestId(ConsignShopService service,Player p,int actual)throws Exception {
        for(ConsignItem it:ConsignShopManager.gI().listItem)if(it.id==actual) {
            Method wire=ConsignShopService.class.getDeclaredMethod("wireId",Player.class,ConsignItem.class);
            wire.setAccessible(true);return (int)wire.invoke(service,p,it);
        }return 0;
    }
    public static void main(String[] args)throws Exception {
        install();
        set(ItemService.class,"i",new ItemService(){
            @Override public nro.models.player_system.Template.ItemTemplate getTemplate(int id){var t=template(id);if(id==555)t.type=0;return t;}
            @Override public nro.models.player_system.Template.ItemOptionTemplate getItemOptionTemplate(int id){var t=new nro.models.player_system.Template.ItemOptionTemplate();t.id=id;t.name="fixture #";return t;}
        });
        set(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}});
        ConsignShopManager manager=new ConsignShopManager(){@Override public void save(){}};
        set(ConsignShopManager.class,"instance",manager);
        ConsignShopService service=new ConsignShopService(){@Override public void openShopKyGui(Player p){}};
        set(ConsignShopService.class,"instance",service);
        MemoryDAO dao=new MemoryDAO();service.transactions=new ConsignTransactions(dao,p->true);
        Player buyer=owner(100),seller=owner(200);
        if(args[0].equals("pagination")) {
            for(int i=1;i<=40;i++)manager.listItem.add(listing(i,200,20));
            Method page=ConsignShopService.class.getDeclaredMethod("getItemKyGui2",Player.class,byte.class,byte.class,byte.class);
            page.setAccessible(true);
            check(((List<?>)page.invoke(service,buyer,(byte)0,(byte)0,(byte)20)).size()==20,"page must contain 20 items, not 21");
            Method modern=ConsignShopService.class.getDeclaredMethod("pageItems",Player.class,byte.class,int.class,int.class);
            modern.setAccessible(true);
            for(int i=41;i<=160;i++)manager.listItem.add(listing(i,200,20));
            check(((List<?>)modern.invoke(service,buyer,(byte)0,140,160)).size()==20,"page7 uses int offsets");
        } else if(args[0].equals("buy-requirements")) {
            manager.listItem.add(listing(1,200,20));
            ConsignTransactions tx=service.transactions;
            buyer.nPoint.power=16_999_999_999L;
            check(!tx.buy(buyer,1).success()&&dao.writes==0,"below17b denied");
            buyer.nPoint.power=17_000_000_000L;buyer.session.actived=false;
            check(tx.buy(buyer,1).message().contains("chưa kích hoạt")&&dao.writes==0,"activation error explicit");
            buyer.session.actived=true;buyer.inventory.ruby=19;
            check(!tx.buy(buyer,1).success()&&dao.writes==0,"ruby below price denied");
            buyer.inventory.ruby=20;
            check(!tx.buy(seller,1).success()&&dao.writes==0,"self purchase denied");
            check(tx.buy(buyer,1).success()&&buyer.inventory.ruby==0&&dao.writes==1,"17b boundary with exact ruby price succeeds");
            check(!tx.buy(buyer,1).success()&&dao.writes==1,"sold listing denied without charge");
        } else if(args[0].equals("currency")) {
            manager.listItem.add(listing(1,200,20));service.buyItem(buyer,requestId(service,buyer,1));
            check(buyer.inventory.ruby==80&&buyer.inventory.gem==100&&buyer.inventory.gold==1000,"purchase must debit ruby only");
            service.claimOrDel(seller,(byte)2,requestId(service,seller,1));
            check(seller.inventory.ruby==118&&seller.inventory.gem==100,"claim must credit ruby only");
        } else if(args[0].equals("full-bag")) {
            for(int i=0;i<buyer.inventory.itemsBag.size();i++)buyer.inventory.itemsBag.set(i,item(555,1));
            ConsignItem it=listing(1,200,20);manager.listItem.add(it);service.buyItem(buyer,requestId(service,buyer,1));
            check(!it.isBuy&&buyer.inventory.ruby==100&&buyer.inventory.gem==100,"full bag must not charge or sell");
        } else if(args[0].equals("post")) {
            Item offered=item(555,1);offered.template.type=0;offered.itemOptions.add(new Item.ItemOption(35,1));
            offered.itemOptions.add(new Item.ItemOption(103,4));offered.itemOptions.add(new Item.ItemOption(86,1));
            long created=offered.createTime;buyer.inventory.itemsBag.set(0,offered);
            service.KiGui(buyer,0,20,(byte)0,1);
            check(dao.writes==0&&buyer.inventory.ruby==100,"reject gold request before charging");
            service.KiGui(buyer,0,20,(byte)1,1);
            check(dao.writes==1&&buyer.inventory.ruby==99&&!buyer.inventory.itemsBag.get(0).isNotNullItem(),"post charges ruby fee once");
            ConsignItem posted=manager.listItem.get(0);
            check(posted.createTime==created&&posted.currency.equals("ruby")&&posted.gemSell==20&&posted.goldSell==-1,"timestamp and ruby price saved");
            check(posted.options.stream().noneMatch(o->o.optionTemplate.id==86)&&posted.options.stream().anyMatch(o->o.optionTemplate.id==30)&&posted.options.stream().anyMatch(o->o.optionTemplate.id==87),"sealed no-trade and ruby-only flags");
            service.KiGui(buyer,0,20,(byte)1,1);check(dao.writes==1,"repeated post cannot sell consumed slot");
        } else if(args[0].equals("legacy")) {
            ConsignItem it=listing(1,200,20);it.currency="legacy";it.goldSell=20;it.gemSell=-1;manager.listItem.add(it);
            service.buyItem(buyer,requestId(service,buyer,1));check(dao.writes==0&&buyer.inventory.ruby==100&&buyer.inventory.gold==1000,"legacy price never reinterpreted");
            service.claimOrDel(seller,(byte)1,requestId(service,seller,1));check(dao.writes==1&&manager.listItem.isEmpty()&&seller.inventory.ruby==100,"owner can recover legacy item");
        } else if(args[0].equals("rollback")) {
            dao.fail=true;ConsignItem it=listing(1,200,20);manager.listItem.add(it);
            service.buyItem(buyer,requestId(service,buyer,1));check(buyer.inventory.ruby==100&&!it.isBuy&&buyer.inventory.itemsBag.stream().noneMatch(Item::isNotNullItem),"failed DB commit leaves RAM unchanged");
            check(!ConsignTransactions.hasPending(buyer),"definite rollback releases gate");
        } else if(args[0].equals("uncertain")) {
            dao.uncertain=true;manager.listItem.add(listing(1,200,20));service.buyItem(buyer,requestId(service,buyer,1));
            check(ConsignTransactions.hasPending(buyer)&&ConsignTransactions.hasPendingAccount(100)&&buyer.inventory.ruby==100,"uncertain commit keeps actor reserved, no premature RAM changes");
            service.claimOrDel(seller,(byte)1,requestId(service,seller,1));check(dao.writes==1,"reserved listing cannot cancel");
            check(!ConsignTransactions.reconcilePending(buyer),"unavailable receipt retains gate");
            dao.outage=false;check(ConsignTransactions.reconcilePending(buyer)&&buyer.inventory.ruby==80&&manager.listItem.get(0).isBuy,"receipt publishes once");
            check(ConsignTransactions.reconcilePending(buyer)&&buyer.inventory.ruby==80,"recovery is idempotent");
        } else if(args[0].equals("concurrency")) {
            Player other=owner(300);manager.listItem.add(listing(1,200,20));
            ExecutorService executor=Executors.newFixedThreadPool(2);CountDownLatch start=new CountDownLatch(1);
            Future<?> a=executor.submit(()->{try{start.await();service.buyItem(buyer,requestId(service,buyer,1));}catch(Exception e){throw new RuntimeException(e);}});
            Future<?> b=executor.submit(()->{try{start.await();service.buyItem(other,requestId(service,other,1));}catch(Exception e){throw new RuntimeException(e);}});
            start.countDown();a.get(5,TimeUnit.SECONDS);b.get(5,TimeUnit.SECONDS);executor.shutdownNow();
            check(dao.writes==1&&buyer.inventory.ruby+other.inventory.ruby==180,"only one concurrent buyer charged");
            service.claimOrDel(seller,(byte)2,requestId(service,seller,1));service.claimOrDel(seller,(byte)2,requestId(service,seller,1));
            check(seller.inventory.ruby==118&&dao.writes==2,"claim paid once");
        } else if(args[0].equals("logout") || args[0].equals("logout-outer")) {
            boolean[] saved={false};int[] attempts={0};
            service.transactions=new ConsignTransactions(dao,p->true,p->{
                attempts[0]++;check(ConsignTransactions.isFinalizingLogout(p)&&ConsignTransactions.hasPendingAccount(100),"account reserved through full logout save");return saved[0];
            });
            dao.uncertain=true;manager.listItem.add(listing(1,200,20));
            service.buyItem(buyer,requestId(service,buyer,1));buyer.beforeDispose=true;dao.outage=false;
            check(!ConsignTransactions.reconcilePending(buyer)&&buyer.inventory.ruby==80,"failed full save retains pending logout with committed inventory");
            check(ConsignTransactions.hasPendingAccount(100)&&ConsignTransactions.hasPendingId(100),"replacement login blocked until full save succeeds");
            saved[0]=true;
            if(args[0].equals("logout"))check(ConsignTransactions.reconcilePending(buyer),"background recovery finishes before ordinary disconnect saver");
            nro.models.database.PlayerDAO.updatePlayer(buyer);
            nro.models.database.PlayerDAO.updatePlayer(buyer);
            check(!ConsignTransactions.hasPending(buyer)&&buyer.inventory.ruby==80&&attempts[0]==2,"outer full-save call returns after checked logout save, without replay or second stale save");
            check(buyer.consignLogoutSaved,"persistent detached-session marker precedes gate release");
            check(!ConsignTransactions.hasPendingAccount(100),"successful full save releases login gate");
        } else if(args[0].equals("wire-ids")) {
            ConsignItem it=listing(40000,200,20);manager.listItem.add(it);
            service.buyItem(buyer,40000);check(dao.writes==0,"raw database id never accepted from client");
            int wire=requestId(service,buyer,40000);check(wire>0&&wire<=32767,"large database id uses bounded alias");
            it.gemSell=30;service.buyItem(buyer,wire);check(dao.writes==0,"price changed since viewed cannot debit changed price");
            wire=requestId(service,buyer,40000);service.buyItem(buyer,wire);
            check(dao.writes==1&&buyer.inventory.ruby==70,"refreshed alias buys full database id");
        } else if(args[0].equals("seal-trade")) {
            var trade=blank(nro.models.services_func.Trade.class);
            Method blocked=nro.models.services_func.Trade.class.getDeclaredMethod("isItemCannotTran",Item.class);
            blocked.setAccessible(true);
            for(int marker:new int[]{34,35,36}) {
                Item gear=item(555,1);gear.template.type=0;gear.itemOptions.add(new Item.ItemOption(marker,1));
                check((boolean)blocked.invoke(trade,gear),"seal itself blocks direct trade without flag30");
                nro.models.item.SealedEquipment.normalize(gear);
                check(ConsignTransactions.eligible(gear),"no-trade seal remains ruby consignable");
            }
            Item locked=item(555,1);locked.itemOptions.add(new Item.ItemOption(30,1));locked.itemOptions.add(new Item.ItemOption(87,1));
            check(!ConsignTransactions.eligible(locked),"unrelated locked item still cannot consign");
        } else if(args[0].equals("cancel-full")) {
            manager.listItem.add(listing(1,200,20));
            for(int i=0;i<seller.inventory.itemsBag.size();i++)seller.inventory.itemsBag.set(i,item(555,1));
            service.claimOrDel(seller,(byte)1,requestId(service,seller,1));
            check(dao.writes==0&&manager.listItem.size()==1,"full bag cancellation retains listing");
        } else if(args[0].equals("top-overflow")) {
            manager.listItem.add(listing(1,200,20));
            check(!service.transactions.top(buyer,1).success()&&dao.writes==0,"foreign owner cannot pay/top");
            check(service.transactions.top(seller,1).success()&&seller.inventory.ruby==95,"top charged five ruby");
            check(!service.transactions.top(seller,1).success()&&seller.inventory.ruby==95,"top retry no fee");
            manager.listItem.get(0).isBuy=true;seller.inventory.ruby=Integer.MAX_VALUE;
            service.claimOrDel(seller,(byte)2,requestId(service,seller,1));check(dao.writes==1&&manager.listItem.size()==1,"overflow claim preserved");
        } else throw new IllegalArgumentException(args[0]);
        System.out.println("PASS ConsignRegression "+args[0]);System.exit(0);
    }
}
