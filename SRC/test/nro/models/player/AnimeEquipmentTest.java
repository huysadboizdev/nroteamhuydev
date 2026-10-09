package nro.models.player;
import java.util.*;
import java.lang.reflect.*;
import nro.models.item.Item;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.ItemService;
import nro.models.task.TaskMain;
import nro.models.intrinsic.Intrinsic;
public class AnimeEquipmentTest {
 static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
 static Map<Integer,Integer> opts(Item it){Map<Integer,Integer> m=new HashMap<>();for(var o:it.itemOptions)check(m.put(o.optionTemplate.id,o.param)==null,"duplicate");return m;}
 static Item item(int id){return ItemService.gI().createNewItem((short)id);}
 public static void main(String[] args)throws Exception {
  for(int i=0;i<260;i++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(i,"#",0));
  for(int i=0;i<2040;i++){var t=new Template.ItemTemplate();t.id=(short)i;t.type=5;t.gender=3;t.name="test";Manager.ITEM_TEMPLATES.add(t);}
  Class<?> c=Class.forName("nro.models.item.AnimeEquipment");
  Method fresh=c.getMethod("freshPurchase",Item.class,Random.class);
  int[] ids={2019,2026,2027,2030,2039};
  int[][][] expected={{{50,13},{77,13},{103,13},{204,13},{14,25},{30,0}},{{77,25},{6,50000},{94,30},{30,0}},{{50,25},{0,30000},{14,15},{30,0}},{{50,20},{77,20},{103,20},{95,20},{96,20},{14,40},{30,0}},{{50,22},{77,22},{103,22},{101,100},{160,70},{14,10},{30,0}}};
  for(int n=0;n<ids.length;n++){
   Map<Integer,Integer> want=new HashMap<>();for(int[] pair:expected[n])want.put(pair[0],pair[1]);
   int permanent=0;
   for(int r=0;r<100;r++){final int roll=r;Item it=item(ids[n]);fresh.invoke(null,it,new Random(){int call;public int nextInt(int b){check(b==(call==0?100:24),"roll bounds");return call++==0?roll:23;}});var actual=opts(it);Integer day=actual.remove(93);if(day==null)permanent++;else check(day==30,"maximum duration");check(actual.equals(want),"profile "+ids[n]);long time=it.createTime; nro.models.item.ItemDefaultOptions.complete(it);check(it.createTime==time&&Objects.equals(opts(it).get(93),day),"restore cannot reroll");}
   check(permanent==20,"exact permanent probability");
   for(int day=0;day<24;day++){final int d=day;Item it=item(ids[n]);fresh.invoke(null,it,new Random(){int call;public int nextInt(int b){return call++==0?99:d;}});check(opts(it).get(93)==day+7,"each 7-30 day outcome");}
  }
  Player p=new Player();p.isPlayer=true;p.playerIntrinsic.intrinsic=new Intrinsic();p.playerTask.taskMain=new TaskMain();p.playerTask.taskMain.id=-1;
  p.nPoint.hpg=10000;p.nPoint.mpg=10000;p.nPoint.dameg=1000;p.nPoint.limitPower=9;for(int i=0;i<12;i++)p.inventory.itemsBody.add(new Item());
  for(int[] pair:new int[][]{{2027,2030,8},{2026,2019,7}}){
   p.inventory.itemsBody.set(5,item(pair[0]));p.inventory.itemsBody.set(pair[2],item(pair[1]));
   p.nPoint.calPoint();check(p.nPoint.tlHp.contains(2)&&p.nPoint.tlMp.contains(2)&&p.nPoint.tlDame.contains(2),"equipped pair effect");
   p.nPoint.calPoint();check(Collections.frequency(p.nPoint.tlHp,2)==1,"no repeated stacking");
   p.inventory.itemsBody.set(pair[2],new Item());p.nPoint.calPoint();check(!p.nPoint.tlHp.contains(2),"unequip removes pair");
   p.inventory.itemsBody.set(5,new Item());
  }
  p.inventory.itemsBody.set(7,item(2019));p.nPoint.calPoint();check(NPoint.class.getField("tlDameBoss").getInt(p.nPoint)==13,"boss percentage aggregated");p.inventory.itemsBody.set(7,new Item());p.nPoint.calPoint();check(NPoint.class.getField("tlDameBoss").getInt(p.nPoint)==0,"boss percent reset");
  p.inventory.itemsBody.set(5,item(2027));Item flag=item(2030);flag.itemOptions.add(new Item.ItemOption(93,-1));p.inventory.itemsBody.set(8,flag);p.nPoint.calPoint();check(!p.nPoint.tlHp.contains(2),"expired accessory no pair");
  var boss=nro.models.services.PinkFixture.blank(nro.models.boss.Boss.class);
  Method damage=c.getMethod("bossDamage",int.class,Player.class,int.class);
  check((int)damage.invoke(null,10000,boss,13)==11300,"boss damage +13%");check((int)damage.invoke(null,10000,p,13)==10000,"PvP unchanged");check((int)damage.invoke(null,10000,null,13)==10000,"mob unchanged");check((int)damage.invoke(null,Integer.MAX_VALUE,boss,13)==Integer.MAX_VALUE,"damage saturates without overflow");
  System.out.println("PASS anime profiles, 20% permanent, all 7-30 days, restore, pairs, boss stat reset");
 }
}
