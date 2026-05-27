package coin.coinzzickmock.feature.market.application.latestwindow;

import coin.coinzzickmock.feature.market.application.history.MarketCandleRollupProjector;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.domain.CompletedMarketCandle;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
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
                .flatMap(latestMinuteOpenTime -> latestMinuteRollupBoundary(
                        symbolId,
                        interval,
                        bucketMinutes,
                        latestMinuteOpenTime
                ));
    }

    private Optional<RestVisibleCandleBoundary> latestMinuteRollupBoundary(
            long symbolId,
            MarketCandleInterval interval,
            int bucketMinutes,
            Instant latestMinuteOpenTime
    ) {
        Instant candidateBucketStart = latestCompleteMinuteBucketStart(latestMinuteOpenTime, bucketMinutes);
        return searchCompleteBoundary(
                candidateBucketStart,
                bucketStart -> bucketStart.minus(bucketMinutes, ChronoUnit.MINUTES),
                bucketStart -> hasCompleteMinuteBucket(symbolId, bucketStart, bucketMinutes)
                        ? Optional.of(new RestVisibleCandleBoundary(symbolId, interval, bucketStart))
                        : Optional.empty()
        );
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
                .flatMap(latestHourOpenTime -> latestHourlyRollupBoundary(symbolId, interval, latestHourOpenTime));
    }

    private Optional<RestVisibleCandleBoundary> latestHourlyRollupBoundary(
            long symbolId,
            MarketCandleInterval interval,
            Instant latestHourOpenTime
    ) {
        Instant candidateBucketStart = latestCompleteHourlyBucketStart(latestHourOpenTime, interval);
        return searchCompleteBoundary(
                candidateBucketStart,
                bucketStart -> previousHourlyBucketStart(bucketStart, interval),
                bucketStart -> hasCompleteHourlyBucket(symbolId, bucketStart, interval)
                        ? Optional.of(new RestVisibleCandleBoundary(symbolId, interval, bucketStart))
                        : Optional.empty()
        );
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
                .flatMap(latestDayOpenTime -> latestDailyRollupBoundary(symbolId, interval, latestDayOpenTime));
    }

    private Optional<RestVisibleCandleBoundary> latestDailyRollupBoundary(
            long symbolId,
            MarketCandleInterval interval,
            Instant latestDayOpenTime
    ) {
        Instant candidateBucketStart = latestCompleteDailyBucketStart(latestDayOpenTime, interval);
        return searchCompleteBoundary(
                candidateBucketStart,
                bucketStart -> MarketTime.atStorageZone(bucketStart).minusWeeks(1).toInstant(),
                bucketStart -> hasCompleteDailyBucket(symbolId, bucketStart, interval)
                        ? Optional.of(new RestVisibleCandleBoundary(symbolId, interval, bucketStart))
                        : Optional.empty()
        );
    }

    private Instant latestCompleteMinuteBucketStart(Instant latestMinuteOpenTime, int bucketMinutes) {
        Instant latestBucketStart = MarketTime.alignToMinuteBucket(latestMinuteOpenTime, bucketMinutes);
        Instant latestBucketClose = latestBucketStart.plus(bucketMinutes, ChronoUnit.MINUTES);
        return latestCompleteBucketStart(
                latestMinuteOpenTime,
                latestBucketStart,
                latestBucketClose,
                Duration.ofMinutes(1),
                bucketStart -> bucketStart.minus(bucketMinutes, ChronoUnit.MINUTES)
        );
    }

    private Instant latestCompleteHourlyBucketStart(Instant latestHourOpenTime, MarketCandleInterval interval) {
        Instant latestBucketStart = MarketTime.bucketStart(latestHourOpenTime, interval);
        Instant latestBucketClose = MarketTime.bucketClose(latestBucketStart, interval);
        return latestCompleteBucketStart(
                latestHourOpenTime,
                latestBucketStart,
                latestBucketClose,
                Duration.ofHours(1),
                bucketStart -> previousHourlyBucketStart(bucketStart, interval)
        );
    }

    private Instant latestCompleteDailyBucketStart(Instant latestDayOpenTime, MarketCandleInterval interval) {
        Instant latestBucketStart = MarketTime.bucketStart(latestDayOpenTime, interval);
        Instant latestBucketClose = MarketTime.bucketClose(latestBucketStart, interval);
        return latestCompleteBucketStart(
                latestDayOpenTime,
                latestBucketStart,
                latestBucketClose,
                Duration.ofDays(1),
                bucketStart -> MarketTime.atStorageZone(bucketStart).minusWeeks(1).toInstant()
        );
    }

    private Instant latestCompleteBucketStart(
            Instant latestSourceOpenTime,
            Instant latestBucketStart,
            Instant latestBucketClose,
            Duration sourceCandleDuration,
            Function<Instant, Instant> previousBucketStart
    ) {
        Instant latestKnownClose = latestSourceOpenTime.plus(sourceCandleDuration);
        if (!latestKnownClose.isBefore(latestBucketClose)) {
            return latestBucketStart;
        }
        return previousBucketStart.apply(latestBucketStart);
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
}
