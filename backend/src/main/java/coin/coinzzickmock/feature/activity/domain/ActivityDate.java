package coin.coinzzickmock.feature.activity.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public final class ActivityDate {
    public static final ZoneId REPORTING_ZONE = ZoneId.of("Asia/Seoul");

    private ActivityDate() {
    }

    public static LocalDate from(Instant observedAt) {
        if (observedAt == null) {
            throw new IllegalArgumentException("observedAt is required");
        }
        return LocalDate.ofInstant(observedAt, REPORTING_ZONE);
    }
}
