package coin.coinzzickmock.feature.market.api;

public record MarketSummaryResponse(
        String symbol,
        String displayName,
        double lastPrice,
        double markPrice,
        double fundingRate,
        double change24h
) {
}
