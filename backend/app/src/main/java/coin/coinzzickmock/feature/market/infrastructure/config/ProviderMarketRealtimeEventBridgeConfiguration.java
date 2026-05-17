package coin.coinzzickmock.feature.market.infrastructure.config;

import coin.coinzzickmock.feature.market.application.dto.RealtimeMarketCandleUpdate;
import coin.coinzzickmock.feature.market.application.realtime.ProviderMarketRealtimeEventBridge;
import coin.coinzzickmock.feature.market.application.implement.MarketTradePriceMovementPublisher;
import coin.coinzzickmock.feature.market.application.service.RealtimeMarketCandleUpdateService;
import coin.coinzzickmock.feature.market.application.implement.RealtimeMarketDataStore;
import coin.coinzzickmock.providers.connector.ProviderMarketRealtimeEvent;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProviderMarketRealtimeEventBridgeConfiguration {
    @Bean
    Consumer<ProviderMarketRealtimeEvent> providerMarketRealtimeEventBridge(
            RealtimeMarketDataStore realtimeMarketDataStore,
            RealtimeMarketCandleUpdateService realtimeMarketCandleUpdateService,
            MarketTradePriceMovementPublisher marketTradePriceMovementPublisher
    ) {
        return new ProviderMarketRealtimeEventBridge(
                realtimeMarketDataStore,
                realtimeMarketCandleUpdateService,
                marketTradePriceMovementPublisher
        );
    }
}
