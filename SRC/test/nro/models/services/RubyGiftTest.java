package nro.models.services;

/** Installed-JAR regression: the transfer feature must exist with integer-safe quotes. */
public final class RubyGiftTest {
 public static void main(String[] args) throws Exception {
  Class<?> service;
  try { service=Class.forName("nro.models.services.RubyGiftService"); }
  catch(ClassNotFoundException missing) { throw new AssertionError("Atomic ruby gift service is missing",missing); }
  for(int amount:new int[]{0,1,2,9,10,11,100,101,Integer.MAX_VALUE}) {
   int fee=(int)service.getMethod("fee",int.class).invoke(null,amount);
   int net=(int)service.getMethod("net",int.class).invoke(null,amount);
   if(fee!=(amount+9L)/10 || net!=amount-fee)throw new AssertionError("Exact ceiling fee for "+amount);
  }
  System.out.println("RubyGiftTest quote PASS");
 }
}
