package coin.coinzzickmock.testsupport;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoricalCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.market.application.gateway.MarketDataGateway;
import java.time.Instant;
import java.util.List;

public abstract class TestMarketDataGateway implements MarketDataGateway {
    @Override
    public List<MarketSnapshot> loadSupportedMarkets() {
        return List.of();
    }

    @Override
    public MarketSnapshot loadMarket(String symbol) {
        throw new UnsupportedOperationException("loadMarket is not implemented for this test fake");
    }

    @Override
    public List<MarketMinuteCandleSnapshot> loadMinuteCandles(
            String symbol,
            Instant fromInclusive,
            Instant toExclusive
    ) {
        return List.of();
    }

    @Override
    public List<MarketHistoricalCandleSnapshot> loadHistoricalCandles(
            String symbol,
            MarketCandleInterval interval,
            Instant fromInclusive,
            Instant toExclusive,
            int limit
    ) {
        return List.of();
    }
}
