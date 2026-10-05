package nro.models.shop_ky_gui;

import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.services.*;
import static nro.models.services.PinkFixture.*;

public final class RubyMaterialTransactionTest {
    public static void main(String[] args)throws Exception {
        install();
        replace(ItemService.class,"i",new ItemService(){
            @Override public nro.models.player_system.Template.ItemTemplate getTemplate(int id){var t=template(id);t.type=27;t.isUpToUp=true;return t;}
            @Override public nro.models.player_system.Template.ItemOptionTemplate getItemOptionTemplate(int id){var t=new nro.models.player_system.Template.ItemOptionTemplate();t.id=id;t.name="fixture #";return t;}
        });
        replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}});
        replace(ConsignShopManager.class,"instance",new ConsignShopManager(){@Override public void save(){}});
        var dao=new ConsignRegressionTest.MemoryDAO();var tx=new ConsignTransactions(dao,p->true);
        for(int id:new int[]{933,934,1820}){
            Player p=DivineBoxTransactionTest.owner(2000+id);
            check(tx.purchaseRubyMaterial(p,id).success(),"material buy "+id);
            Item first=p.inventory.itemsBag.get(0);
            check(first.quantity==1&&first.itemOptions.stream().anyMatch(o->o.optionTemplate.id==31&&o.param==1000),"upgrade count uses option31");
            check(tx.purchaseRubyMaterial(p,id).success(),"second buy");
            check(p.inventory.ruby==99_800&&p.inventory.itemsBag.get(0).quantity==1&&p.inventory.itemsBag.get(0).itemOptions.stream().anyMatch(o->o.optionTemplate.id==31&&o.param==2000),"merge option31 without quantity doubling");
            check(p.inventory.gold==1000&&p.inventory.gem==100,"ruby only");
        }
        Player p=DivineBoxTransactionTest.owner(4000);
        check(tx.purchaseRubyMaterial(p,935).success()&&p.inventory.ruby==99_990&&p.inventory.itemsBag.get(0).quantity==1,"one blue stone costs10");
        check(!tx.purchaseRubyMaterial(p,1775).success(),"closed material whitelist");
        p=DivineBoxTransactionTest.owner(4001);p.inventory.ruby=99;
        check(!tx.purchaseRubyMaterial(p,934).success()&&p.inventory.ruby==99,"insufficient funds");
        p=DivineBoxTransactionTest.owner(4002);for(int n=0;n<6;n++)p.inventory.itemsBag.set(n,item(200+n,1));
        check(!tx.purchaseRubyMaterial(p,933).success()&&p.inventory.ruby==100_000,"full bag no debit");
        p=DivineBoxTransactionTest.owner(4003);dao.fail=true;
        check(!tx.purchaseRubyMaterial(p,1820).success()&&p.inventory.ruby==100_000&&p.inventory.itemsBag.stream().noneMatch(Item::isNotNullItem),"DB rollback");dao.fail=false;
        p=DivineBoxTransactionTest.owner(4004);dao.uncertain=true;
        check(!tx.purchaseRubyMaterial(p,934).success()&&ConsignTransactions.hasPending(p)&&p.inventory.ruby==100_000,"unknown commit held");
        dao.outage=false;check(ConsignTransactions.reconcilePending(p)&&p.inventory.ruby==99_900,"receipt recovery");
        check(ConsignTransactions.reconcilePending(p)&&p.inventory.ruby==99_900,"replay no debit");
        System.out.println("PASS RubyMaterial: upgrade counts, stacking, exact ruby prices, whitelist, capacity, rollback, receipt recovery");System.exit(0);
    }
}
