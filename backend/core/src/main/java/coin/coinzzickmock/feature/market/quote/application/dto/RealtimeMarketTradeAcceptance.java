package coin.coinzzickmock.feature.market.quote.application.dto;

import java.util.Optional;

public record RealtimeMarketTradeAcceptance(boolean accepted, Optional<MarketTradePriceMovedEvent> movement) {
    public static RealtimeMarketTradeAcceptance rejected() {
        return new RealtimeMarketTradeAcceptance(false, Optional.empty());
    }
}
