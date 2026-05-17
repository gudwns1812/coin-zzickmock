package coin.coinzzickmock.feature.market.application.implement;

import coin.coinzzickmock.feature.market.application.dto.MarketRealtimeSourceSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.math.BigDecimal;
import java.time.Instant;

public record RealtimeMarketCandleState(
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
        MarketRealtimeSourceSnapshot source
) {
}
