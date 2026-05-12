package coin.coinzzickmock.feature.market.application.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class MarketRealtimeSourceSnapshotTest {
    private static final Instant RECEIVED_AT = Instant.parse("2026-04-30T02:00:00Z");

    @Test
    void classifiesNormalWebSocketSources() {
        MarketRealtimeSourceSnapshot snapshot = MarketRealtimeSourceSnapshot.webSocket(
                "BTCUSDT",
                MarketRealtimeSourceType.TRADE,
                RECEIVED_AT.minusMillis(100),
                RECEIVED_AT,
                "12345",
                null
        );

        assertThat(snapshot.isWebSocketSource()).isTrue();
        assertThat(snapshot.isRestFallbackSource()).isFalse();
        assertThat(snapshot.ageMs(RECEIVED_AT.plusMillis(250))).isEqualTo(250);
        assertThat(snapshot.isFresh(RECEIVED_AT.plusMillis(999), Duration.ofSeconds(1))).isTrue();
    }

    @Test
    void labelsRestRecoveryAsExplicitFallback() {
        MarketRealtimeSourceSnapshot snapshot = MarketRealtimeSourceSnapshot.restFallback(
                "BTCUSDT",
                MarketRealtimeSourceType.REST_RECOVERY,
                MarketRealtimeHealth.RECOVERING,
                RECEIVED_AT.minusSeconds(1),
                RECEIVED_AT,
                "websocket reconnect gap repair"
        );

        assertThat(snapshot.isRestFallbackSource()).isTrue();
        assertThat(snapshot.fallbackReason()).isEqualTo("websocket reconnect gap repair");
        assertThat(snapshot.reconnectState()).isEqualTo(MarketRealtimeReconnectState.RECONNECTING);
    }

    @Test
    void rejectsUnlabeledRestFallbacks() {
        assertThatThrownBy(() -> MarketRealtimeSourceSnapshot.restFallback(
                "BTCUSDT",
                MarketRealtimeSourceType.REST_RECOVERY,
                MarketRealtimeHealth.RECOVERING,
                RECEIVED_AT,
                RECEIVED_AT,
                " "
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void freshnessPolicyAllowsRestRecoveryOnlyWhenExplicitlyEnabled() {
        MarketRealtimeSourceSnapshot recovery = MarketRealtimeSourceSnapshot.restFallback(
                "BTCUSDT",
                MarketRealtimeSourceType.REST_RECOVERY,
                MarketRealtimeHealth.RECOVERING,
                RECEIVED_AT,
                RECEIVED_AT,
                "ws reconnect"
        );
        MarketRealtimeFreshnessPolicy strict = new MarketRealtimeFreshnessPolicy(Duration.ofSeconds(1), false);
        MarketRealtimeFreshnessPolicy recoveryAllowed = new MarketRealtimeFreshnessPolicy(Duration.ofSeconds(1), true);

        assertThat(strict.accepts(recovery, RECEIVED_AT.plusMillis(500))).isFalse();
        assertThat(recoveryAllowed.accepts(recovery, RECEIVED_AT.plusMillis(500))).isTrue();
        assertThat(recoveryAllowed.accepts(recovery, RECEIVED_AT.plusSeconds(2))).isFalse();
    }

    @Test
    void freshnessPolicyRejectsRestBootstrapForCommandUse() {
        MarketRealtimeSourceSnapshot bootstrap = MarketRealtimeSourceSnapshot.restFallback(
                "BTCUSDT",
                MarketRealtimeSourceType.REST_BOOTSTRAP,
                MarketRealtimeHealth.BOOTSTRAPPING,
                RECEIVED_AT,
                RECEIVED_AT,
                "startup seed"
        );
        MarketRealtimeFreshnessPolicy recoveryAllowed = new MarketRealtimeFreshnessPolicy(Duration.ofSeconds(1), true);

        assertThat(recoveryAllowed.accepts(bootstrap, RECEIVED_AT.plusMillis(500))).isFalse();
    }
}
