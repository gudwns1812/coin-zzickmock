package coin.coinzzickmock.feature.market.infrastructure.config;

import coin.coinzzickmock.feature.market.application.realtime.MarketSupportedMarketRefresher;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "coin.market.startup-warmup",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@RequiredArgsConstructor
public class MarketStartupWarmupReadyEventListener {
    private final MarketSupportedMarketRefresher marketSupportedMarketRefresher;

    @EventListener(ApplicationReadyEvent.class)
    public void warmupSupportedMarketsAfterApplicationReady() {
        marketSupportedMarketRefresher.refreshSupportedMarkets();
    }
}
