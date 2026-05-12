package coin.coinzzickmock.providers.infrastructure;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

public class JavaNetBitgetWebSocketConnectionFactory implements BitgetWebSocketConnectionFactory {
    private final HttpClient httpClient;
    private final URI uri;

    public JavaNetBitgetWebSocketConnectionFactory(HttpClient httpClient, URI uri) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.uri = Objects.requireNonNull(uri, "uri must not be null");
    }

    @Override
    public BitgetWebSocketConnection connect(BitgetWebSocketMessageHandler handler) {
        JavaNetListener listener = new JavaNetListener(handler);
        WebSocket webSocket = httpClient.newWebSocketBuilder()
                .buildAsync(uri, listener)
                .join();
        return new JavaNetConnection(webSocket);
    }

    private static final class JavaNetConnection implements BitgetWebSocketConnection {
        private final WebSocket webSocket;

        private JavaNetConnection(WebSocket webSocket) {
            this.webSocket = webSocket;
        }

        @Override
        public void send(String message) {
            webSocket.sendText(message, true);
        }

        @Override
        public void close() {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "closing");
        }
    }

    private static final class JavaNetListener implements WebSocket.Listener {
        private final BitgetWebSocketMessageHandler handler;
        private final StringBuilder partialText = new StringBuilder();

        private JavaNetListener(BitgetWebSocketMessageHandler handler) {
            this.handler = Objects.requireNonNull(handler, "handler must not be null");
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            partialText.append(data);
            if (last) {
                handler.onText(partialText.toString(), Instant.now());
                partialText.setLength(0);
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            handler.onError(error);
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            handler.onClosed();
            return null;
        }
    }
}
