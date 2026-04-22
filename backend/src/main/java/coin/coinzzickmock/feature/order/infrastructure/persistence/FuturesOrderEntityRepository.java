package coin.coinzzickmock.feature.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FuturesOrderEntityRepository extends JpaRepository<FuturesOrderEntity, Long> {
    List<FuturesOrderEntity> findAllByMemberIdOrderByCreatedAtDesc(String memberId);

    Optional<FuturesOrderEntity> findByMemberIdAndOrderId(String memberId, String orderId);

    void deleteAllByMemberId(String memberId);
}
