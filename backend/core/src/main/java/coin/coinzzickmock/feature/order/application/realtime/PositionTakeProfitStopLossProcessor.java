package coin.coinzzickmock.feature.order.application.realtime;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.implement.OrderMutationLock;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.close.PositionCloseFinalizer;
import coin.coinzzickmock.feature.position.application.close.StaleProtectiveCloseOrderCanceller;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PositionTakeProfitStopLossProcessor {
    private static final double TAKER_FEE_RATE = 0.0005d;

    private final OrderRepository orderRepository;
    private final PositionRepository positionRepository;
    private final PositionCloseFinalizer positionCloseFinalizer;
    private final PendingCloseOrderCapReconciler pendingCloseOrderCapReconciler;
    private final StaleProtectiveCloseOrderCanceller staleProtectiveCloseOrderCanceller;
    private final AfterCommitEventPublisher afterCommitEventPublisher;
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;
    private final OrderMutationLock orderMutationLock;

    @Transactional
    public void closeTriggeredPositions(MarketSummaryResult market) {
        MarketSummaryResult realtimeMarket = freshMarket(market);
        if (realtimeMarket == null) {
            return;
        }
        triggeredCandidates(realtimeMarket).forEach(candidate ->
                closeIfTriggered(candidate.memberId(), candidate.order(), realtimeMarket)
        );
    }

    private List<PendingOrderCandidate> triggeredCandidates(MarketSummaryResult realtimeMarket) {
        double markPrice = realtimeMarket.markPrice();
        return orderRepository.findPendingBySymbol(realtimeMarket.symbol()).stream()
                .filter(candidate -> candidate.order().isConditionalCloseOrder())
                .filter(candidate -> candidate.order().usesMarkPriceTrigger())
                .filter(candidate -> isTriggered(candidate.order(), markPrice))
                .sorted(triggeredOrderComparator())
                .toList();
    }

    private MarketSummaryResult freshMarket(MarketSummaryResult eventMarket) {
        return realtimeMarketPriceReader.freshMarket(eventMarket.symbol())
                .map(prices -> withRealtimePrices(eventMarket, prices))
                .orElse(null);
    }

    private MarketSummaryResult withRealtimePrices(MarketSummaryResult eventMarket, MarketSnapshot prices) {
        return new MarketSummaryResult(
                eventMarket.symbol(),
                eventMarket.displayName(),
                prices.lastPrice(),
                prices.markPrice(),
                prices.indexPrice(),
                eventMarket.fundingRate(),
                eventMarket.change24h(),
                eventMarket.turnover24hUsdt(),
                eventMarket.serverTime(),
                eventMarket.nextFundingAt(),
                eventMarket.fundingIntervalHours()
        );
    }

    private void closeIfTriggered(Long memberId, FuturesOrder candidate, MarketSummaryResult market) {
        lockAccountOrderMutation(memberId);
        Optional<FuturesOrder> reloadedOrder = reloadOrder(memberId, candidate);
        if (reloadedOrder.isEmpty()) {
            return;
        }

        FuturesOrder order = reloadedOrder.get();
        if (!isStillTriggered(order, market.markPrice())) {
            return;
        }

        PositionSnapshot position = openPositionOrCancelStale(memberId, order);
        if (position == null) {
            return;
        }

        PositionSnapshot marked = position.markToMarket(market.markPrice());
        if (Double.compare(marked.quantity(), 0d) == 0) {
            staleProtectiveCloseOrderCanceller.cancel(memberId, marked);
            return;
        }
        order = syncQuantityToCurrentPosition(memberId, order, marked);

        FuturesOrder filled = claimTriggeredOrder(memberId, order, market.lastPrice());
        if (filled == null) {
            return;
        }

        finishTriggeredClose(memberId, marked, filled, market);
    }

    private void lockAccountOrderMutation(Long memberId) {
        // OrderMutationLock obtains a transactional row lock; Spring releases it when this transaction ends.
        orderMutationLock.lock(memberId);
    }

    private Optional<FuturesOrder> reloadOrder(Long memberId, FuturesOrder candidate) {
        return orderRepository.findByMemberIdAndOrderId(memberId, candidate.orderId());
    }

    private boolean isStillTriggered(FuturesOrder order, double markPrice) {
        return order.isPending()
                && order.isConditionalCloseOrder()
                && isTriggered(order, markPrice);
    }

    private PositionSnapshot openPositionOrCancelStale(Long memberId, FuturesOrder order) {
        PositionSnapshot position = positionRepository.findOpenPosition(
                memberId,
                order.symbol(),
                order.positionSide(),
                order.marginMode()
        ).orElse(null);
        if (position == null) {
            staleProtectiveCloseOrderCanceller.cancel(
                    memberId,
                    order.symbol(),
                    order.positionSide(),
                    order.marginMode()
            );
        }
        return position;
    }

    private FuturesOrder syncQuantityToCurrentPosition(Long memberId, FuturesOrder order, PositionSnapshot position) {
        if (Double.compare(position.quantity(), order.quantity()) == 0) {
            return order;
        }
        return orderRepository.updateQuantityAndStatus(
                memberId,
                order.orderId(),
                position.quantity(),
                FuturesOrder.STATUS_PENDING
        );
    }

    private FuturesOrder claimTriggeredOrder(Long memberId, FuturesOrder order, double executionPrice) {
        double estimatedFee = executionPrice * order.quantity() * TAKER_FEE_RATE;
        return orderRepository.claimPendingFill(
                        memberId,
                        order.orderId(),
                        executionPrice,
                        "TAKER",
                        estimatedFee
                )
                .orElse(null);
    }

    private void finishTriggeredClose(
            Long memberId,
            PositionSnapshot position,
            FuturesOrder filled,
            MarketSummaryResult market
    ) {
        String closeReason = closeReason(filled);
        double executionPrice = market.lastPrice();
        var result = positionCloseFinalizer.close(
                memberId,
                position,
                filled.quantity(),
                market.markPrice(),
                executionPrice,
                TAKER_FEE_RATE,
                closeReason
        );
        cancelOcoSiblings(memberId, filled);
        cleanupPendingCloseOrders(memberId, position, Math.max(0, position.quantity() - result.closedQuantity()), executionPrice);
        publishPositionClosedByTrigger(memberId, position, result.closedQuantity(), executionPrice, result.realizedPnl(), closeReason);
    }

    private void publishPositionClosedByTrigger(
            Long memberId,
            PositionSnapshot position,
            double closedQuantity,
            double executionPrice,
            double realizedPnl,
            String closeReason
    ) {
        afterCommitEventPublisher.publish(TradingExecutionEvent.positionClosedByTrigger(
                memberId,
                position.symbol(),
                position.positionSide(),
                position.marginMode(),
                closedQuantity,
                executionPrice,
                realizedPnl,
                closeReason
        ));
    }

    private void cleanupPendingCloseOrders(
            Long memberId,
            PositionSnapshot position,
            double remainingQuantity,
            double currentPrice
    ) {
        pendingCloseOrderCapReconciler.reconcile(memberId, position, remainingQuantity, currentPrice);
        if (remainingQuantity <= 0) {
            staleProtectiveCloseOrderCanceller.cancel(memberId, position);
        }
    }

    private boolean isTriggered(FuturesOrder order, double markPrice) {
        if (order.triggerPrice() == null) {
            return false;
        }
        boolean isLong = "LONG".equalsIgnoreCase(order.positionSide());
        if (order.isTakeProfitOrder()) {
            return isLong ? markPrice >= order.triggerPrice() : markPrice <= order.triggerPrice();
        }
        if (order.isStopLossOrder()) {
            return isLong ? markPrice <= order.triggerPrice() : markPrice >= order.triggerPrice();
        }
        return false;
    }

    private Comparator<PendingOrderCandidate> triggeredOrderComparator() {
        return Comparator
                .comparing((PendingOrderCandidate candidate) -> candidate.order().orderTime())
                .thenComparing(PendingOrderCandidate::orderId);
    }

    private String closeReason(FuturesOrder order) {
        return order.isTakeProfitOrder()
                ? PositionHistory.CLOSE_REASON_TAKE_PROFIT
                : PositionHistory.CLOSE_REASON_STOP_LOSS;
    }

    private void cancelOcoSiblings(Long memberId, FuturesOrder filled) {
        if (filled.ocoGroupId() == null || filled.ocoGroupId().isBlank()) {
            return;
        }
        orderRepository.findPendingConditionalCloseOrders(
                        memberId,
                        filled.symbol(),
                        filled.positionSide(),
                        filled.marginMode()
                ).stream()
                .filter(order -> filled.ocoGroupId().equals(order.ocoGroupId()))
                .filter(order -> !filled.orderId().equals(order.orderId()))
                .forEach(order -> orderRepository.updateStatus(memberId, order.orderId(), FuturesOrder.STATUS_CANCELLED));
    }
}
