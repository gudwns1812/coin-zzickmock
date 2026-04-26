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

    Optional<FuturesOrder> claimPendingFill(
            String memberId,
            String orderId,
            double executionPrice,
            String feeType,
            double estimatedFee
    );

    FuturesOrder updateStatus(String memberId, String orderId, String status);

    default FuturesOrder updateQuantityAndStatus(String memberId, String orderId, double quantity, String status) {
        if (FuturesOrder.STATUS_CANCELLED.equalsIgnoreCase(status)) {
            return updateStatus(memberId, orderId, status);
        }
        throw new UnsupportedOperationException("Order quantity updates are not implemented");
    }
}
