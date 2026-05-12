package coin.coinzzickmock.providers.infrastructure;

public interface BitgetWebSocketConnection {
    void send(String message);

    void close();
}
