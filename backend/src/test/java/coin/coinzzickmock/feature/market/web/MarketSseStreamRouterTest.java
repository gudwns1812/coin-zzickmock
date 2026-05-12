package coin.coinzzickmock.feature.market.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class MarketSseStreamRouterTest {

    @Test
    void routesByExplicitKind() {
        MarketSseStreamStrategy summary = strategy(MarketSseStreamKind.SUMMARY);
        MarketSseStreamStrategy candle = strategy(MarketSseStreamKind.CANDLE);
        MarketSseStreamStrategy unified = strategy(MarketSseStreamKind.UNIFIED);
        MarketSseStreamRouter router = new MarketSseStreamRouter(List.of(summary, candle, unified));
        MarketSseStreamRequest request = MarketSseStreamRequest.summary(Set.of("BTCUSDT"), "tab-1", new SseEmitter());

        router.open(request);

        verify(summary).open(request);
    }

    @Test
    void rejectsDuplicateKind() {
        assertThatThrownBy(() -> new MarketSseStreamRouter(List.of(
                strategy(MarketSseStreamKind.SUMMARY),
                strategy(MarketSseStreamKind.SUMMARY),
                strategy(MarketSseStreamKind.CANDLE),
                strategy(MarketSseStreamKind.UNIFIED)
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate");
    }

    @Test
    void rejectsMissingKind() {
        assertThatThrownBy(() -> new MarketSseStreamRouter(List.of(
                strategy(MarketSseStreamKind.SUMMARY),
                strategy(MarketSseStreamKind.CANDLE)
        ))).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Missing");
    }

    private static MarketSseStreamStrategy strategy(MarketSseStreamKind kind) {
        MarketSseStreamStrategy strategy = mock(MarketSseStreamStrategy.class);
        when(strategy.kind()).thenReturn(kind);
        return strategy;
    }
}
