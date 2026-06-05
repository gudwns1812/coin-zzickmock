package coin.coinzzickmock.feature.market.infrastructure.config;

import coin.coinzzickmock.feature.market.candle.application.dto.RealtimeMarketCandleUpdate;
import coin.coinzzickmock.feature.market.quote.application.service.ProviderMarketRealtimeEventBridge;
import coin.coinzzickmock.feature.market.quote.application.implement.MarketTradePriceMovementPublisher;
import coin.coinzzickmock.feature.market.candle.application.service.RealtimeMarketCandleUpdateService;
import coin.coinzzickmock.feature.market.quote.application.implement.RealtimeMarketDataStore;
import coin.coinzzickmock.providers.Providers;
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
            MarketTradePriceMovementPublisher marketTradePriceMovementPublisher,
            Providers providers
    ) {
        return new ProviderMarketRealtimeEventBridge(
                realtimeMarketDataStore,
                realtimeMarketCandleUpdateService,
                marketTradePriceMovementPublisher,
                providers.telemetry()
        );
    }
}
