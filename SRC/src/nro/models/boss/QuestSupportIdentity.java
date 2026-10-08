package nro.models.boss;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Construction context also covers companions automatically constructed by Boss. */
public final class QuestSupportIdentity {
    private static final AtomicInteger NEXT_ID = new AtomicInteger(-900_000_000);
    private static final ThreadLocal<List<Boss>> CONSTRUCTING = new ThreadLocal<>();
    private static final Set<Integer> ALLOWED = Set.of(BossID.TIEU_DOI_TRUONG, BossID.SO_4,
            BossID.SO_3, BossID.SO_2, BossID.SO_1, BossID.FIDE, BossID.DR_KORE,
            BossID.ANDROID_19, BossID.ANDROID_14, BossID.ANDROID_15, BossID.ANDROID_13,
            BossID.KING_KONG, BossID.POC, BossID.PIC, BossID.XEN_BO_HUNG);
    public static boolean supports(int id) { return ALLOWED.contains(id); }
    public static void begin() {
        if (CONSTRUCTING.get() != null) throw new IllegalStateException("Nested support construction");
        CONSTRUCTING.set(new ArrayList<>());
    }
    public static List<Boss> constructingMembers() { return List.copyOf(CONSTRUCTING.get()); }
    public static void end() { CONSTRUCTING.remove(); }
    public static void initialize(Boss boss, int semanticId) {
        List<Boss> members = CONSTRUCTING.get();
        if (members == null) return;
        if (!supports(semanticId)) throw new IllegalArgumentException("Unsupported quest boss " + semanticId);
        int id = NEXT_ID.getAndIncrement();
        if (id >= -100_000_000) throw new IllegalStateException("Support boss IDs exhausted");
        boss.setQuestSupportIdentity(semanticId, id);
        members.add(boss);
    }
    private QuestSupportIdentity() {}
}
