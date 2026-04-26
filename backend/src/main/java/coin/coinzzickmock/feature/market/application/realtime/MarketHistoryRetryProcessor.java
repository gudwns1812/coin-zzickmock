package coin.coinzzickmock.feature.market.application.realtime;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketHistoryRetryProcessor {
    private final MarketHistoryRetryRegistry marketHistoryRetryRegistry;
    private final MarketHistoryPersistenceCoordinator marketHistoryPersistenceCoordinator;

    public void retryPending() {
        List<PendingMinuteCandleRetry> pendingRetries = marketHistoryRetryRegistry.pendingRetries();
        if (pendingRetries.isEmpty()) {
            return;
        }

        log.debug("Retrying pending market history candles. count={}", pendingRetries.size());
        pendingRetries.forEach(this::retryPending);
    }

    private void retryPending(PendingMinuteCandleRetry pendingRetry) {
        List<MarketHistoryPersistenceResult> results = marketHistoryPersistenceCoordinator.persistClosedMinuteCandles(
                List.of(pendingRetry.symbol()),
                pendingRetry.openTime(),
                pendingRetry.closeTime()
        );
        results.forEach(this::updateRetryResult);
    }

    private void updateRetryResult(MarketHistoryPersistenceResult result) {
        if (result.saved()) {
            marketHistoryRetryRegistry.markSaved(result.symbol(), result.openTime(), result.closeTime());
            return;
        }

        log.debug(
                "Market history candle retry remains pending. symbol={} openTime={} closeTime={} status={}",
                result.symbol(), result.openTime(), result.closeTime(), result.status()
        );
    }
}
