package coin.coinzzickmock.feature.market.history.application.repair;

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
    public boolean isTerminal() {
        return status == MarketHistoryRepairStatus.SUCCEEDED
                || status == MarketHistoryRepairStatus.FAILED;
    }

    public boolean isOneMinute() {
        return interval == MarketCandleInterval.ONE_MINUTE;
    }

    public boolean isOneHour() {
        return interval == MarketCandleInterval.ONE_HOUR;
    }
}
