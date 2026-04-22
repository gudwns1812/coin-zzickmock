package coin.coinzzickmock.feature.market.application.query;

public record GetMarketCandlesQuery(
        String symbol,
        String interval,
        Integer limit
) {
}
