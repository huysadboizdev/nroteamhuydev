package nro.models.services;

import java.lang.reflect.*;
import java.util.*;
import nro.models.item.Item;
import nro.models.player.Player;
import nro.models.player_system.Template;
import nro.models.server.Manager;
import nro.models.services_func.UseItem;

/** Exercise actual gift opening, reward insertion, consumption and expiry boundaries offline. */
public final class GokuDayGiftTest {
    static int empty = 2;
    static boolean accept = true;
    static Item reward;
    static String message;
    static Method open;
    static Random random;
    static void check(boolean b, String s) { if (!b) throw new AssertionError(s); }
    static void replace(Class<?> c, String name, Object value) throws Exception {
        Field f=c.getDeclaredField(name); f.setAccessible(true); f.set(null,value);
    }
    static Item box(Player p, int id) {
        Item b=ItemService.gI().createNewItem((short)id,2); p.inventory.itemsBag.add(b); return b;
    }
    static long seed(int roll) {
        for(long n=0;n<1000000;n++) if(new Random(n).nextInt(1000)==roll) return n;
        throw new AssertionError("No seed for roll "+roll);
    }
    static void invoke(Player p, Item b, int roll) throws Exception {
        reward=null; message=null; random.setSeed(seed(roll)); open.invoke(UseItem.gI(),p,b);
    }
    public static void main(String[] args) throws Exception {
        for(int id=0;id<2001;id++) {
            Template.ItemTemplate t=new Template.ItemTemplate();t.id=(short)id;t.name="Item "+id;
            t.type=5;t.gender=3;t.iconID=(short)id;Manager.ITEM_TEMPLATES.add(t);
        }
        for(int id=0;id<256;id++) Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"Option #",0));
        replace(Service.class,"instance",new Service(){@Override public void sendThongBao(Player p,String s){message=s;}});
        replace(InventoryService.class,"I",new InventoryService(){
            @Override public int getCountEmptyBag(Player p){return empty;}
            @Override public boolean addItemBag(Player p,Item i){if(accept)reward=i;return accept;}
            @Override public void sendItemBags(Player p){}
        });
        Field f=UseItem.class.getDeclaredField("rand");f.setAccessible(true);random=(Random)f.get(null);
        open=UseItem.class.getDeclaredMethod("OpenHopQuaGokuDayVangNgoc",Player.class,Item.class);open.setAccessible(true);
        Set<Integer> pool=Set.of(1588,1589,1595,1587,1593,1590);
        for(int id:new int[]{1591,1594}) {
            for(int roll:new int[]{0,4,5,999}) {
                Player p=new Player();p.isPlayer=true;Item b=box(p,id);invoke(p,b,roll);
                check(reward!=null&&pool.contains((int)reward.template.id),"Original Goku reward pool");
                check(b.quantity==1,"Consume exactly one selected gift "+id);
                for(int option:new int[]{50,77,103})check(reward.getOptionById(option).param==20,"Preserve stats");
                check(reward.getOptionById(210).param==2,"Preserve hidden stat");
                if(roll<5) {
                    check(reward.getOptionById(93)==null,"Permanent reward must have no expiry option93, gift="+id+", roll="+roll);
                    check(!ItemService.gI().isEquippedOutOfDateTime(reward,reward.createTime+10L*86400000),"Permanent survives date change");
                } else {
                    check(reward.getOptionById(93)!=null&&reward.getOptionById(93).param==3,"Timed reward lasts 3 days");
                    check(ItemService.gI().isEquippedOutOfDateTime(reward,reward.createTime+4L*86400000),"Timed reward expires");
                }
            }
            Player p=new Player();Item b=box(p,id);accept=false;invoke(p,b,5);
            check(b.quantity==2&&reward==null,"Rejected reward must not consume gift");accept=true;
            empty=1;invoke(p,b,5);
            check(b.quantity==2&&reward==null&&message.contains("2 ô"),"Insufficient slots message must match two-slot rule");empty=2;
        }
        Player p=new Player();Item b=box(p,1591);int permanent=0;
        for(int roll=0;roll<1000;roll++) {
            b.quantity=2;invoke(p,b,roll);
            check(reward!=null&&b.quantity==1,"Every roll grants and consumes exactly once");
            if(reward.getOptionById(93)==null)permanent++;
            else check(reward.getOptionById(93).param==3,"Every nonpermanent roll lasts three days");
        }
        check(permanent==5,"Exactly five permanent outcomes per 1000 rolls");
        System.out.println("GOKU_DAY_GIFT PASS both boxes, permanent/timed boundaries, stats, consumption, expiry, rejected insertion, free-slot message");
    }
}
