package nro.models.shop;

import java.util.random.RandomGenerator;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.intrinsic.Intrinsic;
import nro.models.task.TaskMain;
import static nro.models.services.PinkFixture.*;

public final class BeerusDestroyerBonusTest {
 static final class Roll implements RandomGenerator {
  final int type,percent,base;Roll(int t,int p){this(t,p,0);}Roll(int t,int p,int b){type=t;percent=p;base=b;}
  public long nextLong(){return 0;}
  public int nextInt(int bound){check(bound==3,"type distribution bound");return type;}
  public int nextInt(int origin,int bound){if(origin==1&&bound==6)return percent;if(origin==1&&bound==101)return base==15?1:base>=11?2:base>=7?16:base>=4?36:61;check(base>=origin&&base<bound,"base roll range");return base;}
 }
 static Item reward(){Item i=BeerusDestroyerTradeTest.gear(650);i.itemOptions.add(new Item.ItemOption(47,1600));return i;}
 public static void main(String[] args){try{install();String m=args[0];
  if(m.equals("roll")){for(int type=0;type<3;type++)for(int percent=1;percent<=5;percent++){
   Item i=reward();DestroyerBonusRoll.apply(i,new Roll(type,percent));check(i.itemOptions.size()==2&&i.itemOptions.get(0).optionTemplate.id==47&&i.itemOptions.get(0).param==1600,"base preserved");
   Item.ItemOption bonus=i.itemOptions.get(1);check(bonus.optionTemplate.id==new int[]{77,103,50}[type]&&bonus.param==percent,"deterministic bonus");
  }}
  else if(m.equals("invalid")){for(int id:new int[]{50,77,103,34,35,36,87}){Item i=reward();i.itemOptions.add(new Item.ItemOption(id,1));boolean rejected=false;try{DestroyerBonusRoll.apply(i,new Roll(0,1));}catch(IllegalArgumentException expected){rejected=true;}check(rejected&&i.itemOptions.size()==2,"reject configured duplicate/flags "+id);}Item locked=reward();locked.itemOptions.add(new Item.ItemOption(30,0));DestroyerBonusRoll.apply(locked,new Roll(0,1));check(locked.itemOptions.size()==3,"explicit nontrade rule coexists with bonus");}
  else if(m.equals("point")){for(int type=0;type<3;type++){
   Player p=new Player();p.idNRNM=-1;p.playerIntrinsic.intrinsic=new Intrinsic();p.playerTask.taskMain=new TaskMain();p.playerTask.taskMain.id=-1;p.nPoint.hpg=100000;p.nPoint.mpg=100000;p.nPoint.dameg=1000;p.nPoint.limitPower=9;p.nPoint.power=90010000000L;p.zone=zone();
   p.inventory.itemsBody.clear();for(int n=0;n<12;n++)p.inventory.itemsBody.add(new Item());
   Item i=reward();p.inventory.itemsBody.set(0,i);p.nPoint.calPoint();int hp=p.nPoint.hpMax,ki=p.nPoint.mpMax,sd=p.nPoint.dame;
   DestroyerBonusRoll.apply(i,new Roll(type,5));p.nPoint.calPoint();check(p.nPoint.hpMax==(type==0?hp*105L/100:hp),"HP real single equipped option");check(p.nPoint.mpMax==(type==1?ki*105L/100:ki),"KI real single equipped option");check(p.nPoint.dame==(type==2?sd*105L/100:sd),"damage real single equipped option");
  }}
  else if(m.equals("base")){for(int base=0;base<=15;base++){
   Item i=BeerusDestroyerTradeTest.gear(657);i.itemOptions.add(new Item.ItemOption(0,8800));i.itemOptions.add(new Item.ItemOption(21,50));i.itemOptions.add(new Item.ItemOption(30,0));
   DestroyerBonusRoll.apply(i,new Roll(2,5,base));check(i.getOptionParam(0)==8800+8800*base/100,"base attack bonus "+base);check(i.getOptionParam(21)==50&&i.getOptionParam(30)==0,"power and nontrade unchanged");check(i.getOptionParam(50)==5,"independent damage line retained");
  }}
  else if(m.equals("baseoptions")){Item i=reward();for(int id:new int[]{0,22,23,14,27,28})i.itemOptions.add(new Item.ItemOption(id,100));i.itemOptions.add(new Item.ItemOption(21,40));i.itemOptions.add(new Item.ItemOption(30,0));DestroyerBonusRoll.apply(i,new Roll(0,3,15));for(int id:new int[]{0,22,23,14,27,28})check(i.getOptionParam(id)==115,"all old base options boosted "+id);check(i.getOptionParam(47)==1840&&i.getOptionParam(77)==3,"armor boosted and HP line independent");check(i.getOptionParam(21)==40&&i.getOptionParam(30)==0,"flags untouched");}
  else throw new AssertionError(m);
  System.out.println("BEERUS_BONUS PASS "+m);System.exit(0);
 }catch(Throwable t){t.printStackTrace();System.exit(1);}}
}
