package nro.models.player;
import java.io.*;
import java.lang.reflect.*;
import nro.models.item.Item;
import nro.models.network.Message;
import nro.models.player_badges.*;
import nro.models.services.*;
import nro.models.shop.*;
import nro.models.server.Manager;

public class OwnedBadgeFreeActionTest {
 static class Owner extends Player{byte[] shopPacket;public void sendMessage(Message m){if(m.command==-44)shopPacket=m.getData();}}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args){try{
  PinkFixture.install();PinkFixture.replace(Service.class,"instance",new Service(){public void sendThongBao(Player p,String s){}public void point(Player p){}public void Send_Info_NV(Player p){}});
  for(int id=0;id<221;id++)Manager.ITEM_OPTION_TEMPLATES.add(new nro.models.player_system.Template.ItemOptionTemplate(id,"option #",0));
  Owner p=new Owner();p.isPlayer=true;p.nPoint=new NPoint(p){public void calPoint(){}};p.nPoint.hpMax=p.nPoint.mpMax=1000;
  p.inventory.gold=12345;p.inventory.gem=12;p.inventory.ruby=34;
  BagesTemplate t=new BagesTemplate();t.idEffect=218;t.idItem=1289;t.NAME="fixture badge";Manager.BAGES_TEMPLATES.add(t);
  BadgesData data=new BadgesData(218,System.currentTimeMillis()+29*86400000L,false);p.dataBadges.add(data);long expiry=data.timeofUseBadges;
  TabShop base=new TabShop();base.id=45;base.name="Sở Hữu";ItemShop row=new ItemShop();row.temp=PinkFixture.template(1289);row.temp.type=36;row.temp.gender=3;row.cost=2;row.typeSell=1;row.tabShop=base;base.itemShops.add(row);
  TabShopSoHuu owned=new TabShopSoHuu(base,p);Shop shop=new Shop();shop.typeShop=0;shop.tagName="DANH_HIEU";shop.tabShops.add(owned);
  if(args[0].equals("copy")){
   check(owned.itemShops.size()==1&&owned.itemShops.get(0).cost==0&&owned.itemShops.get(0).typeSell==0,"owned row must be free");
   check(base.itemShops.get(0).cost==2&&base.itemShops.get(0).typeSell==1,"catalog untouched");
   p.dataBadges.clear();base.id=44;check(new TabShopDanhHieu(base,p).itemShops.get(0).cost==2,"available tab retains catalog price");
  }else if(args[0].equals("wire")){
   Method open=ShopService.class.getDeclaredMethod("openShopType0",Player.class,Shop.class);open.setAccessible(true);open.invoke(ShopService.gI(),p,shop);
   check(p.shopPacket!=null,"actual shop packet");var in=new DataInputStream(new ByteArrayInputStream(p.shopPacket));
   check(in.readByte()==0&&in.readByte()==1&&in.readUTF().equals("Sở Hữu")&&in.readUnsignedByte()==1&&in.readShort()==1289,"header");
   int gold=in.readInt(),gem=in.readInt();check(gold==0&&gem==0,"client free action needs0/0 got"+gold+"/"+gem);
   check(in.readUnsignedByte()==0&&in.readByte()==0&&in.readByte()==0&&in.available()==0,"remaining layout intact");
  }else{
   p.idMark.setShopOpen(shop);p.idMark.setTagNameShop(shop.tagName);
   if(args[0].equals("expired"))data.timeofUseBadges=0;
   if(args[0].equals("missing"))p.dataBadges.clear();
   if(args[0].equals("already")){data.isUse=true;p.badges.idBadges=218;}
   ShopService.gI().buyItem(p,1289);
   if(args[0].equals("action")){check(data.isUse,"activate owned badge");check(data.timeofUseBadges==expiry&&p.dataBadges.size()==1,"no renewal or extra record");ShopService.gI().buyItem(p,1289);check(p.dataBadges.size()==1&&data.timeofUseBadges==expiry,"repeat safe");}
   else if(args[0].equals("expired"))check(!data.isUse,"expired cannot activate");
   else if(args[0].equals("missing"))check(p.dataBadges.isEmpty(),"missing cannot grant");
   else if(args[0].equals("already"))check(data.isUse&&data.timeofUseBadges==expiry,"already active unchanged");
   else throw new AssertionError(args[0]);
   check(p.inventory.gold==12345&&p.inventory.gem==12&&p.inventory.ruby==34,"no money taken");
  }
  System.out.println("OWNED_BADGE_FREE "+args[0]+" PASS");System.exit(0);
 }catch(Throwable e){e.printStackTrace();System.exit(1);}}
}
