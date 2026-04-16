package coin.coinzzickmock.feature.market.domain;

public record MarketSnapshot(
        String symbol,
        String displayName,
        double lastPrice,
        double markPrice,
        double indexPrice,
        double fundingRate,
        double change24h
) {
}
