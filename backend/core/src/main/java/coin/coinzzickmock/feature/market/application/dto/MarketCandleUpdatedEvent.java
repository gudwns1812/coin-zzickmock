package coin.coinzzickmock.feature.market.application.dto;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Duration;
import java.util.Objects;

public record MarketCandleUpdatedEvent(
        String symbol,
        String interval,
        MarketCandleResult candle
) {
    public MarketCandleUpdatedEvent(String symbol) {
        this(symbol, null, null);
    }

    public static MarketCandleUpdatedEvent from(RealtimeMarketCandleUpdate update) {
        Objects.requireNonNull(update, "update must not be null");
        MarketCandleInterval interval = update.interval();
        return new MarketCandleUpdatedEvent(
                update.symbol(),
                interval.value(),
                new MarketCandleResult(
                        update.openTime(),
                        update.openTime().plus(durationOf(interval)),
                        update.openPrice().doubleValue(),
                        update.highPrice().doubleValue(),
                        update.lowPrice().doubleValue(),
                        update.closePrice().doubleValue(),
                        update.baseVolume().doubleValue()
                )
        );
    }

    public boolean hasPayload() {
        return interval != null && !interval.isBlank() && candle != null;
    }

    private static Duration durationOf(MarketCandleInterval interval) {
        return switch (interval) {
            case ONE_MINUTE -> Duration.ofMinutes(1);
            case THREE_MINUTES -> Duration.ofMinutes(3);
            case FIVE_MINUTES -> Duration.ofMinutes(5);
            case FIFTEEN_MINUTES -> Duration.ofMinutes(15);
            case ONE_HOUR -> Duration.ofHours(1);
            case FOUR_HOURS -> Duration.ofHours(4);
            case TWELVE_HOURS -> Duration.ofHours(12);
            case ONE_DAY -> Duration.ofDays(1);
            case ONE_WEEK -> Duration.ofDays(7);
            case ONE_MONTH -> Duration.ofDays(31);
        };
    }
}
