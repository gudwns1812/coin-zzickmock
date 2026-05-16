package coin.coinzzickmock.feature.position.application.service;

import coin.coinzzickmock.common.error.CoreException;
import coin.coinzzickmock.common.error.ErrorCode;
import coin.coinzzickmock.feature.account.application.repository.AccountRepository;
import coin.coinzzickmock.feature.market.application.realtime.RealtimeMarketPriceReader;
import coin.coinzzickmock.feature.market.domain.MarketSnapshot;
import coin.coinzzickmock.feature.order.application.repository.OrderRepository;
import coin.coinzzickmock.feature.order.application.implement.OrderMutationLock;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;
import coin.coinzzickmock.feature.position.application.close.PendingCloseOrderCapReconciler;
import coin.coinzzickmock.feature.position.application.query.PositionSnapshotResultAssembler;
import coin.coinzzickmock.feature.position.application.repository.PositionRepository;
import coin.coinzzickmock.feature.position.application.result.PositionSnapshotResult;
import coin.coinzzickmock.feature.position.domain.PositionSnapshot;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
    private final OrderMutationLock orderMutationLock;
    private final AccountRepository accountRepository;
    private final PositionSnapshotResultAssembler positionSnapshotResultAssembler;

    @Transactional
    public PositionSnapshotResult update(
            Long memberId,
            String symbol,
            String positionSide,
            String marginMode,
            Double takeProfitPrice,
            Double stopLossPrice
    ) {
        orderMutationLock.lock(memberId);
        PositionSnapshot current = positionRepository.findOpenPosition(memberId, symbol, positionSide, marginMode)
                .orElseThrow(() -> new CoreException(ErrorCode.POSITION_NOT_FOUND));
        MarketSnapshot market = realtimeMarketPriceReader.requireFreshMarket(current.symbol());

        double markPrice = market.markPrice();
        validateTargetPrices(current, takeProfitPrice, stopLossPrice, markPrice);

        PositionSnapshot marked = current.markToMarket(markPrice);
        upsertTpslOrders(memberId, marked, takeProfitPrice, stopLossPrice);
        pendingCloseOrderCapReconciler.reconcile(memberId, marked, marked.quantity(), market.lastPrice());

        return positionSnapshotResultAssembler.assemble(memberId, marked);
    }

    private void validateTargetPrices(
            PositionSnapshot position,
            Double takeProfitPrice,
            Double stopLossPrice,
            double markPrice
    ) {
        validatePositivePrice(takeProfitPrice);
        validatePositivePrice(stopLossPrice);

        if (takeProfitPrice != null && triggersTakeProfit(position, takeProfitPrice, markPrice)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
        if (stopLossPrice != null && triggersStopLoss(position, stopLossPrice, markPrice)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validatePositivePrice(Double price) {
        if (price != null && (!Double.isFinite(price) || price <= 0)) {
            throw new CoreException(ErrorCode.INVALID_REQUEST);
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

    private void upsertTpslOrders(
            Long memberId,
            PositionSnapshot position,
            Double takeProfitPrice,
            Double stopLossPrice
    ) {
        List<FuturesOrder> currentOrders = orderRepository.findPendingConditionalCloseOrders(
                        memberId,
                        position.symbol(),
                        position.positionSide(),
                        position.marginMode()
                ).stream()
                .filter(order -> order.isTakeProfitOrder() || order.isStopLossOrder())
                .toList();

        FuturesOrder currentTakeProfit = currentOrder(currentOrders, FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT);
        FuturesOrder currentStopLoss = currentOrder(currentOrders, FuturesOrder.TRIGGER_TYPE_STOP_LOSS);
        String ocoGroupId = nextOcoGroupId(takeProfitPrice, stopLossPrice, currentTakeProfit, currentStopLoss);

        upsertTpslOrder(
                memberId,
                position,
                currentTakeProfit,
                takeProfitPrice,
                FuturesOrder.TRIGGER_TYPE_TAKE_PROFIT,
                ocoGroupId
        );
        upsertTpslOrder(
                memberId,
                position,
                currentStopLoss,
                stopLossPrice,
                FuturesOrder.TRIGGER_TYPE_STOP_LOSS,
                ocoGroupId
        );
    }

    private FuturesOrder currentOrder(List<FuturesOrder> orders, String triggerType) {
        return orders.stream()
                .filter(order -> triggerType.equalsIgnoreCase(order.triggerType()))
                .max(Comparator.comparing(FuturesOrder::orderTime))
                .orElse(null);
    }

    private String nextOcoGroupId(
            Double takeProfitPrice,
            Double stopLossPrice,
            FuturesOrder currentTakeProfit,
            FuturesOrder currentStopLoss
    ) {
        if (takeProfitPrice == null || stopLossPrice == null) {
            return null;
        }
        if (currentTakeProfit != null
                && currentStopLoss != null
                && currentTakeProfit.ocoGroupId() != null
                && Objects.equals(currentTakeProfit.ocoGroupId(), currentStopLoss.ocoGroupId())) {
            return currentTakeProfit.ocoGroupId();
        }
        if (currentTakeProfit != null && currentTakeProfit.ocoGroupId() != null) {
            return currentTakeProfit.ocoGroupId();
        }
        if (currentStopLoss != null && currentStopLoss.ocoGroupId() != null) {
            return currentStopLoss.ocoGroupId();
        }
        return UUID.randomUUID().toString();
    }

    private void upsertTpslOrder(
            Long memberId,
            PositionSnapshot position,
            FuturesOrder currentOrder,
            Double triggerPrice,
            String triggerType,
            String ocoGroupId
    ) {
        if (triggerPrice == null) {
            if (currentOrder != null) {
                orderRepository.cancelPending(memberId, currentOrder.orderId());
            }
            return;
        }
        if (currentOrder == null) {
            orderRepository.save(memberId, FuturesOrder.conditionalClose(
                    UUID.randomUUID().toString(),
                    position.symbol(),
                    position.positionSide(),
                    position.marginMode(),
                    position.leverage(),
                    position.quantity(),
                    triggerPrice,
                    triggerType,
                    ocoGroupId
            ));
            return;
        }
        if (matchesTpslTarget(currentOrder, position, triggerPrice, ocoGroupId)) {
            return;
        }
        orderRepository.updatePendingConditionalCloseOrder(
                memberId,
                currentOrder.orderId(),
                position.leverage(),
                position.quantity(),
                triggerPrice,
                ocoGroupId
        );
    }

    private boolean matchesTpslTarget(
            FuturesOrder order,
            PositionSnapshot position,
            double triggerPrice,
            String ocoGroupId
    ) {
        return order.leverage() == position.leverage()
                && Double.compare(order.quantity(), position.quantity()) == 0
                && Double.compare(order.triggerPrice(), triggerPrice) == 0
                && Objects.equals(order.ocoGroupId(), ocoGroupId);
    }

}
