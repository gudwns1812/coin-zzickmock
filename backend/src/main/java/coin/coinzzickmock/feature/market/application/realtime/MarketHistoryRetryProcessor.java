package coin.coinzzickmock.feature.market.application.realtime;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketHistoryRetryProcessor {
    private final MarketHistoryRetryRegistry marketHistoryRetryRegistry;
    private final MarketHistoryPersistenceCoordinator marketHistoryPersistenceCoordinator;

    public void retryPending() {
        marketHistoryRetryRegistry.pendingRetries()
                .forEach(this::retryPending);
    }

    private void retryPending(PendingMinuteCandleRetry pendingRetry) {
        List<MarketHistoryPersistenceResult> results = marketHistoryPersistenceCoordinator.persistClosedMinuteCandles(
                List.of(pendingRetry.symbol()),
                pendingRetry.openTime(),
                pendingRetry.closeTime()
        );
        results.stream()
                .filter(MarketHistoryPersistenceResult::saved)
                .forEach(result -> marketHistoryRetryRegistry.markSaved(
                        result.symbol(),
                        result.openTime(),
                        result.closeTime()
                ));
    }
}
