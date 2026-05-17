package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.application.dto.MarketTradePriceMovedEvent;

@FunctionalInterface
public interface MarketTradePriceMovementPublisher {
    boolean publish(MarketTradePriceMovedEvent event);
}
