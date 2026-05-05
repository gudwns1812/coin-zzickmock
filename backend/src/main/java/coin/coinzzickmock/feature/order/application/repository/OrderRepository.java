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

    boolean existsPendingByMemberId(Long memberId);

    List<PendingOrderCandidate> findExecutablePendingLimitOrders(
            String symbol,
            double lowerPrice,
            double upperPrice,
            boolean sellSide
    );

    List<FuturesOrder> findPendingCloseOrders(
            Long memberId,
            String symbol,
            String positionSide,
            String marginMode
    );

    List<FuturesOrder> findPendingOpenOrders(Long memberId, String symbol, String positionSide);

    List<FuturesOrder> findPendingConditionalCloseOrders(
            Long memberId,
            String symbol,
            String positionSide,
            String marginMode
    );

    List<FuturesOrder> findPendingConditionalCloseOrdersBySymbol(String symbol);

    Optional<FuturesOrder> claimPendingFill(
            Long memberId,
            String orderId,
            double executionPrice,
            String feeType,
            double estimatedFee
    );

    FuturesOrder updateStatus(Long memberId, String orderId, String status);

    FuturesOrder updatePendingConditionalCloseOrder(
            Long memberId,
            String orderId,
            int leverage,
            double quantity,
            double triggerPrice,
            String ocoGroupId
    );

    int cancelPendingOrders(Long memberId, List<String> orderIds);

    boolean cancelPending(Long memberId, String orderId);

    FuturesOrder updateQuantityAndStatus(Long memberId, String orderId, double quantity, String status);

    int capPendingOrderQuantity(Long memberId, List<String> orderIds, double maxQuantity);
}
