package coin.coinzzickmock.feature.market.application.realtime;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MarketMinuteCandleHistoryListenerTest {
    private static final Instant OPEN_TIME = Instant.parse("2026-04-17T06:00:00Z");
    private static final Instant CLOSE_TIME = Instant.parse("2026-04-17T06:01:00Z");

    @Test
    void delegatesSupportedSymbolsToDelayedScheduler() {
        MarketSnapshotStore marketSnapshotStore = Mockito.mock(MarketSnapshotStore.class);
        DelayedClosedMinuteCandlePersistenceScheduler scheduler = Mockito.mock(
                DelayedClosedMinuteCandlePersistenceScheduler.class);
        when(marketSnapshotStore.hasSupportedMarkets()).thenReturn(true);
        when(marketSnapshotStore.getSupportedMarkets()).thenReturn(List.of(
                summary("BTCUSDT"),
                summary("ETHUSDT")
        ));
        MarketMinuteCandleHistoryListener listener = new MarketMinuteCandleHistoryListener(
                marketSnapshotStore,
                scheduler
        );

        listener.onMinuteClosed(new MarketMinuteClosedEvent(OPEN_TIME, CLOSE_TIME));

        verify(scheduler).scheduleClosedMinutePersistence(List.of("BTCUSDT", "ETHUSDT"), OPEN_TIME, CLOSE_TIME);
    }

    @Test
    void returnsWithoutSchedulingWhenSupportedMarketsAreUnavailable() {
        MarketSnapshotStore marketSnapshotStore = Mockito.mock(MarketSnapshotStore.class);
        DelayedClosedMinuteCandlePersistenceScheduler scheduler = Mockito.mock(
                DelayedClosedMinuteCandlePersistenceScheduler.class);
        when(marketSnapshotStore.hasSupportedMarkets()).thenReturn(false);
        MarketMinuteCandleHistoryListener listener = new MarketMinuteCandleHistoryListener(
                marketSnapshotStore,
                scheduler
        );

        listener.onMinuteClosed(new MarketMinuteClosedEvent(OPEN_TIME, CLOSE_TIME));

        verify(scheduler, never()).scheduleClosedMinutePersistence(Mockito.anyList(), Mockito.any(), Mockito.any());
    }

    private static MarketSummaryResult summary(String symbol) {
        return new MarketSummaryResult(symbol, symbol + " Perpetual", 100.0, 100.0, 100.0, 0.0, 0.0);
    }
}
