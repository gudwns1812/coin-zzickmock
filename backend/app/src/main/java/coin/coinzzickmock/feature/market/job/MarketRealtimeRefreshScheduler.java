package coin.coinzzickmock.feature.market.job;

import coin.coinzzickmock.feature.market.catalog.application.implement.MarketRealtimeRefreshCoordinator;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketRealtimeRefreshScheduler {
    private final MarketRealtimeRefreshCoordinator marketRealtimeRefreshCoordinator;

    @Scheduled(fixedDelayString = "${coin.market.refresh-delay-ms:1000}")
    public void refreshSupportedMarkets() {
        marketRealtimeRefreshCoordinator.refreshSupportedMarkets();
    }
}
