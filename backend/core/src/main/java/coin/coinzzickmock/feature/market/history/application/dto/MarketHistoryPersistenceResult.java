package coin.coinzzickmock.feature.market.history.application.dto;

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

    public boolean isSaved() {
        return status == MarketHistoryPersistenceStatus.PERSISTED
                || status == MarketHistoryPersistenceStatus.ALREADY_RECORDED;
    }

    public boolean isPersisted() {
        return status == MarketHistoryPersistenceStatus.PERSISTED;
    }
}
