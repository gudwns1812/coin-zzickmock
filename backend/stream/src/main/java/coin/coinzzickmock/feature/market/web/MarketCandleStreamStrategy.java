package coin.coinzzickmock.feature.market.web;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
public class MarketCandleStreamStrategy implements MarketSseStreamStrategy {
    private final MarketCandleRealtimeSseBroker marketCandleRealtimeSseBroker;
    private final MarketCandleSnapshotReader marketCandleSnapshotReader;
    private final MarketCurrentCandleBootstrapper currentMarketCandleBootstrapper;

    @Override
    public MarketSseStreamKind kind() {
        return MarketSseStreamKind.CANDLE;
    }

    @Override
    public void open(MarketSseStreamRequest request) {
        MarketCandleRealtimeSseBroker.SubscriptionKey key =
                new MarketCandleRealtimeSseBroker.SubscriptionKey(request.activeSymbol(), request.candleInterval());
        MarketCandleRealtimeSseBroker.SseSubscriptionPermit permit = marketCandleRealtimeSseBroker.reserve(
                key,
                request.clientKey()
        );
        try {
            currentMarketCandleBootstrapper.bootstrapIfNeeded(request.activeSymbol(), request.candleInterval());
            boolean initialSendSucceeded = marketCandleSnapshotReader.latest(
                            request.activeSymbol(),
                            request.candleInterval()
                    )
                    .map(candle -> sendCandleEvent(request.emitter(), candle))
                    .orElse(true);
            if (initialSendSucceeded) {
                marketCandleRealtimeSseBroker.register(permit, request.emitter());
            } else {
                marketCandleRealtimeSseBroker.release(permit);
            }
        } catch (RuntimeException exception) {
            marketCandleRealtimeSseBroker.release(permit);
            throw exception;
        }
    }

    private boolean sendCandleEvent(
            SseEmitter emitter,
            MarketCandleResponse candle
    ) {
        try {
            emitter.send(candle);
            return true;
        } catch (IOException exception) {
            emitter.complete();
            return false;
        }
    }
}
