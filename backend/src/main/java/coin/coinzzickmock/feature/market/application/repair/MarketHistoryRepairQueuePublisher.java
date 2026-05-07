package coin.coinzzickmock.feature.market.application.repair;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketHistoryRepairQueuePublisher {
    private final MarketHistoryRepairQueue marketHistoryRepairQueue;

    @Retryable(
            retryFor = RuntimeException.class,
            maxAttemptsExpression = "${coin.market.history-repair.enqueue-retry.max-attempts:5}",
            backoff = @Backoff(delayExpression = "${coin.market.history-repair.enqueue-retry.delay-ms:1000}")
    )
    public void enqueue(long eventId) {
        marketHistoryRepairQueue.push(eventId);
    }

    @Recover
    void recover(RuntimeException exception, long eventId) {
        log.warn("Failed to enqueue market history repair event after retry exhaustion. eventId={}", eventId,
                exception);
    }
}
