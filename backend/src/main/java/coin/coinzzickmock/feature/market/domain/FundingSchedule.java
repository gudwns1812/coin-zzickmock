package coin.coinzzickmock.feature.market.domain;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public record FundingSchedule(
        int intervalHours,
        int anchorHourKst,
        ZoneId zoneId
) {
    public static final int DEFAULT_INTERVAL_HOURS = 8;
    public static final int DEFAULT_ANCHOR_HOUR_KST = 1;
    public static final ZoneId DEFAULT_ZONE_ID = ZoneId.of("Asia/Seoul");

    public FundingSchedule {
        if (intervalHours <= 0 || intervalHours > 24) {
            throw new IllegalArgumentException("Funding interval must be between 1 and 24 hours");
        }
        if (anchorHourKst < 0 || anchorHourKst > 23) {
            throw new IllegalArgumentException("Funding anchor hour must be between 0 and 23");
        }
        if (zoneId == null) {
            throw new IllegalArgumentException("Funding zone id is required");
        }
    }

    public static FundingSchedule defaultUsdtPerpetual() {
        return new FundingSchedule(DEFAULT_INTERVAL_HOURS, DEFAULT_ANCHOR_HOUR_KST, DEFAULT_ZONE_ID);
    }

    public Instant nextFundingAt(Instant serverTime) {
        ZonedDateTime current = serverTime.atZone(zoneId);
        ZonedDateTime next = current.toLocalDate()
                .atTime(anchorHourKst, 0)
                .atZone(zoneId);
        while (!next.isAfter(current)) {
            next = next.plusHours(intervalHours);
        }
        return next.toInstant();
    }

    public long secondsUntilFunding(Instant serverTime) {
        return Math.max(0, Duration.between(serverTime, nextFundingAt(serverTime)).getSeconds());
    }
}
