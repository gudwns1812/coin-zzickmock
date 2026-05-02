package coin.coinzzickmock.feature.market.job;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryRetryProcessor;
import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryRetryRegistry;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class MarketHistoryRetrySchedulerTest {
    @Test
    void retriesPendingHistoryWithConfiguredFiveSecondDefaultDelay() throws NoSuchMethodException {
        Method method = MarketHistoryRetryScheduler.class.getDeclaredMethod("retryPendingHistory");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertEquals("${coin.market.history-retry-delay-ms:5000}", scheduled.fixedDelayString());
    }

    @Test
    void delegatesRetryWorkToApplicationProcessor() {
        RecordingRetryProcessor retryProcessor = new RecordingRetryProcessor();
        MarketHistoryRetryScheduler scheduler = new MarketHistoryRetryScheduler(retryProcessor);

        scheduler.retryPendingHistory();

        assertEquals(1, retryProcessor.calls());
    }

    private static class RecordingRetryProcessor extends MarketHistoryRetryProcessor {
        private int calls;

        private RecordingRetryProcessor() {
            super(new MarketHistoryRetryRegistry(), null);
        }

        @Override
        public void retryPending() {
            calls++;
        }

        private int calls() {
            return calls;
        }
    }
}
