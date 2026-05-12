package coin.coinzzickmock.feature.market.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class UnifiedMarketStreamStrategyTest {
    @Test
    void anonymousStreamUsesActiveSymbolOnly() {
        MarketStreamBroker broker = mock(MarketStreamBroker.class);
        MarketOpenPositionSymbolsReader openPositionSymbolsReader = mock(MarketOpenPositionSymbolsReader.class);
        UnifiedMarketStreamStrategy strategy = new UnifiedMarketStreamStrategy(
                broker,
                openPositionSymbolsReader,
                Optional::empty
        );
        SseEmitter emitter = new SseEmitter();

        strategy.open(MarketSseStreamRequest.unified("BTCUSDT", "1m", "tab-1", emitter));

        verify(openPositionSymbolsReader, never()).openSymbols(any());
        verify(broker).openSession(eq(null), eq("tab-1"), eq("BTCUSDT"), eq(Set.of()), eq("1m"), eq(emitter));
    }

    @Test
    void authenticatedStreamUsesServerOpenPositionSymbols() {
        MarketStreamBroker broker = mock(MarketStreamBroker.class);
        MarketOpenPositionSymbolsReader openPositionSymbolsReader = mock(MarketOpenPositionSymbolsReader.class);
        when(openPositionSymbolsReader.openSymbols(1L)).thenReturn(Set.of("ETHUSDT", "BTCUSDT"));
        UnifiedMarketStreamStrategy strategy = new UnifiedMarketStreamStrategy(
                broker,
                openPositionSymbolsReader,
                () -> Optional.of(1L)
        );
        SseEmitter emitter = new SseEmitter();

        strategy.open(MarketSseStreamRequest.unified("BTCUSDT", "1m", " tab-1 ", emitter));

        verify(openPositionSymbolsReader).openSymbols(1L);
        verify(broker).openSession(eq(1L), eq("tab-1"), eq("BTCUSDT"), eq(Set.of("ETHUSDT", "BTCUSDT")), eq("1m"), eq(emitter));
    }

    @Test
    void openPositionSymbolLookupFailureDoesNotOpenActiveOnlyFallback() {
        MarketStreamBroker broker = mock(MarketStreamBroker.class);
        MarketOpenPositionSymbolsReader openPositionSymbolsReader = mock(MarketOpenPositionSymbolsReader.class);
        when(openPositionSymbolsReader.openSymbols(1L)).thenThrow(new IllegalStateException("db unavailable"));
        UnifiedMarketStreamStrategy strategy = new UnifiedMarketStreamStrategy(
                broker,
                openPositionSymbolsReader,
                () -> Optional.of(1L)
        );

        assertThatThrownBy(() -> strategy.open(MarketSseStreamRequest.unified("BTCUSDT", "1m", "tab-1", new SseEmitter())))
                .isInstanceOf(IllegalStateException.class);
        verify(broker, never()).openSession(any(), any(), any(), any(), any(), any());
    }
}
