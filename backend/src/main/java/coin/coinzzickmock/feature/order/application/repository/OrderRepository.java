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

    Optional<FuturesOrder> claimPendingFill(
            String memberId,
            String orderId,
            double executionPrice,
            String feeType,
            double estimatedFee
    );

    FuturesOrder updateStatus(String memberId, String orderId, String status);
}
