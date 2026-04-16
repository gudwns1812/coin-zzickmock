package coin.coinzzickmock.providers.connector;

import coin.coinzzickmock.feature.market.domain.MarketSnapshot;

import java.util.List;

public interface MarketDataGateway {
    List<MarketSnapshot> loadSupportedMarkets();

    MarketSnapshot loadMarket(String symbol);
}
