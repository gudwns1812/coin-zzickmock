package coin.coinzzickmock.feature.market.history.application.repository;

import java.time.Instant;

public record MarketHistoryStartupBackfillCursor(
        long symbolId,
        String symbol,
        Instant latestPersistedMinuteOpenTime
) {
}
