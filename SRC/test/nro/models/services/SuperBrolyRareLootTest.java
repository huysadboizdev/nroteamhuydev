package nro.models.services;

import java.util.concurrent.atomic.AtomicInteger;
import nro.models.boss.Broly.SuperBroly;
import nro.models.player.*;
import static nro.models.services.PinkFixture.*;

public final class SuperBrolyRareLootTest {
    public static void main(String[] args)throws Exception {try{
        install();AtomicInteger created=new AtomicInteger();Player owner=player(7);
        replace(PetService.class,"instance",new PetService(){@Override public void createNormalPet(Player p,byte... limits){check(p==owner,"normal pet belongs to credited master");created.incrementAndGet();}});
        SuperBroly b=blank(SuperBroly.class);b.zone=zone();b.location=new Location();b.location.y=200;
        b.reward(null);
        Pet killer=blank(Pet.class);killer.isPet=true;killer.master=owner;b.reward(killer);
        b.reward(owner);check(created.get()==1,"null callback preserves valid callback and normal pet reward exactly once");
        check(b.zone.items.stream().allMatch(i->i.playerId==7&&keptUnchecked(i)),"rare loot credited and private");
        System.out.println("PASS SuperBrolyRareLoot");System.exit(0);
    }catch(Throwable t){t.printStackTrace();System.exit(1);}}
    static boolean keptUnchecked(nro.models.map.ItemMap i){return i.keepOwnerUntilDespawn;}
}
