package coin.coinzzickmock.providers.infrastructure.config;

import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketReconnectState;
import coin.coinzzickmock.providers.infrastructure.BitgetWebSocketLifecycle;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@RequiredArgsConstructor
public class BitgetWebSocketRuntime {
    private final BitgetWebSocketLifecycle lifecycle;

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        lifecycle.start();
    }

    @Scheduled(fixedDelayString = "${coin.bitget.websocket.reconnect-delay:PT5S}")
    public void recoverDisconnectedConnection() {
        try {
            if (lifecycle.reconnectState() == BitgetWebSocketReconnectState.CONNECTED
                    || lifecycle.reconnectState() == BitgetWebSocketReconnectState.CONNECTING) {
                return;
            }
            lifecycle.recover();
        } catch (RuntimeException exception) {
            log.warn("Bitget WebSocket recovery failed; scheduler will retry.", exception);
        }
    }

    @Scheduled(fixedDelayString = "${coin.bitget.websocket.heartbeat-delay:PT25S}")
    public void heartbeat() {
        try {
            lifecycle.heartbeat();
        } catch (RuntimeException exception) {
            log.warn("Bitget WebSocket heartbeat failed; scheduler will retry.", exception);
        }
    }

    @PreDestroy
    public void stop() {
        lifecycle.stop();
    }
}
