package coin.coinzzickmock.feature.market.job;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;

import coin.coinzzickmock.feature.market.application.realtime.MarketHistoryStartupBackfill;
import coin.coinzzickmock.feature.market.application.gateway.MarketDataGateway;
import org.junit.jupiter.api.Test;

class MarketHistoryStartupBackfillReadyEventListenerTest {
    @Test
    void delegatesBackfillThroughMarketDataGatewayWithoutWarmupLoad() {
        MarketHistoryStartupBackfill marketHistoryStartupBackfill = mock(MarketHistoryStartupBackfill.class);
        MarketDataGateway marketDataGateway = mock(MarketDataGateway.class);
        MarketHistoryStartupBackfillReadyEventListener listener =
                new MarketHistoryStartupBackfillReadyEventListener(
                        marketHistoryStartupBackfill,
                        marketDataGateway
                );

        listener.backfillMissingHistoryAfterApplicationReady();

        verify(marketHistoryStartupBackfill).backfillMissingMinuteHistory(any(), same(marketDataGateway));
        verifyNoInteractions(marketDataGateway);
    }
}
