package nro.models.server;

import java.util.*;
import nro.models.player.*;
import nro.models.network.MySession;
import nro.models.services.*;
import nro.models.item.Item;
import static nro.models.services.PinkFixture.*;

public class AdminCommandTest {
    static final List<String> events = new ArrayList<>();
    static final List<Item> grants = new ArrayList<>();
    static Player admin() throws Exception {
        Player p=player(900); p.idMark=new IDMark(); p.nPoint=blank(NPoint.class);
        p.session=blank(MySession.class); p.session.player=p; p.session.isAdmin=true;
        return p;
    }
    static void setup() throws Exception {
        install();
        replace(ItemService.class,"i",new ItemService(){
            @Override public nro.models.player_system.Template.ItemTemplate getTemplate(int id){var t=template(id);if(id==432)t.isUpToUp=false;if(id==14)t.type=10;return t;}
            @Override public nro.models.player_system.Template.ItemOptionTemplate getItemOptionTemplate(int id){var t=new nro.models.player_system.Template.ItemOptionTemplate();t.id=id;return t;}
            @Override public List<Item.ItemOption> getListOptionItemShop(short id){return List.of();}
        });
        replace(Service.class,"instance",new Service(){
            @Override public void sendThongBao(Player p,String s){events.add(s);}
            @Override public void point(Player p){}
            @Override public void addSMTN(Player p,byte t,long n,boolean b){events.add("power:"+n);}
        });
    }
    public static void main(String[] args) throws Exception {
        setup(); Player p=admin(); Command c=Command.gI(); String mode=args[0];
        switch(mode) {
            case "routing" -> {
                for(String s:List.of("ban","buff","menu","map","nhiemvu","123","toadoXYZ"))
                    check(!c.check(p,s),"prefix consumed: "+s);
                check(c.check(p,"dmg   100"),"dmg alias missing"); check(p.nPoint.dameg==100,"dmg alias value");
                check(c.check(p,"hpg  200"),"hpg alias missing"); check(p.nPoint.hpg==200,"hpg alias value");
            }
            case "stats" -> {
                p.nPoint.dameg=25; p.nPoint.hpg=50; p.nPoint.mpg=70;
                c.check(p,"dm -1");c.check(p,"hp 0");c.check(p,"ki -2");c.check(p,"up -3");
                check(p.nPoint.dameg==25 && p.nPoint.hpg==50 && p.nPoint.mpg==70,"invalid stats mutated");
                check(events.stream().noneMatch(s->s.startsWith("power:")),"negative power granted");
                events.clear();p.nPoint.tiemNang=Long.MAX_VALUE-1;
                p.pet=blank(Pet.class);p.pet.master=p;p.pet.nPoint=blank(NPoint.class);
                c.check(p,"upp 100");check(events.stream().noneMatch(s->s.startsWith("power:")),"pet grant overflows master potential");
            }
            case "items" -> {
                replace(InventoryService.class,"I",new InventoryService(){@Override public boolean addItemBag(Player x,Item i){return false;}@Override public void sendItemBags(Player x){}});
                c.check(p,"i 722 2"); check(events.stream().noneMatch(s->s.contains("SUCCESS")),"false inventory success");
                events.clear(); c.check(p,"i 722 -2");check(events.stream().noneMatch(s->s.contains("SUCCESS")),"negative grant success");
                replace(InventoryService.class,"I",new InventoryService(){@Override public boolean addItemBag(Player x,Item i){grants.add(i);return true;}@Override public void sendItemBags(Player x){}});
                c.check(p,"i 65536 1");check(grants.isEmpty(),"item id short wrap");
                c.check(p,"i   432   2 50:10");check(grants.size()==2,"whitespace item parsing");
                check(grants.get(0).itemOptions.get(0)!=grants.get(1).itemOptions.get(0),"shared mutable option");
                events.clear();p.inventory.gem=Integer.MAX_VALUE-1;
                replace(InventoryService.class,"I",new InventoryService(){@Override public boolean addItemBag(Player x,Item i){x.inventory.gem=(int)Math.min(Integer.MAX_VALUE,(long)x.inventory.gem+i.quantity);return true;}@Override public void sendItemBags(Player x){}});
                c.check(p,"i 14 2");check(events.stream().anyMatch(s->s.contains("1/2")),"currency grant did not report actual clamp delta");
            }
            case "expansion" -> {
                replace(InventoryService.class,"I",new InventoryService(){
                    @Override public void sendItemBags(Player x){}
                });
                c.check(p,"i 517 10");check(p.inventory.itemsBag.size()==10,"bag expansion did not execute ten units");
                c.check(p,"i 518 3");check(p.inventory.itemsBox.size()==3,"box expansion did not execute three units");
                events.clear();while(p.inventory.itemsBag.size()<Inventory.MAX_ITEMS_BAG-1)p.inventory.itemsBag.add(new Item());
                c.check(p,"i 517 10");check(p.inventory.itemsBag.size()==Inventory.MAX_ITEMS_BAG,"bag cap exceeded");
                check(events.stream().anyMatch(s->s.contains("1/10")),"bag expansion did not report actual partial receipt");
            }
            case "incremental" -> {
                replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player x){}});
                var existing=item(722,1);existing.itemOptions.add(new Item.ItemOption(1,5));p.inventory.itemsBag.add(existing);
                c.check(p,"i 722 3 1:7");check(existing.itemOptions.get(0).param==26,"incremental option did not apply three units");
                existing.itemOptions.get(0).param=Integer.MAX_VALUE-5;events.clear();
                c.check(p,"i 722 2 1:5");check(existing.itemOptions.get(0).param==Integer.MAX_VALUE,"incremental option overflow");
                check(events.stream().anyMatch(s->s.contains("1/2")),"incremental overflow did not report partial receipt");
                replace(ItemService.class,"i",new ItemService(){
                    @Override public nro.models.player_system.Template.ItemTemplate getTemplate(int id){return template(id);}
                    @Override public nro.models.player_system.Template.ItemOptionTemplate getItemOptionTemplate(int id){var t=new nro.models.player_system.Template.ItemOptionTemplate();t.id=id;return t;}
                    @Override public List<Item.ItemOption> getListOptionItemShop(short id){return List.of(new Item.ItemOption(31,9));}
                });
                p.inventory.itemsBag.clear();existing=item(722,1);existing.itemOptions.add(new Item.ItemOption(31,7));p.inventory.itemsBag.add(existing);
                c.check(p,"i 722 4");check(existing.itemOptions.get(0).param==43,"default shop incremental option did not apply four units");
            }
            case "permission" -> {
                p.session.player=player(901); c.check(p,"dm 100");check(p.nPoint.dameg==0,"stale session accepted");
            }
            case "task" -> {
                p.playerTask=new nro.models.task.TaskPlayer();
                var old=new nro.models.task.TaskMain();old.id=3;old.index=1;p.playerTask.taskMain=old;
                var target=new nro.models.task.TaskMain();target.id=7;target.subTasks.add(new nro.models.task.SubTaskMain());
                Manager.TASKS.clear();Manager.TASKS.add(target);
                replace(TaskService.class,"i",new TaskService(){
                    @Override public nro.models.task.TaskMain getTaskMainById(Player x,int id){var copy=new nro.models.task.TaskMain(target);copy.id=id;return copy;}
                    @Override public void sendNextTaskMain(Player x){events.add("reward");}
                    @Override public void sendTaskMain(Player x){events.add("task");}
                });
                c.check(p,"n 999");check(p.playerTask.taskMain==old&&old.id==3&&old.index==1,"invalid task mutated");
                c.check(p,"n 7");check(p.playerTask.taskMain.id==7&&p.playerTask.taskMain.index==0,"target task not assigned");
                check(events.contains("task")&&!events.contains("reward"),"old task reward paid");
                var zero=new nro.models.task.TaskMain(target);zero.id=0;Manager.TASKS.add(zero);
                c.check(p,"n 0");check(p.playerTask.taskMain.id==0,"valid task zero rejected");
            }
            case "map" -> {
                replace(nro.models.map.service.MapService.class,"i",new nro.models.map.service.MapService(){
                    @Override public nro.models.map.Map getMapById(int id){return null;}
                    @Override public nro.models.map.Zone getMapCanJoin(Player x,int id,int zone){return null;}
                });
                c.check(p,"m 999");check(!events.isEmpty(),"invalid map silently ignored");
                events.clear();c.check(p,"m 5 9");check(!events.isEmpty(),"invalid zone silently ignored");
                var selected=zone();selected.map.tileMap=new int[][]{{0}};selected.map.tileTop=new int[]{0};
                selected.map.zones=new ArrayList<>();selected.map.zones.add(selected);
                var fallback=zone();fallback.map.mapId=21;
                replace(nro.models.map.service.MapService.class,"i",new nro.models.map.service.MapService(){
                    @Override public nro.models.map.Map getMapById(int id){return selected.map;}
                    @Override public nro.models.map.Zone getMapCanJoin(Player x,int id,int zone){x.location.x=300;x.location.y=450;return fallback;}
                });
                p.location=new Location();p.location.x=17;p.location.y=23;p.zone=selected;
                events.clear();c.check(p,"m 5");check(events.stream().anyMatch(s->s.contains("đúng map/khu")),"fallback map not rejected");
                check(p.zone==selected&&p.location.x==17&&p.location.y==23,"rejected map changed position");
            }
        }
        System.out.println("PASS AdminCommandTest "+mode); System.exit(0);
    }
}
