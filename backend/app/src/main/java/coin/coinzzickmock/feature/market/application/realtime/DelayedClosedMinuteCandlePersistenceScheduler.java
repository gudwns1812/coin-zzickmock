package coin.coinzzickmock.feature.market.application.realtime;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class DelayedClosedMinuteCandlePersistenceScheduler {
    private final TaskScheduler taskScheduler;
    private final MarketHistoryPersistenceCoordinator marketHistoryPersistenceCoordinator;
    private final long persistenceDelayMs;
    private final Clock clock;
    private final Set<ClosedMinutePersistenceKey> inFlightClosedMinutes = ConcurrentHashMap.newKeySet();

    @Autowired
    public DelayedClosedMinuteCandlePersistenceScheduler(
            @Qualifier("taskScheduler") TaskScheduler taskScheduler,
            MarketHistoryPersistenceCoordinator marketHistoryPersistenceCoordinator,
            @Value("${coin.market.closed-minute-persistence-delay-ms:2500}") long persistenceDelayMs
    ) {
        this(taskScheduler, marketHistoryPersistenceCoordinator, persistenceDelayMs, Clock.systemUTC());
    }

    DelayedClosedMinuteCandlePersistenceScheduler(
            TaskScheduler taskScheduler,
            MarketHistoryPersistenceCoordinator marketHistoryPersistenceCoordinator,
            long persistenceDelayMs,
            Clock clock
    ) {
        if (persistenceDelayMs < 0) {
            throw new IllegalArgumentException("closed minute persistence delay must be non-negative");
        }
        this.taskScheduler = taskScheduler;
        this.marketHistoryPersistenceCoordinator = marketHistoryPersistenceCoordinator;
        this.persistenceDelayMs = persistenceDelayMs;
        this.clock = clock;
    }

    public void scheduleClosedMinutePersistence(
            List<String> symbols,
            Instant openTime,
            Instant closeTime
    ) {
        if (symbols == null || symbols.isEmpty() || openTime == null || closeTime == null) {
            return;
        }

        List<ClosedMinutePersistenceKey> acceptedKeys = claimInFlightKeys(symbols, openTime);
        if (acceptedKeys.isEmpty()) {
            return;
        }

        List<String> acceptedSymbols = acceptedKeys.stream()
                .map(ClosedMinutePersistenceKey::symbol)
                .toList();
        Runnable persistenceTask = () -> persistAndRelease(acceptedSymbols, openTime, closeTime, acceptedKeys);

        if (persistenceDelayMs == 0) {
            persistenceTask.run();
            return;
        }

        try {
            if (taskScheduler.schedule(persistenceTask, Instant.now(clock).plusMillis(persistenceDelayMs)) == null) {
                releaseInFlightKeys(acceptedKeys);
                throw new IllegalStateException("Closed minute candle persistence scheduling was rejected");
            }
        } catch (RuntimeException exception) {
            releaseInFlightKeys(acceptedKeys);
            throw exception;
        }
    }

    private List<ClosedMinutePersistenceKey> claimInFlightKeys(List<String> symbols, Instant openTime) {
        return distinctKeys(symbols, openTime).stream()
                .filter(inFlightClosedMinutes::add)
                .toList();
    }

    private List<ClosedMinutePersistenceKey> distinctKeys(List<String> symbols, Instant openTime) {
        LinkedHashMap<ClosedMinutePersistenceKey, ClosedMinutePersistenceKey> distinctKeys = new LinkedHashMap<>();
        symbols.stream()
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .map(symbol -> new ClosedMinutePersistenceKey(symbol, openTime))
                .forEach(key -> distinctKeys.putIfAbsent(key, key));
        return distinctKeys.values().stream().toList();
    }

    private void persistAndRelease(
            List<String> symbols,
            Instant openTime,
            Instant closeTime,
            List<ClosedMinutePersistenceKey> acceptedKeys
    ) {
        try {
            marketHistoryPersistenceCoordinator.persistClosedMinuteCandles(symbols, openTime, closeTime);
        } finally {
            releaseInFlightKeys(acceptedKeys);
        }
    }

    private void releaseInFlightKeys(List<ClosedMinutePersistenceKey> acceptedKeys) {
        acceptedKeys.forEach(inFlightClosedMinutes::remove);
    }

    private record ClosedMinutePersistenceKey(String symbol, Instant openTime) {
    }
}
