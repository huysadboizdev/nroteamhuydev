package nro.models.services;

import java.time.*;
import java.util.TimeZone;
import static nro.models.services.PinkFixture.check;

public final class QuestSupportScheduleTest {
    public static void main(String[] args) {
        for (String day : new String[]{"2026-10-07", "2026-10-08"}) {
            for (String time : new String[]{"13:59:59", "14:00:00", "16:59:59", "17:00:00"}) {
                Instant instant = LocalDateTime.parse(day + "T" + time).atZone(QuestSupportSchedule.ZONE).toInstant();
                for (String systemZone : new String[]{"UTC", "America/Los_Angeles", "Asia/Tokyo"}) {
                    TimeZone.setDefault(TimeZone.getTimeZone(systemZone));
                    QuestSupportSchedule schedule = new QuestSupportSchedule(Clock.fixed(instant, ZoneId.systemDefault()));
                    check(schedule.isOpen() == (time.startsWith("14") || time.startsWith("16")), "boundary " + time + " " + systemZone);
                    check(schedule.closingMillis() == LocalDateTime.parse(day + "T17:00:00").atZone(QuestSupportSchedule.ZONE).toInstant().toEpochMilli(), "close fixed to Bangkok");
                }
            }
        }
        check(new QuestSupportSchedule(Clock.fixed(Instant.parse("2026-10-07T08:00:00Z"), ZoneOffset.UTC)).isOpen(), "restart 15:00 opens immediately");
        for (int[] bad : new int[][]{{0,120,300},{4,120,300},{3,0,300},{3,-1,300},{3,3601,300},{3,120,-1},{3,120,301}}) {
            boolean rejected = false;
            try { new QuestSupportConfig(true, bad[0], bad[1], bad[2]); } catch (IllegalArgumentException expected) { rejected = true; }
            check(rejected, "invalid config rejected");
        }
        System.out.println("PASS QuestSupport schedule: boundaries, dates, timezones, restart and limits");
    }
}
