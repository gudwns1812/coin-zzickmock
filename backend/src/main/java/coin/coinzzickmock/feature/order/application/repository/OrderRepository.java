package coin.coinzzickmock.feature.order.application.repository;

import coin.coinzzickmock.feature.order.application.result.PendingOrderCandidate;
import coin.coinzzickmock.feature.order.domain.FuturesOrder;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    FuturesOrder save(String memberId, FuturesOrder futuresOrder);

    List<FuturesOrder> findByMemberId(String memberId);

    Optional<FuturesOrder> findByMemberIdAndOrderId(String memberId, String orderId);

    List<PendingOrderCandidate> findPendingBySymbol(String symbol);

    default List<FuturesOrder> findPendingCloseOrders(
            String memberId,
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

    default List<FuturesOrder> findPendingOpenOrders(String memberId, String symbol, String positionSide) {
        return findByMemberId(memberId).stream()
                .filter(FuturesOrder::isPending)
                .filter(FuturesOrder::isOpenPositionOrder)
                .filter(order -> !order.isConditionalOrder())
                .filter(order -> order.symbol().equalsIgnoreCase(symbol))
                .filter(order -> order.positionSide().equalsIgnoreCase(positionSide))
                .toList();
    }

    default List<FuturesOrder> findPendingConditionalCloseOrders(
            String memberId,
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
            String memberId,
            String orderId,
            double executionPrice,
            String feeType,
            double estimatedFee
    );

    FuturesOrder updateStatus(String memberId, String orderId, String status);

    default boolean cancelPending(String memberId, String orderId) {
        FuturesOrder order = findByMemberIdAndOrderId(memberId, orderId).orElse(null);
        if (order == null || !order.isPending()) {
            return false;
        }
        updateStatus(memberId, orderId, FuturesOrder.STATUS_CANCELLED);
        return true;
    }

    default FuturesOrder updateQuantityAndStatus(String memberId, String orderId, double quantity, String status) {
        if (FuturesOrder.STATUS_CANCELLED.equalsIgnoreCase(status)) {
            return updateStatus(memberId, orderId, status);
        }
        throw new UnsupportedOperationException("Order quantity updates are not implemented");
    }
}
