package coin.coinzzickmock.providers.infrastructure;

import coin.coinzzickmock.feature.market.application.realtime.MarketRealtimeReconnectState;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BitgetWebSocketLifecycle implements BitgetWebSocketMessageHandler {
    private final BitgetWebSocketConnectionFactory connectionFactory;
    private final BitgetWebSocketMarketEventParser parser;
    private final List<BitgetWebSocketSubscription> subscriptions;
    private final Consumer<BitgetWebSocketMarketEvent> eventConsumer;

    private MarketRealtimeReconnectState reconnectState = MarketRealtimeReconnectState.NOT_STARTED;
    private BitgetWebSocketConnection connection;
    private int reconnectAttempts;

    public BitgetWebSocketLifecycle(
            BitgetWebSocketConnectionFactory connectionFactory,
            BitgetWebSocketMarketEventParser parser,
            List<BitgetWebSocketSubscription> subscriptions,
            Consumer<BitgetWebSocketMarketEvent> eventConsumer
    ) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory, "connectionFactory must not be null");
        this.parser = Objects.requireNonNull(parser, "parser must not be null");
        this.subscriptions = List.copyOf(subscriptions);
        this.eventConsumer = Objects.requireNonNull(eventConsumer, "eventConsumer must not be null");
    }

    public void start() {
        if (reconnectState == MarketRealtimeReconnectState.CONNECTED
                || reconnectState == MarketRealtimeReconnectState.CONNECTING) {
            return;
        }

        reconnectState = MarketRealtimeReconnectState.CONNECTING;
        try {
            connection = connectionFactory.connect(this);
            reconnectState = MarketRealtimeReconnectState.CONNECTED;
            subscribeAll();
        } catch (RuntimeException exception) {
            reconnectState = MarketRealtimeReconnectState.DISCONNECTED;
            log.warn("Bitget WebSocket connection failed; runtime will retry.", exception);
        }
    }

    public void recover() {
        if (reconnectState == MarketRealtimeReconnectState.CONNECTED
                || reconnectState == MarketRealtimeReconnectState.CONNECTING) {
            return;
        }

        reconnectAttempts++;
        start();
    }

    public void stop() {
        if (connection != null) {
            connection.close();
        }
        reconnectState = MarketRealtimeReconnectState.DISCONNECTED;
    }

    public void heartbeat() {
        if (reconnectState == MarketRealtimeReconnectState.CONNECTED && connection != null) {
            connection.send("ping");
        }
    }

    @Override
    public void onText(String message, Instant receivedAt) {
        parser.parse(message, receivedAt).forEach(eventConsumer);
    }

    @Override
    public void onError(Throwable error) {
        reconnectState = MarketRealtimeReconnectState.RECONNECTING;
    }

    @Override
    public void onClosed() {
        reconnectState = MarketRealtimeReconnectState.DISCONNECTED;
    }

    public MarketRealtimeReconnectState reconnectState() {
        return reconnectState;
    }

    public int reconnectAttempts() {
        return reconnectAttempts;
    }

    private void subscribeAll() {
        for (BitgetWebSocketSubscription subscription : subscriptions) {
            connection.send(subscription.subscribeMessage());
        }
    }
}
