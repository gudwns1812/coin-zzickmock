package coin.coinzzickmock.feature.order.application.repository;

import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    FuturesOrder save(Long memberId, FuturesOrder futuresOrder);

    List<FuturesOrder> findByMemberId(Long memberId);

    Optional<FuturesOrder> findByMemberIdAndOrderId(Long memberId, String orderId);

    List<PendingOrderCandidate> findPendingBySymbol(String symbol);

    default List<FuturesOrder> findPendingCloseOrders(
            Long memberId,
            String symbol,
            String positionSide,
            String marginMode
    ) {
        return findByMemberId(memberId).stream()
                .filter(FuturesOrder::isPending)
                .filter(FuturesOrder::isClosePositionOrder)
                .filter(order -> order.symbol().equalsIgnoreCase(symbol))
                .filter(order -> order.positionSide().equalsIgnoreCase(positionSide))
                .filter(order -> order.marginMode().equalsIgnoreCase(marginMode))
                .toList();
    }

    default List<FuturesOrder> findPendingOpenOrders(Long memberId, String symbol, String positionSide) {
        return findByMemberId(memberId).stream()
                .filter(FuturesOrder::isPending)
                .filter(FuturesOrder::isOpenPositionOrder)
                .filter(order -> !order.isConditionalOrder())
                .filter(order -> order.symbol().equalsIgnoreCase(symbol))
                .filter(order -> order.positionSide().equalsIgnoreCase(positionSide))
                .toList();
    }

    default List<FuturesOrder> findPendingConditionalCloseOrders(
            Long memberId,
            String symbol,
            String positionSide,
            String marginMode
    ) {
        return findPendingCloseOrders(memberId, symbol, positionSide, marginMode).stream()
                .filter(FuturesOrder::isConditionalCloseOrder)
                .toList();
    }

    default List<FuturesOrder> findPendingConditionalCloseOrdersBySymbol(String symbol) {
        return findPendingBySymbol(symbol).stream()
                .map(PendingOrderCandidate::order)
                .filter(FuturesOrder::isPending)
                .filter(FuturesOrder::isConditionalCloseOrder)
                .toList();
    }

    Optional<FuturesOrder> claimPendingFill(
            Long memberId,
            String orderId,
            double executionPrice,
            String feeType,
            double estimatedFee
    );

    FuturesOrder updateStatus(Long memberId, String orderId, String status);

    default boolean cancelPending(Long memberId, String orderId) {
        FuturesOrder order = findByMemberIdAndOrderId(memberId, orderId).orElse(null);
        if (order == null || !order.isPending()) {
            return false;
        }
        updateStatus(memberId, orderId, FuturesOrder.STATUS_CANCELLED);
        return true;
    }

    default FuturesOrder updateQuantityAndStatus(Long memberId, String orderId, double quantity, String status) {
        if (FuturesOrder.STATUS_CANCELLED.equalsIgnoreCase(status)) {
            return updateStatus(memberId, orderId, status);
        }
        throw new UnsupportedOperationException("Order quantity updates are not implemented");
    }
}
