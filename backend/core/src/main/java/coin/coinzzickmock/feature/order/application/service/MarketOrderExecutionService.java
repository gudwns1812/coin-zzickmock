package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.feature.market.application.realtime.MarketSummaryUpdatedEvent;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.order.application.implement.OrderPendingFillProcessor;
import coin.coinzzickmock.feature.order.application.implement.OrderPositionLiquidationProcessor;
import coin.coinzzickmock.feature.order.application.implement.OrderPositionTakeProfitStopLossProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MarketOrderExecutionService {
    private final OrderPendingFillProcessor pendingOrderFillProcessor;
    private final OrderPositionLiquidationProcessor positionLiquidationProcessor;
    private final OrderPositionTakeProfitStopLossProcessor positionTakeProfitStopLossProcessor;

    @EventListener
    @Transactional
    public void onMarketUpdated(MarketSummaryUpdatedEvent event) {
        MarketSummaryResult market = event.result();
        pendingOrderFillProcessor.fillExecutablePendingOrders(event);
        positionLiquidationProcessor.liquidateBreachedPositions(market);
        positionTakeProfitStopLossProcessor.closeTriggeredPositions(market);
    }
}
