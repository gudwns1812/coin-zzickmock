package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.feature.market.application.realtime.MarketSummaryUpdatedEvent;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.order.application.realtime.PendingOrderFillProcessor;
import coin.coinzzickmock.feature.order.application.realtime.PositionLiquidationProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarketOrderExecutionService {
    private final PendingOrderFillProcessor pendingOrderFillProcessor;
    private final PositionLiquidationProcessor positionLiquidationProcessor;

    @EventListener
    @Transactional
    public void onMarketUpdated(MarketSummaryUpdatedEvent event) {
        MarketSummaryResult market = event.result();
        pendingOrderFillProcessor.fillExecutablePendingOrders(market);
        positionLiquidationProcessor.liquidateBreachedPositions(market);
    }
}
