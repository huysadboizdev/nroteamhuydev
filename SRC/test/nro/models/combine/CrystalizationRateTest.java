package nro.models.combine;

import java.lang.reflect.Field;
import java.util.Random;
import nro.models.utils.Util;

/** Standalone regression test; does not start the server or connect to MySQL. */
public final class CrystalizationRateTest {
    public static void main(String[] args) throws Exception {
        float[] expected = {100f, 88.125f, 76.25f, 64.375f, 52.5f,
                40.625f, 28.75f, 16.875f, 5f};
        for (int currentStars = 0; currentStars < expected.length; currentStars++) {
            checkRate("upgrade to " + (currentStars + 1),
                    PhaLeHoaTrangBi.getRatio(currentStars), expected[currentStars]);
            checkRate("shared ratio " + currentStars,
                    CombineSystem.getRatioPhaLeHoa(currentStars), expected[currentStars]);
        }
        for (int invalid : new int[]{-1, 9, 10, Integer.MAX_VALUE}) {
            checkRate("invalid stars " + invalid, PhaLeHoaTrangBi.getRatio(invalid), 0f);
            checkRate("shared invalid stars " + invalid,
                    CombineSystem.getRatioPhaLeHoa(invalid), 0f);
        }
        if (CombineService.MAX_STAR_ITEM != 9) {
            throw new AssertionError("Maximum crystal stars must be 9");
        }
        checkRandomRolls();
        System.out.println("PASS: all 9 upgrade rates, boundaries, maximum stars and 4500 deterministic rolls");
    }

    private static void checkRate(String name, float actual, float expected) {
        if (actual != expected) {
            throw new AssertionError(name + ": expected " + expected + ", got " + actual);
        }
    }

    private static void checkRandomRolls() throws Exception {
        Field randomField = Util.class.getDeclaredField("rand");
        randomField.setAccessible(true);
        Random actualRandom = (Random) randomField.get(null);
        long[] successThresholds = {100_000, 88_125, 76_250, 64_375, 52_500,
                40_625, 28_750, 16_875, 5_000};
        for (int currentStars = 0; currentStars < successThresholds.length; currentStars++) {
            long seed = 20261002L + currentStars;
            actualRandom.setSeed(seed);
            Random expectedRandom = new Random(seed);
            for (int attempt = 0; attempt < 500; attempt++) {
                boolean expected = expectedRandom.nextLong(100_000L) < successThresholds[currentStars];
                if (PhaLeHoaTrangBi.rollSuccess(currentStars) != expected) {
                    throw new AssertionError("Incorrect random odds at stars " + currentStars
                            + ", attempt " + attempt);
                }
            }
        }
        if (PhaLeHoaTrangBi.rollSuccess(-1) || PhaLeHoaTrangBi.rollSuccess(9)) {
            throw new AssertionError("Invalid star levels must never succeed");
        }
    }
}
