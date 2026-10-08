package nro.models.services;

import java.util.List;
import java.util.function.IntUnaryOperator;
import nro.models.boss.Boss;
import nro.models.item.Item;
import nro.models.item.ItemDefaultOptions;
import nro.models.item.SealedEquipment;
import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.utils.Util;

/** Shared equipment pools with an independently configured policy per boss life. */
public final class SealedBossEquipmentLoot {
    // Audited shop level9-12 snapshot: four levels of each planet piece, then common radars.
    private static final int[][] SHOP = {
        {230,231,232,233,242,243,244,245,254,255,256,257,266,267,268,269,278,279,280,281},
        {234,235,236,237,246,247,248,249,258,259,260,261,270,271,272,273,278,279,280,281},
        {238,239,240,241,250,251,252,253,262,263,264,265,274,275,276,277,278,279,280,281}
    };
    private static final int[][] DIVINE = {{555,556,562,563,561},{557,558,564,565,561},{559,560,566,567,561}};
    private boolean granted;
    private final int shopRate, ordinaryDivineRate;
    public SealedBossEquipmentLoot(){this(60,30);}
    public SealedBossEquipmentLoot(int shopRate,int ordinaryDivineRate){
        if(shopRate<0||ordinaryDivineRate<0||shopRate+ordinaryDivineRate>100)
            throw new IllegalArgumentException("equipment rates must total at most100");
        this.shopRate=shopRate;this.ordinaryDivineRate=ordinaryDivineRate;
    }

    /** Fully builds options before the caller publishes any ItemMap. */
    public static Item build(Player owner, IntUnaryOperator randomBounded) {
        return build(owner,randomBounded,60,30);
    }
    private static Item build(Player owner, IntUnaryOperator randomBounded,int shopRate,int ordinaryDivineRate) {
        if (owner == null || owner.gender < 0 || owner.gender > 2) return null;
        int category = roll(randomBounded,100);
        int[] pool = category < shopRate ? SHOP[owner.gender] : DIVINE[owner.gender];
        int id = pool[roll(randomBounded,pool.length)];
        boolean sealed = category < shopRate || category >= shopRate+ordinaryDivineRate;
        int seal = sealed ? 34 + roll(randomBounded,3) : -1;
        ItemService items = ItemService.gI();
        if (items.getTemplate(id) == null) return null;
        Item equipment = items.createNewItem((short)id);
        if (category < shopRate) {
            List<Item.ItemOption> shopOptions = items.getListOptionItemShop((short)id);
            if (shopOptions != null) for (Item.ItemOption option:shopOptions)
                if (option != null && option.optionTemplate != null) equipment.itemOptions.add(new Item.ItemOption(option));
            if (equipment.itemOptions.isEmpty()) return null;
        } else {
            // Divine shop rows carry custom boosted stats; ordinary drops use randDoTL's canonical base.
            equipment.itemOptions.removeIf(o -> o!=null && o.optionTemplate!=null && switch(o.optionTemplate.id) {
                case 0,6,7,14,22,23,27,28,47,77,103 -> true;
                default -> false;
            });
            divineBaseOptions(equipment);
        }
        ItemDefaultOptions.complete(equipment);
        // A shop profile must never turn this reward into an activation set or stack seal choices.
        equipment.itemOptions.removeIf(o -> o.optionTemplate.id>=34 && o.optionTemplate.id<=36
                || o.optionTemplate.id>=127 && o.optionTemplate.id<=135);
        if (sealed) {
            if (items.getItemOptionTemplate(seal)==null || items.getItemOptionTemplate(30)==null
                    || items.getItemOptionTemplate(87)==null) return null;
            equipment.itemOptions.add(new Item.ItemOption(seal,1));
            SealedEquipment.normalize(equipment);
        }
        for (Item.ItemOption option:equipment.itemOptions)
            if (option==null || option.optionTemplate==null) return null;
        return equipment;
    }

    public boolean drop(Boss boss, Player killer) { return drop(boss,killer,Util::nextInt); }

    synchronized boolean drop(Boss boss, Player killer, IntUnaryOperator randomBounded) {
        Player owner = MiniBossPinkCapsuleDrop.creditedPlayer(killer);
        if (granted || owner == null || boss == null || boss.zone == null || boss.zone.map == null
                || boss.zone.map.mapWidth <= 0 || boss.location == null) return false;
        Item equipment = build(owner,randomBounded,shopRate,ordinaryDivineRate);
        if (equipment == null) return false;
        Zone zone = boss.zone;
        int x = Math.max(0,Math.min(zone.map.mapWidth-1,boss.location.x));
        int y = zone.map.yPhysicInTop(x,boss.location.y-24);
        if (y < 0) return false;
        granted = true;
        ItemMap drop = new ItemMap(zone,equipment.template.id,1,x,y,owner.id,true,equipment.itemOptions);
        try { Service.gI().dropItemMap(zone,drop); }
        catch (Exception packetFailure) { /* Already in zone; never duplicate after packet failure. */ }
        return true;
    }

    private static int roll(IntUnaryOperator randomBounded,int bound) {
        int value = randomBounded.applyAsInt(bound);
        if (value < 0 || value >= bound) throw new IllegalArgumentException("random value outside bound " + bound);
        return value;
    }
    public synchronized void reset() { granted=false; }
    /** Existing randDoTL canonical100% stats, without its optional rarity boosts or random restrictions. */
    private static void divineBaseOptions(Item item) {
        switch(item.template.id) {
            case 555 -> baseOption(item,47,800);
            case 557 -> baseOption(item,47,850);
            case 559 -> baseOption(item,47,900);
            case 556 -> pants(item,52,2600);
            case 558 -> pants(item,50,2500);
            case 560 -> pants(item,48,2400);
            case 562 -> baseOption(item,0,4400);
            case 564 -> baseOption(item,0,4300);
            case 566 -> baseOption(item,0,4500);
            case 563 -> boots(item,48,2400);
            case 565 -> boots(item,50,2500);
            case 567 -> boots(item,46,6900);
            case 561 -> baseOption(item,14,14);
            default -> throw new IllegalArgumentException("unknown divine equipment "+item.template.id);
        }
    }
    private static void pants(Item item,int hp,int regen) {
        baseOption(item,22,hp);baseOption(item,27,regen);
    }
    private static void boots(Item item,int ki,int regen) {
        baseOption(item,23,ki);baseOption(item,28,regen);
    }
    private static void baseOption(Item item,int id,int param) {
        if (item.itemOptions.stream().noneMatch(o->o!=null && o.optionTemplate!=null && o.optionTemplate.id==id))
            item.itemOptions.add(new Item.ItemOption(id,param));
    }
}
