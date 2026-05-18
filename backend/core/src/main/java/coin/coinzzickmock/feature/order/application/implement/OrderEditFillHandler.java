package coin.coinzzickmock.feature.order.application.implement;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.implement.OrderPendingLimitOrderBook;
import coin.coinzzickmock.feature.order.application.dto.TradingExecutionEvent;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.implement.OrderFillApplier.FilledOpenOrder;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.order.domain.OrderPlacementDecision;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.close.PositionCloseFinalizer;
import coin.coinzzickmock.feature.position.application.close.StaleProtectiveCloseOrderCanceller;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderEditFillHandler {
    private static final double QUANTITY_EPSILON = 1e-9;

    private final OrderRepository orderRepository;
    private final PositionRepository positionRepository;
    private final OrderFillApplier orderFillApplier;
    private final PositionCloseFinalizer positionCloseFinalizer;
    private final PendingCloseOrderCapReconciler pendingCloseOrderCapReconciler;
    private final StaleProtectiveCloseOrderCanceller staleProtectiveCloseOrderCanceller;
    private final AfterCommitEventPublisher afterCommitEventPublisher;
    private final OrderPendingLimitOrderBook pendingLimitOrderBook;

    public FuturesOrder fill(Long memberId, FuturesOrder order, MarketSnapshot market, OrderPlacementDecision decision) {
        double estimatedFee = decision.estimatedFee(order.quantity());
        FuturesOrder filledOrder = orderRepository.claimPendingLimitFill(
                memberId,
                order.orderId(),
                order.limitPrice(),
                decision.executionPrice(),
                decision.feeType(),
                estimatedFee
        ).orElseThrow(() -> new CoreException(ErrorCode.INVALID_REQUEST));

        if (filledOrder.isClosePositionOrder()) {
            applyFilledCloseOrder(memberId, filledOrder, market, decision);
        } else {
            applyFilledOpenOrder(memberId, filledOrder, market, estimatedFee);
        }
        pendingLimitOrderBook.removeAfterCommit(memberId, filledOrder.orderId());
        publishFilledEvent(memberId, filledOrder, decision.executionPrice());
        return filledOrder;
    }

    private void applyFilledOpenOrder(
            Long memberId,
            FuturesOrder order,
            MarketSnapshot market,
            double estimatedFee
    ) {
        orderFillApplier.apply(new FilledOpenOrder(
                memberId,
                order.orderId(),
                order.symbol(),
                order.positionSide(),
                order.marginMode(),
                order.leverage(),
                order.quantity(),
                order.executionPrice(),
                market.markPrice(),
                estimatedFee
        ));
    }

    private void applyFilledCloseOrder(
            Long memberId,
            FuturesOrder order,
            MarketSnapshot market,
            OrderPlacementDecision decision
    ) {
        PositionSnapshot existing = positionRepository.findOpenPosition(
                memberId,
                order.symbol(),
                order.positionSide(),
                order.marginMode()
        ).orElseThrow(() -> new CoreException(ErrorCode.POSITION_NOT_FOUND));
        positionCloseFinalizer.close(
                memberId,
                existing,
                order.quantity(),
                market.markPrice(),
                decision.executionPrice(),
                decision.feeRate(),
                PositionHistory.CLOSE_REASON_MANUAL
        );
        cleanupPendingCloseOrders(memberId, existing, Math.max(0, existing.quantity() - order.quantity()), market.lastPrice());
    }

    private void cleanupPendingCloseOrders(
            Long memberId,
            PositionSnapshot position,
            double remainingQuantity,
            double currentPrice
    ) {
        pendingCloseOrderCapReconciler.reconcile(memberId, position, remainingQuantity, currentPrice);
        if (remainingQuantity <= QUANTITY_EPSILON) {
            staleProtectiveCloseOrderCanceller.cancel(memberId, position);
        }
    }

    private void publishFilledEvent(Long memberId, FuturesOrder order, double executionPrice) {
        afterCommitEventPublisher.publish(TradingExecutionEvent.orderFilled(
                memberId,
                order.orderId(),
                order.symbol(),
                order.positionSide(),
                order.marginMode(),
                order.quantity(),
                executionPrice
        ));
    }
}
