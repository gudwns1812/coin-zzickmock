package coin.coinzzickmock.feature.market.web;

public interface MarketSseStreamStrategy {
    MarketSseStreamKind kind();

    void open(MarketSseStreamRequest request);
}
