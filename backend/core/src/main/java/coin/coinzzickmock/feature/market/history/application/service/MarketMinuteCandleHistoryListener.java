package coin.coinzzickmock.feature.market.history.application.service;

import coin.coinzzickmock.feature.market.catalog.application.implement.MarketSnapshotStore;
import coin.coinzzickmock.feature.market.candle.application.dto.MarketMinuteClosedEvent;
import coin.coinzzickmock.feature.market.candle.application.implement.ClosedMinuteCandlePersistenceScheduler;
import coin.coinzzickmock.feature.market.catalog.application.dto.MarketSummaryResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketMinuteCandleHistoryListener {
    private final MarketSnapshotStore marketSnapshotStore;
    private final ClosedMinuteCandlePersistenceScheduler delayedClosedMinuteCandlePersistenceScheduler;

    @EventListener
    public void onMinuteClosed(MarketMinuteClosedEvent event) {
        if (!marketSnapshotStore.hasSupportedMarkets()) {
            return;
        }

        List<String> symbols = marketSnapshotStore.getSupportedMarkets().stream()
                .map(MarketSummaryResult::symbol)
                .toList();
        delayedClosedMinuteCandlePersistenceScheduler.scheduleClosedMinutePersistence(
                symbols,
                event.openTime(),
                event.closeTime()
        );
    }
}
