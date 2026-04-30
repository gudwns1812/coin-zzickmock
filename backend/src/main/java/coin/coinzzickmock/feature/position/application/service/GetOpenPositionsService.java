package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.PositionSnapshotResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GetOpenPositionsService {
    private final PositionRepository positionRepository;
    private final OrderRepository orderRepository;
    private final PendingCloseOrderCapReconciler pendingCloseOrderCapReconciler;
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;

    @Transactional(readOnly = true)
    public List<PositionSnapshotResult> getPositions(String memberId) {
        return positionRepository.findOpenPositions(memberId).stream()
                .map(this::markToMarketForRead)
                .map(snapshot -> toResult(memberId, snapshot))
                .toList();
    }

    private PositionSnapshot markToMarketForRead(PositionSnapshot snapshot) {
        return realtimeMarketPriceReader.freshMarkPrice(snapshot.symbol())
                .map(snapshot::markToMarket)
                .orElse(snapshot);
    }

    private PositionSnapshotResult toResult(String memberId, PositionSnapshot snapshot) {
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
                .max(Comparator.comparing(FuturesOrder::orderTime))
                .map(FuturesOrder::triggerPrice)
                .orElse(null);
    }
}
