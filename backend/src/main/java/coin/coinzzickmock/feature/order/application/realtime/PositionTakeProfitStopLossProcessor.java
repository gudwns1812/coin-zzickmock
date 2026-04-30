package coin.coinzzickmock.feature.order.application.realtime;

import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.domain.WalletHistorySource;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.close.PositionCloseFinalizer;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PositionTakeProfitStopLossProcessor {
    private static final double TAKER_FEE_RATE = 0.0005d;

    private final OrderRepository orderRepository;
    private final PositionRepository positionRepository;
    private final PositionCloseFinalizer positionCloseFinalizer;
    private final PendingCloseOrderCapReconciler pendingCloseOrderCapReconciler;
    private final AfterCommitEventPublisher afterCommitEventPublisher;
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;

    public void closeTriggeredPositions(MarketSummaryResult market) {
        MarketSummaryResult realtimeMarket = freshMarket(market);
        if (realtimeMarket == null) {
            return;
        }
        double markPrice = realtimeMarket.markPrice();
        List<PendingOrderCandidate> candidates = orderRepository.findPendingBySymbol(realtimeMarket.symbol()).stream()
                .filter(candidate -> candidate.order().isConditionalCloseOrder())
                .filter(candidate -> candidate.order().usesMarkPriceTrigger())
                .filter(candidate -> isTriggered(candidate.order(), markPrice))
                .sorted(triggeredOrderComparator())
                .toList();
        for (PendingOrderCandidate candidate : candidates) {
            closeIfTriggered(candidate.memberId(), candidate.order(), realtimeMarket);
        }
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

    private void closeIfTriggered(String memberId, FuturesOrder candidate, MarketSummaryResult market) {
        FuturesOrder order = orderRepository.findByMemberIdAndOrderId(memberId, candidate.orderId())
                .orElse(candidate);
        if (!order.isPending() || !order.isConditionalCloseOrder() || !isTriggered(order, market.markPrice())) {
            return;
        }

        PositionSnapshot position = positionRepository.findOpenPosition(
                memberId,
                order.symbol(),
                order.positionSide(),
                order.marginMode()
        ).orElse(null);
        if (position == null) {
            orderRepository.updateStatus(memberId, order.orderId(), FuturesOrder.STATUS_CANCELLED);
            return;
        }

        PositionSnapshot marked = position.markToMarket(market.markPrice());
        if (marked.quantity() < order.quantity()) {
            order = orderRepository.updateQuantityAndStatus(
                    memberId,
                    order.orderId(),
                    marked.quantity(),
                    FuturesOrder.STATUS_PENDING
            );
        }

        double executionPrice = market.lastPrice();
        double estimatedFee = executionPrice * order.quantity() * TAKER_FEE_RATE;
        FuturesOrder filled = orderRepository.claimPendingFill(
                        memberId,
                        order.orderId(),
                        executionPrice,
                        "TAKER",
                        estimatedFee
                )
                .orElse(null);
        if (filled == null) {
            return;
        }

        String closeReason = closeReason(filled);
        var result = positionCloseFinalizer.close(
                memberId,
                marked,
                filled.quantity(),
                market.markPrice(),
                executionPrice,
                TAKER_FEE_RATE,
                closeReason,
                WalletHistorySource.positionCloseOrderFill(filled.orderId())
        );
        cancelOcoSiblings(memberId, filled);
        pendingCloseOrderCapReconciler.reconcile(
                memberId,
                marked,
                Math.max(0, marked.quantity() - filled.quantity()),
                executionPrice
        );
        afterCommitEventPublisher.publish(TradingExecutionEvent.positionClosedByTrigger(
                memberId,
                marked.symbol(),
                marked.positionSide(),
                marked.marginMode(),
                result.closedQuantity(),
                executionPrice,
                result.realizedPnl(),
                closeReason
        ));
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

    private void cancelOcoSiblings(String memberId, FuturesOrder filled) {
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
