package coin.coinzzickmock.feature.market.latestwindow.application.implement;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import coin.coinzzickmock.feature.market.latestwindow.application.dto.MarketLatestCandleWindowKey;
import coin.coinzzickmock.feature.market.latestwindow.application.dto.RestVisibleCandleBoundary;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class MarketLatestCandleWindowPolicy {
    private static final Set<Integer> ELIGIBLE_LIMITS = Set.of(60, 104, 120, 168, 180);
    private static final Duration MIN_TTL = Duration.ofSeconds(5);
    private static final Duration FALLBACK_TTL = Duration.ofSeconds(30);
    private static final Duration MAX_TTL = Duration.ofMinutes(5);
    private static final Duration PERSISTENCE_GRACE = Duration.ofSeconds(5);

    private final Clock clock;

    public MarketLatestCandleWindowPolicy() {
        this(Clock.systemUTC());
    }

    MarketLatestCandleWindowPolicy(Clock clock) {
        this.clock = clock;
    }

    public boolean isEligible(Instant beforeOpenTime, int normalizedLimit) {
        return beforeOpenTime == null && ELIGIBLE_LIMITS.contains(normalizedLimit);
    }

    public MarketLatestCandleWindowKey key(String symbol, RestVisibleCandleBoundary boundary, int normalizedLimit) {
        return new MarketLatestCandleWindowKey(
                symbol,
                boundary.interval(),
                normalizedLimit,
                boundary.latestOutputOpenTime()
        );
    }

    public Duration ttl(RestVisibleCandleBoundary boundary) {
        Instant nextExpectedBoundary = nextExpectedBoundary(boundary.latestOutputOpenTime(), boundary.interval())
                .plus(PERSISTENCE_GRACE);
        Duration untilNextBoundary = Duration.between(clock.instant(), nextExpectedBoundary);
        if (untilNextBoundary.isNegative() || untilNextBoundary.isZero()) {
            return FALLBACK_TTL;
        }
        if (untilNextBoundary.compareTo(MIN_TTL) < 0) {
            return MIN_TTL;
        }
        if (untilNextBoundary.compareTo(MAX_TTL) > 0) {
            return MAX_TTL;
        }
        return untilNextBoundary;
    }

    private Instant nextExpectedBoundary(Instant latestOutputOpenTime, MarketCandleInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> latestOutputOpenTime.plus(2, ChronoUnit.MINUTES);
            case THREE_MINUTES -> latestOutputOpenTime.plus(6, ChronoUnit.MINUTES);
            case FIVE_MINUTES -> latestOutputOpenTime.plus(10, ChronoUnit.MINUTES);
            case FIFTEEN_MINUTES -> latestOutputOpenTime.plus(30, ChronoUnit.MINUTES);
            case ONE_HOUR -> latestOutputOpenTime.plus(2, ChronoUnit.HOURS);
            case FOUR_HOURS, TWELVE_HOURS, ONE_DAY, ONE_WEEK, ONE_MONTH ->
                    MarketTime.bucketClose(MarketTime.bucketClose(latestOutputOpenTime, interval), interval);
        };
    }
}
