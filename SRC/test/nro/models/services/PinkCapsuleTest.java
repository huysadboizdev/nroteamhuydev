package nro.models.services;
import java.lang.reflect.*;
import java.util.function.IntUnaryOperator;
import java.util.concurrent.*;
import com.google.gson.Gson;
import nro.models.item.Item;
import nro.models.player.*;
import nro.models.map.*;
import static nro.models.services.PinkFixture.*;
public final class PinkCapsuleTest {
    static Class<?> service()throws Exception{try{return Class.forName("nro.models.services.PinkCapsuleService");}catch(ClassNotFoundException e){throw new AssertionError("Capsule 722 opening service is missing");}}
    static void open(Player p,Item i)throws Exception{service().getMethod("open",Player.class,Item.class).invoke(null,p,i);}
    static int roll(int branch,int value)throws Exception{final int[] calls={0};return (int)service().getMethod("rollRuby",IntUnaryOperator.class).invoke(null,(IntUnaryOperator)(bound->{check(bound==(calls[0]++==0?100:branch<80?5:95),"correct random bound");return calls[0]==1?branch:value;}));}
    static void rolls()throws Exception{for(int branch=0;branch<100;branch++)for(int v=0;v<(branch<80?5:95);v++)check(roll(branch,v)==(branch<80?1:6)+v,"exact branch/range "+branch+"/"+v);}
    static void opening()throws Exception{
        install();var p=player(777);Item i=item(722,1);p.inventory.itemsBag.add(i);p.inventory.gem=9;p.event.setEventPoint(17);
        open(p,i);check(i.quantity==0&&p.inventory.ruby>=1&&p.inventory.ruby<=100,"one capsule credits ruby");check(p.inventory.gem==9&&p.event.getEventPoint()==17,"other balances preserved");int ruby=p.inventory.ruby;open(p,i);check(p.inventory.ruby==ruby,"spent object cannot replay");
        Item forged=item(722,1);open(p,forged);check(forged.quantity==1&&p.inventory.ruby==ruby,"forged identity rejected");p.inventory.itemsBox.add(forged);open(p,forged);check(forged.quantity==1,"box rejected");
        Item wrong=item(193,1);p.inventory.itemsBag.add(wrong);open(p,wrong);check(wrong.quantity==1&&p.inventory.ruby==ruby,"teleport capsule preserved");
        Item fresh=item(722,1);p.inventory.itemsBag.add(fresh);p.inventory.ruby=-1;open(p,fresh);check(fresh.quantity==1&&p.inventory.ruby==-1,"negative balance fail closed");p.inventory.ruby=Integer.MAX_VALUE;open(p,fresh);check(fresh.quantity==1&&p.inventory.ruby==Integer.MAX_VALUE,"overflow leaves capsule");
        p.inventory.ruby=0;replace(Service.class,"instance",new Service(){@Override public void sendMoney(Player p){throw new RuntimeException("packet failure");}@Override public void sendThongBao(Player p,String s){throw new RuntimeException("packet failure");}});open(p,fresh);check(fresh.quantity==0&&p.inventory.ruby>=1,"RAM committed before packet exception");ruby=p.inventory.ruby;open(p,fresh);check(p.inventory.ruby==ruby,"packet failure cannot replay");
        install();Item race=item(722,1);p.inventory.itemsBag.add(race);p.inventory.ruby=0;Thread a=new Thread(()->{try{open(p,race);}catch(Exception e){throw new RuntimeException(e);}});Thread b=new Thread(()->{try{open(p,race);}catch(Exception e){throw new RuntimeException(e);}});a.start();b.start();a.join();b.join();check(race.quantity==0&&p.inventory.ruby>=1&&p.inventory.ruby<=100,"concurrent same capsule consumed once");
    }
    static void persistence()throws Exception{install();var p=player(777);Item i=item(722,2);p.inventory.itemsBag.add(i);p.inventory.itemsBag.add(new Item());open(p,i);Inventory loaded=new Gson().fromJson(new Gson().toJson(p.inventory),Inventory.class);check(loaded.ruby==p.inventory.ruby&&loaded.itemsBag.get(0).quantity==1,"inventory ruby and remaining capsule JSON roundtrip");check(nro.models.data.DataGame.vsItem==15,"new item catalog version exactly 15");check(nro.models.task.KolQuestCatalog.description(5).contains("Capsule hồng ngọc")&&nro.models.task.KolQuestCatalog.description(5).contains("722"),"KOL displays Capsule hồng ngọc identity");}
    static void concurrentPickup()throws Exception{
        install();var p=player(777);Item stack=item(722,1);p.inventory.itemsBag.add(stack);Zone z=zone();p.zone=z;ItemMap drop=new ItemMap(z,722,1,20,120,-1);
        replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}@Override public boolean addItemBag(Player p,Item incoming){for(Item existing:p.inventory.itemsBag)if(existing.template!=null&&existing.template.id==722){existing.quantity+=incoming.quantity;incoming.quantity=0;return true;}p.inventory.itemsBag.add(item(722,incoming.quantity));incoming.quantity=0;return true;}});
        CountDownLatch started=new CountDownLatch(1);Thread picker=new Thread(()->{started.countDown();z.pickItem(p,drop.itemMapId);});
        synchronized(p){picker.start();check(started.await(2,TimeUnit.SECONDS),"pickup started");Thread.sleep(150);check(stack.quantity==1&&!drop.isPickedUp&&p.kolQuestState.pinkCapsulesPicked==0,"pickup must wait for capsule opening player monitor");open(p,stack);}
        picker.join(2000);check(!picker.isAlive(),"pickup finishes after player unlock");check(p.inventory.itemsBag.stream().filter(i->i.template!=null&&i.template.id==722).mapToInt(i->i.quantity).sum()==1&&p.kolQuestState.pinkCapsulesPicked==1,"concurrent open/pick leaves newly picked capsule and counts once");
    }
    static void useHook()throws Exception{
        install();var p=player(777);p.nPoint=blank(NPoint.class);p.nPoint.power=Long.MAX_VALUE;Item capsule=item(722,2);p.inventory.itemsBag.add(capsule);p.inventory.itemsBag.add(new Item());
        final int[] taskCalls={0};replace(TaskService.class,"i",new TaskService(){@Override public void checkDoneTaskUseItem(Player p,Item i){check(Thread.holdsLock(p),"capsule use task callback holds player monitor");taskCalls[0]++;}});
        replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){check(Thread.holdsLock(p),"722 use hook must only refresh bag under player monitor");}});
        Method use=nro.models.services_func.UseItem.class.getDeclaredMethod("useItem",Player.class,Item.class,int.class);use.setAccessible(true);use.invoke(nro.models.services_func.UseItem.gI(),p,capsule,0);check(capsule.template.type==27&&capsule.quantity==1&&p.inventory.ruby>=1,"real default-ID use hook opens type27 capsule");check(taskCalls[0]==1,"successful use retains original task callback once");
    }
    public static void main(String[] a)throws Exception{try{switch(a[0]){case "roll"->rolls();case "open"->opening();case "persistence"->persistence();case "concurrent"->concurrentPickup();case "hook"->useHook();default->throw new AssertionError(a[0]);}System.out.println("PASS PinkCapsule "+a[0]);System.exit(0);}catch(Throwable failure){failure.printStackTrace();System.exit(1);}}
}
