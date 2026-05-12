package coin.coinzzickmock.providers.connector;

public record ProviderMarketSnapshot(
        String symbol,
        String displayName,
        double lastPrice,
        double markPrice,
        double indexPrice,
        double fundingRate,
        double change24h,
        double turnover24hUsdt
) {
}
