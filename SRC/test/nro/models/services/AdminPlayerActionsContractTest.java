package nro.models.services;
public class AdminPlayerActionsContractTest {
 public static void main(String[] args)throws Exception {
  try {Class<?> c=Class.forName("nro.models.services.AdminPlayerActions");c.getMethod("isOnline",nro.models.player.Player.class);}
  catch(ClassNotFoundException|NoSuchMethodException e){throw new AssertionError("missing safe online/ban/rename/currency action service",e);}
 }
}
