package coin.coinzzickmock.feature.market.web;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
public class MarketSummaryStreamStrategy implements MarketSseStreamStrategy {
    private final MarketSummarySnapshotReader marketSummarySnapshotReader;
    private final MarketRealtimeSseBroker marketRealtimeSseBroker;

    @Override
    public MarketSseStreamKind kind() {
        return MarketSseStreamKind.SUMMARY;
    }

    @Override
    public void open(MarketSseStreamRequest request) {
        MarketRealtimeSseBroker.SseSubscriptionPermit permit = marketRealtimeSseBroker.reserve(
                request.summarySymbols(),
                request.clientKey()
        );
        try {
            boolean initialSendSucceeded = true;
            for (String symbol : request.summarySymbols()) {
                MarketSummaryResponse currentMarket = marketSummarySnapshotReader.getMarket(symbol);
                if (!sendEvent(request.emitter(), currentMarket)) {
                    initialSendSucceeded = false;
                    break;
                }
            }

            if (initialSendSucceeded) {
                marketRealtimeSseBroker.register(permit, request.emitter());
            } else {
                marketRealtimeSseBroker.release(permit);
            }
        } catch (RuntimeException exception) {
            marketRealtimeSseBroker.release(permit);
            throw exception;
        }
    }

    private boolean sendEvent(
            SseEmitter emitter,
            MarketSummaryResponse result
    ) {
        try {
            emitter.send(result);
            return true;
        } catch (IOException exception) {
            emitter.complete();
            return false;
        }
    }
}
