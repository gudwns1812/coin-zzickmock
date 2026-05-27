package coin.coinzzickmock.feature.market.application.repository;

import java.time.Instant;

public record MarketHistoryStartupBackfillCursor(
        long symbolId,
        String symbol,
        Instant latestPersistedMinuteOpenTime
) {
}
