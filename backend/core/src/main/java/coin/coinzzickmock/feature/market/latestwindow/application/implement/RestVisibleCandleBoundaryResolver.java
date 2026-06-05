package coin.coinzzickmock.feature.market.latestwindow.application.implement;

import coin.coinzzickmock.feature.market.history.application.implement.MarketCandleRollupProjector;
import coin.coinzzickmock.feature.market.history.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.domain.CompletedMarketCandle;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import coin.coinzzickmock.feature.market.latestwindow.application.dto.RestVisibleCandleBoundary;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RestVisibleCandleBoundaryResolver {
    private static final int DERIVED_BOUNDARY_SEARCH_BUCKETS = 2;

    private final MarketHistoryRepository marketHistoryRepository;
    private final MarketCandleRollupProjector rollupProjector;

    public Optional<RestVisibleCandleBoundary> resolve(long symbolId, MarketCandleInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> latestMinuteBoundary(symbolId, interval);
            case THREE_MINUTES -> latestMinuteRollupBoundary(symbolId, interval, 3);
            case FIVE_MINUTES -> latestMinuteRollupBoundary(symbolId, interval, 5);
            case FIFTEEN_MINUTES -> latestMinuteRollupBoundary(symbolId, interval, 15);
            case ONE_HOUR -> latestCompletedHourlyBoundary(symbolId, interval);
            case FOUR_HOURS, TWELVE_HOURS -> latestHourlyRollupBoundary(symbolId, interval);
            case ONE_DAY, ONE_MONTH -> latestCompletedCalendarBoundary(symbolId, interval);
            case ONE_WEEK -> latestDailyRollupBoundary(symbolId, interval);
        };
    }

    private Optional<RestVisibleCandleBoundary> latestMinuteBoundary(long symbolId, MarketCandleInterval interval) {
        return marketHistoryRepository.findLatestMinuteCandleOpenTime(symbolId)
                .map(openTime -> new RestVisibleCandleBoundary(symbolId, interval, openTime));
    }

    private Optional<RestVisibleCandleBoundary> latestMinuteRollupBoundary(
            long symbolId,
            MarketCandleInterval interval,
            int bucketMinutes
    ) {
        return marketHistoryRepository.findLatestMinuteCandleOpenTime(symbolId)
                .flatMap(latestMinuteOpenTime -> latestRollupBoundary(
                        symbolId,
                        interval,
                        latestMinuteOpenTime,
                        minuteBucketCursor(bucketMinutes),
                        bucketStart -> hasCompleteMinuteBucket(symbolId, bucketStart, bucketMinutes)
                ));
    }

    private Optional<RestVisibleCandleBoundary> latestCompletedHourlyBoundary(
            long symbolId,
            MarketCandleInterval interval
    ) {
        return marketHistoryRepository.findLatestCompletedHourlyCandleOpenTime(symbolId)
                .map(openTime -> new RestVisibleCandleBoundary(symbolId, interval, openTime));
    }

    private Optional<RestVisibleCandleBoundary> latestHourlyRollupBoundary(
            long symbolId,
            MarketCandleInterval interval
    ) {
        return marketHistoryRepository.findLatestCompletedHourlyCandleOpenTime(symbolId)
                .flatMap(latestHourOpenTime -> latestRollupBoundary(
                        symbolId,
                        interval,
                        latestHourOpenTime,
                        intervalBucketCursor(
                                Duration.ofHours(1),
                                interval,
                                bucketStart -> previousHourlyBucketStart(bucketStart, interval)
                        ),
                        bucketStart -> hasCompleteHourlyBucket(symbolId, bucketStart, interval)
                ));
    }

    private Optional<RestVisibleCandleBoundary> latestCompletedCalendarBoundary(
            long symbolId,
            MarketCandleInterval interval
    ) {
        return marketHistoryRepository.findLatestCompletedCandleOpenTime(symbolId, interval)
                .map(openTime -> new RestVisibleCandleBoundary(symbolId, interval, openTime));
    }

    private Optional<RestVisibleCandleBoundary> latestDailyRollupBoundary(
            long symbolId,
            MarketCandleInterval interval
    ) {
        return marketHistoryRepository.findLatestCompletedCandleOpenTime(symbolId, MarketCandleInterval.ONE_DAY)
                .flatMap(latestDayOpenTime -> latestRollupBoundary(
                        symbolId,
                        interval,
                        latestDayOpenTime,
                        intervalBucketCursor(
                                Duration.ofDays(1),
                                interval,
                                bucketStart -> MarketTime.atStorageZone(bucketStart).minusWeeks(1).toInstant()
                        ),
                        bucketStart -> hasCompleteDailyBucket(symbolId, bucketStart, interval)
                ));
    }

    private Optional<RestVisibleCandleBoundary> latestRollupBoundary(
            long symbolId,
            MarketCandleInterval interval,
            Instant latestSourceOpenTime,
            BucketCursor cursor,
            Predicate<Instant> isCompleteBucket
    ) {
        return searchCompleteBoundary(
                latestCompleteBucketStart(latestSourceOpenTime, cursor),
                cursor.previousBucketStart(),
                bucketStart -> isCompleteBucket.test(bucketStart)
                        ? Optional.of(new RestVisibleCandleBoundary(symbolId, interval, bucketStart))
                        : Optional.empty()
        );
    }

    private BucketCursor minuteBucketCursor(int bucketMinutes) {
        return new BucketCursor(
                Duration.ofMinutes(1),
                sourceOpenTime -> MarketTime.alignToMinuteBucket(sourceOpenTime, bucketMinutes),
                bucketStart -> bucketStart.plus(bucketMinutes, ChronoUnit.MINUTES),
                bucketStart -> bucketStart.minus(bucketMinutes, ChronoUnit.MINUTES)
        );
    }

    private BucketCursor intervalBucketCursor(
            Duration sourceCandleDuration,
            MarketCandleInterval interval,
            Function<Instant, Instant> previousBucketStart
    ) {
        return new BucketCursor(
                sourceCandleDuration,
                sourceOpenTime -> MarketTime.bucketStart(sourceOpenTime, interval),
                bucketStart -> MarketTime.bucketClose(bucketStart, interval),
                previousBucketStart
        );
    }

    private Instant latestCompleteBucketStart(Instant latestSourceOpenTime, BucketCursor cursor) {
        Instant latestBucketStart = cursor.bucketStart().apply(latestSourceOpenTime);
        Instant latestBucketClose = cursor.bucketClose().apply(latestBucketStart);
        Instant latestKnownClose = latestSourceOpenTime.plus(cursor.sourceCandleDuration());
        if (!latestKnownClose.isBefore(latestBucketClose)) {
            return latestBucketStart;
        }
        return cursor.previousBucketStart().apply(latestBucketStart);
    }

    private Optional<RestVisibleCandleBoundary> searchCompleteBoundary(
            Instant firstCandidate,
            Function<Instant, Instant> previousCandidate,
            Function<Instant, Optional<RestVisibleCandleBoundary>> resolveCandidate
    ) {
        Instant candidateBucketStart = firstCandidate;
        for (int attempt = 0; attempt < DERIVED_BOUNDARY_SEARCH_BUCKETS; attempt++) {
            Optional<RestVisibleCandleBoundary> boundary = resolveCandidate.apply(candidateBucketStart);
            if (boundary.isPresent()) {
                return boundary;
            }
            candidateBucketStart = previousCandidate.apply(candidateBucketStart);
        }
        return Optional.empty();
    }

    private boolean hasCompleteMinuteBucket(long symbolId, Instant bucketStart, int bucketMinutes) {
        List<MarketHistoryCandle> candles = marketHistoryRepository.findMinuteCandles(
                symbolId,
                bucketStart,
                bucketStart.plus(bucketMinutes, ChronoUnit.MINUTES)
        );
        return rollupProjector.isCompleteMinuteBucket(bucketStart, bucketMinutes, candles);
    }

    private boolean hasCompleteHourlyBucket(long symbolId, Instant bucketStart, MarketCandleInterval interval) {
        List<HourlyMarketCandle> candles = marketHistoryRepository.findCompletedHourlyCandles(
                symbolId,
                bucketStart,
                MarketTime.bucketClose(bucketStart, interval)
        );
        return rollupProjector.isCompleteHourlyBucket(bucketStart, interval, candles);
    }

    private boolean hasCompleteDailyBucket(long symbolId, Instant bucketStart, MarketCandleInterval interval) {
        List<CompletedMarketCandle> candles = marketHistoryRepository.findCompletedCandles(
                symbolId,
                MarketCandleInterval.ONE_DAY,
                bucketStart,
                MarketTime.bucketClose(bucketStart, interval)
        );
        return rollupProjector.isCompleteDailyBucket(bucketStart, interval, candles);
    }

    private Instant previousHourlyBucketStart(Instant bucketStart, MarketCandleInterval interval) {
        return switch (interval) {
            case FOUR_HOURS -> bucketStart.minus(4, ChronoUnit.HOURS);
            case TWELVE_HOURS -> bucketStart.minus(12, ChronoUnit.HOURS);
            default -> throw new IllegalArgumentException("Unsupported hourly rollup interval: " + interval);
        };
    }

    private record BucketCursor(
            Duration sourceCandleDuration,
            Function<Instant, Instant> bucketStart,
            Function<Instant, Instant> bucketClose,
            Function<Instant, Instant> previousBucketStart
    ) {
    }
}
