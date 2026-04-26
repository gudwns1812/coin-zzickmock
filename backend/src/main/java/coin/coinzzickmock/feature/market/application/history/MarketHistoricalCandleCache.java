package coin.coinzzickmock.feature.market.application.history;

import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoricalCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.infrastructure.config.CoinCacheNames;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MarketHistoricalCandleCache {
    private static final int SEGMENT_SIZE = 200;
    private static final Duration PROVIDER_PERMIT_TIMEOUT = Duration.ofSeconds(3);

    private final Providers providers;
    private final Cache cache;
    private final Semaphore providerLane = new Semaphore(1);
    private final ConcurrentMap<String, CompletableFuture<List<MarketCandleResult>>> fills = new ConcurrentHashMap<>();

    public MarketHistoricalCandleCache(
            Providers providers,
            @Qualifier("distributedCacheManager") ObjectProvider<CacheManager> distributedCacheManagerProvider
    ) {
        this.providers = providers;
        CacheManager cacheManager = distributedCacheManagerProvider.getIfAvailable();
        this.cache = cacheManager == null
                ? null
                : cacheManager.getCache(CoinCacheNames.MARKET_HISTORICAL_CANDLES_DISTRIBUTED_CACHE);
    }

    public List<MarketCandleResult> loadOlderCandles(
            String symbol,
            MarketCandleInterval interval,
            Instant toExclusive,
            int limit
    ) {
        if (limit <= 0 || toExclusive == null) {
            return List.of();
        }

        List<MarketCandleResult> candles = new ArrayList<>();
        Instant cursor = toExclusive;
        boolean populatedMiss = false;

        while (candles.size() < limit && !populatedMiss) {
            Instant segmentStart = segmentStartContainingPreviousCandle(cursor, interval);
            Instant segmentEnd = segmentEnd(segmentStart, interval);
            String cacheKey = cacheKey(symbol, interval, segmentStart);
            List<MarketCandleResult> segmentCandles = readSegment(cacheKey, symbol, interval, segmentStart);

            if (segmentCandles == null) {
                populatedMiss = true;
                segmentCandles = fillSegment(cacheKey, symbol, interval, segmentStart, segmentEnd);
            }

            Instant currentCursor = cursor;
            candles.addAll(segmentCandles.stream()
                    .filter(candle -> candle.openTime().isBefore(currentCursor))
                    .toList());
            cursor = segmentStart;
        }

        return newest(limit, candles);
    }

    private List<MarketCandleResult> readSegment(
            String cacheKey,
            String symbol,
            MarketCandleInterval interval,
            Instant segmentStart
    ) {
        if (cache == null) {
            record("market.history.redis.miss", symbol, interval, segmentStart, "redis", "unavailable");
            return null;
        }

        Cache.ValueWrapper wrapper = cache.get(cacheKey);
        if (wrapper == null) {
            record("market.history.redis.miss", symbol, interval, segmentStart, "redis", "miss");
            return null;
        }
        Object value = wrapper.get();
        if (value instanceof MarketHistoricalCandlePage page) {
            record("market.history.redis.hit", symbol, interval, segmentStart, "redis", "hit");
            return page.candles();
        }

        record("market.history.redis.miss", symbol, interval, segmentStart, "redis", "unexpected_value");
        return null;
    }

    private List<MarketCandleResult> fillSegment(
            String cacheKey,
            String symbol,
            MarketCandleInterval interval,
            Instant segmentStart,
            Instant segmentEnd
    ) {
        CompletableFuture<List<MarketCandleResult>> fill = new CompletableFuture<>();
        CompletableFuture<List<MarketCandleResult>> existing = fills.putIfAbsent(cacheKey, fill);
        if (existing != null) {
            return existing.join();
        }

        try {
            List<MarketCandleResult> candles = fetchSegment(symbol, interval, segmentStart, segmentEnd);
            if (cache != null && !candles.isEmpty()) {
                cache.put(cacheKey, new MarketHistoricalCandlePage(candles));
            }
            fill.complete(candles);
            return candles;
        } catch (RuntimeException exception) {
            fill.complete(List.of());
            throw exception;
        } finally {
            fills.remove(cacheKey);
        }
    }

    private List<MarketCandleResult> fetchSegment(
            String symbol,
            MarketCandleInterval interval,
            Instant segmentStart,
            Instant segmentEnd
    ) {
        boolean acquired = false;
        try {
            acquired = providerLane.tryAcquire(PROVIDER_PERMIT_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
            if (!acquired) {
                record("market.history.bitget.timeout", symbol, interval, segmentStart, "bitget", "timeout");
                log.warn("Timed out waiting for Bitget historical lane. symbol={} interval={} segmentStart={}",
                        symbol, interval.value(), segmentStart);
                return List.of();
            }

            record("market.history.bitget.request", symbol, interval, segmentStart, "bitget", "request");
            List<MarketCandleResult> candles = providers.connector()
                    .marketDataGateway()
                    .loadHistoricalCandles(symbol, interval, segmentStart, segmentEnd, SEGMENT_SIZE)
                    .stream()
                    .map(this::toResult)
                    .filter(candle -> !candle.openTime().isBefore(segmentStart))
                    .filter(candle -> candle.openTime().isBefore(segmentEnd))
                    .sorted(Comparator.comparing(MarketCandleResult::openTime))
                    .toList();

            record(
                    candles.isEmpty() ? "market.history.bitget.empty" : "market.history.bitget.success",
                    symbol,
                    interval,
                    segmentStart,
                    "bitget",
                    candles.isEmpty() ? "empty" : "success"
            );
            return candles;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            record("market.history.bitget.timeout", symbol, interval, segmentStart, "bitget", "interrupted");
            return List.of();
        } catch (RuntimeException exception) {
            record("market.history.bitget.failure", symbol, interval, segmentStart, "bitget", "failure");
            log.warn("Failed to load Bitget historical candles. symbol={} interval={} segmentStart={}",
                    symbol, interval.value(), segmentStart, exception);
            return List.of();
        } finally {
            if (acquired) {
                providerLane.release();
            }
        }
    }

    private MarketCandleResult toResult(MarketHistoricalCandleSnapshot candle) {
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

    private List<MarketCandleResult> newest(int limit, List<MarketCandleResult> candles) {
        Map<Instant, MarketCandleResult> deduped = new LinkedHashMap<>();
        candles.stream()
                .sorted(Comparator.comparing(MarketCandleResult::openTime))
                .forEach(candle -> deduped.put(candle.openTime(), candle));

        List<MarketCandleResult> sorted = new ArrayList<>(deduped.values());
        if (sorted.size() <= limit) {
            return sorted;
        }
        return sorted.subList(sorted.size() - limit, sorted.size());
    }

    private Instant segmentStartContainingPreviousCandle(Instant toExclusive, MarketCandleInterval interval) {
        Duration duration = intervalDuration(interval);
        long previousCandleEpochMs = toExclusive.minus(duration).toEpochMilli();
        long segmentMillis = duration.toMillis() * SEGMENT_SIZE;
        return Instant.ofEpochMilli(Math.floorDiv(previousCandleEpochMs, segmentMillis) * segmentMillis);
    }

    private Instant segmentEnd(Instant segmentStart, MarketCandleInterval interval) {
        return segmentStart.plus(intervalDuration(interval).multipliedBy(SEGMENT_SIZE));
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
            case ONE_WEEK -> Duration.ofDays(7);
            case ONE_MONTH -> Duration.ofDays(30);
        };
    }

    private String cacheKey(String symbol, MarketCandleInterval interval, Instant segmentStart) {
        return "market:historical-candles:%s:%s:%d:size%d".formatted(
                symbol,
                providerGranularity(interval),
                segmentStart.toEpochMilli(),
                SEGMENT_SIZE
        );
    }

    private String providerGranularity(MarketCandleInterval interval) {
        return switch (interval) {
            case ONE_HOUR -> "1H";
            case FOUR_HOURS -> "4H";
            case TWELVE_HOURS -> "12H";
            default -> interval.value();
        };
    }

    private void record(
            String eventName,
            String symbol,
            MarketCandleInterval interval,
            Instant rangeStart,
            String source,
            String result
    ) {
        providers.telemetry().recordEvent(eventName, Map.of(
                "symbol", symbol,
                "interval", interval.value(),
                "range_bucket", rangeBucket(interval, rangeStart),
                "source", source,
                "result", result
        ));
    }

    private String rangeBucket(MarketCandleInterval interval, Instant rangeStart) {
        if (interval == MarketCandleInterval.ONE_MINUTE || interval == MarketCandleInterval.ONE_HOUR) {
            return YearMonth.from(rangeStart.atZone(ZoneOffset.UTC)).toString();
        }
        return String.valueOf(MarketTime.atStorageZone(rangeStart).getYear());
    }
}
