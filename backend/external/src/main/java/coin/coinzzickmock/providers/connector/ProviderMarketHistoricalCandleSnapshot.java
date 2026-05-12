package coin.coinzzickmock.providers.connector;

import java.time.Instant;

public record ProviderMarketHistoricalCandleSnapshot(
        Instant openTime,
        Instant closeTime,
        double openPrice,
        double highPrice,
        double lowPrice,
        double closePrice,
        double volume,
        double quoteVolume
) {
}
