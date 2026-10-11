package nro.models.npc_list;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicInteger;
import nro.models.map.Map;
import nro.models.map.Zone;
import nro.models.network.Message;
import nro.models.npc.Npc;
import nro.models.player.*;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services.*;
import nro.models.services_func.Input;
import nro.models.skill.*;
import nro.models.utils.Util;
import sun.misc.Unsafe;

/** Real NPC handlers and pet factory; network/task boundaries replaced for offline tests. */
public final class HomeNpcSupportTest {
    private static boolean taskDone;
    private static int taskCalls;
    private static final int MENU = 2026100201;
    private static Unsafe unsafe;
    private static final AtomicInteger created = new AtomicInteger();
    private static void replace(Class<?> c,String name,Object value)throws Exception {
        Field f=c.getDeclaredField(name);f.setAccessible(true);
        if(java.lang.reflect.Modifier.isFinal(f.getModifiers()))
            unsafe.putObjectVolatile(unsafe.staticFieldBase(f),unsafe.staticFieldOffset(f),value);
        else f.set(null,value);
    }
    private static final class Owner extends Player {
        final List<byte[]> menus = new ArrayList<>();
        final List<byte[]> forms = new ArrayList<>();
        @Override public void sendMessage(Message m) {
            if(m.command==32)menus.add(m.getData());
            if(m.command==-125)forms.add(m.getData());
        }
    }
    public static void main(String[] args)throws Exception {
        Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);unsafe=(Unsafe)f.get(null);
        replace(Service.class,"instance",new Service(){
            @Override public void sendThongBao(Player p,String text){}
            @Override public void chatJustForMe(Player p,Player pet,String text){}
            @Override public void sendMoney(Player p){}
            @Override public void point(Player p){}
            @Override public void showInfoPet(Player p){}
        });
        replace(TaskService.class,"i",new TaskService(){
            @Override public boolean checkDoneTaskTalkNpc(Player p,Npc npc){taskCalls++;return taskDone;}
        });
        replace(PetService.class,"instance",new PetService(){
            @Override public void createNormalPet(Player p,byte... limit){
                created.incrementAndGet();super.createNormalPet(p,limit);
            }
        });
        for(int planet=0;planet<3;planet++) {
            NClass c=new NClass();Manager.NCLASS.add(c);
            Template.SkillTemplate t=new Template.SkillTemplate();t.id=(byte)(planet*2);t.name="Basic attack";
            Skill s=new Skill();s.template=t;s.point=1;s.skillId=(short)planet;t.skillss.add(s);c.skillTemplatess.add(t);
        }
        switch(args[0]) {
            case "menu" -> menu(); case "gems" -> gems(); case "pets" -> pets();
            case "concurrent" -> concurrent(); case "guards" -> guards();
            case "giftcode" -> giftcode(); case "membership" -> membership();
            default -> throw new AssertionError("Unknown mode");
        }
        System.out.println("PASS: Home NPC "+args[0]);
    }
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    private static Owner owner(int map)throws Exception {
        Owner p=new Owner();p.id=456;p.gender=1;
        Map m=(Map)unsafe.allocateInstance(Map.class);m.mapId=map;
        p.zone=new Zone(m,0,20);p.inventory.gold=123;p.inventory.ruby=456;
        p.idMark.setIndexMenu(MENU);return p;
    }
    private static OngGohan npc(int planet) {
        return switch(planet) {
            case 0 -> new OngGohan(21,1,100,100,0,0);
            case 1 -> new OngMoori(22,1,100,100,2,0);
            default -> new OngParagus(23,1,100,100,1,0);
        };
    }
    private static void menu()throws Exception {
        for(int planet=0;planet<3;planet++) {
            Owner p=owner(21+planet);OngGohan n=npc(planet);taskDone=false;int before=taskCalls;
            n.openBaseMenu(p);
            check(taskCalls==before+1,"Talk-task processing retained");
            check(p.menus.size()==1 && p.idMark.getIndexMenu()==MENU,"All houses expose support menu");
            DataInputStream in=new DataInputStream(new ByteArrayInputStream(p.menus.get(0)));
            check(in.readShort()==n.tempId,"Correct NPC template");
            String greeting = in.readUTF();
            check(greeting.contains("dưới 10 triệu"), "Menu explains eligibility threshold");
            check(!greeting.contains("Dùng hết"), "Menu does not require spending every gem");
            check(in.readByte()==5,"Five menu choices including free membership");
            check(in.readUTF().contains("ngọc xanh"),"Green gem choice");
            check(in.readUTF().contains("đệ tử"),"Normal pet choice");
            check(in.readUTF().equals("Nhập giftcode"),"Giftcode choice");
            check(in.readUTF().contains("Mở thành viên"),"Free membership choice in all houses");
            check(in.readUTF().equals("Đóng"),"Close choice");
            taskDone=true;n.openBaseMenu(p);check(p.menus.size()==1,"Task completion does not override quest dialog");taskDone=false;
        }
    }
    public static class Connected extends nro.models.network.MySession {
        public Connected()throws Exception{super(null);}
        @Override public boolean isConnected(){return true;}
    }
    private static void membership()throws Exception {
        AtomicInteger writes=new AtomicInteger();
        Field dao=FreeMembershipService.class.getDeclaredField("dao");dao.setAccessible(true);
        dao.set(FreeMembershipService.gI(),new nro.models.database.FreeMembershipDAO(){
            @Override public boolean activate(long p,int a,String t){writes.incrementAndGet();return true;}
        });
        for(int planet=0;planet<3;planet++){
            Owner p=owner(21+planet);OngGohan n=npc(planet);
            p.session=(Connected)unsafe.allocateInstance(Connected.class);p.session.player=p;p.session.userId=123;
            p.playerTask.taskMain=new nro.models.task.TaskMain();p.playerTask.taskMain.id=1;
            p.playerTask.taskMain.subTasks.add(new nro.models.task.SubTaskMain());
            n.confirmMenu(p,3);check(!p.session.actived&&writes.get()==planet,"unfinished task denied at house");
            p.playerTask.taskMain.id=2;n.confirmMenu(p,3);
            check(p.session.actived&&writes.get()==planet+1,"all three NPC routes open free membership");
            n.confirmMenu(p,3);check(writes.get()==planet+1,"no repeated activation");
            check(p.inventory.gold==123&&p.inventory.ruby==456,"NPC activation charges no currencies");
        }
    }
    private static void gems() throws Exception {
        for (int planet = 0; planet < 3; planet++) {
            Owner p = owner(21 + planet);
            OngGohan n = npc(planet);
            for (int before : new int[]{0, 1, 5_000_000, 9_999_999}) {
                p.inventory.gem = before;
                n.confirmMenu(p, 0);
                int expected = before + 10_000_000;
                check(p.inventory.gem == expected, "Add full reward below threshold: " + before);
                n.confirmMenu(p, 0);
                check(p.inventory.gem == expected, "Repeated click above threshold gives no reward");
            }
            for (int before : new int[]{10_000_000, 15_000_000, -1, Integer.MAX_VALUE}) {
                p.inventory.gem = before;
                n.confirmMenu(p, 0);
                check(p.inventory.gem == before, "Blocked balance preserved: " + before);
            }
            p.inventory.gem = 9_000_000;
            n.confirmMenu(p, 0);
            check(p.inventory.gem == 19_000_000, "Claim again after spending below threshold");
            check(p.inventory.gold == 123 && p.inventory.ruby == 456, "Other currencies preserved");
        }
    }
    private static void pets()throws Exception {
        Field rf=Util.class.getDeclaredField("rand");rf.setAccessible(true);Random rng=(Random)rf.get(null);
        Set<Byte> genders=new HashSet<>();
        // Seeds fixed so this checks all outcomes without a probabilistic assertion.
        for(int seed=0;seed<30;seed++) {
            Owner p=owner(21+seed%3);OngGohan n=npc(seed%3);rng.setSeed(seed);n.confirmMenu(p,1);
            check(p.pet!=null,"Receive normal pet when absent");Pet pet=p.pet;genders.add(pet.gender);
            check(pet.typePet==0 && pet.nPoint.power==2000 && pet.nPoint.limitPower==0 && pet.nPoint.tiemNang==0,"Fresh normal defaults");
            check(pet.gender>=0 && pet.gender<=2,"Three planet gender range");
            n.confirmMenu(p,1);check(p.pet==pet,"Existing normal pet remains identical");
            check(p.inventory.gold==123 && p.inventory.ruby==456 && p.inventory.gem==0,"Pet is free");
        }
        check(genders.size()==3,"Fixed seeds exercise all three genders");
        for(int type=0;type<=4;type++) {
            Owner p=owner(21);Pet pet=new Pet(p);pet.typePet=(byte)type;pet.nPoint.limitPower=9;pet.nPoint.power=80_000_000_000L;p.pet=pet;
            npc(0).confirmMenu(p,1);check(p.pet==pet && pet.master==p && pet.nPoint.limitPower==9 && pet.nPoint.power==80_000_000_000L,"Never replace existing type "+type);
        }
    }
    private static void race(Runnable action)throws Exception {
        CountDownLatch start=new CountDownLatch(1);AtomicReference<Throwable> error=new AtomicReference<>();List<Thread> threads=new ArrayList<>();
        for(int i=0;i<12;i++){Thread t=new Thread(()->{try{start.await();action.run();}catch(Throwable e){error.compareAndSet(null,e);}});threads.add(t);t.start();}
        start.countDown();for(Thread t:threads)t.join(5000);for(Thread t:threads)check(!t.isAlive(),"No deadlock");
        if(error.get()!=null)throw new AssertionError("Concurrent failure",error.get());
    }
    private static void concurrent()throws Exception {
        Owner p=owner(21);OngGohan n=npc(0);
        p.inventory.gem=9_999_999;
        race(()->n.confirmMenu(p,0));check(p.inventory.gem==19_999_999,"Concurrent gem award once");
        race(()->n.confirmMenu(p,0));check(p.inventory.gem==19_999_999,"Concurrent replay above threshold gives no reward");
        int before=created.get();race(()->n.confirmMenu(p,1));check(p.pet!=null && created.get()==before+1,"Concurrent pet award exactly once");Pet first=p.pet;
        race(()->n.confirmMenu(p,1));check(p.pet==first && first.master==p && created.get()==before+1,"Concurrent replay retains original pet");
    }
    private static void guards()throws Exception {
        Owner p=owner(22);OngGohan n=npc(0);n.openBaseMenu(p);n.confirmMenu(p,0);n.confirmMenu(p,1);n.confirmMenu(p,2);
        check(p.menus.isEmpty() && p.forms.isEmpty() && p.inventory.gem==0 && p.pet==null,"Wrong map gives no support");
        p=owner(21);p.idMark.setIndexMenu(123);n.confirmMenu(p,0);n.confirmMenu(p,1);n.confirmMenu(p,2);check(p.forms.isEmpty() && p.inventory.gem==0 && p.pet==null,"Wrong menu cannot claim");
        p.idMark.setIndexMenu(MENU);for(int select:new int[]{-1,3,4,255})n.confirmMenu(p,select);
        check(p.forms.isEmpty() && p.inventory.gem==0 && p.pet==null,"Invalid or close selection gives no award");
        Owner outside=owner(100);OngGohan other=new OngGohan(100,1,100,100,0,0);other.openBaseMenu(outside);other.confirmMenu(outside,0);other.confirmMenu(outside,1);other.confirmMenu(outside,2);
        check(outside.menus.isEmpty() && outside.forms.isEmpty() && outside.inventory.gem==0 && outside.pet==null,"Duplicate NPC template outside houses cannot grant");
    }
    private static void giftcode() throws Exception {
        List<String> received = new ArrayList<>();
        List<Player> recipients = new ArrayList<>();
        replace(GiftCodeService.class, "INSTANCE", new GiftCodeService(null, p -> true) {
            @Override public void giftCode(Player p, String code) {
                received.add(code); recipients.add(p);
            }
        });
        for(int planet=0;planet<3;planet++) {
            Owner p=owner(21+planet);OngGohan n=npc(planet);
            n.confirmMenu(p,2);
            check(p.forms.size()==1,"Every home opens giftcode input");
            check(p.idMark.getTypeInput()==Input.GIFT_CODE,"Existing giftcode input route selected");
            DataInputStream form=new DataInputStream(new ByteArrayInputStream(p.forms.get(0)));
            check(form.readUTF().equals("Giftcode"),"Existing form title");
            check(form.readByte()==1,"One code field");
            check(form.readUTF().equals("Gift-code"),"Existing code field");
            check(form.readByte()==Input.ANY && form.available()==0,"Text input packet complete");
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            DataOutputStream out=new DataOutputStream(bytes);
            String code="HOME-TEST-"+planet;out.writeByte(1);out.writeUTF(code);
            Message submission=new Message((byte)-125,bytes.toByteArray());
            try { Input.gI().doInput(p,submission); } finally { submission.cleanup(); }
            check(received.size()==planet+1 && received.get(planet).equals(code)
                    && recipients.get(planet)==p,"Submission reaches existing service for correct player");
            n.confirmMenu(p,3);check(p.forms.size()==1,"Close does not reopen input");
            check(p.inventory.gem==0 && p.inventory.gold==123 && p.inventory.ruby==456
                    && p.pet==null,"Opening form does not grant items or currency");
        }
    }
}
