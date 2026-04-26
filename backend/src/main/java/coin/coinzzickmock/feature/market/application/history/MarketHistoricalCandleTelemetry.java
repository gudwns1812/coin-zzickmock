package coin.coinzzickmock.feature.market.application.history;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketTime;
import coin.coinzzickmock.providers.Providers;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MarketHistoricalCandleTelemetry {
    private final Providers providers;

    public MarketHistoricalCandleTelemetry(Providers providers) {
        this.providers = providers;
    }

    public void record(
            String eventName,
            MarketHistoricalCandleSegment segment,
            String source,
            String result
    ) {
        providers.telemetry().recordEvent(eventName, Map.of(
                "symbol", segment.symbol(),
                "interval", segment.interval().value(),
                "range_bucket", rangeBucket(segment),
                "source", source,
                "result", result
        ));
    }

    private String rangeBucket(MarketHistoricalCandleSegment segment) {
        if (segment.interval() == MarketCandleInterval.ONE_MINUTE
                || segment.interval() == MarketCandleInterval.ONE_HOUR) {
            return YearMonth.from(segment.startInclusive().atZone(ZoneOffset.UTC)).toString();
        }
        return String.valueOf(MarketTime.atStorageZone(segment.startInclusive()).getYear());
    }
}
