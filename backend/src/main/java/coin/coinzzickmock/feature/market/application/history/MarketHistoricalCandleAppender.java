package coin.coinzzickmock.feature.market.application.history;

import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MarketHistoricalCandleAppender {
    private final MarketHistoricalCandleCache historicalCandleCache;
    private final Clock clock;

    @Autowired
    public MarketHistoricalCandleAppender(MarketHistoricalCandleCache historicalCandleCache) {
        this(historicalCandleCache, Clock.systemUTC());
    }

    MarketHistoricalCandleAppender(MarketHistoricalCandleCache historicalCandleCache, Clock clock) {
        this.historicalCandleCache = historicalCandleCache;
        this.clock = clock;
    }

    public List<MarketCandleResult> appendOlderCandles(
            String symbol,
            MarketCandleInterval interval,
            Instant beforeOpenTime,
            int limit,
            List<MarketCandleResult> persistedCandles
    ) {
        if (persistedCandles.size() >= limit) {
            return persistedCandles;
        }

        Instant supplementalToExclusive = supplementalToExclusive(interval, beforeOpenTime, persistedCandles);
        int missingCount = limit - persistedCandles.size();

        List<MarketCandleResult> supplementalCandles = historicalCandleCache.loadOlderCandles(
                symbol,
                interval,
                supplementalToExclusive,
                missingCount
        );

        return mergeSupplementalCandles(persistedCandles, supplementalCandles, limit);
    }

    private Instant supplementalToExclusive(
            MarketCandleInterval interval,
            Instant beforeOpenTime,
            List<MarketCandleResult> persistedCandles
    ) {
        return persistedCandles.stream()
                .map(MarketCandleResult::openTime)
                .min(Instant::compareTo)
                .orElseGet(() -> safeEmptyPersistedSupplementalBoundary(interval, beforeOpenTime));
    }

    private Instant safeEmptyPersistedSupplementalBoundary(MarketCandleInterval interval, Instant beforeOpenTime) {
        if (beforeOpenTime != null) {
            return beforeOpenTime;
        }
        if (interval == MarketCandleInterval.ONE_HOUR) {
            return MarketTime.truncate(clock.instant(), ChronoUnit.HOURS);
        }
        return clock.instant();
    }

    private List<MarketCandleResult> mergeSupplementalCandles(
            List<MarketCandleResult> persistedCandles,
            List<MarketCandleResult> supplementalCandles,
            int limit
    ) {
        Map<Instant, MarketCandleResult> candlesByOpenTime = new LinkedHashMap<>();
        supplementalCandles.stream()
                .sorted(Comparator.comparing(MarketCandleResult::openTime))
                .forEach(candle -> candlesByOpenTime.put(candle.openTime(), candle));
        persistedCandles.stream()
                .sorted(Comparator.comparing(MarketCandleResult::openTime))
                .forEach(candle -> candlesByOpenTime.put(candle.openTime(), candle));

        List<MarketCandleResult> merged = candlesByOpenTime.values().stream()
                .sorted(Comparator.comparing(MarketCandleResult::openTime))
                .toList();
        if (merged.size() <= limit) {
            return merged;
        }
        return merged.subList(merged.size() - limit, merged.size());
    }
}
