package coin.coinzzickmock.feature.order.application.realtime;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.close.PositionCloseFinalizer;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
    private final AfterCommitEventPublisher afterCommitEventPublisher;

    public void fillExecutablePendingOrders(MarketSummaryResult market) {
        List<PendingOrderCandidate> candidates = pendingOrderExecutionCache.refresh(
                market.symbol(),
                orderRepository.findPendingBySymbol(market.symbol())
        );

        for (PendingOrderCandidate candidate : candidates) {
            fillIfExecutable(candidate, market);
        }
    }

    private void fillIfExecutable(PendingOrderCandidate candidate, MarketSummaryResult market) {
        FuturesOrder order = candidate.order();
        if (!isExecutable(order, market.lastPrice())) {
            return;
        }
        if (order.isClosePositionOrder() && !hasEnoughOpenPosition(candidate.memberId(), order)) {
            orderRepository.updateStatus(candidate.memberId(), order.orderId(), FuturesOrder.STATUS_CANCELLED);
            pendingOrderExecutionCache.evict(market.symbol(), candidate.memberId(), order.orderId());
            return;
        }

        double estimatedFee = market.lastPrice() * order.quantity() * MAKER_FEE_RATE;
        Optional<FuturesOrder> claimed = orderRepository.claimPendingFill(
                candidate.memberId(),
                order.orderId(),
                market.lastPrice(),
                FEE_TYPE_MAKER,
                estimatedFee
        );

        if (claimed.isEmpty()) {
            pendingOrderExecutionCache.evict(market.symbol(), candidate.memberId(), order.orderId());
            return;
        }

        FuturesOrder filledOrder = claimed.orElseThrow();
        if (filledOrder.isClosePositionOrder()) {
            applyFilledCloseOrder(candidate.memberId(), filledOrder, market.lastPrice(), market.markPrice());
        } else {
            applyFilledOpenOrder(candidate.memberId(), filledOrder, market.lastPrice(), market.markPrice());
        }
        pendingOrderExecutionCache.evict(market.symbol(), candidate.memberId(), order.orderId());
        afterCommitEventPublisher.publish(TradingExecutionEvent.orderFilled(
                candidate.memberId(),
                filledOrder.orderId(),
                filledOrder.symbol(),
                filledOrder.positionSide(),
                filledOrder.marginMode(),
                filledOrder.quantity(),
                market.lastPrice()
        ));
    }

    private boolean isExecutable(FuturesOrder order, double lastPrice) {
        if (!order.isPending() || order.limitPrice() == null) {
            return false;
        }
        if (order.isClosePositionOrder()) {
            if ("LONG".equalsIgnoreCase(order.positionSide())) {
                return lastPrice >= order.limitPrice();
            }
            return lastPrice <= order.limitPrice();
        }
        if ("LONG".equalsIgnoreCase(order.positionSide())) {
            return lastPrice <= order.limitPrice();
        }
        return lastPrice >= order.limitPrice();
    }

    private boolean hasEnoughOpenPosition(String memberId, FuturesOrder order) {
        return positionRepository.findOpenPosition(
                        memberId,
                        order.symbol(),
                        order.positionSide(),
                        order.marginMode()
                )
                .filter(position -> position.quantity() >= order.quantity())
                .isPresent();
    }

    private void applyFilledOpenOrder(String memberId, FuturesOrder order, double executionPrice, double markPrice) {
        double initialMargin = (executionPrice * order.quantity()) / order.leverage();
        TradingAccount account = accountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        accountRepository.save(account.reserveForFilledOrder(order.estimatedFee(), initialMargin));
        afterCommitEventPublisher.publish(new WalletBalanceChangedEvent(memberId));

        PositionSnapshot existing = positionRepository.findOpenPosition(
                memberId,
                order.symbol(),
                order.positionSide(),
                order.marginMode()
        ).orElse(null);

        if (existing == null) {
            positionRepository.save(memberId, PositionSnapshot.open(
                    order.symbol(),
                    order.positionSide(),
                    order.marginMode(),
                    order.leverage(),
                    order.quantity(),
                    executionPrice,
                    markPrice
            ));
            return;
        }

        positionRepository.save(
                memberId,
                existing.increase(order.leverage(), order.quantity(), executionPrice, markPrice)
        );
    }

    private void applyFilledCloseOrder(String memberId, FuturesOrder order, double executionPrice, double markPrice) {
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
    }
}
