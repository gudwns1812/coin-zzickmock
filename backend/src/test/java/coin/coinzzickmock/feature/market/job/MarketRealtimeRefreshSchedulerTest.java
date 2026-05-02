package coin.coinzzickmock.feature.market.job;

import static org.junit.jupiter.api.Assertions.assertEquals;

import coin.coinzzickmock.feature.market.application.realtime.MarketRealtimeFeed;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class MarketRealtimeRefreshSchedulerTest {
    @Test
    void refreshesSupportedMarketsWithConfiguredOneSecondDefaultDelay() throws NoSuchMethodException {
        Method method = MarketRealtimeRefreshScheduler.class.getDeclaredMethod("refreshSupportedMarkets");
        Scheduled scheduled = method.getAnnotation(Scheduled.class);

        assertEquals("${coin.market.refresh-delay-ms:1000}", scheduled.fixedDelayString());
    }

    @Test
    void delegatesRefreshWorkToApplicationFeed() {
        RecordingMarketRealtimeFeed feed = new RecordingMarketRealtimeFeed();
        MarketRealtimeRefreshScheduler scheduler = new MarketRealtimeRefreshScheduler(feed);

        scheduler.refreshSupportedMarkets();

        assertEquals(1, feed.calls());
    }

    private static class RecordingMarketRealtimeFeed extends MarketRealtimeFeed {
        private int calls;

        private RecordingMarketRealtimeFeed() {
            super(null, null, null);
        }

        @Override
        public void refreshSupportedMarkets() {
            calls++;
        }

        private int calls() {
            return calls;
        }
    }
}
