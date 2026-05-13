package coin.coinzzickmock.feature.market.web.config;

import coin.coinzzickmock.feature.market.web.MarketStreamGateway;
import coin.coinzzickmock.feature.market.web.MarketSseStreamRequest;
import coin.coinzzickmock.feature.market.web.MarketSseStreamRouter;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
class StreamMarketGatewayAdapter implements MarketStreamGateway {
    private final MarketSseStreamRouter marketSseStreamRouter;

    @Override
    public void openUnified(String symbol, String interval, String clientKey, SseEmitter emitter) {
        marketSseStreamRouter.open(MarketSseStreamRequest.unified(symbol, interval, clientKey, emitter));
    }

    @Override
    public void openSummary(Set<String> symbols, String clientKey, SseEmitter emitter) {
        marketSseStreamRouter.open(MarketSseStreamRequest.summary(symbols, clientKey, emitter));
    }

    @Override
    public void openCandle(String symbol, String interval, String clientKey, SseEmitter emitter) {
        marketSseStreamRouter.open(MarketSseStreamRequest.candle(symbol, interval, clientKey, emitter));
    }
}
