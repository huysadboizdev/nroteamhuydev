package nro.models.services;
import java.lang.reflect.*;
public class CommesonStateTest {
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
 public static void main(String[] args)throws Exception{
  Class<?> c;
  try{c=Class.forName("nro.models.commeson.CommesonAttempt");}catch(ClassNotFoundException e){throw new AssertionError("Durable Commeson state machine is missing");}
  try{Class enumType=Class.forName("nro.models.commeson.CommesonAttempt$State");java.lang.Enum.valueOf(enumType,"RESERVED");}catch(IllegalArgumentException e){throw new AssertionError("Durable pre-start RESERVED phase is missing");}
  Method day=c.getMethod("day",long.class);
  long midnight=java.time.Instant.parse("2026-10-05T17:00:00Z").toEpochMilli();
  check(!day.invoke(null,midnight-1).equals(day.invoke(null,midnight)),"UTC+7 midnight resets character day");
  Object a=c.getConstructor(String.class,long.class,long.class).newInstance("match",7L,1000L);
  Method win=c.getMethod("win",long.class),pick=c.getMethod("pick",long.class),expire=c.getMethod("due",long.class);
  check(!(boolean)expire.invoke(a,300999L),"five minutes has not elapsed");
  check((boolean)expire.invoke(a,301000L),"five minute exact boundary");
  check((boolean)win.invoke(a,5000L),"first win");
  check(!(boolean)win.invoke(a,6000L),"duplicate win cannot drop another bottle");
  check(!(boolean)expire.invoke(a,64999L),"full sixty seconds to pick");
  check((boolean)pick.invoke(a,64000L),"pickup before expiry");
  check(!(boolean)pick.invoke(a,64001L),"one pickup only");
  check(!(boolean)expire.invoke(a,123999L),"fresh sixty seconds after pickup");
  check((boolean)expire.invoke(a,124000L),"return exact boundary");
  Object b=c.getConstructor(String.class,long.class,long.class).newInstance("late",8L,1000L);
  win.invoke(b,5000L);
  check(!(boolean)pick.invoke(b,65000L),"pickup at despawn deadline denied");
  System.out.println("PASS CommesonState: timezone, fight limit, one reward, pickup deadline, fresh return timer");
 }
}
