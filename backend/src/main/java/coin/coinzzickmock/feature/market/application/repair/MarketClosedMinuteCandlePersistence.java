package coin.coinzzickmock.feature.market.application.repair;

import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryPersistenceResult;
import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryPersistenceStatus;
import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryRecorder;
import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MarketClosedMinuteCandlePersistence {
    private final MarketDataGateway marketDataGateway;
    private final MarketHistoryRecorder marketHistoryRecorder;
    private final MarketHistoryRepairRequestRecorder marketHistoryRepairRequestRecorder;

    @Retryable(
            retryFor = MarketHistoryPersistenceAttemptException.class,
            maxAttemptsExpression = "${coin.market.history-repair.persistence-retry.max-attempts:3}",
            backoff = @Backoff(delayExpression = "${coin.market.history-repair.persistence-retry.delay-ms:100}")
    )
    public MarketHistoryPersistenceResult persist(String symbol, Instant openTime, Instant closeTime) {
        List<MarketMinuteCandleSnapshot> minuteCandles = loadClosedMinuteCandles(symbol, openTime, closeTime);
        if (minuteCandles.isEmpty()) {
            throw new MarketHistoryPersistenceAttemptException("closed minute candle history is not ready");
        }

        if (!recordClosedMinuteCandles(symbol, minuteCandles, openTime, closeTime)) {
            throw new MarketHistoryPersistenceAttemptException("closed minute candle history was not persisted");
        }

        return new MarketHistoryPersistenceResult(
                symbol,
                openTime,
                closeTime,
                MarketHistoryPersistenceStatus.PERSISTED
        );
    }

    @Recover
    MarketHistoryPersistenceResult recover(
            MarketHistoryPersistenceAttemptException exception,
            String symbol,
            Instant openTime,
            Instant closeTime
    ) {
        marketHistoryRepairRequestRecorder.recordOneMinuteFailure(symbol, openTime, closeTime, exception);
        return new MarketHistoryPersistenceResult(
                symbol,
                openTime,
                closeTime,
                MarketHistoryPersistenceStatus.FAILED
        );
    }

    private List<MarketMinuteCandleSnapshot> loadClosedMinuteCandles(
            String symbol,
            Instant openTime,
            Instant closeTime
    ) {
        try {
            List<MarketMinuteCandleSnapshot> minuteCandles = marketDataGateway.loadMinuteCandles(symbol, openTime, closeTime);
            return minuteCandles == null ? List.of() : minuteCandles;
        } catch (Exception exception) {
            log.warn("Failed to load closed market minute candle history. symbol={} openTime={} closeTime={}",
                    symbol, openTime, closeTime, exception);
            throw new MarketHistoryPersistenceAttemptException("closed minute candle history load failed", exception);
        }
    }

    private boolean recordClosedMinuteCandles(
            String symbol,
            List<MarketMinuteCandleSnapshot> minuteCandles,
            Instant openTime,
            Instant closeTime
    ) {
        try {
            Map<String, Boolean> saveResults = marketHistoryRecorder.recordClosedMinuteCandlesBySymbol(
                    Map.of(symbol, minuteCandles),
                    openTime,
                    closeTime
            );
            return Boolean.TRUE.equals(saveResults.get(symbol));
        } catch (RuntimeException exception) {
            log.warn("Failed to persist closed market minute candle history. symbol={} openTime={} closeTime={}",
                    symbol, openTime, closeTime, exception);
            throw new MarketHistoryPersistenceAttemptException("closed minute candle history persist failed", exception);
        }
    }
}
