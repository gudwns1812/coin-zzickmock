package coin.coinzzickmock.feature.market.application.result;

public record MarketSummaryResult(
        String symbol,
        String displayName,
        double lastPrice,
        double markPrice,
        double indexPrice,
        double fundingRate,
        double change24h
) {
}
