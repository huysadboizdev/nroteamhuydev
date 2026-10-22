package nro.models.services;

import nro.models.server.Client;

/** Runs under the gameplay write barrier, never from the SQL queue worker. */
final class ServerMaintenanceRuntime {
    private ServerMaintenanceRuntime() {}
    static void disconnectSessions() {
        Client.gI().disconnectSessionsForWebMaintenance();
    }
    static void drain() throws Exception {
        LiveSyncService.gI().drainForMaintenance();
        if (!AchievementService.gI().drainPending()) throw new IllegalStateException("Achievement claims pending");
        if (!GiftCodeService.gI().drainPending()) throw new IllegalStateException("Gift claims pending");
        if (!nro.models.services.shenron.SummonDragonNamek.reconcilePendingForMaintenance())
            throw new IllegalStateException("Namek wish pending");
        nro.models.event.EventCleanupService.drainOnline();
        nro.models.event.EventCleanupLogoutService.drain();
        if (!nro.models.event.EventCleanupLogoutService.empty()) throw new IllegalStateException("Event logout saves pending");
        Client.gI().drainForWebMaintenance();
        nro.models.event.EventCleanupLogoutService.drain();
        if (!nro.models.event.EventCleanupLogoutService.empty() || Client.gI().hasUnsavedLogouts())
            throw new IllegalStateException("Logout saves pending; maintenance remains closed");
        // Native event maintenance handles offline cleanup jobs and consign expiry.
        nro.models.event.EventCleanupService.maintenance();
        nro.models.event.EventCleanupLogoutService.drain();
        if (!nro.models.event.EventCleanupLogoutService.empty())
            throw new IllegalStateException("Event cleanup logout saves pending");
        // The clan save used by native maintenance must also report failure here.
        ClanService.gI().saveForMaintenance();
    }
}
