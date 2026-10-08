package nro.models.boss.Broly;

import java.util.IdentityHashMap;
import nro.models.boss.Boss;
import nro.models.map.Zone;

/** Short ownership operations only: never invoke gameplay callbacks under this lock. */
public final class BrolyZoneCoordinator {
    public static final class Lease {
        public final Zone zone;
        public final Object owner;
        public final long generation;
        private Lease(Zone zone, Object owner, long generation) {
            this.zone=zone;this.owner=owner;this.generation=generation;
        }
    }
    private final IdentityHashMap<Zone,Lease> slots=new IdentityHashMap<>();
    private long generation;

    public synchronized Lease reserve(Zone zone, Object owner) {
        if(zone==null||owner==null)return null;
        Lease current=slots.get(zone);
        if(current!=null)return current.owner==owner?current:null;
        for (var boss : zone.getBosses()) if(boss!=owner)return null;
        Lease lease=new Lease(zone,owner,++generation);slots.put(zone,lease);return lease;
    }
    public synchronized Lease transfer(Lease expected,Boss next) {
        if(expected==null||next==null||slots.get(expected.zone)!=expected)return null;
        Lease lease=new Lease(expected.zone,next,++generation);slots.put(expected.zone,lease);return lease;
    }
    public synchronized boolean owns(Lease lease) {
        return lease!=null&&slots.get(lease.zone)==lease;
    }
    public synchronized void release(Lease lease) {
        if(owns(lease))slots.remove(lease.zone);
    }
    public synchronized boolean isReserved(Zone zone) {return slots.containsKey(zone);}
    public synchronized boolean tryReserveExternal(Zone zone,Object owner) {return reserve(zone,owner)!=null;}
    public synchronized void releaseExternal(Zone zone,Object owner) {
        Lease current=slots.get(zone);if(current!=null&&current.owner==owner)slots.remove(zone);
    }
}
