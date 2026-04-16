package coin.coinzzickmock.feature.order.application.repository;

import coin.coinzzickmock.feature.order.domain.FuturesOrder;

import java.util.List;

public interface OrderRepository {
    FuturesOrder save(String memberId, FuturesOrder futuresOrder);

    List<FuturesOrder> findByMemberId(String memberId);
}
