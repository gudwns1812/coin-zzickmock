package coin.coinzzickmock.feature.push.web;

import coin.coinzzickmock.feature.push.application.PushSseConnectionRegistry;
import coin.coinzzickmock.feature.push.application.PushServerProperties;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/futures")
@RequiredArgsConstructor
public class PushStreamController {
    private final PushSseConnectionRegistry registry;
    private final PushServerProperties properties;

    @GetMapping(value = {"/stream/orders", "/orders/stream"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter orderStream(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String clientKey
    ) {
        return registry.register("member:" + memberId(jwt), clientKey, properties.sseTimeoutMs());
    }

    @GetMapping(value = {"/stream/markets", "/markets/stream"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter unifiedMarketStream(
            @RequestParam String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) String clientKey
    ) {
        String normalizedSymbol = normalize(symbol);
        return registry.register(Set.of(
                "unified:" + normalizedSymbol,
                "unified:" + normalizedSymbol + ":" + interval
        ), clientKey, properties.sseTimeoutMs());
    }

    @GetMapping(value = {"/stream/markets/summary", "/markets/summary/stream"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter marketSummaryStream(
            @RequestParam String symbols,
            @RequestParam(required = false) String clientKey
    ) {
        Set<String> keys = new LinkedHashSet<>();
        parseSymbols(symbols).forEach(symbol -> keys.add("summary:" + symbol));
        return registry.register(keys, clientKey, properties.sseTimeoutMs());
    }

    @GetMapping(value = {"/stream/markets/{symbol}", "/markets/{symbol}/stream"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter marketSymbolStream(
            @PathVariable String symbol,
            @RequestParam(required = false) String clientKey
    ) {
        return registry.register("summary:" + normalize(symbol), clientKey, properties.sseTimeoutMs());
    }

    @GetMapping(value = {"/stream/markets/{symbol}/candles", "/markets/{symbol}/candles/stream"}, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter marketCandleStream(
            @PathVariable String symbol,
            @RequestParam String interval,
            @RequestParam(required = false) String clientKey
    ) {
        return registry.register("candle:" + normalize(symbol) + ":" + interval, clientKey, properties.sseTimeoutMs());
    }

    private Set<String> parseSymbols(String symbols) {
        Set<String> parsed = new LinkedHashSet<>();
        for (String symbol : symbols.split(",")) {
            if (!symbol.isBlank()) {
                parsed.add(normalize(symbol));
            }
        }
        if (parsed.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "symbols must not be empty");
        }
        return parsed;
    }

    private String normalize(String symbol) {
        return symbol.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private Long memberId(Jwt jwt) {
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid access token.", exception);
        }
    }
}
