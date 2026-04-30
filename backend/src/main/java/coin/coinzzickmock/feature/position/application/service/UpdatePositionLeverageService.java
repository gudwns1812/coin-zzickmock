package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.common.event.AfterCommitEventPublisher;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.account.domain.WalletHistorySource;
import coin.coinzzickmock.feature.leaderboard.application.event.WalletBalanceChangedEvent;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.PositionMutationResult;
import coin.coinzzickmock.feature.position.application.result.PositionSnapshotResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePositionLeverageService {
    private static final int MIN_LEVERAGE = 1;
    private static final int MAX_LEVERAGE = 50;

    private final PositionRepository positionRepository;
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final PendingCloseOrderCapReconciler pendingCloseOrderCapReconciler;
    private final AfterCommitEventPublisher afterCommitEventPublisher;

    @Transactional
    public PositionSnapshotResult update(
            Long memberId,
            String symbol,
            String positionSide,
            String marginMode,
            int leverage
    ) {
        validateLeverage(leverage);
        PositionSnapshot current = positionRepository.findOpenPosition(memberId, symbol, positionSide, marginMode)
                .orElseThrow(() -> new CoreException(ErrorCode.POSITION_NOT_FOUND));

        if (current.leverage() == leverage) {
            return toResult(memberId, current);
        }

        rejectPendingOpenOrders(memberId, symbol, positionSide);

        PositionSnapshot next = current.withLeverage(leverage);
        TradingAccount account = accountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        TradingAccount adjusted = account.adjustAvailableMarginForLeverageChange(
                current.initialMargin(),
                next.initialMargin()
        );

        accountRepository.save(
                adjusted,
                WalletHistorySource.positionLeverageChange(symbol, positionSide, marginMode, Instant.now())
        );

        PositionMutationResult mutation = positionRepository.updateWithVersion(memberId, current, next);
        if (!mutation.succeeded()) {
            throw new CoreException(ErrorCode.POSITION_CHANGED);
        }

        afterCommitEventPublisher.publish(new WalletBalanceChangedEvent(memberId));
        return toResult(memberId, mutation.updatedSnapshot());
    }

    private void validateLeverage(int leverage) {
        if (leverage < MIN_LEVERAGE || leverage > MAX_LEVERAGE) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "레버리지는 1x부터 50x까지 설정할 수 있습니다.");
        }
    }

    private void rejectPendingOpenOrders(Long memberId, String symbol, String positionSide) {
        if (!orderRepository.findPendingOpenOrders(memberId, symbol, positionSide).isEmpty()) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "미체결 오픈 주문을 취소하거나 체결한 뒤 레버리지를 변경해주세요.");
        }
    }

    private PositionSnapshotResult toResult(Long memberId, PositionSnapshot snapshot) {
        double pendingCloseQuantity = pendingCloseOrderCapReconciler.pendingCloseQuantity(
                memberId,
                snapshot
        );
        List<FuturesOrder> tpslOrders = orderRepository.findPendingConditionalCloseOrders(
                memberId,
                snapshot.symbol(),
                snapshot.positionSide(),
                snapshot.marginMode()
        );

        return new PositionSnapshotResult(
                snapshot.symbol(),
                snapshot.positionSide(),
                snapshot.marginMode(),
                snapshot.leverage(),
                snapshot.quantity(),
                snapshot.entryPrice(),
                snapshot.markPrice(),
                snapshot.liquidationPrice(),
                snapshot.unrealizedPnl(),
                snapshot.realizedPnl(),
                snapshot.initialMargin(),
                snapshot.roi(),
                snapshot.accumulatedClosedQuantity(),
                pendingCloseQuantity,
                Math.max(0, snapshot.quantity() - pendingCloseQuantity),
                triggerPrice(tpslOrders, FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT),
                triggerPrice(tpslOrders, FuturesOrder.TRIGGER_TYPE_STOP_LOSS)
        );
    }

    private Double triggerPrice(List<FuturesOrder> orders, String triggerType) {
        return orders.stream()
                .filter(order -> triggerType.equalsIgnoreCase(order.triggerType()))
                .max(java.util.Comparator.comparing(FuturesOrder::orderTime))
                .map(FuturesOrder::triggerPrice)
                .orElse(null);
    }
}
