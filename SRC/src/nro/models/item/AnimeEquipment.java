package nro.models.item;

import java.util.*;
import nro.models.player.Player;
import nro.models.boss.Boss;
import nro.models.services.ItemService;

/** Fixed anime stats; only a fresh shop purchase rolls a duration. */
public final class AnimeEquipment {
    private AnimeEquipment() {}
    private static final Map<Integer,int[][]> PROFILES=Map.of(
        2019,new int[][]{{50,13},{77,13},{103,13},{204,13},{14,25},{30,0}},
        2026,new int[][]{{77,25},{6,50000},{94,30},{30,0}},
        2027,new int[][]{{50,25},{0,30000},{14,15},{30,0}},
        2030,new int[][]{{50,20},{77,20},{103,20},{95,20},{96,20},{14,40},{30,0}},
        2039,new int[][]{{50,22},{77,22},{103,22},{101,100},{160,70},{14,10},{30,0}});
    public static boolean contains(int id){return PROFILES.containsKey(id);}
    public static void complete(Item item){
        if(item==null||item.template==null||!contains(item.template.id))return;
        if(item.itemOptions==null)item.itemOptions=new ArrayList<>();
        for(int[] v:PROFILES.get((int)item.template.id)){
            Item.ItemOption saved=null;
            for(var o:item.itemOptions)if(o!=null&&o.optionTemplate!=null&&o.optionTemplate.id==v[0])saved=o;
            final int id=v[0];final Item.ItemOption keep=saved;
            item.itemOptions.removeIf(o->o!=null&&o.optionTemplate!=null&&o.optionTemplate.id==id&&o!=keep);
            if(saved==null)item.itemOptions.add(new Item.ItemOption(id,v[1]));
            else if(id==30)saved.param=0;
        }
    }
    public static void freshPurchase(Item item,Random rng){
        if(item==null||item.template==null||!contains(item.template.id))return;
        // Shop preview and restored items never call this method.
        item.itemOptions.clear();complete(item);
        if(rng.nextInt(100)>=20)item.itemOptions.add(new Item.ItemOption(93,7+rng.nextInt(24)));
        item.createTime=System.currentTimeMillis();item.expire=0;
    }
    private static boolean worn(Player p,int slot,int id,long now){
        if(p.inventory==null||p.inventory.itemsBody==null||p.inventory.itemsBody.size()<=slot)return false;
        Item it=p.inventory.itemsBody.get(slot);
        return it!=null&&it.template!=null&&it.template.id==id&&it.quantity>0
            &&(it.expire<=0||it.expire>now)&&!ItemService.gI().isEquippedOutOfDateTime(it,now);
    }
    public static boolean paired(Player p){
        long now=System.currentTimeMillis();
        return (worn(p,5,2027,now)&&worn(p,8,2030,now))
            ||(worn(p,5,2026,now)&&worn(p,7,2019,now));
    }
    public static int bossDamage(int damage,Player target,int percent){
        if(!(target instanceof Boss)||percent<=0||damage<=0)return damage;
        return (int)Math.min(Integer.MAX_VALUE,(long)damage*(100L+percent)/100L);
    }
}
