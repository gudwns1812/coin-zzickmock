package coin.coinzzickmock.providers.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BitgetWebSocketLifecycleTest {
    @Test
    void subscribesToConfiguredChannelsOnStart() {
        RecordingConnectionFactory connectionFactory = new RecordingConnectionFactory();
        List<BitgetWebSocketMarketEvent> events = new ArrayList<>();
        BitgetWebSocketLifecycle lifecycle = new BitgetWebSocketLifecycle(
                connectionFactory,
                new BitgetWebSocketMarketEventParser(new ObjectMapper()),
                List.of(
                        BitgetWebSocketSubscription.usdtFutures(BitgetWebSocketChannel.TRADE, "BTCUSDT"),
                        BitgetWebSocketSubscription.usdtFutures(BitgetWebSocketChannel.CANDLE_1M, "BTCUSDT")
                ),
                events::add
        );

        lifecycle.start();

        assertThat(connectionFactory.connection().sentMessages()).containsExactly(
                "{\"op\":\"subscribe\",\"args\":[{\"instType\":\"USDT-FUTURES\",\"channel\":\"trade\",\"instId\":\"BTCUSDT\"}]}",
                "{\"op\":\"subscribe\",\"args\":[{\"instType\":\"USDT-FUTURES\",\"channel\":\"candle1m\",\"instId\":\"BTCUSDT\"}]}"
        );
    }

    @Test
    void forwardsParsedEventsFromIncomingMessages() {
        RecordingConnectionFactory connectionFactory = new RecordingConnectionFactory();
        List<BitgetWebSocketMarketEvent> events = new ArrayList<>();
        BitgetWebSocketLifecycle lifecycle = new BitgetWebSocketLifecycle(
                connectionFactory,
                new BitgetWebSocketMarketEventParser(new ObjectMapper()),
                List.of(BitgetWebSocketSubscription.usdtFutures(BitgetWebSocketChannel.TRADE, "BTCUSDT")),
                events::add
        );

        lifecycle.start();
        connectionFactory.handler().onText("""
                {
                  "action": "snapshot",
                  "arg": {"instType": "USDT-FUTURES", "channel": "trade", "instId": "BTCUSDT"},
                  "data": [{"ts": "1695716760565", "price": "27000.5", "size": "0.001", "side": "buy", "tradeId": "111"}],
                  "ts": 1695716761589
                }
                """, Instant.parse("2026-04-30T03:00:00Z"));

        assertThat(events).hasSize(1);
        assertThat(events.get(0)).isInstanceOf(BitgetWebSocketTradeEvent.class);
    }

    @Test
    void tracksReconnectStateWithoutAutoStartingProductionConnections() {
        RecordingConnectionFactory connectionFactory = new RecordingConnectionFactory();
        BitgetWebSocketLifecycle lifecycle = new BitgetWebSocketLifecycle(
                connectionFactory,
                new BitgetWebSocketMarketEventParser(new ObjectMapper()),
                List.of(BitgetWebSocketSubscription.usdtFutures(BitgetWebSocketChannel.TICKER, "BTCUSDT")),
                event -> {
                }
        );

        lifecycle.start();
        lifecycle.onError(new IllegalStateException("boom"));

        assertThat(lifecycle.reconnectState()).isEqualTo(
                coin.coinzzickmock.feature.market.application.realtime.MarketRealtimeReconnectState.RECONNECTING);

        lifecycle.recover();

        assertThat(lifecycle.reconnectAttempts()).isEqualTo(1);
        assertThat(lifecycle.reconnectState()).isEqualTo(
                coin.coinzzickmock.feature.market.application.realtime.MarketRealtimeReconnectState.CONNECTED);
    }

    private static final class RecordingConnectionFactory implements BitgetWebSocketConnectionFactory {
        private RecordingConnection connection;
        private BitgetWebSocketMessageHandler handler;

        @Override
        public BitgetWebSocketConnection connect(BitgetWebSocketMessageHandler handler) {
            this.handler = handler;
            this.connection = new RecordingConnection();
            return connection;
        }

        RecordingConnection connection() {
            return connection;
        }

        BitgetWebSocketMessageHandler handler() {
            return handler;
        }
    }

    private static final class RecordingConnection implements BitgetWebSocketConnection {
        private final List<String> sentMessages = new ArrayList<>();

        @Override
        public void send(String message) {
            sentMessages.add(message);
        }

        @Override
        public void close() {
        }

        List<String> sentMessages() {
            return sentMessages;
        }
    }
}
