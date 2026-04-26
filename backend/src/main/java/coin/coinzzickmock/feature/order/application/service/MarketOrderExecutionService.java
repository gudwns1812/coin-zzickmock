package coin.coinzzickmock.feature.order.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import coin.coinzzickmock.feature.market.application.realtime.MarketSummaryUpdatedEvent;
import coin.coinzzickmock.feature.market.application.result.MarketSummaryResult;
import coin.coinzzickmock.feature.order.application.realtime.PendingOrderExecutionCache;
import coin.coinzzickmock.feature.order.application.realtime.TradingExecutionEvent;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.OpenPositionCandidate;
import coin.coinzzickmock.feature.position.domain.CrossLiquidationAssessment;
import coin.coinzzickmock.feature.position.domain.IsolatedLiquidationAssessment;
import coin.coinzzickmock.feature.position.domain.LiquidationPolicy;
import coin.coinzzickmock.feature.position.domain.PositionHistory;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import coin.coinzzickmock.feature.position.application.service.PositionCloseFinalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class MarketOrderExecutionService {
    private static final String FEE_TYPE_MAKER = "MAKER";
    private static final double MAKER_FEE_RATE = 0.00015d;
    private static final double TAKER_FEE_RATE = 0.0005d;

    private final OrderRepository orderRepository;
    private final PositionRepository positionRepository;
    private final AccountRepository accountRepository;
    private final PendingOrderExecutionCache pendingOrderExecutionCache;
    private final LiquidationPolicy liquidationPolicy;
    private final PositionCloseFinalizer positionCloseFinalizer;
    private final ApplicationEventPublisher applicationEventPublisher;

    @EventListener
    @Transactional
    public void onMarketUpdated(MarketSummaryUpdatedEvent event) {
        MarketSummaryResult market = event.result();
        fillExecutablePendingOrders(market);
        liquidateBreachedPositions(market);
    }

    private void fillExecutablePendingOrders(MarketSummaryResult market) {
        List<PendingOrderCandidate> candidates = pendingOrderExecutionCache.refresh(
                market.symbol(),
                orderRepository.findPendingBySymbol(market.symbol())
        );

        for (PendingOrderCandidate candidate : candidates) {
            FuturesOrder order = candidate.order();
            if (!isExecutable(order, market.lastPrice())) {
                continue;
            }
            if (order.isClosePositionOrder() && !hasEnoughOpenPosition(candidate.memberId(), order)) {
                orderRepository.updateStatus(candidate.memberId(), order.orderId(), FuturesOrder.STATUS_CANCELLED);
                pendingOrderExecutionCache.evict(market.symbol(), candidate.memberId(), order.orderId());
                continue;
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
                continue;
            }

            FuturesOrder filledOrder = claimed.orElseThrow();
            if (filledOrder.isClosePositionOrder()) {
                applyFilledCloseOrder(candidate.memberId(), filledOrder, market.lastPrice(), market.markPrice());
            } else {
                applyFilledOrder(candidate.memberId(), filledOrder, market.lastPrice(), market.markPrice());
            }
            pendingOrderExecutionCache.evict(market.symbol(), candidate.memberId(), order.orderId());
            publishAfterCommit(TradingExecutionEvent.orderFilled(
                    candidate.memberId(),
                    order.orderId(),
                    order.symbol(),
                    order.positionSide(),
                    order.marginMode(),
                    order.quantity(),
                    market.lastPrice()
            ));
        }
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

    private void applyFilledOrder(String memberId, FuturesOrder order, double executionPrice, double markPrice) {
        double initialMargin = (executionPrice * order.quantity()) / order.leverage();
        TradingAccount account = accountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        accountRepository.save(account.reserveForFilledOrder(order.estimatedFee(), initialMargin));
        publishAfterCommit(new WalletBalanceChangedEvent(memberId));

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

    private void liquidateBreachedPositions(MarketSummaryResult market) {
        List<OpenPositionCandidate> candidates = positionRepository.findOpenBySymbol(market.symbol());
        Set<String> assessedCrossMembers = new HashSet<>();

        for (OpenPositionCandidate candidate : candidates) {
            PositionSnapshot marked = candidate.position().markToMarket(market.markPrice());
            if (marked.isCrossMargin()) {
                if (assessedCrossMembers.add(candidate.memberId())) {
                    liquidateCrossIfNeeded(candidate.memberId(), market);
                }
                continue;
            }

            IsolatedLiquidationAssessment assessment = liquidationPolicy.assessIsolated(marked, market.markPrice());
            if (assessment.breached()) {
                liquidate(candidate.memberId(), marked, market.lastPrice(), market.markPrice());
            } else {
                positionRepository.save(candidate.memberId(), marked);
            }
        }
    }

    private void liquidateCrossIfNeeded(String memberId, MarketSummaryResult market) {
        TradingAccount account = accountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        List<PositionSnapshot> positions = positionRepository.findOpenPositions(memberId).stream()
                .map(position -> position.symbol().equalsIgnoreCase(market.symbol())
                        ? position.markToMarket(market.markPrice())
                        : position)
                .toList();

        CrossLiquidationAssessment assessment = liquidationPolicy.assessCross(account.availableMargin(), positions);
        if (!assessment.breached()) {
            saveMarkedPositions(memberId, market, positions);
            return;
        }

        assessment.liquidationCandidate()
                .filter(candidate -> candidate.position().symbol().equalsIgnoreCase(market.symbol()))
                .ifPresent(candidate -> liquidate(memberId, candidate.position(), market.lastPrice(), market.markPrice()));
    }

    private void saveMarkedPositions(String memberId, MarketSummaryResult market, List<PositionSnapshot> positions) {
        positions.stream()
                .filter(position -> position.symbol().equalsIgnoreCase(market.symbol()))
                .forEach(position -> positionRepository.save(memberId, position));
    }

    private void liquidate(String memberId, PositionSnapshot position, double executionPrice, double markPrice) {
        var result = positionCloseFinalizer.close(
                memberId,
                position,
                position.quantity(),
                markPrice,
                executionPrice,
                TAKER_FEE_RATE,
                PositionHistory.CLOSE_REASON_LIQUIDATION
        );
        publishAfterCommit(TradingExecutionEvent.positionLiquidated(
                memberId,
                position.symbol(),
                position.positionSide(),
                position.marginMode(),
                result.closedQuantity(),
                executionPrice,
                result.realizedPnl()
        ));
    }

    private void publishAfterCommit(Object event) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            applicationEventPublisher.publishEvent(event);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                applicationEventPublisher.publishEvent(event);
            }
        });
    }
}
