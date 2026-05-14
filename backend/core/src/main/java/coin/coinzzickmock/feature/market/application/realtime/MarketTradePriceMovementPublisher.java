package coin.coinzzickmock.feature.market.application.realtime;

@FunctionalInterface
public interface MarketTradePriceMovementPublisher {
    boolean publish(MarketTradePriceMovedEvent event);
}
