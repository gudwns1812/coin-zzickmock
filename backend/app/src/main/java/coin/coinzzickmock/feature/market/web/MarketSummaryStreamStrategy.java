package coin.coinzzickmock.feature.market.web;

import coin.coinzzickmock.feature.market.application.query.GetMarketQuery;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.application.service.GetMarketSummaryService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
@RequiredArgsConstructor
public class MarketSummaryStreamStrategy implements MarketSseStreamStrategy {
    private final GetMarketSummaryService getMarketSummaryService;
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
                MarketSummaryResult currentMarket = getMarketSummaryService.getMarket(new GetMarketQuery(symbol));
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
            MarketSummaryResult result
    ) {
        try {
            emitter.send(MarketSummaryResponse.from(result));
            return true;
        } catch (IOException exception) {
            emitter.complete();
            return false;
        }
    }
}
