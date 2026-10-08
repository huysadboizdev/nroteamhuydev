package nro.models.services;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

/** Serializes login against commands; retains identities while a commit is unresolved. */
public final class LiveSyncCommandGate {
    public static final Object LIFECYCLE_LOCK=new Object();
    private static final Set<Long> players=ConcurrentHashMap.newKeySet();
    private static final Set<Integer> accounts=ConcurrentHashMap.newKeySet();
    private static final Set<Long> retiringPlayers=ConcurrentHashMap.newKeySet();
    private static final Set<Integer> retiringAccounts=ConcurrentHashMap.newKeySet();
    private LiveSyncCommandGate() {}
    public static boolean hasPendingId(long id){return players.contains(id);}
    public static boolean hasPendingAccount(int id){return accounts.contains(id);}
    public static boolean isRetiringId(long id){return retiringPlayers.contains(id);}
    public static boolean isRetiringAccount(int id){return retiringAccounts.contains(id);}
    public static void mark(long id,int account){if(id>0)players.add(id);if(account>0)accounts.add(account);}
    public static void clear(long id,int account){players.remove(id);accounts.remove(account);}
    public static void retire(long id,int account){retiringPlayers.add(id);retiringAccounts.add(account);}
    public static void retired(long id,int account){retiringPlayers.remove(id);retiringAccounts.remove(account);}
}
