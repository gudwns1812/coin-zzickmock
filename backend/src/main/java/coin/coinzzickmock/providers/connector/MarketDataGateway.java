package coin.coinzzickmock.providers.connector;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoricalCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import java.time.Instant;
import java.util.List;

public interface MarketDataGateway {
    List<MarketSnapshot> loadSupportedMarkets();

    MarketSnapshot loadMarket(String symbol);

    default List<MarketMinuteCandleSnapshot> loadMinuteCandles(
            String symbol,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        return List.of();
    }

    default List<MarketHistoricalCandleSnapshot> loadHistoricalCandles(
            String symbol,
            MarketCandleInterval interval,
            Instant fromInclusive,
            Instant toExclusive,
            int limit
    ) {
        return List.of();
    }
}
