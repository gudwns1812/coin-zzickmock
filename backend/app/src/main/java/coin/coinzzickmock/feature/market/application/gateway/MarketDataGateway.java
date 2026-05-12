package coin.coinzzickmock.feature.market.application.gateway;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketHistoricalCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import java.time.Instant;
import java.util.List;

public interface MarketDataGateway {
    List<MarketSnapshot> loadSupportedMarkets();

    MarketSnapshot loadMarket(String symbol);

    List<MarketMinuteCandleSnapshot> loadMinuteCandles(
            String symbol,
            Instant fromInclusive,
            Instant toExclusive
    );

    /**
     * Loads at most {@code limit} newest historical candles inside {@code [fromInclusive, toExclusive)} after provider
     * alignment. Implementations return an empty list for non-positive limits or empty ranges.
     */
    List<MarketHistoricalCandleSnapshot> loadHistoricalCandles(
            String symbol,
            MarketCandleInterval interval,
            Instant fromInclusive,
            Instant toExclusive,
            int limit
    );
}
