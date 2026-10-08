package nro.models.services;

import java.time.*;
import java.time.temporal.TemporalAdjusters;

public final class WhisWeeklyPolicy {
    private WhisWeeklyPolicy() {}
    public static LocalDate weekStart(long millis) {
        return Instant.ofEpochMilli(millis).atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                .toLocalDate().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }
    public static int bonusPercent(int rank) {
        return switch (rank) { case 1 -> 5; case 2 -> 4; case 3 -> 3; default -> 0; };
    }
    public static long applyBonus(long value, int percent) {
        value = Math.max(0, Math.min(Integer.MAX_VALUE, value));
        return Math.min(Integer.MAX_VALUE, value + value * Math.max(0, Math.min(5, percent)) / 100L);
    }
}
