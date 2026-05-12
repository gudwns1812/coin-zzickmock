package coin.coinzzickmock.providers.infrastructure.config;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "coin.bitget.websocket")
public class BitgetWebSocketProperties {
    private boolean enabled = false;
    private URI uri = URI.create("wss://ws.bitget.com/v2/ws/public");
    private List<String> symbols = new ArrayList<>(List.of("BTCUSDT", "ETHUSDT"));
    private Duration reconnectDelay = Duration.ofSeconds(5);
    private Duration heartbeatDelay = Duration.ofSeconds(25);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public URI getUri() {
        return uri;
    }

    public void setUri(URI uri) {
        this.uri = uri;
    }

    public List<String> getSymbols() {
        return List.copyOf(symbols);
    }

    public void setSymbols(List<String> symbols) {
        this.symbols = symbols == null ? new ArrayList<>() : new ArrayList<>(symbols);
    }

    public Duration getReconnectDelay() {
        return reconnectDelay;
    }

    public void setReconnectDelay(Duration reconnectDelay) {
        this.reconnectDelay = reconnectDelay;
    }

    public Duration getHeartbeatDelay() {
        return heartbeatDelay;
    }

    public void setHeartbeatDelay(Duration heartbeatDelay) {
        this.heartbeatDelay = heartbeatDelay;
    }
}
