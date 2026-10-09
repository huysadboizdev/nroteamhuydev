package nro.models.boss;

import java.lang.reflect.*;
import java.util.*;
import nro.models.boss.Boss_mini.SoiHecQuyn;
import nro.models.player.*;
import nro.models.item.Item;
import nro.models.services.*;
import nro.models.services_func.UseItem;
import nro.models.consts.BossStatus;
import static nro.models.services.PinkFixture.*;

public final class MiniWolfRegressionTest {
    public static class Wolf extends SoiHecQuyn {
        int deaths,rewards;
        public Wolf() throws Exception { super(); }
        @Override protected void setDie(Player p) { }
        @Override public void die(Player p) { deaths++; reward(p); bossStatus=BossStatus.DIE; }
        @Override public void rewardDefault(Player p) { rewards++; }
        @Override public void joinMapByZone(nro.models.map.Zone z) { zone=z; }
        @Override public void wakeupAnotherBossWhenAppear() { }
        @Override public boolean noticePublic() { return false; }
    }
    static final List<String> notices=new ArrayList<>();
    static Wolf wolf() throws Exception { Wolf b=blank(Wolf.class);b.id=BossID.SOI_HEC_QUYN1;b.nPoint=blank(NPoint.class);b.nPoint.hp=10000;b.zone=zone();b.location=new Location();b.location.x=100;b.location.y=100;b.bossStatus=BossStatus.ACTIVE;b.currentLevel=0;return b; }
    static Player owner() throws Exception { Player p=player(777);p.nPoint=blank(NPoint.class);p.nPoint.hp=100;p.idNRNM=-1;p.location=new Location();p.location.x=100;p.location.y=100;return p; }
    static Item bone(int quantity) { Item i=item(460,quantity);i.itemOptions=new ArrayList<>();return i; }
    static void setup() throws Exception { install();notices.clear();replace(Service.class,"instance",new Service(){@Override public void sendThongBao(Player p,String s){notices.add(s);}@Override public void chat(Player p,String s){}@Override public void dropItemMap(nro.models.map.Zone z,nro.models.map.ItemMap i){}}); }
    static void fight() throws Exception { setup();Wolf b=wolf();Player p=owner();check(b.injured(p,100,true,false)==100&&b.nPoint.hp==9900,"bone-free player can fight wolf");b.nPoint.hp=50;b.injured(p,100,true,false);check(b.deaths==1&&b.rewards==1,"bone-free player can kill wolf"); }
    static void bones() throws Exception { setup();Player p=owner();for(boolean box:new boolean[]{false,true}){Wolf b=wolf();Item bone=bone(1);(box?p.inventory.itemsBox:p.inventory.itemsBag).add(bone);check(b.injured(p,100,true,false)==0&&b.nPoint.hp==10000,"bone in bag or box prevents damage");check(!notices.isEmpty(),"bone attack supplies use instruction");(box?p.inventory.itemsBox:p.inventory.itemsBag).clear();}Wolf b=wolf();Item zero=bone(0);p.inventory.itemsBag.add(zero);check(b.injured(p,100,true,false)==100,"zero-quantity bone does not block combat");zero.quantity=1;zero.expire=1;check(b.injured(p,100,true,false)==100,"expired bone does not block combat"); }
    static void pet() throws Exception { setup();Wolf b=wolf();Player p=owner();p.inventory.itemsBox.add(bone(1));Pet pet=blank(Pet.class);pet.master=p;pet.idNRNM=-1;check(b.injured(pet,100,true,false)==0&&b.nPoint.hp==10000,"pet attacks use master's bone protection"); }
    static void invokeFeed(Player p,Item i) throws Exception { Method m=UseItem.class.getDeclaredMethod("XuongCho",Player.class,Item.class);m.setAccessible(true);m.invoke(UseItem.gI(),p,i); }
    static void feed() throws Exception { setup();final boolean[] full={true};replace(InventoryService.class,"I",new InventoryService(){@Override public int getCountEmptyBag(Player p){return full[0]?0:1;}@Override public boolean addItemBag(Player p,Item i){p.inventory.itemsBag.add(i);return true;}@Override public void subQuantityItemsBag(Player p,Item i,int n){i.quantity-=n;}@Override public void sendItemBags(Player p){}});Wolf b=wolf();Player p=owner();p.zone=b.zone;b.zone.getBosses().add(b);Item i=bone(2);p.inventory.itemsBag.add(i);invokeFeed(p,i);check(i.quantity==2&&!b.KiemTraNhatXuong(),"full bag preserves bone and wolf");full[0]=false;long start=System.nanoTime();invokeFeed(p,i);check((System.nanoTime()-start)/1000000<1000,"feeding does not block request for five seconds");check(i.quantity==1&&b.rewards==1&&b.KiemTraNhatXuong(),"feeding consumes one bone and rewards once");invokeFeed(p,i);check(i.quantity==1&&b.rewards==1,"repeat feeding cannot consume or reward twice");check(b.injured(p,100,true,false)==0,"fed wolf cannot be killed for duplicate reward"); }
    static void spawn() throws Exception { setup();Wolf b=wolf();b.zoneFinal=b.zone;b.NhatXuong();b.joinMap();check(!b.KiemTraNhatXuong(),"respawn clears fed flag");b.autoLeaveMap();check(b.bossStatus!=BossStatus.LEAVE_MAP,"zoneFinal wolf receives real lifetime timer"); }
    static void guards() throws Exception {
        setup(); final boolean[] failAdd={true};
        replace(InventoryService.class,"I",new InventoryService(){@Override public int getCountEmptyBag(Player p){return 1;}@Override public boolean addItemBag(Player p,Item i){return !failAdd[0];}@Override public void subQuantityItemsBag(Player p,Item i,int n){i.quantity-=n;}@Override public void sendItemBags(Player p){}});
        Wolf b=wolf();Player p=owner();p.zone=b.zone;b.zone.getBosses().add(b);Item i=bone(1);p.inventory.itemsBag.add(i);
        invokeFeed(p,i);check(i.quantity==1&&!b.KiemTraNhatXuong()&&b.rewards==0,"failed gift delivery leaves bone, wolf and reward intact");
        failAdd[0]=false;p.location.x=350;invokeFeed(p,i);check(i.quantity==1&&!b.KiemTraNhatXuong(),"cannot feed remote wolf");
        p.location.x=100;i.expire=1;invokeFeed(p,i);check(i.quantity==1&&!b.KiemTraNhatXuong(),"expired bone cannot feed wolf");
        i.expire=0;i.quantity=0;invokeFeed(p,i);check(!b.KiemTraNhatXuong(),"zero-quantity bone cannot feed wolf");
        i.quantity=1;b.zone.getBosses().clear();Boss arena=blank(Boss.class);arena.id=BossID.SOI_HEC_QUYN;arena.zone=b.zone;b.zone.getBosses().add(arena);invokeFeed(p,i);check(i.quantity==1,"same-ID arena boss is excluded without invalid cast");
    }
    static void concurrent() throws Exception {
        setup();replace(InventoryService.class,"I",new InventoryService(){@Override public int getCountEmptyBag(Player p){return 1;}@Override public boolean addItemBag(Player p,Item i){return true;}@Override public void subQuantityItemsBag(Player p,Item i,int n){i.quantity-=n;}@Override public void sendItemBags(Player p){}});
        Wolf b=wolf();Player p=owner(),q=owner();q.id=778;p.zone=q.zone=b.zone;b.zone.getBosses().add(b);Item first=bone(1),second=bone(1);p.inventory.itemsBag.add(first);q.inventory.itemsBag.add(second);
        var start=new java.util.concurrent.CountDownLatch(1);var failures=new java.util.concurrent.ConcurrentLinkedQueue<Throwable>();
        Thread one=new Thread(()->{try{start.await();invokeFeed(p,first);}catch(Throwable e){failures.add(e);}}),two=new Thread(()->{try{start.await();invokeFeed(q,second);}catch(Throwable e){failures.add(e);}});
        one.start();two.start();start.countDown();one.join(2000);two.join(2000);
        check(!one.isAlive()&&!two.isAlive()&&failures.isEmpty(),"concurrent feeding completes without lock failure");
        check(first.quantity+second.quantity==1&&b.rewards==1,"concurrent players consume exactly one bone and reward once");
    }
    public static void main(String[] a) throws Exception {switch(a[0]){case "fight"->fight();case "bones"->bones();case "pet"->pet();case "feed"->feed();case "spawn"->spawn();case "guards"->guards();case "concurrent"->concurrent();default->throw new AssertionError(a[0]);}System.out.println("PASS MiniWolf "+a[0]);}
}
