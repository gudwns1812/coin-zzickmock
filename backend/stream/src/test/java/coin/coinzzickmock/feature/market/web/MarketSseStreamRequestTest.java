package coin.coinzzickmock.feature.market.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coin.coinzzickmock.common.web.SseClientKeyException;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class MarketSseStreamRequestTest {
    @Test
    void summaryResolvesClientKeyAndKeepsImmutableSymbols() {
        Set<String> symbols = new LinkedHashSet<>(Set.of("BTCUSDT", "ETHUSDT"));
        MarketSseStreamRequest request = MarketSseStreamRequest.summary(symbols, " tab-1 ", new SseEmitter());

        assertThat(request.kind()).isEqualTo(MarketSseStreamKind.SUMMARY);
        assertThat(request.clientKey()).isEqualTo("tab-1");
        assertThat(request.summarySymbols()).containsExactlyInAnyOrder("BTCUSDT", "ETHUSDT");
        assertThatThrownBy(() -> request.summarySymbols().add("SOLUSDT"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void candleRequiresActiveSymbolAndInterval() {
        MarketSseStreamRequest request = MarketSseStreamRequest.candle(" BTCUSDT ", "1m", "tab-1", new SseEmitter());

        assertThat(request.kind()).isEqualTo(MarketSseStreamKind.CANDLE);
        assertThat(request.activeSymbol()).isEqualTo("BTCUSDT");
        assertThat(request.candleInterval()).isEqualTo("1m");
    }

    @Test
    void unifiedRequiresActiveSymbolAndInterval() {
        MarketSseStreamRequest request = MarketSseStreamRequest.unified("BTCUSDT", "1H", "tab-1", new SseEmitter());

        assertThat(request.kind()).isEqualTo(MarketSseStreamKind.UNIFIED);
        assertThat(request.activeSymbol()).isEqualTo("BTCUSDT");
        assertThat(request.candleInterval()).isEqualTo("1H");
    }

    @Test
    void requiredFieldsFailFast() {
        assertThatThrownBy(() -> MarketSseStreamRequest.summary(Set.of(), "tab-1", new SseEmitter()))
                .isInstanceOf(SseClientKeyException.class);
        assertThatThrownBy(() -> MarketSseStreamRequest.summary(Set.of(" "), "tab-1", new SseEmitter()))
                .isInstanceOf(SseClientKeyException.class);
        assertThatThrownBy(() -> MarketSseStreamRequest.candle(" ", "1m", "tab-1", new SseEmitter()))
                .isInstanceOf(SseClientKeyException.class);
        assertThatThrownBy(() -> MarketSseStreamRequest.unified("BTCUSDT", null, "tab-1", new SseEmitter()))
                .isInstanceOf(SseClientKeyException.class);
    }
}
