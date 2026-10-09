package nro.models.services;
import java.sql.*;
import java.util.*;
import nro.models.server.Manager;
import nro.models.player_system.Template;
import nro.models.item.ItemTemplateIndex;
import nro.models.database.ShopDAO;
public class AnimeShopSqlReadOnlyTest {
 static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception {
  Class.forName("com.mysql.jdbc.Driver");
  try(Connection con=DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/team2026?useUnicode=true&characterEncoding=UTF-8","root","")){
   try(Statement st=con.createStatement()){st.execute("SET SESSION TRANSACTION READ ONLY");}
   con.setAutoCommit(false);
   try(Statement st=con.createStatement();ResultSet rs=st.executeQuery("SELECT * FROM item_template ORDER BY id")){
    while(rs.next()){var t=new Template.ItemTemplate();t.id=rs.getShort("id");t.type=rs.getByte("type");t.gender=rs.getByte("gender");t.name=rs.getString("name");t.description=rs.getString("description");t.iconID=rs.getShort("icon_id");t.part=rs.getShort("part");t.head=rs.getInt("head");t.body=rs.getInt("body");t.leg=rs.getInt("leg");Manager.ITEM_TEMPLATES.add(t);}
   }
   ItemTemplateIndex.align(Manager.ITEM_TEMPLATES);
   try(Statement st=con.createStatement();ResultSet rs=st.executeQuery("SELECT * FROM item_option_template ORDER BY id")){
    while(rs.next())Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(rs.getInt("id"),rs.getString("name"),0));
   }
   var shops=ShopDAO.getShops(con);var shop=shops.stream().filter(s->s.id==37).findFirst().orElseThrow();
   check(shop.tagName.equals("SANTA_HONG_NGOC")&&shop.npcId==39&&shop.typeShop==3,"actual ruby shop");
   for(int id:new int[]{2019,2026,2027,2030,2039}){
    var row=shop.getItemShop(id);check(row!=null&&row.temp.id==id&&row.cost==200&&row.typeSell==3&&row.iconSpec==7743,"SQL and real DAO item "+id);
    var item=ItemService.gI().createItemFromItemShop(row);check(item.template.id==id,"actual template mapping");
    check(item.itemOptions.stream().anyMatch(o->o.optionTemplate.id==30),"nontrade");
   }
   var akatsuki=shop.getItemShop(2039).temp;check(akatsuki.head==2134&&akatsuki.body==2135&&akatsuki.leg==2136,"actual Akatsuki path");
   con.rollback();
  }
  System.out.println("PASS deployed SQL + real ShopDAO: all five correct IDs, 200 ruby, options and Akatsuki path (read only)");
 }
}
