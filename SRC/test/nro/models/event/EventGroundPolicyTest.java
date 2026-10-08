package nro.models.event;
import java.lang.reflect.*;
import java.util.*;
import nro.models.map.ItemMap;
import nro.models.player_system.Template.ItemTemplate;
public final class EventGroundPolicyTest {
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
 static ItemMap item(int id)throws Exception{
  Class<?> type=Class.forName("sun.misc.Unsafe");Field f=type.getDeclaredField("theUnsafe");f.setAccessible(true);
  ItemMap item=(ItemMap)type.getMethod("allocateInstance",Class.class).invoke(f.get(null),ItemMap.class);item.itemTemplate=new ItemTemplate();item.itemTemplate.id=(short)id;return item;
 }
 @SuppressWarnings("unchecked") public static void main(String[]args)throws Exception{
  Method canPick;try{canPick=EventRuntimeService.class.getMethod("canPick",ItemMap.class);}catch(NoSuchMethodException e){throw new AssertionError("source-owned kept gift pickup gate missing",e);}
  Field field=EventRuntimeService.class.getDeclaredField("GROUND");field.setAccessible(true);Map<ItemMap,String> ground=(Map<ItemMap,String>)field.get(null);
  ItemMap eventGift=item(648),ordinaryGift=item(648),pumpkin=item(585);ground.put(eventGift,"christmas");
  try{
   EventItemPolicy.publish(Map.of("christmas",true,"halloween",true));
   check((Boolean)canPick.invoke(null,eventGift),"enabled source gift can be collected");
   EventItemPolicy.publish(Map.of());
   check(!(Boolean)canPick.invoke(null,eventGift),"closed source cannot grant unpicked kept gift");
   check((Boolean)canPick.invoke(null,ordinaryGift),"ordinary shared gift remains collectible");
   check(!(Boolean)canPick.invoke(null,pumpkin),"closed consumable mob drop denied");
   check(!EventItemPolicy.isBlocked(648),"already collected Christmas gift remains usable");
   Class<?> unsafeType=Class.forName("sun.misc.Unsafe");Field u=unsafeType.getDeclaredField("theUnsafe");u.setAccessible(true);
   nro.models.map.Map map=(nro.models.map.Map)unsafeType.getMethod("allocateInstance",Class.class).invoke(u.get(null),nro.models.map.Map.class);map.mapId=5;
   nro.models.map.Zone zone=new nro.models.map.Zone(map,0,20);
   ItemMap disabledSummer=item(1612);zone.addItem(disabledSummer);
   check(!zone.getItemsSnapshot().contains(disabledSummer),"OFF mob constructor cannot leave invisible consumable ground item");
   zone.addItem(ordinaryGift);check(zone.getItemsSnapshot().contains(ordinaryGift),"ordinary kept gift ground preserved");
   EventItemPolicy.publish(Map.of("summer",true));ItemMap activeSummer=item(1612);zone.addItem(activeSummer);
   check(zone.getItemsSnapshot().contains(activeSummer),"ON source can add consumable ground item");
   System.out.println("Event ground source policy PASS");
  }finally{ground.remove(eventGift);}
 }
}
