package coin.coinzzickmock.feature.market.history.application.service;

import coin.coinzzickmock.feature.market.history.application.service.MarketHistoryPersistenceCoordinator;
import coin.coinzzickmock.feature.market.history.application.dto.MarketHistoryPersistenceResult;
import coin.coinzzickmock.feature.market.history.application.dto.MarketHistoryPersistenceStatus;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.feature.market.history.application.repair.MarketClosedMinuteCandlePersistence;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
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
    void preventsDuplicatePersistenceWhileFirstAttemptIsRunning() throws Exception {
        MarketClosedMinuteCandlePersistence persistence = Mockito.mock(MarketClosedMinuteCandlePersistence.class);
        CountDownLatch persistEntered = new CountDownLatch(1);
        CountDownLatch releasePersist = new CountDownLatch(1);
        when(persistence.persist("BTCUSDT", OPEN_TIME, CLOSE_TIME))
                .thenAnswer(invocation -> {
                    persistEntered.countDown();
                    assertThat(releasePersist.await(1, TimeUnit.SECONDS)).isTrue();
                    return result(MarketHistoryPersistenceStatus.PERSISTED);
                });
        MarketHistoryPersistenceCoordinator coordinator = new MarketHistoryPersistenceCoordinator(persistence);

        CompletableFuture<List<MarketHistoryPersistenceResult>> first = CompletableFuture.supplyAsync(() ->
                coordinator.persistClosedMinuteCandles(List.of("BTCUSDT"), OPEN_TIME, CLOSE_TIME));
        assertThat(persistEntered.await(1, TimeUnit.SECONDS)).isTrue();

        List<MarketHistoryPersistenceResult> secondResults = coordinator.persistClosedMinuteCandles(
                List.of("BTCUSDT"),
                OPEN_TIME,
                CLOSE_TIME
        );
        releasePersist.countDown();
        List<MarketHistoryPersistenceResult> firstResults = first.join();

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
    void retriesAfterFailedPersistenceAttempt() {
        MarketClosedMinuteCandlePersistence persistence = Mockito.mock(MarketClosedMinuteCandlePersistence.class);
        when(persistence.persist("BTCUSDT", OPEN_TIME, CLOSE_TIME))
                .thenReturn(
                        result(MarketHistoryPersistenceStatus.FAILED),
                        result(MarketHistoryPersistenceStatus.PERSISTED)
                );
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
                .isEqualTo(MarketHistoryPersistenceStatus.FAILED);
        assertThat(secondResults).singleElement()
                .extracting(MarketHistoryPersistenceResult::status)
                .isEqualTo(MarketHistoryPersistenceStatus.PERSISTED);
        verify(persistence, Mockito.times(2)).persist("BTCUSDT", OPEN_TIME, CLOSE_TIME);
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
