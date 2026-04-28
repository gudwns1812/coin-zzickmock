package coin.coinzzickmock.feature.market.domain;

public record MarketSnapshot(
        String symbol,
        String displayName,
        double lastPrice,
        double markPrice,
        double indexPrice,
        double fundingRate,
        double change24h,
        double turnover24hUsdt
) {
    public MarketSnapshot(
            String symbol,
            String displayName,
            double lastPrice,
            double markPrice,
            double indexPrice,
            double fundingRate,
            double change24h
    ) {
        this(symbol, displayName, lastPrice, markPrice, indexPrice, fundingRate, change24h, 0.0);
    }
}
