package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import coin.coinzzickmock.feature.market.application.repair.MarketClosedMinuteCandlePersistence;
import coin.coinzzickmock.feature.market.application.repair.MarketHistoryPersistenceAttemptException;
import coin.coinzzickmock.feature.market.application.repair.MarketHistoryRepairEvent;
import coin.coinzzickmock.feature.market.application.repair.MarketHistoryRepairEventRepository;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.market.application.repair.MarketHistoryRepairQueueRequestedEvent;
import coin.coinzzickmock.feature.market.application.repair.MarketHistoryRepairQueuePublisher;
import coin.coinzzickmock.feature.market.application.repair.MarketHistoryRepairRequestRecorder;
import coin.coinzzickmock.feature.market.application.repair.MarketHistoryRepairStatus;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class MarketHistoryLoggingTest {
    private static final Instant OPEN_TIME = Instant.parse("2026-04-17T06:00:00Z");
    private static final Instant CLOSE_TIME = Instant.parse("2026-04-17T06:01:00Z");

    @Test
    void logsProviderFailureWhenClosedMinuteHistoryCannotBeLoaded(CapturedOutput output) {
        MarketClosedMinuteCandlePersistence persistence = new MarketClosedMinuteCandlePersistence(
                new ThrowingMinuteCandleGateway(),
                mock(MarketHistoryRecorder.class),
                mock(MarketHistoryRepairRequestRecorder.class)
        );

        assertThatThrownBy(() -> persistence.persist("BTCUSDT", OPEN_TIME, CLOSE_TIME))
                .isInstanceOf(MarketHistoryPersistenceAttemptException.class);
        assertThat(output)
                .contains("Failed to load closed market minute candle history")
                .contains("symbol=BTCUSDT")
                .contains("provider unavailable");
    }

    @Test
    void logsRepairQueueStateTransition(CapturedOutput output) {
        AfterCommitEventPublisher afterCommitEventPublisher = mock(AfterCommitEventPublisher.class);
        MarketHistoryRepairRequestRecorder recorder = new MarketHistoryRepairRequestRecorder(
                new RecordingRepairEventRepository(),
                afterCommitEventPublisher
        );

        recorder.recordOneMinuteFailure(
                "BTCUSDT",
                OPEN_TIME,
                CLOSE_TIME,
                new MarketHistoryPersistenceAttemptException("provider unavailable")
        );

        assertThat(output)
                .contains("Queued market history repair event")
                .contains("symbol=BTCUSDT")
                .contains("interval=1m");
        verify(afterCommitEventPublisher).publish(new MarketHistoryRepairQueueRequestedEvent(1L));
    }

    private static class ThrowingMinuteCandleGateway extends coin.coinzzickmock.testsupport.TestMarketDataGateway {
        @Override
        public List<MarketSnapshot> loadSupportedMarkets() {
            return List.of(new MarketSnapshot("BTCUSDT", "BTCUSDT Perpetual", 101000, 100950, 100900, 0.0001, 3.2));
        }

        @Override
        public List<coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot> loadMinuteCandles(
                String symbol,
                Instant fromInclusive,
                Instant toExclusive
        ) {
            throw new IllegalStateException("provider unavailable");
        }
    }

    private static class RecordingRepairEventRepository implements MarketHistoryRepairEventRepository {
        @Override
        public MarketHistoryRepairEvent queueRepair(
                String symbol,
                MarketCandleInterval interval,
                Instant openTime,
                Instant closeTime,
                String reason
        ) {
            return new MarketHistoryRepairEvent(1L, symbol, interval, openTime, closeTime, MarketHistoryRepairStatus.QUEUED, 0);
        }

        @Override
        public Optional<MarketHistoryRepairEvent> findById(long eventId) {
            return Optional.empty();
        }

        @Override
        public boolean markProcessing(long eventId) {
            return false;
        }

        @Override
        public void markQueued(long eventId, String reason) {
        }

        @Override
        public void markWaitingForMinutes(long eventId, String reason) {
        }

        @Override
        public void markSucceeded(long eventId) {
        }

        @Override
        public void markFailed(long eventId, String reason) {
        }

        @Override
        public List<Long> queueWaitingHourlyRepairEvents(String symbol, Instant hourlyOpenTime) {
            return List.of();
        }
    }
}
