package coin.coinzzickmock.feature.market.application.implement;

import coin.coinzzickmock.feature.market.application.dto.MarketTradePriceMovedEvent;

@FunctionalInterface
public interface MarketTradePriceMovementPublisher {
    boolean publish(MarketTradePriceMovedEvent event);
}
