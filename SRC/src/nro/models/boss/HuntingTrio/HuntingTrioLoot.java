package nro.models.boss.HuntingTrio;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntUnaryOperator;
import nro.models.boss.Boss;
import nro.models.item.Item;
import nro.models.services.SealedBossEquipmentLoot;
import nro.models.map.ItemMap;
import nro.models.map.Zone;
import nro.models.player.Player;
import nro.models.services.ItemService;
import nro.models.services.MiniBossPinkCapsuleDrop;
import nro.models.services.Service;
import nro.models.utils.Util;

/** Own one instance per boss. Reset only when that boss starts a new life. */
public final class HuntingTrioLoot {
    private boolean granted;

    public boolean drop(Boss boss, Player killer) { return drop(boss, killer, Util::nextInt); }

    synchronized boolean drop(Boss boss, Player killer, IntUnaryOperator randomBounded) {
        Player owner = MiniBossPinkCapsuleDrop.creditedPlayer(killer);
        if (granted || owner == null || owner.gender < 0 || owner.gender > 2 || boss == null
                || boss.zone == null || boss.zone.map == null || boss.zone.map.mapWidth <= 0
                || boss.location == null) return false;
        Zone zone = boss.zone;
        Item equipment = SealedBossEquipmentLoot.build(owner,randomBounded);
        if (equipment == null) return false;
        int id = equipment.template.id;
        List<Reward> rewards = new ArrayList<>();
        rewards.add(new Reward(id,1));
        rewards.add(new Reward(189,10000+roll(randomBounded,20001)));
        rewards.add(new Reward(722,1+roll(randomBounded,3)));
        if (roll(randomBounded,100)<70) rewards.add(new Reward(220+roll(randomBounded,5),1+roll(randomBounded,2)));
        if (roll(randomBounded,100)<30) rewards.add(new Reward(14+roll(randomBounded,7),1));

        // Validate every template, option and terrain position before the constructor publishes any item.
        ItemService items = ItemService.gI();
        for (Reward reward:rewards) {
            if (items.getTemplate(reward.id)==null) return false;
        }
        for (int i=0;i<rewards.size();i++) {
            Reward reward=rewards.get(i);
            long intendedX=(long)boss.location.x+(i-rewards.size()/2)*20L;
            reward.x=(int)Math.max(0,Math.min(zone.map.mapWidth-1L,intendedX));
            reward.y=zone.map.yPhysicInTop(reward.x,boss.location.y-24);
            if (reward.y<0) return false;
        }
        granted=true;
        for (int i=0;i<rewards.size();i++) {
            Reward reward=rewards.get(i);
            ItemMap drop=new ItemMap(zone,reward.id,reward.quantity,reward.x,reward.y,owner.id,true,
                    i==0 ? equipment.itemOptions : List.of());
            try { Service.gI().dropItemMap(zone,drop); }
            catch (Exception packetFailure) { /* Item is already in the zone; never compensate with another drop. */ }
        }
        return true;
    }

    private static int roll(IntUnaryOperator randomBounded,int bound) {
        int value=randomBounded.applyAsInt(bound);
        if (value<0 || value>=bound) throw new IllegalArgumentException("random value outside bound "+bound);
        return value;
    }

    private static final class Reward {
        final int id,quantity;int x,y;
        Reward(int id,int quantity){this.id=id;this.quantity=quantity;}
    }
    public synchronized void reset() { granted=false; }
}
