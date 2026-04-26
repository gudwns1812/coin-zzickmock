package coin.coinzzickmock.feature.market.application.history;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import coin.coinzzickmock.providers.connector.MarketHistoricalCandleGranularity;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import org.springframework.stereotype.Component;

@Component
public class MarketHistoricalCandleSegmentPolicy {
    static final int SEGMENT_SIZE = 200;
    private static final ZonedDateTime WEEK_ANCHOR = ZonedDateTime.of(1970, 1, 5, 0, 0, 0, 0, MarketTime.STORAGE_ZONE);
    private static final YearMonth MONTH_ANCHOR = YearMonth.of(1970, 1);

    public MarketHistoricalCandleSegment segmentContainingPreviousCandle(
            String symbol,
            MarketCandleInterval interval,
            Instant toExclusive
    ) {
        Instant previousCandleTime = previousCandleTime(toExclusive, interval);
        Instant startInclusive = segmentStart(previousCandleTime, interval);
        return new MarketHistoricalCandleSegment(
                symbol,
                interval,
                MarketHistoricalCandleGranularity.from(interval).value(),
                startInclusive,
                segmentEnd(startInclusive, interval),
                SEGMENT_SIZE
        );
    }

    private Instant previousCandleTime(Instant toExclusive, MarketCandleInterval interval) {
        return switch (interval) {
            case ONE_WEEK -> MarketTime.atStorageZone(toExclusive)
                    .minusWeeks(1)
                    .toInstant();
            case ONE_MONTH -> MarketTime.atStorageZone(toExclusive)
                    .minusMonths(1)
                    .toInstant();
            default -> toExclusive.minus(intervalDuration(interval));
        };
    }

    private Instant segmentStart(Instant time, MarketCandleInterval interval) {
        return switch (interval) {
            case ONE_WEEK -> weeklySegmentStart(time);
            case ONE_MONTH -> monthlySegmentStart(time);
            default -> fixedDurationSegmentStart(time, interval);
        };
    }

    private Instant segmentEnd(Instant segmentStart, MarketCandleInterval interval) {
        return switch (interval) {
            case ONE_WEEK -> MarketTime.atStorageZone(segmentStart)
                    .plusWeeks(SEGMENT_SIZE)
                    .toInstant();
            case ONE_MONTH -> MarketTime.atStorageZone(segmentStart)
                    .plusMonths(SEGMENT_SIZE)
                    .toInstant();
            default -> segmentStart.plus(intervalDuration(interval).multipliedBy(SEGMENT_SIZE));
        };
    }

    private Instant weeklySegmentStart(Instant time) {
        ZonedDateTime weekStart = MarketTime.atStorageZone(time)
                .truncatedTo(ChronoUnit.DAYS)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        long weeksFromAnchor = ChronoUnit.WEEKS.between(WEEK_ANCHOR, weekStart);
        long segmentIndex = Math.floorDiv(weeksFromAnchor, SEGMENT_SIZE);
        return WEEK_ANCHOR.plusWeeks(segmentIndex * SEGMENT_SIZE).toInstant();
    }

    private Instant monthlySegmentStart(Instant time) {
        YearMonth monthStart = YearMonth.from(MarketTime.atStorageZone(time));
        long monthsFromAnchor = ChronoUnit.MONTHS.between(MONTH_ANCHOR, monthStart);
        long segmentIndex = Math.floorDiv(monthsFromAnchor, SEGMENT_SIZE);
        return MONTH_ANCHOR.plusMonths(segmentIndex * SEGMENT_SIZE)
                .atDay(1)
                .atStartOfDay(MarketTime.STORAGE_ZONE)
                .toInstant();
    }

    private Instant fixedDurationSegmentStart(Instant time, MarketCandleInterval interval) {
        Duration duration = intervalDuration(interval);
        long segmentMillis = duration.toMillis() * SEGMENT_SIZE;
        return Instant.ofEpochMilli(Math.floorDiv(time.toEpochMilli(), segmentMillis) * segmentMillis);
    }

    private Duration intervalDuration(MarketCandleInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> Duration.ofMinutes(1);
            case THREE_MINUTES -> Duration.ofMinutes(3);
            case FIVE_MINUTES -> Duration.ofMinutes(5);
            case FIFTEEN_MINUTES -> Duration.ofMinutes(15);
            case ONE_HOUR -> Duration.ofHours(1);
            case FOUR_HOURS -> Duration.ofHours(4);
            case TWELVE_HOURS -> Duration.ofHours(12);
            case ONE_DAY -> Duration.ofDays(1);
            case ONE_WEEK, ONE_MONTH -> throw new IllegalArgumentException("Calendar interval has no fixed duration");
        };
    }
}
