package coin.coinzzickmock.providers.connector;

import coin.coinzzickmock.providers.connector.ProviderMarketCandleInterval;
import coin.coinzzickmock.providers.connector.ProviderMarketHistoricalCandleSnapshot;
import coin.coinzzickmock.providers.connector.ProviderMarketMinuteCandleSnapshot;
import coin.coinzzickmock.providers.connector.ProviderMarketSnapshot;
import java.time.Instant;
import java.util.List;

public interface MarketDataGateway {
    List<ProviderMarketSnapshot> loadSupportedMarkets();

    ProviderMarketSnapshot loadMarket(String symbol);

    List<ProviderMarketMinuteCandleSnapshot> loadMinuteCandles(
            String symbol,
            Instant fromInclusive,
            Instant toExclusive
    );

    List<ProviderMarketHistoricalCandleSnapshot> loadHistoricalCandles(
            String symbol,
            ProviderMarketCandleInterval interval,
            Instant fromInclusive,
            Instant toExclusive,
            int limit
    );
}
