package nro.models.player;

import java.lang.reflect.*;
import java.util.*;
import nro.models.item.Item;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.*;

public final class CollectionChestTest {
 static Class<?> rules,codec;
 static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
 @SuppressWarnings("unchecked") static List<Item> chest(Inventory inv)throws Exception{return (List<Item>)Inventory.class.getField("itemsCollection").get(inv);}
 static Item item(int id){Item i=new Item();i.template=Manager.ITEM_TEMPLATES.get(id);i.quantity=1;return i;}
 static boolean full(Inventory inv)throws Exception{return (boolean)rules.getMethod("hasBonus",Inventory.class).invoke(null,inv);}
 static Player player(){Player p=new Player();p.isPlayer=true;p.nPoint.hpg=10000;p.nPoint.mpg=10000;p.nPoint.dameg=1000;return p;}
 static void grant(Inventory inv)throws Exception {List<Item> c=chest(inv);for(int n=0;n<20;n++)c.set(n,item(710+n));}
 public static void main(String[]args)throws Exception {
  for(int id=0;id<2001;id++){Template.ItemTemplate t=new Template.ItemTemplate();t.id=(short)id;t.name="fixture";t.type=5;t.gender=3;Manager.ITEM_TEMPLATES.add(t);}
  for(int id=0;id<256;id++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"Option #",0));
  try{rules=Class.forName("nro.models.item.CollectionChestRules");codec=Class.forName("nro.models.item.CollectionChestCodec");}catch(ClassNotFoundException e){throw new AssertionError("Collection chest behavior is missing",e);}
  if(args[0].equals("rules")){
   Inventory inv=new Inventory();check(chest(inv).size()==20&&!full(inv),"Empty chest has exactly 20 slots and no bonus");grant(inv);check(full(inv),"Twenty permanent items grant bonus");
   for(int days:new int[]{-1,0,1,30}){Item i=chest(inv).get(0);i.itemOptions.add(new Item.ItemOption(93,days));check(!full(inv),"Any duration option excludes permanent status");i.itemOptions.clear();}
   chest(inv).get(0).expire=System.currentTimeMillis()+10000;check(!full(inv),"Absolute expiry excludes item");chest(inv).get(0).expire=0;
   chest(inv).get(0).quantity=20;check(!full(inv),"One stack cannot count as multiple collection slots");chest(inv).get(0).quantity=1;
   Item last=chest(inv).get(19);chest(inv).set(19,new Item());check(!full(inv),"Nineteen items do not grant any bonus");chest(inv).set(19,last);
   chest(inv).add(item(999));check(!full(inv),"Oversized collection does not bypass capacity");
  }else if(args[0].equals("stats")){
   Player p=player();p.nPoint.calPoint();long hp=p.nPoint.hpMax,ki=p.nPoint.mpMax,sd=p.nPoint.dame;grant(p.inventory);
   for(int n=0;n<5;n++){p.nPoint.calPoint();check(p.nPoint.hpMax==hp*103/100&&p.nPoint.mpMax==ki*103/100&&p.nPoint.dame==sd*103/100,"Exactly total three percent, recalculation never compounds");}
   chest(p.inventory).set(19,new Item());p.nPoint.hp=p.nPoint.hpMax;p.nPoint.mp=p.nPoint.mpMax;p.nPoint.calPoint();check(p.nPoint.hpMax==hp&&p.nPoint.mpMax==ki&&p.nPoint.dame==sd&&p.nPoint.hp<=hp&&p.nPoint.mp<=ki,"Withdrawing one removes bonus and clamps HP/KI");
   for(int flag=0;flag<4;flag++){p=flag==0?new Pet(player()):player();p.nPoint.hpg=10000;p.nPoint.mpg=10000;p.nPoint.dameg=1000;grant(p.inventory);if(flag==0)p.isPet=true;if(flag==1)p.isBoss=true;if(flag==2)p.isBot=true;if(flag==3)p.isNewPet=true;p.nPoint.calPoint();check(!p.nPoint.hasCollectionChestBonus,"Only player receives collection bonus");}
  }else if(args[0].equals("fusion")){
   Player p=player();p.pet=new Pet(p);p.pet.nPoint.hpg=10000;p.pet.nPoint.mpg=10000;p.pet.nPoint.dameg=1000;p.pet.typePet=0;p.pet.status=Pet.FUSION;p.fusion.typeFusion=6;p.nPoint.calPoint();long hp=p.nPoint.hpMax,ki=p.nPoint.mpMax,sd=p.nPoint.dame;grant(p.inventory);p.nPoint.calPoint();check(p.nPoint.hpMax==hp*103/100&&p.nPoint.mpMax==ki*103/100&&p.nPoint.dame==sd*103/100,"Three percent must apply to total stats including fusion contribution");
  }else if(args[0].equals("codec")){
   Inventory inv=new Inventory();grant(inv);Item i=chest(inv).get(0);i.createTime=123456789L;i.id=42;i.itemOptions.add(new Item.ItemOption(50,12345));
   String saved=(String)codec.getMethod("encode",List.class).invoke(null,chest(inv));
   @SuppressWarnings("unchecked") List<Item> loaded=(List<Item>)codec.getMethod("decode",String.class).invoke(null,saved);Inventory restored=new Inventory();chest(restored).clear();chest(restored).addAll(loaded);check(full(restored)&&loaded.get(0).createTime==123456789L&&loaded.get(0).id==42&&loaded.get(0).itemOptions.get(0).param==12345,"Collection and item metadata survive real codec");
   @SuppressWarnings("unchecked") List<Item> empty=(List<Item>)codec.getMethod("decode",String.class).invoke(null,"[]");check(empty.size()==20,"Old/new characters start empty with 20 slots");
   for(String bad:new String[]{"oops","null","[[]]"}){try{codec.getMethod("decode",String.class).invoke(null,bad);throw new AssertionError("Malformed collection must not be silently cleared");}catch(InvocationTargetException expected){check(expected.getCause() instanceof IllegalArgumentException,"Corrupt save fails explicitly");}}
  }else throw new AssertionError(args[0]);
  System.out.println("PASS CollectionChest "+args[0]);
 }
}
