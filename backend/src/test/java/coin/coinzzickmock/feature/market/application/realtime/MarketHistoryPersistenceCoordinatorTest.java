package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MarketHistoryPersistenceCoordinatorTest {
    private static final Instant OPEN_TIME = Instant.parse("2026-04-17T06:00:00Z");
    private static final Instant CLOSE_TIME = Instant.parse("2026-04-17T06:01:00Z");

    @Test
    void returnsAlreadyRecordedWithoutCallingProviderOrRecorderAgain() {
        RecordingMinuteCandleGateway gateway = new RecordingMinuteCandleGateway();
        MarketHistoryRecorder recorder = mock(MarketHistoryRecorder.class);
        when(recorder.recordClosedMinuteCandlesBySymbol(anyMap(), eq(OPEN_TIME), eq(CLOSE_TIME)))
                .thenReturn(Map.of("BTCUSDT", true));
        MarketHistoryPersistenceCoordinator coordinator = new MarketHistoryPersistenceCoordinator(gateway, recorder);

        List<MarketHistoryPersistenceResult> firstResults = coordinator.persistClosedMinuteCandles(
                List.of("BTCUSDT"),
                OPEN_TIME,
                CLOSE_TIME
        );
        List<MarketHistoryPersistenceResult> secondResults = coordinator.persistClosedMinuteCandles(
                List.of("BTCUSDT"),
                OPEN_TIME,
                CLOSE_TIME
        );

        assertThat(firstResults).singleElement()
                .extracting(MarketHistoryPersistenceResult::status)
                .isEqualTo(MarketHistoryPersistenceStatus.PERSISTED);
        assertThat(secondResults).singleElement()
                .extracting(MarketHistoryPersistenceResult::status)
                    .isEqualTo(MarketHistoryPersistenceStatus.ALREADY_RECORDED);
        assertThat(gateway.loadMinuteCandleCalls).isEqualTo(1);
        verify(recorder).recordClosedMinuteCandlesBySymbol(anyMap(), eq(OPEN_TIME), eq(CLOSE_TIME));
    }

    @Test
    void returnsEmptyWhenProviderHasNoClosedMinuteCandle() {
        RecordingMinuteCandleGateway gateway = new RecordingMinuteCandleGateway();
        gateway.minuteCandles = List.of();
        MarketHistoryRecorder recorder = mock(MarketHistoryRecorder.class);
        MarketHistoryPersistenceCoordinator coordinator = new MarketHistoryPersistenceCoordinator(gateway, recorder);

        List<MarketHistoryPersistenceResult> results = coordinator.persistClosedMinuteCandles(
                List.of("BTCUSDT"),
                OPEN_TIME,
                CLOSE_TIME
        );

        assertThat(results).singleElement()
                .extracting(MarketHistoryPersistenceResult::status)
                .isEqualTo(MarketHistoryPersistenceStatus.EMPTY);
        verify(recorder, never()).recordClosedMinuteCandlesBySymbol(any(), any(), any());
    }

    @Test
    void returnsFailedWhenProviderLoadFails() {
        RecordingMinuteCandleGateway gateway = new RecordingMinuteCandleGateway();
        gateway.loadFailure = new IllegalStateException("provider unavailable");
        MarketHistoryRecorder recorder = mock(MarketHistoryRecorder.class);
        MarketHistoryPersistenceCoordinator coordinator = new MarketHistoryPersistenceCoordinator(gateway, recorder);

        List<MarketHistoryPersistenceResult> results = coordinator.persistClosedMinuteCandles(
                List.of("BTCUSDT"),
                OPEN_TIME,
                CLOSE_TIME
        );

        assertThat(results).singleElement()
                .extracting(MarketHistoryPersistenceResult::status)
                .isEqualTo(MarketHistoryPersistenceStatus.FAILED);
        verify(recorder, never()).recordClosedMinuteCandlesBySymbol(any(), any(), any());
    }

    @Test
    void returnsFailedWhenRecorderDoesNotPersistLoadedCandle() {
        RecordingMinuteCandleGateway gateway = new RecordingMinuteCandleGateway();
        MarketHistoryRecorder recorder = mock(MarketHistoryRecorder.class);
        when(recorder.recordClosedMinuteCandlesBySymbol(anyMap(), eq(OPEN_TIME), eq(CLOSE_TIME)))
                .thenReturn(Map.of("BTCUSDT", false));
        MarketHistoryPersistenceCoordinator coordinator = new MarketHistoryPersistenceCoordinator(gateway, recorder);

        List<MarketHistoryPersistenceResult> results = coordinator.persistClosedMinuteCandles(
                List.of("BTCUSDT"),
                OPEN_TIME,
                CLOSE_TIME
        );

        assertThat(results).singleElement()
                .extracting(MarketHistoryPersistenceResult::status)
                .isEqualTo(MarketHistoryPersistenceStatus.FAILED);
    }

    private static MarketMinuteCandleSnapshot minuteCandle() {
        return new MarketMinuteCandleSnapshot(
                OPEN_TIME,
                OPEN_TIME.plus(1, ChronoUnit.MINUTES),
                101000.0,
                101500.0,
                100500.0,
                101250.0,
                10.0,
                1012500.0
        );
    }

    private static class RecordingMinuteCandleGateway extends coin.coinzzickmock.testsupport.TestMarketDataGateway {
        private int loadMinuteCandleCalls;
        private List<MarketMinuteCandleSnapshot> minuteCandles = List.of(minuteCandle());
        private RuntimeException loadFailure;

        @Override
        public List<MarketSnapshot> loadSupportedMarkets() {
            return List.of();
        }

        @Override
        public MarketSnapshot loadMarket(String symbol) {
            return null;
        }

        @Override
        public List<MarketMinuteCandleSnapshot> loadMinuteCandles(
                String symbol,
                Instant fromInclusive,
                Instant toExclusive
        ) {
            loadMinuteCandleCalls++;
            if (loadFailure != null) {
                throw loadFailure;
            }
            return minuteCandles;
        }
    }
}
