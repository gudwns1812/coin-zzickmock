package coin.coinzzickmock.feature.market.api;

import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;

public record MarketSummaryResponse(
        String symbol,
        String displayName,
        double lastPrice,
        double markPrice,
        double indexPrice,
        double fundingRate,
        double change24h
) {
    public static MarketSummaryResponse from(MarketSummaryResult result) {
        return new MarketSummaryResponse(
                result.symbol(),
                result.displayName(),
                result.lastPrice(),
                result.markPrice(),
                result.indexPrice(),
                result.fundingRate(),
                result.change24h()
        );
    }
}
