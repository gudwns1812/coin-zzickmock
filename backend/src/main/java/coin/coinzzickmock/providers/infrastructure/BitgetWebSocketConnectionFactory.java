package coin.coinzzickmock.providers.infrastructure;

public interface BitgetWebSocketConnectionFactory {
    BitgetWebSocketConnection connect(BitgetWebSocketMessageHandler handler);
}
