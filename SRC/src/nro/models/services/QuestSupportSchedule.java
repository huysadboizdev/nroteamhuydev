package nro.models.services;

import java.time.Clock;
import java.time.ZoneId;

public final class QuestSupportSchedule {
    public static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
    private final Clock clock;
    public QuestSupportSchedule(Clock clock) { this.clock = clock; }
    public boolean isOpen() {
        int hour = clock.instant().atZone(ZONE).getHour();
        return hour >= 14 && hour < 17;
    }
    public long closingMillis() {
        return clock.instant().atZone(ZONE).withHour(17).withMinute(0).withSecond(0).withNano(0)
                .toInstant().toEpochMilli();
    }
}
