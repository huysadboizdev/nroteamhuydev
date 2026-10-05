package nro.models.shop_ky_gui;

import java.util.*;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.*;
import static nro.models.services.PinkFixture.*;

public class DivineBoxTransactionTest {
    static void check(boolean b,String s){PinkFixture.check(b,s);}
    static Player owner(long id)throws Exception {
        var p=ConsignRegressionTest.owner(id);p.inventory.ruby=100_000;
        p.inventory.itemsBag.clear();for(int n=0;n<6;n++)p.inventory.itemsBag.add(new Item());return p;
    }
    static Item box(Player p,int quantity){var box=item(1775,quantity);p.inventory.itemsBag.set(0,box);return box;}
    public static void main(String[] args)throws Exception {
        install();
        replace(ItemService.class,"i",new ItemService(){
            @Override public nro.models.player_system.Template.ItemTemplate getTemplate(int id){
                var t=template(id);if(id>=555&&id<=567){t.isUpToUp=false;t.type=(byte)(id==561?4:id<=560?(id%2==1?0:1):(id%2==0?2:3));}return t;
            }
            @Override public nro.models.player_system.Template.ItemOptionTemplate getItemOptionTemplate(int id){var t=new nro.models.player_system.Template.ItemOptionTemplate();t.id=id;t.name="fixture #";return t;}
        });
        replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}});
        replace(ConsignShopManager.class,"instance",new ConsignShopManager(){@Override public void save(){}});
        var dao=new ConsignRegressionTest.MemoryDAO();var tx=new ConsignTransactions(dao,p->true);
        Player p=owner(901);Item box=box(p,1);
        check(tx.redeemDivineBox(p,box,1).success(),"set transaction success");
        check(p.inventory.ruby==100_000&&p.inventory.itemsBag.stream().filter(i->i.template!=null).count()==5,"5 items without charging");
        check(!tx.redeemDivineBox(p,box,1).success()&&dao.writes==1,"stale/repeated box cannot grant twice");
        p=owner(902);box=box(p,2);for(int n=2;n<6;n++)p.inventory.itemsBag.set(n,item(200,1));
        check(!tx.redeemDivineBox(p,box,0).success()&&box.quantity==2&&dao.writes==1,"stacked box + full bag unchanged");
        p=owner(903);box=box(p,1);dao.fail=true;
        check(!tx.redeemDivineBox(p,box,2).success()&&p.inventory.itemsBag.get(0)==box&&box.quantity==1,"SQL rollback keeps original box");dao.fail=false;
        p=owner(904);check(tx.purchaseDivineBox(p).success()&&p.inventory.ruby==50_000,"buy exactly 50000 ruby");
        check(p.inventory.gold==1000&&p.inventory.gem==100,"no gold/green gem debit");
        p=owner(905);p.inventory.ruby=49_999;check(!tx.purchaseDivineBox(p).success()&&p.inventory.ruby==49_999,"insufficient ruby");
        p=owner(906);for(int n=0;n<6;n++)p.inventory.itemsBag.set(n,item(200,1));
        check(!tx.purchaseDivineBox(p).success()&&p.inventory.ruby==100_000,"full purchase bag no debit");
        p=owner(907);box=box(p,1);dao.uncertain=true;
        check(!tx.redeemDivineBox(p,box,0).success()&&ConsignTransactions.hasPending(p)&&p.inventory.itemsBag.get(0)==box,"unknown commit stays gated");
        dao.outage=false;check(ConsignTransactions.reconcilePending(p)&&!ConsignTransactions.hasPending(p),"unknown commit recovery");
        check(p.inventory.itemsBag.stream().filter(i->i.template!=null).count()==5,"recovery publishes full set exactly once");
        check(ConsignTransactions.reconcilePending(p)&&p.inventory.itemsBag.stream().filter(i->i.template!=null).count()==5,"no replay grant");
        System.out.println("PASS DivineBox transactions: exact cost, three-set generation, capacity, rollback, stale/replay, unknown commit recovery");System.exit(0);
    }
}
