package coin.coinzzickmock.feature.market.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record RealtimeMarketTickerSnapshot(
        String symbol,
        BigDecimal lastPrice,
        BigDecimal markPrice,
        BigDecimal indexPrice,
        BigDecimal fundingRate,
        Instant nextFundingTime,
        MarketRealtimeSourceSnapshot source
) {
}
