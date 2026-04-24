package coin.coinzzickmock.feature.market.application.realtime;

import java.time.Instant;

public record MarketHistoryPersistenceResult(
        String symbol,
        Instant openTime,
        Instant closeTime,
        MarketHistoryPersistenceStatus status
) {
    public boolean shouldRetry() {
        return status == MarketHistoryPersistenceStatus.EMPTY
                || status == MarketHistoryPersistenceStatus.FAILED;
    }

    public boolean saved() {
        return status == MarketHistoryPersistenceStatus.SAVED;
    }
}
