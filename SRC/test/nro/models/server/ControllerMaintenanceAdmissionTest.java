package nro.models.server;

/** Static resource transfer remains available without holding gameplay admission. */
public final class ControllerMaintenanceAdmissionTest {
    public static void main(String[] args) {
        check(Controller.isMaintenanceExemptCommand((byte) -74), "resource transfer bypasses gameplay drain lock");
        check(!Controller.isMaintenanceExemptCommand((byte) -27), "login remains subject to maintenance admission");
        check(!Controller.isMaintenanceExemptCommand((byte) 0), "gameplay remains subject to maintenance admission");
        System.out.println("PASS maintenance admission exemptions");
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
