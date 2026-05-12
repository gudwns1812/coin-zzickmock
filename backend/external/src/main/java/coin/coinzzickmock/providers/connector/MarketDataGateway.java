package coin.coinzzickmock.providers.connector;

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

    /**
     * Loads at most {@code limit} historical candles for {@code symbol} and {@code interval}.
     *
     * <p>The returned candles are the newest candles inside {@code [fromInclusive, toExclusive)} after provider
     * alignment. Implementations return an empty list when {@code limit <= 0} or the time range is empty and may cap
     * excessive limits to protect provider and process resources.</p>
     */
    List<ProviderMarketHistoricalCandleSnapshot> loadHistoricalCandles(
            String symbol,
            ProviderMarketCandleInterval interval,
            Instant fromInclusive,
            Instant toExclusive,
            int limit
    );
}
