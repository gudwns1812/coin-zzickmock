package coin.coinzzickmock.feature.market.latestwindow.application.dto;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;

public record MarketLatestCandleWindowKey(
        String symbol,
        MarketCandleInterval interval,
        int limit,
        Instant latestOutputOpenTime
) {
    public String cacheKey() {
        return "market:latest-candles:%s:%s:limit%d:closedUntil:%d".formatted(
                symbol,
                interval.value(),
                limit,
                latestOutputOpenTime.toEpochMilli()
        );
    }
}
