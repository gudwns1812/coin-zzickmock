package coin.coinzzickmock.feature.market.job;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import coin.coinzzickmock.feature.market.application.implement.MarketRealtimeRefreshCoordinator;
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
    void delegatesRefreshWorkToApplicationCoordinator() {
        MarketRealtimeRefreshCoordinator coordinator = mock(MarketRealtimeRefreshCoordinator.class);
        MarketRealtimeRefreshScheduler scheduler = new MarketRealtimeRefreshScheduler(coordinator);

        scheduler.refreshSupportedMarkets();

        verify(coordinator).refreshSupportedMarkets();
    }
}
