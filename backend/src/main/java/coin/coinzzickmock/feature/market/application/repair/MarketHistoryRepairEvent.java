package coin.coinzzickmock.feature.market.application.repair;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;

public record MarketHistoryRepairEvent(
        long id,
        String symbol,
        MarketCandleInterval interval,
        Instant openTime,
        Instant closeTime,
        MarketHistoryRepairStatus status,
        int attemptCount
) {
    public boolean terminal() {
        return status == MarketHistoryRepairStatus.SUCCEEDED
                || status == MarketHistoryRepairStatus.FAILED;
    }

    public boolean oneMinute() {
        return interval == MarketCandleInterval.ONE_MINUTE;
    }

    public boolean oneHour() {
        return interval == MarketCandleInterval.ONE_HOUR;
    }
}
