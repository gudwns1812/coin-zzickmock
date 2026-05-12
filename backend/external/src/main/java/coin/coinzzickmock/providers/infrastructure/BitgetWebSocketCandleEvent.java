package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.providers.connector.ProviderMarketCandleInterval;
import java.math.BigDecimal;
import java.time.Instant;

public record BitgetWebSocketCandleEvent(
        String symbol,
        ProviderMarketCandleInterval interval,
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
