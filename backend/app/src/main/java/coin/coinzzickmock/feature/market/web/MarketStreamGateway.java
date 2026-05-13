package coin.coinzzickmock.feature.market.web;

import java.util.Set;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface MarketStreamGateway {
    void openUnified(String symbol, String interval, String clientKey, SseEmitter emitter);

    void openSummary(Set<String> symbols, String clientKey, SseEmitter emitter);

    void openCandle(String symbol, String interval, String clientKey, SseEmitter emitter);
}
