package nro.models.services;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import nro.models.boss.Boss;
import nro.models.item.Item;
import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.utils.Util;

/** One rare-loot callback per boss life; pet kills belong to the master. */
public final class MabuRareLoot {
    private boolean granted;

    public boolean drop(Boss boss, Player killer, int eggRate, int bottleRate) {
        return drop(boss,killer,eggRate,bottleRate,Util::nextInt);
    }

    synchronized boolean drop(Boss boss, Player killer, int eggRate, int bottleRate, IntUnaryOperator randomBounded) {
        Player owner = MiniBossPinkCapsuleDrop.creditedPlayer(killer);
        if (granted || owner == null || boss == null || boss.zone == null || boss.zone.map == null
                || boss.zone.map.mapWidth <= 0 || boss.location == null) return false;
        if (eggRate < 0 || eggRate > 100 || bottleRate < 0 || bottleRate > 100)
            throw new IllegalArgumentException("drop rates must be between zero and one hundred");
        // Freeze eligibility before either roll or any publication/pickup can introduce an egg.
        boolean bottleEligible = owner.mabuEgg != null || owner.pet != null && owner.pet.typePet == 1
                || owner.inventory != null && (hasEgg(owner.inventory.itemsBag) || hasEgg(owner.inventory.itemsBox));
        boolean egg = roll(randomBounded) < eggRate;
        boolean bottle = roll(randomBounded) < bottleRate && bottleEligible;
        ItemService items = ItemService.gI();
        if (egg && items.getTemplate(568) == null || bottle && (items.getTemplate(1795) == null
                || items.getItemOptionTemplate(250) == null)) return false;
        Zone zone = boss.zone;
        List<Reward> rewards = new ArrayList<>();
        if (egg) rewards.add(new Reward(568,List.of()));
        if (bottle) rewards.add(new Reward(1795,List.of(new Item.ItemOption(250,0))));
        for (int i=0;i<rewards.size();i++) {
            Reward reward = rewards.get(i);
            long intendedX = (long)boss.location.x + (i-rewards.size()/2)*20L;
            reward.x = (int)Math.max(0,Math.min(zone.map.mapWidth-1L,intendedX));
            reward.y = zone.map.yPhysicInTop(reward.x,boss.location.y-24);
            if (reward.y < 0) return false;
        }
        granted = true;
        for (Reward reward : rewards) {
            ItemMap drop = new ItemMap(zone,reward.id,1,reward.x,reward.y,owner.id,true,reward.options);
            try { Service.gI().dropItemMap(zone,drop); }
            catch (Exception packetFailure) { /* Already published; never duplicate after packet failure. */ }
        }
        return true;
    }

    private static boolean hasEgg(List<Item> items) {
        if (items == null) return false;
        for (Item item : items)
            if (item != null && item.template != null && item.template.id == 568 && item.quantity > 0) return true;
        return false;
    }
    private static int roll(IntUnaryOperator randomBounded) {
        int value = randomBounded.applyAsInt(100);
        if (value < 0 || value >= 100) throw new IllegalArgumentException("random value outside bound 100");
        return value;
    }
    public synchronized void reset() { granted=false; }
    private static final class Reward {
        final int id; final List<Item.ItemOption> options; int x,y;
        Reward(int id,List<Item.ItemOption> options) { this.id=id;this.options=options; }
    }
}
