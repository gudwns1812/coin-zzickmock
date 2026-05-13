package coin.coinzzickmock.feature.market.infrastructure.config;

import coin.coinzzickmock.feature.market.application.realtime.ProviderMarketRealtimeEventBridge;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleUpdateService;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketDataStore;
import coin.coinzzickmock.providers.connector.ProviderMarketRealtimeEvent;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ProviderMarketRealtimeEventBridgeConfiguration {
    @Bean
    Consumer<ProviderMarketRealtimeEvent> providerMarketRealtimeEventBridge(
            RealtimeMarketDataStore realtimeMarketDataStore,
            RealtimeMarketCandleUpdateService realtimeMarketCandleUpdateService
    ) {
        return new ProviderMarketRealtimeEventBridge(
                realtimeMarketDataStore,
                realtimeMarketCandleUpdateService
        );
    }
}
