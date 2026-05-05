package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.account.domain.TradingAccount;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.PositionSnapshotResult;
import coin.coinzzickmock.feature.position.domain.CrossLiquidationEstimate;
import coin.coinzzickmock.feature.position.domain.LiquidationPolicy;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GetOpenPositionsService {
    private final PositionRepository positionRepository;
    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final PendingCloseOrderCapReconciler pendingCloseOrderCapReconciler;
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;
    private final LiquidationPolicy liquidationPolicy;

    @Transactional(readOnly = true)
    public List<PositionSnapshotResult> getPositions(Long memberId) {
        List<PositionSnapshot> markedPositions = positionRepository.findOpenPositions(memberId).stream()
                .map(this::markToMarketForRead)
                .toList();
        TradingAccount account = accountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CoreException(ErrorCode.ACCOUNT_NOT_FOUND));
        return markedPositions.stream()
                .map(snapshot -> toResult(memberId, account.walletBalance(), markedPositions, snapshot))
                .toList();
    }

    private PositionSnapshot markToMarketForRead(PositionSnapshot snapshot) {
        return realtimeMarketPriceReader.freshMarkPrice(snapshot.symbol())
                .map(snapshot::markToMarket)
                .orElse(snapshot);
    }

    private PositionSnapshotResult toResult(
            Long memberId,
            double walletBalance,
            List<PositionSnapshot> positions,
            PositionSnapshot snapshot
    ) {
        List<FuturesOrder> tpslOrders = orderRepository.findPendingConditionalCloseOrders(
                memberId,
                snapshot.symbol(),
                snapshot.positionSide(),
                snapshot.marginMode()
        );
        double pendingCloseQuantity = pendingCloseOrderCapReconciler.pendingCloseQuantity(
                memberId,
                snapshot
        );
        ResolvedLiquidationPrice liquidation = resolveLiquidationPrice(walletBalance, positions, snapshot);

        return new PositionSnapshotResult(
                snapshot.symbol(),
                snapshot.positionSide(),
                snapshot.marginMode(),
                snapshot.leverage(),
                snapshot.quantity(),
                snapshot.entryPrice(),
                snapshot.markPrice(),
                liquidation.price(),
                liquidation.type(),
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

    private ResolvedLiquidationPrice resolveLiquidationPrice(
            double walletBalance,
            List<PositionSnapshot> positions,
            PositionSnapshot snapshot
    ) {
        if (snapshot.isCrossMargin()) {
            CrossLiquidationEstimate estimate = liquidationPolicy.estimateCrossLiquidationPrice(
                    walletBalance,
                    positions,
                    snapshot.symbol()
            );
            return new ResolvedLiquidationPrice(estimate.liquidationPrice(), estimate.liquidationPriceType());
        }
        return new ResolvedLiquidationPrice(snapshot.liquidationPrice(), isolatedLiquidationPriceType(snapshot));
    }

    private String isolatedLiquidationPriceType(PositionSnapshot snapshot) {
        return snapshot.liquidationPrice() == null
                ? CrossLiquidationEstimate.TYPE_UNAVAILABLE
                : CrossLiquidationEstimate.TYPE_EXACT;
    }

    private Double triggerPrice(List<FuturesOrder> orders, String triggerType) {
        return orders.stream()
                .filter(order -> triggerType.equalsIgnoreCase(order.triggerType()))
                .max(Comparator.comparing(FuturesOrder::orderTime))
                .map(FuturesOrder::triggerPrice)
                .orElse(null);
    }

    private record ResolvedLiquidationPrice(Double price, String type) {
    }
}
