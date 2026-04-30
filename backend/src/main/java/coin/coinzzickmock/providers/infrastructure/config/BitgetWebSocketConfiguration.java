package coin.coinzzickmock.providers.infrastructure.config;

import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketCandleUpdateService;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketDataStore;
import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketChannel;
import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketConnectionFactory;
import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketLifecycle;
import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketMarketEventConsumer;
import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketMarketEventParser;
import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketSubscription;
import coin.coinzzickmock.providers.infrastructure.JavaNetBitgetWebSocketConnectionFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BitgetWebSocketProperties.class)
@ConditionalOnProperty(prefix = "coin.bitget.websocket", name = "enabled", havingValue = "true")
public class BitgetWebSocketConfiguration {
    @Bean
    HttpClient bitgetWebSocketHttpClient() {
        return HttpClient.newHttpClient();
    }

    @Bean
    BitgetWebSocketConnectionFactory bitgetWebSocketConnectionFactory(
            HttpClient bitgetWebSocketHttpClient,
            BitgetWebSocketProperties properties
    ) {
        return new JavaNetBitgetWebSocketConnectionFactory(bitgetWebSocketHttpClient, properties.getUri());
    }

    @Bean
    BitgetWebSocketMarketEventParser bitgetWebSocketMarketEventParser(ObjectMapper objectMapper) {
        return new BitgetWebSocketMarketEventParser(objectMapper);
    }

    @Bean
    List<BitgetWebSocketSubscription> bitgetWebSocketSubscriptions(BitgetWebSocketProperties properties) {
        List<BitgetWebSocketSubscription> subscriptions = new ArrayList<>();
        for (String symbol : properties.getSymbols()) {
            subscriptions.add(BitgetWebSocketSubscription.usdtFutures(BitgetWebSocketChannel.TRADE, symbol));
            subscriptions.add(BitgetWebSocketSubscription.usdtFutures(BitgetWebSocketChannel.TICKER, symbol));
            subscriptions.add(BitgetWebSocketSubscription.usdtFutures(BitgetWebSocketChannel.CANDLE_1M, symbol));
            subscriptions.add(BitgetWebSocketSubscription.usdtFutures(BitgetWebSocketChannel.CANDLE_1H, symbol));
        }
        return List.copyOf(subscriptions);
    }

    @Bean
    BitgetWebSocketMarketEventConsumer bitgetWebSocketMarketEventConsumer(
            RealtimeMarketDataStore realtimeMarketDataStore,
            RealtimeMarketCandleUpdateService realtimeMarketCandleUpdateService
    ) {
        return new BitgetWebSocketMarketEventConsumer(realtimeMarketDataStore, realtimeMarketCandleUpdateService);
    }

    @Bean
    BitgetWebSocketLifecycle bitgetWebSocketLifecycle(
            BitgetWebSocketConnectionFactory bitgetWebSocketConnectionFactory,
            BitgetWebSocketMarketEventParser bitgetWebSocketMarketEventParser,
            List<BitgetWebSocketSubscription> bitgetWebSocketSubscriptions,
            BitgetWebSocketMarketEventConsumer bitgetWebSocketMarketEventConsumer
    ) {
        return new BitgetWebSocketLifecycle(
                bitgetWebSocketConnectionFactory,
                bitgetWebSocketMarketEventParser,
                bitgetWebSocketSubscriptions,
                bitgetWebSocketMarketEventConsumer
        );
    }

    @Bean
    BitgetWebSocketRuntime bitgetWebSocketRuntime(BitgetWebSocketLifecycle bitgetWebSocketLifecycle) {
        return new BitgetWebSocketRuntime(bitgetWebSocketLifecycle);
    }
}
