package coin.coinzzickmock.feature.market.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.history.MarketHistoricalCandleCache;
import coin.coinzzickmock.feature.market.application.query.GetMarketCandlesQuery;
import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.HourlyMarketCandle;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import coin.coinzzickmock.providers.Providers;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetMarketCandlesService {
    private static final int DEFAULT_LIMIT = 120;
    private static final int MAX_LIMIT = 240;

    private final MarketHistoryRepository marketHistoryRepository;
    private final MarketHistoricalCandleCache historicalCandleCache;
    private final Providers providers;

    public List<MarketCandleResult> getCandles(GetMarketCandlesQuery query) {
        MarketCandleInterval interval = MarketCandleInterval.from(query.interval());
        int limit = normalizeLimit(query.limit());
        long symbolId = resolveSymbolId(query.symbol());

        List<MarketCandleResult> persistedCandles = switch (interval) {
            case ONE_MINUTE -> {
                Instant latestMinuteOpenTime = resolveLatestMinuteOpenTime(symbolId, query.beforeOpenTime());
                if (latestMinuteOpenTime == null) {
                    yield List.of();
                }
                yield minuteResults(
                        marketHistoryRepository.findMinuteCandles(
                                symbolId,
                                latestMinuteOpenTime.minus(limit - 1L, ChronoUnit.MINUTES),
                                latestMinuteOpenTime.plus(1, ChronoUnit.MINUTES)
                        )
                );
            }
            case THREE_MINUTES -> {
                Instant latestMinuteOpenTime = resolveLatestMinuteOpenTime(symbolId, query.beforeOpenTime());
                if (latestMinuteOpenTime == null) {
                    yield List.of();
                }
                yield rollupMinuteResults(symbolId, latestMinuteOpenTime, limit, 3);
            }
            case FIVE_MINUTES -> {
                Instant latestMinuteOpenTime = resolveLatestMinuteOpenTime(symbolId, query.beforeOpenTime());
                if (latestMinuteOpenTime == null) {
                    yield List.of();
                }
                yield rollupMinuteResults(symbolId, latestMinuteOpenTime, limit, 5);
            }
            case FIFTEEN_MINUTES -> {
                Instant latestMinuteOpenTime = resolveLatestMinuteOpenTime(symbolId, query.beforeOpenTime());
                if (latestMinuteOpenTime == null) {
                    yield List.of();
                }
                yield rollupMinuteResults(symbolId, latestMinuteOpenTime, limit, 15);
            }
            case ONE_HOUR -> hourlyResults(symbolId, limit, query.beforeOpenTime());
            case FOUR_HOURS, TWELVE_HOURS, ONE_DAY, ONE_WEEK, ONE_MONTH ->
                    rollupHourlyResults(symbolId, limit, interval, query.beforeOpenTime());
        };

        recordDbLookup(query.symbol(), interval, beforeOrEarliest(query.beforeOpenTime(), persistedCandles),
                dbLookupResult(persistedCandles, limit));
        return completeWithHistoricalCache(query.symbol(), interval, query.beforeOpenTime(), limit, persistedCandles);
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(requestedLimit, MAX_LIMIT);
    }

    private long resolveSymbolId(String symbol) {
        Long symbolId = marketHistoryRepository.findSymbolIdsBySymbols(List.of(symbol)).get(symbol);
        if (symbolId == null) {
            throw new CoreException(ErrorCode.MARKET_NOT_FOUND);
        }
        return symbolId;
    }

    private List<MarketCandleResult> completeWithHistoricalCache(
            String symbol,
            MarketCandleInterval interval,
            Instant beforeOpenTime,
            int limit,
            List<MarketCandleResult> persistedCandles
    ) {
        if (persistedCandles.size() >= limit) {
            return persistedCandles;
        }

        Instant supplementalToExclusive = persistedCandles.stream()
                .map(MarketCandleResult::openTime)
                .min(Instant::compareTo)
                .orElseGet(() -> beforeOpenTime == null ? Instant.now() : beforeOpenTime);
        int missingCount = limit - persistedCandles.size();

        List<MarketCandleResult> supplementalCandles = historicalCandleCache.loadOlderCandles(
                symbol,
                interval,
                supplementalToExclusive,
                missingCount
        );

        return mergeSupplementalCandles(persistedCandles, supplementalCandles, limit);
    }

    private List<MarketCandleResult> mergeSupplementalCandles(
            List<MarketCandleResult> persistedCandles,
            List<MarketCandleResult> supplementalCandles,
            int limit
    ) {
        Map<Instant, MarketCandleResult> candlesByOpenTime = new LinkedHashMap<>();
        supplementalCandles.stream()
                .sorted(java.util.Comparator.comparing(MarketCandleResult::openTime))
                .forEach(candle -> candlesByOpenTime.put(candle.openTime(), candle));
        persistedCandles.stream()
                .sorted(java.util.Comparator.comparing(MarketCandleResult::openTime))
                .forEach(candle -> candlesByOpenTime.put(candle.openTime(), candle));

        List<MarketCandleResult> merged = candlesByOpenTime.values().stream()
                .sorted(java.util.Comparator.comparing(MarketCandleResult::openTime))
                .toList();
        if (merged.size() <= limit) {
            return merged;
        }
        return merged.subList(merged.size() - limit, merged.size());
    }

    private void recordDbLookup(
            String symbol,
            MarketCandleInterval interval,
            Instant rangeStart,
            String result
    ) {
        String eventName = "partial".equals(result) ? "market.history.db.miss" : "market.history.db." + result;
        providers.telemetry().recordEvent(eventName, Map.of(
                "symbol", symbol,
                "interval", interval.value(),
                "range_bucket", rangeBucket(interval, rangeStart),
                "source", "db",
                "result", result
        ));
    }

    private String dbLookupResult(List<MarketCandleResult> persistedCandles, int limit) {
        if (persistedCandles.isEmpty()) {
            return "miss";
        }
        if (persistedCandles.size() < limit) {
            return "partial";
        }
        return "hit";
    }

    private Instant beforeOrEarliest(Instant beforeOpenTime, List<MarketCandleResult> persistedCandles) {
        return persistedCandles.stream()
                .map(MarketCandleResult::openTime)
                .min(Instant::compareTo)
                .orElseGet(() -> beforeOpenTime == null ? Instant.now() : beforeOpenTime);
    }

    private String rangeBucket(MarketCandleInterval interval, Instant rangeStart) {
        if (interval == MarketCandleInterval.ONE_MINUTE || interval == MarketCandleInterval.ONE_HOUR) {
            return YearMonth.from(rangeStart.atZone(ZoneOffset.UTC)).toString();
        }
        return String.valueOf(MarketTime.atStorageZone(rangeStart).getYear());
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

    private List<MarketCandleResult> minuteResults(List<MarketHistoryCandle> candles) {
        return candles.stream()
                .map(this::toMinuteResult)
                .toList();
    }

    private List<MarketCandleResult> hourlyResults(long symbolId, int limit, Instant beforeOpenTime) {
        Instant latestHourOpenTime = resolveLatestHourlyOpenTime(symbolId, beforeOpenTime);
        if (latestHourOpenTime == null) {
            return List.of();
        }
        List<HourlyMarketCandle> candles = marketHistoryRepository.findHourlyCandles(
                symbolId,
                latestHourOpenTime.minus(limit - 1L, ChronoUnit.HOURS),
                latestHourOpenTime.plus(1, ChronoUnit.HOURS)
        );

        return candles.stream()
                .map(this::toHourlyResult)
                .toList();
    }

    private List<MarketCandleResult> rollupMinuteResults(
            long symbolId,
            Instant latestMinuteOpenTime,
            int limit,
            int bucketMinutes
    ) {
        Instant latestBucketStart = MarketTime.alignToMinuteBucket(latestMinuteOpenTime, bucketMinutes);
        List<MarketHistoryCandle> rawCandles = marketHistoryRepository.findMinuteCandles(
                symbolId,
                latestBucketStart.minus((long) bucketMinutes * (limit - 1), ChronoUnit.MINUTES),
                latestMinuteOpenTime.plus(1, ChronoUnit.MINUTES)
        );

        Map<Instant, List<MarketHistoryCandle>> grouped = new LinkedHashMap<>();
        for (MarketHistoryCandle candle : rawCandles) {
            grouped.computeIfAbsent(
                    MarketTime.alignToMinuteBucket(candle.openTime(), bucketMinutes),
                    key -> new ArrayList<>()
            )
                    .add(candle);
        }

        return grouped.entrySet().stream()
                .filter(entry -> entry.getValue().size() == bucketMinutes)
                .map(entry -> rollupMinuteBucket(entry.getKey(), bucketMinutes, entry.getValue()))
                .toList();
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
        Instant earliestBucketStart = earliestHourlyBucketStart(latestHourOpenTime, interval, limit);
        List<HourlyMarketCandle> rawCandles = marketHistoryRepository.findHourlyCandles(
                symbolId,
                earliestBucketStart,
                latestHourOpenTime.plus(1, ChronoUnit.HOURS)
        );

        Map<Instant, List<HourlyMarketCandle>> grouped = new LinkedHashMap<>();
        for (HourlyMarketCandle candle : rawCandles) {
            grouped.computeIfAbsent(
                    MarketTime.bucketStart(candle.openTime(), interval),
                    key -> new ArrayList<>()
            )
                    .add(candle);
        }

        return grouped.entrySet().stream()
                .filter(entry -> entry.getValue().size() == expectedHourlyBucketSize(entry.getKey(), interval))
                .map(entry -> rollupHourlyBucket(entry.getKey(), interval, entry.getValue()))
                .toList();
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

    private MarketCandleResult rollupMinuteBucket(Instant bucketStart, int bucketMinutes, List<MarketHistoryCandle> candles) {
        MarketHistoryCandle first = candles.get(0);
        MarketHistoryCandle last = candles.get(0);
        double high = first.highPrice();
        double low = first.lowPrice();
        double volume = 0.0;

        for (MarketHistoryCandle candle : candles) {
            if (candle.openTime().isBefore(first.openTime())) {
                first = candle;
            }
            if (candle.openTime().isAfter(last.openTime())) {
                last = candle;
            }
            high = Math.max(high, candle.highPrice());
            low = Math.min(low, candle.lowPrice());
            volume += candle.volume();
        }

        return new MarketCandleResult(
                bucketStart,
                bucketStart.plus(bucketMinutes, ChronoUnit.MINUTES),
                first.openPrice(),
                high,
                low,
                last.closePrice(),
                volume
        );
    }

    private MarketCandleResult rollupHourlyBucket(
            Instant bucketStart,
            MarketCandleInterval interval,
            List<HourlyMarketCandle> candles
    ) {
        HourlyMarketCandle first = candles.get(0);
        HourlyMarketCandle last = candles.get(0);
        double high = first.highPrice();
        double low = first.lowPrice();
        double volume = 0.0;

        for (HourlyMarketCandle candle : candles) {
            if (candle.openTime().isBefore(first.openTime())) {
                first = candle;
            }
            if (candle.openTime().isAfter(last.openTime())) {
                last = candle;
            }
            high = Math.max(high, candle.highPrice());
            low = Math.min(low, candle.lowPrice());
            volume += candle.volume();
        }

        return new MarketCandleResult(
                bucketStart,
                MarketTime.bucketClose(bucketStart, interval),
                first.openPrice(),
                high,
                low,
                last.closePrice(),
                volume
        );
    }

    private int expectedHourlyBucketSize(Instant bucketStart, MarketCandleInterval interval) {
        return switch (interval) {
            case FOUR_HOURS -> 4;
            case TWELVE_HOURS -> 12;
            case ONE_DAY -> 24;
            case ONE_WEEK, ONE_MONTH -> (int) ChronoUnit.HOURS.between(
                    bucketStart,
                    MarketTime.bucketClose(bucketStart, interval)
            );
            default -> throw new CoreException(ErrorCode.INVALID_REQUEST);
        };
    }

    private MarketCandleResult toMinuteResult(MarketHistoryCandle candle) {
        return new MarketCandleResult(
                candle.openTime(),
                candle.closeTime(),
                candle.openPrice(),
                candle.highPrice(),
                candle.lowPrice(),
                candle.closePrice(),
                candle.volume()
        );
    }

    private MarketCandleResult toHourlyResult(HourlyMarketCandle candle) {
        return new MarketCandleResult(
                candle.openTime(),
                candle.closeTime(),
                candle.openPrice(),
                candle.highPrice(),
                candle.lowPrice(),
                candle.closePrice(),
                candle.volume()
        );
    }
}
