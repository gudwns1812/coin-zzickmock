package coin.coinzzickmock.feature.market.web;

public interface MarketCurrentCandleBootstrapper {
    boolean bootstrapIfNeeded(String symbol, String interval);
}
