package coin.coinzzickmock.feature.activity.domain;

import java.time.Instant;
import java.time.LocalDate;

public record DailyActiveUserSummary(
        LocalDate activityDate,
        long activeUserCount,
        Instant sampledAt
) {
}
