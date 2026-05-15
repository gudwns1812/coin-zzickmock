package coin.coinzzickmock.feature.order.application.realtime;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.market.application.realtime.MarketPriceMovementDirection;
import coin.coinzzickmock.feature.market.application.realtime.MarketSummaryUpdatedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketTradePriceMovedEvent;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.application.service.AccountOrderMutationLock;
import coin.coinzzickmock.feature.order.application.service.FilledOpenOrderApplier;
import coin.coinzzickmock.feature.order.application.service.FilledOpenOrderApplier.FilledOpenOrder;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.close.PositionCloseFinalizer;
import coin.coinzzickmock.feature.position.application.close.StaleProtectiveCloseOrderCanceller;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class PendingOrderFillProcessor {
    private static final String FEE_TYPE_MAKER = "MAKER";
    private static final double MAKER_FEE_RATE = 0.00015d;

    private final OrderRepository orderRepository;
    private final PositionRepository positionRepository;
    private final PendingOrderExecutionCache pendingOrderExecutionCache;
    private final PendingLimitOrderBook pendingLimitOrderBook;
    private final PositionCloseFinalizer positionCloseFinalizer;
    private final PendingCloseOrderCapReconciler pendingCloseOrderCapReconciler;
    private final StaleProtectiveCloseOrderCanceller staleProtectiveCloseOrderCanceller;
    private final AfterCommitEventPublisher afterCommitEventPublisher;
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;
    private final FilledOpenOrderApplier filledOpenOrderApplier;
    private final AccountOrderMutationLock accountOrderMutationLock;

    @Transactional
    public void fillExecutablePendingOrders(MarketSummaryUpdatedEvent event) {
        if (!event.hasPriceMovement()) {
            return;
        }

        MarketSummaryResult market = freshMarket(event);
        if (market == null) {
            return;
        }
        MarketSummaryUpdatedEvent realtimeEvent = new MarketSummaryUpdatedEvent(
                market,
                event.previousLastPrice(),
                MarketPriceMovementDirection.between(event.previousLastPrice(), market.lastPrice())
        );
        if (!realtimeEvent.hasPriceMovement()) {
            return;
        }
        double lowerPrice = Math.min(realtimeEvent.previousLastPrice(), market.lastPrice());
        double upperPrice = Math.max(realtimeEvent.previousLastPrice(), market.lastPrice());
        boolean sellSide = realtimeEvent.direction() == MarketPriceMovementDirection.UP;
        while (fillNextExecutable(realtimeEvent, lowerPrice, upperPrice, sellSide)) {
            // Keep refreshing one fill at a time so price edits between selection and fill
            // are re-sorted before another pending order is claimed.
        }
    }

    @Transactional
    public void fillExecutablePendingOrders(MarketTradePriceMovedEvent event) {
        if (!event.hasPriceMovement()) {
            return;
        }

        MarketSummaryResult market = freshMarket(event);
        if (market == null) {
            return;
        }

        MarketSummaryUpdatedEvent realtimeEvent = new MarketSummaryUpdatedEvent(
                market,
                event.previousLastPrice(),
                event.direction()
        );
        List<PendingOrderCandidate> candidates = executableCandidates(
                realtimeEvent,
                pendingLimitOrderBook.executableCandidates(
                        market.symbol(),
                        event.previousLastPrice(),
                        event.currentLastPrice(),
                        event.direction(),
                        event.receivedAt()
                )
        );
        for (PendingOrderCandidate candidate : candidates) {
            if (!fillIfExecutable(candidate, realtimeEvent, event.receivedAt()).shouldContinue()) {
                break;
            }
        }
    }

    private boolean fillNextExecutable(
            MarketSummaryUpdatedEvent event,
            double lowerPrice,
            double upperPrice,
            boolean sellSide
    ) {
        MarketSummaryResult market = event.result();
        List<PendingOrderCandidate> candidates = executableCandidates(
                event,
                pendingOrderExecutionCache.refresh(
                        market.symbol(),
                        orderRepository.findExecutablePendingLimitOrders(
                                market.symbol(),
                                lowerPrice,
                                upperPrice,
                                sellSide
                        )
                )
        ).stream()
                .toList();
        if (candidates.isEmpty()) {
            return false;
        }
        return fillIfExecutable(candidates.get(0), event).shouldContinue();
    }

    private List<PendingOrderCandidate> executableCandidates(
            MarketSummaryUpdatedEvent event,
            List<PendingOrderCandidate> candidates
    ) {
        MarketSummaryResult market = event.result();
        return candidates.stream()
                .filter(candidate -> isExecutableInDirection(candidate.order(), event.direction()))
                .filter(candidate -> isInsideMove(candidate.order(), event.previousLastPrice(), market.lastPrice()))
                .sorted(candidateComparator(event.direction()))
                .toList();
    }

    private MarketSummaryResult freshMarket(MarketSummaryUpdatedEvent event) {
        return realtimeMarketPriceReader.freshMarket(event.result().symbol())
                .map(prices -> withRealtimePrices(event.result(), prices))
                .orElse(null);
    }

    private MarketSummaryResult freshMarket(MarketTradePriceMovedEvent event) {
        return realtimeMarketPriceReader.freshMarket(event.symbol())
                .map(prices -> new MarketSummaryResult(
                        prices.symbol(),
                        prices.displayName(),
                        event.currentLastPrice(),
                        prices.markPrice(),
                        prices.indexPrice(),
                        prices.fundingRate(),
                        prices.change24h(),
                        prices.turnover24hUsdt()
                ))
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

    private Comparator<PendingOrderCandidate> candidateComparator(MarketPriceMovementDirection direction) {
        Comparator<PendingOrderCandidate> byPrice = Comparator.comparingDouble(
                candidate -> candidate.order().limitPrice()
        );
        if (direction == MarketPriceMovementDirection.DOWN) {
            byPrice = byPrice.reversed();
        }
        return byPrice
                .thenComparing(candidate -> candidate.order().orderTime())
                .thenComparing(PendingOrderCandidate::orderId);
    }

    private boolean isExecutableInDirection(FuturesOrder order, MarketPriceMovementDirection direction) {
        if (!order.isPending() || order.isConditionalOrder() || order.limitPrice() == null) {
            return false;
        }
        if (direction == MarketPriceMovementDirection.UP) {
            return order.isSellSideLimitOrder();
        }
        if (direction == MarketPriceMovementDirection.DOWN) {
            return order.isBuySideLimitOrder();
        }
        return false;
    }

    private boolean isInsideMove(FuturesOrder order, double previousLastPrice, double currentLastPrice) {
        double lower = Math.min(previousLastPrice, currentLastPrice);
        double upper = Math.max(previousLastPrice, currentLastPrice);
        double limitPrice = order.limitPrice();
        return limitPrice >= lower && limitPrice <= upper;
    }

    private FillAttemptResult fillIfExecutable(PendingOrderCandidate candidate, MarketSummaryUpdatedEvent event) {
        return fillIfExecutable(candidate, event, null);
    }

    private FillAttemptResult fillIfExecutable(
            PendingOrderCandidate candidate,
            MarketSummaryUpdatedEvent event,
            Instant latestOrderTime
    ) {
        MarketSummaryResult market = event.result();
        accountOrderMutationLock.lock(candidate.memberId());
        Optional<FuturesOrder> reloadedOrder = reloadOrder(candidate);
        if (reloadedOrder.isEmpty()) {
            pendingOrderExecutionCache.evict(market.symbol(), candidate.memberId(), candidate.orderId());
            pendingLimitOrderBook.remove(candidate.memberId(), candidate.orderId());
            return FillAttemptResult.stopLoop();
        }

        FuturesOrder order = reloadedOrder.get();
        Optional<FillAttemptResult> preClaimResult = preClaimResult(candidate, order, event, latestOrderTime);
        if (preClaimResult.isPresent()) {
            return preClaimResult.orElseThrow();
        }

        Optional<FuturesOrder> claimed = claimPendingFill(candidate, order);
        if (claimed.isEmpty()) {
            pendingOrderExecutionCache.evict(market.symbol(), candidate.memberId(), order.orderId());
            syncOrderBookAfterClaimMiss(candidate);
            return FillAttemptResult.stopLoop();
        }

        FuturesOrder filledOrder = claimed.orElseThrow();
        applyFilledOrder(candidate.memberId(), filledOrder, order.limitPrice(), market.markPrice());
        pendingOrderExecutionCache.evict(market.symbol(), candidate.memberId(), order.orderId());
        pendingLimitOrderBook.removeAfterCommit(candidate.memberId(), order.orderId());
        publishOrderFilled(candidate.memberId(), filledOrder, order.limitPrice());
        return FillAttemptResult.continueLoop();
    }

    private Optional<FuturesOrder> reloadOrder(PendingOrderCandidate candidate) {
        return orderRepository.findByMemberIdAndOrderId(candidate.memberId(), candidate.orderId());
    }

    private Optional<FillAttemptResult> preClaimResult(
            PendingOrderCandidate candidate,
            FuturesOrder order,
            MarketSummaryUpdatedEvent event,
            Instant latestOrderTime
    ) {
        MarketSummaryResult market = event.result();
        if (!order.isPending() || order.isConditionalOrder() || order.limitPrice() == null) {
            pendingOrderExecutionCache.evict(market.symbol(), candidate.memberId(), candidate.orderId());
            pendingLimitOrderBook.remove(candidate.memberId(), candidate.orderId());
            return Optional.of(FillAttemptResult.continueLoop());
        }
        if (hasCandidatePriceChanged(candidate.order(), order)) {
            pendingOrderExecutionCache.evict(market.symbol(), candidate.memberId(), candidate.orderId());
            pendingLimitOrderBook.replaceObservedNow(candidate.memberId(), order);
            return Optional.of(FillAttemptResult.stopLoop());
        }
        if (latestOrderTime != null && order.orderTime().isAfter(latestOrderTime)) {
            pendingOrderExecutionCache.evict(market.symbol(), candidate.memberId(), candidate.orderId());
            pendingLimitOrderBook.replaceObservedNow(candidate.memberId(), order);
            return Optional.of(FillAttemptResult.continueLoop());
        }
        if (!isExecutableInDirection(order, event.direction())
                || !isInsideMove(order, event.previousLastPrice(), market.lastPrice())) {
            pendingOrderExecutionCache.evict(market.symbol(), candidate.memberId(), candidate.orderId());
            pendingLimitOrderBook.replaceObservedNow(candidate.memberId(), order);
            return Optional.of(FillAttemptResult.continueLoop());
        }
        if (order.isClosePositionOrder() && !hasEnoughOpenPosition(candidate.memberId(), order)) {
            cancelPendingCandidate(candidate.memberId(), order, market.symbol());
            return Optional.of(FillAttemptResult.continueLoop());
        }
        if (order.isOpenPositionOrder() && hasDifferentMarginPosition(candidate.memberId(), order)) {
            cancelPendingCandidate(candidate.memberId(), order, market.symbol());
            return Optional.of(FillAttemptResult.continueLoop());
        }
        return Optional.empty();
    }

    private void syncOrderBookAfterClaimMiss(PendingOrderCandidate candidate) {
        Optional<FuturesOrder> current = reloadOrder(candidate);
        if (current.filter(this::isOrderBookIndexable).isPresent()) {
            pendingLimitOrderBook.replaceObservedNow(candidate.memberId(), current.orElseThrow());
            return;
        }
        pendingLimitOrderBook.remove(candidate.memberId(), candidate.orderId());
    }

    private boolean isOrderBookIndexable(FuturesOrder order) {
        return order.isPending()
                && !order.isConditionalOrder()
                && FuturesOrder.TYPE_LIMIT.equalsIgnoreCase(order.orderType())
                && order.limitPrice() != null;
    }

    private Optional<FuturesOrder> claimPendingFill(PendingOrderCandidate candidate, FuturesOrder order) {
        double executionPrice = order.limitPrice();
        double estimatedFee = executionPrice * order.quantity() * MAKER_FEE_RATE;
        return orderRepository.claimPendingLimitFill(
                candidate.memberId(),
                order.orderId(),
                candidate.order().limitPrice(),
                executionPrice,
                FEE_TYPE_MAKER,
                estimatedFee
        );
    }

    private void applyFilledOrder(Long memberId, FuturesOrder filledOrder, double executionPrice, double markPrice) {
        if (filledOrder.isClosePositionOrder()) {
            applyFilledCloseOrder(memberId, filledOrder, executionPrice, markPrice);
        } else {
            applyFilledOpenOrder(memberId, filledOrder, executionPrice, markPrice);
        }
    }

    private void publishOrderFilled(Long memberId, FuturesOrder filledOrder, double executionPrice) {
        afterCommitEventPublisher.publish(TradingExecutionEvent.orderFilled(
                memberId,
                filledOrder.orderId(),
                filledOrder.symbol(),
                filledOrder.positionSide(),
                filledOrder.marginMode(),
                filledOrder.quantity(),
                executionPrice
        ));
    }

    private boolean hasCandidatePriceChanged(FuturesOrder candidateOrder, FuturesOrder currentOrder) {
        return Double.compare(candidateOrder.limitPrice(), currentOrder.limitPrice()) != 0;
    }

    private record FillAttemptResult(boolean shouldContinue) {
        private static FillAttemptResult continueLoop() {
            return new FillAttemptResult(true);
        }

        private static FillAttemptResult stopLoop() {
            return new FillAttemptResult(false);
        }
    }

    private void cancelPendingCandidate(Long memberId, FuturesOrder order, String symbol) {
        orderRepository.cancelPending(memberId, order.orderId());
        pendingOrderExecutionCache.evict(symbol, memberId, order.orderId());
        pendingLimitOrderBook.removeAfterCommit(memberId, order.orderId());
    }

    private boolean hasEnoughOpenPosition(Long memberId, FuturesOrder order) {
        return positionRepository.findOpenPosition(
                        memberId,
                        order.symbol(),
                        order.positionSide(),
                        order.marginMode()
                )
                .filter(position -> position.quantity() >= order.quantity())
                .isPresent();
    }

    private boolean hasDifferentMarginPosition(Long memberId, FuturesOrder order) {
        return positionRepository.findOpenPosition(memberId, order.symbol(), order.positionSide())
                .filter(position -> !position.marginMode().equalsIgnoreCase(order.marginMode()))
                .isPresent();
    }

    private void applyFilledOpenOrder(Long memberId, FuturesOrder order, double executionPrice, double markPrice) {
        filledOpenOrderApplier.apply(new FilledOpenOrder(
                memberId,
                order.orderId(),
                order.symbol(),
                order.positionSide(),
                order.marginMode(),
                order.leverage(),
                order.quantity(),
                executionPrice,
                markPrice,
                order.estimatedFee()
        ));
    }

    private void applyFilledCloseOrder(Long memberId, FuturesOrder order, double executionPrice, double markPrice) {
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
                markPrice,
                executionPrice,
                MAKER_FEE_RATE,
                PositionHistory.CLOSE_REASON_LIMIT_CLOSE
        );
        cleanupPendingCloseOrders(memberId, existing, Math.max(0, existing.quantity() - order.quantity()), executionPrice);
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
}
