package coin.coinzzickmock.feature.market.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
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
        MarketSseStreamRequest request = MarketSseStreamRequest.candle(
                " BTCUSDT ",
                MarketCandleInterval.ONE_MINUTE,
                "tab-1",
                new SseEmitter()
        );

        assertThat(request.kind()).isEqualTo(MarketSseStreamKind.CANDLE);
        assertThat(request.activeSymbol()).isEqualTo("BTCUSDT");
        assertThat(request.candleInterval()).isEqualTo(MarketCandleInterval.ONE_MINUTE);
    }

    @Test
    void unifiedRequiresActiveSymbolAndInterval() {
        MarketSseStreamRequest request = MarketSseStreamRequest.unified(
                "BTCUSDT",
                MarketCandleInterval.ONE_HOUR,
                "tab-1",
                new SseEmitter()
        );

        assertThat(request.kind()).isEqualTo(MarketSseStreamKind.UNIFIED);
        assertThat(request.activeSymbol()).isEqualTo("BTCUSDT");
        assertThat(request.candleInterval()).isEqualTo(MarketCandleInterval.ONE_HOUR);
    }

    @Test
    void requiredFieldsFailFast() {
        assertThatThrownBy(() -> MarketSseStreamRequest.summary(Set.of(), "tab-1", new SseEmitter()))
                .isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> MarketSseStreamRequest.summary(Set.of(" "), "tab-1", new SseEmitter()))
                .isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> MarketSseStreamRequest.candle(" ", MarketCandleInterval.ONE_MINUTE, "tab-1", new SseEmitter()))
                .isInstanceOf(CoreException.class);
        assertThatThrownBy(() -> MarketSseStreamRequest.unified("BTCUSDT", null, "tab-1", new SseEmitter()))
                .isInstanceOf(NullPointerException.class);
    }
}
