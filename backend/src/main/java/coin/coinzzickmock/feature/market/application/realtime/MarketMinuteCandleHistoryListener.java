package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketMinuteCandleHistoryListener {
    private final MarketSnapshotStore marketSnapshotStore;
    private final MarketHistoryPersistenceCoordinator marketHistoryPersistenceCoordinator;
    private final MarketHistoryRetryRegistry marketHistoryRetryRegistry;

    @EventListener
    public void onMinuteClosed(MarketMinuteClosedEvent event) {
        if (!marketSnapshotStore.hasSupportedMarkets()) {
            return;
        }

        List<String> symbols = marketSnapshotStore.getSupportedMarkets().stream()
                .map(MarketSummaryResult::symbol)
                .toList();
        List<MarketHistoryPersistenceResult> results = marketHistoryPersistenceCoordinator.persistClosedMinuteCandles(
                symbols,
                event.openTime(),
                event.closeTime()
        );
        results.forEach(this::updateRetryRegistry);
    }

    private void updateRetryRegistry(MarketHistoryPersistenceResult result) {
        if (result.saved()) {
            marketHistoryRetryRegistry.markSaved(result.symbol(), result.openTime(), result.closeTime());
            return;
        }

        if (result.shouldRetry()) {
            marketHistoryRetryRegistry.markPending(result.symbol(), result.openTime(), result.closeTime());
        }
    }
}
