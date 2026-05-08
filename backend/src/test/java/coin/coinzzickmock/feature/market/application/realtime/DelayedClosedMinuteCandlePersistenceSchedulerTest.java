package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.scheduling.TaskScheduler;

class DelayedClosedMinuteCandlePersistenceSchedulerTest {
    private static final Instant NOW = Instant.parse("2026-04-17T06:01:01Z");
    private static final Instant OPEN_TIME = Instant.parse("2026-04-17T06:00:00Z");
    private static final Instant CLOSE_TIME = Instant.parse("2026-04-17T06:01:00Z");

    @Test
    void schedulesPersistenceAfterConfiguredDelayWithoutCallingCoordinatorBeforeTaskRuns() {
        TaskScheduler taskScheduler = schedulableTaskScheduler();
        MarketHistoryPersistenceCoordinator coordinator = Mockito.mock(MarketHistoryPersistenceCoordinator.class);
        DelayedClosedMinuteCandlePersistenceScheduler scheduler = scheduler(taskScheduler, coordinator, 2500);

        scheduler.scheduleClosedMinutePersistence(List.of("BTCUSDT"), OPEN_TIME, CLOSE_TIME);

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(taskScheduler).schedule(taskCaptor.capture(), instantCaptor.capture());
        assertThat(instantCaptor.getValue()).isEqualTo(Instant.parse("2026-04-17T06:01:03.500Z"));
        verify(coordinator, never()).persistClosedMinuteCandles(List.of("BTCUSDT"), OPEN_TIME, CLOSE_TIME);

        taskCaptor.getValue().run();

        verify(coordinator).persistClosedMinuteCandles(List.of("BTCUSDT"), OPEN_TIME, CLOSE_TIME);
    }

    @Test
    void runsCoordinatorImmediatelyWhenDelayIsZero() {
        TaskScheduler taskScheduler = Mockito.mock(TaskScheduler.class);
        MarketHistoryPersistenceCoordinator coordinator = Mockito.mock(MarketHistoryPersistenceCoordinator.class);
        DelayedClosedMinuteCandlePersistenceScheduler scheduler = scheduler(taskScheduler, coordinator, 0);

        scheduler.scheduleClosedMinutePersistence(List.of("BTCUSDT"), OPEN_TIME, CLOSE_TIME);

        verify(taskScheduler, never()).schedule(Mockito.any(Runnable.class), Mockito.any(Instant.class));
        verify(coordinator).persistClosedMinuteCandles(List.of("BTCUSDT"), OPEN_TIME, CLOSE_TIME);
    }

    @Test
    void suppressesDuplicateInFlightEventsBySymbolAndOpenTimeUntilTaskCompletes() {
        TaskScheduler taskScheduler = schedulableTaskScheduler();
        MarketHistoryPersistenceCoordinator coordinator = Mockito.mock(MarketHistoryPersistenceCoordinator.class);
        DelayedClosedMinuteCandlePersistenceScheduler scheduler = scheduler(taskScheduler, coordinator, 2500);

        scheduler.scheduleClosedMinutePersistence(List.of("BTCUSDT"), OPEN_TIME, CLOSE_TIME);
        scheduler.scheduleClosedMinutePersistence(List.of("BTCUSDT"), OPEN_TIME, CLOSE_TIME);

        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).schedule(taskCaptor.capture(), Mockito.any(Instant.class));

        taskCaptor.getValue().run();
        scheduler.scheduleClosedMinutePersistence(List.of("BTCUSDT"), OPEN_TIME, CLOSE_TIME);

        verify(taskScheduler, Mockito.times(2)).schedule(Mockito.any(Runnable.class), Mockito.any(Instant.class));
    }

    @Test
    void releasesInFlightKeyWhenCoordinatorThrows() {
        TaskScheduler taskScheduler = schedulableTaskScheduler();
        MarketHistoryPersistenceCoordinator coordinator = Mockito.mock(MarketHistoryPersistenceCoordinator.class);
        DelayedClosedMinuteCandlePersistenceScheduler scheduler = scheduler(taskScheduler, coordinator, 2500);
        doThrow(new IllegalStateException("boom"))
                .when(coordinator)
                .persistClosedMinuteCandles(List.of("BTCUSDT"), OPEN_TIME, CLOSE_TIME);

        scheduler.scheduleClosedMinutePersistence(List.of("BTCUSDT"), OPEN_TIME, CLOSE_TIME);
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).schedule(taskCaptor.capture(), Mockito.any(Instant.class));

        assertThatThrownBy(() -> taskCaptor.getValue().run())
                .isInstanceOf(IllegalStateException.class);
        scheduler.scheduleClosedMinutePersistence(List.of("BTCUSDT"), OPEN_TIME, CLOSE_TIME);

        verify(taskScheduler, Mockito.times(2)).schedule(Mockito.any(Runnable.class), Mockito.any(Instant.class));
    }

    @Test
    void filtersBlankAndDuplicateSymbolsBeforeScheduling() {
        TaskScheduler taskScheduler = Mockito.mock(TaskScheduler.class);
        MarketHistoryPersistenceCoordinator coordinator = Mockito.mock(MarketHistoryPersistenceCoordinator.class);
        DelayedClosedMinuteCandlePersistenceScheduler scheduler = scheduler(taskScheduler, coordinator, 0);

        scheduler.scheduleClosedMinutePersistence(List.of("BTCUSDT", "", "BTCUSDT", "ETHUSDT"), OPEN_TIME,
                CLOSE_TIME);

        verify(coordinator).persistClosedMinuteCandles(List.of("BTCUSDT", "ETHUSDT"), OPEN_TIME, CLOSE_TIME);
    }

    @Test
    void rejectsNegativeDelay() {
        TaskScheduler taskScheduler = Mockito.mock(TaskScheduler.class);
        MarketHistoryPersistenceCoordinator coordinator = Mockito.mock(MarketHistoryPersistenceCoordinator.class);

        assertThatThrownBy(() -> scheduler(taskScheduler, coordinator, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }

    private static TaskScheduler schedulableTaskScheduler() {
        TaskScheduler taskScheduler = Mockito.mock(TaskScheduler.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<Object> future = Mockito.mock(ScheduledFuture.class);
        doReturn(future).when(taskScheduler).schedule(Mockito.any(Runnable.class), Mockito.any(Instant.class));
        return taskScheduler;
    }

    private static DelayedClosedMinuteCandlePersistenceScheduler scheduler(
            TaskScheduler taskScheduler,
            MarketHistoryPersistenceCoordinator coordinator,
            long delayMs
    ) {
        return new DelayedClosedMinuteCandlePersistenceScheduler(
                taskScheduler,
                coordinator,
                delayMs,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }
}
