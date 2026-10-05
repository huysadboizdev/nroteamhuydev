package nro.models.services;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player_system.Template;
import static nro.models.services.PinkFixture.*;

public final class RubyCapsuleTest {
    static final int TICKET = 2000;
    static Item.ItemOption option(int id, int param) {
        Template.ItemOptionTemplate t = new Template.ItemOptionTemplate();
        t.id=id; t.name="option #"; return new Item.ItemOption(t,param);
    }
    static void setup() throws Exception {
        install();
        replace(ItemService.class,"i",new ItemService(){
            @Override public Template.ItemTemplate getTemplate(int id){
                var t=template(id); t.isUpToUp=true; return t;
            }
            @Override public Template.ItemOptionTemplate getItemOptionTemplate(int id){return option(id,0).optionTemplate;}
        });
        replace(InventoryService.class,"I",new InventoryService(){@Override public void sendItemBags(Player p){}});
    }
    static Item empty(){return new Item();}
    static int tickets(Player p){return p.inventory.itemsBag.stream().filter(i->i.template!=null&&i.template.id==TICKET).mapToInt(i->i.quantity).sum();}
    static Item ticket(int n){Item i=ItemService.gI().createNewItem((short)TICKET,n);i.itemOptions.clear();i.itemOptions.add(option(30,1));return i;}
    static boolean open(Player p,Item i)throws Exception{return (boolean)Class.forName("nro.models.services.PinkCapsuleService").getMethod("open",Player.class,Item.class).invoke(null,p,i);}
    static void rewards()throws Exception{
        setup();Player p=player(800);p.inventory.gem=73;Item c=item(722,2);p.inventory.itemsBag.add(c);p.inventory.itemsBag.add(empty());
        check(open(p,c),"capsule opens");check(tickets(p)==1,"each capsule must grant one ruby gift ticket");check(p.inventory.gem==73&&p.inventory.ruby>=1&&p.inventory.ruby<=100,"only ruby balance credited");
        Item remaining=p.inventory.itemsBag.stream().filter(i->i.template!=null&&i.template.id==722).findFirst().get();
        check(open(p,remaining)&&tickets(p)==2,"second capsule merges a second ticket");
        Item t=p.inventory.itemsBag.stream().filter(i->i.template!=null&&i.template.id==TICKET).findFirst().get();
        check(t.itemOptions.stream().anyMatch(o->o.optionTemplate.id==30),"ticket cannot be traded");
        check(!open(p,remaining)&&tickets(p)==2,"spent capsule never replays");
        setup();p=player(801);c=item(722,1);p.inventory.itemsBag.add(c);
        final Player owner=p;final Item cap=c;
        replace(Service.class,"instance",new Service(){@Override public void sendMoney(Player x){throw new RuntimeException("packet");}@Override public void sendThongBao(Player x,String s){throw new RuntimeException("packet");}});
        check(open(owner,cap)&&tickets(owner)==1&&!open(owner,cap),"packet failure doesn't duplicate capsule reward");
    }
    static void capacity()throws Exception{
        setup();Player p=player(810);Item c=item(722,2);p.inventory.itemsBag.add(c);
        check(!open(p,c)&&c.quantity==2&&p.inventory.ruby==0&&tickets(p)==0,"full bag leaves all assets unchanged");
        c.quantity=1;check(open(p,c)&&tickets(p)==1&&p.inventory.itemsBag.size()==1,"last capsule slot can hold ticket");
        p=player(811);c=item(722,2);p.inventory.itemsBag.add(c);p.inventory.itemsBag.add(ticket(9));
        check(open(p,c)&&tickets(p)==10,"full bag can merge ticket");
        p=player(812);c=item(722,2);p.inventory.itemsBag.add(c);p.inventory.itemsBag.add(ticket(Integer.MAX_VALUE));
        check(!open(p,c)&&c.quantity==2&&p.inventory.ruby==0,"saturated ticket stack and full bag reject");
        p=player(813);c=item(722,1);p.inventory.itemsBag.add(c);p.inventory.ruby=Integer.MAX_VALUE;
        check(!open(p,c)&&c.quantity==1&&tickets(p)==0,"ruby overflow grants nothing");
        p.inventory.ruby=-1;check(!open(p,c)&&c.quantity==1,"negative ruby rejected");
    }
    static void normalize()throws Exception{
        setup();Item c=item(722,100);c.createTime=12345;c.itemOptions.add(option(30,1));c.itemOptions.add(option(97,30));
        check(InventoryService.gI().addItemList(new ArrayList<>(List.of(empty())),c),"grant old capsule");
        Item copy=ItemService.gI().copyItem(c);
        check(copy.itemOptions.stream().noneMatch(o->o.optionTemplate.id==97),"remove capsule reflect option");
        check(copy.itemOptions.stream().anyMatch(o->o.optionTemplate.id==30)&&copy.createTime==12345,"preserve remaining capsule metadata");
        Item equipment=item(719,1);equipment.itemOptions.add(option(97,30));
        check(ItemService.gI().copyItem(equipment).itemOptions.stream().anyMatch(o->o.optionTemplate.id==97),"other items retain reflection");
        Item t=ItemService.gI().createNewItem((short)TICKET);
        check(t.itemOptions.stream().anyMatch(o->o.optionTemplate.id==30),"all new ticket sources block trade");
    }
    static void concurrent()throws Exception{
        setup();Player p=player(820);Item c=item(722,1);p.inventory.itemsBag.add(c);
        ExecutorService pool=Executors.newFixedThreadPool(2);
        try{Future<Boolean> a=pool.submit(()->open(p,c));Future<Boolean>b=pool.submit(()->open(p,c));check(a.get(5,TimeUnit.SECONDS)^b.get(5,TimeUnit.SECONDS),"only one concurrent opening");check(tickets(p)==1&&c.quantity==0,"one ticket and one capsule consumption");}finally{pool.shutdownNow();}
    }
    public static void main(String[] a)throws Exception{
        try{switch(a[0]){case "rewards"->rewards();case "capacity"->capacity();case "normalize"->normalize();case "concurrent"->concurrent();default->throw new AssertionError(a[0]);}System.out.println("PASS RubyCapsule "+a[0]);System.exit(0);}catch(Throwable e){e.printStackTrace();System.exit(1);}
    }
}
