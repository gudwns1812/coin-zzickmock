package coin.coinzzickmock.feature.market.application.history;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketPersistedCandleReader {
    private final MarketHistoryRepository marketHistoryRepository;
    private final MarketCandleRollupProjector rollupProjector;

    public List<MarketCandleResult> read(
            long symbolId,
            MarketCandleInterval interval,
            int limit,
            Instant beforeOpenTime
    ) {
        return switch (interval) {
            case ONE_MINUTE -> minuteResults(symbolId, limit, beforeOpenTime);
            case THREE_MINUTES -> rollupMinuteResults(symbolId, limit, beforeOpenTime, 3);
            case FIVE_MINUTES -> rollupMinuteResults(symbolId, limit, beforeOpenTime, 5);
            case FIFTEEN_MINUTES -> rollupMinuteResults(symbolId, limit, beforeOpenTime, 15);
            case ONE_HOUR -> hourlyResults(symbolId, limit, beforeOpenTime);
            case FOUR_HOURS, TWELVE_HOURS, ONE_DAY, ONE_WEEK, ONE_MONTH ->
                    rollupHourlyResults(symbolId, limit, interval, beforeOpenTime);
        };
    }

    private List<MarketCandleResult> minuteResults(long symbolId, int limit, Instant beforeOpenTime) {
        Instant latestMinuteOpenTime = resolveLatestMinuteOpenTime(symbolId, beforeOpenTime);
        if (latestMinuteOpenTime == null) {
            return List.of();
        }

        return rollupProjector.minuteResults(
                marketHistoryRepository.findMinuteCandles(
                        symbolId,
                        latestMinuteOpenTime.minus(limit - 1L, ChronoUnit.MINUTES),
                        latestMinuteOpenTime.plus(1, ChronoUnit.MINUTES)
                )
        );
    }

    private List<MarketCandleResult> rollupMinuteResults(
            long symbolId,
            int limit,
            Instant beforeOpenTime,
            int bucketMinutes
    ) {
        Instant latestMinuteOpenTime = resolveLatestMinuteOpenTime(symbolId, beforeOpenTime);
        if (latestMinuteOpenTime == null) {
            return List.of();
        }

        Instant latestBucketStart = MarketTime.alignToMinuteBucket(latestMinuteOpenTime, bucketMinutes);
        List<MarketHistoryCandle> rawCandles = marketHistoryRepository.findMinuteCandles(
                symbolId,
                latestBucketStart.minus((long) bucketMinutes * (limit - 1), ChronoUnit.MINUTES),
                latestMinuteOpenTime.plus(1, ChronoUnit.MINUTES)
        );
        return rollupProjector.rollupMinuteResults(rawCandles, bucketMinutes);
    }

    private List<MarketCandleResult> hourlyResults(long symbolId, int limit, Instant beforeOpenTime) {
        Instant latestHourOpenTime = resolveLatestHourlyOpenTime(symbolId, beforeOpenTime);
        if (latestHourOpenTime == null) {
            return List.of();
        }

        return rollupProjector.hourlyResults(
                marketHistoryRepository.findHourlyCandles(
                        symbolId,
                        latestHourOpenTime.minus(limit - 1L, ChronoUnit.HOURS),
                        latestHourOpenTime.plus(1, ChronoUnit.HOURS)
                )
        );
    }

    private List<MarketCandleResult> rollupHourlyResults(
            long symbolId,
            int limit,
            MarketCandleInterval interval,
            Instant beforeOpenTime
    ) {
        Instant latestHourOpenTime = resolveLatestHourlyOpenTime(symbolId, beforeOpenTime);
        if (latestHourOpenTime == null) {
            return List.of();
        }

        Instant latestCompleteBucketStart = latestCompleteHourlyBucketStart(latestHourOpenTime, interval);
        Instant earliestBucketStart = earliestHourlyBucketStart(latestCompleteBucketStart, interval, limit);
        List<HourlyMarketCandle> rawCandles = marketHistoryRepository.findHourlyCandles(
                symbolId,
                earliestBucketStart,
                MarketTime.bucketClose(latestCompleteBucketStart, interval)
        );
        return rollupProjector.rollupHourlyResults(rawCandles, interval);
    }

    private Instant resolveLatestMinuteOpenTime(long symbolId, Instant beforeOpenTime) {
        if (beforeOpenTime == null) {
            return marketHistoryRepository.findLatestMinuteCandleOpenTime(symbolId).orElse(null);
        }

        return marketHistoryRepository.findLatestMinuteCandleOpenTimeBefore(symbolId, beforeOpenTime)
                .orElse(null);
    }

    private Instant resolveLatestHourlyOpenTime(long symbolId, Instant beforeOpenTime) {
        if (beforeOpenTime == null) {
            return marketHistoryRepository.findLatestHourlyCandleOpenTime(symbolId).orElse(null);
        }

        return marketHistoryRepository.findLatestHourlyCandleOpenTimeBefore(symbolId, beforeOpenTime)
                .orElse(null);
    }

    private Instant earliestHourlyBucketStart(Instant latestHourOpenTime, MarketCandleInterval interval, int limit) {
        Instant latestBucketStart = MarketTime.bucketStart(latestHourOpenTime, interval);
        ZonedDateTime latestBucket = MarketTime.atStorageZone(latestBucketStart);

        return switch (interval) {
            case FOUR_HOURS -> latestBucket.minusHours(4L * (limit - 1)).toInstant();
            case TWELVE_HOURS -> latestBucket.minusHours(12L * (limit - 1)).toInstant();
            case ONE_DAY -> latestBucket.minusDays(limit - 1L).toInstant();
            case ONE_WEEK -> latestBucket.minusWeeks(limit - 1L).toInstant();
            case ONE_MONTH -> latestBucket.minusMonths(limit - 1L).toInstant();
            default -> throw new CoreException(ErrorCode.INVALID_REQUEST);
        };
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

    private Instant previousHourlyBucketStart(Instant bucketStart, MarketCandleInterval interval) {
        ZonedDateTime bucket = MarketTime.atStorageZone(bucketStart);
        return switch (interval) {
            case FOUR_HOURS -> bucket.minusHours(4).toInstant();
            case TWELVE_HOURS -> bucket.minusHours(12).toInstant();
            case ONE_DAY -> bucket.minusDays(1).toInstant();
            case ONE_WEEK -> bucket.minusWeeks(1).toInstant();
            case ONE_MONTH -> bucket.minusMonths(1).toInstant();
            default -> throw new CoreException(ErrorCode.INVALID_REQUEST);
        };
    }
}
