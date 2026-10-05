package nro.models.shop;

import java.sql.*;
import nro.models.database.ShopDAO;
import nro.models.item.*;
import nro.models.player.*;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.*;
import static nro.models.services.PinkFixture.*;

/** Read-only JDBC integration; purchases operate on a disconnected fixture, never a saved player. */
public final class CostumeSqlIntegrationTest {
    public static void main(String[] args) throws Exception {
        check(args.length==1 && args[0].matches("team2026_costume_test_[a-z0-9]+"), "isolated test database required");
        Class.forName("com.mysql.jdbc.Driver");
        try(Connection con=DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/"+args[0]+"?useUnicode=true&characterEncoding=UTF-8&useSSL=false","root","")) {
            for(int id=0;id<2001;id++)Manager.ITEM_TEMPLATES.add(template(id));
            for(int id=0;id<=251;id++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"option #",0));
            try(Statement st=con.createStatement();ResultSet rs=st.executeQuery("SELECT * FROM item_template WHERE id IN (710,711,1710,1869)")) {
                while(rs.next()) {
                    var t=Manager.ITEM_TEMPLATES.get(rs.getInt("id"));t.type=rs.getByte("type");t.name=rs.getString("name");t.gender=rs.getByte("gender");t.isUpToUp=false;
                    if(t.id==1710)check(rs.getInt("head")==1603&&rs.getInt("body")==1606&&rs.getInt("leg")==1607,"Doctor parts persisted");
                    if(t.id==1869)check(rs.getString("description").equals("Khi mặc vào sẽ đánh yếu hơn bình thường 40%, khi cởi ra sẽ tăng sức đánh 50%"),"full armor description persisted without truncation");
                }
            }
            try(Statement st=con.createStatement();ResultSet rs=st.executeQuery("SELECT NAME FROM item_option_template WHERE id=251")) {
                check(rs.next()&&rs.getString(1).equals("Khi mặc vào sẽ đánh yếu hơn bình thường 40%, khi cởi ra sẽ tăng sức đánh 50%"),"UTF-8 effect description persisted");
            }
            ItemDefaultOptions.loadFromDatabase(con);
            Shop shop=ShopDAO.getShops(con).stream().filter(s->"SANTA_HONG_NGOC".equals(s.tagName)).findFirst().orElseThrow();
            check(shop.typeShop==3&&shop.npcId==39&&shop.tabShops.size()==1,"actual SQL shop context: armor and costumes share one tab");
            replace(Service.class,"instance",new Service(){@Override public void sendThongBao(Player p,String s){}@Override public void sendMoney(Player p){}});
            replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}});
            Player p=player(987654321);p.idMark=new IDMark();p.inventory.ruby=7000;p.inventory.gem=123;p.inventory.gold=456;
            for(int i=0;i<8;i++)p.inventory.itemsBag.add(ItemService.gI().createItemNull());
            p.idMark.setShopOpen(shop);p.idMark.setTagNameShop(shop.tagName);
            ShopService.gI().takeItem(p,(byte)0,1710);check(p.inventory.ruby==7000,"chain prevents premature purchase");
            int[] ids={710,711,1710,1869},prices={1000,1500,2000,2500};int remaining=7000,total=0;
            for(int i=0;i<ids.length;i++) {
                ItemShop row=shop.getItemShop(ids[i]);check(row.cost==prices[i]&&row.typeSell==3&&row.iconSpec==7743,"persisted ruby price");
                ShopService.gI().takeItem(p,(byte)0,ids[i]);remaining-=prices[i];check(p.inventory.ruby==remaining,"actual SQL purchase debit");
                int itemId=ids[i];
                Item item=p.inventory.itemsBag.stream().filter(x->x.isNotNullItem()&&x.template.id==itemId).findFirst().orElseThrow();
                check(item.itemOptions.size()==(ids[i]==1869?6:5),"full purchased options");total+=item.itemOptions.size();
                if(ids[i]==1869)check(item.itemOptions.stream().anyMatch(o->o.optionTemplate.id==47&&o.param==50000),"50000 armor integer");
            }
            check(total==21&&p.inventory.ruby==0&&p.inventory.gem==123&&p.inventory.gold==456,"21 options and ruby-only chain");
            Item restored=ItemService.gI().createItemForRestore((short)1869,1);restored.itemOptions.add(new Item.ItemOption(9,12));restored.itemOptions.add(new Item.ItemOption(47,60000));
            ItemDefaultOptions.complete(restored);check(restored.itemOptions.size()==6&&restored.itemOptions.stream().anyMatch(o->o.optionTemplate.id==9&&o.param==12),"saved timer preserved");
            System.out.println("PASS SQL integration: 21 options, ruby chain, UTF-8, Doctor parts, saved timer");
        }
    }
}
