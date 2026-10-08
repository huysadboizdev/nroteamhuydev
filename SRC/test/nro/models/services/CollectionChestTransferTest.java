package nro.models.services;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import nro.models.item.*;
import nro.models.player.Player;
import nro.models.network.Message;
import nro.models.npc.Npc;
import nro.models.server.Manager;
import nro.models.player_system.Template;
import static nro.models.services.PinkFixture.*;

public final class CollectionChestTransferTest {
 static class Owner extends Player {
  List<byte[]> boxes=new ArrayList<>();
  @Override public void sendMessage(Message m){if(m.command==-35)boxes.add(m.getData().clone());}
 }
 static class Chest extends Npc {Chest(){super(5,0,0,0,82,15288);}@Override public void confirmMenu(Player p,int i){} }
 static class Summon extends nro.models.player.NewPet {boolean disposed;Summon(Player p){super(p,(short)1,(short)1,(short)1);}@Override public void dispose(){disposed=true;}}
 static int clearedFollowers;
 static Owner owner()throws Exception{Owner p=new Owner();p.isPlayer=true;p.id=System.nanoTime();p.zone=zone();p.location.x=0;p.location.y=0;p.inventory.itemsBag.add(new Item());p.inventory.itemsBag.add(new Item());for(int i=0;i<13;i++)p.inventory.itemsBody.add(new Item());return p;}
 static Chest npc()throws Exception{Chest n=blank(Chest.class);n.tempId=82;n.mapId=5;return n;}
 static Item costume(int id,int quantity){Item i=item(id,quantity);i.template.type=5;return i;}
 public static void main(String[] args)throws Exception {
  for(int id=0;id<2001;id++){Template.ItemTemplate t=template(id);t.type=5;t.gender=3;Manager.ITEM_TEMPLATES.add(t);}
  for(int id=0;id<256;id++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"Option #",0));
  replace(Service.class,"instance",new Service(){@Override public void point(Player p){p.nPoint.calPoint();}@Override public void sendThongBao(Player p,String s){}@Override public void sendFlagBag(Player p){}@Override public void sendSpeedPlayer(Player p,int s){}@Override public void Send_Caitrang(Player p){}@Override public void sendPetFollow(Player p,short icon){if(icon==0)clearedFollowers++;}});
  replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}@Override public void sendItemBody(Player p){}});
  Owner p=owner();Chest n=npc();Item first=costume(710,1);p.inventory.itemsBag.set(0,first);
  check(!CollectionChestService.transfer(p,11,0),"Forged transfer without opening rejected");
  check(CollectionChestService.open(p,n),"Nearby NPC opens collection");
  check(p.boxes.size()==2,"Open sends contents and panel packet");
  DataInputStream in=new DataInputStream(new ByteArrayInputStream(p.boxes.get(0)));check(in.readByte()==0&&in.readUnsignedByte()==20,"Wire sends twenty slots");for(int i=0;i<20;i++)check(in.readShort()==-1,"Empty wire slot");check(in.available()==0,"Exact empty packet size");
  check(Arrays.equals(p.boxes.get(1),new byte[]{1,2}),"Collection marker matches client");
  check(CollectionChestService.transfer(p,11,0)&&p.inventory.itemsCollection.get(0)==first&&!p.inventory.itemsBag.get(0).isNotNullItem(),"Deposit preserves item identity and removes source once");
  check(!CollectionChestService.transfer(p,11,0),"Repeated packet cannot duplicate removed item");
  check(p.inventory.itemsBox.isEmpty(),"Collection never modifies ordinary box");
  check(CollectionChestService.transfer(p,13,0)&&p.inventory.itemsBag.get(0)==first,"Withdraw to bag preserves actual item");
  Item timed=costume(711,1);timed.itemOptions.add(new Item.ItemOption(93,30));p.inventory.itemsBag.set(0,timed);check(!CollectionChestService.transfer(p,11,0)&&p.inventory.itemsBag.get(0)==timed,"Reject timed costume without loss");
  Item stack=costume(712,2);p.inventory.itemsBag.set(0,stack);check(CollectionChestService.transfer(p,11,0)&&stack.quantity==1&&p.inventory.itemsCollection.get(0).quantity==1,"One dose per slot, retain remaining source");
  p.inventory.itemsBag.set(1,costume(999,1));check(!CollectionChestService.transfer(p,13,0),"Full bag cannot lose stored item");
  p.location.x=500;check(!CollectionChestService.transfer(p,11,0),"Distant access rejected");p.location.x=0;
  var old=p.zone;p.zone=zone();check(!CollectionChestService.transfer(p,11,0),"Changed zone invalidates access");p.zone=old;
  check(!CollectionChestService.transfer(p,11,-1)&&!CollectionChestService.transfer(p,13,20),"Malformed indices rejected");
  p.beforeDispose=true;check(!CollectionChestService.transfer(p,11,0),"Logging out cannot mutate chest");p.beforeDispose=false;
  p=owner();CollectionChestService.open(p,n);p.inventory.itemsBody.set(0,costume(710,1));check(CollectionChestService.transfer(p,12,0)&&!p.inventory.itemsBody.get(0).isNotNullItem(),"Body deposit unequips exactly once");
  p=owner();CollectionChestService.open(p,n);Summon pet=new Summon(p);p.newPet=pet;Item petItem=costume(710,1);petItem.template.type=27;p.inventory.itemsBody.set(7,petItem);check(CollectionChestService.transfer(p,12,7)&&p.newPet==null&&pet.disposed,"Stored pet equipment removes active summoned pet");
  p=owner();CollectionChestService.open(p,n);p.inventory.itemsBody.set(12,costume(710,1));check(CollectionChestService.transfer(p,12,12)&&clearedFollowers==1,"Stored follower equipment clears follower");
  for(int pending=0;pending<2;pending++){p=owner();CollectionChestService.open(p,n);p.inventory.itemsBag.set(0,costume(710,1));if(pending==0)p.kolRewardPending=true;else p.kolProgressPending=true;check(!CollectionChestService.transfer(p,11,0)&&p.inventory.itemsBag.get(0).isNotNullItem(),"Pending quest state blocks transfer without loss");}
  p=owner();CollectionChestService.open(p,n);for(int i=0;i<20;i++)p.inventory.itemsCollection.set(i,costume(710+i,1));p.inventory.itemsBag.set(0,costume(999,1));check(!CollectionChestService.transfer(p,11,0)&&p.inventory.itemsBag.get(0).template.id==999,"Twenty slots is hard maximum");
  p=owner();CollectionChestService.open(p,n);p.inventory.itemsBag.set(0,costume(710,1));Owner concurrent=p;ExecutorService pool=Executors.newFixedThreadPool(2);try{var a=pool.submit(()->CollectionChestService.transfer(concurrent,11,0));var b=pool.submit(()->CollectionChestService.transfer(concurrent,11,0));check(a.get()!=b.get()&&CollectionChestRules.count(p.inventory)==1,"Concurrent transfer grants one item only");}finally{pool.shutdownNow();}
  System.out.println("PASS CollectionChestTransfer actual mutations, wire, cap, failures, concurrency");
 }
}
