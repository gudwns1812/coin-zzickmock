package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import coin.coinzzickmock.feature.market.domain.MarketMinuteCandleSnapshot;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import java.time.Instant;
import java.util.List;
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
        MarketHistoryPersistenceCoordinator coordinator = new MarketHistoryPersistenceCoordinator(
                new ThrowingMinuteCandleGateway(),
                new MarketHistoryRecorder(null)
        );

        List<MarketHistoryPersistenceResult> results = coordinator.persistClosedMinuteCandles(
                List.of("BTCUSDT"),
                OPEN_TIME,
                CLOSE_TIME
        );

        assertThat(results).singleElement()
                .extracting(MarketHistoryPersistenceResult::status)
                .isEqualTo(MarketHistoryPersistenceStatus.FAILED);
        assertThat(output)
                .contains("Failed to load closed market minute candle history")
                .contains("symbol=BTCUSDT")
                .contains("provider unavailable");
    }

    @Test
    void logsRetryQueueStateTransitions(CapturedOutput output) {
        MarketHistoryRetryRegistry retryRegistry = new MarketHistoryRetryRegistry();

        retryRegistry.markPending("BTCUSDT", OPEN_TIME, CLOSE_TIME);
        retryRegistry.markPending("BTCUSDT", OPEN_TIME, CLOSE_TIME);
        retryRegistry.markSaved("BTCUSDT", OPEN_TIME, CLOSE_TIME);

        assertThat(output)
                .contains("Queued market history candle retry")
                .contains("Resolved market history candle retry");
    }

    private static class ThrowingMinuteCandleGateway extends coin.coinzzickmock.testsupport.TestMarketDataGateway {
        @Override
        public List<MarketSnapshot> loadSupportedMarkets() {
            return List.of();
        }

        @Override
        public MarketSnapshot loadMarket(String symbol) {
            return null;
        }

        @Override
        public List<MarketMinuteCandleSnapshot> loadMinuteCandles(
                String symbol,
                Instant fromInclusive,
                Instant toExclusive
        ) {
            throw new IllegalStateException("provider unavailable");
        }
    }
}
