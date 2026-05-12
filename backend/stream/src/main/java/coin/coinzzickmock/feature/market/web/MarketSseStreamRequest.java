package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.common.web.SseClientKey;
import coin.coinzzickmock.common.web.SseClientKeyException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public final class MarketSseStreamRequest {
    private final MarketSseStreamKind kind;
    private final Set<String> summarySymbols;
    private final String activeSymbol;
    private final String candleInterval;
    private final String clientKey;
    private final SseEmitter emitter;

    private MarketSseStreamRequest(
            MarketSseStreamKind kind,
            Set<String> summarySymbols,
            String activeSymbol,
            String candleInterval,
            String clientKey,
            SseEmitter emitter
    ) {
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
        this.summarySymbols = summarySymbols;
        this.activeSymbol = activeSymbol;
        this.candleInterval = candleInterval;
        this.clientKey = SseClientKey.resolve(clientKey).value();
        this.emitter = Objects.requireNonNull(emitter, "emitter must not be null");
    }

    public static MarketSseStreamRequest summary(
            Set<String> summarySymbols,
            String clientKey,
            SseEmitter emitter
    ) {
        Set<String> resolvedSymbols = normalizeSymbols(summarySymbols);
        return new MarketSseStreamRequest(
                MarketSseStreamKind.SUMMARY,
                resolvedSymbols,
                null,
                null,
                clientKey,
                emitter
        );
    }

    public static MarketSseStreamRequest candle(
            String activeSymbol,
            String candleInterval,
            String clientKey,
            SseEmitter emitter
    ) {
        return new MarketSseStreamRequest(
                MarketSseStreamKind.CANDLE,
                null,
                normalizeSymbol(activeSymbol),
                normalizeSymbol(candleInterval),
                clientKey,
                emitter
        );
    }

    public static MarketSseStreamRequest unified(
            String activeSymbol,
            String candleInterval,
            String clientKey,
            SseEmitter emitter
    ) {
        return new MarketSseStreamRequest(
                MarketSseStreamKind.UNIFIED,
                null,
                normalizeSymbol(activeSymbol),
                normalizeSymbol(candleInterval),
                clientKey,
                emitter
        );
    }

    public MarketSseStreamKind kind() {
        return kind;
    }

    public Set<String> summarySymbols() {
        return summarySymbols;
    }

    public String activeSymbol() {
        return activeSymbol;
    }

    public String candleInterval() {
        return candleInterval;
    }

    public String clientKey() {
        return clientKey;
    }

    public SseEmitter emitter() {
        return emitter;
    }

    private static Set<String> normalizeSymbols(Set<String> rawSymbols) {
        if (rawSymbols == null || rawSymbols.isEmpty()) {
            throw new SseClientKeyException("Invalid market stream symbols");
        }
        Set<String> symbols = new LinkedHashSet<>();
        for (String rawSymbol : rawSymbols) {
            symbols.add(normalizeSymbol(rawSymbol));
        }
        return Collections.unmodifiableSet(symbols);
    }

    private static String normalizeSymbol(String rawSymbol) {
        if (rawSymbol == null || rawSymbol.isBlank()) {
            throw new SseClientKeyException("Invalid market stream symbol");
        }
        return rawSymbol.trim();
    }
}
