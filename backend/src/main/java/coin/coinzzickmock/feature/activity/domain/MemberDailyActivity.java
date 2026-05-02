package coin.coinzzickmock.feature.activity.domain;

import java.time.Instant;
import java.time.LocalDate;

public record MemberDailyActivity(
        LocalDate activityDate,
        Long memberId,
        Instant firstSeenAt,
        Instant lastSeenAt,
        long activityCount,
        ActivitySource firstSource,
        ActivitySource lastSource
) {
    public static MemberDailyActivity firstSeen(
            LocalDate activityDate,
            Long memberId,
            Instant observedAt,
            ActivitySource source
    ) {
        return new MemberDailyActivity(activityDate, memberId, observedAt, observedAt, 1, source, source);
    }

    public MemberDailyActivity record(Instant observedAt, ActivitySource source) {
        return new MemberDailyActivity(
                activityDate,
                memberId,
                firstSeenAt,
                observedAt,
                activityCount + 1,
                firstSource,
                source
        );
    }
}
