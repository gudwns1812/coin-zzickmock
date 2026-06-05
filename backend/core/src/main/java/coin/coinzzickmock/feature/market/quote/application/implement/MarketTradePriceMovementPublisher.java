package coin.coinzzickmock.feature.market.quote.application.implement;

import coin.coinzzickmock.feature.market.quote.application.dto.MarketTradePriceMovedEvent;

@FunctionalInterface
public interface MarketTradePriceMovementPublisher {
    boolean publish(MarketTradePriceMovedEvent event);
}
