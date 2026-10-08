package nro.models.services;
import nro.models.player.Player;
import nro.models.player.Pet;

/** Regular gameplay must wait while a web commit is being reconciled/published. */
public final class LiveSyncMutationGate {
    private LiveSyncMutationGate() {}
    public static Player owner(Player player){return player instanceof Pet pet&&pet.master!=null?pet.master:player;}
    public static boolean blocked(Player player) {
        Player owner=owner(player);
        return owner!=null&&(LiveSyncCommandGate.hasPendingId(owner.id)
                ||nro.models.event.EventCleanupService.pending(owner.id)
                ||GiftCodeService.hasPendingId(owner.id)
                ||(owner.session!=null&&GiftCodeService.hasPendingAccount(owner.session.userId))
                ||(owner.session!=null&&LiveSyncCommandGate.hasPendingAccount(owner.session.userId)));
    }
}
