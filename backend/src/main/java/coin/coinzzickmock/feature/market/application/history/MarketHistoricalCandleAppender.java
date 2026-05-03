package coin.coinzzickmock.feature.market.application.history;

import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketHistoricalCandleAppender {
    private final MarketHistoricalCandleCache historicalCandleCache;

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
