package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.math.BigDecimal;
import java.time.Instant;

public record BitgetWebSocketCandleEvent(
        String symbol,
        MarketCandleInterval interval,
        Instant openTime,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal closePrice,
        BigDecimal baseVolume,
        BigDecimal quoteVolume,
        BigDecimal usdtVolume,
        Instant sourceEventTime,
        Instant receivedAt
) implements BitgetWebSocketMarketEvent {
}
