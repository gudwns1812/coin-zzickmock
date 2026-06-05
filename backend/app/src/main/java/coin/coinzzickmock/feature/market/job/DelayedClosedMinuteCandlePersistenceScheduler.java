package coin.coinzzickmock.feature.market.job;

import coin.coinzzickmock.feature.market.candle.application.implement.ClosedMinuteCandlePersistenceScheduler;
import coin.coinzzickmock.feature.market.candle.application.implement.DelayedClosedMinuteCandlePersistenceCoordinator;
import coin.coinzzickmock.feature.market.candle.application.implement.DelayedClosedMinuteCandlePersistenceCoordinator.ClosedMinutePersistenceTask;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class DelayedClosedMinuteCandlePersistenceScheduler implements ClosedMinuteCandlePersistenceScheduler {
    private final TaskScheduler taskScheduler;
    private final DelayedClosedMinuteCandlePersistenceCoordinator delayedClosedMinuteCandlePersistenceCoordinator;
    private final long persistenceDelayMs;
    private final Clock clock;

    @Autowired
    public DelayedClosedMinuteCandlePersistenceScheduler(
            @Qualifier("taskScheduler") TaskScheduler taskScheduler,
            DelayedClosedMinuteCandlePersistenceCoordinator delayedClosedMinuteCandlePersistenceCoordinator,
            @Value("${coin.market.closed-minute-persistence-delay-ms:2500}") long persistenceDelayMs
    ) {
        this(taskScheduler, delayedClosedMinuteCandlePersistenceCoordinator, persistenceDelayMs, Clock.systemUTC());
    }

    public DelayedClosedMinuteCandlePersistenceScheduler(
            TaskScheduler taskScheduler,
            DelayedClosedMinuteCandlePersistenceCoordinator delayedClosedMinuteCandlePersistenceCoordinator,
            long persistenceDelayMs,
            Clock clock
    ) {
        if (persistenceDelayMs < 0) {
            throw new IllegalArgumentException("closed minute persistence delay must be non-negative");
        }
        this.taskScheduler = taskScheduler;
        this.delayedClosedMinuteCandlePersistenceCoordinator = delayedClosedMinuteCandlePersistenceCoordinator;
        this.persistenceDelayMs = persistenceDelayMs;
        this.clock = clock;
    }

    @Override
    public void scheduleClosedMinutePersistence(
            List<String> symbols,
            Instant openTime,
            Instant closeTime
    ) {
        delayedClosedMinuteCandlePersistenceCoordinator
                .claimClosedMinutePersistence(symbols, openTime, closeTime)
                .ifPresent(this::scheduleClaimedPersistence);
    }

    private void scheduleClaimedPersistence(ClosedMinutePersistenceTask persistenceTask) {
        if (persistenceDelayMs == 0) {
            persistenceTask.persistAndRelease();
            return;
        }

        try {
            if (taskScheduler.schedule(persistenceTask::persistAndRelease,
                    Instant.now(clock).plusMillis(persistenceDelayMs)) == null) {
                persistenceTask.release();
                throw new IllegalStateException("Closed minute candle persistence scheduling was rejected");
            }
        } catch (RuntimeException exception) {
            persistenceTask.release();
            throw exception;
        }
    }
}
