package coin.coinzzickmock.feature.market.application.realtime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.feature.market.infrastructure.config.MarketHistoryStartupBackfillReadyEventListener;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.connector.ConnectorProvider;
import coin.coinzzickmock.providers.connector.MarketDataGateway;
import org.junit.jupiter.api.Test;

class MarketHistoryStartupBackfillReadyEventListenerTest {
    @Test
    void delegatesBackfillThroughMarketDataGatewayWithoutWarmupLoad() {
        MarketHistoryStartupBackfill marketHistoryStartupBackfill = mock(MarketHistoryStartupBackfill.class);
        MarketDataGateway marketDataGateway = mock(MarketDataGateway.class);
        ConnectorProvider connectorProvider = mock(ConnectorProvider.class);
        Providers providers = mock(Providers.class);
        when(providers.connector()).thenReturn(connectorProvider);
        when(connectorProvider.marketDataGateway()).thenReturn(marketDataGateway);
        MarketHistoryStartupBackfillReadyEventListener listener =
                new MarketHistoryStartupBackfillReadyEventListener(
                        marketHistoryStartupBackfill,
                        providers
                );

        listener.backfillMissingHistoryAfterApplicationReady();

        verify(marketHistoryStartupBackfill).backfillMissingMinuteHistory(any(), same(marketDataGateway));
        verifyNoInteractions(marketDataGateway);
    }
}
