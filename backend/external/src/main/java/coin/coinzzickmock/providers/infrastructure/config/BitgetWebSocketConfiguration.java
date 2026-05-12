package coin.coinzzickmock.providers.infrastructure.config;

import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketChannel;
import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketConnectionFactory;
import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketLifecycle;
import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketMarketEvent;
import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketMarketEventParser;
import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketSubscription;
import coin.coinzzickmock.providers.infrastructure.JavaNetBitgetWebSocketConnectionFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
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
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
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
    BitgetWebSocketLifecycle bitgetWebSocketLifecycle(
            BitgetWebSocketConnectionFactory bitgetWebSocketConnectionFactory,
            BitgetWebSocketMarketEventParser bitgetWebSocketMarketEventParser,
            List<BitgetWebSocketSubscription> bitgetWebSocketSubscriptions,
            Consumer<BitgetWebSocketMarketEvent> bitgetWebSocketMarketEventBridge
    ) {
        return new BitgetWebSocketLifecycle(
                bitgetWebSocketConnectionFactory,
                bitgetWebSocketMarketEventParser,
                bitgetWebSocketSubscriptions,
                bitgetWebSocketMarketEventBridge
        );
    }

    @Bean
    BitgetWebSocketRuntime bitgetWebSocketRuntime(BitgetWebSocketLifecycle bitgetWebSocketLifecycle) {
        return new BitgetWebSocketRuntime(bitgetWebSocketLifecycle);
    }
}
