package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;

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
        RecordingMarketHistoryRecorder recorder = new RecordingMarketHistoryRecorder();
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
        assertThat(recorder.recordClosedMinuteCandleCalls).isEqualTo(1);
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

    private static class RecordingMinuteCandleGateway implements MarketDataGateway {
        private int loadMinuteCandleCalls;

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
            return List.of(minuteCandle());
        }
    }

    private static class RecordingMarketHistoryRecorder extends MarketHistoryRecorder {
        private int recordClosedMinuteCandleCalls;

        private RecordingMarketHistoryRecorder() {
            super(null);
        }

        @Override
        public Map<String, Boolean> recordClosedMinuteCandlesBySymbol(
                Map<String, List<MarketMinuteCandleSnapshot>> minuteCandlesBySymbol,
                Instant openTime,
                Instant closeTime
        ) {
            recordClosedMinuteCandleCalls++;
            return Map.of("BTCUSDT", true);
        }
    }
}
