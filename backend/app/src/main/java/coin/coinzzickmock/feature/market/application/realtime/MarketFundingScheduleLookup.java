package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.domain.FundingSchedule;

/**
 * Retrieves the funding cadence configured for a market symbol.
 */
public interface MarketFundingScheduleLookup {
    /**
     * Returns the {@link FundingSchedule} for a market symbol.
     *
     * <p>Implementations should return the default USDT perpetual schedule when {@code symbol}
     * is {@code null}, blank, or not configured. The returned value is always non-null.
     *
     * @param symbol market symbol such as {@code BTCUSDT}; null, blank, and unknown symbols use the default schedule
     * @return non-null funding schedule to use for countdown and market metadata
     * @throws RuntimeException when the backing lookup fails unexpectedly
     */
    FundingSchedule scheduleFor(String symbol);
}
