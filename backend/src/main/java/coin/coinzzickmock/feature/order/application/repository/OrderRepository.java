package coin.coinzzickmock.feature.order.application.repository;

import coin.coinzzickmock.feature.order.domain.FuturesOrder;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {
    FuturesOrder save(String memberId, FuturesOrder futuresOrder);

    List<FuturesOrder> findByMemberId(String memberId);

    Optional<FuturesOrder> findByMemberIdAndOrderId(String memberId, String orderId);

    FuturesOrder updateStatus(String memberId, String orderId, String status);
}
