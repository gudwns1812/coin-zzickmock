package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.PositionSnapshotResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePositionTpslService {
    private final PositionRepository positionRepository;
    private final OrderRepository orderRepository;
    private final PendingCloseOrderCapReconciler pendingCloseOrderCapReconciler;
    private final RealtimeMarketPriceReader realtimeMarketPriceReader;

    @Transactional
    public PositionSnapshotResult update(
            Long memberId,
            String symbol,
            String positionSide,
            String marginMode,
            Double takeProfitPrice,
            Double stopLossPrice
    ) {
        PositionSnapshot current = positionRepository.findOpenPosition(memberId, symbol, positionSide, marginMode)
                .orElseThrow(() -> new CoreException(ErrorCode.POSITION_NOT_FOUND));
        MarketSnapshot market = realtimeMarketPriceReader.requireFreshMarket(current.symbol());

        double markPrice = market.markPrice();
        validateTargetPrices(current, takeProfitPrice, stopLossPrice, markPrice);

        PositionSnapshot marked = current.markToMarket(markPrice);
        cancelExistingTpslOrders(memberId, marked);
        createReplacementOrders(memberId, marked, takeProfitPrice, stopLossPrice);
        pendingCloseOrderCapReconciler.reconcile(memberId, marked, marked.quantity(), market.lastPrice());

        return toResult(memberId, marked);
    }

    private void validateTargetPrices(
            PositionSnapshot position,
            Double takeProfitPrice,
            Double stopLossPrice,
            double markPrice
    ) {
        validatePositivePrice(takeProfitPrice, "TP 가격을 확인해주세요.");
        validatePositivePrice(stopLossPrice, "SL 가격을 확인해주세요.");

        if (takeProfitPrice != null && triggersTakeProfit(position, takeProfitPrice, markPrice)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "이미 발동된 TP 가격은 설정할 수 없습니다.");
        }
        if (stopLossPrice != null && triggersStopLoss(position, stopLossPrice, markPrice)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, "이미 발동된 SL 가격은 설정할 수 없습니다.");
        }
    }

    private void validatePositivePrice(Double price, String message) {
        if (price != null && (!Double.isFinite(price) || price <= 0)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST, message);
        }
    }

    private boolean triggersTakeProfit(PositionSnapshot position, double takeProfitPrice, double markPrice) {
        return isLong(position)
                ? markPrice >= takeProfitPrice
                : markPrice <= takeProfitPrice;
    }

    private boolean triggersStopLoss(PositionSnapshot position, double stopLossPrice, double markPrice) {
        return isLong(position)
                ? markPrice <= stopLossPrice
                : markPrice >= stopLossPrice;
    }

    private boolean isLong(PositionSnapshot position) {
        return "LONG".equalsIgnoreCase(position.positionSide());
    }

    private void cancelExistingTpslOrders(Long memberId, PositionSnapshot position) {
        orderRepository.findPendingConditionalCloseOrders(
                        memberId,
                        position.symbol(),
                        position.positionSide(),
                        position.marginMode()
                ).stream()
                .filter(order -> order.isTakeProfitOrder() || order.isStopLossOrder())
                .forEach(order -> orderRepository.updateStatus(memberId, order.orderId(), FuturesOrder.STATUS_CANCELLED));
    }

    private void createReplacementOrders(
            Long memberId,
            PositionSnapshot position,
            Double takeProfitPrice,
            Double stopLossPrice
    ) {
        String ocoGroupId = takeProfitPrice != null && stopLossPrice != null ? UUID.randomUUID().toString() : null;
        if (takeProfitPrice != null) {
            orderRepository.save(memberId, FuturesOrder.conditionalClose(
                    UUID.randomUUID().toString(),
                    position.symbol(),
                    position.positionSide(),
                    position.marginMode(),
                    position.leverage(),
                    position.quantity(),
                    takeProfitPrice,
                    FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                    ocoGroupId
            ));
        }
        if (stopLossPrice != null) {
            orderRepository.save(memberId, FuturesOrder.conditionalClose(
                    UUID.randomUUID().toString(),
                    position.symbol(),
                    position.positionSide(),
                    position.marginMode(),
                    position.leverage(),
                    position.quantity(),
                    stopLossPrice,
                    FuturesOrder.TRIGGER_TYPE_STOP_LOSS,
                    ocoGroupId
            ));
        }
    }

    private PositionSnapshotResult toResult(Long memberId, PositionSnapshot snapshot) {
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
