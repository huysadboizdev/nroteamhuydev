package nro.models.services;
import nro.models.player.*;
import nro.models.task.*;
import nro.models.consts.*;
import nro.models.boss.*;
import nro.models.npc.Npc;
import static nro.models.services.PinkFixture.*;
public final class TaskProgressTest {
 static TaskService tasks;static int failures;
 interface Check {void run()throws Exception;}
 static void test(String name,Check c){try{c.run();System.out.println("PASS task "+name);}catch(Throwable e){failures++;System.out.println("FAIL task "+name+": "+e);}}
 static Owner playerTask(int id,int step)throws Exception{
  Owner p=player(701);p.gender=0;p.zone=zone();p.nPoint=new NPoint(p);p.nPoint.power=100;p.playerTask=new TaskPlayer();TaskMain t=new TaskMain();t.id=id;t.index=step;t.name="fixture";t.detail="fixture";
  for(int i=0;i<8;i++){SubTaskMain s=new SubTaskMain();s.name="fixture";s.notify="";s.maxCount=1;s.npcId=-1;s.mapId=-1;t.subTasks.add(s);}p.playerTask.taskMain=t;return p;
 }
 static Npc npc(int id)throws Exception{Npc n=blank(nro.models.npc_list.OngGohan.class);n.tempId=id;return n;}
 public static void main(String[] args)throws Exception{
  install();tasks=new TaskService();replace(TaskService.class,"i",tasks);
  replace(Service.class,"instance",new Service(){@Override public void sendThongBao(Player p,String s){}@Override public void sendFlagBag(Player p){}@Override public void addSMTN(Player p,byte t,long a,boolean o){}@Override public void sendMoney(Player p){}});
  replace(NpcService.class,"i",new NpcService(){@Override public void createTutorial(Player p,int a,String s){}@Override public int getAvatar(int id){return 0;}});
  replace(AchievementService.class,"instance",new AchievementService(){@Override public void checkDoneTask(Player p,int id){}});
  test("500k threshold",()->{Owner p=playerTask(11,0);tasks.checkDoneTaskPower(p,40000);check(p.playerTask.taskMain.index==0,"40k must not complete 500k");tasks.checkDoneTaskPower(p,600000);check(p.playerTask.taskMain.index==3,"all three eligible power steps advance");});
  test("10k base damage",()->{Owner p=playerTask(27,0);p.nPoint.dameg=10000;tasks.checkDoneTaskNangCS(p);check(p.playerTask.taskMain.index==1,"displayed10k accepted");});
  test("Android meeting maps",()->{for(int map:new int[]{93,94,96}){Owner p=playerTask(23,0);p.zone.map.mapId=map;tasks.checkDoneTaskGoToMap(p,p.zone);check(p.playerTask.taskMain.index==1,"Android meeting map "+map+" accepted");}});
  test("NPC matches metadata",()->{Owner p=playerTask(8,2);p.zone.map.mapId=21;p.playerTask.taskMain.subTasks.get(2).npcId=ConstNpc.ONG_GOHAN;p.playerTask.taskMain.subTasks.get(2).mapId=21;check(!tasks.checkDoneTaskTalkNpc(p,npc(ConstNpc.ONG_MOORI)),"wrong planet NPC rejected");check(p.playerTask.taskMain.index==2,"wrong NPC cannot advance");});
  test("capsule stack quantity",()->{Owner p=playerTask(27,1);p.playerTask.taskMain.subTasks.get(1).maxCount=50;var drop=new nro.models.map.ItemMap(p.zone,380,5,0,0,p.id);tasks.checkDoneTaskPickItem(p,drop);check(p.playerTask.taskMain.subTasks.get(1).count==5,"five capsules count five");});
  test("boss event one phase",()->{Owner p=playerTask(28,2);Boss b=blank(Boss.class);b.id=BossID.BUI_BUI;p.playerTask.taskMain.subTasks.get(2).maxCount=1;tasks.checkDoneTaskKillBoss(p,b);check(p.playerTask.taskMain.index==3,"one BuiBui kill cannot also complete second phase");});
  test("already qualified",()->{Owner p=playerTask(20,0);p.nPoint.power=700000000;TaskService.class.getMethod("checkCurrentTaskConditions",Player.class).invoke(tasks,p);check(p.playerTask.taskMain.index==1,"existing power rechecked");});
  test("terminal sentinel",()->{Owner p=playerTask(29,0);p.playerTask.taskMain.subTasks.get(0).maxCount=-1;tasks.sendNextTaskMain(p);check(p.playerTask.taskMain.index==0,"terminal task not rewarded or advanced");});
  test("turn-in requires actual item",()->{Owner p=playerTask(8,2);p.zone.map.mapId=21;p.playerTask.taskMain.subTasks.get(2).npcId=ConstNpc.ONG_GOHAN;p.playerTask.taskMain.subTasks.get(2).mapId=21;check(!tasks.checkDoneTaskTalkNpc(p,npc(ConstNpc.ONG_GOHAN)),"no dragonball means cannot turn in");p.inventory.itemsBag.add(item(20,1));check(tasks.checkDoneTaskTalkNpc(p,npc(ConstNpc.ONG_GOHAN)),"valid material turn-in succeeds");check(p.playerTask.taskMain.index==3,"turn-in advances once");});
  test("daily reward one slot",()->{Owner p=playerTask(29,0);SideTask side=p.playerTask.sideTask;side.template=new SideTaskTemplate();side.template.name="fixture";side.maxCount=1;side.count=1;side.level=0;replace(InventoryService.class,"I",new InventoryService(){@Override public int getCountEmptyBag(Player p){return 1;}@Override public boolean addItemBag(Player p,nro.models.item.Item i){p.inventory.itemsBag.add(i);return true;}@Override public void sendItemBags(Player p){}});tasks.paySideTask(p);check(side.template==null&&p.inventory.itemsBag.size()==1,"one slot receives one reward");});
  test("main reward full bag",()->{Owner p=playerTask(7,2);p.playerTask.taskMain.subTasks.get(2).npcId=ConstNpc.BUNMA;replace(InventoryService.class,"I",new InventoryService(){@Override public int getCountEmptyBag(Player p){return 0;}@Override public void sendItemBags(Player p){}});check(!tasks.checkDoneTaskTalkNpc(p,npc(ConstNpc.BUNMA)),"full bag must not consume item-reward task");check(p.playerTask.taskMain.index==2,"reward remains claimable");});
  test("planet map placeholders",()->{Owner p=playerTask(4,0);p.playerTask.taskMain.subTasks.get(0).mapId=ConstTask.MAP_500;nro.models.server.Manager.TASKS.add(p.playerTask.taskMain);try{for(byte gender=0;gender<3;gender++){p.gender=gender;check(tasks.getTaskMainById(p,4).subTasks.get(0).mapId==2+7*gender,"500 map placeholder resolves per planet");}}finally{nro.models.server.Manager.TASKS.clear();}});
  test("NPC validation uses player lock",()->{Owner p=playerTask(9,1);p.playerTask.taskMain.subTasks.get(1).npcId=ConstNpc.QUY_LAO_KAME;var reads=new java.util.concurrent.atomic.AtomicInteger();var steps=p.playerTask.taskMain.subTasks;p.playerTask.taskMain.subTasks=new java.util.ArrayList<SubTaskMain>(steps){@Override public SubTaskMain get(int i){reads.incrementAndGet();return super.get(i);}};Npc n=npc(ConstNpc.QUY_LAO_KAME);Thread t=new Thread(()->tasks.checkDoneTaskTalkNpc(p,n));boolean safe; synchronized(p){t.start();long end=System.nanoTime()+2_000_000_000L;while(t.getState()!=Thread.State.BLOCKED&&t.isAlive()&&System.nanoTime()<end)Thread.yield();safe=t.getState()==Thread.State.BLOCKED&&reads.get()==0;}t.join(3000);check(safe,"NPC metadata cannot be read outside player monitor");});
  if(failures>0)throw new AssertionError(failures+" task checks failed");System.out.println("PASS TaskProgress");
 }
}
