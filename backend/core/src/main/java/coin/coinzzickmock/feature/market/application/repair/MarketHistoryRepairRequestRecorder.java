package coin.coinzzickmock.feature.market.application.repair;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketHistoryRepairRequestRecorder {
    private final MarketHistoryRepairEventRepository marketHistoryRepairEventRepository;
    private final AfterCommitEventPublisher afterCommitEventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MarketHistoryRepairEvent recordOneMinuteFailure(
            String symbol,
            Instant openTime,
            Instant closeTime,
            RuntimeException cause
    ) {
        return recordFailure(symbol, MarketCandleInterval.ONE_MINUTE, openTime, closeTime, cause);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public MarketHistoryRepairEvent recordOneHourFailure(
            String symbol,
            Instant openTime,
            Instant closeTime,
            RuntimeException cause
    ) {
        return recordFailure(symbol, MarketCandleInterval.ONE_HOUR, openTime, closeTime, cause);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public MarketHistoryRepairEvent recordOneHourFailureAfterCurrentCommit(
            String symbol,
            Instant openTime,
            Instant closeTime,
            RuntimeException cause
    ) {
        return recordFailure(symbol, MarketCandleInterval.ONE_HOUR, openTime, closeTime, cause);
    }

    private MarketHistoryRepairEvent recordFailure(
            String symbol,
            MarketCandleInterval interval,
            Instant openTime,
            Instant closeTime,
            RuntimeException cause
    ) {
        MarketHistoryRepairEvent event = marketHistoryRepairEventRepository.queueRepair(
                symbol,
                interval,
                openTime,
                closeTime,
                reason(cause)
        );
        afterCommitEventPublisher.publish(new MarketHistoryRepairQueueRequestedEvent(event.id()));
        log.warn(
                "Queued market history repair event. eventId={} symbol={} interval={} openTime={} closeTime={}",
                event.id(), symbol, interval.value(), openTime, closeTime, cause
        );
        return event;
    }

    private String reason(RuntimeException cause) {
        if (cause == null || cause.getMessage() == null || cause.getMessage().isBlank()) {
            return "market history persistence failed";
        }
        return cause.getMessage();
    }
}
