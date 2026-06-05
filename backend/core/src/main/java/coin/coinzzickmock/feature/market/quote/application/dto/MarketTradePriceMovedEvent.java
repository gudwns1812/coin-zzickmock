package coin.coinzzickmock.feature.market.quote.application.dto;

import java.time.Instant;

public record MarketTradePriceMovedEvent(
        String symbol,
        double previousLastPrice,
        double currentLastPrice,
        MarketPriceMovementDirection direction,
        Instant sourceEventTime,
        Instant receivedAt
) {
    public boolean hasPriceMovement() {
        return direction != MarketPriceMovementDirection.UNCHANGED
                && Double.compare(previousLastPrice, currentLastPrice) != 0;
    }
}
