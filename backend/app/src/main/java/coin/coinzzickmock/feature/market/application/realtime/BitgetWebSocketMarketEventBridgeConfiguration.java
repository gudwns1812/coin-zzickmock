package coin.coinzzickmock.feature.market.application.realtime;

import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketMarketEvent;
import java.util.function.Consumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BitgetWebSocketMarketEventBridgeConfiguration {
    @Bean
    Consumer<BitgetWebSocketMarketEvent> bitgetWebSocketMarketEventBridge(
            RealtimeMarketDataStore realtimeMarketDataStore,
            RealtimeMarketCandleUpdateService realtimeMarketCandleUpdateService
    ) {
        return new BitgetWebSocketMarketEventBridge(
                realtimeMarketDataStore,
                realtimeMarketCandleUpdateService
        );
    }
}
