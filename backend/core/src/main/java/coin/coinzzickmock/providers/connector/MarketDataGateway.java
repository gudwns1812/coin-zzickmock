package coin.coinzzickmock.providers.connector;

import java.time.Instant;
import java.util.List;

public interface MarketDataGateway {
    /**
     * Loads currently supported markets from the external provider.
     *
     * @return a non-null, immutable or defensive-copy list of {@link ProviderMarketSnapshot}; empty when no markets are
     * supported. Implementations may throw runtime provider exceptions for transport or provider failures.
     */
    List<ProviderMarketSnapshot> loadSupportedMarkets();

    /**
     * Loads one market snapshot by provider symbol such as {@code BTCUSDT}.
     *
     * @param symbol uppercase provider market symbol; must not be null or blank
     * @return a non-null {@link ProviderMarketSnapshot}; implementations may return a provider fallback snapshot or throw
     * a runtime provider exception when the market cannot be loaded
     * @throws IllegalArgumentException when {@code symbol} is blank or otherwise invalid
     */
    ProviderMarketSnapshot loadMarket(String symbol);

    /**
     * Loads minute candles inside {@code [fromInclusive, toExclusive)} for {@code symbol}.
     *
     * @param symbol uppercase provider market symbol; must not be null or blank
     * @param fromInclusive start time, inclusive; must not be null
     * @param toExclusive end time, exclusive; must not be null
     * @return a non-null, immutable or defensive-copy list of {@link ProviderMarketMinuteCandleSnapshot}; empty when the
     * range is empty or provider data is unavailable
     * @throws IllegalArgumentException when the symbol or time range is invalid
     */
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
     *
     * @param symbol uppercase provider market symbol; must not be null or blank
     * @param interval provider candle interval; must not be null
     * @param fromInclusive start time, inclusive; must not be null
     * @param toExclusive end time, exclusive; must not be null
     * @param limit maximum number of candles to return; non-positive values return an empty list
     * @return a non-null, immutable or defensive-copy list of {@link ProviderMarketHistoricalCandleSnapshot}
     * @throws IllegalArgumentException when the symbol, interval, or time range is invalid
     */
    List<ProviderMarketHistoricalCandleSnapshot> loadHistoricalCandles(
            String symbol,
            ProviderMarketCandleInterval interval,
            Instant fromInclusive,
            Instant toExclusive,
            int limit
    );
}
