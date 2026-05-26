package coin.coinzzickmock.feature.market.application.dto;

import java.math.BigDecimal;

public record RealtimeMarketTradeSnapshot(
        String symbol,
        String tradeId,
        BigDecimal price,
        BigDecimal size,
        String side,
        MarketRealtimeSourceSnapshot source
) {
}
