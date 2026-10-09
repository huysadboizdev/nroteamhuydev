package nro.models.services;
public final class BossAdminDropRedTest {
 public static void main(String[] args) throws Exception {
  try { Class.forName("nro.models.services.BossAdminDropService"); }
  catch (ClassNotFoundException missing) { throw new AssertionError("boss drop replacement engine is missing",missing); }
 }
}
