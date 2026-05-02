package coin.coinzzickmock.feature.market.job;

import coin.coinzzickmock.feature.market.application.realtime.MarketRealtimeFeed;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarketRealtimeRefreshScheduler {
    private final MarketRealtimeFeed marketRealtimeFeed;

    @Scheduled(fixedDelayString = "${coin.market.refresh-delay-ms:1000}")
    public void refreshSupportedMarkets() {
        marketRealtimeFeed.refreshSupportedMarkets();
    }
}
