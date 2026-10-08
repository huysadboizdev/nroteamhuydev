package nro.models.boss;

import java.util.*;
import nro.models.boss.Boss_mini.*;
import nro.models.services.PinkFixture;

/** Entity IDs must identify individual bosses even when their maps overlap. */
public final class MiniBossIdentityTest {
    static Boss make(int type) throws Exception {
        return switch (type) {
            case 0 -> new AnTrom();
            case 1 -> new Odo();
            case 2 -> new MatTroi();
            default -> new SoiHecQuyn();
        };
    }
    public static void main(String[] args) throws Exception {
        PinkFixture.install();
        Set<Long> ids = Collections.synchronizedSet(new HashSet<>());
        if (args[0].equals("unique")) {
            for (int i = 0; i < 32; i++) {
                Boss b = make(i % 4);
                PinkFixture.check(b.id < 0, "network boss identity must remain negative");
                PinkFixture.check(b.id > -100_000_000, "mini boss must not be classified as a clone");
                PinkFixture.check(ids.add(b.id), "different instances must not share network identity: " + b.id);
            }
        } else if (args[0].equals("concurrent")) {
            List<Thread> workers = new ArrayList<>();
            List<Throwable> failures = Collections.synchronizedList(new ArrayList<>());
            for (int i = 0; i < 16; i++) {
                final int type = i % 4;
                Thread t = new Thread(() -> {
                    try {
                        for (int j = 0; j < 10; j++) {
                            Boss b = make(type);
                            PinkFixture.check(ids.add(b.id), "concurrent constructor duplicated identity");
                        }
                    } catch (Throwable failure) { failures.add(failure); }
                });
                workers.add(t); t.start();
            }
            for (Thread t : workers) t.join();
            PinkFixture.check(failures.isEmpty(), "concurrent identity failures: " + failures);
            PinkFixture.check(ids.size() == 160, "all concurrent entities retain distinct identities");
        } else throw new AssertionError("Unknown mode");
        System.out.println("PASS MiniBossIdentityTest " + args[0]);
    }
}
