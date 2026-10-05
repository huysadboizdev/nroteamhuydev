package nro.models.map;
import static nro.models.services.PinkFixture.*;
public final class NamekDropOwnershipTest {
    static class PublishingZone extends Zone {
        PublishingZone(nro.models.map.Map map){super(map,0,20);}
        @Override public synchronized void addItem(ItemMap item){try{check(kept(item)&&item.playerId==-777,"private flag and exact owner must exist before publication");}catch(Exception e){throw new RuntimeException(e);}super.addItem(item);}
    }
    static void publication()throws Exception{install();Zone z=new PublishingZone(zone().map);try{ItemMap.class.getConstructor(Zone.class,int.class,int.class,int.class,int.class,long.class,boolean.class).newInstance(z,432,1,50,150,-777L,true);}catch(NoSuchMethodException missing){throw new AssertionError("atomic private drop constructor missing");}}
    static void ownership()throws Exception{install();Zone z=zone();ItemMap d=new ItemMap(z,432,1,50,150,7);owner(d,true);d.createTime=System.currentTimeMillis()-46000;d.update();check(d.playerId==7,"killer-owned costume must remain private at 46 seconds");ItemMap old=new ItemMap(z,77,1,50,150,7);old.createTime=d.createTime;old.update();check(old.playerId==-1,"ordinary legacy drop becomes public");}
    static void pickup()throws Exception{install();Zone z=zone();var p=player(7);p.zone=z;ItemMap d=new ItemMap(z,432,1,50,150,999999993);owner(d,true);z.pickItem(p,d.itemMapId);check(!d.isPickedUp,"legacy alias cannot claim permanent killer ownership");d.playerId=-1;z.pickItem(p,d.itemMapId);check(!d.isPickedUp,"flagged public sentinel not accepted");d.playerId=7;z.pickItem(p,d.itemMapId);check(d.isPickedUp,"exact killer picks private drop");ItemMap legacy=new ItemMap(z,77,1,50,150,999999993);z.pickItem(p,legacy.itemMapId);check(legacy.isPickedUp,"legacy alias preserved");}
    static void copy()throws Exception{install();Zone z=zone();ItemMap d=new ItemMap(z,19,1,50,150,7);owner(d,true);ItemMap c=new ItemMap(d);check(kept(c)&&c.playerId==7,"copy preserves ownership flag");}
    static void expiry()throws Exception{install();Zone z=zone();ItemMap d=new ItemMap(z,19,1,50,150,7);owner(d,true);d.createTime=System.currentTimeMillis()-51000;d.update();check(!z.items.contains(d)&&d.itemTemplate==null,"private dragonball still despawns after 50 seconds");}
    public static void main(String[] a)throws Exception{switch(a[0]){case "ownership"->ownership();case "pickup"->pickup();case "copy"->copy();case "expiry"->expiry();case "publication"->publication();default->throw new AssertionError(a[0]);}System.out.println("PASS NamekOwnership "+a[0]);System.exit(0);}
}
