package coin.coinzzickmock.providers.infrastructure;

import java.time.Instant;

public interface BitgetWebSocketMessageHandler {
    void onText(String message, Instant receivedAt);

    void onError(Throwable error);

    void onClosed();
}
