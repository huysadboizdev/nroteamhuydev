package nro.models.services;

import java.util.List;
import java.util.function.IntUnaryOperator;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.map.service.MapService;
import nro.models.mob.Mob;
import nro.models.player.NewPet;
import nro.models.player.Pet;
import nro.models.player.Player;
import nro.models.utils.Util;

/** Fixed regional farm rates; unrelated luck and costume bonuses do not alter them. */
public final class EquipmentFarmDropService {
    /** A reserved death survives revival; its reward can only be delivered once. */
    public static final class DeathClaim {
        private final java.util.concurrent.atomic.AtomicBoolean delivered=new java.util.concurrent.atomic.AtomicBoolean();
        private boolean take() {return delivered.compareAndSet(false,true);}
    }
    private static final int[][] DIVINE={{555,556,562,563,561},{557,558,564,565,561},{559,560,566,567,561}};
    private EquipmentFarmDropService() { }

    public static List<ItemMap> rewards(Mob mob,Player killer,int x,int y) {
        return rewards(mob,killer,x,y,Util::nextInt,System.currentTimeMillis());
    }

    public static List<ItemMap> rewards(Mob mob,Player killer,int x,int y,DeathClaim claim) {
        return rewards(mob,killer,x,y,Util::nextInt,System.currentTimeMillis(),claim);
    }

    public static List<ItemMap> rewards(Mob mob,Player killer,int x,int y,IntUnaryOperator random,long now) {
        DeathClaim claim=mob==null?null:mob.claimEquipmentFarmDeath(mob.equipmentFarmGeneration());
        return rewards(mob,killer,x,y,random,now,claim);
    }

    public static List<ItemMap> rewards(Mob mob,Player killer,int x,int y,IntUnaryOperator random,long now,DeathClaim claim) {
        if(claim==null || !claim.take())return List.of();
        Player owner=killer instanceof Pet pet?pet.master:killer instanceof NewPet pet?pet.master:killer;
        if(owner==null || !owner.isPl() || owner.isBot || owner.isBoss || mob==null || mob.tempId==0
                || mob.isBigBoss() || mob.zone==null || mob.zone.map==null || owner.zone!=mob.zone) return List.of();
        synchronized(owner) {
            int map=mob.zone.map.mapId;
            boolean prison=MapService.gI().isMapNgucTu(map);
            int foodRate=MapService.gI().isMapCold(map)?60:MapService.gI().isMapTuongLai(map)?20:0;
            if(!(prison?fullSet(owner,true):foodRate>0 && fullSet(owner,false)))return List.of();
            if(!prison && random.applyAsInt(100)>=foodRate)return List.of();
            int quantity=1;
            if(prison && owner.itemTime!=null && owner.itemTime.isUseCoBonLa
                    && owner.itemTime.timeLengthCoBonLa>0 && now>=owner.itemTime.lastTimeUseCoBonLa
                    && now-owner.itemTime.lastTimeUseCoBonLa<owner.itemTime.timeLengthCoBonLa)quantity=20;
            int id=(prison?1066:663)+random.applyAsInt(5);
            return List.of(new ItemMap(mob.zone,id,quantity,x,y,owner.id));
        }
    }

    /** Read equipped slots directly, never mutate or count bag/chest gear. */
    private static boolean fullSet(Player owner,boolean destroyer) {
        if(owner.inventory==null || owner.inventory.itemsBody==null || owner.inventory.itemsBody.size()<5
                || owner.gender<0 || owner.gender>2)return false;
        for(int slot=0;slot<5;slot++) {
            Item item=owner.inventory.itemsBody.get(slot);
            if(!InventoryService.gI().isBillIngredientUsable(item) || item.quantity!=1
                    || item.template.id!=DIVINE[owner.gender][slot]+(destroyer?95:0)
                    || item.template.type!=slot || item.template.level!=(destroyer?14:13)
                    || item.template.gender!=(slot==4?3:owner.gender))return false;
        }
        return true;
    }
}
