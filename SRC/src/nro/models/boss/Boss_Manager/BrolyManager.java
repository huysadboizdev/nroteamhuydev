package nro.models.boss.Boss_Manager;

public class BrolyManager extends BossManager {
    private final nro.models.boss.Broly.BrolyZoneCoordinator zones = new nro.models.boss.Broly.BrolyZoneCoordinator();
    public nro.models.boss.Broly.BrolyZoneCoordinator zones() { return zones; }

    // Boss's constructor calls addBoss before subclass fields are initialized.
    @Override public void addBoss(nro.models.boss.Boss boss) {
        if (!(boss instanceof nro.models.boss.Broly.Broly)
                && !(boss instanceof nro.models.boss.Broly.SuperBroly)) super.addBoss(boss);
    }
    public void publish(nro.models.boss.Boss boss) { if(!bosses.contains(boss))super.addBoss(boss); }
    /** Recovery for instances already visible when a lifecycle migration starts. */
    public void reconcileExistingZones() {
        var grouped=new java.util.IdentityHashMap<nro.models.map.Zone,java.util.List<nro.models.boss.Boss>>();
        for(var boss:bosses)if(boss.zone!=null && (boss instanceof nro.models.boss.Broly.Broly || boss instanceof nro.models.boss.Broly.SuperBroly))
            grouped.computeIfAbsent(boss.zone,z->new java.util.ArrayList<>()).add(boss);
        for(var entry:grouped.entrySet()) {
            var candidates=entry.getValue();
            var keeper=candidates.stream().filter(b->b instanceof nro.models.boss.Broly.SuperBroly && !b.isDie()).findFirst().orElse(candidates.get(0));
            for(var boss:candidates)if(boss!=keeper) {
                boss.leaveMap();
                nro.models.utils.Logger.warningln("Broly duplicate retired id="+boss.id+" map="+entry.getKey().map.mapId+" zone="+entry.getKey().zoneId+" keeper="+keeper.id);
            }
            if(keeper instanceof nro.models.boss.Broly.Broly normal)normal.claimExistingZone();
            else if(keeper instanceof nro.models.boss.Broly.SuperBroly superBroly)superBroly.claimExistingZone();
        }
    }
    @Override public void run() {reconcileExistingZones();super.run();}

    private static BrolyManager instance;

    public static synchronized BrolyManager gI() {
        if (instance == null) {
            instance = new BrolyManager();
        }
        return instance;
    }

}
