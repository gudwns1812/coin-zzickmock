package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.feature.market.application.repair.MarketClosedMinuteCandlePersistence;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class MarketHistoryPersistenceCoordinatorTest {
    private static final Instant OPEN_TIME = Instant.parse("2026-04-17T06:00:00Z");
    private static final Instant CLOSE_TIME = Instant.parse("2026-04-17T06:01:00Z");

    @Test
    void returnsAlreadyRecordedWithoutCallingPersistenceAgain() {
        MarketClosedMinuteCandlePersistence persistence = Mockito.mock(MarketClosedMinuteCandlePersistence.class);
        when(persistence.persist("BTCUSDT", OPEN_TIME, CLOSE_TIME))
                .thenReturn(result(MarketHistoryPersistenceStatus.PERSISTED));
        MarketHistoryPersistenceCoordinator coordinator = new MarketHistoryPersistenceCoordinator(persistence);

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
        verify(persistence).persist("BTCUSDT", OPEN_TIME, CLOSE_TIME);
    }

    @Test
    void keepsFailedResultFromRetryablePersistenceDelegate() {
        MarketClosedMinuteCandlePersistence persistence = Mockito.mock(MarketClosedMinuteCandlePersistence.class);
        when(persistence.persist("BTCUSDT", OPEN_TIME, CLOSE_TIME))
                .thenReturn(result(MarketHistoryPersistenceStatus.FAILED));
        MarketHistoryPersistenceCoordinator coordinator = new MarketHistoryPersistenceCoordinator(persistence);

        List<MarketHistoryPersistenceResult> results = coordinator.persistClosedMinuteCandles(
                List.of("BTCUSDT"),
                OPEN_TIME,
                CLOSE_TIME
        );

        assertThat(results).singleElement()
                .extracting(MarketHistoryPersistenceResult::status)
                .isEqualTo(MarketHistoryPersistenceStatus.FAILED);
    }

    @Test
    void ignoresBlankDuplicateSymbols() {
        MarketClosedMinuteCandlePersistence persistence = Mockito.mock(MarketClosedMinuteCandlePersistence.class);
        when(persistence.persist("BTCUSDT", OPEN_TIME, CLOSE_TIME))
                .thenReturn(result(MarketHistoryPersistenceStatus.PERSISTED));
        MarketHistoryPersistenceCoordinator coordinator = new MarketHistoryPersistenceCoordinator(persistence);

        List<MarketHistoryPersistenceResult> results = coordinator.persistClosedMinuteCandles(
                Arrays.asList("BTCUSDT", "", "BTCUSDT", null),
                OPEN_TIME,
                CLOSE_TIME
        );

        assertThat(results).hasSize(1);
        verify(persistence).persist("BTCUSDT", OPEN_TIME, CLOSE_TIME);
    }

    @Test
    void returnsEmptyWhenInputIsInvalid() {
        MarketClosedMinuteCandlePersistence persistence = Mockito.mock(MarketClosedMinuteCandlePersistence.class);
        MarketHistoryPersistenceCoordinator coordinator = new MarketHistoryPersistenceCoordinator(persistence);

        assertThat(coordinator.persistClosedMinuteCandles(List.of(), OPEN_TIME, CLOSE_TIME)).isEmpty();
        assertThat(coordinator.persistClosedMinuteCandles(List.of("BTCUSDT"), null, CLOSE_TIME)).isEmpty();
        verify(persistence, never()).persist("BTCUSDT", OPEN_TIME, CLOSE_TIME);
    }

    private static MarketHistoryPersistenceResult result(MarketHistoryPersistenceStatus status) {
        return new MarketHistoryPersistenceResult("BTCUSDT", OPEN_TIME, CLOSE_TIME, status);
    }
}
