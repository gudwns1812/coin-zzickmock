package coin.coinzzickmock.feature.market.catalog.application.dto;

import coin.coinzzickmock.feature.market.quote.application.dto.MarketPriceMovementDirection;

public record MarketSummaryUpdatedEvent(
        MarketSummaryResult result,
        Double previousLastPrice,
        MarketPriceMovementDirection direction
) {
    public MarketSummaryUpdatedEvent(MarketSummaryResult result) {
        this(result, null, MarketPriceMovementDirection.UNCHANGED);
    }

    public static MarketSummaryUpdatedEvent from(
            MarketSummaryResult previous,
            MarketSummaryResult current
    ) {
        Double previousLastPrice = previous == null ? null : previous.lastPrice();
        return new MarketSummaryUpdatedEvent(
                current,
                previousLastPrice,
                MarketPriceMovementDirection.between(previousLastPrice, current.lastPrice())
        );
    }

    public boolean hasPriceMovement() {
        return previousLastPrice != null && direction != MarketPriceMovementDirection.UNCHANGED;
    }
}
