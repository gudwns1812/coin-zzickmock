package coin.coinzzickmock.feature.market.application.repair;

import coin.coinzzickmock.feature.market.application.repository.MarketHistoryRepository;
import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryRecorder;
import coin.coinzzickmock.feature.market.domain.MarketHistoryCandle;
import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.feature.market.application.gateway.MarketDataGateway;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketHistoryRepairProcessor {
    private final MarketHistoryRepairQueue marketHistoryRepairQueue;
    private final MarketHistoryRepairEventRepository marketHistoryRepairEventRepository;
    private final MarketHistoryRepairQueuePublisher marketHistoryRepairQueuePublisher;
    private final MarketHistoryRepository marketHistoryRepository;
    private final MarketDataGateway marketDataGateway;
    private final MarketHistoryRecorder marketHistoryRecorder;

    public boolean processNext(Duration timeout) {
        return marketHistoryRepairQueue.pop(timeout)
                .map(this::processQueuedEvent)
                .orElse(false);
    }

    private boolean processQueuedEvent(long eventId) {
        MarketHistoryRepairEvent event = marketHistoryRepairEventRepository.findById(eventId).orElse(null);
        if (event == null || event.terminal()) {
            return true;
        }
        if (!marketHistoryRepairEventRepository.markProcessing(event.id())) {
            return true;
        }

        process(event);
        return true;
    }

    private void process(MarketHistoryRepairEvent event) {
        try {
            if (event.oneMinute()) {
                repairMinute(event);
                return;
            }
            if (event.oneHour()) {
                repairHour(event);
                return;
            }
            marketHistoryRepairEventRepository.markFailed(event.id(), "unsupported repair interval");
        } catch (RuntimeException exception) {
            marketHistoryRepairEventRepository.markFailed(event.id(), reason(exception));
            log.warn("Market history repair processing failed. eventId={}", event.id(), exception);
        }
    }

    private void repairMinute(MarketHistoryRepairEvent event) {
        List<MarketMinuteCandleSnapshot> minuteCandles = marketDataGateway.loadMinuteCandles(
                event.symbol(),
                event.openTime(),
                event.closeTime()
        );
        if (minuteCandles == null || minuteCandles.isEmpty()) {
            throw new MarketHistoryPersistenceAttemptException("repair minute candle history is not ready");
        }

        Map<String, Boolean> saveResults = marketHistoryRecorder.recordClosedMinuteCandlesBySymbol(
                Map.of(event.symbol(), minuteCandles),
                event.openTime(),
                event.closeTime()
        );
        if (!Boolean.TRUE.equals(saveResults.get(event.symbol()))) {
            throw new MarketHistoryPersistenceAttemptException("repair minute candle history was not persisted");
        }

        enqueueWaitingHourlyRepairs(event);
        marketHistoryRepairEventRepository.markSucceeded(event.id());
    }

    private void enqueueWaitingHourlyRepairs(MarketHistoryRepairEvent event) {
        Instant hourlyOpenTime = event.openTime().truncatedTo(java.time.temporal.ChronoUnit.HOURS);
        marketHistoryRepairEventRepository.queueWaitingHourlyRepairEvents(event.symbol(), hourlyOpenTime)
                .forEach(marketHistoryRepairQueuePublisher::enqueue);
    }

    private void repairHour(MarketHistoryRepairEvent event) {
        Long symbolId = symbolId(event.symbol());
        if (symbolId == null) {
            throw new MarketHistoryPersistenceAttemptException("repair hourly candle symbol is unknown");
        }
        if (!hasCompleteMinuteCoverage(symbolId, event.openTime(), event.closeTime())) {
            marketHistoryRepairEventRepository.markWaitingForMinutes(event.id(), "waiting for complete minute coverage");
            return;
        }
        if (!marketHistoryRecorder.rebuildCompletedHourlyCandle(symbolId, event.openTime())) {
            throw new MarketHistoryPersistenceAttemptException("repair hourly candle was not rebuilt");
        }
        marketHistoryRepairEventRepository.markSucceeded(event.id());
    }

    private Long symbolId(String symbol) {
        return marketHistoryRepository.findSymbolIdsBySymbols(List.of(symbol)).get(symbol);
    }

    private boolean hasCompleteMinuteCoverage(long symbolId, Instant openTime, Instant closeTime) {
        List<MarketHistoryCandle> minuteCandles = marketHistoryRepository.findMinuteCandles(symbolId, openTime, closeTime);
        return minuteCandles.size() == 60;
    }

    private String reason(RuntimeException exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? "market history repair failed"
                : exception.getMessage();
    }
}
