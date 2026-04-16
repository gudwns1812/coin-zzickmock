package coin.coinzzickmock.feature.market.application.result;

public record MarketSummaryResult(
        String symbol,
        String displayName,
        double lastPrice,
        double markPrice,
        double fundingRate,
        double change24h
) {
}
