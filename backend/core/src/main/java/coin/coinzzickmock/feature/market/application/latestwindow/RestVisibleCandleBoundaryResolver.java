package coin.coinzzickmock.feature.market.application.latestwindow;

import coin.coinzzickmock.feature.market.application.history.MarketCandleRollupProjector;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.domain.CompletedMarketCandle;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class RestVisibleCandleBoundaryResolver {
    private static final int DERIVED_BOUNDARY_SEARCH_BUCKETS = 2;

    private final MarketHistoryRepository marketHistoryRepository;
    private final MarketCandleRollupProjector rollupProjector;

    public RestVisibleCandleBoundaryResolver(
            MarketHistoryRepository marketHistoryRepository,
            MarketCandleRollupProjector rollupProjector
    ) {
        this.marketHistoryRepository = marketHistoryRepository;
        this.rollupProjector = rollupProjector;
    }

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
        Optional<Instant> latestMinuteOpenTime = marketHistoryRepository.findLatestMinuteCandleOpenTime(symbolId);
        if (latestMinuteOpenTime.isEmpty()) {
            return Optional.empty();
        }

        Instant candidateBucketStart = latestCompleteMinuteBucketStart(latestMinuteOpenTime.get(), bucketMinutes);
        for (int attempt = 0; attempt < DERIVED_BOUNDARY_SEARCH_BUCKETS; attempt++) {
            if (hasCompleteMinuteBucket(symbolId, candidateBucketStart, bucketMinutes)) {
                return Optional.of(new RestVisibleCandleBoundary(symbolId, interval, candidateBucketStart));
            }
            candidateBucketStart = candidateBucketStart.minus(bucketMinutes, ChronoUnit.MINUTES);
        }
        return Optional.empty();
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
        Optional<Instant> latestHourOpenTime = marketHistoryRepository.findLatestCompletedHourlyCandleOpenTime(symbolId);
        if (latestHourOpenTime.isEmpty()) {
            return Optional.empty();
        }

        Instant candidateBucketStart = latestCompleteHourlyBucketStart(latestHourOpenTime.get(), interval);
        for (int attempt = 0; attempt < DERIVED_BOUNDARY_SEARCH_BUCKETS; attempt++) {
            if (hasCompleteHourlyBucket(symbolId, candidateBucketStart, interval)) {
                return Optional.of(new RestVisibleCandleBoundary(symbolId, interval, candidateBucketStart));
            }
            candidateBucketStart = previousHourlyBucketStart(candidateBucketStart, interval);
        }
        return Optional.empty();
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
        Optional<Instant> latestDayOpenTime = marketHistoryRepository.findLatestCompletedCandleOpenTime(
                symbolId,
                MarketCandleInterval.ONE_DAY
        );
        if (latestDayOpenTime.isEmpty()) {
            return Optional.empty();
        }

        Instant candidateBucketStart = latestCompleteDailyBucketStart(latestDayOpenTime.get(), interval);
        for (int attempt = 0; attempt < DERIVED_BOUNDARY_SEARCH_BUCKETS; attempt++) {
            if (hasCompleteDailyBucket(symbolId, candidateBucketStart, interval)) {
                return Optional.of(new RestVisibleCandleBoundary(symbolId, interval, candidateBucketStart));
            }
            candidateBucketStart = MarketTime.atStorageZone(candidateBucketStart).minusWeeks(1).toInstant();
        }
        return Optional.empty();
    }

    private Instant latestCompleteMinuteBucketStart(Instant latestMinuteOpenTime, int bucketMinutes) {
        Instant latestBucketStart = MarketTime.alignToMinuteBucket(latestMinuteOpenTime, bucketMinutes);
        Instant latestBucketClose = latestBucketStart.plus(bucketMinutes, ChronoUnit.MINUTES);
        Instant latestKnownClose = latestMinuteOpenTime.plus(1, ChronoUnit.MINUTES);
        if (!latestKnownClose.isBefore(latestBucketClose)) {
            return latestBucketStart;
        }
        return latestBucketStart.minus(bucketMinutes, ChronoUnit.MINUTES);
    }

    private Instant latestCompleteHourlyBucketStart(Instant latestHourOpenTime, MarketCandleInterval interval) {
        Instant latestBucketStart = MarketTime.bucketStart(latestHourOpenTime, interval);
        Instant latestBucketClose = MarketTime.bucketClose(latestBucketStart, interval);
        Instant latestKnownClose = latestHourOpenTime.plus(1, ChronoUnit.HOURS);
        if (!latestKnownClose.isBefore(latestBucketClose)) {
            return latestBucketStart;
        }
        return previousHourlyBucketStart(latestBucketStart, interval);
    }

    private Instant latestCompleteDailyBucketStart(Instant latestDayOpenTime, MarketCandleInterval interval) {
        Instant latestBucketStart = MarketTime.bucketStart(latestDayOpenTime, interval);
        Instant latestBucketClose = MarketTime.bucketClose(latestBucketStart, interval);
        Instant latestKnownClose = latestDayOpenTime.plus(1, ChronoUnit.DAYS);
        if (!latestKnownClose.isBefore(latestBucketClose)) {
            return latestBucketStart;
        }
        return MarketTime.atStorageZone(latestBucketStart).minusWeeks(1).toInstant();
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
