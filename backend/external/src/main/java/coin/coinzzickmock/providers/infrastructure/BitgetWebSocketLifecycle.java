package coin.coinzzickmock.providers.infrastructure;

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

    private BitgetWebSocketReconnectState reconnectState = BitgetWebSocketReconnectState.NOT_STARTED;
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

    public synchronized void start() {
        if (reconnectState == BitgetWebSocketReconnectState.CONNECTED
                || reconnectState == BitgetWebSocketReconnectState.CONNECTING) {
            return;
        }

        reconnectState = BitgetWebSocketReconnectState.CONNECTING;
        try {
            connection = connectionFactory.connect(this);
            subscribeAll();
            reconnectState = BitgetWebSocketReconnectState.CONNECTED;
        } catch (RuntimeException exception) {
            closeConnection();
            reconnectState = BitgetWebSocketReconnectState.DISCONNECTED;
            log.warn("Bitget WebSocket connection failed; runtime will retry.", exception);
        }
    }

    public synchronized void recover() {
        if (reconnectState == BitgetWebSocketReconnectState.CONNECTED
                || reconnectState == BitgetWebSocketReconnectState.CONNECTING) {
            return;
        }

        reconnectAttempts++;
        start();
    }

    public synchronized void stop() {
        closeConnection();
        reconnectState = BitgetWebSocketReconnectState.DISCONNECTED;
    }

    public synchronized void heartbeat() {
        if (reconnectState == BitgetWebSocketReconnectState.CONNECTED && connection != null) {
            connection.send("ping");
        }
    }

    @Override
    public void onText(String message, Instant receivedAt) {
        parser.parse(message, receivedAt).forEach(eventConsumer);
    }

    @Override
    public synchronized void onError(Throwable error) {
        reconnectState = BitgetWebSocketReconnectState.RECONNECTING;
        log.warn("Bitget WebSocket error; switching to reconnecting state.", error);
    }

    @Override
    public synchronized void onClosed() {
        reconnectState = BitgetWebSocketReconnectState.DISCONNECTED;
    }

    public synchronized BitgetWebSocketReconnectState reconnectState() {
        return reconnectState;
    }

    public synchronized int reconnectAttempts() {
        return reconnectAttempts;
    }

    private void subscribeAll() {
        for (BitgetWebSocketSubscription subscription : subscriptions) {
            connection.send(subscription.subscribeMessage());
        }
    }

    private void closeConnection() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
    }
}
