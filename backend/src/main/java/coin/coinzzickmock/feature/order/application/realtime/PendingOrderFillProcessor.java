package coin.coinzzickmock.feature.order.application.realtime;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.application.result.AccountMutationResult;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySource;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketPriceMovementDirection;
import coin.coinzzickmock.feature.market.application.realtime.MarketSummaryUpdatedEvent;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.close.PositionCloseFinalizer;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.position.application.result.PositionMutationResult;
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
    private final AccountRepository accountRepository;
    private final PendingOrderExecutionCache pendingOrderExecutionCache;
    private final PositionCloseFinalizer positionCloseFinalizer;
    private final PendingCloseOrderCapReconciler pendingCloseOrderCapReconciler;
    private final AfterCommitEventPublisher afterCommitEventPublisher;
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;

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
        List<PendingOrderCandidate> candidates = executableCandidates(
                realtimeEvent,
                pendingOrderExecutionCache.refresh(
                        market.symbol(),
                        orderRepository.findPendingBySymbol(market.symbol())
                )
        );

        for (PendingOrderCandidate candidate : candidates) {
            fillIfExecutable(candidate, market);
        }
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

    private void fillIfExecutable(PendingOrderCandidate candidate, MarketSummaryResult market) {
        FuturesOrder order = orderRepository.findByMemberIdAndOrderId(candidate.memberId(), candidate.orderId())
                .orElse(candidate.order());
        if (!order.isPending() || order.isConditionalOrder() || order.limitPrice() == null) {
            pendingOrderExecutionCache.evict(market.symbol(), candidate.memberId(), candidate.orderId());
            return;
        }
        if (order.isClosePositionOrder() && !hasEnoughOpenPosition(candidate.memberId(), order)) {
            cancelPendingCandidate(candidate.memberId(), order, market.symbol());
            return;
        }
        if (order.isOpenPositionOrder() && hasDifferentMarginPosition(candidate.memberId(), order)) {
            cancelPendingCandidate(candidate.memberId(), order, market.symbol());
            return;
        }

        double executionPrice = order.limitPrice();
        double estimatedFee = executionPrice * order.quantity() * MAKER_FEE_RATE;
        Optional<FuturesOrder> claimed = orderRepository.claimPendingFill(
                candidate.memberId(),
                order.orderId(),
                executionPrice,
                FEE_TYPE_MAKER,
                estimatedFee
        );

        if (claimed.isEmpty()) {
            pendingOrderExecutionCache.evict(market.symbol(), candidate.memberId(), order.orderId());
            return;
        }

        FuturesOrder filledOrder = claimed.orElseThrow();
        if (filledOrder.isClosePositionOrder()) {
            applyFilledCloseOrder(candidate.memberId(), filledOrder, executionPrice, market.markPrice());
        } else {
            applyFilledOpenOrder(candidate.memberId(), filledOrder, executionPrice, market.markPrice());
        }
        pendingOrderExecutionCache.evict(market.symbol(), candidate.memberId(), order.orderId());
        afterCommitEventPublisher.publish(TradingExecutionEvent.orderFilled(
                candidate.memberId(),
                filledOrder.orderId(),
                filledOrder.symbol(),
                filledOrder.positionSide(),
                filledOrder.marginMode(),
                filledOrder.quantity(),
                executionPrice
        ));
    }

    private void cancelPendingCandidate(Long memberId, FuturesOrder order, String symbol) {
        orderRepository.cancelPending(memberId, order.orderId());
        pendingOrderExecutionCache.evict(symbol, memberId, order.orderId());
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
        PositionSnapshot existing = positionRepository.findOpenPosition(
                memberId,
                order.symbol(),
                order.positionSide()
        ).orElse(null);

        if (existing != null && !existing.marginMode().equalsIgnoreCase(order.marginMode())) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "기존 포지션과 다른 마진 모드의 대기 주문은 체결할 수 없습니다.");
        }

        int effectiveLeverage = existing == null ? order.leverage() : existing.leverage();
        double initialMargin = (executionPrice * order.quantity()) / effectiveLeverage;
        TradingAccount account = accountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        validateAccountMutation(accountRepository.updateWithVersion(
                account,
                account.reserveForFilledOrder(order.estimatedFee(), initialMargin),
                WalletHistorySource.orderFill(order.orderId())
        ));
        afterCommitEventPublisher.publish(new WalletBalanceChangedEvent(memberId));

        if (existing == null) {
            positionRepository.save(memberId, PositionSnapshot.open(
                    order.symbol(),
                    order.positionSide(),
                    order.marginMode(),
                    effectiveLeverage,
                    order.quantity(),
                    executionPrice,
                    markPrice,
                    order.estimatedFee()
            ));
            return;
        }

        validatePositionMutation(positionRepository.updateWithVersion(
                memberId,
                existing,
                existing.increase(effectiveLeverage, order.quantity(), executionPrice, markPrice, order.estimatedFee())
        ));
    }

    private void validateAccountMutation(AccountMutationResult mutationResult) {
        if (mutationResult.succeeded()) {
            return;
        }
        if (mutationResult.status() == AccountMutationResult.Status.NOT_FOUND) {
            throw new CoreException(ErrorCode.ACCOUNT_NOT_FOUND);
        }
        throw new CoreException(ErrorCode.ACCOUNT_CHANGED);
    }

    private void validatePositionMutation(PositionMutationResult mutationResult) {
        if (mutationResult.succeeded()) {
            return;
        }
        if (mutationResult.status() == PositionMutationResult.Status.NOT_FOUND) {
            throw new CoreException(ErrorCode.POSITION_NOT_FOUND);
        }
        throw new CoreException(ErrorCode.POSITION_CHANGED);
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
                PositionHistory.CLOSE_REASON_LIMIT_CLOSE,
                WalletHistorySource.positionCloseOrderFill(order.orderId())
        );
        pendingCloseOrderCapReconciler.reconcile(
                memberId,
                existing,
                Math.max(0, existing.quantity() - order.quantity()),
                executionPrice
        );
    }
}
