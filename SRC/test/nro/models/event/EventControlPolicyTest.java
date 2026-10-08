package nro.models.event;
import java.lang.reflect.*;
import java.util.*;
public final class EventControlPolicyTest {
 static void check(boolean ok,String text){if(!ok)throw new AssertionError(text);}
 public static void main(String[]args)throws Exception{
  Class<?> policy;try{policy=Class.forName("nro.models.event.EventItemPolicy");}catch(ClassNotFoundException e){throw new AssertionError("Event consume/keep policy is missing",e);}
  Method publish=policy.getMethod("publish",Map.class),blocked=policy.getMethod("isBlocked",int.class);
  publish.invoke(null,Map.of("hung_vuong",true,"summer",true));
  check(!(boolean)blocked.invoke(null,1220),"Enabled event admits its materials");
  publish.invoke(null,Map.of("hung_vuong",false,"summer",false));
  check((boolean)blocked.invoke(null,1220)&&(boolean)blocked.invoke(null,1612),"OFF blocks exclusive materials");
  check(!(boolean)blocked.invoke(null,1776)&&!(boolean)blocked.invoke(null,1777),"Gift boxes remain usable after event OFF");
  check(!(boolean)blocked.invoke(null,0)&&!(boolean)blocked.invoke(null,999),"Normal and unclassified items stay intact");
  Class<?> catalog=Class.forName("nro.models.event.EventCatalog");List<?> rows=(List<?>)catalog.getMethod("all").invoke(null);
  check(rows.size()==10,"All ten named themes are visible, including unsupported");
  Set<Integer> consumed=new HashSet<>(),kept=new HashSet<>();
  for(Object row:rows){for(int id:(int[])row.getClass().getMethod("consumables").invoke(row))check(consumed.add(id),"No ambiguous consume ownership");for(int id:(int[])row.getClass().getMethod("keptRewards").invoke(row))kept.add(id);}
  check(Collections.disjoint(consumed,kept),"A kept reward cannot be deleted by another theme");
  System.out.println("EVENT_CONTROL_POLICY PASS");
 }
}
