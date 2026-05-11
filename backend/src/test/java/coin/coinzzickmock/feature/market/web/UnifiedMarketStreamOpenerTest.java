package coin.coinzzickmock.feature.market.web;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import coin.coinzzickmock.feature.market.domain.MarketCandleInterval;
import coin.coinzzickmock.feature.position.application.query.OpenPositionSymbolsReader;
import coin.coinzzickmock.providers.Providers;
import coin.coinzzickmock.providers.auth.Actor;
import coin.coinzzickmock.providers.auth.AuthProvider;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class UnifiedMarketStreamOpenerTest {

    @Test
    void anonymousStreamUsesActiveSymbolOnly() {
        MarketStreamBroker broker = mock(MarketStreamBroker.class);
        OpenPositionSymbolsReader openPositionSymbolsReader = mock(OpenPositionSymbolsReader.class);
        UnifiedMarketStreamOpener opener = new UnifiedMarketStreamOpener(
                broker,
                openPositionSymbolsReader,
                providers(Optional.empty())
        );
        SseEmitter emitter = new SseEmitter();

        opener.open("BTCUSDT", "1m", "tab-1", emitter);

        verify(openPositionSymbolsReader, never()).openSymbols(any());
        verify(broker).openSession(
                eq(null),
                eq("tab-1"),
                eq("BTCUSDT"),
                eq(Set.of()),
                eq(MarketCandleInterval.ONE_MINUTE),
                eq(emitter)
        );
    }

    @Test
    void authenticatedStreamUsesServerOpenPositionSymbols() {
        MarketStreamBroker broker = mock(MarketStreamBroker.class);
        OpenPositionSymbolsReader openPositionSymbolsReader = mock(OpenPositionSymbolsReader.class);
        when(openPositionSymbolsReader.openSymbols(1L)).thenReturn(List.of("ETHUSDT", "BTCUSDT", "ETHUSDT"));
        UnifiedMarketStreamOpener opener = new UnifiedMarketStreamOpener(
                broker,
                openPositionSymbolsReader,
                providers(Optional.of(new Actor(1L, "user", "user@example.com", "User")))
        );
        SseEmitter emitter = new SseEmitter();

        opener.open("BTCUSDT", "1m", " tab-1 ", emitter);

        verify(openPositionSymbolsReader).openSymbols(1L);
        verify(broker).openSession(
                eq(1L),
                eq("tab-1"),
                eq("BTCUSDT"),
                eq(Set.of("ETHUSDT", "BTCUSDT")),
                eq(MarketCandleInterval.ONE_MINUTE),
                eq(emitter)
        );
    }

    @Test
    void openPositionSymbolLookupFailureDoesNotOpenActiveOnlyFallback() {
        MarketStreamBroker broker = mock(MarketStreamBroker.class);
        OpenPositionSymbolsReader openPositionSymbolsReader = mock(OpenPositionSymbolsReader.class);
        when(openPositionSymbolsReader.openSymbols(1L)).thenThrow(new IllegalStateException("db unavailable"));
        UnifiedMarketStreamOpener opener = new UnifiedMarketStreamOpener(
                broker,
                openPositionSymbolsReader,
                providers(Optional.of(new Actor(1L, "user", "user@example.com", "User")))
        );

        assertThatThrownBy(() -> opener.open("BTCUSDT", "1m", "tab-1", new SseEmitter()))
                .isInstanceOf(IllegalStateException.class);

        verify(broker, never()).openSession(any(), any(), any(), any(), any(), any());
    }

    private static Providers providers(Optional<Actor> actor) {
        Providers providers = mock(Providers.class);
        AuthProvider authProvider = mock(AuthProvider.class);
        when(authProvider.currentActorOptional()).thenReturn(actor);
        when(providers.auth()).thenReturn(authProvider);
        return providers;
    }
}
