package coin.coinzzickmock.providers.connector;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;

public record MarketHistoricalCandleGranularity(String value) {
    public static MarketHistoricalCandleGranularity from(MarketCandleInterval interval) {
        return switch (interval) {
            case ONE_HOUR -> new MarketHistoricalCandleGranularity("1H");
            case FOUR_HOURS -> new MarketHistoricalCandleGranularity("4H");
            case TWELVE_HOURS -> new MarketHistoricalCandleGranularity("12H");
            default -> new MarketHistoricalCandleGranularity(interval.value());
        };
    }
}
