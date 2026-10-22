package nro.models.server;

import java.lang.reflect.*;
import java.util.concurrent.atomic.*;
import nro.models.network.MySession;
import nro.models.player.Player;
import nro.models.services.LiveSyncCommandGate;
import nro.models.services.ServerMaintenanceGate;

/** Dependency-injected logout tests; never starts a worker or connects to SQL. */
public final class ClientLogoutRetentionTest {
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    @SuppressWarnings("unchecked") static <T> T blank(Class<T> type) throws Exception {
        Class<?> unsafe = Class.forName("sun.misc.Unsafe"); Field field = unsafe.getDeclaredField("theUnsafe"); field.setAccessible(true);
        return (T) unsafe.getMethod("allocateInstance", Class.class).invoke(field.get(null), type);
    }
    public static void main(String[] args) throws Exception {
        Class<?> actions;
        try { actions = Class.forName("nro.models.server.Client$LogoutActions"); }
        catch (ClassNotFoundException missing) { throw new AssertionError("Client lacks checked retained logout lifecycle", missing); }
        boolean[] saveReady = {false}, metadataReady = {false};
        AtomicInteger saves = new AtomicInteger(), disposes = new AtomicInteger(), disconnects = new AtomicInteger(),
                transportDisconnects = new AtomicInteger(), cleanups = new AtomicInteger();
        Object implementation = Proxy.newProxyInstance(actions.getClassLoader(), new Class<?>[]{actions}, (proxy, method, values) -> {
            switch (method.getName()) {
                case "cleanup": check(!Thread.holdsLock(values[0]), "cleanup/trade cancellation holds player monitor"); cleanups.incrementAndGet(); return null;
                case "savePlayer": saves.incrementAndGet(); return saveReady[0];
                case "saveAccount": return metadataReady[0];
                case "dispose": disposes.incrementAndGet(); return null;
                case "disconnect": disconnects.incrementAndGet(); return null;
                case "disconnectTransport": transportDisconnects.incrementAndGet(); return null;
                default: throw new AssertionError(method.getName());
            }
        });
        Constructor<Client> constructor = Client.class.getDeclaredConstructor(actions); constructor.setAccessible(true);
        Client client = constructor.newInstance(implementation);
        MySession session = blank(MySession.class); Player player = blank(Player.class);
        session.userId = 871234; session.joinedGame = true; session.player = player;
        player.session = session; player.id = 871234; player.name = "retention-test";
        client.put(player);
        ServerMaintenanceGate.pause();
        try {
            Client.class.getMethod("disconnectSessionsForWebMaintenance").invoke(client);
            Client.class.getMethod("disconnectSessionsForWebMaintenance").invoke(client);
            check(transportDisconnects.get() == 1, "maintenance closes the client transport immediately");
            Client.class.getMethod("handleSessionDisconnect", MySession.class).invoke(client, session);
            check(cleanups.get() == 0 && saves.get() == 0, "collector callback defers player cleanup and save");
            try (var ignored = ServerMaintenanceGate.freeze(java.time.Duration.ofSeconds(1))) {
                try {
                    client.drainForWebMaintenance();
                    throw new AssertionError("maintenance must retain an unsaved logout");
                } catch (IllegalStateException expected) { }
            }
            check(cleanups.get() == 1 && saves.get() > 0 && disconnects.get() == 1,
                    "barrier begins checked logout after immediate transport close");
            Client.class.getMethod("handleSessionDisconnect", MySession.class).invoke(client, session);
            check(cleanups.get() == 1 && disconnects.get() == 1, "late duplicate callback does not repeat logout");
        } finally { ServerMaintenanceGate.resume(); }
        check(disposes.get() == 0 && !player.liveLogoutCompleted, "failed player save disposed snapshot");
        check((boolean)Client.class.getMethod("hasUnsavedLogoutForAccount", int.class).invoke(null, session.userId), "failed save did not fence login");
        check(LiveSyncCommandGate.isRetiringAccount(session.userId), "retirement was released after failed save");
        client.kickSession(session); check(cleanups.get() == 1 && disconnects.get() == 1, "duplicate logout repeated cleanup");
        saveReady[0] = true; Client.class.getMethod("retryUnsavedLogouts").invoke(client);
        check(disposes.get() == 0 && !player.liveLogoutCompleted, "account metadata failure was ignored");
        metadataReady[0] = true; Client.class.getMethod("retryUnsavedLogouts").invoke(client);
        check(disposes.get() == 1 && player.liveLogoutCompleted, "durable logout did not complete exactly once");
        check(!(boolean)Client.class.getMethod("hasUnsavedLogouts").invoke(client), "completed logout retained");
        check(!LiveSyncCommandGate.isRetiringAccount(session.userId), "successful retry left login fenced");
        Client.class.getMethod("retryUnsavedLogouts").invoke(client);
        check(disposes.get() == 1 && cleanups.get() == 1, "successful retry repeated dispose/cleanup");
        System.out.println("PASS checked logout retains player and metadata failures, retries once, fences reconnect");
    }
}
