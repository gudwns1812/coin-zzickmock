package coin.coinzzickmock.feature.market.application.history;

import coin.coinzzickmock.feature.market.application.result.MarketCandleResult;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import coin.coinzzickmock.providers.Providers;
import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class MarketHistoryLookupTelemetry {
    private final Providers providers;
    private final Clock clock;

    @Autowired
    public MarketHistoryLookupTelemetry(Providers providers) {
        this(providers, Clock.systemUTC());
    }

    MarketHistoryLookupTelemetry(Providers providers, Clock clock) {
        this.providers = providers;
        this.clock = clock;
    }

    public void recordDbLookup(
            String symbol,
            MarketCandleInterval interval,
            Instant beforeOpenTime,
            List<MarketCandleResult> persistedCandles,
            int limit
    ) {
        String result = dbLookupResult(persistedCandles, limit);
        Instant rangeStart = beforeOrEarliest(beforeOpenTime, persistedCandles);
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
                .orElseGet(() -> beforeOpenTime == null ? Instant.now(clock) : beforeOpenTime);
    }

    private String rangeBucket(MarketCandleInterval interval, Instant rangeStart) {
        if (interval == MarketCandleInterval.ONE_MINUTE || interval == MarketCandleInterval.ONE_HOUR) {
            return YearMonth.from(rangeStart.atZone(ZoneOffset.UTC)).toString();
        }
        return String.valueOf(MarketTime.atStorageZone(rangeStart).getYear());
    }
}
